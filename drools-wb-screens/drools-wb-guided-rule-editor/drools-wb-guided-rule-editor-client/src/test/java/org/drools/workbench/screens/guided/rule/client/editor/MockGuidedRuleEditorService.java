package org.drools.workbench.screens.guided.rule.client.editor;

import java.util.List;

import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.screens.guided.rule.model.GuidedEditorContent;
import org.drools.workbench.screens.guided.rule.service.GuidedRuleEditorService;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.uberfire.backend.vfs.Path;

import static org.mockito.Mockito.*;

/**
 * Created on 06-11-2017.
 */
public class MockGuidedRuleEditorService implements GuidedRuleEditorService {

    private RemoteCallback callback;
    public GuidedEditorContent loadContentCallbackArg;
    public List<ValidationMessage> validateCallbackArg;


    public void setCallback( RemoteCallback remoteCallback ) {
        callback = remoteCallback;
    }

    @Override
    public GuidedEditorContent loadContent( Path path ) {
        callback.callback( loadContentCallbackArg );
        return null;
    }

    @Override
    public Path save( Path path, RuleModel content, Metadata metadata, String comment ) {
        callback.callback( mock( Path.class ) );
        return null;
    }


    @Override
    public List<ValidationMessage> validate( Path path, RuleModel content ) {
        callback.callback( validateCallbackArg );
        return null;
    }


    @Override
    public String toSource( Path path, RuleModel model ) {
        return null;
    }

    @Override
    public Path copy( Path path, String newName, String comment ) {
        return null;
    }

    @Override
    public Path create( Path context, String fileName, RuleModel content, String comment ) {
        return null;
    }

    @Override
    public void delete( Path path, String comment ) {

    }

    @Override
    public RuleModel load( Path path ) {
        return null;
    }

    @Override
    public Path rename( Path path, String newName, String comment ) {
        return null;
    }
}
