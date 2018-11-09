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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibConfigurationTest {

    ResourceBundle bundle = ResourceBundle.getBundle("org.eclipse.bridgeiot.lib.test_config", Locale.ROOT);

    @Test
    public void getCustomConfig() {
        assertThat(LibConfiguration.ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS).isEqualTo(false);
        assertThat(LibConfiguration.ACCESS_TOKEN_VALIDATION_REQUIRED).isEqualTo(false);
        assertThat(LibConfiguration.EXECUTOR_POOL_SIZE).isEqualTo(23);
        assertThat(LibConfiguration.FEED_SYNC_INTERVAL).isEqualTo(7);
        assertThat(LibConfiguration.JSON_MAPPING_DEPTH).isEqualTo(23);
    }

    @Test
    public void getBoolNullBundle() {
        // Act
        boolean result = LibConfiguration.getBool(null, "someBoolean", true);

        // Assert
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void getBoolFindInBundleFailed() {
        // Act
        boolean result = LibConfiguration.getBool(bundle, "failedBoolean", true);

        // Assert
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void getBoolTypeMissmatch() {
        // Act
        boolean result = LibConfiguration.getBool(bundle, "nonBoolean", true);

        // Assert
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void getBoolFindInBundleSuccess() {
        // Act
        boolean result = LibConfiguration.getBool(bundle, "someBoolean", true);

        // Assert
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void getIntegerNullBundle() {
        // Act
        int result = LibConfiguration.getInteger(null, "someInteger", 42);

        // Assert
        assertThat(result).isEqualTo(42);
    }

    @Test
    public void getIntegerFindInBundleFailed() {
        // Act
        int result = LibConfiguration.getInteger(bundle, "failedInteger", 42);

        // Assert
        assertThat(result).isEqualTo(42);
    }

    @Test
    public void getIntegerTypeMissmatch() {
        // Act
        int result = LibConfiguration.getInteger(bundle, "nonInteger", 42);

        // Assert
        assertThat(result).isEqualTo(42);
    }

    @Test
    public void getIntegerFindInBundleSuccess() {
        // Act
        int result = LibConfiguration.getInteger(bundle, "someInteger", 42);

        // Assert
        assertThat(result).isEqualTo(7);
    }

    @Test
    public void getStringNullBundle() {
        // Act
        String result = LibConfiguration.getString(null, "someString", "default");

        // Assert
        assertThat(result).isEqualTo("default");
    }

    @Test
    public void getStringFindInBundleFailed() {
        // Act
        String result = LibConfiguration.getString(bundle, "failedString", "default");

        // Assert
        assertThat(result).isEqualTo("default");
    }

    @Test
    public void getStringEmpty() {
        // Act
        String result = LibConfiguration.getString(bundle, "emptyString", "default");

        // Assert
        assertThat(result).isEqualTo("");
    }

    @Test
    public void getStringFindInBundleSuccess() {
        // Act
        String result = LibConfiguration.getString(bundle, "someString", "default");

        // Assert
        assertThat(result).isEqualTo("ImAstring");
    }

}
