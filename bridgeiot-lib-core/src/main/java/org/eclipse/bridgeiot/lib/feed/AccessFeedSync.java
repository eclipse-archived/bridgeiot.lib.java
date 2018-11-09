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
package org.eclipse.bridgeiot.lib.feed;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.IOfferingCore;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for synchronous feed support
 * 
 *
 */
public class AccessFeedSync extends AccessFeed {

    private Duration interval;
    private ScheduledExecutorService accessExecutor;
    private ScheduledExecutorService terminationExecutor;
    private AccessParameters accessParameters;
    private String accessSessionId;
    private final IOfferingCore offering;

    private static final Logger logger = LoggerFactory.getLogger(AccessFeedSync.class);

    public AccessFeedSync(IOfferingCore offering, AccessParameters accessParameters, Duration lifetime,
            Duration interval, FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure) {

        super(lifetime, onSuccess, onFailure);
        this.interval = interval;
        this.accessParameters = accessParameters;
        this.offering = offering;
        this.accessSessionId = String.valueOf(new Date().getTime());
        this.scheduleExecutorWithTermination();
    }

    /**
     * Explicitly stop data reception
     * 
     */
    @Override
    public void stop() {
        logger.info("Stopping feed");
        accessExecutor.shutdown();
        this.terminationExecutor.shutdownNow();
    }

    @Override
    public FeedStatus getStatus() {

        return new FeedStatus(accessExecutor.isTerminated(), expirationDate, BridgeIotTypes.FeedTypes.SYNC,
                this.interval);
    }

    /**
     * Set lifetime of feed with duration. After that the feed is automatically terminated.
     * 
     */
    @Override
    public void setLifetimeSeconds(long lifetimeSeconds) {
        if (lifetimeSeconds < 0)
            throw new BridgeIoTException("Illegal lifetime value: " + lifetimeSeconds);

        expirationDate = DateTime.now().plus(lifetimeSeconds);
        // this.terminationExecutorFuture.cancel(true); Try this if the other
        // not working
        this.terminationExecutor.shutdownNow();
        this.terminationExecutor = Executors.newScheduledThreadPool(1);

        // this.terminationExecutorFuture =
        // this.terminationExecutor.schedule(()->this.accessExecutor.shutdown(),
        // expirationDate.getMillis()- DateTime.now().getMillis(),
        // TimeUnit.MILLISECONDS);
        final ScheduledExecutorService finalAccessExecutor = accessExecutor;
        this.terminationExecutor.schedule(new Callable<Integer>() {
            public Integer call() {
                finalAccessExecutor.shutdown();
                return 0;

            }
        }, expirationDate.getMillis() - DateTime.now().getMillis(), TimeUnit.MILLISECONDS);
        if (logger.isInfoEnabled()) {
            logger.info("Feed expiration date is now set to " + Helper.formatDate("H:mm:ss", expirationDate));
        }

    }

    /**
     * Resumes a stopped data feed
     * 
     */
    @Override
    public void resume() {
        if (this.accessExecutor.isTerminated() && expirationDate.getMillis() - DateTime.now().getMillis() > 10) {
            logger.info("Resuming feed");
            this.scheduleExecutorWithTermination();
        } else {
            logger.info("Cannot resume a terminated feed subscription");
        }

        // this.accessExecutor.scheduleAtFixedRate(()->offering.accessOneTime(accessParameters,
        // (q,r)-> this.onSuccess.processNotificationOnSuccess(this,r), (q,r)->
        // this.onFailure.processNotificationOnFailure(this,null)), 0,
        // interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void scheduleExecutorWithTermination() {
        if (terminationExecutor != null && !terminationExecutor.isTerminated())
            terminationExecutor.shutdown();
        if (terminationExecutor != null && !accessExecutor.isTerminated())
            accessExecutor.shutdown();
        if (expirationDate.getMillis() - DateTime.now().getMillis() > 0) {
            this.accessExecutor = Executors.newScheduledThreadPool(1);
            // this.accessExecutor.scheduleAtFixedRate(()->this.offering.accessOneTime(accessParameters,
            // (q,r)-> this.onSuccess.processNotificationOnSuccess(this,r),
            // (q,r)-> this.onFailure.processNotificationOnFailure(this,null)),
            // 0, interval.getMillis(), TimeUnit.MILLISECONDS);
            // this.accessExecutor.scheduleAtFixedRate(()->offering.accessOneTime(accessParameters, (q,r)->
            // this.onSuccess.processNotificationOnSuccess(this,r), (q,r)->
            // this.onFailure.processNotificationOnFailure(this,null)), 0, interval.toMillis(), TimeUnit.MILLISECONDS);
        }

        this.accessExecutor.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    offering.accessOneTimeWithSessionId(accessSessionId, accessParameters,
                            new AccessResponseSuccessHandler() {

                                @Override
                                public void processResponseOnSuccess(IOfferingCore reference, AccessResponse response)
                                        throws InterruptedException, ExecutionException {
                                    onSuccess.processNotificationOnSuccess(AccessFeedSync.this, response);
                                }
                            }, new AccessResponseFailureHandler() {

                                @Override
                                public void processResponseOnFailure(IOfferingCore reference, AccessResponse response) {
                                    onFailure.processNotificationOnFailure(AccessFeedSync.this, null);
                                }
                            });
                } catch (IllegalAccessParameterException | AccessToNonSubscribedOfferingException
                        | AccessToNonActivatedOfferingException e) {
                    logger.error(e.getMessage(), e);
                }

            }
        }, 0, interval.getMillis(), TimeUnit.MILLISECONDS);

        this.terminationExecutor = Executors.newScheduledThreadPool(1);
        // this.terminationExecutorFuture =
        // this.terminationExecutor.schedule(()->this.accessExecutor.shutdown(),
        // expirationDate.getMillis()- DateTime.now().getMillis(),
        // TimeUnit.MILLISECONDS);
        this.terminationExecutor.schedule(new Callable<Integer>() {
            public Integer call() {
                accessExecutor.shutdown();
                return 0;
            }
        }, expirationDate.getMillis() - DateTime.now().getMillis(), TimeUnit.MILLISECONDS);
        if (logger.isInfoEnabled()) {
            logger.info("Feed will terminate at " + Helper.formatDate("yyyy-MM-dd HH:mm:ss", expirationDate));
        }

        // this.terminationExecutor.schedule(()->this.accessExecutor.shutdown(),
        // expirationDate.getTime()-new Date().getTime(),
        // TimeUnit.MILLISECONDS);

    }

}
