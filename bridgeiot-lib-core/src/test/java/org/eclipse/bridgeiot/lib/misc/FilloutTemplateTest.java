/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Bosch Software Innovations GmbH, Robert Bosch GmbH, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Denis Kramer     (Bosch Software Innovations GmbH)
 *    Stefan Schmid    (Robert Bosch GmbH)
 *    Andreas Ziller   (Siemens AG)
 */
package org.eclipse.bridgeiot.lib.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FilloutTemplateTest {

    static final String NO_REPLACE_TEST_TEMPLATE = "nothing: { here: toReplace}";
    static final String DEFAULT_TEST_TEMPLATE = "test: { with: { prop1: %%valueProp1, prop2: %%valueProp2 }, and: %%valueProp3 }";

    static final String PROP_VAL1 = "valueProp1";
    static final String PROP_VAL2 = "valueProp2";
    static final String PROP_VAL3 = "valueProp3";

    FilloutTemplate template;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void initialize() {

    }

    @Test
    public void createTemplateFilloutDefaultKeyTokenLeftOver() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Fillout template: key token left over");

        // Arrange
        Map<String, String> emptyMap = Collections.emptyMap();
        template = new FilloutTemplate(DEFAULT_TEST_TEMPLATE, emptyMap);

        // Act
        template.fillout();

        // Assert
        // see expectedException rule
    }

    @Test
    public void createTemplateFilloutDefaultEmptyMap() {
        // Arrange
        Map<String, String> emptyMap = Collections.emptyMap();
        template = new FilloutTemplate(NO_REPLACE_TEST_TEMPLATE, emptyMap);

        // Act
        String result = template.fillout();

        // Assert
        assertThat(result).isEqualTo(NO_REPLACE_TEST_TEMPLATE);
    }

    @Test
    public void createTemplateFilloutTwice() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Template already filled out");

        // Arrange
        Map<String, String> emptyMap = Collections.emptyMap();
        template = new FilloutTemplate(NO_REPLACE_TEST_TEMPLATE, emptyMap);

        // Act
        template.fillout();
        template.fillout(); // fillout twice in order to provoke exception

        // Assert
        // see expectedException rule
    }

    @Test
    public void createTemplateFilloutEscapeSequenceInKey() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(
                StringContains.containsString("FilloutTemplate: Escape Sequence should be ommited in hash map"));

        // Arrange
        Map<String, String> oneValueMap = Collections.singletonMap("%%invalidKey", "someValue");
        template = new FilloutTemplate(DEFAULT_TEST_TEMPLATE, oneValueMap);

        // Act
        template.fillout();

        // Assert
        // see expectedException rule
    }

    @Test
    public void createTemplateFilloutEmptyKey() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("FilloutTemplate: Empty Key String"));

        // Arrange
        Map<String, String> oneValueMap = Collections.singletonMap("", "someValue");
        template = new FilloutTemplate(DEFAULT_TEST_TEMPLATE, oneValueMap);

        // Act
        template.fillout();

        // Assert
        // see expectedException rule
    }

    @Test
    public void createTemplateFilloutSuccess() {
        // Arrange
        Map<String, String> kvm = new HashMap<>();
        kvm.put(PROP_VAL1, "one");
        kvm.put(PROP_VAL2, "two");
        kvm.put(PROP_VAL3, "three");
        template = new FilloutTemplate(DEFAULT_TEST_TEMPLATE, kvm);

        // Act
        String result = template.fillout();

        // Assert
        assertThat(result).contains("one").contains("two").contains("three").doesNotContain(PROP_VAL1)
                .doesNotContain(PROP_VAL2).doesNotContain(PROP_VAL3);
    }

    @Test
    public void createTemplateFromFileName() {
        // fail("Not yet implemented");
    }

}
