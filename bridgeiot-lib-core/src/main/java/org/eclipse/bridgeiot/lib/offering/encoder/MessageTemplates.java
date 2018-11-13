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
package org.eclipse.bridgeiot.lib.offering.encoder;

import java.util.HashMap;
import java.util.Map;

public class MessageTemplates {

    private String template;

    private Map<String, String> subTemplates = new HashMap<>();

    public MessageTemplates() {
    }

    public MessageTemplates(String template) {
        super();
        this.template = template;
    }

    public static MessageTemplates create(String template) {

        return new MessageTemplates(template);
    }

    public MessageTemplates addSubTemplate(String subTemplate, String placeholder) {
        subTemplates.put(placeholder, subTemplate);
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, String> getSubTemplates() {
        return subTemplates;
    }

    public void setSubTemplates(Map<String, String> subTemplates) {
        this.subTemplates = subTemplates;
    }

}
