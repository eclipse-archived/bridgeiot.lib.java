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

import org.eclipse.bridgeiot.lib.ConsumerCore;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements IOfferingQuery. It leaves the implementation of query string generation
 * (toOfferingQueryString()) open.
 * 
 *
 */
public abstract class OfferingQuery implements IOfferingQuery {

    protected String id;

    private static final Logger logger = LoggerFactory.getLogger(ConsumerCore.class);

    public OfferingQuery() {
    }

    public static OfferingQueryChain create() throws IncompleteOfferingQueryException {
        return create("myDefaultQueryId");
    }

    /**
     * Factory methods for incremental Offering Query creation
     * 
     * @param consumerId
     *            Consumer ID
     * @return
     * @throws IncompleteOfferingQueryException
     */
    public static OfferingQueryChain create(String localId) throws IncompleteOfferingQueryException {
        validate(localId);
        return new OfferingQueryChain(localId);
    }

    protected static void validate(String localId) throws IncompleteOfferingQueryException {
        if (localId == null) {
            throw new IncompleteOfferingQueryException();
        }

    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

}