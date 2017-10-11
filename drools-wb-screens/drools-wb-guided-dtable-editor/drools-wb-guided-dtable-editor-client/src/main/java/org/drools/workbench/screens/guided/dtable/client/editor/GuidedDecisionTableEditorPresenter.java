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

package org.drools.workbench.screens.guided.dtable.client.editor;

import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.models.guided.dtable.shared.model.DTCellValue52;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.models.guided.dtable.shared.model.MetadataCol52;
import org.drools.workbench.screens.guided.dtable.client.type.GuidedDTableResourceType;
import org.drools.workbench.screens.guided.dtable.model.GuidedDecisionTableEditorContent;
import org.drools.workbench.screens.guided.dtable.service.GuidedDecisionTableEditorService;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleBaselinePayload;
import org.kie.workbench.common.services.shared.rulename.RuleNamesService;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracleFactory;
import org.kie.workbench.common.widgets.configresource.client.widget.bound.ImportsWidgetPresenter;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPREditor;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnFocus;
import org.uberfire.lifecycle.OnMayClose;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.Menus;

import static org.guvnor.common.services.shared.metadata.model.LprMetadataConsts.*;

/**
 * Guided Decision Table Editor Presenter
 */
@Dependent
@WorkbenchEditor(identifier = "GuidedDecisionTableEditor", supportedTypes = {GuidedDTableResourceType.class})
public class GuidedDecisionTableEditorPresenter
        extends LPREditor {

    private GuidedDecisionTableEditorView view;

    @Inject
    private ImportsWidgetPresenter importsWidget;

    @Inject
    private Caller<GuidedDecisionTableEditorService> service;

    @Inject
    private Event<NotificationEvent> notification;

    @Inject
    private Caller<RuleNamesService> ruleNameService;

    @Inject
    private GuidedDTableResourceType type;

    @Inject
    private AsyncPackageDataModelOracleFactory oracleFactory;

    private GuidedDecisionTable52 model;
    private AsyncPackageDataModelOracle oracle;
    private GuidedDecisionTableEditorContent content;

    @Inject
    public GuidedDecisionTableEditorPresenter( final GuidedDecisionTableEditorView view ) {
        super( view );
        this.view = view;
    }

    @OnStartup
    public void onStartup( final ObservablePath path,
                           final PlaceRequest place ) {
        super.init( path,
                place,
                type );
    }

    @OnFocus
    public void onFocus() {
        // The height of the Sidebar widget in the underlying Decorated Grid library is set to the offsetHeight() of the Header
        // widget. When the Decision Table is not visible (i.e. it is not the top most editor in a TabPanel) offsetHeight() is zero
        // and the Decision Table Header and Sidebar are not sized correctly. Therefore we need to ensure the widgets are sized
        // correctly when the widget becomes visible.
        view.onFocus();
    }

    protected void loadContent() {
        view.showLoading();
        service.call( getModelSuccessCallback(),
                getNoSuchFileExceptionErrorCallback() ).loadContent( versionRecordManager.getCurrentPath() );
    }

    private RemoteCallback<GuidedDecisionTableEditorContent> getModelSuccessCallback() {
        return new RemoteCallback<GuidedDecisionTableEditorContent>() {

            @Override
            public void callback( final GuidedDecisionTableEditorContent content ) {
                //Path is set to null when the Editor is closed (which can happen before async calls complete).
                if ( versionRecordManager.getCurrentPath() == null ) {
                    return;
                }

                GuidedDecisionTableEditorPresenter.this.content = content;

                model = content.getModel();
                metadata = content.getOverview().getMetadata();
                final PackageDataModelOracleBaselinePayload dataModel = content.getDataModel();
                oracle = oracleFactory.makeAsyncPackageDataModelOracle( versionRecordManager.getCurrentPath(),
                        model,
                        dataModel );

                resetEditorPages( content.getOverview() );
                addSourcePage();

                addImportsTab( importsWidget );

                importsWidget.setContent( oracle,
                        model.getImports(),
                        isReadOnly );

                view.hideBusyIndicator();
            }
        };
    }

    @Override
    public void onEditTabSelected() {
        view.setContent( place,
                versionRecordManager.getCurrentPath(),
                model,
                content.getWorkItemDefinitions(),
                oracle,
                ruleNameService,
                isReadOnly );
    }

    protected Command onValidate() {
        return getValidationCallback( service, view );
    }

    @Override
    protected void save( String commitMessage ) {
        //todo ttn make unit test
        updateGDTMetaData( RULE_TYPE, String.valueOf( metadata.getRuleType() ) );
        updateGDTMetaData( ERROR_TYPE, String.valueOf( metadata.getErrorType() ) );
        updateGDTMetaData( RULE_GROUP, String.valueOf( metadata.getRuleGroup() ) );
        updateGDTMetaData( ERROR_TEXT, String.valueOf( metadata.getErrorText() ) );
        updateGDTMetaData( ERROR_NUMBER, String.valueOf( metadata.getErrorNumber() ) );
        updateGDTMetaData( IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( metadata.isValidForDUSASAbroadReports() ) );
        updateGDTMetaData( IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, String.valueOf( metadata.isValidForDUSASSpecialityReports() ) );
        updateGDTMetaData( IS_VALID_FOR_LPR_REPORTS, String.valueOf( metadata.isValidForLPRReports() ) );
        updateGDTMetaData( IS_VALID_FOR_PRIVATE_SECTOR_REPORTS, String.valueOf( metadata.isValidForPrivateSectorReports() ) );
        updateGDTMetaData( PRODUCTION_DATE, String.valueOf( metadata.getProductionDate() ) );
        updateGDTMetaData( ARCHIVED_DATE, String.valueOf( metadata.getArchivedDate() ) );
        updateGDTMetaData( REPORT_RECEIVED_FROM_DATE, String.valueOf( metadata.getReportReceivedFromDate() ) );
        updateGDTMetaData( REPORT_RECEIVED_TO_DATE, String.valueOf( metadata.getReportReceivedToDate() ) );
        updateGDTMetaData( ENCOUNTER_START_FROM_DATE, String.valueOf( metadata.getEncounterStartFromDate() ) );
        updateGDTMetaData( ENCOUNTER_START_TO_DATE, String.valueOf( metadata.getEncounterStartToDate() ) );
        updateGDTMetaData( ENCOUNTER_END_FROM_DATE, String.valueOf( metadata.getEncounterEndFromDate() ) );
        updateGDTMetaData( ENCOUNTER_END_TO_DATE, String.valueOf( metadata.getEncounterEndToDate() ) );
        updateGDTMetaData( EPISODE_OF_CARE_START_FROM_DATE, String.valueOf( metadata.getEpisodeOfCareStartFromDate() ) );
        updateGDTMetaData( EPISODE_OF_CARE_START_TO_DATE, String.valueOf( metadata.getEpisodeOfCareStartToDate() ) );

        service.call( getSaveSuccessCallback( getCurrentHash() ),
                new HasBusyIndicatorDefaultErrorCallback( view ) ).save( versionRecordManager.getCurrentPath(),
                model,
                metadata,
                commitMessage );
    }

    private void updateGDTMetaData( String name, String newValue ) {
        boolean found = false;
        for ( MetadataCol52 metadataCol : model.getMetadataCols() ) {
            if ( metadataCol.getMetadata().equals( name ) ) {
                found = true;
                if ( "".equals( newValue ) || "null".equals( newValue ) ) {
                    //delete column + cells
                    int index = model.getExpandedColumns().indexOf( metadataCol );
                    model.getMetadataCols().remove( metadataCol );
                    for ( List<DTCellValue52> row : model.getData() ) {
                        DTCellValue52 dcv = row.remove( index );
                    }
                } else {
                    //update each row with new cell values
                    int index = model.getExpandedColumns().indexOf( metadataCol );
                    for ( List<DTCellValue52> row : model.getData() ) {
                        DTCellValue52 dcv = row.get( index );
                        dcv.setStringValue( newValue );
                    }
                }
            }
        }
        if ( !found ) {
            //create new column + cells
            MetadataCol52 newMetaCol = new MetadataCol52();
            newMetaCol.setHideColumn( true );
            newMetaCol.setMetadata( name );
            model.getMetadataCols().add( newMetaCol );
            for ( List<DTCellValue52> row : model.getData() ) {
                row.add( new DTCellValue52( newValue ) );
            }
        }
    }

    @Override
    public void onSourceTabSelected() {
        service.call( new RemoteCallback<String>() {
            @Override
            public void callback( String source ) {
                updateSource( source );
            }
        } ).toSource( versionRecordManager.getCurrentPath(),
                model );
    }

    @OnClose
    public void onClose() {
        this.versionRecordManager.clear();
        this.oracleFactory.destroy( oracle );
        this.view.onClose();
    }

    @OnMayClose
    public boolean mayClose() {
        return mayClose( model );
    }

    @WorkbenchPartView
    public IsWidget getWidget() {
        return super.getWidget();
    }

    @WorkbenchMenu
    public Menus getMenus() {
        return menus;
    }

    @Override
    protected Integer getCurrentHash() {
        return model != null ? model.hashCode() : null;
    }
}
