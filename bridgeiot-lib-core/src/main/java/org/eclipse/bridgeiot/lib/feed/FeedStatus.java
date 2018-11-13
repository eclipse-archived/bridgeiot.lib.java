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

import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.FeedTypes;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * FeedStatus represents the status of a data feed
 * 
 */
public class FeedStatus {

    private FeedTypes feedType;
    private DateTime expirationDate;
    private Duration interval;
    private boolean terminated;

    public FeedStatus(boolean terminated, DateTime expirationDate, FeedTypes feedType, Duration interval) {
        super();
        this.terminated = terminated;
        this.expirationDate = expirationDate;
        this.feedType = feedType;
        this.interval = interval;
    }

    public FeedTypes getFeedType() {
        return feedType;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public Duration getInterval() {
        return interval;
    }

    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public String toString() {
        return "FeedStatus [feedType=" + feedType + ", expirationDate=" + Helper.formatDate("H:mm:ss", expirationDate)
                + ", interval=" + interval.toString() + ", terminated=" + terminated + "]";
    }

}
