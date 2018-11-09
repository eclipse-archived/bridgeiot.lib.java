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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.junit.Test;

public class OfferingQueryTest {

    @Test
    public void testIfferingQueryCreation() throws IncompleteOfferingQueryException {
        // Arrange
        OfferingQueryChain offeringQuery = createOfferingQueryFixture();

        // Act
        String queryAsString = offeringQuery.toString();

        // Assert
        assertThat(queryAsString).contains("TemperatureQuery").contains("Barcelona").contains("license")
                .contains(LicenseType.OPEN_DATA_LICENSE.toString()).contains("price").contains("money")
                .contains("0.002").contains("EUR").contains(BridgeIotTypes.PricingModel.PER_ACCESS.toString());
    }

    @Test(expected = IncompleteOfferingQueryException.class)
    public void testIfferingQueryCreationIncompleteOfferingQueryException() throws IncompleteOfferingQueryException {
        // Arrange
        // nothing to do

        // Act
        OfferingQuery.create(null);

        // Assert
        // see expected exception
    }

    OfferingQueryChain createOfferingQueryFixture() throws IncompleteOfferingQueryException {
        return OfferingQuery.create("TemperatureQuery").withName("Temperature sensor query")
                .withCategory("schema:temperature").inRegion("Barcelona")
                .withPricingModel(BridgeIotTypes.PricingModel.PER_ACCESS).withMaxPrice(Euros.amount(0.002))
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE);
    }

}
