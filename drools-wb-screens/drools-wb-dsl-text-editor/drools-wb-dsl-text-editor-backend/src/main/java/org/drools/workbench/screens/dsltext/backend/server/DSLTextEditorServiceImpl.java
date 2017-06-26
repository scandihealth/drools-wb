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

package org.drools.workbench.screens.dsltext.backend.server;

import com.dxc.drools.log.annotation.DroolsLoggingToDB;
import org.drools.compiler.lang.dsl.DSLMappingParseException;
import org.drools.compiler.lang.dsl.DSLTokenizedMappingFile;
import org.drools.workbench.screens.dsltext.model.DSLTextEditorContent;
import org.drools.workbench.screens.dsltext.service.DSLTextEditorService;
import org.drools.workbench.screens.dsltext.type.DSLResourceTypeDefinition;
import org.guvnor.common.services.backend.config.SafeSessionInfo;
import org.guvnor.common.services.backend.exceptions.ExceptionUtilities;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.project.builder.events.InvalidateDMOPackageCacheEvent;
import org.guvnor.common.services.shared.message.Level;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.backend.service.KieService;
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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
@ApplicationScoped
public class DSLTextEditorServiceImpl
        extends KieService<DSLTextEditorContent>
        implements DSLTextEditorService {

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
    private Event<InvalidateDMOPackageCacheEvent> invalidateDMOPackageCache;

    @Inject
    private Event<ResourceOpenedEvent> resourceOpenedEvent;

    @Inject
    private DSLResourceTypeDefinition resourceTypeDefinition;

    @Inject
    private CommentedOptionFactory commentedOptionFactory;

    private SafeSessionInfo safeSessionInfo;

    public DSLTextEditorServiceImpl() {
    }

    @Inject
    public DSLTextEditorServiceImpl( final SessionInfo sessionInfo ) {
        safeSessionInfo = new SafeSessionInfo( sessionInfo );
    }

    @DroolsLoggingToDB
    @Override
    public Path create( final Path context,
                        final String fileName,
                        final String content,
                        final String comment ) {
        try {
            final org.uberfire.java.nio.file.Path nioPath = Paths.convert( context ).resolve( fileName );
            final Path newPath = Paths.convert( nioPath );

            if ( ioService.exists( nioPath ) ) {
                throw new FileAlreadyExistsException( nioPath.toString() );
            }

            ioService.write( nioPath,
                             content,
                             commentedOptionFactory.makeCommentedOption( comment ) );

            return newPath;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public String load( final Path path ) {
        try {
            final String content = ioService.readAllString( Paths.convert( path ) );

            //Signal opening to interested parties
            resourceOpenedEvent.fire( new ResourceOpenedEvent( path,
                                                               safeSessionInfo ) );

            return content;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public DSLTextEditorContent loadContent( final Path path ) {
        return super.loadContent( path );
    }

    @Override
    protected DSLTextEditorContent constructContent( Path path, Overview overview ) {
        return new DSLTextEditorContent( load( path ),
                                         overview );
    }

    @DroolsLoggingToDB
    @Override
    public Path save( final Path resource,
                      final String content,
                      final Metadata metadata,
                      final String comment ) {
        try {
            Metadata currentMetadata = metadataService.getMetadata( resource );
            ioService.write( Paths.convert( resource ),
                             content,
                             metadataService.setUpAttributes( resource,
                                                              metadata ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

            //Invalidate Package-level DMO cache as a DSL has been altered
            invalidateDMOPackageCache.fire( new InvalidateDMOPackageCacheEvent( resource ) );

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
    public boolean accepts( final Path path ) {
        return resourceTypeDefinition.accept( path );
    }

    @Override
    public List<ValidationMessage> validate( final Path path ) {
        try {
            final String content = ioService.readAllString( Paths.convert( path ) );
            return validate( path,
                             content );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public List<ValidationMessage> validate( final Path path,
                                             final String content ) {
        return doValidation( content );
    }

    private List<ValidationMessage> doValidation( final String content ) {
        final List<ValidationMessage> validationMessages = new ArrayList<ValidationMessage>();
        final DSLTokenizedMappingFile dslLoader = new DSLTokenizedMappingFile();
        try {
            if ( !dslLoader.parseAndLoad( new StringReader( content ) ) ) {
                validationMessages.addAll( makeValidationMessages( dslLoader ) );
            }
            return validationMessages;

        } catch ( IOException e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    private List<ValidationMessage> makeValidationMessages( final DSLTokenizedMappingFile dslLoader ) {
        final List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
        for ( final Object o : dslLoader.getErrors() ) {
            if ( o instanceof DSLMappingParseException ) {
                final DSLMappingParseException dslMappingParseException = (DSLMappingParseException) o;
                messages.add( makeNewValidationMessage( dslMappingParseException ) );
            } else if ( o instanceof Exception ) {
                final Exception e = (Exception) o;
                messages.add( makeNewValidationMessage( e ) );
            } else {
                messages.add( makeNewValidationMessage( o ) );
            }
        }
        return messages;
    }

    private ValidationMessage makeNewValidationMessage( final DSLMappingParseException e ) {
        final ValidationMessage msg = new ValidationMessage();
        msg.setLevel( Level.ERROR );
        msg.setLine( e.getLine() );
        msg.setText( e.getMessage() );
        return msg;
    }

    private ValidationMessage makeNewValidationMessage( final Exception e ) {
        final ValidationMessage msg = new ValidationMessage();
        msg.setLevel( Level.ERROR );
        msg.setText( "Exception " + e.getClass() + " " + e.getMessage() + " " + e.getCause() );
        return msg;
    }

    private ValidationMessage makeNewValidationMessage( final Object o ) {
        final ValidationMessage msg = new ValidationMessage();
        msg.setLevel( Level.ERROR );
        msg.setText( "Uncategorized error " + o );
        return msg;
    }
}
