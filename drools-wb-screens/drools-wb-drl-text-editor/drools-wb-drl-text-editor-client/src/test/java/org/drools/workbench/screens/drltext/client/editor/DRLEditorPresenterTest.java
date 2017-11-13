package org.drools.workbench.screens.drltext.client.editor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.event.Event;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.guvnor.common.services.shared.metadata.model.LprRuleType;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.kie.workbench.common.widgets.metadata.client.KieEditorWrapperView;
import org.kie.workbench.common.widgets.metadata.client.lpr.LPRFileMenuBuilder;
import org.kie.workbench.common.widgets.metadata.client.widget.OverviewWidgetPresenter;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.workbench.events.NotificationEvent;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

/**
 * Created on 06-11-2017.
 */
@RunWith(GwtMockitoTestRunner.class)
public class DRLEditorPresenterTest {

    @Mock
    private DRLEditorView view;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private VersionRecordManager versionRecordManager;

    @Mock
    @SuppressWarnings("unused") //injected into DRLEditorPresenter via @InjectMocks
    private OverviewWidgetPresenter overviewWidget;

    @Spy
    @SuppressWarnings("unused") //injected into DRLEditorPresenter via @InjectMocks
    private LPRFileMenuBuilder lprMenuBuilder;

    @Mock
    @SuppressWarnings("unused") //injected into DRLEditorPresenter via @InjectMocks
    private KieEditorWrapperView kieView;

    @Spy
    private MockDRLTextEditorServiceCaller drlTextEditorServiceCaller;

    @Spy
    private EventMock eventMock;

    @Spy
    @InjectMocks
    private DRLEditorPresenter drlEditorPresenter;

    private String simpleRule = "rule \"No dialect\"\n" +
            "\twhen\n" +
            "\tthen\n" +
            "end";

    private String simpleRuleWithMetadata = "rule \"No dialect\"\n" +
            "\t@lprmeta.archivedDate(0)\n" +
            "\t@lprmeta.productionDate(0)\n" +
            "\t@lprmeta.hasProdVersion(false)\n" +
            "\t@lprmeta.isValidForPrimarySectorReports(false)\n" +
            "\t@lprmeta.isValidForLPRReports(false)\n" +
            "\t@lprmeta.isValidForDUSASSpecialityReports(false)\n" +
            "\t@lprmeta.isValidForDUSASAbroadReports(false)\n" +
            "\t@lprmeta.errorNumber(1)\n" +
            "\t@lprmeta.errorText(error text)\n" +
            "\t@lprmeta.errorType(ERROR)\n" +
            "\t@lprmeta.type(REPORT_VALIDATION)\n" +
            "\twhen\n" +
            "\tthen\n" +
            "end";

    private String drlFileWithPackageImportDialect = "package sds.lpr;\n" +
            "\n" +
            "import com.dxc.lpr3.domain.Kontakt;\n" +
            "\n" +
            "rule \"With Dialect\"\n" +
            "\tdialect \"mvel\"\n" +
            "\twhen\n" +
            "\t\tKontakt( )\n" +
            "\tthen\n" +
            "end";

    private String drlFileWithoutRule = "package sds.lpr;\n" +
            "\n" +
            "import com.dxc.lpr3.domain.Kontakt;\n";

    private Metadata metadata;

    private String viewDrlContent = "";

