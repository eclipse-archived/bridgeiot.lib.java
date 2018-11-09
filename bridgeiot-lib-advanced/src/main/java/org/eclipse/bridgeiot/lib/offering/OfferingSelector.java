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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfferingSelector {

    LinkedList<SelectionCriteria> orderedCriteria = new LinkedList<>();

    static final Logger logger = LoggerFactory.getLogger(OfferingSelector.class);

    public static OfferingSelector create() {
        return new OfferingSelector();
    }

    public OfferingSelector cheapest() {
        orderedCriteria.add(new SelectionCriteria.Cheapest());
        return this;
    }

    public OfferingSelector mostPermissive() {
        orderedCriteria.add(new SelectionCriteria.MostPermissive());
        return this;
    }

    public OfferingSelector onlyLocalhost() {
        orderedCriteria.add(new SelectionCriteria.OnlyLocalhost());
        return this;
    }

    public SubscribableOfferingDescription select(List<SubscribableOfferingDescription> initialSet) {
        // if(initialSet== null) return null;

        List<SubscribableOfferingDescription> remainingOfferingDescriptions = new LinkedList<>();

        for (final SubscribableOfferingDescription subscribableOfferingDescription : initialSet) {
            remainingOfferingDescriptions.add(subscribableOfferingDescription);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Applying selection criteria on {} offering description{}",
                    remainingOfferingDescriptions.size(), (remainingOfferingDescriptions.size() > 1 ? "s" : ""));
        }

        for (final SelectionCriteria criterion : orderedCriteria) {
            if (remainingOfferingDescriptions.isEmpty() || remainingOfferingDescriptions.size() == 1) {
                break;
            }

            remainingOfferingDescriptions = criterion.filter(remainingOfferingDescriptions);
            logger.info("After applying {} criterion {} offering descriptions remaining", criterion,
                    remainingOfferingDescriptions.size());

        }

        if (remainingOfferingDescriptions.isEmpty()) {
            logger.info("No matching offering description found");
            return null;
        } else if (remainingOfferingDescriptions.size() == 1) {
            logger.info("Found exactly one matching offering description");
        } else {
            logger.info("Found {} matching offering descriptions", remainingOfferingDescriptions.size());
        }
        // Assume that all remaining element in selection are optimal w.r.t. the selection criteria:
        return remainingOfferingDescriptions.get(0);
    }

}
