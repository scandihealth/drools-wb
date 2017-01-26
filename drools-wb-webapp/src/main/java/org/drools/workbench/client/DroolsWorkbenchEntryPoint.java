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
package org.drools.workbench.client;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import org.drools.workbench.client.menu.IncomingChangesMenuBuilder;
import org.drools.workbench.client.menu.RecentlyEditedMenuBuilder;
import org.drools.workbench.client.menu.RecentlyViewedMenuBuilder;
import org.drools.workbench.client.menu.WorkbenchViewModeSwitcherUtilityMenuBuilder;
import org.guvnor.common.services.shared.config.AppConfigService;
import org.guvnor.common.services.shared.security.KieWorkbenchACL;
import org.guvnor.common.services.shared.security.KieWorkbenchPolicy;
import org.guvnor.common.services.shared.security.KieWorkbenchSecurityService;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.AfterInitialization;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.client.container.IOCBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.jboss.errai.security.shared.service.AuthenticationService;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.kie.workbench.common.widgets.client.menu.ResetPerspectivesMenuBuilder;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.client.mvp.AbstractWorkbenchPerspectiveActivity;
import org.uberfire.client.mvp.ActivityManager;
import org.uberfire.client.mvp.PerspectiveActivity;
import org.uberfire.client.mvp.PerspectiveManager;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.widgets.menu.UtilityMenuBar;
import org.uberfire.client.workbench.widgets.menu.WorkbenchMenuBarPresenter;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.ForcedPlaceRequest;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.Menus;

/**
 * GWT's Entry-point for Drools Workbench
 */
@EntryPoint
public class DroolsWorkbenchEntryPoint {

    @Inject
    private Caller<AppConfigService> appConfigService;

    @Inject
    private WorkbenchMenuBarPresenter menubar;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private SyncBeanManager iocManager;

    @Inject
    private ActivityManager activityManager;

    @Inject
    private KieWorkbenchACL kieACL;

    @Inject
    private Caller<KieWorkbenchSecurityService> kieSecurityService;

    @Inject
    private Caller<AuthenticationService> authService;

    @Inject
    private UtilityMenuBar utilityMenuBar;

    @Inject
    private PerspectiveManager perspectiveManager;

    @AfterInitialization
    public void startApp() {
        kieSecurityService.call( new RemoteCallback<String>() {
            public void callback( final String str ) {
                KieWorkbenchPolicy policy = new KieWorkbenchPolicy( str );
                kieACL.activatePolicy( policy );
                loadPreferences();
                setupMenu();
                hideLoadingPopup();
            }
        } ).loadPolicy();
    }

    private void loadPreferences() {
        appConfigService.call( new RemoteCallback<Map<String, String>>() {
            @Override
            public void callback( final Map<String, String> response ) {
                ApplicationPreferences.setUp( response );
            }
        } ).loadPreferences();
    }

    private void setupMenu() {
//        final AbstractWorkbenchPerspectiveActivity defaultPerspective = getDefaultPerspectiveActivity();

        final Menus utilityMenus = MenuFactory
                .newTopLevelCustomMenu( iocManager.lookupBean( RecentlyViewedMenuBuilder.class ).getInstance() )
                .endMenu()
                .newTopLevelCustomMenu( iocManager.lookupBean( RecentlyEditedMenuBuilder.class ).getInstance() )
                .endMenu()
                .newTopLevelCustomMenu( iocManager.lookupBean( IncomingChangesMenuBuilder.class ).getInstance() )
                .endMenu()
                .newTopLevelCustomMenu( iocManager.lookupBean( ResetPerspectivesMenuBuilder.class ).getInstance() )
                .endMenu()
                .newTopLevelCustomMenu( iocManager.lookupBean( WorkbenchViewModeSwitcherUtilityMenuBuilder.class ).getInstance() )
                .endMenu()
                .build();

        utilityMenuBar.addMenus( utilityMenus );

        final Menus menus = MenuFactory
                .newTopLevelMenu( "Authoring" ).perspective( "AuthoringPerspective" )
                .endMenu()
                .newTopLevelMenu( "LPR" ).perspective( "LPRPerspective" )
                .endMenu()
                .newTopLevelMenu( CommonConstants.INSTANCE.ResetPerspectivesTooltip() ).respondsWith( getResetPerspectivesCommand() )
                .endMenu()
                .build();

        menubar.addMenus( menus );
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


/*
    private List<MenuItem> getPerspectives() {
        final List<MenuItem> perspectives = new ArrayList<MenuItem>();
        for ( final PerspectiveActivity perspective : getPerspectiveActivities() ) {
            final String name = perspective.getDefaultPerspectiveLayout().getName();
            final MenuItem item = newSimpleItem( name ).perspective( perspective.getIdentifier() ).endMenu().build().getItems().get( 0 );
            perspectives.add( item );
        }

        return perspectives;
    }
*/

    private AbstractWorkbenchPerspectiveActivity getDefaultPerspectiveActivity() {
        AbstractWorkbenchPerspectiveActivity defaultPerspective = null;
        final Collection<IOCBeanDef<AbstractWorkbenchPerspectiveActivity>> perspectives = iocManager.lookupBeans( AbstractWorkbenchPerspectiveActivity.class );
        final Iterator<IOCBeanDef<AbstractWorkbenchPerspectiveActivity>> perspectivesIterator = perspectives.iterator();
        outer_loop:
        while ( perspectivesIterator.hasNext() ) {
            final IOCBeanDef<AbstractWorkbenchPerspectiveActivity> perspective = perspectivesIterator.next();
            final AbstractWorkbenchPerspectiveActivity instance = perspective.getInstance();
            if ( instance.isDefault() ) {
                defaultPerspective = instance;
                break outer_loop;
            } else {
                iocManager.destroyBean( instance );
            }
        }
        return defaultPerspective;
    }

/*
    private List<PerspectiveActivity> getPerspectiveActivities() {

        //Get Perspective Providers
        final Set<PerspectiveActivity> activities = activityManager.getActivities( PerspectiveActivity.class );

        //Sort Perspective Providers so they're always in the same sequence!
        List<PerspectiveActivity> sortedActivities = new ArrayList<PerspectiveActivity>( activities );
        Collections.sort( sortedActivities,
                          new Comparator<PerspectiveActivity>() {

                              @Override
                              public int compare( PerspectiveActivity o1,
                                                  PerspectiveActivity o2 ) {
                                  return o1.getDefaultPerspectiveLayout().getName().compareTo( o2.getDefaultPerspectiveLayout().getName() );
                              }

                          } );

        return sortedActivities;
    }
*/

    //Fade out the "Loading application" pop-up
    private void hideLoadingPopup() {
        final Element e = RootPanel.get( "loading" ).getElement();

        new Animation() {

            @Override
            protected void onUpdate( double progress ) {
                e.getStyle().setOpacity( 1.0 - progress );
            }

            @Override
            protected void onComplete() {
                e.getStyle().setVisibility( Style.Visibility.HIDDEN );
            }
        }.run( 500 );
    }

    public static native void redirect( String url )/*-{
        $wnd.location = url;
    }-*/;

}