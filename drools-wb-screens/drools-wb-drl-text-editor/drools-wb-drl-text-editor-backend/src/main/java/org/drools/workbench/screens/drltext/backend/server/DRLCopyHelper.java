package org.drools.workbench.screens.drltext.backend.server;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.drools.workbench.screens.drltext.type.DRLResourceTypeDefinition;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.backend.service.helper.CopyHelper;
import org.uberfire.io.IOService;

import static org.uberfire.commons.validation.PortablePreconditions.*;

@ApplicationScoped
public class DRLCopyHelper implements CopyHelper {

    private IOService ioService;
    private DRLResourceTypeDefinition drlResourceType;
    private CommentedOptionFactory commentedOptionFactory;

    public DRLCopyHelper() {
    }

    @Inject
    public DRLCopyHelper(@Named("ioStrategy") IOService ioService,
                         DRLResourceTypeDefinition drlResourceType,
                         CommentedOptionFactory commentedOptionFactory) {
        this.ioService = checkNotNull("ioService", ioService);
        this.drlResourceType = checkNotNull("drlResourceType", drlResourceType);
        this.commentedOptionFactory = checkNotNull("commentedOptionFactory", commentedOptionFactory);
    }

    @Override
    public boolean supports(Path destination) {
        return drlResourceType.accept(destination);
    }

    @Override
    public void postProcess(Path source, Path destination) {
        //Load existing file
        org.uberfire.java.nio.file.Path _destination = Paths.convert(destination);
        String drl = ioService.readAllString(_destination);
        //Set LPR metadata
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(LprMetadataConsts.PRODUCTION_DATE, 0L);
        attributes.put(LprMetadataConsts.ARCHIVED_DATE, 0L);
        drl = setDroolsLPRMetadata(drl);

        ioService.write(_destination,
                drl,
                attributes,
                commentedOptionFactory.makeCommentedOption("File [" + source.toURI() + "] copied to [" + destination.toURI() + "]."));
    }

    private String setDroolsLPRMetadata(String drl) {
        String prodDate = StringUtils.substringBetween(drl, LprMetadataConsts.PRODUCTION_DATE + "(", ")");
        String archivedDate = StringUtils.substringBetween(drl, LprMetadataConsts.ARCHIVED_DATE + "(", ")");
        String newDrl = StringUtils.replace(drl, LprMetadataConsts.PRODUCTION_DATE + "(" + prodDate + ")", LprMetadataConsts.PRODUCTION_DATE + "(0)");
        newDrl = StringUtils.replace(newDrl, LprMetadataConsts.ARCHIVED_DATE + "(" + archivedDate + ")", LprMetadataConsts.ARCHIVED_DATE + "(0)");
        return newDrl;
    }
}
