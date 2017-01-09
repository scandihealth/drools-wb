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
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuPosition;
import org.uberfire.workbench.model.menu.impl.BaseMenuCustom;

@ApplicationScoped
public class RecentlyViewedMenuBuilder implements MenuFactory.CustomMenuBuilder {

    @Inject
    private PlaceManager placeManager;

    private AnchorListItem link = new AnchorListItem();

    public RecentlyViewedMenuBuilder() {
        link.setIcon( IconType.EYE );
        link.setTitle( AppConstants.INSTANCE.RecentlyOpened() );
        link.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                DefaultPlaceRequest recentlyViewedPlaceRequest = new DefaultPlaceRequest( "Inbox" );
                recentlyViewedPlaceRequest.addParameter( "inboxname", InboxPresenter.RECENT_VIEWED_ID );
                placeManager.goTo( recentlyViewedPlaceRequest );
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
