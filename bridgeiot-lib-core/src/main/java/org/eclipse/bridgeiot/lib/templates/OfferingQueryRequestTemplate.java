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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.misc.FilloutTemplate;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;

/**
 * Template for Offering Query
 * 
 *
 */
public class OfferingQueryRequestTemplate extends FilloutTemplate {

    static final String DEFAULT_REQUEST_TEMPLATE = GraphQLQueries.getQueryBaseTemplateString();

    public OfferingQueryRequestTemplate(final List<String> queryElements) {
        super(DEFAULT_REQUEST_TEMPLATE, new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                String queryElementsString = "";
                boolean first = true;
                for (String element : queryElements) {
                    if (!first)
                        queryElementsString += " ";
                    first = false;
                    queryElementsString += " " + element;
                }

                put("queryElements", queryElementsString);
            }
        });
    }

    static Map<String, String> createFilledMap(final List<String> queryElements) {
        boolean first = Boolean.TRUE;
        StringBuilder builder = new StringBuilder();
        for (String element : queryElements) {
            if (!first) {
                builder.append(" ");
            }
            builder.append(element);
            first = false;
        }
        return Collections.singletonMap("queryElements", builder.toString());
    }

}
