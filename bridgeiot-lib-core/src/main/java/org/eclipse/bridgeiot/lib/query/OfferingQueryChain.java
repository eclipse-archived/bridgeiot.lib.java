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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingQueryException;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Information;
import org.eclipse.bridgeiot.lib.model.Money;
import org.eclipse.bridgeiot.lib.model.Price;
import org.eclipse.bridgeiot.lib.model.RDFType;
import org.eclipse.bridgeiot.lib.model.Region;
import org.eclipse.bridgeiot.lib.model.TimePeriod;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.eclipse.bridgeiot.lib.query.elements.PriceFilter;
import org.eclipse.bridgeiot.lib.query.elements.PriceFilter.PriceFilterMax;
import org.eclipse.bridgeiot.lib.templates.OfferingQueryRequestTemplate;
import org.joda.time.DateTime;

/**
 * This implementation of an Offering Query allows the incremental construction of an Offering Query via chaining
 * functions. The methods in this class add elements to the offering query, e.g. a max price, an accounting type or a
 * region.
 *
 * As a central element in the Bridge.IoT API, the implementation of OfferingQuery shall allow natural, user centric
 * query generation. Motivation is that the user gets a strongly typed interface for Offering Query Generation without
 * being overstrained by parameters he needn't for his query. The stream orientation of the incremental approach and the
 * method names oriented an natural language support this.
 * <p>
 * <p>
 * An example for the usage is
 * <p>
 * 
 * <pre>
 * {@code IOfferingQuery query = OfferingQuery.create("... Local Query Id ...")}
 *                                     {@code .withName("Example Offering Query")}
 *                                     {@code .withCategory("urn:big-iot:ParkingSpaceCategory")}
 *                                     {@code .inRegion(RegionFilter.city("Barcelona"))}
 *                                     {@code .withAccountingType(EnumTypes.AccountingType.PER_ACCESS)}
 *                                     {@code .withMaxPrice(Euros.amount(0.002))}
 *                                     {@code .withLicenseType(LicenseType.OPEN_DATA_LICENSE);}
 * </pre>
 * 
 * 
 *
 */
public class OfferingQueryChain extends OfferingQuery {

    protected String localId;
    protected Information information = null;
    protected PriceFilter priceFilter = new PriceFilterMax(Price.free());
    protected Region region = null;
    protected LinkedList<LicenseType> acceptedLicenseTypes = new LinkedList<>();
    protected ObjectParameter inputData = ObjectParameter.create();
    protected ObjectParameter outputData = ObjectParameter.create();
    protected TimePeriod timePeriod = null;

    /**
     * Constructs a minimal Offering Query
     * 
     * @param consumerId
     *            Consumer ID
     * @throws IncompleteOfferingQueryException
     */
    public OfferingQueryChain(String localId) {
        super();
        this.localId = localId;
    }

    /**
     * All Offerings with specified information element
     * 
     * @param information
     *            Information Element
     * @return Offering Query
     */
    @Deprecated
    public OfferingQueryChain withInformation(Information information) {
        this.information = information;
        return this;
    }

    /**
     * All Offerings with specified information element
     * 
     * @param name
     *            Query Name
     * @param rdfTypeUri
     *            RDF Type (category) of desired Offerings
     * @return Offering Query
     */
    @Deprecated
    public OfferingQueryChain withInformation(String name, String rdfTypeUri) {
        this.information = new Information(name, rdfTypeUri);
        return this;
    }

    /**
     * All Offerings with specified information element
     * 
     * @param name
     *            Query Name
     * @param rdfTypeUri
     *            RDF Type (category) of desired Offerings
     * @return Offering Query
     */
    public OfferingQueryChain withName(String name) {
        if (this.information != null) {
            this.information.setName(name);
        } else {
            this.information = new Information(name);
        }
        return this;
    }

    /**
     * All Offerings with specified information element
     * 
     * @param rdfTypeUri
     *            RDF Type (category) of desired Offerings
     * @return Offering Query
     */
    public OfferingQueryChain withCategory(String rdfTypeUri) {
        if (this.information != null) {
            this.information.setRdfType(new RDFType(rdfTypeUri));
        } else {
            this.information = new Information("Default Query Name", rdfTypeUri);
        }
        return this;
    }

    /**
     * All Offerings with a max price
     * 
     * @param price
     *            Price
     * @return Offering Query
     */
    public OfferingQueryChain withMaxPrice(Price price) {
        this.priceFilter = new PriceFilterMax(price);
        return this;
    }

    /**
     * All Offerings with a max price for selected pricing model
     * 
     * @param price
     *            Price
     * @return Offering Query
     */
    public OfferingQueryChain withMaxPrice(Money money) {
        return withMaxPrice(new Price(money, priceFilter.getPricingModel()));
    }

    /**
     * All Offerings in a region
     * 
     * @param region
     * @return Offering Query
     */
    public OfferingQueryChain inRegion(Region region) {
        this.region = region;
        return this;
    }

    /**
     * All Offerings in a region
     * 
     * @param boundingBox
     * @return Offering Query
     */
    public OfferingQueryChain inRegion(BoundingBox boundingBox) {
        this.region = Region.create(boundingBox);
        return this;
    }

    /**
     * All Offerings in a region
     * 
     * @param regionName
     * @return Offering Query
     */
    public OfferingQueryChain inRegion(String regionName) {
        this.region = Region.create(regionName);
        return this;
    }

    /**
     * All Offerings in a city
     * 
     * @param cityName
     * @return Offering Query
     */
    public OfferingQueryChain inCity(String cityName) {
        this.region = Region.create(cityName);
        return this;
    }

