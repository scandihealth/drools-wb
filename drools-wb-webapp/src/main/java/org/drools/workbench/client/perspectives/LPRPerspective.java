package org.drools.workbench.client.perspectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.user.client.Window;
import org.drools.workbench.client.docks.LPRWorkbenchDocks;
import org.drools.workbench.client.resources.i18n.AppConstants;
import org.guvnor.inbox.client.InboxPresenter;
import org.kie.workbench.common.widgets.client.handlers.lpr.NewRulesMenu;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.mvp.PerspectiveActivity;
import org.uberfire.client.mvp.PerspectiveManager;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.panels.impl.MultiListWorkbenchPanelPresenter;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.mvp.impl.ForcedPlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuPosition;
import org.uberfire.workbench.model.menu.Menus;

@ApplicationScoped
@WorkbenchPerspective(identifier = "LPRPerspective", isTransient = false)
public class LPRPerspective {

    @Inject
    private PerspectiveManager perspectiveManager;

    @Inject
    private NewRulesMenu newRulesMenu;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private LPRWorkbenchDocks docks;

    @PostConstruct
    public void setup() {
        docks.setup("LPRPerspective" );
    }

    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl(MultiListWorkbenchPanelPresenter.class.getName());
        perspective.setName("LPR");

        return perspective;
    }

    @WorkbenchMenu
    public Menus buildMenuBar() {
        return MenuFactory
//                .newTopLevelMenu( AppConstants.INSTANCE.Explore())
//                .withItems(getExploreMenuItems())
//                .endMenu()
                .newTopLevelMenu( "Menu")
                .withItems( Arrays.asList(
                        MenuFactory.newSimpleItem( "Reset Perspectives" ).respondsWith(
                                getResetPerspectivesCommand() )
                                .endMenu()
                                .build().getItems().get( 0 )
                ))
                .endMenu()
                .newTopLevelMenu(AppConstants.INSTANCE.New())
                .withItems( newRulesMenu.getMenuItems())
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

    private Command getResetPerspectivesCommand() {
        return new Command() {
            @Override
            public void execute() {
                if ( Window.confirm( CommonConstants.INSTANCE.PromptResetPerspectives() ) ) {
                    final PerspectiveActivity currentPerspective = perspectiveManager.getCurrentPerspective();
                    perspectiveManager.removePerspectiveStates( new Command() {
                        @Override
                        public void execute() {
                            if ( currentPerspective != null ) {
                                //Use ForcedPlaceRequest to force re-loading of the current Perspective
                                final PlaceRequest pr = new ForcedPlaceRequest( currentPerspective.getIdentifier(),
                                        currentPerspective.getPlace().getParameters() );
                                placeManager.goTo( pr );
                            }
                        }
                    } );
                }
            }
        };
    }

    private List<? extends MenuItem> getExploreMenuItems() {
        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        menuItems.add(MenuFactory.newSimpleItem(AppConstants.INSTANCE.IncomingChanges()).respondsWith(
                new Command() {
                    @Override
                    public void execute() {
                        placeManager.goTo("Inbox");
                    }
                }).endMenu().build().getItems().get(0));
        menuItems.add(MenuFactory.newSimpleItem(AppConstants.INSTANCE.RecentlyEdited()).respondsWith(
                new Command() {
                    @Override
                    public void execute() {
                        PlaceRequest p = new DefaultPlaceRequest("Inbox");
                        p.addParameter("inboxname", InboxPresenter.RECENT_EDITED_ID);
                        placeManager.goTo(p);
                    }
                }).endMenu().build().getItems().get(0));
        menuItems.add(MenuFactory.newSimpleItem(AppConstants.INSTANCE.RecentlyOpened()).respondsWith(
                new Command() {
                    @Override
                    public void execute() {
                        PlaceRequest p = new DefaultPlaceRequest("Inbox");
                        p.addParameter("inboxname", InboxPresenter.RECENT_VIEWED_ID);
                        placeManager.goTo(p);
                    }
                }).endMenu().build().getItems().get(0));
        return menuItems;
    }


}
