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

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import org.eclipse.bridgeiot.lib.Consumer;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Offering extends OfferingCore implements IOffering {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    protected LinkedList<AccessFeed> feeds;

    protected Offering() {

    }

    public Offering(SubscribableOfferingDescription offeringDescription, String offeringToken) {
        super(offeringDescription, offeringToken);
    }

    /**
     * Retrieves data from an Offering in a request/response manner asynchronously. Since the return value is a
     * CompletableFuture, a blocking behavior can be achieved on calling .get() on the return value.
     *
     */
    @Override
    public abstract CompletableFuture<AccessResponse> accessOneTime(AccessParameters accessParameters);

    /**
     * Returns corresponding Offering Description
     */
    @Override
    public SubscribableOfferingDescription getOfferingDescription() {
        return (SubscribableOfferingDescription) offeringDescription;
    }

}
