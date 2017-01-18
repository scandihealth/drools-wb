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
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

@ApplicationScoped
public class LPRWorkbenchDocks {

    @Inject
    private UberfireDocks uberfireDocks;

    public void setup( String perspectiveIdentifier ) {
        DefaultPlaceRequest recentlyViewedPlaceRequest = new DefaultPlaceRequest( "Inbox" );
        recentlyViewedPlaceRequest.addParameter( "inboxname", InboxPresenter.RECENT_VIEWED_ID );
        UberfireDock recentlyViewedDock = new UberfireDock( UberfireDockPosition.SOUTH, "EYE", recentlyViewedPlaceRequest, perspectiveIdentifier ).withSize( 450 ).withLabel( AppConstants.INSTANCE.RecentlyOpened() );

        DefaultPlaceRequest recentlyEditedPlaceRequest = new DefaultPlaceRequest( "Inbox" );
        recentlyEditedPlaceRequest.addParameter( "inboxname", InboxPresenter.RECENT_EDITED_ID );
        UberfireDock recentlyEditedDock = new UberfireDock( UberfireDockPosition.SOUTH, "BOOKMARK", recentlyEditedPlaceRequest, perspectiveIdentifier ).withSize( 450 ).withLabel( AppConstants.INSTANCE.RecentlyEdited() );

        DefaultPlaceRequest incomingChanges = new DefaultPlaceRequest( "Inbox" );
        incomingChanges.addParameter( "inboxname", InboxPresenter.INCOMING_ID );
        UberfireDock incomingChangesDock = new UberfireDock( UberfireDockPosition.SOUTH, "BELL", incomingChanges, perspectiveIdentifier ).withSize( 450 ).withLabel( AppConstants.INSTANCE.IncomingChanges() );

        uberfireDocks.add( recentlyViewedDock, recentlyEditedDock, incomingChangesDock );
    }

}
