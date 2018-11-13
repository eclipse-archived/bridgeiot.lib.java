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
 * Denis Kramer     (Bosch Software Innovations GmbH)
 * Stefan Schmid    (Robert Bosch GmbH)
 * Andreas Ziller   (Siemens AG)
 */
package org.eclipse.bridgeiot.lib.offering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.HttpErrorException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.feed.AccessFeedSync;
import org.eclipse.bridgeiot.lib.feed.CoapAccessFeedAsync;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.misc.CoapClient;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.misc.HttpClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.offering.encoder.*;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.californium.core.CoapResponse;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfferingByExternal extends Offering {
    private static final Logger logger = LoggerFactory.getLogger(OfferingByExternal.class);

    protected EndPoint endPoint;
    private LinkedList<AccessFeed> accessFeeds = new LinkedList<>();

    public OfferingByExternal(SubscribableOfferingDescription offeringDescription) {
        super(offeringDescription, null);
        this.endPoint = (offeringDescription != null) ? offeringDescription.getEndpoints().get(0) : null;
    }

    private String doHttpRequest(EndPoint endpoint, String URL, String body) {
        String responseString = "";
        Response response = null;
        HttpClient httpClient;
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", endpoint.getContentType().toString());
        headers.put("Accept", endpoint.getAcceptType().toString());

        if (endPoint.isSecured()) {
            httpClient = HttpClient.createHttpsClient();
        } else {
            httpClient = HttpClient.createHttpClient();
        }

        try {
            if (endPoint.getEndpointType().isGet()) {
                response = httpClient.get(URL, headers);
            } else if (endPoint.getEndpointType().isPost()) {
                response = httpClient.post(URL, headers, body);
            } else if (endpoint.getEndpointType().isPut()) {
                response = httpClient.put(URL, headers, body);
            } else {
                throw new RuntimeException("Unsupported HTTP method: " + endPoint.getEndpointType().toString());
            }
        } catch (IOException e) {
            logger.error("Request " + endPoint.getEndpointType() + " failed with exception.", e);
            throw new RuntimeException("External offering request failed. ", e);
        }
        // Check 404
        try {
            responseString = response.body().string();
            response.body().close();
            if (!response.isSuccessful()) {
                throw new HttpErrorException(response.code(), responseString);
            }
            return responseString;
        } catch (IOException e) {
            logger.info("Access: HTTP operation failed.", e);
        } catch (HttpErrorException e) {
            logger.error("Access: HTTP error.", e);
        }
        throw new RuntimeException("Request for external offering failed.");
    }

    private String doCoapRequest(EndPoint endpoint, String url, String body) {
        CoapClient c = new CoapClient();
        CoapResponse res;
        if (endpoint.getEndpointType().isGet()) {
            res = c.doGet(url, endpoint.getAcceptType());
        } else if (endpoint.getEndpointType().isPost()) {
            res = c.doPost(url, endpoint.getAcceptType(), endpoint.getContentType(), body);
        } else if (endpoint.getEndpointType().isPut()) {
            res = c.doPut(url, endpoint.getAcceptType(), endpoint.getContentType(), body);
        } else {
            throw new RuntimeException("Unsupported CoAP method: " + endpoint.getEndpointType().toString());
        }
        if (res == null) {
            logger.error("CoAP request for external offering timed out.");
            throw new RuntimeException("CoAP request timed out.");
        }
        if (!res.isSuccess()) {
            logger.error("CoAP request for external offering returned error: {}", res.getCode().toString());
            throw new RuntimeException("CoAP request error.");
        }
        return res.getResponseText();
    }

    private String encodeURI(EndPoint ep, Map<String, Object> parametersMapAll) {
        String urlString = ep.getUri();
        Map<String, Object> parametersMapPath = AccessParameters.getParametersForEncoding(parametersMapAll,
                offeringDescription.getInputs(), ParameterEncodingType.ROUTE);
        Map<String, Object> parametersMapQuery = AccessParameters.getParametersForEncoding(parametersMapAll,
                offeringDescription.getInputs(), ParameterEncodingType.QUERY);

        if (!parametersMapPath.isEmpty()) {
            if (!urlString.endsWith("/")) {
                urlString += "/";
            }
            ParameterEncoder encoder = new ParameterEncoderPath(urlString);
            urlString = encoder.encode(parametersMapPath);
            logger.debug("URL with path parameters: {}.", urlString);
        }

        if (!parametersMapQuery.isEmpty()) {
            ParameterEncoder encoder = new ParameterEncoderQuery();
            urlString = urlString + encoder.encode(parametersMapQuery);
            logger.debug("URL with query parameters: {}", urlString);
        }
        return urlString;
    }

    private String encodeBody(EndPoint ep, Map<String, Object> parametersMapAll) {
        Map<String, Object> parametersMapBody = AccessParameters.getParametersForEncoding(parametersMapAll,
                offeringDescription.getInputs(), ParameterEncodingType.BODY);
        Map<String, Object> parametersMapTemplate = AccessParameters.getParametersForEncoding(parametersMapAll,
                offeringDescription.getInputs(), ParameterEncodingType.TEMPLATE);

        if (!parametersMapBody.isEmpty() && !parametersMapTemplate.isEmpty()) {
            throw new RuntimeException(
                    "Cannot have body encoded  together with template encoded parameters in same message");
        }

        String body = null;

        if (!parametersMapBody.isEmpty()) {
            if (endPoint.getAcceptType().equals(MimeType.APPLICATION_JSON)) {
                ParameterEncoder encoder = new ParameterEncoderJson();
                body = encoder.encode(parametersMapBody);
                logger.debug("Request body (JSON): {}", body);
            } else if (endPoint.getAcceptType().equals(MimeType.APPLICATION_XML)) {
                ParameterEncoder encoder = new ParameterEncoderXml();
                body = encoder.encode(parametersMapBody);
                logger.debug("Request body (XML): {}", body);
            } else {
                throw new RuntimeException("Unsupported MIME type for parameter encoding in request body");
            }
        } else if (!parametersMapTemplate.isEmpty()) {
            ParameterEncoder encoder = new ParameterEncoderTemplate(offeringDescription.getRequestTemplates());
            body = encoder.encode(parametersMapTemplate);
            logger.debug("Request body (template): {}", body);
        }

        if (body != null && endPoint.getEndpointType().isGet()) {
            throw new RuntimeException("HTTP GET with request body not supported");
        }

        return body;
    }

    @Override
    public CompletableFuture<AccessResponse> accessOneTime(AccessParameters accessParameters) {
        logger.trace("Called accessOneTime. Parameters: {}", accessParameters);
        CompletableFuture<AccessResponse> responseFuture = CompletableFuture.supplyAsync(() -> {

            if (offeringDescription.getEndpoints() == null || offeringDescription.getEndpoints().size() != 1) {
                throw new RuntimeException("Number of endpoints have to be exactly 1");
            }
            logger.debug("Accessing URL {}.", endPoint.getUri());

            Map<String, Object> parametersMapAll = null;

            try {
                parametersMapAll = accessParameters != null
                        ? accessParameters.toNameMap(offeringDescription.getInputs(),
                                LibConfiguration.JSON_MAPPING_DEPTH)
                        : new HashMap<>();
            } catch (IllegalAccessParameterException e) {
                logger.warn("IllegalAccessParameterException" + (e.getMessage() != null ? e.getMessage() : ""));
                parametersMapAll = new HashMap<>();
            }
            logger.info("Accessing " + offeringDescription.getId() + " with "
                    + new ParameterEncoderJson().encode(parametersMapAll));

            String urlString = encodeURI(endPoint, parametersMapAll);
            String body = encodeBody(endPoint, parametersMapAll);

            logger.info("Sending " + endPoint.getEndpointType() + " request to " + urlString + " with content-type: "
                    + endPoint.getContentType().toString() + " accept type: " + endPoint.getAcceptType());
            String responseString;
            if (endPoint.getEndpointType().isHTTP()) {
                responseString = doHttpRequest(endPoint, urlString, body);
            } else if (endPoint.getEndpointType().isCOAP()) {
                responseString = doCoapRequest(endPoint, urlString, body);
            } else {
                throw new RuntimeException(
                        "unsupported endpoint type in integration mode 3: " + endPoint.getEndpointType().toString());
            }

            AccessResponse result = new AccessResponse(responseString, offeringDescription, endPoint.getContentType());
            if (offeringDescription.mapping != null) {
                try {
                    return result.remap(offeringDescription.mapping);
                } catch (IOException e) {
                    logger.error("Failed to remap access response.");
                    throw new RuntimeException(e);
                }
            } else {
                return result;
            }
        });

        return responseFuture;
    }

    @Override
    public void accessOneTime(AccessParameters accessParameters, AccessResponseSuccessHandler onSuccess,
            AccessResponseFailureHandler onFailure) throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {

        this.accessOneTime(accessParameters).thenAccept(r -> {
            try {
                onSuccess.processResponseOnSuccess(this, r);
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e.getMessage(), e);
            }
        }).exceptionally(e -> {
            onFailure.processResponseOnFailure(this, null);
            return null;
        });

    }

    @Override
    public void accessOneTime(AccessParameters accessParameters, AccessResponseSuccessHandler onSuccess)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException {
        this.accessOneTime(accessParameters, onSuccess, null);
    }

    @Override
    public AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure)
            throws IOException, AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        return this.accessContinuous(accessParameters, lifetimeMillis, Constants.syncFeedInterval.getMillis(),
                onSuccess, onFailure);

    }

    @Override
    public AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis, long intervalMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure) throws IOException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException, RuntimeException {
        AccessFeed accessFeed;
        if (endPoint.getEndpointType().isGet() && endPoint.getEndpointType().isCOAP()) {
            // CoAP observe is the same as a continuous get
            try {
                accessFeed = new CoapAccessFeedAsync(
                        new URI(encodeURI(endPoint,
                                accessParameters.toNameMap(offeringDescription.getInputs(),
                                        LibConfiguration.JSON_MAPPING_DEPTH))),
                        offeringDescription, Duration.millis(lifetimeMillis), onSuccess, onFailure);
            } catch (IllegalAccessParameterException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                logger.error("Invalid URI {} for endpoint.", endPoint.getUri(), e);
                throw new RuntimeException(e);
            }
        } else {
            // Otherwise, use periodic access.
            accessFeed = new AccessFeedSync(this, accessParameters, new Duration(lifetimeMillis),
                    new Duration(intervalMillis), onSuccess, onFailure);
        }
        accessFeeds.add(accessFeed);
        return accessFeed;
    }

    @Override
    public List<AccessFeed> getMyAccessFeeds() {
        return accessFeeds;
    }

    @Override
    public void unsubscribe() {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void unsubscribeBlocking() {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public List<AccountingReport> getCurrentAccountingReports() {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void accessOneTimeWithSessionId(String accessSessionId, AccessParameters accessParameters,
            AccessResponseSuccessHandler onAccessSuccess, AccessResponseFailureHandler onAccessFailure)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException {
        throw new RuntimeException("Not yet implemented");
    }

}
