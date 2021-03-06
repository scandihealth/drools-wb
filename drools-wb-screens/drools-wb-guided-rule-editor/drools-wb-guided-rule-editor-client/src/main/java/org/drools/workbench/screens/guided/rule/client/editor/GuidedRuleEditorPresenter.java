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

package org.drools.workbench.screens.guided.rule.client.editor;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.models.datamodel.rule.RuleMetadata;
import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.screens.guided.rule.client.editor.validator.GuidedRuleEditorValidator;
import org.drools.workbench.screens.guided.rule.client.resources.GuidedRuleEditorResources;
import org.drools.workbench.screens.guided.rule.client.type.GuidedRuleDRLResourceType;
import org.drools.workbench.screens.guided.rule.client.type.GuidedRuleDSLRResourceType;
import org.drools.workbench.screens.guided.rule.model.GuidedEditorContent;
import org.drools.workbench.screens.guided.rule.service.GuidedRuleEditorService;
import org.guvnor.messageconsole.events.PublishMessagesEvent;
import org.guvnor.messageconsole.events.UnpublishMessagesEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleBaselinePayload;
import org.kie.workbench.common.services.shared.lpr.LPRManageProductionService;
import org.kie.workbench.common.services.shared.rulename.RuleNamesService;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracleFactory;
import org.kie.workbench.common.widgets.client.datamodel.ImportAddedEvent;
import org.kie.workbench.common.widgets.client.datamodel.ImportRemovedEvent;
import org.kie.workbench.common.widgets.client.source.ViewSourceView;
import org.kie.workbench.common.widgets.configresource.client.widget.bound.ImportsWidgetPresenter;
import org.kie.workbench.common.widgets.metadata.client.KieEditorWrapperView;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPREditor;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPRFileMenuBuilder;
import org.kie.workbench.common.widgets.metadata.client.widget.OverviewWidgetPresenter;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.client.workbench.type.ClientResourceType;
import org.uberfire.ext.editor.commons.client.file.SaveOperationService;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.ext.widgets.common.client.common.popups.errors.ErrorPopup;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnMayClose;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.ParameterizedCommand;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.Menus;

import static org.guvnor.common.services.shared.metadata.model.LprMetadataConsts.*;

