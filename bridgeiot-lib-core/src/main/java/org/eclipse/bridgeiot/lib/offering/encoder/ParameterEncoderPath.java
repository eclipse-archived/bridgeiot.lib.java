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
import java.util.Map.Entry;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterEncoderPath extends ParameterEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ParameterEncoderPath.class);
    String urlTemplate;

    public ParameterEncoderPath(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String encode(Object parameters) {

        String url = urlTemplate;
        if (!(parameters instanceof Map)) {
            throw new BridgeIoTException("Encode has to be called with a map");
        }

        Map<String, Object> map = (Map<String, Object>) parameters;

        for (Entry<String, Object> entry : map.entrySet()) {
            if (!url.contains(entry.getKey())) {
                logger.error(
                        "Url template does not contain all parameters marked as path parameters: " + entry.getKey());
            }

            url = url.replaceAll(ESCAPE_SEQUENCE + entry.getKey() + ESCAPE_SEQUENCE + "/",
                    entry.getValue() != null ? entry.getValue().toString() + "/" : "");
        }

        if (url.contains(ESCAPE_SEQUENCE)) {
            // url = url.replaceAll("@@:word:*@@/", ""); Untested
            // url = url.replaceAll("@@:word:*@@", "");
            logger.error("Not all path parameters could be set. Url is now: {}", url);
        }

        return url;
    }

}
