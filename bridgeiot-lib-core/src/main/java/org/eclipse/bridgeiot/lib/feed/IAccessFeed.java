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

import org.joda.time.DateTime;

/**
 * Interface for lifecycle operations on data feeds
 * 
 */
public interface IAccessFeed {

    /**
     * Resumes a stopped data feed
     * 
     */
    void resume();

    /**
     * Explicitly stop data reception
     * 
     */
    void stop();

    /**
     * Set lifetime of data feed with duration. After that the feed is automatically terminated.
     * 
     */
    void setLifetimeSeconds(long lifetimeSeconds);

    /**
     * Expiration data of data feed
     * 
     */
    DateTime getExpirationDate();

    /**
     * Returns status of data feed
     * 
     */
    FeedStatus getStatus();

}