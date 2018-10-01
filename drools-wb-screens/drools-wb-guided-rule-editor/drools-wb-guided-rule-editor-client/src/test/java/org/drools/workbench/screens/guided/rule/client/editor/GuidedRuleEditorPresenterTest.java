package org.drools.workbench.screens.guided.rule.client.editor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.event.Event;

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import org.apache.commons.lang3.ArrayUtils;
import org.drools.workbench.models.datamodel.rule.RuleMetadata;
import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.screens.guided.rule.model.GuidedEditorContent;
import org.guvnor.common.services.shared.message.Level;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.guvnor.common.services.shared.metadata.model.LprRuleType;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.guvnor.messageconsole.events.PublishMessagesEvent;
import org.guvnor.messageconsole.events.UnpublishMessagesEvent;
import org.jboss.errai.common.client.api.Caller;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleBaselinePayload;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracleFactory;
import org.kie.workbench.common.widgets.client.popups.validation.ValidationPopup;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.kie.workbench.common.widgets.configresource.client.widget.bound.ImportsWidgetPresenter;
import org.kie.workbench.common.widgets.metadata.client.KieEditorWrapperView;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPREditor;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPRFileMenuBuilder;
import org.kie.workbench.common.widgets.metadata.client.widget.OverviewWidgetPresenter;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.ext.editor.commons.client.file.SaveOperationService;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.ext.editor.commons.client.menu.MenuItems;
import org.uberfire.ext.editor.commons.client.validation.Validator;
import org.uberfire.ext.widgets.common.client.common.popups.errors.ErrorPopup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.ParameterizedCommand;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuItemCommand;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.same;

/**
 * Created on 09-11-2017.
 */
@RunWith(GwtMockitoTestRunner.class)
@WithClassesToStub({ValidationPopup.class, ErrorPopup.class})
public class GuidedRuleEditorPresenterTest {

    @Mock
    private GuidedRuleEditorView view;

    @Mock
    private SaveOperationService saveOperationService;

    @Mock
    private SaveOperationService.SaveOperationNotifier saveOperationNotifier;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private VersionRecordManager versionRecordManager;

    @Spy
    private LPRFileMenuBuilder lprMenuBuilder;

    @Spy
    private EventMock notificationsEventMock;

    @Spy
    private EventMock changeTitleEventMock;

    @Spy
    private EventMock publishMessagesMock;

    @Spy
    private EventMock unpublishMessagesMock;

    private RuleModel viewContent = new RuleModel();
    private Overview overview = mock( Overview.class );

    private GuidedEditorContent guidedEditorContent = new GuidedEditorContent( viewContent, overview, mock( PackageDataModelOracleBaselinePayload.class ) );
    private ArrayList<ValidationMessage> validationMessages = new ArrayList<ValidationMessage>();
    @Spy
    private MockGuidedRuleEditorServiceCaller guidedRuleEditorServiceCaller = new MockGuidedRuleEditorServiceCaller( guidedEditorContent, validationMessages );

    @Spy
    private MockLPRManageProductionServiceCaller lprManageProductionServiceCaller = new MockLPRManageProductionServiceCaller();

    @Spy
    private Metadata metadata;

    private GuidedRuleEditorPresenter presenter;

