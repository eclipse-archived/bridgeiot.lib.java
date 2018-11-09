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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisteredOffering {

    protected RegistrableOfferingDescription registrableOfferingDescription;

    protected AccessStream offeringStream = null;
    protected HashMap<String, AccessStream> mapAccessStreams = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(RegisteredOffering.class);

    public RegisteredOffering(RegistrableOfferingDescription offeringDescription) {
        this.registrableOfferingDescription = offeringDescription;
    }

    /**
     * Get corresponding offering id to offering
     * 
     * @return
     */
    public OfferingId getOfferingId() {
        return new OfferingId(registrableOfferingDescription.getId());
    }

    /**
     * Get corresponding offering description
     * 
     */
    public RegistrableOfferingDescription getOfferingDescription() {
        return registrableOfferingDescription;
    }

    /**
     * Deregisters Offering from the marketplace
     * 
     */
    public void deregister() {
        registrableOfferingDescription.deregister();
    }

    /**
     * Queue JSON Object in Offering Stream
     * 
     * @return
     */
    public RegisteredOffering queue(JsonObject jsonObj) {
        if (offeringStream == null) {
            offeringStream = AccessStream.create();
        }

        // add new information record to offerStream (complete stream)
        offeringStream.offer(jsonObj);

        // add new information record to all active access streams
        Iterator<Entry<String, AccessStream>> it = mapAccessStreams.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, AccessStream> streamEntry = it.next();
            if (!streamEntry.getValue().hasExpired()) {
                streamEntry.getValue().offer(jsonObj);
            } else {
                logger.info("Access Stream removed: {}", streamEntry.getKey());
                it.remove();
            }
        }

        return this;
    }

    /**
     * Clear Offering Stream Queue
     * 
     * @return
     */
    public RegisteredOffering flush() {
        if (offeringStream == null) {
            offeringStream = AccessStream.create();
            return this;
        }

        // add new information records to offerStream (complete stream)
        offeringStream.flush();

        mapAccessStreams.clear();

        return this;
    }

    public RegisteredOffering clear() {
        return this.flush();
    }

}
