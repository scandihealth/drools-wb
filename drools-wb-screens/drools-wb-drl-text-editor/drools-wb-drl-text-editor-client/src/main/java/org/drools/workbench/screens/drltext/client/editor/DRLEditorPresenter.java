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

package org.drools.workbench.screens.drltext.client.editor;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.models.datamodel.rule.DSLSentence;
import org.drools.workbench.screens.drltext.client.type.DRLResourceType;
import org.drools.workbench.screens.drltext.client.type.DSLRResourceType;
import org.drools.workbench.screens.drltext.model.DrlModelContent;
import org.drools.workbench.screens.drltext.service.DRLTextEditorService;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.widgets.metadata.client.KieEditorWrapperView;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPREditor;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPRFileMenuBuilder;
import org.kie.workbench.common.widgets.metadata.client.widget.OverviewWidgetPresenter;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.callbacks.Callback;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.client.workbench.type.ClientResourceType;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnMayClose;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.Menus;

import static org.guvnor.common.services.shared.metadata.model.LprMetadataConsts.*;

/**
 * This is the default rule editor widget (just text editor based).
 */
@Dependent
@WorkbenchEditor(identifier = "DRLEditor", supportedTypes = {DRLResourceType.class, DSLRResourceType.class})
public class DRLEditorPresenter
        extends LPREditor {

    @Inject
    private Caller<DRLTextEditorService> drlTextEditorService;

    private DRLEditorView view;

    @Inject
    private DRLResourceType resourceTypeDRL;

    @Inject
    private DSLRResourceType resourceTypeDSLR;

    private boolean isDSLR;


    /**
     * Wide constructor used by unit test to set mocks
     */
    @Inject
    public DRLEditorPresenter( final DRLEditorView view,
                               final Caller<DRLTextEditorService> drlTextEditorService,
                               final KieEditorWrapperView kieView,
                               final VersionRecordManager versionRecordManager,
                               final OverviewWidgetPresenter overviewWidget,
                               final Event<NotificationEvent> notification,
                               final Event<ChangeTitleWidgetEvent> changeTitleNotification,
                               final LPRFileMenuBuilder lprMenuBuilder ) {
        super( view );
        this.view = view;
        this.drlTextEditorService = drlTextEditorService;
        this.notification = notification;
        this.kieView = kieView;
        this.versionRecordManager = versionRecordManager;
        this.overviewWidget = overviewWidget;
        this.changeTitleNotification = changeTitleNotification;
        this.lprMenuBuilder = lprMenuBuilder;
    }

    @PostConstruct
    public void init() {
        view.init( this );
    }

    @OnStartup
    public void onStartup( final ObservablePath path,
                           final PlaceRequest place ) {
        super.init( path,
                place,
                getResourceType( path ) );
        this.isDSLR = resourceTypeDSLR != null && resourceTypeDSLR.accept( path );
    }

    protected void loadContent() {
        view.showLoading();
        drlTextEditorService.call( getLoadContentSuccessCallback(),
                getNoSuchFileExceptionErrorCallback() ).loadContent( versionRecordManager.getCurrentPath() );
    }

    private RemoteCallback<DrlModelContent> getLoadContentSuccessCallback() {
        return new RemoteCallback<DrlModelContent>() {

            @Override
            public void callback( final DrlModelContent content ) {
                //Path is set to null when the Editor is closed (which can happen before async calls complete).
                if ( versionRecordManager.getCurrentPath() == null ) {
                    return;
                }

                resetEditorPages( content.getOverview() );

                final String drl = assertContent( content.getDrl() );
                final List<String> fullyQualifiedClassNames = content.getFullyQualifiedClassNames();
                final List<DSLSentence> dslConditions = content.getDslConditions();
                final List<DSLSentence> dslActions = content.getDslActions();

                //Populate view
                if ( isDSLR ) {
                    view.setContent( drl,
                            fullyQualifiedClassNames,
                            dslConditions,
                            dslActions );
                } else {
                    view.setContent( drl,
                            fullyQualifiedClassNames );
                }
                view.setReadOnly( isReadOnly );
                view.hideBusyIndicator();
                createOriginalHash( view.getContent() );
            }

            private String assertContent( final String drl ) {
                if ( drl == null || drl.isEmpty() ) {
                    return "";
                }
                return drl;
            }

        };
    }

    public void loadClassFields( final String fullyQualifiedClassName,
                                 final Callback<List<String>> callback ) {
        drlTextEditorService.call( getLoadClassFieldsSuccessCallback( callback ),
                new HasBusyIndicatorDefaultErrorCallback( view ) ).loadClassFields( versionRecordManager.getCurrentPath(),
                fullyQualifiedClassName );

    }

    private RemoteCallback<List<String>> getLoadClassFieldsSuccessCallback( final Callback<List<String>> callback ) {
        return new RemoteCallback<List<String>>() {

            @Override
            public void callback( final List<String> fields ) {
                callback.callback( fields );
            }
        };
    }

    protected Command onValidate() {
        return getValidationCallback( drlTextEditorService, view );
    }

    @Override
    protected void save( String commitMessage ) {
        setDroolsMetadata();
        drlTextEditorService.call( getSaveSuccessCallback( getCurrentHash() ),
                new HasBusyIndicatorDefaultErrorCallback( view ) ).save( versionRecordManager.getCurrentPath(),
                view.getContent(),
                metadata,
                commitMessage );
    }

    @Override
    protected void setDroolsMetadata() {
        if ( !isDSLR ) {
            // split on rule, but keep delimiter as part of string by using lookahead regexp
            RegExp pattern = RegExp.compile( "(?=rule\\s*[\"'])", "g" );
            SplitResult splitResult = pattern.split( view.getContent() );
            StringBuilder contentBuilder = new StringBuilder();
            for ( int i = 0; i < splitResult.length(); i++ ) {
                String rule = splitResult.get( i );
                StringBuilder ruleBuilder = new StringBuilder( rule );
                updateDRLMetaData( ruleBuilder, RULE_TYPE, String.valueOf( metadata.getRuleType() ) );
                updateDRLMetaData( ruleBuilder, ERROR_TYPE, String.valueOf( metadata.getErrorType() ) );
                updateDRLMetaData( ruleBuilder, RULE_GROUP, String.valueOf( metadata.getRuleGroup() ) );
                updateDRLMetaData( ruleBuilder, ERROR_TEXT, String.valueOf( metadata.getErrorText() ) );
                updateDRLMetaData( ruleBuilder, ERROR_NUMBER, String.valueOf( metadata.getErrorNumber() ) );
                updateDRLMetaData( ruleBuilder, ERROR_BY_DAYS, String.valueOf( metadata.getErrorByDays() ) );
                updateDRLMetaData( ruleBuilder, IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( metadata.isValidForDUSASAbroadReports() ) );
                updateDRLMetaData( ruleBuilder, IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, String.valueOf( metadata.isValidForDUSASSpecialityReports() ) );
                updateDRLMetaData( ruleBuilder, IS_VALID_FOR_LPR_REPORTS, String.valueOf( metadata.isValidForLPRReports() ) );
                updateDRLMetaData( ruleBuilder, IS_VALID_FOR_PRIMARY_SECTOR_REPORTS, String.valueOf( metadata.isValidForPrimarySectorReports() ) );
                updateDRLMetaData( ruleBuilder, HAS_PROD_VERSION, String.valueOf( metadata.hasProdVersion() ) );
                updateDRLMetaData( ruleBuilder, PRODUCTION_DATE, String.valueOf( metadata.getProductionDate() ) );
                updateDRLMetaData( ruleBuilder, ARCHIVED_DATE, String.valueOf( metadata.getArchivedDate() ) );
                updateDRLMetaData( ruleBuilder, REPORT_RECEIVED_FROM_DATE, String.valueOf( metadata.getReportReceivedFromDate() ) );
                updateDRLMetaData( ruleBuilder, REPORT_RECEIVED_TO_DATE, String.valueOf( metadata.getReportReceivedToDate() ) );
                updateDRLMetaData( ruleBuilder, ENCOUNTER_START_FROM_DATE, String.valueOf( metadata.getEncounterStartFromDate() ) );
                updateDRLMetaData( ruleBuilder, ENCOUNTER_START_TO_DATE, String.valueOf( metadata.getEncounterStartToDate() ) );
                updateDRLMetaData( ruleBuilder, ENCOUNTER_END_FROM_DATE, String.valueOf( metadata.getEncounterEndFromDate() ) );
                updateDRLMetaData( ruleBuilder, ENCOUNTER_END_TO_DATE, String.valueOf( metadata.getEncounterEndToDate() ) );
                updateDRLMetaData( ruleBuilder, EPISODE_OF_CARE_START_FROM_DATE, String.valueOf( metadata.getEpisodeOfCareStartFromDate() ) );
                updateDRLMetaData( ruleBuilder, EPISODE_OF_CARE_START_TO_DATE, String.valueOf( metadata.getEpisodeOfCareStartToDate() ) );
                contentBuilder.append( ruleBuilder );
            }
            view.setContent( contentBuilder.toString() );
        }
    }

    private void updateDRLMetaData( StringBuilder rule, String name, String newValue ) {
        RegExp pattern = RegExp.compile( "rule\\s*(\".*?\")|('.*?')", "gs" ); //matches rule "my name is 'bob'" and matches rule 'my name is "bob"'. (uses "g" flag so getLastIndex() returns end of match)
        //index from where to insert new metadata (metadata should start next line after rule keyword)
        int newMetadataIndex = pattern.test( rule.toString() ) ? pattern.getLastIndex() : -1;

        //index from where to replace or delete existing metadata
        int currentMetadataIndex = -1;
        RegExp patternCurrentMetadata = RegExp.compile( "\\s+@" + name );
        MatchResult matchResultCurrentMetadata = patternCurrentMetadata.exec( rule.toString() );
        if ( matchResultCurrentMetadata != null ) {
            currentMetadataIndex = matchResultCurrentMetadata.getIndex();
        }
        int currentMetadataIndexEnd = rule.indexOf( ")", currentMetadataIndex ) + 1; //index where existing metadata ends

        //modify rule
        String metadataString = "\n\t@" + name + "(" + newValue + ")";
        if ( currentMetadataIndex == -1 && !("".equals( newValue ) || "null".equals( newValue ) || "undefined".equals( newValue )) ) {
            //create
            if ( newMetadataIndex > -1 ) { //only create if the DRL file actually has a rule
                rule.insert( newMetadataIndex, metadataString );
            }
        } else if ( currentMetadataIndex > -1 && !("".equals( newValue ) || "null".equals( newValue ) || "undefined".equals( newValue )) ) {
            //update
            rule.replace( currentMetadataIndex, currentMetadataIndexEnd, metadataString );
        } else if ( currentMetadataIndex > -1 && ("".equals( newValue ) || "null".equals( newValue ) || "undefined".equals( newValue )) ) {
            //delete
            rule.delete( currentMetadataIndex, currentMetadataIndexEnd );
        }
    }

    @OnClose
    public void onClose() {
        versionRecordManager.clear();
    }

    @OnMayClose
    public boolean mayClose() {
        return super.mayClose( view.getContent() );
    }

    private ClientResourceType getResourceType( Path path ) {
        if ( resourceTypeDRL != null && resourceTypeDRL.accept( path ) ) {
            return resourceTypeDRL;
        } else {
            return resourceTypeDSLR;
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

    @Override
    protected Integer getCurrentHash() {
        return view != null && view.getContent() != null ? view.getContent().hashCode() : null;
    }
}