    @Before
    public void setUp() throws Exception {
        ApplicationPreferences.setUp( new HashMap<String, String>() {{
            put( ApplicationPreferences.DATE_FORMAT,
                    "dd-MM-yyyy" );
        }} );

        metadata = new Metadata();
        metadata.setRuleType( LprRuleType.REPORT_VALIDATION );
        metadata.setProductionDate( 0L );
        metadata.setHasProdVersion( false );
        metadata.setArchivedDate( 0L );
        metadata.setErrorType( LprErrorType.ERROR );
        metadata.setErrorNumber( 1L );
        metadata.setErrorText( "error text" );
        drlEditorPresenter.setMetadata( metadata );

        //create mock methods on view so when setContent is called, its input is returned when getContent is called
        when( view.getContent() ).thenAnswer( new Answer<String>() {
            @Override
            public String answer( InvocationOnMock invocation ) throws Throwable {
                return viewDrlContent;
            }
        } );
        doAnswer( new Answer() {
            @Override
            public Object answer( InvocationOnMock invocation ) throws Throwable {
                viewDrlContent = ( String ) invocation.getArguments()[0];
                return null;
            }
        } ).when( view ).setContent( anyString() );
    }

    @Test
    public void saveDraft() throws Exception {
        view.setContent( simpleRule );
        drlEditorPresenter.save( "commitMessage" );

        //BaseEditor#getSaveSuccessCallback
        verify( view ).hideBusyIndicator();
        verify( versionRecordManager ).reloadVersions( any( Path.class ) );
        verify( eventMock, atLeastOnce() ).fire( any( NotificationEvent.class ) );
        verify( drlEditorPresenter ).setOriginalHash( not( eq( simpleRule.hashCode() ) ) ); //hash should not be the old drl without metadata
        verify( drlEditorPresenter ).setOriginalHash( eq( view.getContent().hashCode() ) ); //hash should be changed to new drl (that includes metadata)

        //LPREditor#getSaveSuccessCallback
        verify( eventMock, atLeastOnce() ).fire( any( ChangeTitleWidgetEvent.class ) );
        verify( view, atLeastOnce() ).refreshTitle( contains( "Kladde" ) );
        verify( drlEditorPresenter, never() ).reload(); //editor should not reload since it is not archived
    }

    @Test
    public void metadataNotInsertedInFileWithoutRule() throws Exception {
        //test that metadata is not inserted in file without a rule
        viewDrlContent = drlFileWithoutRule;
        drlEditorPresenter.save( null );
        verify( view ).setContent( eq( drlFileWithoutRule ) );
    }

    @Test
    public void metadataInserted() throws Exception {
        viewDrlContent = drlFileWithPackageImportDialect;
        drlEditorPresenter.save( "commitMessage" );
        ArgumentCaptor<String> drlCaptor = ArgumentCaptor.forClass( String.class );
        verify( view ).setContent( drlCaptor.capture() );
        String capturedDrl = drlCaptor.getValue();
        //check right metadata values inserted
        assertTrue( capturedDrl.contains( "@" + LprMetadataConsts.RULE_TYPE + "(" + metadata.getRuleType() + ")" ) );
        assertTrue( capturedDrl.contains( "@" + LprMetadataConsts.PRODUCTION_DATE + "(" + metadata.getProductionDate() + ")" ) );
        assertTrue( capturedDrl.contains( "@" + LprMetadataConsts.HAS_PROD_VERSION + "(" + metadata.hasProdVersion() + ")" ) );
        assertTrue( capturedDrl.contains( "@" + LprMetadataConsts.ARCHIVED_DATE + "(" + metadata.getArchivedDate() + ")" ) );
        assertTrue( capturedDrl.contains( "@" + LprMetadataConsts.ERROR_TYPE + "(" + metadata.getErrorType() + ")" ) );
        assertTrue( capturedDrl.contains( "@" + LprMetadataConsts.ERROR_NUMBER + "(" + metadata.getErrorNumber() + ")" ) );
    }

