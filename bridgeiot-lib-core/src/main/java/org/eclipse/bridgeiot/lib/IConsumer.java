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
package org.eclipse.bridgeiot.lib;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseErrorHandler;
import org.eclipse.bridgeiot.lib.handlers.DiscoverResponseHandler;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.offering.OfferingCore;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.query.IOfferingQuery;

/**
 * Enables basic lifecycle operations on offerings for consumer. Consumer foresees discovery operations for offering
 * descriptions on the marketplace.
 *
 * IConsumer foresees discover operations for Offering Descriptions on the marketplace.
 * 
 * The interface functions are all non-blocking either by returning a completable future or passing delegate objects for
 * callback. CompletableFuture allows stream processing and reactive operations on the Type of the nested Object in the
 * CompletableFuture. Calling .get() returns the nested object in a synchronized manner. The call blocks until the
 * operation is finished. Alternatively method calls foresee callback via delegate objects. The relevant delegate types
 * are defined as functional interfaces, which allow lambda expression.
 * 
 *
 */
public interface IConsumer {

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     * @throws IOException
     */
    ConsumerCore authenticate(String clientSecret) throws IOException;

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking as it
     * returns the list of offering descriptions as a future.
     * 
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    <T extends OfferingDescription> Future<List<T>> discoverFuture(IOfferingQuery offeringQuery) throws IOException;

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking. On
     * successful execution, onSuccess receives the result via executing its callback method. On failure the respective
     * callback method of onFailure is called.
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @param onSuccess
     *            Delegate object for processing successful discoveries
     * @param onFailure
     *            Delegate object for failing discoveries
     */
    void discover(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccess,
            DiscoverResponseErrorHandler onFailure) throws IncompleteOfferingQueryException;

    /**
     * Performs a marketplace discovery for Offerings matching the implementation as Offering Query. The call is
     * non-blocking. On successful execution, onSuccess receives the result via executing its callback method. On
     * failure nothing is done.
     * 
     * @param offeringQuery
     *            Offering query used for discover
     * @param onSuccess
     *            Delegate object for processing successful discoveries
     */
    void discover(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccess)
            throws IncompleteOfferingQueryException;

    /**
     * Performs a marketplace discovery for Offerings according to the offering query. The call is non-blocking as it
     * returns the list of offering descriptions as a future.
     *
     * @param offeringQuery
     *            Offering query used for discover
     * @return List of found Offering Descriptions matching Offering Query as CompletableFuture.
     */
    Future<List<SubscribableOfferingDescriptionCore>> discoverByIdFuture(final String queryId) throws IOException;

    /**
     * Discover offerings regularly using a fixed time interval.
     * 
     * @param offeringQuery
     * @param onSuccess
     * @throws IncompleteOfferingQueryException
     */
    void discoverContinous(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccess,
            DiscoverResponseErrorHandler onFailure) throws IncompleteOfferingQueryException;

    /**
     * Discover offerings regularly using a specified time interval.
     * 
     * @param offeringQuery
     * @param onSuccess
     * @param discoverInterval
     * @throws IncompleteOfferingQueryException
     */
    void discoverContinous(IOfferingQuery offeringQuery, DiscoverResponseHandler onSuccess,
            DiscoverResponseErrorHandler onFailure, int discoverInterval) throws IncompleteOfferingQueryException;

    /**
     * Stop continuous discovery of offerings.
     * 
     * @param offeringQuery
     */
    void stopDiscoverContinuous(IOfferingQuery offeringQuery);

    // /** Subscription to the Offering on the Marketplace.
    // *
    // */
    // String subscribe(OfferingDescriptionCore offeringDescription);
    // String subscribe(String offeringId);

    // /** Terminates the subscription to the Offering on the Marketplace.
    // * Note: Don't confuse it with a termination of a subscription to a feed. It is performed on the specific feed
    // object.
    // *
    // */
    // void unsubscribe(OfferingDescriptionCore offeringDescription);
    // void unsubscribe(String offeringId);

    public void addSubscribedOffering(OfferingCore subscribedOffering);

    public void removeSubscribedOffering(OfferingCore subscribedOffering);

    /**
     * Terminates Consumer session on marketplace
     */
    void terminate();

    /**
     * Get client Id
     * 
     * @return client Id
     */
    BridgeIotClientId getClientId();

    // TODO remove visibility
    String getProviderCertFile();

    MarketplaceClient getMarketplaceClient();

}