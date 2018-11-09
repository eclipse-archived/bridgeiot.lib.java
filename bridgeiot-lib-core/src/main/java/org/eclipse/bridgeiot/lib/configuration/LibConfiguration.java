/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Bosch Software Innovations GmbH, Robert Bosch GmbH, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.bridgeiot.lib.configuration;

import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BridgeIotProperties.class);

    private static final String KEY_FEED_SYNC_INTERVAL = "feedSyncInterval";
    private static final String KEY_EXECUTOR_POOL_SIZE = "executorPoolSize";
    private static final String KEY_JSON_MAPPING_DEPTH = "jsonMappingDepth";
    private static final String KEY_ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS = "isParameterMatchForced";
    private static final String KEY_ACCESS_TOKEN_VALIDATION_REQUIRED = "isAccessTokenValidationRequired";

    static final String DEFAULT_CONFIG_NAME = "org.eclipse.bridgeiot.lib.configuration";
    private static final String CUSTOM_CONFIG_NAME = "org.eclipse.bridgeiot.lib.custom_configuration";

    public static final int FEED_SYNC_INTERVAL;
    public static final int EXECUTOR_POOL_SIZE;
    public static final int JSON_MAPPING_DEPTH;
    public static final boolean ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS;
    public static final boolean ACCESS_TOKEN_VALIDATION_REQUIRED;
    public static final ParameterEncodingType DEFAULT_PARAMETER_ENCODING_TYPE = ParameterEncodingType.QUERY;

    static {
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(CUSTOM_CONFIG_NAME, Locale.ROOT);
            logger.info("The Lib configuration is read from a custom properties file: ({})", CUSTOM_CONFIG_NAME);
        } catch (RuntimeException e) {
            bundle = ResourceBundle.getBundle(DEFAULT_CONFIG_NAME, Locale.ROOT);
            logger.info("The Lib configuration is read from the default properties file: ({})", DEFAULT_CONFIG_NAME);
        }
        FEED_SYNC_INTERVAL = getInteger(bundle, KEY_FEED_SYNC_INTERVAL, 4);
        EXECUTOR_POOL_SIZE = getInteger(bundle, KEY_EXECUTOR_POOL_SIZE, 10);
        JSON_MAPPING_DEPTH = getInteger(bundle, KEY_JSON_MAPPING_DEPTH, 10);
        ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS = getBool(bundle,
                KEY_ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS, false);
        ACCESS_TOKEN_VALIDATION_REQUIRED = getBool(bundle, KEY_ACCESS_TOKEN_VALIDATION_REQUIRED, true);
    }

    private LibConfiguration() {

    }

    static String getString(ResourceBundle bundle, String key, String defaultValue) {
        try {
            return bundle.getString(key);
        } catch (RuntimeException e) {
            logger.warn(
                    "Configuration-Property \"{}\" of type String could not be read from configuration file. Defalut value \"{}\" is used.",
                    key, defaultValue);
            return defaultValue;
        }
    }

    static int getInteger(ResourceBundle bundle, String key, int defaultValue) {
        try {
            return Integer.parseInt(bundle.getString(key));
        } catch (RuntimeException e) {
            logger.warn(
                    "Configuration-Property \"{}\" of type Integer could not be read from configuration file. Defalut value \"{}\" is used.",
                    key, defaultValue);
            return defaultValue;
        }
    }

    static boolean getBool(ResourceBundle bundle, String key, boolean defaultValue) {
        try {
            String parsed = bundle.getString(key);

            return "true".equalsIgnoreCase(parsed) || "false".equalsIgnoreCase(parsed) ? Boolean.parseBoolean(parsed)
                    : defaultValue;
        } catch (RuntimeException e) {
            logger.warn(
                    "Configuration-Property \"{}\" of type Boolean could not be read from configuration file. Defalut value \"{}\" is used.",
                    key, defaultValue);
            return defaultValue;
        }
    }
}
