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
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * AccessFeed represents data feed and brings functionality for lifecycle management
 * 
 *
 */
public abstract class AccessFeed implements IAccessFeed {

    protected DateTime expirationDate;
    protected FeedNotificationSuccessHandler onSuccess;
    protected FeedNotificationFailureHandler onFailure;

    public AccessFeed(Duration lifetime, FeedNotificationSuccessHandler onSuccess,
            FeedNotificationFailureHandler onFailure) {
        super();
        this.expirationDate = DateTime.now().plus(lifetime);
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    public AccessFeed(FeedNotificationSuccessHandler onSuccess, FeedNotificationFailureHandler onFailure) {
        super();
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    /**
     * Expiration data of data feed
     * 
     */
    @Override
    public DateTime getExpirationDate() {
        return expirationDate;
    }

}
