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
package org.eclipse.bridgeiot.lib.model;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Price consisting of value and currency
 * 
 *
 */
public class Price implements Comparable<Price> {

    private static final PricingModel DEFAULT_PRICING_MODEL = PricingModel.PER_ACCESS;

    protected PricingModel pricingModel = DEFAULT_PRICING_MODEL;
    protected Money money = new Money();

    /**
     * Factory class for prices in EUR
     * 
     */
    public static class Euros {
        public static Money amount(double euros) {
            return new Money(euros, Money.EURO);
        }
    }

    /**
     * Factory class for prices in USD
     * 
     */
    public static class USDollars {
        public static Money amount(double dollars) {
            return new Money(dollars, Money.USDOLLAR);
        }
    }

    /**
     * Factory class for Free Offering price
     * 
     */
    public static Price free() {
        return new Price(Euros.amount(0.0), PricingModel.FREE);
    }

    public Price() {
    }

    public Price(Money money, PricingModel pricingModel) {
        this.money = money;
        this.pricingModel = pricingModel;
    }

    public Price(double amount, String currency, PricingModel pricingModel) {
        this(new Money(amount, currency), pricingModel);
    }

    public Price(double amount, String currency) {
        this(new Money(amount, currency), DEFAULT_PRICING_MODEL);
    }

    public void setPricingModel(PricingModel pricingModel) {
        this.pricingModel = pricingModel;
    }

    public PricingModel getPricingModel() {
        return this.pricingModel;
    }

    public void setMoney(Money money) {
        this.money = money;
    }

    public Money getMoney() {
        return this.money;
    }

    @JsonIgnore
    public double getAmount() {
        return this.money.getAmount();
    }

    public void setAmount(double amount) {
        this.money.setAmount(amount);
    }

    @JsonIgnore
    public String getCurrency() {
        return this.money.getCurrency();
    }

    public void setCurrency(String currency) {
        this.money.setCurrency(currency);
    }

    @Override
    public int compareTo(Price o) {
        if (this.getAmount() < o.getAmount()) {
            return -1;
        }
        if (this.getAmount() > o.getAmount()) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return money.toString() + " [ " + pricingModel.toString() + " ]";
    }

}