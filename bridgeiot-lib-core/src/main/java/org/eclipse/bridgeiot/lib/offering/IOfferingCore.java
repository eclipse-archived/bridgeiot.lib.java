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
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;

/**
 * Interface for an Offering implementation. It provides access to the operations of the Offering
 */
public interface IOfferingCore {

    /**
     * Retrieves data from an Offering in a request/response manner asynchronously. Since the return value is a Future,
     * a blocking behavior can be achieved on calling .get() on the return value.
     * 
     * @param accessParameters
     * @return
     * @throws AccessToNonActivatedOfferingException
     * @throws AccessToNonSubscribedOfferingException
     */

    Future<AccessResponse> accessOneTime(AccessParameters accessParameters)
            throws AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException;

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     * 
     * @param accessParameters
     * @param onSuccess
     * @param onFailure
     * @throws IllegalAccessParameterException
     * @throws AccessToNonActivatedOfferingException
     * @throws AccessToNonSubscribedOfferingException
     */

    void accessOneTime(AccessParameters accessParameters, AccessResponseSuccessHandler onSuccess,
            AccessResponseFailureHandler onFailure) throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException;

    /**
     * Retrieves data from an Offering in a request/response manner. Method call is nonblocking.
     * 
     * @param accessParameters
     * @param onSuccess
     * @throws IllegalAccessParameterException
     * @throws AccessToNonActivatedOfferingException
     * @throws AccessToNonSubscribedOfferingException
     */

    void accessOneTime(AccessParameters accessParameters, AccessResponseSuccessHandler onSuccess)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException;

    void accessOneTimeWithSessionId(String accessSessionId, AccessParameters accessParameters,
            AccessResponseSuccessHandler onAccessSuccess, AccessResponseFailureHandler onAccessFailure)
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException;

    /**
     * Retrieves data from an Offering continuously as a feed. Lifecycle operations are performed on the returned
     * object. It is taken care that the feed setup is correctly according to the feed capabilities of the Offering.
     * Multiple calls e.g. with varying parameter set is supported in order to create multiple feeds for the same
     * Offering.
     * 
     * @param accessParameters
     * @param lifetimeMillis
     * @param onSuccess
     * @param onFailure
     * @return
     * @throws IOException
     * @throws AccessToNonActivatedOfferingException
     * @throws AccessToNonSubscribedOfferingException
     */
    AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure)
            throws IOException, AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException;

    /**
     * Retrieves data from an Offering continuously as a feed where an interval has to be specified. Lifecycle
     * operations are performed on the returned object. It is taken care that the feed setup is correctly according to
     * the feed capabilities of the Offering. Multiple calls e.g. with varying parameter set is supported in order to
     * create multiple feeds for the same Offering.
     * 
     * @param accessParameters
     * @param lifetimeMillis
     * @param intervalMillis
     * @param onSuccess
     * @param onFailure
     * @return
     * @throws IOException
     * @throws AccessToNonActivatedOfferingException
     * @throws AccessToNonSubscribedOfferingException
     */
    AccessFeed accessContinuous(AccessParameters accessParameters, long lifetimeMillis, long intervalMillis,
            FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure)
            throws IOException, AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException;

    /**
     * Returns a list of all feed subscriptions
     * 
     * @return List of feed subscriptions
     */
    List<AccessFeed> getMyAccessFeeds();

    /**
     * Unsubscribes an offering an deactivates automatic renewal.
     */
    void unsubscribe();

    /**
     * Unsubscribes an offering an deactivates automatic renewal.
     */
    void unsubscribeBlocking();

}
