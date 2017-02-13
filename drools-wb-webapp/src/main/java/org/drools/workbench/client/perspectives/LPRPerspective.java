package org.drools.workbench.client.perspectives;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.drools.workbench.client.docks.LPRWorkbenchDocks;
import org.drools.workbench.client.resources.i18n.AppConstants;
import org.kie.workbench.common.widgets.client.handlers.lpr.NewRulesMenu;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.panels.impl.MultiListWorkbenchPanelPresenter;
import org.uberfire.mvp.Command;
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
    private PlaceManager placeManager;

    @Inject
    private LPRWorkbenchDocks docks;

    @PostConstruct
    public void setup() {
        docks.setup("LPRPerspective" );
        listenForMessages();
    }

//    public final native void onBeforeUnload() /*-{
//        console.log('unbeforeunload');
//        window.onbeforeunload = function() {return "test";};
//    }-*/;

    public final native void listenForMessages() /*-{
        console.log('Drools-WB listening for messages on ', window.location.href);
        window.addEventListener("message", receiveMessage, false);

        function receiveMessage(event) {
            console.log('Message received: ' + event.data);
            e.source.postMessage('Hello back', event.origin);
        }
    }-*/;


    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl(MultiListWorkbenchPanelPresenter.class.getName());
        perspective.setName("LPR");

        return perspective;
    }

    @WorkbenchMenu
    public Menus buildMenuBar() {
        return MenuFactory
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

}
