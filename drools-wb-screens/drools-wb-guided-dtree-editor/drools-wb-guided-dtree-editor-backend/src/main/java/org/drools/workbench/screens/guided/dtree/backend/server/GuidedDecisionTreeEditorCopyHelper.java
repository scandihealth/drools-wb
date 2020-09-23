/*
* Copyright 2014 Red Hat, Inc. and/or its affiliates.
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
package org.drools.workbench.screens.guided.dtree.backend.server;

import java.util.Date;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.drools.workbench.models.datamodel.oracle.PackageDataModelOracle;
import org.drools.workbench.models.guided.dtree.backend.GuidedDecisionTreeDRLPersistence;
import org.drools.workbench.models.guided.dtree.shared.model.GuidedDecisionTree;
import org.drools.workbench.screens.guided.dtree.type.GuidedDTreeResourceTypeDefinition;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.jboss.errai.security.shared.api.identity.User;
import org.kie.workbench.common.services.datamodel.backend.server.service.DataModelService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.backend.service.helper.CopyHelper;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.base.options.CommentedOption;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.type.FileNameUtil;

/**
 * CopyHelper for Guided Decision Trees
 */
@ApplicationScoped
public class GuidedDecisionTreeEditorCopyHelper implements CopyHelper {

    @Inject
    private GuidedDTreeResourceTypeDefinition resourceType;

    @Inject
    private User identity;

    @Inject
    private SessionInfo sessionInfo;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private DataModelService dataModelService;

    @Inject
    private CommentedOptionFactory commentedOptionFactory;

    @Override
    public boolean supports( final Path destination ) {
        return ( resourceType.accept( destination ) );
    }

    @Override
    public void postProcess( final Path source,
                             final Path destination,
                             final String comment ) {
        //Load existing file
        final org.uberfire.java.nio.file.Path _destination = Paths.convert( destination );
        final String drl = ioService.readAllString( Paths.convert( destination ) );
        final String baseFileName = FileNameUtil.removeExtension( source,
                                                                  resourceType );
        final PackageDataModelOracle oracle = dataModelService.getDataModel( source );

        final GuidedDecisionTree model = GuidedDecisionTreeDRLPersistence.getInstance().unmarshal( drl,
                                                                                                   baseFileName,
                                                                                                   oracle );

        //Update tree name
        final String treeName = FileNameUtil.removeExtension( destination,
                                                              resourceType );
        model.setTreeName( treeName );

        //Save file
        ioService.write( _destination,
                         GuidedDecisionTreeDRLPersistence.getInstance().marshal( model ),
                         commentedOptionFactory.makeCommentedOption( "File [" + source.toURI() + "] copied to [" + destination.toURI() + "]." ) );
    }

}
