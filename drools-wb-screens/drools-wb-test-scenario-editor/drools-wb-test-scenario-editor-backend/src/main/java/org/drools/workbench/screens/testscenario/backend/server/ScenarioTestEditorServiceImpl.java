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

package org.drools.workbench.screens.testscenario.backend.server;

import com.dxc.drools.log.annotation.DroolsLoggingToDB;
import org.drools.workbench.models.datamodel.imports.Import;
import org.drools.workbench.models.datamodel.imports.Imports;
import org.drools.workbench.models.datamodel.oracle.ModelField;
import org.drools.workbench.models.datamodel.oracle.PackageDataModelOracle;
import org.drools.workbench.models.testscenarios.backend.util.ScenarioXMLPersistence;
import org.drools.workbench.models.testscenarios.shared.Scenario;
import org.drools.workbench.screens.testscenario.model.TestScenarioModelContent;
import org.drools.workbench.screens.testscenario.model.TestScenarioResult;
import org.drools.workbench.screens.testscenario.service.ScenarioTestEditorService;
import org.guvnor.common.services.backend.config.SafeSessionInfo;
import org.guvnor.common.services.backend.exceptions.ExceptionUtilities;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.backend.service.KieService;
import org.kie.workbench.common.services.datamodel.backend.server.DataModelOracleUtilities;
import org.kie.workbench.common.services.datamodel.backend.server.service.DataModelService;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleBaselinePayload;
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
import java.util.*;

@Service
@ApplicationScoped
public class ScenarioTestEditorServiceImpl
        extends KieService<TestScenarioModelContent>
        implements ScenarioTestEditorService {

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private CopyService copyService;

    @Inject
    private DeleteService deleteService;

    @Inject
    private RenameService renameService;

    @Inject
    private Event<ResourceOpenedEvent> resourceOpenedEvent;

    @Inject
    private DataModelService dataModelService;

    @Inject
    private ScenarioRunnerService scenarioRunner;

    @Inject
    private CommentedOptionFactory commentedOptionFactory;
    private SafeSessionInfo safeSessionInfo;

    public ScenarioTestEditorServiceImpl() {
    }

    @Inject
    public ScenarioTestEditorServiceImpl( final SessionInfo sessionInfo ) {
        safeSessionInfo = new SafeSessionInfo( sessionInfo );
    }

    @DroolsLoggingToDB
    @Override
    public Path create( final Path context,
                        final String fileName,
                        final Scenario content,
                        final String comment ) {
        try {
            final org.uberfire.java.nio.file.Path nioPath = Paths.convert( context ).resolve( fileName );
            final Path newPath = Paths.convert( nioPath );

            if ( ioService.exists( nioPath ) ) {
                throw new FileAlreadyExistsException( nioPath.toString() );
            }

            ioService.write( nioPath,
                             ScenarioXMLPersistence.getInstance().marshal( content ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

            return newPath;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public Scenario load( final Path path ) {
        try {
            final String content = ioService.readAllString( Paths.convert( path ) );

            Scenario scenario = ScenarioXMLPersistence.getInstance().unmarshal( content );
            scenario.setName( path.getFileName() );

            return scenario;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @DroolsLoggingToDB
    @Override
    public Path save( final Path resource,
                      final Scenario content,
                      final Metadata metadata,
                      final String comment ) {
        try {
            Metadata currentMetadata = metadataService.getMetadata( resource );
            ioService.write( Paths.convert( resource ),
                             ScenarioXMLPersistence.getInstance().marshal( content ),
                             metadataService.setUpAttributes( resource,
                                                              metadata ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

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

    @DroolsLoggingToDB
    @Override
    public TestScenarioModelContent loadContent( Path path ) {
        return super.loadContent( path );
    }

    @Override
    protected TestScenarioModelContent constructContent( Path path,
                                                         Overview overview ) {
        final Scenario scenario = load( path );
        final String packageName = projectService.resolvePackage( path ).getPackageName();
        final PackageDataModelOracle dataModelOracle = getDataModel( path );
        final PackageDataModelOracleBaselinePayload dataModel = new PackageDataModelOracleBaselinePayload();
        final Set<String> usedFullyQualifiedClassNames = getUsedFullyQualifiedClassNames( scenario, dataModelOracle );

        DataModelOracleUtilities.populateDataModel( dataModelOracle,
                                                    dataModel,
                                                    usedFullyQualifiedClassNames );

        //Signal opening to interested parties
        resourceOpenedEvent.fire( new ResourceOpenedEvent( path,
                                                           safeSessionInfo ) );

        return new TestScenarioModelContent( scenario,
                                             overview,
                                             packageName,
                                             dataModel );
    }

    @Override
    public TestScenarioResult runScenario( final Path path,
                                           final Scenario scenario ) {
        final Imports existingScenarioImports = new Imports( scenario.getImports().getImports() );

        try {
            addDependentImportsToScenario( scenario,
                                           path );
            final TestScenarioResult result = scenarioRunner.run( scenario,
                                                                  projectService.resolveProject( path ) );
            return result;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );

        } finally {
            scenario.setImports( existingScenarioImports );
        }
    }

    void addDependentImportsToScenario( final Scenario scenario,
                                        final Path path ) {
        final PackageDataModelOracle dataModel = getDataModel( path );
        final Set<String> usedFullyQualifiedClassNames = getUsedFullyQualifiedClassNames( scenario,
                                                                                          dataModel );

        for ( String className : usedFullyQualifiedClassNames ) {
            final Import imp = new Import( className );
            final List<Import> scenarioImports = scenario.getImports().getImports();

            if ( !scenarioImports.contains( imp ) ) {
                scenarioImports.add( imp );
            }
        }
    }

    private PackageDataModelOracle getDataModel( final Path path ) {
        return dataModelService.getDataModel( path );
    }

    private Set<String> getUsedFullyQualifiedClassNames( final Scenario scenario,
                                                         final PackageDataModelOracle dataModelOracle ) {
        return new HashSet<String>() {{
            addAll( getFullyQualifiedClassNamesUsedByModel( scenario,
                                                            dataModelOracle ) );
            addAll( getFullyQualifiedClassNamesUsedByGlobals( dataModelOracle ) );
        }};
    }

    private Set<String> getFullyQualifiedClassNamesUsedByModel( final Scenario scenario,
                                                                final PackageDataModelOracle dataModelOracle ) {
        final Set<String> fullyQualifiedClassNames = new HashSet<String>();
        for ( String fullyQualifiedClassName : getFullyQualifiedClassNameFromScenario( scenario ) ) {
            final ModelField[] modelFields = dataModelOracle.getProjectModelFields().get( fullyQualifiedClassName );
            if ( modelFields != null ) {
                for ( ModelField modelField : modelFields ) {
                    fullyQualifiedClassNames.add( modelField.getClassName() );
                }
                fullyQualifiedClassNames.add( fullyQualifiedClassName );
            }
        }

        return fullyQualifiedClassNames;
    }

    private List<String> getFullyQualifiedClassNameFromScenario( final Scenario scenario ) {
        final TestScenarioModelVisitor testScenarioModelVisitor = new TestScenarioModelVisitor( scenario );

        return new ArrayList<String>() {{
            addAll( testScenarioModelVisitor.visit() );
            for ( Import i : scenario.getImports().getImports() ) {
                add( i.getType() );
            }
        }};
    }

    private Collection<String> getFullyQualifiedClassNamesUsedByGlobals( final PackageDataModelOracle dataModelOracle ) {
        return dataModelOracle.getPackageGlobals().values();
    }

}
