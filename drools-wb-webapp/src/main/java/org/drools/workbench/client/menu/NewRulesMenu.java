/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.workbench.client.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.core.client.Callback;
import org.kie.workbench.common.widgets.client.handlers.lpr.NewRuleHandler;
import org.kie.workbench.common.widgets.client.handlers.lpr.NewRulePresenter;
import org.drools.workbench.screens.drltext.client.handlers.NewDrlTextHandler;
import org.drools.workbench.screens.dtablexls.client.handlers.NewDecisionTableXLSHandler;
import org.drools.workbench.screens.guided.dtable.client.handlers.NewGuidedDecisionTableHandler;
import org.drools.workbench.screens.guided.dtable.client.wizard.NewGuidedDecisionTableWizard;
import org.drools.workbench.screens.guided.dtree.client.handlers.NewGuidedDecisionTreeHandler;
import org.drools.workbench.screens.guided.rule.client.handlers.NewGuidedRuleHandler;
import org.drools.workbench.screens.guided.template.client.handlers.NewGuidedRuleTemplateHandler;
import org.guvnor.common.services.project.context.ProjectContext;
import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.jboss.errai.ioc.client.container.IOCBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.uberfire.mvp.Command;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;

/**
 * A menu to create new rules
 */
@ApplicationScoped
public class NewRulesMenu {

    private SyncBeanManager iocBeanManager;
    private NewRulePresenter newRulePresenter;
    private ProjectContext projectContext;

    private final List<MenuItem> items = new ArrayList<MenuItem>();
    private final Map<NewRuleHandler, MenuItem> newRuleHandlers = new HashMap<NewRuleHandler, MenuItem>();

    public NewRulesMenu() {
        //Zero argument constructor for CDI proxies
    }

    @Inject
    public NewRulesMenu(final SyncBeanManager iocBeanManager,
                        final NewRulePresenter newRulePresenter,
                        final ProjectContext projectContext) {
        this.iocBeanManager = iocBeanManager;
        this.newRulePresenter = newRulePresenter;
        this.projectContext = projectContext;
    }

    private MenuItem projectMenuItem;

    @PostConstruct
    public void setup() {

        addNewRuleHandlers();

        sortMenuItemsByCaption();

        addProjectMenuItem();
    }

    private void addNewRuleHandlers() {
        final Collection<IOCBeanDef<NewRuleHandler>> handlerBeans = iocBeanManager.lookupBeans(NewRuleHandler.class);

        for (final IOCBeanDef<NewRuleHandler> handlerBean : handlerBeans) {
            NewRuleHandler newRuleHandler = handlerBean.getInstance();
            if (isRuleHandler(newRuleHandler)) {
                addMenuItem(newRuleHandler);
            }
        }
    }

    private boolean isRuleHandler(NewRuleHandler newRuleHandler) {
        return newRuleHandler instanceof NewDrlTextHandler
                || newRuleHandler instanceof NewDecisionTableXLSHandler
                || newRuleHandler instanceof NewGuidedDecisionTableHandler
                || newRuleHandler instanceof NewGuidedDecisionTableWizard
                || newRuleHandler instanceof NewGuidedDecisionTreeHandler
                || newRuleHandler instanceof NewGuidedRuleHandler
                || newRuleHandler instanceof NewGuidedRuleTemplateHandler;
    }

    private void addMenuItem(final NewRuleHandler newRuleHandler) {

        if (newRuleHandler.canCreate()) {

            final MenuItem menuItem = getMenuItem(newRuleHandler);

            newRuleHandlers.put(newRuleHandler,
                    menuItem);

            if (isProjectMenuItem(newRuleHandler)) {
                this.projectMenuItem = menuItem;
            } else {
                items.add(menuItem);
            }
        }
    }

    /*
    * We set the project menu item first if it is in.
     */
    private void addProjectMenuItem() {
        if (projectMenuItem != null) {
            items.add(0,
                    projectMenuItem);
        }
    }

    private void sortMenuItemsByCaption() {
        Collections.sort(items,
                new Comparator<MenuItem>() {
                    @Override
                    public int compare(final MenuItem o1,
                                       final MenuItem o2) {
                        return o1.getCaption().compareToIgnoreCase(o2.getCaption());
                    }
                });
    }

    private MenuItem getMenuItem(final NewRuleHandler activeHandler) {
        final String description = activeHandler.getDescription();
        return MenuFactory.newSimpleItem(description).respondsWith(new Command() {
            @Override
            public void execute() {
                final Command command = activeHandler.getCommand( newRulePresenter );
                command.execute();
            }
        }).endMenu().build().getItems().get(0);
    }

    private boolean isProjectMenuItem(final NewRuleHandler activeHandler) {
        return activeHandler.getClass().getName().contains("NewProjectHandler");
    }

    public List<MenuItem> getMenuItems() {
        enableMenuItemsForContext();

        return items;
    }

    public List<MenuItem> getMenuItemsWithoutProject() {
        enableMenuItemsForContext();

        if (projectMenuItem != null && items.contains(projectMenuItem)) {
            return items.subList(1,
                    items.size());
        } else {
            return items;
        }
    }

    @SuppressWarnings("unused")
    public void onProjectContextChanged(@Observes final ProjectContextChangeEvent event) {
        enableMenuItemsForContext();
    }

    void enableMenuItemsForContext() {
        for (Map.Entry<NewRuleHandler, MenuItem> entry : this.newRuleHandlers.entrySet()) {
            final NewRuleHandler handler = entry.getKey();
            final MenuItem menuItem = entry.getValue();

            handler.acceptContext(projectContext,
                    new Callback<Boolean, Void>() {
                        @Override
                        public void onFailure(Void reason) {
                            // Nothing to do there right now.
                        }

                        @Override
                        public void onSuccess(final Boolean result) {
                            if (result != null) {
                                menuItem.setEnabled(result);
                            }
                        }
                    });
        }
    }
}
