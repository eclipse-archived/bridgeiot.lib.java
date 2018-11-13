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

import java.util.concurrent.CompletableFuture;

import org.eclipse.bridgeiot.lib.IConsumer;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribableOfferingDescription extends SubscribableOfferingDescriptionCore {

    private static final Logger logger = LoggerFactory.getLogger(SubscribableOfferingDescription.class);

    protected SubscribableOfferingDescription() {
        super();
    }

    public SubscribableOfferingDescription(IConsumer consumer) {
        super(consumer);
    }

    public static SubscribableOfferingDescription create(IConsumer consumer) {

        return new SubscribableOfferingDescription(consumer);
    }

    public CompletableFuture<Offering> subscribe() {

        return CompletableFuture.supplyAsync(() -> {

            Offering offering = null;
            try {
                offering = subscribeBlocking();
            } catch (IllegalEndpointException | IncompleteOfferingDescriptionException e) {
                logger.error(e.getMessage(), e);
            }
            return offering;

        }).exceptionally(e -> {
            logger.error(e.getMessage(), e);
            if (e instanceof RuntimeException) {
            }
            return null;
        });
    }

    /**
     * Subscribe to Offering and activate automatic renewal.
     * 
     * @return
     * @throws IncompleteOfferingDescriptionException
     * @throws IllegalEndpointException
     */
    @Override
    public Offering subscribeBlocking() throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        final String offeringToken = subscribeAtMarketplace();
        switch (getAccessInterfaceType()) {
        case BRIDGEIOT_LIB:
        case BRIDGEIOT_PROXY:
            OfferingByLib subscribedOffering = new OfferingByLib(this, offeringToken);
            consumer.addSubscribedOffering(subscribedOffering);
            return subscribedOffering;
        case EXTERNAL:
            OfferingByExternal subscribedOfferingByExternal = new OfferingByExternal(this);
            consumer.addSubscribedOffering(subscribedOfferingByExternal);
            return subscribedOfferingByExternal;
        default:
            throw new RuntimeException(
                    "Cannot create Offering Access Object due to unsupported or unspecified integration mode");
        }
    }
}
