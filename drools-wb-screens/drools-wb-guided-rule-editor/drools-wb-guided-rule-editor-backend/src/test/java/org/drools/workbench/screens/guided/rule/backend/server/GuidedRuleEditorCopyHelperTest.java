/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.drools.workbench.screens.guided.rule.backend.server;

import java.util.HashMap;
import java.util.Map;

import org.drools.workbench.models.commons.backend.oracle.PackageDataModelOracleImpl;
import org.drools.workbench.screens.guided.rule.type.GuidedRuleDRLResourceTypeDefinition;
import org.drools.workbench.screens.guided.rule.type.GuidedRuleDSLRResourceTypeDefinition;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.datamodel.backend.server.service.DataModelService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.vfs.Path;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.base.options.CommentedOption;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GuidedRuleEditorCopyHelperTest {

    @Mock
    private IOService ioService;

    @Mock
    private GuidedRuleEditorServiceUtilities utilities;

    @Mock
    private CommentedOptionFactory commentedOptionFactory;

    @Mock
    private DataModelService dataModelService;

    private GuidedRuleEditorCopyHelper helper;
    private GuidedRuleDRLResourceTypeDefinition drlResourceType = new GuidedRuleDRLResourceTypeDefinition();
    private GuidedRuleDSLRResourceTypeDefinition dslrResourceType = new GuidedRuleDSLRResourceTypeDefinition();

    private final String drl = "rule \"rule\"\n" +
            "@lprmeta.hasProdVersion(true)\n" +
            "@lprmeta.productionDate(1)\n" +
            "@lprmeta.archivedDate(2)\n" +
            "@lprmeta.errorNumber(3)\n" +
            "when\n" +
            "$p : Person()\n" +
            "then\n" +
            "modify( $p ) {\n" +
            "}\n" +
            "end";

    private final String dslr = "rule \"rule\"\n" +
            "@lprmeta.hasProdVersion(true)\n" +
            "@lprmeta.productionDate(1)\n" +
            "@lprmeta.archivedDate(2)\n" +
            "@lprmeta.errorNumber(3)\n" +
            "when\n" +
            ">$p : Person()\n" +
            "then\n" +
            ">modify( $p ) {\n" +
            ">}\n" +
            "end";

    private String[] dsls = new String[]{"There is a person=Person()"};

    private Map<String, Object> attributes = new HashMap<String, Object>() {{
        put( LprMetadataConsts.HAS_PROD_VERSION, true );
        put(LprMetadataConsts.PRODUCTION_DATE, 1L);
        put(LprMetadataConsts.ARCHIVED_DATE, 2L);
        put(LprMetadataConsts.ERROR_NUMBER, 3L);
    }};

    @Before
    public void setup() {
        helper = new GuidedRuleEditorCopyHelper(ioService,
                drlResourceType,
                dslrResourceType,
                utilities,
                commentedOptionFactory,
                dataModelService);
        when(utilities.loadDslsForPackage(any(Path.class))).thenReturn(dsls);
        when(dataModelService.getDataModel(any(Path.class))).thenReturn(new PackageDataModelOracleImpl());
        when(ioService.readAttributes(any(org.uberfire.java.nio.file.Path.class))).thenReturn(attributes);
    }

    @Test
    public void testRDRLFile() {
        final Path pathSource = mock(Path.class);
        final Path pathDestination = mock(Path.class);
        when(pathSource.toURI()).thenReturn("default://p0/src/main/resources/MyFile.rdrl");
        when(pathDestination.toURI()).thenReturn("default://p0/src/main/resources/MyNewFile.rdrl");
        when(pathDestination.getFileName()).thenReturn("MyNewFile.rdrl");
        when(ioService.readAllString(any(org.uberfire.java.nio.file.Path.class))).thenReturn(drl);

        helper.postProcess(pathSource,
                pathDestination);

        final ArgumentCaptor<String> drlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Map> attributesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        //noinspection unchecked
        verify(ioService,
                times(1)).write(any(org.uberfire.java.nio.file.Path.class),
                drlArgumentCaptor.capture(),
                attributesArgumentCaptor.capture(),
                any(CommentedOption.class));

        final String newDrl = drlArgumentCaptor.getValue();
        assertNotNull(newDrl);
        assertTrue(newDrl.contains("MyNewFile"));

        //test that rule is copied with LPR draft status (and preserving other LPR attributes)
        assertEquals(false, attributesArgumentCaptor.getValue().get(LprMetadataConsts.HAS_PROD_VERSION));
        assertEquals(0L, attributesArgumentCaptor.getValue().get(LprMetadataConsts.PRODUCTION_DATE));
        assertEquals(0L, attributesArgumentCaptor.getValue().get(LprMetadataConsts.ARCHIVED_DATE));
        assertEquals(3, attributesArgumentCaptor.getValue().size()); //should not write any other attributes
        assertTrue(newDrl.contains("@" + LprMetadataConsts.HAS_PROD_VERSION + "(false)"));
        assertTrue(newDrl.contains("@" + LprMetadataConsts.PRODUCTION_DATE + "(0)"));
        assertTrue(newDrl.contains("@" + LprMetadataConsts.ARCHIVED_DATE + "(0)"));
        assertTrue(newDrl.contains("@" + LprMetadataConsts.ERROR_NUMBER + "(3)"));
    }

    @Test
    public void testRDSLRFile() {
        final Path pathSource = mock(Path.class);
        final Path pathDestination = mock(Path.class);
        when(pathSource.toURI()).thenReturn("default://p0/src/main/resources/MyFile.rdslr");
        when(pathDestination.toURI()).thenReturn("default://p0/src/main/resources/MyNewFile.rdslr");
        when(pathDestination.getFileName()).thenReturn("MyNewFile.rdslr");
        when(ioService.readAllString(any(org.uberfire.java.nio.file.Path.class))).thenReturn(dslr);

        helper.postProcess(pathSource,
                pathDestination);

        final ArgumentCaptor<String> drlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(ioService,
                times(1)).write(any(org.uberfire.java.nio.file.Path.class),
                drlArgumentCaptor.capture(),
                not(eq(attributes)),
                any(CommentedOption.class));

        final String newDrl = drlArgumentCaptor.getValue();
        assertNotNull(newDrl);
        assertTrue(newDrl.contains("MyNewFile"));
    }
}
