package org.drools.workbench.screens.guided.rule.client.editor;

import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.services.shared.lpr.LPRManageProductionService;
import org.uberfire.backend.vfs.Path;

import static org.mockito.Mockito.*;

/**
 * Created on 06-11-2017.
 */
public class MockLPRManageProductionService implements LPRManageProductionService {

    public boolean throwError = false;
    private RemoteCallback<Path> callback;
    private ErrorCallback<Message> errorCallback;

    public void setCallbacks( RemoteCallback<Path> remoteCallback, ErrorCallback errorCallback ) {
        callback = remoteCallback;
        this.errorCallback = errorCallback;
    }

    @Override
    public Path copyToProductionBranch( Path sourcePath ) {
        Path mockPath = mock( Path.class );
        if ( throwError )
            errorCallback.error( mock( Message.class ), new RuntimeException() );
        else
            callback.callback( mockPath );
        return mockPath;
    }

    @Override
    public Path deleteFromProductionBranch( Path sourcePath ) {
        Path mockPath = mock( Path.class );
        if ( throwError )
            errorCallback.error( mock( Message.class ), new RuntimeException() );
        else
            callback.callback( mockPath );
        return mockPath;
    }

    @Override
    public Path getProdVersion( Path pathToLatest ) {
        Path mockPath = mock( Path.class );
        if ( throwError )
            errorCallback.error( mock( Message.class ), new RuntimeException() );
        else
            callback.callback( mockPath );
        return mockPath;
    }
}
