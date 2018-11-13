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
package org.eclipse.bridgeiot.lib.offering;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.feed.AccessFeedSync;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.misc.HttpClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.FeedTypes;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.offering.encoder.ParameterEncoder;
import org.eclipse.bridgeiot.lib.offering.encoder.ParameterEncoderQuery;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for Offering running in Integration mode 1,2 and 4. The A1 interface is provided by the Bridge.IoT
 * provider Lib. Accordingly the transformation of the call semantics to the legacy platform semantics is performed by
 * the Provider Lib.
 */
public class OfferingCoreByLib extends OfferingCore {

    private static final Logger logger = LoggerFactory.getLogger(OfferingCore.class);

    private FeedTypes feedMode = FeedTypes.SYNC;
    private HttpClient httpClient;

    private ExecutorService executorPool = Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE);
    private LinkedList<AccessFeed> accessFeeds = new LinkedList<>();

    private static final String DEFAULT_PEM_CERTIFICATE_FILE = "keystore/bigiot-lib-cert.pem";

    protected OfferingCoreByLib() {
        super();
    }

    public OfferingCoreByLib(SubscribableOfferingDescriptionCore offeringDescription, String offeringToken)
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {

        this(offeringDescription, offeringToken,
                createHttpClient(offeringDescription, offeringDescription.getConsumer().getProviderCertFile()));
    }

    OfferingCoreByLib(SubscribableOfferingDescriptionCore offeringDescription, String offeringToken,
            HttpClient httpClient) {
        super(offeringDescription, offeringToken);
        this.consumer = offeringDescription.getConsumer();
        this.httpClient = httpClient;
    }

    /**
     * Creates an HttpClient based on data given by an Offering Description. The configured endpoint is inspected (and
     * some consistency checks are performed) and evaluated. Based on this data, a HttpClient is created.
     * 
     * @param offeringDescription
     *            the data the parameters for the HttpClient are taken from
     * @param providerCertFileName
     *            an optional String representation of a custom PEM certificate file. Only needed for secured protocols
     *            (<code>https</code>). If a secured protocol is used and it is <code>null</code> or empty, a default
     *            PEM certificated is used.
     * @return HttpClient
     * 
     * @throws IllegalEndpointException
     * @throws IncompleteOfferingDescriptionException
     */
    static HttpClient createHttpClient(SubscribableOfferingDescriptionCore offeringDescription,
            String providerCertFileName) throws IllegalEndpointException, IncompleteOfferingDescriptionException {

        List<EndPoint> endPoints = Collections.emptyList(); // be robust towards null values
        if (offeringDescription.getEndpoints() != null) {
            endPoints = offeringDescription.getEndpoints();
        }

        if (endPoints.isEmpty()) {
            logger.error("No Endpoint defined in Offering Description!");
            throw new IncompleteOfferingDescriptionException();
        }

        EndPoint first = endPoints.get(0);

        if (!first.getEndpointType().isHTTP()) {
            logger.error("Endpoint {} is not supported!", offeringDescription.getEndpoints().get(0).getUri());
            throw new IllegalEndpointException();
        }

        if (first.isSecured()) {
            if ((providerCertFileName == null) || providerCertFileName.isEmpty()) {
                InputStream is = OfferingCoreByLib.class.getClassLoader()
                        .getResourceAsStream(DEFAULT_PEM_CERTIFICATE_FILE);
                return HttpClient.createHttpsClient(is);
            }
            return HttpClient.createHttpsClient(providerCertFileName);
        }

        return HttpClient.createHttpClient();
    }

    /**
     * Retrieves data from an Offering in a request/response manner asynchronously. Since the return value is a Future,
     * a blocking behavior can be achieved on calling .get() on the return value.
     */
    @Override
    public Future<AccessResponse> accessOneTime(final AccessParameters accessParameters)
            throws AccessToNonSubscribedOfferingException {

        final String offeringAccessToken = getOfferingToken();

        return executorPool.submit(new Callable<AccessResponse>() {
            @Override
            public AccessResponse call() throws IOException, IllegalAccessParameterException,
                    AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {

                final String accessSessionId = String.valueOf(new Date().getTime());
                final String responseString = accessOneTimeInternal(httpClient, offeringDescription, accessParameters,
                        accessSessionId, offeringAccessToken);
                addAccountingEvent(accessSessionId, responseString);
                return new AccessResponse(responseString, offeringDescription);
            }
        });
    }

    protected static String accessOneTimeInternal(HttpClient httpClient, OfferingDescription offeringDescription,
            AccessParameters accessParameters, String accessSessionId, final String offeringAccessToken) {

        String emtpyResponse = "";
        Map<String, Object> parametersMap = null;
        try {
            parametersMap = accessParameters.toNameMap(offeringDescription.getInputs(),
                    LibConfiguration.JSON_MAPPING_DEPTH);
        } catch (IllegalAccessParameterException e) {
            logger.warn("IllegalAccessParameterException {}", (e.getMessage() != null ? e.getMessage() : ""));
            parametersMap = new HashMap<>();
        }

        List<EndPoint> epList = offeringDescription.getEndpoints();
        if (epList.isEmpty()) {
            logger.error("No Endpoint defined in Offering!");
            return emtpyResponse;
        }

        EndPoint endPoint = epList.get(0);
        logger.info("Accessing {} at {} with {}", offeringDescription.getId(), endPoint, parametersMap);
        try {
            validate(offeringDescription);
        } catch (AccessToNonActivatedOfferingException e) {
            logger.error("AccessToNonActivatedOfferingException: {}", e.getMessage());
            return emtpyResponse;
        }

        URI url = null;
        try {
            url = new URI(endPoint.getUri());
        } catch (URISyntaxException e) {
            logger.error("URISyntaxException: {}", e.getMessage());
            return emtpyResponse;
        }

        String urlString = prepareUrlWithParameters(url.toString(), parametersMap);
        Map<String, String> addedHeaders = prepareHeaderWithAccessToken(offeringDescription.getAccessInterfaceType(),
                offeringAccessToken, accessSessionId);
        Response response;
        String responseString = "";
        try {
            response = httpClient.get(urlString, addedHeaders);
            // Check 404
            responseString = response.body().string();
            response.body().close();
            if (!response.isSuccessful()) {
                logger.error("Got a {} HTTP code.Response body is \n{}", response.code(), responseString);
                return emtpyResponse;
            }
            logger.debug("Response received: \n{}", responseString);
        } catch (IOException e) {
            logger.error("Access: HttpGet failed", e);
            return emtpyResponse;
        }

        return responseString;
    }

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     */
    public void accessOneTimeWithSessionId(final String accessSessionId, final AccessParameters accessParameters,
            final AccessResponseSuccessHandler onAccessSuccess, final AccessResponseFailureHandler onAccessFailure)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException {

        final String offeringAccessToken = this.getOfferingToken();

        Map<String, Object> parametersMap = accessParameters.toNameMap(offeringDescription.getInputs(),
                LibConfiguration.JSON_MAPPING_DEPTH);
        List<EndPoint> epList = offeringDescription.getEndpoints();

        if (epList.isEmpty()) {
            logger.error("No Endpoint defined in Offering!");
            throw new AccessToNonActivatedOfferingException();
        }

        EndPoint endPoint = epList.get(0);

        validate(offeringDescription);

        URI url = null;
        try {
            url = new URI(endPoint.getUri());
        } catch (URISyntaxException e) {
            logger.error("URISyntaxException: {}", e.getMessage());
            throw new AccessToNonActivatedOfferingException(e);
        }

        String urlString = prepareUrlWithParameters(url.toString(), parametersMap);
        Map<String, String> addedHeaders = prepareHeaderWithAccessToken(offeringDescription.getAccessInterfaceType(),
                offeringAccessToken, accessSessionId);

        final IOfferingCore offering = this;

        httpClient.get(urlString, addedHeaders, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("Access Request failed!");
                logger.debug("IOException: {}", e);
                if (onAccessFailure != null) {
                    onAccessFailure.processResponseOnFailure(offering,
                            new AccessResponse("{ \"error\": \"Access request failed\" }", offeringDescription));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (onAccessFailure != null) {
                        onAccessFailure.processResponseOnFailure(offering,
                                new AccessResponse(response.message(), offeringDescription));
                    }
                } else {
                    String responseString = response.body().string();
                    response.body().close();
                    try {
                        addAccountingEvent(accessSessionId, responseString);
                        onAccessSuccess.processResponseOnSuccess(offering,
                                new AccessResponse(responseString, offeringDescription));
                    } catch (ExecutionException | InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });

    }

    /**
     * Decorates the url with the encoded parameters ( if available).
     * 
     * @param url
     * @param parametersMap
     * @return decorated url
     */
    static String prepareUrlWithParameters(String url, Map<String, Object> parametersMap) {
        if (parametersMap != null && !parametersMap.isEmpty()) {
            ParameterEncoder encoder = new ParameterEncoderQuery();
            url = url + encoder.encode(parametersMap);
            logger.debug("URL: {}", url);
        }
        return url;
    }

    /**
     * For BRIDGEIOT_LIB mode add Offering Access Token as Authorization header. For all other modes, <code>null</code>
     * is returned indicating an empty Authorization header.
     * 
     * @param accessType
     * @param offeringAccessToken
     * @return the configured Authorization header in case of BRIDGEIOT_LIB mode, and <code>null</code> for all other
     *         modes.
     */
    static Map<String, String> prepareHeaderWithAccessToken(AccessInterfaceType accessType, String offeringAccessToken,
            String accessSessionId) {
        Map<String, String> addedHeaders = null;
        if (AccessInterfaceType.BRIDGEIOT_LIB.equals(accessType)) {
            addedHeaders = new HashMap<>();
            addedHeaders.put("Authorization", "Bearer " + offeringAccessToken);
            logger.debug("-- Add Authorization Header with OfferingAccessToken: Bearer {}", offeringAccessToken);
            if (accessSessionId != null) {
                addedHeaders.put("AccessSessionId", accessSessionId);
            }
        }
        return addedHeaders;
    }

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     */
    @Override
    public void accessOneTime(AccessParameters accessParameters, final AccessResponseSuccessHandler onAccessSuccess,
            final AccessResponseFailureHandler onAccessFailure) throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {

        final String accessSessionId = String.valueOf(new Date().getTime());
        accessOneTimeWithSessionId(accessSessionId, accessParameters, onAccessSuccess, onAccessFailure);

    }

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     */
    @Override
    public void accessOneTime(AccessParameters accessParameters, AccessResponseSuccessHandler onSuccess)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException {

        this.accessOneTime(accessParameters, onSuccess, null);

    }

    /**
     * Retrieves data from an Offering continuously as a feed. Lifecycle operations are performed on the returned
     * object. It is taken care that the feed setup is correctly according to the feed capabilities of the Offering.
     * Multiple calls e.g. with varying parameter set is supported in order to create multiple feeds for the same
     * Offering.
     */
    @Override
    public AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure)
            throws AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException, IOException {

        return accessContinuous(accessParameters, lifetimeMillis, Constants.syncFeedInterval.getMillis(), onSuccess,
                onFailure);

    }

    /**
     * Retrieves data from an Offering continuously as a feed where an interval has to be specified. Lifecycle
     * operations are performed on the returned object. It is taken care that the feed setup is correctly according to
     * the feed capabilities of the Offering. Multiple calls e.g. with varying parameter set is supported in order to
     * create multiple feeds for the same Offering.
     */
    @Override
    public AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis, long intervalMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure)
            throws AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        validate();

        switch (this.feedMode) {
        case ASYNC:
            throw new BridgeIoTException("Unsupported feed mode: " + feedMode);
        case SYNC:
            AccessFeed accessFeed = new AccessFeedSync(this, accessParameters, new Duration(lifetimeMillis),
                    new Duration(intervalMillis), onSuccess, onFailure);
            accessFeeds.add(accessFeed);
            return accessFeed;
        default:
            throw new BridgeIoTException("Unsupported feed mode: " + feedMode);
        }

    }

    /**
     * Returns a list of all feed subscriptions
     * 
     */
    @Override
    public List<AccessFeed> getMyAccessFeeds() {
        return accessFeeds;
    }

    /**
     * Unsubscribes an offering an deactivates automatic renewal.
     * 
     */
    @Override
    public void unsubscribe() {
        executorPool.submit(new Runnable() {
            @Override
            public void run() {
                unsubscribeBlocking();
            }
        });
    }

    /**
     * Unsubscribes an offering an deactivates automatic renewal.
     * 
     */
    @Override
    public void unsubscribeBlocking() {
        // unsubscribeAtMarketplace(); // TODO: Not yet supported
        consumer.removeSubscribedOffering(this);
        this.terminate();
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }

    protected void unsubscribeAtMarketplace() {
        String unsubscribe = GraphQLQueries.getUnsubscribtionString(this.offeringDescription.getId());
        logger.debug("Unsubscripe Request: {}", unsubscribe);

        try {

            Response response = this.consumer.getMarketplaceClient().request(unsubscribe);

            if (!response.isSuccessful()) {
                throw new BridgeIoTException(
                        String.format("Unsubscribe of Offering=%s failed!", this.offeringDescription.getId()));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Unsubscripe Response: {}", response.body().string());
            }

            response.close();
        } catch (IOException e) {
            logger.error("ERROR: Unsubscribe of Offering={} failed!", this.offeringDescription.getId());
            logger.error(e.getMessage(), e);
        }
    }

    protected static void validate(OfferingDescription offeringDescription)
            throws AccessToNonActivatedOfferingException {
        if ((offeringDescription == null) || (offeringDescription.getActivation() == null)
                || !offeringDescription.getActivation().getStatus()) {
            throw new AccessToNonActivatedOfferingException();
        }
    }

    protected void validate() throws AccessToNonActivatedOfferingException {
        validate(this.offeringDescription);
    }

    @Override
    protected void terminate() {
        super.terminate();
        if (executorPool != null) {
            executorPool.shutdownNow();
        }
        for (AccessFeed feed : accessFeeds) {
            if (!feed.getStatus().isTerminated()) {
                feed.stop();
            }
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

    // Needed to force termination of all executor threads (even if the Consumer/Provider does not call the .terminate()
    // method)
    @Override
    protected void finalize() throws Throwable {
        try {
            terminate();
        } finally {
            super.finalize();
        }
    }

}
