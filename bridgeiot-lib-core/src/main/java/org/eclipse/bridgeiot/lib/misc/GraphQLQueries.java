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
package org.eclipse.bridgeiot.lib.misc;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.offering.parameters.ComplexParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectMember;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLQueries {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLQueries.class);

    /**
     * private constructor
     */
    private GraphQLQueries() {
    }

    /**
     * Generates a valid registration message accepted by the Marketplace
     *
     * @param providerId
     *            Identifier of the provider issued by the Marketplace
     * @return Message for Offering registration
     */
    public static String getRegistrationString(RegistrableOfferingDescription offering) {

        final Long expirationTime = new Date().getTime() + offering.getExpirationInterval();

        String registration = "{ \"query\" : \"mutation addOffering { addOffering ( input: { "
                // + "semanticContext: {context: \\\"http://w3c.github.io/bigiot/offering-context.jsonld\\\"} "
                + Helper.formatString("id: \\\"%s\\\" ", offering.getProviderId())
                + Helper.formatString("localId: \\\"%s\\\" ", offering.getLocalId())
                + Helper.formatString("name: \\\"%s\\\" ", offering.getDescription().getName())
                // + Helper.formatString("active: %s ", true)
                + Helper.formatString("activation: {status: %s, expirationTime: %d} ", true, expirationTime)
                + Helper.formatString("rdfUri: \\\"%s\\\" ", offering.getDescription().getRdfType().getUri());

        // Marketplace only array of object members below inputs
        if (offering.getInputs() instanceof ObjectParameter) {
            String inputDataString = Helper
                    .convertJsonToGraphQl(
                            Helper.getPojoAsJsonCompact(((ObjectParameter) offering.getInputs()).getMembers()))
                    .replaceAll("'", "\\\\'").replaceAll("\"", "\\\\\"");
            registration += Helper.formatString("inputs: ") + inputDataString + Helper.formatString(" ");
        } else {
            logger.warn(
                    "Marketplace supports only object type parameters. Offering description has input parameters modelled as {}",
                    offering.getInputs().getClass().getName());
        }

        // Marketplace only array of object members below outputs
        if (offering.getOutputs() instanceof ObjectParameter) {
            String outputDataString = Helper
                    .convertJsonToGraphQl(
                            Helper.getPojoAsJsonCompact(((ObjectParameter) offering.getOutputs()).getMembers()))
                    .replaceAll("'", "\\\\'").replaceAll("\"", "\\\\\"");
            registration += Helper.formatString("outputs: ") + outputDataString + Helper.formatString(" ");
        } else {
            logger.warn(
                    "Marketplace supports only object type parameters. Offering description has output parameters modelled as {}",
                    offering.getOutputs().getClass().getName());
        }

        registration += Helper.formatString("endpoints: {uri: \\\"%s\\\", endpointType: %s, accessInterfaceType: %s} ",
                offering.getEndpoints().get(0).getUri(), offering.getEndpoints().get(0).getEndpointType().toString(),
                offering.getEndpoints().get(0).getAccessInterfaceType().toString());

        registration += Helper.formatString("license: %s ", offering.getLicense().toString());

        if (offering.getPrice().getPricingModel() != PricingModel.FREE) {
            // TODO: EUR once Marketplace supports others:
            registration += Helper.formatString("price: {money: {amount: %f, currency: EUR}, pricingModel: %s } ",
                    offering.getPrice().getAmount(), offering.getPrice().getPricingModel().toString());
        } else {
            registration += Helper.formatString("price: { pricingModel: %s } ",
                    offering.getPrice().getPricingModel().toString());
        }

        if (offering.getTimePeriod() != null) {
            registration += offering.getTimePeriod().toQueryElement() + " ";
        }

        if (offering.getRegion() != null) {
            registration += offering.getRegion().toQueryElement() + " ";
        }

        if (offering.getAccessList() != null) {
            registration += offering.getAccessList().toQueryElement() + " ";
        }

        if (offering.getExtension1() != null) {
            registration += Helper.formatString("extension1: \\\"%s\\\" ",
                    offering.getExtension1().replaceAll("\"", "\\" + Constants.DOUBLE_QUOTE_ESCAPE).replaceAll("\\\\",
                            "\\" + Constants.BACKSLASH_ESCAPE));
        }
        Helper.getPojoAsJson(offering.getInputs());

        registration += " } ) { id activation { status expirationTime } } " + "}\" }";

        return registration;
    }

    // DELETE
    public static String toIODataString(Parameter complexParameter) {

        StringBuilder stringBuilder = new StringBuilder();

        if (complexParameter instanceof ObjectParameter) {
            ObjectParameter objectParameter = (ObjectParameter) complexParameter;
            for (ObjectMember member : objectParameter.getMembers()) {
                if (member.getValue() instanceof ComplexParameter) {
                    logger.warn("Deep structure of complex parameter '{}' is not visible on marketplace!",
                            member.getName());
                }
                String valueType = Helper.convertJsonToGraphQl(Helper.getPojoAsJsonCompact(member.getValue()))
                        .replaceAll("'", "\\\\'").replaceAll("\"", "\\\\\"");
                String name = (member.getName() != null) ? Helper.formatString("name: \\\"%s\\\",", member.getName())
                        : "";
                stringBuilder.append(Helper.formatString("{ %s rdfUri: \\\"%s\\\", value: %s } ", name,
                        member.getRdfUri(), valueType));
            }
        } else {
            throw new BridgeIoTException("Only objects are supported as top input parameter");
        }

        return stringBuilder.toString();
    }

    /**
     * Generates activation message
     *
     * @return Activation message
     */
    public static String getActivationString(String offeringId) {
        // @formatter:off
        return "{ \"query\" : \"mutation activateOffering { activateOffering ( input: { "
                + Helper.formatString("id: \\\"%s\\\" ", offeringId)
                + " } )"
                + " { id } "
                + " }\" }";
        // @formatter:on
    }

    public static String getActivationStringFullResponse(String offeringId) {
        // @formatter:off
        return "{ \"query\" : \"mutation activateOffering { activateOffering ( input: { "
                + Helper.formatString("id: \\\"%s\\\" ", offeringId)
                + " } )"
                + " {"
                   + " id name rdfAnnotation { uri } endpoints { uri endpointType accessInterfaceType } }"
                + " }\" }";
        // @formatter:on
    }

    public static String getOfferingDescriptionString(String offeringId) {
        // @formatter:off
        return "{ \"query\" : \"query offering { offering ( "
                + Helper.formatString("id: \\\"%s\\\"", offeringId)
                + " )"
                + " {"
                   + " id name rdfAnnotation { uri } license"
                   + " price { pricingModel money { amount currency } }"
                   + " inputs { name rdfAnnotation { uri } }"
                   + " outputs { name rdfAnnotation { uri } }"
                   + " endpoints { uri endpointType accessInterfaceType }"
                   + " temporalExtent { from to }"
                   + " spatialExtent { city boundary { l1 { lng lat } l2 { lng lat } } }"
                   + " activation { status expirationTime }"
                + " } }\" }";
        // @formatter:on
    }

    /**
     * Generates a valid subscription message (based on a query) accepted by the Marketplace
     *
     * @return Message for Offering subscription
     */
    public static String getSubscribtionWithQueryString(String offeringId, String queryId) {
        // @formatter:off
        return "{ \"query\" : \"mutation subscribeQueryToOffering { subscribeQueryToOffering ( input: { "
                + Helper.formatString("id: \\\"%s\\\" ", queryId)
                + Helper.formatString("offeringId: \\\"%s\\\" ", offeringId)
                + " } ) { id accessToken } "
                + "}\" }";
        // @formatter:on
    }

    /**
     * Generates a valid subscription message (based on a consumer) accepted by the Marketplace
     *
     * @return Message for Offering subscription
     */
    public static String getSubscribtionWithConsumerString(String offeringId, String consumerId) {
        // @formatter:off
        return "{ \"query\" : \"mutation subscribeConsumerToOffering { subscribeConsumerToOffering ( input: { "
                + Helper.formatString("id: \\\"%s\\\" ", consumerId)
                + Helper.formatString("offeringId: \\\"%s\\\" ", offeringId)
                + " } ) { id accessToken } "
                + "}\" }";
        // @formatter:on
    }

    /**
     * Generates a valid subscription message accepted by the Marketplace
     *
     * @return Message for Offering subscription
     */
    public static String getUnsubscribtionString(String offeringId) {
        // @formatter:off
        return "{ \"query\" : \"mutation unsubscribeQueryFromOffering { unsubscribeQueryFromOffering ( input: { "
                + Helper.formatString("id: \\\"%s\\\" ", "Bosch_CR-TestConsumer-ParkingQuery")
                // TODO Remove QueryId all together as soon Martin fixes the eXchange
                + Helper.formatString("offeringId: \\\"%s\\\" ", offeringId)
                + " } ) { id subscriptions { id } }"
                + "}\" }";
        // @formatter:on
    }

    /**
     * Generates deactivation message
     *
     * @return Deactivation message
     */
    public static String getDeactivationString(String offeringId) {
        // @formatter:off
        return "{ \"query\" : \"mutation deactivateOffering { deactivateOffering ( input: { "
                + Helper.formatString("id: \\\"%s\\\" ", offeringId)
                + " } ) { id }"
                + "}\" }";
        // @formatter:on
    }

    /*
     * NOT NEEDED?
     * @formatter:off
     * public static String getFindProviderByIdString(String offeringId) {
     *    return "{ \"query\" : \"query q { findProviderById(id: \\\"" + offeringId + "\\\") { "
     *           + "id "
     *           + "name "
     *           + "} } "
     *           + "\"}";
     * }
     * @formatter:on
     */

    public static String getFindMatchingOfferingsString(String queryId) {
        // @formatter:off
        return "{\"query\": \"query q { matchingOfferings(queryId: \\\"" + queryId + "\\\")" +
                " { id " +
                "name " +
                "rdfAnnotation { uri } " +
                "spatialExtent { city boundary { l1 { lng lat } l2 { lng lat } } } " +
                "endpoints { uri endpointType accessInterfaceType } " +
                "license " +
                "temporalExtent { from to } " +
                "price { pricingModel money { amount currency } } " +
                "activation { status expirationTime } " +
                "extension1" +
                " } }\" }";
        // @formatter:on
    }

    // @formatter:off
    // @Deprecated
    // public static String getQueryTemplateStringOld() {
    // return "{ \"query\" : \"mutation addOfferingQuery { addOfferingQuery ( input : "
    //                + "{ id: \\\"%%consumerId\\\" localId: \\\"%%localId\\\" name: \\\"%%name\\\" rdfUri: \\\"%%rdfUri\\\" license: %%licenseType price: %%price extent: %%region } "
    // + ") { id } }\" }";
    // }
    // @formatter:on

    public static String getQueryBaseTemplateString() {
        // @formatter:off
        return "{ \"query\" : \"mutation addOfferingQuery { addOfferingQuery ( input : "
                + "{ %%queryElements } "
                + ") { id } }\" }";
        // @formatter:on
    }

    public static String getAccountingReportString(List<AccountingReport> accountingReportList) {
        // @formatter:off
        StringBuilder strBuilder = new StringBuilder()
                .append("{ \"query\" : \"mutation accountingReport { trackProviderAccess ( input : ")
                   .append("{ accesses: [ ");
        
        Iterator<AccountingReport> it = accountingReportList.iterator();
        boolean firstRecord = true;
        while (it.hasNext()) {
            AccountingReport report = it.next();
            if (! report.getSubscriptionId().equals(Constants.UNKNOWN_SUBSCRIPTION_ID)) { 
                if (firstRecord) {
                    firstRecord = false;
                }
                else {
                    strBuilder.append(", ");
                }
                strBuilder.append("{ ")
                            .append("id: \\\"").append(report.getSubscriptionId()).append("\\\" ")
                            .append("accessSessionId: \\\"").append(report.getAccessSessionId()).append("\\\" ")
                            .append("report: { ")
                                .append("accesses: ").append(report.getRecord().getCurrentAccesses()).append(" ")
                                .append("records: ").append(report.getRecord().getCurrentDataRecords()).append(" ")
                                .append("inputBytes: 0 ")         // accouting for input data is not yet supported
                                .append("outputBytes: ").append(report.getRecord().getCurrentBytes()).append(" ")
                                .append("totalAccesses: ").append(report.getRecord().getTotalAccesses()).append(" ")
                                .append("totalRecords: ").append(report.getRecord().getTotalDataRecords()).append(" ")
                                .append("totalInputBytes: 0 ")    // accouting for input data is not yet supported
                                .append("totalOutputBytes: ").append(report.getRecord().getTotalBytes()).append(" ")
                            .append("} ")
                            .append("time: { ")
                                .append("start: ").append(report.getRecord().getTsLastReport()).append(" ")
                                .append("end: ").append(report.getRecord().getTsCurrentReport()).append(" ")
                            .append("} ")
                          .append("}");
            }
        }
                   
        strBuilder.append(" ] } ) { id } }\" }");
        // @formatter:off
        
        return strBuilder.toString();
    }

}