    @Test
    public void metadataUpdated() throws Exception {
        viewDrlContent = simpleRuleWithMetadata;
        metadata.setErrorNumber( 2L );
        metadata.setValidForDUSASAbroadReports( true );
        metadata.setErrorText( "new text" );
        drlEditorPresenter.save( "commitMessage" );
        verify( view ).setContent( contains( "@lprmeta.hasProdVersion(false)" ) ); //same
        verify( view ).setContent( contains( "@lprmeta.errorType(ERROR)" ) ); //same

        verify( view ).setContent( not( contains( "@lprmeta.errorNumber(1)" ) ) ); //old value
        verify( view ).setContent( contains( "@lprmeta.errorNumber(2)" ) ); //new long value

        verify( view ).setContent( not( contains( "@lprmeta.isValidForDUSASAbroadReports(false)" ) ) ); //old value
        verify( view ).setContent( contains( "@lprmeta.isValidForDUSASAbroadReports(true)" ) ); //new boolean value

        verify( view ).setContent( not( contains( "@lprmeta.errorText(error text)" ) ) ); //old value
        verify( view ).setContent( contains( "@lprmeta.errorText(new text)" ) ); //new string value
    }

    @Test
    public void metadataDeleted() throws Exception {
        metadata = new Metadata();
        metadata.setProductionDate( 0L );
        metadata.setHasProdVersion( false );
        metadata.setArchivedDate( 0L );
        metadata.setErrorType( LprErrorType.ERROR );
        drlEditorPresenter.setMetadata( metadata );

        viewDrlContent = simpleRuleWithMetadata;
        drlEditorPresenter.save( "commitMessage" );
        verify( view ).setContent( contains( "@lprmeta.hasProdVersion(false)" ) ); //same
        verify( view ).setContent( contains( "@lprmeta.errorType(ERROR)" ) ); //same
        verify( view ).setContent( not( contains( "@lprmeta.errorNumber" ) ) ); //deleted
        verify( view ).setContent( contains( "@lprmeta.isValidForDUSASAbroadReports(false)" ) ); //default value when metadata not present (due to primitive type)
        verify( view ).setContent( not( contains( "@lprmeta.errorText" ) ) ); //deleted

    }

    @Test
    public void metadataInsertedAfterRule() throws Exception {
        viewDrlContent = simpleRule;
        drlEditorPresenter.save( "commitMessage" );
        verify( view ).setContent( find( "rule\\s+[\"'].*[\"']\\s+@" + LprMetadataConsts.LPRMETA ) );
    }

    @Test
    public void metadataInsertedBeforeDialect() throws Exception {
        viewDrlContent = drlFileWithPackageImportDialect;
        drlEditorPresenter.save( "commitMessage" );
        verify( view ).setContent( find( "@" + LprMetadataConsts.LPRMETA + ".+\\s+dialect \"mvel\"" ) );
    }

    @Test
    public void metadataInsertedBeforeWhen() throws Exception {
        viewDrlContent = simpleRule;
        drlEditorPresenter.save( "commitMessage" );
        verify( view ).setContent( find( "@" + LprMetadataConsts.LPRMETA + ".+\\s+when" ) );
    }

    @Test
    public void metadataInsertedForAllRulesInDRLFile() throws Exception {
        viewDrlContent = drlFileWithPackageImportDialect + "\n" + simpleRule; //2 rules in 1 file
        drlEditorPresenter.save( "commitMessage" );
        ArgumentCaptor<String> drlCaptor = ArgumentCaptor.forClass( String.class );
        verify( view ).setContent( drlCaptor.capture() );
        String capturedDrl = drlCaptor.getValue();
        //test that a metadata has been inserted twice
        Pattern pattern = Pattern.compile( LprMetadataConsts.RULE_TYPE );
        Matcher matcher = pattern.matcher( capturedDrl );
        int matches = 0;
        while ( matcher.find() ) {
            matches++;
        }
        assertEquals( 2, matches );

        //(test that lprmeta.type is inserted once before dialect, and once after the first when and before second when)
        verify( view ).setContent( find( "@" + LprMetadataConsts.LPRMETA + ".+\\s+dialect \"mvel\"" ) );
        verify( view ).setContent( find( "@" + LprMetadataConsts.LPRMETA + ".+\\s+when" ) );
    }

    private class EventMock implements Event {
        public EventMock() {
        }

        @Override
        public void fire( Object o ) {

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