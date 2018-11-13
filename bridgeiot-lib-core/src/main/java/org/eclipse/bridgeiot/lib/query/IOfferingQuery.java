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
package org.eclipse.bridgeiot.lib.query;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;

/**
 * Interface for an Offering Query.
 *
 */
public interface IOfferingQuery {

    /**
     * Compiles a Offering Query message accepted by the Marketplace
     * 
     * @param consumerId
     *            Consumer identifier issued by Marketplace
     * @return Offering Query string
     */
    String toOfferingQueryString(BridgeIotClientId consumerId);

    /**
     * Internal use only
     * 
     * @return Local Query ID string
     */
    String getLocalId();

    /**
     * Internal use only
     * 
     * @return Query ID string
     */
    String getId();

    /**
     * Internal use only
     * 
     * Set Query ID
     */
    void setId(String queryId);

    /**
     * Internal use only
     * 
     * Compare Query content
     * 
     * @return boolean (true if queries have the same content; false otherwise)
     */
    boolean sameQuery(IOfferingQuery query);

}