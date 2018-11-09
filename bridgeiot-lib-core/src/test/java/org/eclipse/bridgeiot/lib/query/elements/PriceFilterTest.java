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
package org.eclipse.bridgeiot.lib.query.elements;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.Money;
import org.eclipse.bridgeiot.lib.model.Price;
import org.eclipse.bridgeiot.lib.query.elements.PriceFilter.PriceFilterMax;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriceFilterTest {

    private static final Logger logger = LoggerFactory.getLogger(PriceFilterTest.class);

    @Test
    public void testPriceFilterMaxQuery() {
        Money money = new Money(1000.01, Money.EURO);
        Price price = new Price(money, PricingModel.PER_MONTH);
        PriceFilterMax filterUnderTest = new PriceFilterMax(price);

        String query = filterUnderTest.toQueryElement();
        assertThat(query).contains("price").contains("money").contains("amount").contains("1000.01")
                .contains("currency").contains(Money.EURO).contains("pricingModel")
                .contains(PricingModel.PER_MONTH.name());

        logger.info(query);
    }

    @Test
    public void testPriceFilterMaxQuery2() {
        Price price = new Price();
        price.setAmount(2000000000000000000000000.0);
        price.setCurrency("Bitcoin");
        price.setPricingModel(PricingModel.PER_MESSAGE);
        PriceFilterMax filterUnderTest = new PriceFilterMax(price);

        String query = filterUnderTest.toQueryElement();
        assertThat(query).contains("price").contains("money").contains("amount").contains("2000000000000000000000000")
                .contains("currency").contains("Bitcoin").contains("pricingModel")
                .contains(PricingModel.PER_MESSAGE.name());

        logger.info(query);
    }

}
