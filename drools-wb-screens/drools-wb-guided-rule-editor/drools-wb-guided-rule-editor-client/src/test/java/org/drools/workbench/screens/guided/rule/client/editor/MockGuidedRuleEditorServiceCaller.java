package org.drools.workbench.screens.guided.rule.client.editor;

import java.util.List;

import org.drools.workbench.screens.guided.rule.model.GuidedEditorContent;
import org.drools.workbench.screens.guided.rule.service.GuidedRuleEditorService;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;

/**
 * Created on 06-11-2017.
 */
public class MockGuidedRuleEditorServiceCaller implements Caller<GuidedRuleEditorService> {

    public MockGuidedRuleEditorService service = new MockGuidedRuleEditorService();

    public MockGuidedRuleEditorServiceCaller( GuidedEditorContent loadContentCallbackArg, List<ValidationMessage> validateCallbackArg ) {
        service.loadContentCallbackArg = loadContentCallbackArg;
        service.validateCallbackArg = validateCallbackArg;
    }

    @Override
    public GuidedRuleEditorService call() {
        return service;
    }

    @Override
    public GuidedRuleEditorService call( RemoteCallback remoteCallback ) {
        service.setCallback( remoteCallback );
        return service;
    }

    @Override
    public GuidedRuleEditorService call( RemoteCallback remoteCallback, ErrorCallback errorCallback ) {
        return call( remoteCallback );
    }

}