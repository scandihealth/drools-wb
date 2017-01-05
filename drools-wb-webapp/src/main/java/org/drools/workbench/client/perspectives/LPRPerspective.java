package org.drools.workbench.client.perspectives;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.drools.workbench.client.docks.LPRWorkbenchDocks;
import org.kie.workbench.common.widgets.client.handlers.NewResourcesMenu;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.workbench.panels.impl.MultiListWorkbenchPanelPresenter;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;

@ApplicationScoped
@WorkbenchPerspective(identifier = "LPRPerspective", isTransient = false)
public class LPRPerspective {

    @Inject
    private NewResourcesMenu newResourcesMenu;

    @Inject
    private LPRWorkbenchDocks docks;

    @PostConstruct
    public void setup() {
        docks.setup("LPRPerspective", new DefaultPlaceRequest( "FindForm" ) );
    }

    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl(MultiListWorkbenchPanelPresenter.class.getName());
        perspective.setName("LPR");

        return perspective;
    }


}
