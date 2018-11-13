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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameter Encoder for Json
 * 
 *
 */
public class ParameterEncoderTemplate extends ParameterEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ParameterEncoderPath.class);

    MessageTemplates templates;

    public ParameterEncoderTemplate(MessageTemplates templates) {
        super();
        this.templates = templates;
    }

    @Override
    public String encode(Object parameters) {

        Map<String, Object> placeHolderAccessObjectMap = (Map<String, Object>) parameters;

        String result = templates.getTemplate();

        for (String placeholder : placeHolderAccessObjectMap.keySet()) {
            result = result.replaceAll(ESCAPE_SEQUENCE + placeholder + ESCAPE_SEQUENCE,
                    placeHolderAccessObjectMap.get(placeholder) != null
                            ? placeHolderAccessObjectMap.get(placeholder).toString()
                            : "");
        }

        if (result.contains(ESCAPE_SEQUENCE)) {
            logger.error("Remplate still contains escape characters");
        }

        return result;
    }

}
