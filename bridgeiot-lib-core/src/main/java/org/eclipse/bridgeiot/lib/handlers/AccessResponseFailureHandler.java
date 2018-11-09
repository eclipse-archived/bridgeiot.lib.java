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
package org.eclipse.bridgeiot.lib.handlers;

import org.eclipse.bridgeiot.lib.offering.AccessResponse;
import org.eclipse.bridgeiot.lib.offering.IOfferingCore;

/**
 * Handles access failure
 * 
 *
 */
@FunctionalInterface
public interface AccessResponseFailureHandler {

    /**
     * Processes failure on Offering access
     * 
     * @param reference
     *            Reference to offering
     * @param response
     *            Response from Offering
     */
    public void processResponseOnFailure(IOfferingCore reference, AccessResponse response);

}
