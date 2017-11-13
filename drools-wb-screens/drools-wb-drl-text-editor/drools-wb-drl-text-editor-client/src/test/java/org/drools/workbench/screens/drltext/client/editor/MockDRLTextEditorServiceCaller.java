package org.drools.workbench.screens.drltext.client.editor;

import org.drools.workbench.screens.drltext.service.DRLTextEditorService;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;

/**
 * Created on 06-11-2017.
 */
public class MockDRLTextEditorServiceCaller implements Caller<DRLTextEditorService> {

    public MockDRLTextEditorService service = new MockDRLTextEditorService();

    @Override
    public DRLTextEditorService call() {
        return service;
    }

    @Override
    public DRLTextEditorService call( RemoteCallback remoteCallback ) {
        service.setCallback( remoteCallback );
        return service;
    }

    @Override
    public DRLTextEditorService call( RemoteCallback remoteCallback, ErrorCallback errorCallback ) {
        return call( remoteCallback );
    }

}