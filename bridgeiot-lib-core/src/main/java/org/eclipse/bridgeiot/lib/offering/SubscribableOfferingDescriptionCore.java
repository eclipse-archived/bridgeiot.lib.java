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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.IConsumer;
import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OfferingDescription is a subscribable extension of an OfferingDescriptionData. In order to create the access object
 * (Offering) for an Offering Description this class provides the subscribe() method
 */
public class SubscribableOfferingDescriptionCore extends OfferingDescription {

    private ExecutorService executorPool = Executors.newFixedThreadPool(LibConfiguration.EXECUTOR_POOL_SIZE); // for
                                                                                                              // futures

    ObjectMapper mapper = new ObjectMapper();

    protected IConsumer consumer = null;
    protected String queryId = null;
    protected String subscriptionId = null;

    private static final Logger logger = LoggerFactory.getLogger(SubscribableOfferingDescriptionCore.class);

    protected SubscribableOfferingDescriptionCore() {
        super();
    }

    public SubscribableOfferingDescriptionCore(IConsumer consumer) {
        super();
        this.setConsumer(consumer);
    }

    public static SubscribableOfferingDescriptionCore create(IConsumer consumer) {
        return new SubscribableOfferingDescriptionCore(consumer);
    }

    /**
     * Checks whether a parameter set is a valid input parameter set
     * 
     * @param parameters
     * @return
     */

    protected boolean isPojoAValidParameterObject(Object parameters) {

        // Check via introspection whether parameters match defined
        // inputParameters

        return true;
    }

    /**
     * Subscribe to Offering and activate automatic renewal.
     * 
     * @return
     */
    public Future<OfferingCore> subscribeFuture() {
        return executorPool.submit(new Callable<OfferingCore>() {
            @Override
            public OfferingCore call() throws IllegalEndpointException, IncompleteOfferingDescriptionException {
                return subscribeBlocking();
            }
        });
    }

    /**
     * Subscribe to Offering and activate automatic renewal.
     * 
     * @return
     * @throws IncompleteOfferingDescriptionException
     * @throws IllegalEndpointException
     */
    public OfferingCore subscribeBlocking() throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        final String offeringToken = subscribeAtMarketplace();
        switch (getAccessInterfaceType()) {
        case BRIDGEIOT_LIB:
        case BRIDGEIOT_PROXY:
            OfferingCoreByLib subscribedOffering = new OfferingCoreByLib(this, offeringToken);
            consumer.addSubscribedOffering(subscribedOffering);
            return subscribedOffering;
        case EXTERNAL:
            throw new BridgeIoTException("Unsupported integration mode");
        default:
            throw new BridgeIoTException(
                    "Cannot create Offering Access Object due to unsupported or unspecified integration mode");
        }
    }

    /**
     * Pretty print of list of Offering Description
     * 
     * @param l
     * @return
     */
    public static <T extends OfferingDescription> List<T> showOfferingDescriptions(List<T> l) {
        if (logger.isInfoEnabled()) {
            logger.info(showOfferingDescriptionsString(l));
        }
        return l;
    }

    /**
     * Pretty print of list of Offering Description
     * 
     * @param l
     * @return
     */
    public static <T extends OfferingDescription> String showOfferingDescriptionsString(List<T> l) {
        return Helper.showOfferingDescriptions(l, false);
    }

    /**
     * Pretty print of list of Offering Description
     * 
     * @param l
     * @return
     */
    public static <T extends OfferingDescription> List<T> showOfferingDescriptionsDetailed(List<T> l) {
        if (logger.isInfoEnabled()) {
            logger.info(Helper.showOfferingDescriptions(l, true));
        }
        return l;
    }

    public void setConsumer(IConsumer consumer) {
        this.consumer = consumer;
    }

    public IConsumer getConsumer() {
        return this.consumer;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryId() {
        return this.queryId;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    protected String subscribeAtMarketplace() {

        String accessToken = null;

        final String queryName;
        final String subscriptionRequest;
        if (this.getQueryId() != null) {
            subscriptionRequest = GraphQLQueries.getSubscribtionWithQueryString(this.getId(), this.getQueryId());
            queryName = "subscribeQueryToOffering";
        } else {
            subscriptionRequest = GraphQLQueries.getSubscribtionWithConsumerString(this.getId(),
                    this.getConsumer().getClientId().asString());
            queryName = "subscribeConsumerToOffering";
        }
        logger.info("Subscription Request: {}", subscriptionRequest);

        try {

            Response response = this.consumer.getMarketplaceClient().request(subscriptionRequest);

            String responseString = response.body().string();
            response.close();

            Subscription subscription = Helper.unmarshallSingleFromQueryResponse(queryName, responseString,
                    Subscription.class);

            if (subscription.getId().contains(this.getId())) {
                this.subscriptionId = subscription.getId();
                accessToken = subscription.getAccessToken();
            }

            if (accessToken == null) {
                throw new BridgeIoTException(
                        "Subscription failed - no valid OfferingAccessToken obtained from Marketplace!");
            }

            logger.info("You are subscribed to {}", this.getId());
            logger.debug("OfferingAccessToken (for OfferingId={}) is: {}", this.getId(), accessToken);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            logger.error("Subscription failed!");
        }

        return accessToken;

    }

    /**
     * Terminates automatic renewal and implicitly unsubscribes offering
     */
    public void terminate() {
        if (executorPool != null) {
            executorPool.shutdownNow();
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
