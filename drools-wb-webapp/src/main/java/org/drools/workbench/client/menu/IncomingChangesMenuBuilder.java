package org.drools.workbench.client.menu;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.client.resources.i18n.AppConstants;
import org.guvnor.inbox.client.InboxPresenter;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuPosition;
import org.uberfire.workbench.model.menu.impl.BaseMenuCustom;

@ApplicationScoped
public class IncomingChangesMenuBuilder implements MenuFactory.CustomMenuBuilder {

    @Inject
    private PlaceManager placeManager;

    private AnchorListItem link = new AnchorListItem();

    public IncomingChangesMenuBuilder() {
        link.setIcon( IconType.BELL );
        link.setTitle( AppConstants.INSTANCE.IncomingChanges() );
        link.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                DefaultPlaceRequest placeRequest = new DefaultPlaceRequest( "Inbox" );
                placeRequest.addParameter( "inboxname", InboxPresenter.INCOMING_ID );
                placeManager.goTo( placeRequest );
            }
        } );

    }

    @Override
    public void push( final MenuFactory.CustomMenuBuilder element ) {
        //Do nothing
    }

    @Override
    public MenuItem build() {
        return new BaseMenuCustom<IsWidget>() {
            @Override
            public IsWidget build() {
                return link;
            }

            @Override
            public MenuPosition getPosition() {
                return MenuPosition.RIGHT;
            }
        };
    }
}
