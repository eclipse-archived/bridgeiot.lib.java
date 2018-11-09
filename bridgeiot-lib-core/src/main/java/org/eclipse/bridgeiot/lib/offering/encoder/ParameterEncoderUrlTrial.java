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

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parameter encoder for URL query parameters
 */
public class ParameterEncoderUrlTrial extends ParameterEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ParameterEncoderUrlTrial.class);

    public enum ComplexObjectUrlSyntax {
        DOT_SYNTAX, BRACE_SYNTAX
    }

    private static final int DEPTH_OF_NESTED_TYPES = 12;
    public static final ComplexObjectUrlSyntax complexObjectUrlSyntax = ComplexObjectUrlSyntax.DOT_SYNTAX;

    /**
     * Generates a valid encoding as a URL parameter string for the parameterization of an Offering call.
     * 
     * @param parameters
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public String encode(Object parameters) {

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("http").host("localhost");

        if (parameters instanceof Map) {
            encodeMap((Map) parameters, urlBuilder);
        } else {
            encodePojo(parameters, urlBuilder);
        }

        return "?" + urlBuilder.build().encodedQuery();
    }

    private void encodeMap(Map parameters, HttpUrl.Builder urlBuilder) {
        switch (complexObjectUrlSyntax) {
        case DOT_SYNTAX:
            encodeMapDotSyntax(parameters, urlBuilder, "", DEPTH_OF_NESTED_TYPES);
            break;
        case BRACE_SYNTAX:
            encodeMapBraceSyntax(parameters, urlBuilder);
            break;
        default:
            throw new BridgeIoTException("Unsupported URL encoding for complex objects used");
        }
    }

    /**
     * Encodes a Pojo or a Map, where the key is expressable as a String, to a url query String
     * 
     * @param parameters
     *            The parameter as a PoJo or Map<String,Object>
     * @param urlBuilder
     *            url to be extended
     * @param prefix
     *            A Prefix for expressing nested types in dot syntax
     * @param counter
     *            A counter to terminate recursion in case loops occurs.
     */

    @SuppressWarnings("rawtypes")
    private void encodeMapDotSyntax(Map parameters, HttpUrl.Builder urlBuilder, String prefix, int counter) {
        if (counter <= 0) {
            throw new BridgeIoTException("Your access parameter object seems to have a cycle");
        }

        if (!parameters.keySet().isEmpty()) {
            if (parameters.keySet().iterator().next() instanceof String) {

                Map map = parameters;
                Iterator itr = map.entrySet().iterator();

                while (itr.hasNext()) {
                    // Map INPUT data from AccessRequest to Request Parameter
                    Entry entry = (Entry) itr.next();
                    String key = entry.getKey().toString();
                    if (key != null) {

                        if (entry.getValue() instanceof Map) {
                            encodeMapDotSyntax((Map) entry.getValue(), urlBuilder, prefix + key + ".", counter - 1);
                        }
                        if (entry.getValue() instanceof Object[]) {
                            Object[] values = (Object[]) entry.getValue();

                            String s = "[";
                            for (int i = 0; i < values.length; i++) {
                                if (i > 0)
                                    s += ",";
                                if (values[i] instanceof Map) {
                                    HttpUrl.Builder newUrlBuilder = new HttpUrl.Builder().scheme("http")
                                            .host("localhost");
                                    encodeMapDotSyntax((Map) values[i], newUrlBuilder, prefix, counter);

                                }
                            }
                            s += "]";

                        } else {
                            String queryValue = entry.getValue().toString();
                            urlBuilder.addQueryParameter(prefix + key, queryValue).build();
                        }

                    }
                }
            }
        } else {
            throw new BridgeIoTException(
                    "Map encoded access parameters should have String typed keys. Disable this exception and try if the code still works");
        }

    }

    @SuppressWarnings("rawtypes")
    private void encodeMapBraceSyntax(Map parameters, Builder urlBuilder) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString = mapper.writeValueAsString(parameters);
            jsonString = jsonString.substring(2, jsonString.length() - 1);

            jsonString.replaceAll("\":\"", "=");
            jsonString.replaceAll(",", "&");
            jsonString.replaceAll("\"", "");

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Cannot serialue parameter object into JSON", e);
        }

    }

    private void encodePojo(Object parameters, HttpUrl.Builder urlBuilder) {
        encodePojo(parameters, DEPTH_OF_NESTED_TYPES);
    }

    private void encodePojo(Object parameters, int counter) {

        if (counter <= 0) {
            throw new BridgeIoTException("Your access paramter object seems to have a cycle");
        }
        try {
            // Assume attribute of PoJo represents parameters
            Class<?> c = parameters.getClass();
            // Public and private fields
            for (Field field : c.getDeclaredFields()) {
                String fieldName = field.getName();
                String fieldType = field.getType().getName();
                Object value = field.get(parameters);
                // logger.info( " {} {} = {}", fieldType, fieldName, value.toString() );

                throw new BridgeIoTException("This is not yet supported !!! - requires Java 7 solution");
                /*
                 * if ( fieldType.equals(AccessParameters.class.getTypeName() ) ){ encodePojo(value, urlBuilder,
                 * fieldName + ".", counter - 1); } else { String queryValue = value.toString();
                 * urlBuilder.addQueryParameter(fieldName, prefix + queryValue).build(); }
                 */
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        }
    }

}