    /**
     * All Offerings with specified pricing model
     * 
     * @param pricingModel
     * @return
     */
    public OfferingQueryChain withPricingModel(PricingModel pricingModel) {
        this.priceFilter.setPricingModel(pricingModel);
        return this;
    }

    /**
     * All Offerings with specified license type
     * 
     * @param licenseType
     *            Licens Type
     * @return
     */
    public OfferingQueryChain withLicenseType(LicenseType licenseType) {
        this.acceptedLicenseTypes.add(licenseType);
        return this;
    }

    /**
     * Sets the time period in OfferingQuery
     * 
     * @param from
     * @param to
     * @return
     */
    public OfferingQueryChain withTimePeriod(long from, long to) {
        this.timePeriod = TimePeriod.create(from, to);
        return this;
    }

    /**
     * Sets the time period in OfferingQuery
     * 
     * @param from
     * @param to
     * @return
     */
    public OfferingQueryChain withTimePeriod(DateTime from, DateTime to) {
        this.timePeriod = TimePeriod.create(from, to);
        return this;
    }

    /**
     * Sets the time period in OfferingQuery
     * 
     * @param period
     * @return
     */
    public OfferingQueryChain withTimePeriod(TimePeriod period) {
        this.timePeriod = period;
        return this;
    }

    public OfferingQueryChain withLiveDataOnly() {
        return withTimePeriod(TimePeriod.create(0L, 0L));
    }

    public ObjectParameter getInputData() {
        return this.inputData;
    }

    public void setInputData(ObjectParameter parameter) {
        this.inputData = parameter;
    }

    public ObjectParameter getOutputData() {
        return outputData;
    }

    public void setOutputData(ObjectParameter outputData) {
        this.outputData = outputData;
    }

    /**
     * Adds an input parameter to an offering.
     * 
     * @param rdfType
     * @param valueType
     * @return
     */
    public OfferingQueryChain addInputData(String rdfAnnotation, ValueType valueType) {
        inputData.addMember(null, rdfAnnotation, valueType);
        return this;
    }

    /**
     * Adds an input parameter to an offering - the value type 'undefined' is set.
     * 
     * @param rdfType
     * @return
     */
    public OfferingQueryChain addInputData(String rdfAnnotation) {
        inputData.addMember(null, rdfAnnotation, ValueType.UNDEFINED);
        return this;
    }

    /**
     * Adds a output parameter to an offering.
     * 
     * @param rdfType
     * @param valueType
     * @return
     */
    public OfferingQueryChain addOutputData(String rdfAnnotation, ValueType valueType) {
        outputData.addMember(null, rdfAnnotation, valueType);
        return this;
    }

    /**
     * Adds an output parameter to an offering - the value type 'undefined' is set.
     * 
     * @param rdfType
     * @return
     */
    public OfferingQueryChain addOutputData(String rdfAnnotation) {
        outputData.addMember(null, rdfAnnotation, ValueType.UNDEFINED);
        return this;
    }

    public String getLocalId() {
        return this.localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public Information getInformation() {
        return information;
    }

    public PriceFilter getPriceFilter() {
        return priceFilter;
    }

    public Region getRegion() {
        return region;
    }

    public List<LicenseType> getAcceptedLicenseTypes() {
        return acceptedLicenseTypes;
    }

    @Override
    public String toString() {
        return toOfferingQueryString(new BridgeIotClientId("unspecified"));
    }

    @Override
    public boolean sameQuery(IOfferingQuery query) {
        return this.getId().equals(query.getId()) && this.toString().equals(query.toString());
    }

    /**
     * Compiles a Offering Query message accepted by the Marketplace
     * 
     * @param consumerId
     *            Consumer identifier issued by Marketplace
     * @return
     */
    @Override
    public String toOfferingQueryString(BridgeIotClientId consumerId) {

        LinkedList<String> queryElements = new LinkedList<>();

        queryElements.add(consumerId.toQueryElement());
        queryElements.add("localId: \\\"" + this.localId + "\\\"");
        if (information != null) {
            if (information.getName() != null) {
                queryElements.add("name: \\\"" + information.getName() + "\\\"");
            }
            if (information.getRdfType() != null && information.getRdfType().getUri() != null) {
                queryElements.add("rdfUri: \\\"" + information.getRdfType().getUri() + "\\\"");
            }

        } else {
            queryElements.add("name: \\\"" + "MyQuery" + "\\\"");
        }

        if (acceptedLicenseTypes != null && !acceptedLicenseTypes.isEmpty()) {
            queryElements.add("license: " + acceptedLicenseTypes.getFirst().name());
        }

        if (priceFilter != null) {
            if (priceFilter.getPricingModel() != PricingModel.FREE) {
                queryElements.add(priceFilter.toQueryElement());
            } else {
                queryElements.add("price: { pricingModel: FREE }");
            }
        }

        if (region != null) {
            queryElements.add(region.toQueryElement());
        }

        if (timePeriod != null) {
            queryElements.add(timePeriod.toQueryElement());
        }

        String inputDataString = GraphQLQueries.toIODataString(inputData);
        if (inputDataString != null && !inputDataString.isEmpty()) {
            queryElements.add("inputs: [ " + inputDataString + " ] ");
        }

        String outputDataString = GraphQLQueries.toIODataString(outputData);
        if (outputDataString != null && !outputDataString.isEmpty()) {
            queryElements.add("outputs: [ " + outputDataString + " ] ");
        }

        return new OfferingQueryRequestTemplate(queryElements).fillout();

    }

}