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

import java.net.URI;

import org.eclipse.bridgeiot.lib.handlers.FeedFailureException;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Jan Seeger on 08.11.2017.
 */
public class CoapAccessFeedAsync extends AccessFeed {
    private final URI uri;
    private final OfferingDescription off;
    private CoapObserveRelation rel;
    private boolean isRunning = false;
    private static final Logger logger = LoggerFactory.getLogger(CoapAccessFeedAsync.class);

    public CoapAccessFeedAsync(URI coapURI, OfferingDescription off, Duration duration,
            FeedNotificationSuccessHandler success, FeedNotificationFailureHandler failure) {
        super(duration, success, failure);
        this.uri = coapURI;
        this.off = off;
        resume();
    }

    @Override
    public synchronized void resume() {
        // Is already running.
        if (isRunning)
            return;
        CoapClient cl = new CoapClient(uri);
        rel = cl.observe(new CoapHandlerAdapter(this, off, this.onSuccess, this.onFailure));
        this.isRunning = true;
    }

    @Override
    public synchronized void stop() {
        if (!isRunning)
            return;
        rel.proactiveCancel();
        isRunning = false;
    }

    @Override
    public void setLifetimeSeconds(long lifetimeSeconds) {

    }

    @Override
    public synchronized FeedStatus getStatus() {
        // What *is* the interval of an asynchronous feed?
        return new FeedStatus(!isRunning, expirationDate, BridgeIotTypes.FeedTypes.ASYNC, Duration.ZERO);
    }

    private class CoapHandlerAdapter implements CoapHandler {

        private final IAccessFeed parent;
        private final OfferingDescription off;
        private final FeedNotificationFailureHandler fail;
        private final FeedNotificationSuccessHandler succ;

        public CoapHandlerAdapter(IAccessFeed parent, OfferingDescription off, FeedNotificationSuccessHandler succ,
                FeedNotificationFailureHandler fail) {
            this.parent = parent;
            this.off = off;
            this.succ = succ;
            this.fail = fail;
        }

        @Override
        public void onLoad(CoapResponse response) {
            AccessResponse resp = new AccessResponse(response.getResponseText(), off);
            try {
                succ.processNotificationOnSuccess(parent, resp);
            } catch (Exception e) {
                logger.error("Failed to send async feed success notification.", e);
            }
        }

        @Override
        public void onError() {
            logger.debug("Error while accessing CoAP offering.");
            fail.processNotificationOnFailure(parent, new FeedFailureException());
        }
    }
}
