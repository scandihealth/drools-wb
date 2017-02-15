package org.drools.workbench.client.perspectives;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.drools.workbench.client.docks.LPRWorkbenchDocks;
import org.drools.workbench.client.resources.i18n.AppConstants;
import org.kie.workbench.common.widgets.client.handlers.lpr.NewRulesMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.mvp.Activity;
import org.uberfire.client.mvp.ActivityManager;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.WorkbenchActivity;
import org.uberfire.client.workbench.panels.impl.MultiListWorkbenchPanelPresenter;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuPosition;
import org.uberfire.workbench.model.menu.Menus;

@ApplicationScoped
@WorkbenchPerspective(identifier = "LPRPerspective", isTransient = false)
public class LPRPerspective {

    @Inject
    private NewRulesMenu newRulesMenu;

    @Inject
    private ActivityManager activityManager;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private LPRWorkbenchDocks docks;

    private static final Logger log = LoggerFactory.getLogger( LPRPerspective.class );

    @PostConstruct
    public void setup() {
        docks.setup( "LPRPerspective" );
        listenForPostMessage();
    }

//    public final native void onBeforeUnload() /*-{
//        console.log('unbeforeunload');
//        window.onbeforeunload = function() {return "test";};
//    }-*/;

    private native void listenForPostMessage() /*-{
        console.log('Drools-WB listening for messages');
        var that = this;
        $wnd.addEventListener("message", function (event) {
//        $wnd.setTimeout(function () {
            console.log('Drools-WB message received: ', event.data);
//          todo filter out messages not from our drools-wb application
//          var origin = event.origin || event.originalEvent.origin; // For Chrome, the origin property is in the event.originalEvent object.
//          if (origin !== "http://example.org:8080")
//                return;
            try {
                if (event.data === 'isDirty') {
                    var activityManager = that.@org.drools.workbench.client.perspectives.LPRPerspective::activityManager;
//                    console.log('activityManager', activityManager);
                    var startedActivities = activityManager.@org.uberfire.client.mvp.ActivityManagerImpl::startedActivities;
//                    console.log('startedActivities', startedActivities);
                    var dirty = that.@org.drools.workbench.client.perspectives.LPRPerspective::isDirty(*)(startedActivities);
                    var message = {message: dirty, isResponseTo: 'isDirty'};
                    console.log('Drools-WB sending message:', message);
                    event.source.postMessage(message, event.origin);
                }
            } catch (e) {
                console.error(e);
            }
//        }, 10000);
        }, false);
    }-*/;

    private boolean isDirty( Map<Activity, PlaceRequest> startedActivities ) {
        boolean dirty = false;
        log.info( "Calling isDirty with startedActivities=" + Arrays.toString( startedActivities.entrySet().toArray() ) );
        for ( Activity activity : startedActivities.keySet() ) {
            log.info( "Checking Activity: " + activity.getIdentifier() );
            if ( !dirty && activity instanceof WorkbenchActivity ) {
                dirty = !(( WorkbenchActivity ) activity).onMayClose();
                log.info( "WorkbenchActivity: " + activity.getIdentifier() + " is " + dirty + " dirty" );
            }
        }
        return dirty;
    }

    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl( MultiListWorkbenchPanelPresenter.class.getName() );
        perspective.setName( "LPR" );

        return perspective;
    }

    @WorkbenchMenu
    public Menus buildMenuBar() {
        return MenuFactory
                .newTopLevelMenu( AppConstants.INSTANCE.New() )
                .withItems( newRulesMenu.getMenuItems() )
                .endMenu()
                .newTopLevelMenu( AppConstants.INSTANCE.assetSearch() ).position( MenuPosition.RIGHT ).respondsWith( new Command() {
                    @Override
                    public void execute() {
                        placeManager.goTo( "FindForm" );
                    }
                } )
                .endMenu()
                .build();
    }

}
