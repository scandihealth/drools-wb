package org.drools.workbench.client.docks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.drools.workbench.client.resources.i18n.AppConstants;
import org.guvnor.inbox.client.InboxPresenter;
import org.uberfire.client.workbench.docks.UberfireDock;
import org.uberfire.client.workbench.docks.UberfireDockPosition;
import org.uberfire.client.workbench.docks.UberfireDockReadyEvent;
import org.uberfire.client.workbench.docks.UberfireDocks;
import org.uberfire.client.workbench.widgets.menu.WorkbenchMenuBar;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

@ApplicationScoped
public class LPRWorkbenchDocks {

    @Inject
    private UberfireDocks uberfireDocks;
    private UberfireDock projectExplorerDock;
    private String perspectiveIdentifier;

    @Inject
    private WorkbenchMenuBar menubar;

    public void perspectiveChangeEvent( @Observes UberfireDockReadyEvent dockReadyEvent ) {
        if ( perspectiveIdentifier != null && dockReadyEvent.getCurrentPerspective().equals( perspectiveIdentifier ) ) {
            if ( projectExplorerDock != null ) {
                uberfireDocks.expand( projectExplorerDock );
                menubar.collapse();
            }
        }
    }

    public void setup( String perspectiveIdentifier ) {
        this.perspectiveIdentifier = perspectiveIdentifier;
        DefaultPlaceRequest projectExplorerPlaceRequest = new DefaultPlaceRequest( "org.kie.guvnor.explorer" );
        projectExplorerPlaceRequest.addParameter( "no_context", "true" );
        //todo ttn localize label
        projectExplorerDock = new UberfireDock( UberfireDockPosition.WEST, "ADJUST", projectExplorerPlaceRequest, perspectiveIdentifier ).withSize( 400 ).withLabel( "Project Explorer" );

        DefaultPlaceRequest recentlyViewedPlaceRequest = new DefaultPlaceRequest( "Inbox" );
        recentlyViewedPlaceRequest.addParameter( "inboxname", InboxPresenter.RECENT_VIEWED_ID );
        UberfireDock recentlyViewedDock = new UberfireDock( UberfireDockPosition.SOUTH, "EYE", recentlyViewedPlaceRequest, perspectiveIdentifier ).withSize( 450 ).withLabel( AppConstants.INSTANCE.RecentlyOpened() );

        DefaultPlaceRequest recentlyEditedPlaceRequest = new DefaultPlaceRequest( "Inbox" );
        recentlyEditedPlaceRequest.addParameter( "inboxname", InboxPresenter.RECENT_EDITED_ID );
        UberfireDock recentlyEditedDock = new UberfireDock( UberfireDockPosition.SOUTH, "BOOKMARK", recentlyEditedPlaceRequest, perspectiveIdentifier ).withSize( 450 ).withLabel( AppConstants.INSTANCE.RecentlyEdited() );

        DefaultPlaceRequest incomingChanges = new DefaultPlaceRequest( "Inbox" );
        incomingChanges.addParameter( "inboxname", InboxPresenter.INCOMING_ID );
        UberfireDock incomingChangesDock = new UberfireDock( UberfireDockPosition.SOUTH, "BELL", incomingChanges, perspectiveIdentifier ).withSize( 450 ).withLabel( AppConstants.INSTANCE.IncomingChanges() );

        uberfireDocks.add( projectExplorerDock, recentlyViewedDock, recentlyEditedDock, incomingChangesDock );
    }

}
