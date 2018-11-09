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
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.Response;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.offering.LegacyOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.security.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jsonwebtoken.MalformedJwtException;

public class MarketplaceClient {

    private static final Logger logger = LoggerFactory.getLogger(MarketplaceClient.class);

    private String marketplaceUri = null;
    private String marketplaceGraphQLUri = null;
    private BridgeIotClientId clientId = null;
    private String clientSecret = null;
    private String clientAccessToken = null;
    private HttpClient httpClient = null;

    // scheduled executor to update marketplace token prior to expiration
    @JsonIgnore
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final List<String> localMarketplaceStringList = Arrays.asList("localhost", "127.0.0.1", "192.168.",
            "local");

    private MarketplaceClient(String marketplaceUri, BridgeIotClientId clientId, String clientSecret) {
        this.marketplaceUri = marketplaceUri;
        marketplaceGraphQLUri = marketplaceUri + "/graphql";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Deprecated
    public static MarketplaceClient createHttpOnlyClient(String marketplaceUri, BridgeIotClientId clientId,
            String clientSecret) {
        final MarketplaceClient client = new MarketplaceClient(marketplaceUri, clientId, clientSecret);
        client.httpClient = HttpClient.createHttpClient();
        return client;
    }

    public static MarketplaceClient create(String marketplaceUri, BridgeIotClientId clientId, String clientSecret) {
        return MarketplaceClient.create(marketplaceUri, clientId, clientSecret, "");
    }

    public static MarketplaceClient create(String marketplaceUri, BridgeIotClientId clientId, String clientSecret,
            String marketplaceCertFileName) {

        final MarketplaceClient client = new MarketplaceClient(marketplaceUri, clientId, clientSecret);

        if (useHttps(marketplaceUri)) {
            if ((marketplaceCertFileName == null) || marketplaceCertFileName.isEmpty()) {
                // check if the marketplace is a local instance
                if (localMarketplace(marketplaceUri)) {
                    client.httpClient = HttpClient.createTrustingHttpsClient();
                } else {
                    // @formatter:off
                    // THIS CODE IS ONLY NEEDED if CloudMarketplace uses a self-signed certificate!
                    // ClassLoader classLoader = OfferingCoreByLib.class.getClassLoader();
                    // InputStream is = classLoader.getResourceAsStream(DEFAULT_PEM_CERTIFICATE_FILE);
                    // @formatter:on
                    client.httpClient = HttpClient.createHttpsClient(); // (is);
                }
            } else {
                final File marketplaceCertFile = new File(marketplaceCertFileName);
                client.httpClient = HttpClient.createHttpsClient(marketplaceCertFile);
            }
        } else {
            client.httpClient = HttpClient.createHttpClient();
        }

        return client;
    }

    /*
	 * @formatter:off
	 * NOT YET NEEDED - but maybe in the future!
     * public static BridgeIotMarketplaceClient createTrustingClient(String marketplaceUri) {
     *   MarketplaceClient client = new MarketplaceClient(marketplaceUri, "");
     *   client.httpClient = HttpClient.createTrustingHttpsClient();
     *   return client;
     * }
     * @formatter:on
     */

    public void authenticate() throws IOException {

        // Check if Cloud Marketplace is used, as access token only works with Cloud Marketplace so far!
        if (!localMarketplace(marketplaceUri)) {

            final HashMap<String, String> addHeaders = new HashMap<>();
            addHeaders.put("Connection", "close"); // Work around to avoid: java.io.IOException: unexpected end of
                                                   // stream on okhttp3
            Response response = httpClient.get(
                    marketplaceUri + "/accessToken?clientId=" + clientId.asString() + "&clientSecret=" + clientSecret,
                    addHeaders);

            clientAccessToken = response.body().string();

            try {
                if (!response.isSuccessful() || !AccessToken.validateMarketplaceToken(clientAccessToken, clientSecret,
                        clientId.asString())) {
                    throw new BridgeIoTException(
                            "ERROR: Marketplace Authentication failed - check your Consumer/Provider ID and Secret!");
                }
            } catch (final MalformedJwtException e) {
                throw new BridgeIoTException(
                        "ERROR: Marketplace Authentication failed - check your Consumer/Provider ID and Secret!", e);
            }

            response.close();

            logger.info("Successfully authenticated at marketplace");

            // Create schedule for timely re-registration - to avoid expiration of the offering
            Long timeToReregister = Math
                    .max(AccessToken.getExpirationTime(clientAccessToken) - new Date().getTime() - 3600000L, 60000);
            executor.schedule(authenticationRunnable, timeToReregister, TimeUnit.MILLISECONDS);

        }

    }

    public void request(String json, Callback callback) {
        if (clientAccessToken == null) {
            httpClient.post(marketplaceGraphQLUri, callback, json);
        } else {
            final HashMap<String, String> addHeaders = new HashMap<>();
            addHeaders.put("Authorization", "Bearer " + clientAccessToken);
            httpClient.post(marketplaceGraphQLUri, addHeaders, callback, json);
        }
    }

    public Response request(String json) throws IOException {
        if (clientAccessToken == null) {
            return httpClient.post(marketplaceGraphQLUri, json);
        } else {
            final HashMap<String, String> addHeaders = new HashMap<>();
            addHeaders.put("Authorization", "Bearer " + clientAccessToken);
            return httpClient.post(marketplaceGraphQLUri, addHeaders, json);
        }
    }

    public OfferingDescription getOfferingDescription(String offeringId) throws InvalidOfferingException, IOException {
        String getOfferingString = GraphQLQueries.getOfferingDescriptionString(offeringId);

        try {

            Response response = request(getOfferingString);

            if (!response.isSuccessful()) {
                throw new InvalidOfferingException();
            }

            String responseString = response.body().string();
            response.close();

            LegacyOfferingDescription legacyOfferingDescription = Helper.unmarshallSingleFromQueryResponse("offering",
                    responseString, LegacyOfferingDescription.class);

            OfferingDescription offeringDescription = new OfferingDescription();
            offeringDescription.copyFromLegacy(legacyOfferingDescription);
            return offeringDescription;

        } catch (BridgeIoTException e) {
            logger.error("Fetching Offering Description with ID={} failed!", offeringId);
            throw new InvalidOfferingException(e);
        }

    }

    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
        httpClient.close();
    }

    private static boolean useHttps(String marketplaceUri) {
        return marketplaceUri.toUpperCase().startsWith("HTTPS");
    }

    private static boolean localMarketplace(String marketplaceUri) {
        final Iterator<String> listIterator = localMarketplaceStringList.iterator();
        while (listIterator.hasNext()) {
            // check if marketplaceUri contains a string that identifies it as a local marketplaceUri
            if (marketplaceUri.contains(listIterator.next())) {
                // marketplaceUri matches a string that identifies it as a local marketplaceUri
                return true;
            }
        }
        return false;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    // Needed to force termination of all executor threads (even if the Consumer/Provider does not call the .terminate()
    // method)
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private Runnable authenticationRunnable = new Runnable() {
        @Override
        public void run() {
            logger.info("Re-authenticate client on Marketplace to refresh access token!");
            try {
                authenticate();
            } catch (IOException e) {
                logger.error("Re-authentication of client on Marketplace failed ... try again in 60 seconds!");
                logger.error(e.getMessage(), e);
                executor.schedule(authenticationRunnable, 60, TimeUnit.SECONDS);
            }
        }
    };

}