    @Before
    public void setUp() throws Exception {
        ApplicationPreferences.setUp( new HashMap<String, String>() {{
            put( ApplicationPreferences.DATE_FORMAT,
                    "dd-MM-yyyy" );
        }} );
        guidedRuleEditorServiceCaller.service.loadContentCallbackArg = guidedEditorContent;
        guidedRuleEditorServiceCaller.service.validateCallbackArg = validationMessages;

        metadata.setRuleType( LprRuleType.REPORT_VALIDATION );
        metadata.setProductionDate( 0L );
        metadata.setHasProdVersion( false );
        metadata.setArchivedDate( 0L );
        metadata.setErrorType( LprErrorType.ERROR );
        metadata.setErrorNumber( 1L );
        metadata.setErrorText( "error text" );
        metadata.setEncounterStartFromDate( 1000L );
        reset( metadata );
        when( overview.getMetadata() ).thenReturn( metadata );

        when( versionRecordManager.isCurrentLatest() ).thenReturn( true );

        //create mock methods on view so when setContent is called, its input is returned when getContent is called
        when( view.getContent() ).thenAnswer( new Answer<RuleModel>() {
            @Override
            public RuleModel answer( InvocationOnMock invocation ) throws Throwable {
                return viewContent;
            }
        } );
        doAnswer( new Answer() {
            @Override
            public Object answer( InvocationOnMock invocation ) throws Throwable {
                viewContent = invocation.getArgumentAt( 1, RuleModel.class );
                return null;
            }
        } ).when( view ).setContent( any( Path.class ), any( RuleModel.class ), any( AsyncPackageDataModelOracle.class ), any( Caller.class ), anyBoolean(), anyBoolean() );

        //create mock operation on saveOperationService#save that immediately executes the passed save command instead of showing save popup
        doAnswer( new Answer() {
            @Override
            public Object answer( InvocationOnMock invocation ) throws Throwable {
                invocation.getArgumentAt( 1, ParameterizedCommand.class ).execute( "commitMessage" );
                return null;
            }
        } ).when( saveOperationService ).save( any( Path.class ), any( ParameterizedCommand.class ) );

        presenter = spy( new GuidedRuleEditorPresenter(
                view,
                guidedRuleEditorServiceCaller,
                lprManageProductionServiceCaller,
                mock( KieEditorWrapperView.class ),
                versionRecordManager,
                mock( OverviewWidgetPresenter.class ),
                notificationsEventMock,
                changeTitleEventMock,
                publishMessagesMock,
                unpublishMessagesMock,
                lprMenuBuilder,
                mock( AsyncPackageDataModelOracleFactory.class ),
                mock( ImportsWidgetPresenter.class ),
                saveOperationService,
                mock( SessionInfo.class, RETURNS_MOCKS )
        ) );
        doReturn( saveOperationNotifier ).when( presenter ).getSaveNotifier();
    }

