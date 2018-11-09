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

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.Price;
import org.eclipse.bridgeiot.lib.model.QueryElement;
import org.eclipse.bridgeiot.lib.templates.OfferingQueryPriceTemplate;

/**
 * Abstract price filter used for Offering Query
 * 
 */
public abstract class PriceFilter implements QueryElement {

    /**
     * Max pricefilter
     * 
     */
    public static class PriceFilterMax extends PriceFilter {

        Price price;

        public PriceFilterMax(Price max) {
            super();
            this.price = max;
        }

        public Price getMax() {
            return price;
        }

        @Override
        public String toQueryElement() {
            return new OfferingQueryPriceTemplate(Helper.df0_0000.format(price.getAmount()), price.getCurrency(),
                    price.getPricingModel().name()).fillout();
        }

        @Override
        public void setPricingModel(PricingModel pricingModel) {
            price = new Price(price.getAmount(), price.getCurrency(), pricingModel);

        }

        @Override
        public void setPrice(Price price) {
            this.price = price;

        }

        @Override
        public PricingModel getPricingModel() {
            return price.getPricingModel();
        }
    }

    /**
     * Min price filter
     * 
     */
    public static class PriceFilterMin extends PriceFilter {

        Price price;

        public PriceFilterMin(Price min) {
            super();
            this.price = min;
        }

        public Price getMin() {
            return price;
        }

        @Override
        public String toQueryElement() {
            throw new BridgeIoTException("Price filter min not supported");
        }

        @Override
        public void setPricingModel(PricingModel pricingModel) {
            price = new Price(price.getAmount(), price.getCurrency(), pricingModel);

        }

        @Override
        public void setPrice(Price price) {
            this.price = new Price(price.getAmount(), price.getCurrency(), this.price.getPricingModel());

        }

        @Override
        public PricingModel getPricingModel() {
            return price.getPricingModel();
        }

    }

    /**
     * Filter for price in an interval
     * 
     *
     */
    public static class PriceFilterBetween extends PriceFilter {

        Price min;
        Price max;

        public PriceFilterBetween(Price min, Price max) {
            super();
            this.min = min;
            this.max = max;
        }

        public Price getMin() {
            return min;
        }

        public Price getMax() {
            return max;
        }

        @Override
        public String toQueryElement() {
            throw new BridgeIoTException("Price filter max not supported");
        }

        @Override
        public void setPricingModel(PricingModel accountingType) {
            throw new BridgeIoTException("Price filter max not supported");

        }

        @Override
        public void setPrice(Price price) {
            throw new BridgeIoTException("Price filter max not supported");

        }

        @Override
        public PricingModel getPricingModel() {
            return min.getPricingModel();
        }

    }

    /**
     * Sets accotuning type
     * 
     * @param pricingModel
     */
    public abstract void setPricingModel(PricingModel pricingModel);

    /**
     * Returns accounting type
     * 
     * @return
     */
    public abstract PricingModel getPricingModel();

    /**
     * Sets price
     * 
     * @param price
     */
    public abstract void setPrice(Price price);

}