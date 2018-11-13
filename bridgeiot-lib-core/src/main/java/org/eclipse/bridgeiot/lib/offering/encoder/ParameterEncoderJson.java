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

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parameter Encoder for Json
 */
public class ParameterEncoderJson extends ParameterEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ParameterEncoderJson.class);

    /**
     * Generates a valid encoding in JSON notation for the access parameters used for an Offering call.
     * 
     * @param parameters
     * @return
     */
    static ObjectMapper mapper = new ObjectMapper();

    @Override
    public String encode(Object parameters) {
        try {
            return mapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Cannot serial parameter object into JSON", e);
        }
    }

}
