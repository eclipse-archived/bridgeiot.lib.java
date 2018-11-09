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
package org.eclipse.bridgeiot.lib.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.bridgeiot.lib.offering.OfferingCoreByLib;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAccessKeystoreFiles {

    final static Logger logger = LoggerFactory.getLogger(TestAccessKeystoreFiles.class);

    @Test
    public void test() throws IOException {

        final ClassLoader classLoader = OfferingCoreByLib.class.getClassLoader();
        final InputStream is = classLoader.getResourceAsStream("keystore/bigiot-lib-cert.pem");

        // THIS DOES NOT WORK - unclear whey
        // URL url = classLoader.getResource("keystore/bigiot-lib-cert.pem");
        // File file = new File(url.getFile());

        // THIS WORKS, BUT USES SPARK LIB -> not good for Lib Core
        // File file = ResourceUtils.getFile("keystore/bigiot-lib-cert.pem");

        // if (file.canRead()) {
        if (is != null) {

            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            // BufferedReader reader = new BufferedReader(new FileReader(file));

            String str = "";
            String s;
            while ((s = reader.readLine()) != null) {
                str += s + "\n";
            }

            assert (str.contains("-----BEGIN CERTIFICATE-----"));
            assert (str.contains("-----END CERTIFICATE-----"));

        } else {
            fail("Keystore Files not found!");
        }

    }

}
