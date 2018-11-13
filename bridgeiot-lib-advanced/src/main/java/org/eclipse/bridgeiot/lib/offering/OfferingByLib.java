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
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.misc.HttpClient;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;

/**
 * Implementation for Offering running in Integration mode 1,2 and 4. The A1 interface is provided by the Bridge.IoT
 * provider Lib. Accordingly the transformation of the call semantics to the legacy platform semantics is performed by
 * the Provider Lib.
 */
public class OfferingByLib extends Offering {

    private HttpClient httpClient;
    private OfferingCore offeringCore;

    protected OfferingByLib() {
        super();
    }

    protected OfferingByLib(SubscribableOfferingDescription offeringDescription, String offeringToken)
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        super(offeringDescription, null);
        final OfferingCoreByLib offeringCoreByLib = new OfferingCoreByLib(offeringDescription, offeringToken);
        this.httpClient = offeringCoreByLib.getHttpClient();
        this.offeringCore = offeringCoreByLib;
    }

    /**
     * Retrieves data from an Offering in a request/response manner asynchronously. Since the return value is a
     * CompletableFuture, a blocking behavior can be achieved on calling .get() on the return value.
     */
    @Override
    public CompletableFuture<AccessResponse> accessOneTime(AccessParameters accessParameters) {

        final String offeringAccessToken = offeringCore.getOfferingToken();

        return CompletableFuture.supplyAsync(() -> {

            final String accessSessionId = String.valueOf(new Date().getTime());
            final String responseString = OfferingCoreByLib.accessOneTimeInternal(httpClient, offeringDescription,
                    accessParameters, accessSessionId, offeringAccessToken);
            offeringCore.addAccountingEvent(accessSessionId, responseString);
            return new AccessResponse(responseString, offeringDescription);
        });

    }

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     */
    @Override
    public void accessOneTime(AccessParameters accessParameters, AccessResponseSuccessHandler onSuccess,
            AccessResponseFailureHandler onFailure) throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        offeringCore.accessOneTime(accessParameters, onSuccess, onFailure);
    }

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     *
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
     *
     */
    @Override
    public AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure)
            throws AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException, IOException {
        return offeringCore.accessContinuous(accessParameters, lifetimeMillis, onSuccess, onFailure);
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
            throws AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException, IOException {
        return offeringCore.accessContinuous(accessParameters, lifetimeMillis, intervalMillis, onSuccess, onFailure);
    }

    @Override
    public void unsubscribe() {
        offeringCore.unsubscribe();
    }

    @Override
    public void unsubscribeBlocking() {
        offeringCore.unsubscribeBlocking();
    }

    /**
     * Returns a list of all feed subscriptions
     *
     */
    @Override
    public List<AccessFeed> getMyAccessFeeds() {
        return offeringCore.getMyAccessFeeds();
    }

    @Override
    public List<AccountingReport> getCurrentAccountingReports() {
        return offeringCore.getCurrentAccountingReports();
    }

    /**
     * Unsubscribes an offering an deactivates automatic renewal
     *
     */
    @Override
    public void terminate() {
        super.terminate();
        if (offeringCore != null) {
            offeringCore.terminate();
        }
    }

    @Override
    public void accessOneTimeWithSessionId(String accessSessionId, AccessParameters accessParameters,
            AccessResponseSuccessHandler onAccessSuccess, AccessResponseFailureHandler onAccessFailure)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException {
        offeringCore.accessOneTimeWithSessionId(accessSessionId, accessParameters, onAccessSuccess, onAccessFailure);
    }

    @Override
    public String getOfferingToken() {
        return offeringCore.getOfferingToken();
    }

}
