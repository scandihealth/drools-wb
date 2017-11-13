package org.drools.workbench.screens.drltext.client.editor;

import java.util.List;

import org.drools.workbench.screens.drltext.model.DrlModelContent;
import org.drools.workbench.screens.drltext.service.DRLTextEditorService;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.uberfire.backend.vfs.Path;

import static org.mockito.Mockito.*;

/**
 * Created on 06-11-2017.
 */
public class MockDRLTextEditorService implements DRLTextEditorService {

    private RemoteCallback callback;
    private DrlModelContent loadContentCallbackArg;


    public void setCallback( RemoteCallback remoteCallback ) {
        callback = remoteCallback;
    }

    @Override
    public DrlModelContent loadContent( Path path ) {
        callback.callback( loadContentCallbackArg );
        return null;
    }

    public void setLoadContentCallbackArg( DrlModelContent drlModelContent ) {
        this.loadContentCallbackArg = drlModelContent;
    }

    @Override
    public List<String> loadClassFields( Path path, String fullyQualifiedClassName ) {
        return null;
    }

    @Override
    public String assertPackageName( String drl, Path resource ) {
        return null;
    }

    @Override
    public Path save( Path path, String content, Metadata metadata, String comment ) {
        callback.callback( mock( Path.class ) );
        return null;
    }

    @Override
    public List<ValidationMessage> validate( Path path, String content ) {
        return null;
    }

    @Override
    public Path copy( Path path, String newName, String comment ) {
        return null;
    }

    @Override
    public Path create( Path context, String fileName, String content, String comment ) {
        return null;
    }

    @Override
    public void delete( Path path, String comment ) {

    }

    @Override
    public String load( Path path ) {
        return null;
    }

    @Override
    public Path rename( Path path, String newName, String comment ) {
        return null;
    }
}
