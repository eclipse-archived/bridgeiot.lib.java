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
package org.eclipse.bridgeiot.lib.templates;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class OfferingQueryRequestTemplateTest {

    @Test
    public void createFromElementListSuccess() {
        // Arrange
        List<String> queryElements = new ArrayList<>();
        queryElements.add("elementOne");
        queryElements.add("elementTwo");
        queryElements.add("elementThree");
        OfferingQueryRequestTemplate template = new OfferingQueryRequestTemplate(queryElements);

        // Act
        String result = template.fillout();

        // Assert
        assertThat(result).contains("elementOne").contains("elementTwo").contains("elementThree");
    }

    @Test
    public void createFromElementListEmpty() {
        // Arrange
        List<String> emptyList = Collections.emptyList();
        OfferingQueryRequestTemplate oqrTemplate = new OfferingQueryRequestTemplate(emptyList);

        // Act
        String result = oqrTemplate.fillout();

        // Assert
        assertThat(result);
    }

}
