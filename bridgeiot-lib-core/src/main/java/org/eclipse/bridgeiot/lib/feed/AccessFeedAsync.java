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

import org.eclipse.bridgeiot.lib.handlers.FeedNotificationFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.FeedNotificationSuccessHandler;
import org.joda.time.Duration;

/**
 * Implementation for asynchronous feed support. AccessFeedAsync represents data feed and brings functionality for
 * lifecycle management
 */
public class AccessFeedAsync extends AccessFeed {

    public AccessFeedAsync(Duration lifetime, FeedNotificationSuccessHandler onSuccess,
            FeedNotificationFailureHandler onFailure) {
        super(lifetime, onSuccess, onFailure);
    }

    /**
     * Explicitly stop data reception
     */
    @Override
    public void stop() {
    }

    /**
     * Returns status of Feed
     */
    @Override
    public FeedStatus getStatus() {
        return null;
    }

    /**
     * Set lifetime of feed with duration. After that the feed is automatically terminated.
     */
    @Override
    public void setLifetimeSeconds(long lifetimeSeconds) {
    }

    /**
     * Resumes a stopped data feed
     */
    @Override
    public void resume() {
    }

}
