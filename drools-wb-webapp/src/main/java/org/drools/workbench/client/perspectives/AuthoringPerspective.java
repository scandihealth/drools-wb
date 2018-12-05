/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.drools.workbench.client.perspectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.drools.workbench.client.docks.AuthoringWorkbenchDocks;
import org.drools.workbench.client.resources.i18n.AppConstants;
import org.guvnor.inbox.client.InboxPresenter;
import org.guvnor.messageconsole.client.console.resources.MessageConsoleResources;
import org.kie.workbench.common.screens.projecteditor.client.menu.ProjectMenu;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;
import org.kie.workbench.common.widgets.client.handlers.NewResourcesMenu;
import org.kie.workbench.common.widgets.client.menu.RepositoryMenu;
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
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuPosition;
import org.uberfire.workbench.model.menu.Menus;

/**
 * A Perspective for Rule authors. Note the @WorkbenchPerspective has the same identifier as kie-drools-wb
 * since org.kie.workbench.common.screens.projecteditor.client.messages.ProblemsService "white-lists" a
 * set of Perspectives for which to show the Problems Panel
 */
@ApplicationScoped
@WorkbenchPerspective(identifier = "AuthoringPerspective", isTransient = false)
public class AuthoringPerspective {

    @Inject
    private NewResourcePresenter newResourcePresenter;

    @Inject
    private NewResourcesMenu newResourcesMenu;

    @Inject
    private ProjectMenu projectMenu;

    @Inject
    private RepositoryMenu repositoryMenu;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private AuthoringWorkbenchDocks docks;

    @Inject
    private ActivityManager activityManager;

    private static final Logger log = LoggerFactory.getLogger( AuthoringPerspective.class );

    @PostConstruct
    public void setup() {
        listenForMessageEvents();
        docks.setup("AuthoringPerspective", new DefaultPlaceRequest( "org.kie.guvnor.explorer" ) );
    }

    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl(MultiListWorkbenchPanelPresenter.class.getName());
        perspective.setName("Author");

        return perspective;
    }

    @WorkbenchMenu
    public Menus buildMenuBar() {
        return MenuFactory
                .newTopLevelMenu(AppConstants.INSTANCE.Explore())
                .withItems(getExploreMenuItems())
                .endMenu()
                .newTopLevelMenu(AppConstants.INSTANCE.New())
                .withItems(newResourcesMenu.getMenuItems())
                .endMenu()
                .newTopLevelMenu(AppConstants.INSTANCE.Project())
                .withItems(projectMenu.getMenuItems())
                .endMenu()
                .newTopLevelMenu(AppConstants.INSTANCE.Repository())
                .withItems(repositoryMenu.getMenuItems())
                .endMenu()
                .newTopLevelMenu( MessageConsoleResources.CONSTANTS.MessageConsole() ).position( MenuPosition.RIGHT ).respondsWith( new Command() {
                    @Override
                    public void execute() {
                        placeManager.goTo( "org.kie.workbench.common.screens.messageconsole.MessageConsole" );
                    }
                } )
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

    private List<? extends MenuItem> getExploreMenuItems() {
        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        menuItems.add(MenuFactory.newSimpleItem(AppConstants.INSTANCE.Projects()).respondsWith(
                new Command() {
                    @Override
                    public void execute() {
                        placeManager.goTo("org.kie.guvnor.explorer");
                    }
                }).endMenu().build().getItems().get(0));
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

    private native void listenForMessageEvents() /*-{
        console.log('Drools-WB listening for messages');
        var that = this;
        $wnd.addEventListener("message", function (event) {
            console.log('Drools-WB message received: ', event.data);
            try {
                if (event.data === 'mayClose') {
                    var activityManager = that.@org.drools.workbench.client.perspectives.AuthoringPerspective::activityManager;
                    var startedActivities = activityManager.@org.uberfire.client.mvp.ActivityManagerImpl::startedActivities;
                    var mayClose = that.@org.drools.workbench.client.perspectives.AuthoringPerspective::mayClose(*)(startedActivities);
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

}
