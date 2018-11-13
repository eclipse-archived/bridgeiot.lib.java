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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import okhttp3.HttpUrl;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.misc.Constants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ParameterEncoderQuery extends ParameterEncoder {

    @Override
    @SuppressWarnings({ "unchecked" })
    public String encode(Object parameters) {
        if (!(parameters instanceof Map)) {
            throw new BridgeIoTException("Encode has to be called with a map");
        }

        Map<?, ?> map = (Map<?, ?>) parameters;
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("http").host("localhost");

        if (map.keySet().isEmpty()) {
            return "?" + urlBuilder.build().encodedQuery();
        }

        if (!(map.keySet().iterator().next() instanceof String)) {
            throw new BridgeIoTException(
                    "Map encoded access parameters should have String typed keys. Disable this exception and try if the code still works");
        }

        return encodeQuery(((Map<String, Object>) map), urlBuilder);
    }

    static String encodeQuery(Map<String, Object> map, HttpUrl.Builder urlBuilder) {
        boolean isComplex = false; // On default, parameters encoded in url query parameter syntax

        // Using URL query parameter is only possible if access parameter is flat

        Iterator<Entry<String, Object>> itr = map.entrySet().iterator();

        while (itr.hasNext()) {
            Entry<String, Object> entry = itr.next();
            if (entry.getValue().getClass().isArray() || entry.getValue() instanceof Collection
                    || entry.getValue() instanceof Map) {
                isComplex = true;
                break;
            }
        }
        if (isComplex) {
            ObjectMapper mapper = new ObjectMapper();

            try {

                String json = mapper.writeValueAsString(map);
                urlBuilder.addQueryParameter(Constants.COMPLEX_PARAMETER_KEY, json);
            } catch (JsonProcessingException e) {
                throw new BridgeIoTException("Canot serialize access parameters", e);
            }

        } else {
            ParameterEncoderUrlTrial parameterEncoderSimple = new ParameterEncoderUrlTrial();
            return parameterEncoderSimple.encode(map);
        }

        return "?" + urlBuilder.build().encodedQuery();
    }

}
