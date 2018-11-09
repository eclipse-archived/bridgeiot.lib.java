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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SelectionCriteria {

    static final Logger logger = LoggerFactory.getLogger(SelectionCriteria.class);

    public static class OnlyLocalhost extends SelectionCriteria {

        @Override
        public List<SubscribableOfferingDescription> filter(
                List<SubscribableOfferingDescription> offeringDescriptions) {
            return offeringDescriptions.stream().filter(
                    e -> !e.getEndpoints().isEmpty() && (e.getEndpoints().get(0).getUri().contains("//localhost")
                            || e.getEndpoints().get(0).getUri().contains("//127.0.0.1")))
                    .collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return "only on localhost";
        }

    }

    public static class MostPermissive extends SelectionCriteria {

        static List<String> licenseOrder = Arrays.asList(LicenseType.CREATIVE_COMMONS.toString(),
                LicenseType.OPEN_DATA_LICENSE.toString(), LicenseType.OPEN_DATA_LICENSE.toString());

        @Override
        public List<SubscribableOfferingDescription> filter(
                List<SubscribableOfferingDescription> offeringDescriptions) {
            if (offeringDescriptions.isEmpty()) {
                return offeringDescriptions;
            }

            boolean first = true;

            int lowest = -1;

            for (final OfferingDescription offeringDescription : offeringDescriptions) {
                if (offeringDescription.getLicense() != null) {
                    final int index = licenseOrder.contains(offeringDescription.getLicense().toString())
                            ? licenseOrder.indexOf(offeringDescription.getLicense().toString())
                            : licenseOrder.size() - 1;

                    if (first || index < lowest) {
                        lowest = index;
                        first = false;
                    }
                }

            }

            if (!first) {
                final int chosenValue = lowest;
                return offeringDescriptions.stream()
                        .filter(e -> licenseOrder.indexOf(e.getLicense().toString()) == chosenValue)
                        .collect(Collectors.toList());
            } else {
                logger.error("Cannot select w.r.t. license as value not set in offering descriptions");
                return offeringDescriptions;
            }

        }

        @Override
        public String toString() {
            return "most permissive license";
        }

    }

    public static class Cheapest extends SelectionCriteria {

        @Override
        public List<SubscribableOfferingDescription> filter(
                List<SubscribableOfferingDescription> offeringDescriptions) {
            if (offeringDescriptions.isEmpty()) {
                return offeringDescriptions;
            }

            boolean first = true;

            double lowest = -1;

            for (final OfferingDescription offeringDescription : offeringDescriptions) {
                if (offeringDescription.getPrice() != null && offeringDescription.getPrice().getMoney() != null) {
                    if (first) {
                        lowest = offeringDescription.getPrice().getAmount();
                        first = false;
                        continue;
                    }
                    lowest = offeringDescription.getPrice().getAmount() < lowest
                            ? offeringDescription.getPrice().getAmount()
                            : lowest;
                }
            }

            if (!first) {
                final double chosenValue = lowest;
                return offeringDescriptions.stream().filter(e -> e.getPrice() != null && e.getPrice().getMoney() != null
                        && e.getPrice().getAmount() == chosenValue).collect(Collectors.toList());
            } else {
                logger.error("Cannot select w.r.t. price as value not set in offering descriptions");
                return offeringDescriptions;
            }
        }

        @Override
        public String toString() {
            return "cheapest price";
        }

    }

    public abstract List<SubscribableOfferingDescription> filter(List<SubscribableOfferingDescription> e);

}