    @Test
    public void addsLPRMenuItems() throws Exception {
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) );
        verify( lprMenuBuilder ).addSave( any( Command.class ) );
        verify( lprMenuBuilder ).addCopy( any( Path.class ), any( Validator.class ) );
        verify( lprMenuBuilder ).addDelete( any( Command.class ) );
        verify( lprMenuBuilder ).addMoveToProduction( any( Command.class ) );
        verify( lprMenuBuilder ).addArchive( any( Command.class ) );
        verify( lprMenuBuilder ).addValidate( any( Command.class ) );
        verify( lprMenuBuilder ).addVersionMenu( any( MenuItem.class ) );
        verify( lprMenuBuilder, never() ).addRename( any( Path.class ), any( Validator.class ) ); //rename is permanently disabled as it truncates version history and this screws up the the draft/prod/archive LPR rule lifecycle
        verify( lprMenuBuilder ).build();

        Map<Object, MenuItem> menuMap = presenter.getMenus().getItemsMap();
        assertTrue( menuMap.containsKey( MenuItems.SAVE ) );
        assertTrue( menuMap.containsKey( MenuItems.COPY ) );
        assertTrue( menuMap.containsKey( MenuItems.DELETE ) );
        assertTrue( menuMap.containsKey( MenuItems.MOVETOPRODUCTION ) );
        assertTrue( menuMap.containsKey( MenuItems.ARCHIVE ) );
        assertTrue( menuMap.containsKey( MenuItems.DELETE ) );
        assertTrue( menuMap.containsKey( MenuItems.VALIDATE ) );
        assertFalse( menuMap.containsKey( MenuItems.RENAME ) ); //rename is permanently disabled as it truncates version history and this screws up the the draft/prod/archive LPR rule lifecycle
        assertFalse( menuMap.containsKey( MenuItems.RESTORE ) ); //todo ttn restore is not implemented correctly yet..

        assertTrue( menuMap.get( MenuItems.SAVE ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.DELETE ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.COPY ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.MOVETOPRODUCTION ).isEnabled() );
        assertFalse( menuMap.get( MenuItems.ARCHIVE ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.VALIDATE ).isEnabled() );

    }

    @Test
    public void saveDraft() throws Exception {
        int oldHash = viewContent.hashCode();
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        Map<Object, MenuItem> menuMap = presenter.getMenus().getItemsMap();
        Command saveCommand = (( MenuItemCommand ) menuMap.get( MenuItems.SAVE )).getCommand();
        saveCommand.execute();

        //LPREditor
        verify( metadata ).setProductionDate( eq( 0L ) ); //saving as draft
        //GuidedRuleEditorPresenter#save
        verify( view ).showSaving();
        verify( presenter ).setDroolsMetadata();
        //BaseEditor#getSaveSuccessCallback
        verify( view, times( 2 ) ).hideBusyIndicator(); //1 during loadContent and 1 during save
        verify( versionRecordManager ).reloadVersions( any( Path.class ) );

        ArgumentCaptor<NotificationEvent> notificationEventArg = ArgumentCaptor.forClass( NotificationEvent.class );
        verify( notificationsEventMock, atLeastOnce() ).fire( notificationEventArg.capture() );
        assertEquals( NotificationEvent.NotificationType.DEFAULT, notificationEventArg.getValue().getType() );
        assertEquals( CommonConstants.INSTANCE.ItemSavedSuccessfully(), notificationEventArg.getValue().getNotification() );

        verify( presenter ).setOriginalHash( not( eq( oldHash ) ) ); //hash should not be the same as old model without metadata
        verify( presenter ).setOriginalHash( eq( view.getContent().hashCode() ) ); //hash should be changed to new drl (that includes metadata)
        //LPREditor#getSaveSuccessCallback
        verify( changeTitleEventMock, times( 2 ) ).fire( isA( ChangeTitleWidgetEvent.class ) ); //1 during loadContent and 1 during save
        verify( view, times( 2 ) ).refreshTitle( contains( "Kladde" ) ); //1 during loadContent and 1 during save
        verify( presenter, never() ).reload(); //editor should not reload since rule is not being archived

        assertTrue( menuMap.get( MenuItems.SAVE ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.DELETE ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.COPY ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.MOVETOPRODUCTION ).isEnabled() );
        assertFalse( menuMap.get( MenuItems.ARCHIVE ).isEnabled() );
        assertTrue( menuMap.get( MenuItems.VALIDATE ).isEnabled() );

    }

    @Test
    public void cantMoveToProdWhenUnsavedChanges() throws Exception {
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        //setup prodCommand to stop after dirty checking (validate service will never respond)
        guidedRuleEditorServiceCaller.service = mock( MockGuidedRuleEditorService.class );
        LPREditor.MoveToProductionCommand prodCommand = ( LPREditor.MoveToProductionCommand ) (( MenuItemCommand ) presenter.getMenus().getItemsMap().get( MenuItems.MOVETOPRODUCTION )).getCommand();

        //test dirty handling
        when( presenter.isDirty( anyInt() ) ).thenReturn( true );
        prodCommand.execute();

        ArgumentCaptor<NotificationEvent> notificationEventArg = ArgumentCaptor.forClass( NotificationEvent.class );
        verify( notificationsEventMock ).fire( notificationEventArg.capture() );
        assertEquals( NotificationEvent.NotificationType.ERROR, notificationEventArg.getValue().getType() );
        assertThat( notificationEventArg.getValue().getNotification(), containsString( "kan ikke produktionssættes. Reglen har ikke-gemte ændringer. Gem eller fjern ændringerne." ) );

        ArgumentCaptor<PublishMessagesEvent> publishMessageEventArg = ArgumentCaptor.forClass( PublishMessagesEvent.class );
        verify( publishMessagesMock ).fire( publishMessageEventArg.capture() );
        PublishMessagesEvent publishMessagesEvent = publishMessageEventArg.getValue();
        assertEquals( 1, publishMessagesEvent.getMessagesToPublish().size() );
        assertEquals( Level.ERROR, publishMessagesEvent.getMessagesToPublish().get( 0 ).getLevel() );
        assertThat( publishMessagesEvent.getMessagesToPublish().get( 0 ).getMessageType(), containsString( LPREditor.MoveToProductionCommand.MOVE_TO_PROD_DIRTY_ERROR_MESSAGE_TYPE ) );

        verify( presenter, never() ).save( anyString() );

        //test that error messages gets unpublished when fixed
        when( presenter.isDirty( anyInt() ) ).thenReturn( false );
        prodCommand.execute();
        ArgumentCaptor<UnpublishMessagesEvent> unpublishMessageEventArg = ArgumentCaptor.forClass( UnpublishMessagesEvent.class );
        verify( unpublishMessagesMock ).fire( unpublishMessageEventArg.capture() );
        assertThat( unpublishMessageEventArg.getValue().getMessageType(), containsString( LPREditor.MoveToProductionCommand.MOVE_TO_PROD_DIRTY_ERROR_MESSAGE_TYPE ) );
    }

    @Test
    public void cantMoveToProdWithValidationError() throws Exception {
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        //setup prodCommand to stop after validation
        LPREditor.MoveToProductionCommand prodCommand = getPresenterMoveToProdCommand();
        when( prodCommand.getShowPopupCommand( any( Command.class ) ) ).thenReturn( mock( Command.class ) );
        //make validation fail when performed
        validationMessages.add( new ValidationMessage( 0, Level.ERROR, 1, 1, "test" ) );
        prodCommand.execute();

        verify( presenter, never() ).save( anyString() );

        ArgumentCaptor<PublishMessagesEvent> publishMessageEventArg = ArgumentCaptor.forClass( PublishMessagesEvent.class );
        verify( publishMessagesMock ).fire( publishMessageEventArg.capture() );
        PublishMessagesEvent publishMessagesEvent = publishMessageEventArg.getValue();
        assertEquals( 1, publishMessagesEvent.getMessagesToPublish().size() );
        assertEquals( Level.ERROR, publishMessagesEvent.getMessagesToPublish().get( 0 ).getLevel() );
        assertThat( publishMessagesEvent.getMessagesToPublish().get( 0 ).getMessageType(), containsString( LPREditor.VALIDATION_ERROR ) );

        //test that validation errors gets unpublished when fixed
        validationMessages.clear();
        prodCommand.execute();
        ArgumentCaptor<UnpublishMessagesEvent> unpublishMessageEventArg = ArgumentCaptor.forClass( UnpublishMessagesEvent.class );
        verify( unpublishMessagesMock, atLeastOnce() ).fire( unpublishMessageEventArg.capture() );
        assertThat( unpublishMessageEventArg.getValue().getMessageType(), containsString( LPREditor.VALIDATION_ERROR ) );
    }

    @Test
    public void cantMoveToProdWhenNotMostRecentVersion() throws Exception {
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        LPREditor.MoveToProductionCommand prodCommand = getPresenterMoveToProdCommand();
        doNothing().when( presenter ).save( anyString() );
        when( versionRecordManager.isCurrentLatest() ).thenReturn( false );
        prodCommand.execute();

        verify( presenter, never() ).save( anyString() );

        ArgumentCaptor<NotificationEvent> notificationEventArg = ArgumentCaptor.forClass( NotificationEvent.class );
        verify( notificationsEventMock, atLeastOnce() ).fire( notificationEventArg.capture() );
        assertEquals( NotificationEvent.NotificationType.ERROR, notificationEventArg.getValue().getType() );
        assertThat( notificationEventArg.getValue().getNotification(), containsString( "kan ikke produktionssættes: Kun nyeste version kan produktionssættes" ) );

        ArgumentCaptor<PublishMessagesEvent> publishMessageEventArg = ArgumentCaptor.forClass( PublishMessagesEvent.class );
        verify( publishMessagesMock ).fire( publishMessageEventArg.capture() );
        PublishMessagesEvent publishMessagesEvent = publishMessageEventArg.getValue();
        assertEquals( 1, publishMessagesEvent.getMessagesToPublish().size() );
        assertEquals( Level.ERROR, publishMessagesEvent.getMessagesToPublish().get( 0 ).getLevel() );
        assertThat( publishMessagesEvent.getMessagesToPublish().get( 0 ).getMessageType(), containsString( LPREditor.MoveToProductionCommand.MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE ) );

        //test previous error messages gets unpublished when there is no error
        when( versionRecordManager.isCurrentLatest() ).thenReturn( true );
        prodCommand.execute();
        ArgumentCaptor<UnpublishMessagesEvent> unpublishMessageEventArg = ArgumentCaptor.forClass( UnpublishMessagesEvent.class );
        verify( unpublishMessagesMock, atLeastOnce() ).fire( unpublishMessageEventArg.capture() );
        assertThat( unpublishMessageEventArg.getValue().getMessageType(), containsString( LPREditor.MoveToProductionCommand.MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE ) );
    }

    @Test
    public void moveToProdRollbackOnError() throws Exception {
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        LPREditor.MoveToProductionCommand prodCommand = getPresenterMoveToProdCommand();
        lprManageProductionServiceCaller.service.throwError = true; //make error
        prodCommand.execute();

        verify( lprManageProductionServiceCaller.service ).copyToProductionBranch( any( Path.class ) );
        //verify rollback
        assertEquals( 0L, metadata.getProductionDate().longValue() );
        verify( presenter ).save( eq( "Produktionsættelse rullet tilbage pga. systemfejl" ) );
    }

    @Test
    public void moveToProd() throws Exception {
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        LPREditor.MoveToProductionCommand prodCommand = getPresenterMoveToProdCommand();
        prodCommand.execute();

        verify( lprManageProductionServiceCaller.service ).copyToProductionBranch( any( Path.class ) );

        ArgumentCaptor<NotificationEvent> notificationEventArg = ArgumentCaptor.forClass( NotificationEvent.class );
        verify( notificationsEventMock, atLeastOnce() ).fire( notificationEventArg.capture() );
        assertEquals( NotificationEvent.NotificationType.SUCCESS, notificationEventArg.getValue().getType() );
        assertEquals( CommonConstants.INSTANCE.LPRItemMoveToProductionSuccessfully(), notificationEventArg.getValue().getNotification() );
        verify( view ).refreshTitle( contains( "Produktionssat" ) );

        assertTrue( presenter.getMenus().getItemsMap().get( MenuItems.SAVE ).isEnabled() );
        assertFalse( presenter.getMenus().getItemsMap().get( MenuItems.DELETE ).isEnabled() );
        assertTrue( presenter.getMenus().getItemsMap().get( MenuItems.COPY ).isEnabled() );
        assertFalse( presenter.getMenus().getItemsMap().get( MenuItems.MOVETOPRODUCTION ).isEnabled() );
        assertTrue( presenter.getMenus().getItemsMap().get( MenuItems.ARCHIVE ).isEnabled() );
    }


    @Test
    @Ignore
    public void saveArchived() throws Exception {
        //todo ttn impl
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        verify( view ).refreshTitle( contains( "Arkiveret" ) );
        verify( presenter ).reload(); //editor should be reload since rule is archived (to change into read only mode)
        assertTrue( presenter.isReadOnly() );
    }

    @Test
    @Ignore
    public void cantArchiveWhenUnsavedChanges() throws Exception {
        //todo ttn impl
    }

    @Test
    @Ignore
    public void cantArchiveWhenNotMostRecentVersion() throws Exception {
        //todo ttn impl
    }

    @Test
    @Ignore
    public void deleteDraftWithoutProdVersion() throws Exception {
        //todo ttn impl
    }

    @Test
    @Ignore
    public void deleteDraftWithProdVersion() throws Exception {
        //todo ttn impl
    }

    @Test
    public void metadataInserted() throws Exception {
        metadata.setEncounterStartFromDate( 1L );
        metadata.setEncounterStartToDate( 2L );
        metadata.setEncounterEndFromDate( 3L );
        metadata.setEncounterEndToDate( 4L );
        metadata.setEpisodeOfCareStartFromDate( 5L );
        metadata.setEpisodeOfCareStartToDate( 6L );
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        presenter.save( "commitMessage" );
//        ArgumentCaptor<RuleModel> ruleModelArg = ArgumentCaptor.forClass( RuleModel.class );
//        verify( view ).setContent( any( Path.class ), ruleModelArg.capture(), any( AsyncPackageDataModelOracle.class ), any( Caller.class ), anyBoolean(), anyBoolean() );
//        RuleModel ruleModelArgValue = ruleModelArg.getValue();
        //check that the rule model has been set on content as expected
        verify( view ).setContent( any( Path.class ), same( viewContent ), any( AsyncPackageDataModelOracle.class ), any( Caller.class ), anyBoolean(), anyBoolean() );
        //check right metadata values inserted

        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.RULE_TYPE, String.valueOf( metadata.getRuleType() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.PRODUCTION_DATE, String.valueOf( metadata.getProductionDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.HAS_PROD_VERSION, String.valueOf( metadata.hasProdVersion() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ARCHIVED_DATE, String.valueOf( metadata.getArchivedDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_TEXT, String.valueOf( metadata.getErrorText() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_TYPE, String.valueOf( metadata.getErrorType() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_NUMBER, String.valueOf( metadata.getErrorNumber() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( metadata.isValidForDUSASAbroadReports() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, String.valueOf( metadata.isValidForDUSASSpecialityReports() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_LPR_REPORTS, String.valueOf( metadata.isValidForLPRReports() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_PRIVATE_SECTOR_REPORTS, String.valueOf( metadata.isValidForPrivateSectorReports() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ENCOUNTER_START_FROM_DATE, String.valueOf( metadata.getEncounterStartFromDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ENCOUNTER_START_TO_DATE, String.valueOf( metadata.getEncounterStartToDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ENCOUNTER_END_FROM_DATE, String.valueOf( metadata.getEncounterEndFromDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ENCOUNTER_END_TO_DATE, String.valueOf( metadata.getEncounterEndToDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.EPISODE_OF_CARE_START_FROM_DATE, String.valueOf( metadata.getEpisodeOfCareStartFromDate() ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.EPISODE_OF_CARE_START_TO_DATE, String.valueOf( metadata.getEpisodeOfCareStartToDate() ) ) ) );
    }


    @Test
    public void metadataUpdated() throws Exception {
        long oldErrorNumber = metadata.getErrorNumber();
        String oldErrorText = metadata.getErrorText();
        boolean oldValidForDUSASAbroadReports = metadata.isValidForDUSASAbroadReports();
        LprErrorType oldErrorType = metadata.getErrorType();
        long oldEncounterStartFromDate = metadata.getEncounterStartFromDate();

        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        verify( view ).setContent( any( Path.class ), same( viewContent ), any( AsyncPackageDataModelOracle.class ), any( Caller.class ), anyBoolean(), anyBoolean() );

        presenter.save( "commitMessage1" );
        metadata.setErrorNumber( oldErrorNumber + 1 );
        metadata.setValidForDUSASAbroadReports( !oldValidForDUSASAbroadReports );
        metadata.setErrorText( oldErrorText + "new text" );
        presenter.save( "commitMessage2" );

        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_TYPE, String.valueOf( oldErrorType ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ENCOUNTER_START_FROM_DATE, String.valueOf( oldEncounterStartFromDate ) ) ) );

        assertFalse( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_NUMBER, String.valueOf( oldErrorNumber ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_NUMBER, String.valueOf( oldErrorNumber + 1 ) ) ) );

        assertFalse( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( oldValidForDUSASAbroadReports ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( !oldValidForDUSASAbroadReports ) ) ) );

        assertFalse( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_TEXT, String.valueOf( oldErrorText ) ) ) );
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_TEXT, String.valueOf( oldErrorText + "new text" ) ) ) );
    }

    @Test
    public void metadataDeleted() throws Exception {
        //setup
        metadata.setErrorNumber( 1L );
        metadata.setErrorText( "some text" );
        metadata.setErrorType( LprErrorType.WARN );
        metadata.setEncounterStartFromDate( 123L );
        metadata.setHasProdVersion( false );
        metadata.setValidForDUSASAbroadReports( true );
        viewContent.metadataList = new RuleMetadata[]{
                new RuleMetadata( LprMetadataConsts.ERROR_NUMBER, String.valueOf( metadata.getErrorNumber() ) ),
                new RuleMetadata( LprMetadataConsts.ERROR_TEXT, String.valueOf( metadata.getErrorText() ) ),
                new RuleMetadata( LprMetadataConsts.ERROR_TYPE, String.valueOf( metadata.getErrorType() ) ),
                new RuleMetadata( LprMetadataConsts.ENCOUNTER_START_FROM_DATE, String.valueOf( metadata.getEncounterStartFromDate() ) ),
                new RuleMetadata( LprMetadataConsts.HAS_PROD_VERSION, String.valueOf( metadata.hasProdVersion() ) ),
                new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( metadata.isValidForDUSASAbroadReports() ) )
        };
        presenter.onStartup( mock( ObservablePath.class ), mock( PlaceRequest.class ) ); //initializes presenter (menus, content, etc)
        verify( view ).setContent( any( Path.class ), same( viewContent ), any( AsyncPackageDataModelOracle.class ), any( Caller.class ), anyBoolean(), anyBoolean() );

        //test
        metadata.setErrorNumber( null );
        metadata.setErrorText( null );
        metadata.setEncounterStartFromDate( null );
        presenter.save( "commitMessage" );

        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.HAS_PROD_VERSION, String.valueOf( false ) ) ) ); //same
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.ERROR_TYPE, String.valueOf( LprErrorType.WARN ) ) ) ); //same
        assertTrue( ArrayUtils.contains( viewContent.metadataList, new RuleMetadata( LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, String.valueOf( true ) ) ) ); //same

        for ( RuleMetadata ruleMetadata : viewContent.metadataList ) {
            assertFalse( "error number should be deleted", ruleMetadata.getAttributeName().equalsIgnoreCase( LprMetadataConsts.ERROR_NUMBER ) );
            assertFalse( "error text should be deleted", ruleMetadata.getAttributeName().equalsIgnoreCase( LprMetadataConsts.ERROR_TEXT ) );
            assertFalse( "encounter start from date should be deleted", ruleMetadata.getAttributeName().equalsIgnoreCase( LprMetadataConsts.ENCOUNTER_START_FROM_DATE ) );
        }
    }

    private LPREditor.MoveToProductionCommand getPresenterMoveToProdCommand() {
        LPREditor.MoveToProductionCommand prodCommand = ( LPREditor.MoveToProductionCommand ) (( MenuItemCommand ) presenter.getMenus().getItemsMap().get( MenuItems.MOVETOPRODUCTION )).getCommand();
        //create a spy of prodCommand that skips the popup
        prodCommand = spy( prodCommand );
        when( prodCommand.getShowPopupCommand( any( Command.class ) ) ).thenAnswer( new Answer<Object>() {
            @Override
            public Command answer( InvocationOnMock invocation ) throws Throwable {
                Command popupOkCommand = invocation.getArgumentAt( 0, Command.class );
                //skip the popup and return the okCommand immediately, as if "Ok" button was pressed
                return popupOkCommand;
            }
        } );
        return prodCommand;
    }

    private class EventMock implements Event {

        public EventMock() {
        }

        @Override
        public void fire( Object o ) {
            int x = 0;
        }

        @Override
        public Event select( Annotation... annotations ) {
            return null;
        }

        @Override
        public Event select( Class aClass, Annotation... annotations ) {
            return null;
        }
    }


}