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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeIotProperties {

    Properties properties;

    public String MARKETPLACE_URI = "";
    public String MARKETPLACE_URI2 = "";
    public String ORGANIZATION = "";
    public String PROVIDER_ID = "";
    public String PROVIDER_SECRET = "";
    public String PROVIDER_ID2 = "";
    public String PROVIDER_SECRET2 = "";
    public String CONSUMER_ID = "";
    public String CONSUMER_SECRET = "";
    public String CONSUMER_ID2 = "";
    public String CONSUMER_SECRET2 = "";
    public String PROVIDER_DNS_NAME = "localhost";
    public int PROVIDER_PORT = 9999;
    public String PROXY = "";
    public int PROXY_PORT = 3128;
    public String PROXY_BYPASS = "";

    public static final String MARKETPLACE_URI_KEY = "marketplaceUri";
    public static final String MARKETPLACE_URI2_KEY = "marketplaceUri2";
    public static final String ORGANIZATION_KEY = "organization";
    public static final String PROVIDER_ID_KEY = "providerId";
    public static final String PROVIDER_SECRET_KEY = "providerSecret";
    public static final String PROVIDER_ID2_KEY = "providerId2";
    public static final String PROVIDER_SECRET2_KEY = "providerSecret2";
    public static final String CONSUMER_ID_KEY = "consumerId";
    public static final String CONSUMER_SECRET_KEY = "consumerSecret";
    public static final String CONSUMER_ID2_KEY = "consumerId2";
    public static final String CONSUMER_SECRET2_KEY = "consumerSecret2";
    public static final String PROVIDER_DNS_NAME_KEY = "providerDnsName";
    public static final String PROVIDER_PORT_KEY = "providerPort";
    public static final String PROXY_KEY = "proxyAddress";
    public static final String PROXY_PORT_KEY = "proxyPort";
    public static final String PROXY_BYPASS_KEY = "proxyByPassAddress";

    private static final String DEFAULT_FILE_NAME = "bridgeiot.properties";
    private static final Logger logger = LoggerFactory.getLogger(BridgeIotProperties.class);

    protected BridgeIotProperties() {
        properties = new Properties();
    }

    public static BridgeIotProperties load() throws FileNotFoundException {
        return load(DEFAULT_FILE_NAME);
    }

    public static BridgeIotProperties load(String fileName) throws FileNotFoundException {
        return new BridgeIotProperties().loadFile(fileName);
    }

    public static BridgeIotProperties load(InputStream inputStream) throws FileNotFoundException {
        return new BridgeIotProperties().loadFile(inputStream);
    }

    private BridgeIotProperties loadFile(String fileName) throws FileNotFoundException {

        logger.info("Loading configuration from {}", fileName);

        File file = new File(fileName);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return loadFile(inputStream);
        } catch (IOException e) {
            logger.error("ERROR: Property file '{}' not found or not readable", fileName);
            logger.error(e.getMessage(), e);
            throw new FileNotFoundException("Property file '" + fileName + "' not found or not readable");
        }
    }

    private BridgeIotProperties loadFile(InputStream inputStream) throws FileNotFoundException {

        try {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("ERROR: Reading Property File Input Stream!");
            logger.error(e.getMessage(), e);
            throw new FileNotFoundException();
        }

        this.MARKETPLACE_URI = getProperty(MARKETPLACE_URI_KEY, MARKETPLACE_URI);
        this.MARKETPLACE_URI2 = getProperty(MARKETPLACE_URI2_KEY);
        this.ORGANIZATION = getProperty(ORGANIZATION_KEY);
        this.PROVIDER_ID = getProperty(PROVIDER_ID_KEY);
        this.PROVIDER_ID2 = getProperty(PROVIDER_ID2_KEY);
        this.PROVIDER_SECRET = getProperty(PROVIDER_SECRET_KEY);
        this.PROVIDER_SECRET2 = getProperty(PROVIDER_SECRET2_KEY);
        this.CONSUMER_ID = getProperty(CONSUMER_ID_KEY);
        this.CONSUMER_ID2 = getProperty(CONSUMER_ID2_KEY);
        this.CONSUMER_SECRET = getProperty(CONSUMER_SECRET_KEY);
        this.CONSUMER_SECRET2 = getProperty(CONSUMER_SECRET2_KEY);
        this.PROVIDER_DNS_NAME = getProperty(PROVIDER_DNS_NAME_KEY, PROVIDER_DNS_NAME);
        this.PROVIDER_PORT = Integer.parseInt(getProperty(PROVIDER_PORT_KEY, String.valueOf(PROVIDER_PORT)));
        this.PROXY = getProperty(PROXY_KEY, PROXY);
        this.PROXY_PORT = Integer.parseInt(getProperty(PROXY_PORT_KEY, String.valueOf(PROXY_PORT)));
        this.PROXY_BYPASS = getProperty(PROXY_BYPASS_KEY, PROXY_BYPASS);

        return this;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
