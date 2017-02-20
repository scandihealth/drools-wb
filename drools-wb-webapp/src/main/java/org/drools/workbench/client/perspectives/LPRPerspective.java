package org.drools.workbench.client.perspectives;

import java.util.Arrays;
import java.util.Map;
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
        listenForMessageEvents();
        docks.setup( "LPRPerspective" );
    }

    private native void listenForMessageEvents() /*-{
        console.log('Drools-WB listening for messages');
        var that = this;
        $wnd.addEventListener("message", function (event) {
            console.log('Drools-WB message received: ', event.data);
//          todo filter out messages not from our drools-wb application
//          var origin = event.origin || event.originalEvent.origin; // For Chrome, the origin property is in the event.originalEvent object.
//          if (origin !== "http://example.org:8080")
//                return;
            try {
                if (event.data === 'mayClose') {
                    var activityManager = that.@org.drools.workbench.client.perspectives.LPRPerspective::activityManager;
//                    console.log('activityManager', activityManager);
                    var startedActivities = activityManager.@org.uberfire.client.mvp.ActivityManagerImpl::startedActivities;
//                    console.log('startedActivities', startedActivities);
                    var mayClose = that.@org.drools.workbench.client.perspectives.LPRPerspective::mayClose(*)(startedActivities);
                    var message = {message: mayClose, isResponseTo: 'mayClose'};
                    console.log('Drools-WB sending message:', message);
                    event.source.postMessage(message, event.origin);
                }
            } catch (e) {
                console.error(e);
            }
        }, false);
    }-*/;

    private boolean mayClose( Map<Activity, PlaceRequest> startedActivities ) {
        boolean mayClose = true;
        log.trace( "Calling mayClose with startedActivities=" + Arrays.toString( startedActivities.entrySet().toArray() ) );
        for ( Activity activity : startedActivities.keySet() ) {
            log.trace( "Checking Activity: " + activity.getIdentifier() );
            if ( mayClose && activity instanceof WorkbenchActivity ) {
                mayClose = (( WorkbenchActivity ) activity).onMayClose();
                log.trace( "WorkbenchActivity: " + activity.getIdentifier() + " may close: " + mayClose );
            }
        }
        return mayClose;
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
