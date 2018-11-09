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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.bridgeiot.lib.ConsumerCore;
import org.eclipse.bridgeiot.lib.IConsumer;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.feed.AccessFeed;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.security.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OfferingCore implements IOfferingCore {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerCore.class);

    protected SubscribableOfferingDescriptionCore offeringDescription;
    protected IConsumer consumer;
    protected LinkedList<AccessFeed> feeds;
    protected Accounting accounting;
    private String offeringToken;

    // scheduled executor to re-register the offering prior to expiration
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    protected OfferingCore() {
    }

    public OfferingCore(SubscribableOfferingDescriptionCore offeringDescription, String offeringToken) {
        this.offeringDescription = offeringDescription;
        this.feeds = new LinkedList<>();
        this.setOfferingToken(offeringToken);
        this.accounting = Accounting.create(offeringDescription.getId());
    }

    /**
     * Retrieves data from an Offering in a request/response manner asynchronously. Since the return value is a Future,
     * a blocking behavior can be achieved on calling .get() on the return value.
     * 
     */
    @Override
    public abstract Future<AccessResponse> accessOneTime(AccessParameters accessParameters)
            throws AccessToNonSubscribedOfferingException;

    public String getOfferingToken() {
        return this.offeringToken;
    }

    public void setOfferingToken(String offeringToken) {
        if (offeringToken != null) {
            this.offeringToken = offeringToken;
            // Create schedule for timely re-registration - to avoid expiration of the offering
            Long timeToResubscribe = Math
                    .max(AccessToken.getExpirationTime(offeringToken) - new Date().getTime() - 60000L, 1000);
            executor.schedule(subscriptionRunnable, timeToResubscribe, TimeUnit.MILLISECONDS);
        } else {
            // offeringToken will be NULL in the following situations:
            // 1. The initial subscribe to the offering failed
            // 2. When OfferingByLib is created it calls super() - i.e. OfferingCore - with an offeringToken = null
            // In case 2, nothing should happen (this happens by "design")
            // In case 1, we need to log the error, as this happens only if the subscription on marketplace failed
            if (this instanceof OfferingCoreByLib) {
                logger.error("ERROR: Offering subscription on Marketplace failed!");
            }
        }
    }

    public void setRenewedOfferingToken(String offeringToken) {
        // offeringToken will be NULL, if the re-subscription failed, e.g. due to Marketplace overload or restart.
        // Re-subscription will be attempted 30 seconds later.
        if (offeringToken != null) {
            setOfferingToken(offeringToken);
        } else {
            logger.error("Subscription failed - attempt to resubscribe in 30 seconds!");
            executor.schedule(subscriptionRunnable, 30000L, TimeUnit.MILLISECONDS); // attempt to resubscribe in 30
            // seconds
        }
    }

    private Runnable subscriptionRunnable = new Runnable() {
        @Override
        public void run() {
            final String offeringToken = (offeringDescription != null) ? offeringDescription.subscribeAtMarketplace()
                    : null;
            setRenewedOfferingToken(offeringToken);
        }
    };

    /**
     * Returns Offering Description
     * 
     */
    public SubscribableOfferingDescriptionCore getOfferingDescription() {
        return offeringDescription;
    }

    protected void addAccountingEvent(String accessSessionId, String str) {
        accounting.addEvent(offeringDescription.getSubscriptionId(), accessSessionId, str);
    }

    public List<AccountingReport> getCurrentAccountingReports() {
        return accounting.getCurrentReports();
    }

    // Compare with unsubscribe
    protected void terminate() {
        if (executor != null) {
            executor.shutdownNow();
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
