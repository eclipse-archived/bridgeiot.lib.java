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
package org.eclipse.bridgeiot.lib.templates;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.bridgeiot.lib.misc.FilloutTemplate;

/**
 * Template for Price Filter in Offering Query
 * 
 *
 */

public class OfferingQueryPriceTemplate extends FilloutTemplate {

    static final String DEFAULT_PRICE_TEMPLATE = "price: { money: { amount: %%amount, currency: %%currencyShort }, pricingModel: %%accountingModel }";

    public OfferingQueryPriceTemplate(final String amount, final String currencyShort, final String accountingModel) {
        super(DEFAULT_PRICE_TEMPLATE, createFilledMap(amount, currencyShort, accountingModel));
    }

    static Map<String, String> createFilledMap(final String amount, final String currencyShort,
            final String accountingModel) {
        Map<String, String> filledMap = new HashMap<>();
        filledMap.put("amount", amount);
        filledMap.put("currencyShort", currencyShort);
        filledMap.put("accountingModel", accountingModel);

        return filledMap;
    }

}
