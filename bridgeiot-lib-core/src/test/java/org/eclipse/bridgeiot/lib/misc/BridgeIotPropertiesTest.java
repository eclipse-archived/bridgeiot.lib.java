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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BridgeIotPropertiesTest {

    private static final String SOME_URL = "https://someUrl.org";
    private static final String PROVIDER_DNS_NAME = "localhost";
    private static final int PROVIDER_PORT = 9999;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPropertiesFile() throws FileNotFoundException {
        BridgeIotProperties testProperties = BridgeIotProperties.load("src/test/resources/test.properties");
        assertThat(testProperties.MARKETPLACE_URI).contains("https://");
        assertThat(testProperties.MARKETPLACE_URI).contains(".big-iot.org");
    }

    @Test
    public void testIncompletePropertiesFile() throws FileNotFoundException {
        BridgeIotProperties testProperties = BridgeIotProperties.load("src/test/resources/test_incomplete.properties");
        assertThat(testProperties.PROVIDER_DNS_NAME).isEqualTo(PROVIDER_DNS_NAME);
        assertThat(testProperties.PROVIDER_PORT).isEqualTo(PROVIDER_PORT);
        assertThat(testProperties.getProperty("testValue1")).isEqualTo("123456789");
        assertThat(testProperties.getProperty("testValue2")).isEqualTo("\"test\"");
        assertThat(testProperties.getProperty("testValue3")).isEqualTo("test");
    }

    @Test
    public void testDefaultValuesPropertiesFile() throws FileNotFoundException {
        BridgeIotProperties testProperties = BridgeIotProperties.load("src/test/resources/test_incomplete.properties");
        assertThat(testProperties.getProperty("notAvailableKey", SOME_URL)).isEqualTo(SOME_URL);
    }

    @Test(expected = FileNotFoundException.class)
    public void testWrongPropertiesFile() throws FileNotFoundException {
        BridgeIotProperties.load("xxx");
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoPropertiesFile() throws FileNotFoundException {
        BridgeIotProperties.load("");
    }

    @Test(expected = FileNotFoundException.class)
    public void testDefaultPropertiesFile() throws FileNotFoundException {
        BridgeIotProperties.load();
    }

}
