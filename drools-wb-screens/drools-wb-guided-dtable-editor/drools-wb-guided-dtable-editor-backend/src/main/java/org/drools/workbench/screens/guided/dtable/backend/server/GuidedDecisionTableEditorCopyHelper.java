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
package org.drools.workbench.screens.guided.dtable.backend.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.drools.workbench.models.guided.dtable.backend.GuidedDTXMLPersistence;
import org.drools.workbench.models.guided.dtable.shared.model.DTCellValue52;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.models.guided.dtable.shared.model.MetadataCol52;
import org.drools.workbench.screens.guided.dtable.type.GuidedDTableResourceTypeDefinition;
import org.drools.workbench.screens.guided.rule.backend.server.GuidedRuleEditorServiceUtilities;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.backend.service.helper.CopyHelper;
import org.uberfire.io.IOService;
import org.uberfire.workbench.type.FileNameUtil;

/**
 * CopyHelper for Guided Rule Templates
 */
@ApplicationScoped
public class GuidedDecisionTableEditorCopyHelper implements CopyHelper {

    @Inject
    private GuidedDTableResourceTypeDefinition resourceType;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private GuidedRuleEditorServiceUtilities utilities;

    @Inject
    private CommentedOptionFactory commentedOptionFactory;

    @Override
    public boolean supports(final Path destination) {
        return (resourceType.accept(destination));
    }

    @Override
    public void postProcess(final Path source,
                            final Path destination) {
        //Load existing file
        final org.uberfire.java.nio.file.Path _destination = Paths.convert(destination);
        final String content = ioService.readAllString(Paths.convert(destination));
        final GuidedDecisionTable52 model = GuidedDTXMLPersistence.getInstance().unmarshal(content);

        //Update table name
        final String tableName = FileNameUtil.removeExtension(destination,
                resourceType);
        model.setTableName(tableName);

        //Set LPR metadata
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put( LprMetadataConsts.HAS_PROD_VERSION, false );
        attributes.put( LprMetadataConsts.PRODUCTION_DATE, 0L );
        attributes.put( LprMetadataConsts.ARCHIVED_DATE, 0L );
        setDroolsLPRMetadata( model );

        //Save file
        ioService.write(_destination,
                GuidedDTXMLPersistence.getInstance().marshal(model),
                attributes,
                commentedOptionFactory.makeCommentedOption("File [" + source.toURI() + "] copied to [" + destination.toURI() + "]."));
    }

    private void setDroolsLPRMetadata(GuidedDecisionTable52 model) {
        for (MetadataCol52 metadataCol : model.getMetadataCols()) {
            if (LprMetadataConsts.PRODUCTION_DATE.equals(metadataCol.getMetadata())) {
                int index = model.getExpandedColumns().indexOf(metadataCol);
                for (List<DTCellValue52> row : model.getData()) {
                    DTCellValue52 dcv = row.get(index);
                    dcv.setStringValue(Long.toString(0L));
                }
            }
            if (LprMetadataConsts.HAS_PROD_VERSION.equals(metadataCol.getMetadata())) {
                int index = model.getExpandedColumns().indexOf(metadataCol);
                for (List<DTCellValue52> row : model.getData()) {
                    DTCellValue52 dcv = row.get(index);
                    dcv.setBooleanValue(false);
                }
            }
            if (LprMetadataConsts.ARCHIVED_DATE.equals(metadataCol.getMetadata())) {
                int index = model.getExpandedColumns().indexOf(metadataCol);
                for (List<DTCellValue52> row : model.getData()) {
                    DTCellValue52 dcv = row.get(index);
                    dcv.setStringValue(Long.toString(0L));
                }
            }
        }
    }
}
