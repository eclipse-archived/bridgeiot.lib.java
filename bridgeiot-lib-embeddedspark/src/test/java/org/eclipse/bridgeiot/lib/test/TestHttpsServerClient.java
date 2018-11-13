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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.bridgeiot.lib.embeddedspark.EmbeddedSpark;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.security.AccessToken;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.junit.Test;

public class TestHttpsServerClient {

    private static final String DEFAULT_KEYSTORE_LOCATION = "keystore/keystore.jks";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "12345678";
    private static final String DEFAULT_PEM_CERTIFICATE_FILE = "keystore/bigiot-lib-cert.pem";

    private static AccessRequestHandler accessCallback = new AccessRequestHandler() {
        @Override
        public BridgeIotHttpResponse processRequestHandler(OfferingDescription offeringDescription,
                Map<String, Object> inputData, String subscriptionId, String consumerInfo) {
            return BridgeIotHttpResponse.okay().withBody("TEST").asJsonType();
        };
    };

    @Test
    public void test() {

        EmbeddedSpark server = new EmbeddedSpark("localhost", 9443);
        server.start();
        // server.start(DEFAULT_KEYSTORE_LOCATION, DEFAULT_KEYSTORE_PASSWORD);
        server.addRoute("/test", accessCallback, new RegistrableOfferingDescription(), true);

        final String jwtToken = AccessToken.generate("1234567890", "TestOfferingId");
        // logger.debug("-- Add Authorization Header with Access Token: Bearer {}", jwtToken);
        HashMap<String, String> addedHeaders = new HashMap<>();
        addedHeaders.put("Authorization", "Bearer " + jwtToken);

        /*
         * Response response;
         * 
         * HttpClient client = HttpClient.createTrustingHttpsClient(); try { response =
         * client.get("https://localhost:9443/test", addedHeaders);
         * 
         * assert(response.code() == 200);
         * 
         * String responseString = response.body().string(); assert(responseString.equals("TEST"));
         * 
         * } catch (IOException e) { e.printStackTrace(); fail("Test failed!"); }
         * 
         * client = HttpClient.createHttpsClient(DEFAULT_PEM_CERTIFICATE_FILE); try { response =
         * client.get("https://localhost:9443/test", addedHeaders);
         * 
         * assert(response.code() == 200); assert(response.body().string().equals("TEST"));
         * 
         * } catch (IOException e) { e.printStackTrace(); fail("Test failed!"); }
         * 
         * ClassLoader classLoader = OfferingCoreByLib.class.getClassLoader(); // URL filename =
         * classLoader.getResource(DEFAULT_PEM_CERTIFICATE_FILE); InputStream is =
         * classLoader.getResourceAsStream("keystore/bigiot-lib-cert.pem"); // File providerCertFile = new
         * File(filename.getFile()); client = HttpClient.createHttpsClient(is); try { response =
         * client.get("https://localhost:9443/test", addedHeaders);
         * 
         * assert(response.code() == 200); assert(response.body().string().equals("TEST"));
         * 
         * } catch (IOException e) { e.printStackTrace(); fail("Test failed!"); }
         */

    }

}