@Dependent
@WorkbenchEditor(identifier = "GuidedRuleEditor", supportedTypes = {GuidedRuleDRLResourceType.class, GuidedRuleDSLRResourceType.class}, priority = 102)
public class GuidedRuleEditorPresenter
        extends LPREditor {

    @Inject
    private ImportsWidgetPresenter importsWidget;

    private GuidedRuleEditorView view;

    @Inject
    private ViewSourceView viewSource;

    @Inject
    private Caller<GuidedRuleEditorService> service;

    @Inject
    private Caller<RuleNamesService> ruleNamesService;

    @Inject
    private GuidedRuleDRLResourceType resourceTypeDRL;

    @Inject
    private GuidedRuleDSLRResourceType resourceTypeDSL;

    @Inject
    private AsyncPackageDataModelOracleFactory oracleFactory;

    private boolean isDSLEnabled;

    private RuleModel model;
    private AsyncPackageDataModelOracle oracle;

    /**
     * Wide constructor used by unit test to set mocks
     */
    @Inject
    public GuidedRuleEditorPresenter( final GuidedRuleEditorView view,
                                      final Caller<GuidedRuleEditorService> service,
                                      final Caller<LPRManageProductionService> lprProdService,
                                      final KieEditorWrapperView kieView,
                                      final VersionRecordManager versionRecordManager,
                                      final OverviewWidgetPresenter overviewWidget,
                                      final Event<NotificationEvent> notification,
                                      final Event<ChangeTitleWidgetEvent> changeTitleNotification,
                                      final Event<PublishMessagesEvent> publishMessages,
                                      final Event<UnpublishMessagesEvent> unpublishMessages,
                                      final LPRFileMenuBuilder lprMenuBuilder,
                                      final AsyncPackageDataModelOracleFactory oracleFactory,
                                      final ImportsWidgetPresenter importsWidget,
                                      final SaveOperationService saveOperationService,
                                      final SessionInfo sessionInfo ) {
        super( view );
        this.view = view;
        this.service = service;
        this.lprProdService = lprProdService;
        this.notification = notification;
        this.kieView = kieView;
        this.versionRecordManager = versionRecordManager;
        this.overviewWidget = overviewWidget;
        this.changeTitleNotification = changeTitleNotification;
        this.publishMessages = publishMessages;
        this.unpublishMessages = unpublishMessages;
        this.lprMenuBuilder = lprMenuBuilder;
        this.oracleFactory = oracleFactory;
        this.importsWidget = importsWidget;
        this.saveOperationService = saveOperationService;
        this.sessionInfo = sessionInfo;
    }

    @OnStartup
    public void onStartup( final ObservablePath path,
                           final PlaceRequest place ) {

        super.init( path,
                place,
                getResourceType( path ) );
        this.isDSLEnabled = resourceTypeDSL != null && resourceTypeDSL.accept( path );
    }

    protected void loadContent() {
        view.showLoading();
        service.call( getModelSuccessCallback(),
                getNoSuchFileExceptionErrorCallback() ).loadContent( versionRecordManager.getCurrentPath() );
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

    private RemoteCallback<GuidedEditorContent> getModelSuccessCallback() {
        return new RemoteCallback<GuidedEditorContent>() {

            @Override
            public void callback( final GuidedEditorContent content ) {
                //Path is set to null when the Editor is closed (which can happen before async calls complete).
                if ( versionRecordManager.getCurrentPath() == null ) {
                    return;
                }

                GuidedRuleEditorPresenter.this.model = content.getModel();
                final PackageDataModelOracleBaselinePayload dataModel = content.getDataModel();
                oracle = oracleFactory.makeAsyncPackageDataModelOracle( versionRecordManager.getPathToLatest(),
                        model,
                        dataModel );

                resetEditorPages( content.getOverview() );

                addSourcePage();

                addImportsTab( importsWidget );

                view.setContent( versionRecordManager.getCurrentPath(),
                        model,
                        oracle,
                        ruleNamesService,
                        isReadOnly,
                        isDSLEnabled );
                importsWidget.setContent( oracle,
                        model.getImports(),
                        isReadOnly );

                view.hideBusyIndicator();

                createOriginalHash( model );
            }
        };
    }

    public void handleImportAddedEvent( @Observes ImportAddedEvent event ) {
        if ( !event.getDataModelOracle().equals( this.oracle ) ) {
            return;
        }
        view.refresh();
    }

    public void handleImportRemovedEvent( @Observes ImportRemovedEvent event ) {
        if ( !event.getDataModelOracle().equals( this.oracle ) ) {
            return;
        }
        view.refresh();
    }

    protected Command onValidate() {
        return getValidationCallback( service, view );
    }

    protected void save() {
        GuidedRuleEditorValidator validator = new GuidedRuleEditorValidator( model,
                GuidedRuleEditorResources.CONSTANTS );

        if ( validator.isValid() ) {
            saveOperationService.save( versionRecordManager.getPathToLatest(),
                    new ParameterizedCommand<String>() {
                        @Override
                        public void execute( final String commitMessage ) {
                            view.showSaving();
                            save( commitMessage );
                        }
                    } );

            concurrentUpdateSessionInfo = null;
        } else {
            ErrorPopup.showMessage( validator.getErrors().get( 0 ) );
        }
    }

    @Override
    protected void save( String commitMessage ) {
        setDroolsMetadata();
        service.call( getSaveSuccessCallback( getCurrentHash() ),
                new HasBusyIndicatorDefaultErrorCallback( view ) ).save( versionRecordManager.getCurrentPath(),
                view.getContent(),
                metadata,
                commitMessage );
    }

    @Override
    protected void setDroolsMetadata() {
        updateGRMetaData( RULE_TYPE, String.valueOf( metadata.getRuleType() ) );
        updateGRMetaData( ERROR_TYPE, String.valueOf( metadata.getErrorType() ) );
        updateGRMetaData( RULE_GROUP, String.valueOf( metadata.getRuleGroup() ) );
        updateGRMetaData( ERROR_TEXT, String.valueOf( metadata.getErrorText() ) );
        updateGRMetaData( ERROR_NUMBER, String.valueOf( metadata.getErrorNumber() ) );
        updateGRMetaData( WARNING_TIME_LIMIT, String.valueOf( metadata.getWarningTimeLimit() ) );
        updateGRMetaData( IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( metadata.isValidForDUSASAbroadReports() ) );
        updateGRMetaData( IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, String.valueOf( metadata.isValidForDUSASSpecialityReports() ) );
        updateGRMetaData( IS_VALID_FOR_LPR_REPORTS, String.valueOf( metadata.isValidForLPRReports() ) );
        updateGRMetaData( IS_VALID_FOR_PRIVATE_SECTOR_REPORTS, String.valueOf( metadata.isValidForPrivateSectorReports() ) );
        updateGRMetaData( PRODUCTION_DATE, String.valueOf( metadata.getProductionDate() ) );
        updateGRMetaData( HAS_PROD_VERSION, String.valueOf( metadata.hasProdVersion() ) );
        updateGRMetaData( ARCHIVED_DATE, String.valueOf( metadata.getArchivedDate() ) );
        updateGRMetaData( REPORT_RECEIVED_FROM_DATE, String.valueOf( metadata.getReportReceivedFromDate() ) );
        updateGRMetaData( REPORT_RECEIVED_TO_DATE, String.valueOf( metadata.getReportReceivedToDate() ) );
        updateGRMetaData( ENCOUNTER_START_FROM_DATE, String.valueOf( metadata.getEncounterStartFromDate() ) );
        updateGRMetaData( ENCOUNTER_START_TO_DATE, String.valueOf( metadata.getEncounterStartToDate() ) );
        updateGRMetaData( ENCOUNTER_END_FROM_DATE, String.valueOf( metadata.getEncounterEndFromDate() ) );
        updateGRMetaData( ENCOUNTER_END_TO_DATE, String.valueOf( metadata.getEncounterEndToDate() ) );
        updateGRMetaData( EPISODE_OF_CARE_START_FROM_DATE, String.valueOf( metadata.getEpisodeOfCareStartFromDate() ) );
        updateGRMetaData( EPISODE_OF_CARE_START_TO_DATE, String.valueOf( metadata.getEpisodeOfCareStartToDate() ) );
    }


    @OnClose
    public void onClose() {
        this.versionRecordManager.clear();
        this.oracleFactory.destroy( oracle );
    }

    @OnMayClose
    public boolean mayClose() {
        return super.mayClose( view.getContent() );
    }

    private ClientResourceType getResourceType( final Path path ) {
        if ( resourceTypeDRL != null && resourceTypeDRL.accept( path ) ) {
            return resourceTypeDRL;
        } else {
            return resourceTypeDSL;
        }
    }

    @WorkbenchPartView
    public IsWidget getWidget() {
        return super.getWidget();
    }

    @WorkbenchMenu
    public Menus getMenus() {
        return menus;
    }

    private void updateGRMetaData( String name, String newValue ) {
        RuleMetadata currentMetaData = model.getMetaData( name );
        if ( currentMetaData == null && !("".equals( newValue ) || "null".equals( newValue ) || "undefined".equals( newValue )) ) {
            //create
            model.addMetadata( new RuleMetadata( name, newValue ) );
        } else if ( currentMetaData != null && !("".equals( newValue ) || "null".equals( newValue ) || "undefined".equals( newValue )) ) {
            //update
            currentMetaData.setValue( newValue );
            model.updateMetadata( currentMetaData );

        } else if ( currentMetaData != null && ("".equals( newValue ) || "null".equals( newValue ) || "undefined".equals( newValue )) ) {
            //delete
            for ( int i = 0; i < model.metadataList.length; i++ ) {
                RuleMetadata metadata = model.metadataList[i];
                if ( metadata.getAttributeName().equals( currentMetaData.getAttributeName() ) ) {
                    model.removeMetadata( i );
                    break;
                }
            }
        }
    }

    @Override
    protected Integer getCurrentHash() {
        return model != null ? model.hashCode() : null;
    }
}
