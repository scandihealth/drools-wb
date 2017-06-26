/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.globals.backend.server;

import com.dxc.drools.log.annotation.DroolsLoggingToDB;
import org.drools.workbench.models.datamodel.oracle.ProjectDataModelOracle;
import org.drools.workbench.screens.globals.backend.server.util.GlobalsPersistence;
import org.drools.workbench.screens.globals.model.GlobalsEditorContent;
import org.drools.workbench.screens.globals.model.GlobalsModel;
import org.drools.workbench.screens.globals.service.GlobalsEditorService;
import org.guvnor.common.services.backend.config.SafeSessionInfo;
import org.guvnor.common.services.backend.exceptions.ExceptionUtilities;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.backend.validation.GenericValidator;
import org.guvnor.common.services.project.builder.events.InvalidateDMOPackageCacheEvent;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.backend.service.KieService;
import org.kie.workbench.common.services.datamodel.backend.server.service.DataModelService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.service.CopyService;
import org.uberfire.ext.editor.commons.service.DeleteService;
import org.uberfire.ext.editor.commons.service.RenameService;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.FileAlreadyExistsException;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.ResourceOpenedEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

@Service
@ApplicationScoped
public class GlobalsEditorServiceImpl
        extends KieService<GlobalsEditorContent>
        implements GlobalsEditorService {

    @Inject
    @Named( "ioStrategy" )
    private IOService ioService;

    @Inject
    private CopyService copyService;

    @Inject
    private DeleteService deleteService;

    @Inject
    private RenameService renameService;

    @Inject
    private Event<InvalidateDMOPackageCacheEvent> invalidatePackageDMOEvent;

    @Inject
    private Event<ResourceOpenedEvent> resourceOpenedEvent;

    @Inject
    private DataModelService dataModelService;

    @Inject
    private GenericValidator genericValidator;

    @Inject
    private CommentedOptionFactory commentedOptionFactory;

    private SafeSessionInfo safeSessionInfo;

    public GlobalsEditorServiceImpl() {
    }

    @Inject
    public GlobalsEditorServiceImpl( final SessionInfo sessionInfo ) {
        safeSessionInfo = new SafeSessionInfo( sessionInfo );
    }

    @DroolsLoggingToDB
    @Override
    public Path create( final Path context,
                        final String fileName,
                        final GlobalsModel content,
                        final String comment ) {
        try {
            final Package pkg = projectService.resolvePackage( context );
            final String packageName = (pkg == null ? null : pkg.getPackageName());
            content.setPackageName( packageName );

            final org.uberfire.java.nio.file.Path nioPath = Paths.convert( context ).resolve( fileName );
            final Path newPath = Paths.convert( nioPath );

            if ( ioService.exists( nioPath ) ) {
                throw new FileAlreadyExistsException( nioPath.toString() );
            }

            ioService.write( nioPath,
                             GlobalsPersistence.getInstance().marshal( content ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

            return newPath;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public GlobalsModel load( final Path path ) {
        try {
            final String content = ioService.readAllString( Paths.convert( path ) );

            return GlobalsPersistence.getInstance().unmarshal( content );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public GlobalsEditorContent loadContent( final Path path ) {
        return super.loadContent( path );
    }

    @Override
    protected GlobalsEditorContent constructContent( Path path, Overview overview ) {
        //De-serialize model
        final GlobalsModel model = load( path );
        final ProjectDataModelOracle oracle = dataModelService.getProjectDataModel( path );
        final String[] fullyQualifiedClassNames = new String[oracle.getProjectModelFields().size()];
        oracle.getProjectModelFields().keySet().toArray( fullyQualifiedClassNames );

        //Signal opening to interested parties
        resourceOpenedEvent.fire( new ResourceOpenedEvent( path,
                                                           safeSessionInfo) );

        return new GlobalsEditorContent( model,
                                         overview,
                                         Arrays.asList( fullyQualifiedClassNames ) );

    }

    @DroolsLoggingToDB
    @Override
    public Path save( final Path resource,
                      final GlobalsModel content,
                      final Metadata metadata,
                      final String comment ) {
        try {
            final Package pkg = projectService.resolvePackage( resource );
            final String packageName = (pkg == null ? null : pkg.getPackageName());
            content.setPackageName( packageName );

            Metadata currentMetadata = metadataService.getMetadata( resource );
            ioService.write( Paths.convert( resource ),
                             GlobalsPersistence.getInstance().marshal( content ),
                             metadataService.setUpAttributes( resource,
                                                              metadata ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

            //Invalidate Package-level DMO cache as Globals have changed.
            invalidatePackageDMOEvent.fire( new InvalidateDMOPackageCacheEvent( resource ) );

            fireMetadataSocialEvents( resource, currentMetadata, metadata );
            return resource;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public void delete( final Path path,
                        final String comment ) {
        try {
            deleteService.delete( path,
                                  comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public Path rename( final Path path,
                        final String newName,
                        final String comment ) {
        try {
            return renameService.rename( path,
                                         newName,
                                         comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public Path copy( final Path path,
                      final String newName,
                      final String comment ) {
        try {
            return copyService.copy( path,
                                     newName,
                                     comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public String toSource(final Path path,
                           final GlobalsModel model) {
        return sourceServices.getServiceFor(Paths.convert(path)).getSource(Paths.convert(path),
                GlobalsPersistence.getInstance().marshal(model));
    }

    @Override
    public List<ValidationMessage> validate( final Path path,
                                             final GlobalsModel globalsModel ) {
        try {
            final String content = GlobalsPersistence.getInstance().marshal( globalsModel );

            return genericValidator.validate( path, content );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }
}
