package org.drools.workbench.screens.guided.rule.client.editor;

import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.services.shared.lpr.LPRManageProductionService;

import static org.mockito.Mockito.*;

/**
 * Created on 06-11-2017.
 */
public class MockLPRManageProductionServiceCaller implements Caller<LPRManageProductionService> {

    public MockLPRManageProductionService service = spy( new MockLPRManageProductionService() );

    @Override
    public MockLPRManageProductionService call() {
        return service;
    }

    @Override
    public MockLPRManageProductionService call( RemoteCallback remoteCallback ) {
        service.setCallbacks( remoteCallback, null );
        return service;
    }

    @Override
    public MockLPRManageProductionService call( RemoteCallback remoteCallback, ErrorCallback errorCallback ) {
        service.setCallbacks( remoteCallback, errorCallback );
        return service;

    }

}