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
 * Denis Kramer     (Bosch Software Innovations GmbH)
 * Stefan Schmid    (Robert Bosch GmbH)
 * Andreas Ziller   (Siemens AG)
 */
package org.eclipse.bridgeiot.lib.offering;

import java.io.IOException;
import java.util.Map;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.IProvider;
import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessStreamFilterHandler;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.AccessList;
import org.eclipse.bridgeiot.lib.model.BoundingBox;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Description;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.model.Information;
import org.eclipse.bridgeiot.lib.model.Money;
import org.eclipse.bridgeiot.lib.model.Price;
import org.eclipse.bridgeiot.lib.model.RDFType;
import org.eclipse.bridgeiot.lib.model.Region;
import org.eclipse.bridgeiot.lib.model.TimePeriod;
import org.eclipse.bridgeiot.lib.offering.encoder.MessageTemplates;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.eclipse.bridgeiot.lib.offering.parameters.ComplexParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectMember;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.Parameter;
import org.eclipse.bridgeiot.lib.offering.parameters.RdfReferenceParameter;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation allows the incremental construction of an Offering via chaining functions. The methods in this
 * class add elements to the offering query, e.g. a price, an accounting type or a region.
 *
 * <p>
 * An example for the usage is
 * <p>
 * 
 * <pre>
 * {@code OfferingDescription offeringDescription = OfferingDescription.create("... LocalID ...")}
 *                    {@code .withName("Example Offering Query")}
 *                    {@code .withCategory("urn:big-iot:ParkingSpaceCategory")}
 *                        {@code .addInputData("longitude", new RDFType("schema:longitude"))}
 *                        {@code .addInputData("latitude", new RDFType("schema:latitude"))}
 *                        {@code .addInputData("radius", new RDFType("schema:geoRadius"))}
 *                        {@code .addOutputData("available", new RDFType("datex:availableparking"))}
 *                        {@code .addOutputData("occupied", new RDFType("datex:occupiedparking"))}
 *                    {@code .inRegion(BoundingBox.create(Location.create(42.1, 9.0), Location.create(43.2, 10.0)))}
 *                    {@code .withPrice(new PriceWithAccountingType(Euros.amount(0.002), AccountingType.PER_ACCESS))}
 *                    {@code .withLicense(LicenseType.OPEN_DATA_LICENSE)}
 * </pre>
 */
public class RegistrableOfferingDescriptionChain extends RegistrableOfferingDescription {

    protected static final Logger logger = LoggerFactory.getLogger(RegistrableOfferingDescriptionChain.class);

    public RegistrableOfferingDescriptionChain() {
    }

    public RegistrableOfferingDescriptionChain(String localId, IProvider provider, BridgeIotClientId clientId,
            MarketplaceClient marketplaceClient, Map<OfferingId, RegisteredOffering> offerings) {
        super(localId, provider, clientId, marketplaceClient, offerings);
    }

    /**
     * Sets description
     * 
     * @param description
     * @return
     */
    @Deprecated
    public RegistrableOfferingDescriptionChain withInformation(Description description) {
        this.setDescription(description);
        return this;
    }

    /**
     * Sets description
     * 
     * @param description
     * @return
     */
    @Deprecated
    public RegistrableOfferingDescriptionChain withDescription(Description description) {
        this.setDescription(description);
        return this;
    }

    /**
     * Sets information object describing offering
     * 
     * @param information
     * @return
     */
    @Deprecated
    public RegistrableOfferingDescriptionChain withInformation(Information information) {
        this.setDescription(information);
        return this;
    }

    /**
     * Convenicence function for setting name and rdftype URI
     *
     * @param name
     * @param rdfTypeUri
     * @return
     */
    @Deprecated
    public RegistrableOfferingDescriptionChain withInformation(String name, String rdfTypeUri) {
        this.setName(name);
        this.setRdfType(new RDFType(rdfTypeUri));
        this.setDescription(new Information(name, rdfTypeUri));
        return this;
    }

    /**
     * Convenicence function for setting Offering name
     *
     * @param name
     * @return
     */
    public RegistrableOfferingDescriptionChain withName(String name) {
        this.setName(name);
        if (this.getDescription() != null) {
            this.getDescription().setName(name);
        } else {
            this.setDescription(new Information(name));
        }
        return this;
    }

    /**
     * Convenicence function for setting Offering category
     *
     * @param name
     * @return
     */
    public RegistrableOfferingDescriptionChain withCategory(String category) {
        this.setRdfType(new RDFType(category));
        if (this.getDescription() != null) {
            this.getDescription().setRdfType(new RDFType(category));
        } else {
            this.setDescription(new Information("Default Offering Name", new RDFType(category)));
        }
        return this;
    }

    /**
     * Convenicence function for setting name and rdftype
     *
     * @param name
     * @param rdfType
     * @return
     */
    @Deprecated
    public RegistrableOfferingDescriptionChain withInformation(String name, RDFType rdfType) {
        this.setName(name);
        this.setRdfType(rdfType);
        this.setDescription(new Information(name, rdfType));
        return this;
    }

    /**
     * Sets a region
     * 
     * @param region
     * @return
     */
    public RegistrableOfferingDescriptionChain inRegion(Region region) {
        this.setRegion(region);
        return this;
    }

    /**
     * Sets a region
     * 
     * @param boundingBox
     * @return
     */
    public RegistrableOfferingDescriptionChain inRegion(BoundingBox boundingBox) {
        this.setRegion(Region.create(boundingBox));
        return this;
    }

    /**
     * Sets a region
     * 
     * @param regioName
     * @return
     */
    public RegistrableOfferingDescriptionChain inRegion(String regionName) {
        this.setRegion(Region.create(regionName));
        return this;
    }

    /**
     * Sets a region filter by city name
     * 
     * @param cityName
     * @return
     */
    public RegistrableOfferingDescriptionChain inCity(String cityName) {
        this.setRegion(Region.create(cityName));
        return this;
    }

    /**
     * Sets the price of an offering
     *
     * @param price
     * @return
     */
    protected RegistrableOfferingDescriptionChain withPrice(Price price) {
        if (this.getPrice() != null) {
            this.getPrice().setAmount(price.getAmount());
            this.getPrice().setCurrency(price.getCurrency());
        } else {
            this.setPrice(price);
        }
        return this;
    }

    /**
     * Sets the price of an offering
     *
     * @param money
     * @return
     */
    public RegistrableOfferingDescriptionChain withPrice(Money money) {
        if (this.getPrice() != null) {
            this.getPrice().setMoney(money);
        } else {
            this.setPrice(new Price(money.getAmount(), money.getCurrency()));
        }
        return this;

    }

    /**
     * Sets the pricing model of an offering
     *
     * @param pricingModel
     * @return
     */
    public RegistrableOfferingDescriptionChain withPricingModel(PricingModel pricingModel) {
        if (this.getPrice() != null) {
            this.getPrice().setPricingModel(pricingModel);

        } else {
            this.setPrice(new Price(0, Money.EURO, pricingModel));
        }
        return this;
    }

    /**
     * Sets the license type of an offering
     *
     * @param type
     * @return
     */
    public RegistrableOfferingDescriptionChain withLicenseType(LicenseType type) {
        this.setLicense(type);
        return this;
    }

    /**
     * Sets the access list of an offering
     *
     * @param accessList
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessList(AccessList accessList) {
        this.setAccessList(accessList);
        return this;
    }

    /**
     * Sets the access list of an offering
     *
     * @param organizations
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessList(String... organizations) {
        this.setAccessList(AccessList.create(organizations));
        return this;
    }

    /**
     * Sets the access list of an offering
     *
     * @param organizations
     * @return
     */
    public RegistrableOfferingDescriptionChain restrictedToOrganizations(String... organizations) {
        this.setAccessList(AccessList.create(organizations));
        return this;
    }

    /**
     * Adds an endpoint to an offering. Currently only one endpoint is supported.
     *
     * @param accessInterfaceType
     * @param uri
     * @return
     */
    public RegistrableOfferingDescriptionChain addEndPoint(AccessInterfaceType accessInterfaceType, String uri) {
        EndPoint endPoint = new EndPoint(EndpointType.HTTP_GET, accessInterfaceType, uri);
        if (endpoints.isEmpty()) {
            endpoints.add(endPoint);
        } else {
            endpoints.set(0, endPoint);
        }
        return this;
    }

    /**
     * Adds an external endpoint to an offering. Currently only one endpoint is supported. Sets offering implicitly to
     * an int mode 3 offering.
     *
     * @param endpointUrl
     * @return
     */
    public RegistrableOfferingDescriptionChain onExternalEndpoint(String endpointUrl) {
        this.specialTreatment = true;
        this.setAccessInterfaceType(AccessInterfaceType.EXTERNAL);
        if (endpoints.isEmpty()) {
            EndPoint endPoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, endpointUrl);
            endpoints.add(endPoint);
        } else {
            for (EndPoint endPoint : endpoints) {
                endPoint.setAccessInterfaceType(AccessInterfaceType.EXTERNAL);
                endPoint.setUri(endpointUrl);
            }
        }

        return this;
    }

    /**
     * Add a non-complex input parameter to an offering.
     * 
     * @param name
     * @param rdfType
     * @param dataSchemaType
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputData(String name, String rdfAnnotation, ValueType valueType) {
        return this.addInputData(name, rdfAnnotation, valueType, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                false);
    }

    /**
     * Add a non-complex input parameter to an offering, which is either required or not
     * 
     * @param name
     * @param rdfType
     * @param dataSchemaType
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputData(String name, String rdfAnnotation, ValueType valueType,
            boolean isRequired) {
        return this.addInputData(name, rdfAnnotation, valueType, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                isRequired);
    }

    /**
     * Add a non-complex input parameter to an offering with an encoding type (only required for IM3), which is either
     * required or not
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @param encodingType
     * @param isRequired
     * @return
     */
    protected RegistrableOfferingDescriptionChain addInputData(String name, String rdfAnnotation, ValueType valueType,
            ParameterEncodingType encodingType, boolean isRequired) {

        if (this.inputs instanceof ObjectParameter) {
            ((ObjectParameter) this.inputs).addMember(name, rdfAnnotation, valueType, encodingType, isRequired);
        } else {
            throw new BridgeIoTException("Cannot add a member parameter specification to input data specified as "
                    + this.inputs.getClass().getCanonicalName());
        }
        return this;

    }

    /**
     * Adds a complex input parameter to an offering.
     * 
     * @param name
     * @param rdfAnnotation
     * @param complexParameter
     * @return
     */

    public RegistrableOfferingDescriptionChain addInputData(String name, String rdfAnnotation,
            ComplexParameter complexParameter) {

        return addInputData(name, rdfAnnotation, complexParameter, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                false);
    }

    /**
     * Adds a complex input parameter to an offering, which is either required or not
     * 
     * @param name
     * @param rdfAnnotation
     * @param complexParameter
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputData(String name, String rdfAnnotation,
            ComplexParameter complexParameter, boolean isRequired) {
        return addInputData(name, rdfAnnotation, complexParameter, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                isRequired);
    }

    protected RegistrableOfferingDescriptionChain addInputData(String name, String rdfAnnotation,
            ComplexParameter complexParameter, ParameterEncodingType encodingType, boolean isRequired) {

        if (this.inputs instanceof ObjectParameter) {
            ((ObjectParameter) this.inputs).addMember(name, rdfAnnotation, complexParameter, encodingType, isRequired);
        } else {
            throw new BridgeIoTException("Cannot add a member parameter specification to input data specified as "
                    + this.inputs.getClass().getCanonicalName());
        }
        return this;
    }

    /**
     * Adds an input parameter to an offering, which is encoded as a query parameter. Useful for provider side
     * integration in integration mode 3.
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputDataInQuery(String name, String rdfAnnotation,
            ValueType valueType) {

        this.addInputData(name, rdfAnnotation, valueType, ParameterEncodingType.QUERY, false);
        return this;
    }

    /**
     * Adds an input parameter to an offering, which is encoded as a body parameter. Useful for provider side
     * integration in integration mode 3.
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputDataInBody(String name, String rdfAnnotation,
            ValueType valueType) {

        this.addInputData(name, rdfAnnotation, valueType, ParameterEncodingType.BODY, false);
        return this;
    }

    /**
     * Adds an input parameter to an offering, which is encoded as a route parameter. Useful for provider side
     * integration in integration mode 3.
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputDataInRoute(String name, String rdfAnnotation,
            ValueType valueType) {

        this.addInputData(name, rdfAnnotation, valueType, ParameterEncodingType.ROUTE, false);
        return this;
    }

    /**
     * Adds an input parameter to an offering, which is encoded as a template parameter. Useful for provider side
     * integration in integration mode 3.
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputDataInTemplate(String name, String rdfAnnotation,
            ValueType valueType) {

        this.addInputData(name, rdfAnnotation, valueType, ParameterEncodingType.TEMPLATE, false);
        return this;
    }

    /**
     * Specify input parameters by RDF reference
     * 
     * @param rdfReference
     * @return
     */
    public RegistrableOfferingDescriptionChain addInputData(String rdfReference) {
        this.inputs = new RdfReferenceParameter(rdfReference);
        return this;
    }

    //////////////////////////////////
    /**
     * Convenience function for adding a non-complex output parameter to an offering with only non-complex output
     * parameters.
     * 
     * @param name
     * @param rdfType
     * @param dataSchemaType
     * @return
     */
    public RegistrableOfferingDescriptionChain addOutputData(String name, String rdfAnnotation, ValueType valueType) {
        return this.addOutputData(name, rdfAnnotation, valueType, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                false);
    }

    public RegistrableOfferingDescriptionChain addOutputData(String name, String rdfAnnotation, ValueType valueType,
            boolean isRequired) {
        return this.addOutputData(name, rdfAnnotation, valueType, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                isRequired);
    }

    public RegistrableOfferingDescriptionChain addOutputData(String name, String rdfAnnotation, ValueType valueType,
            ParameterEncodingType encodingType, boolean isRequired) {

        if (this.outputs instanceof ObjectParameter) {
            ((ObjectParameter) this.outputs).addMember(name, rdfAnnotation, valueType, encodingType, isRequired);
        } else {
            throw new BridgeIoTException("Cannot add a member parameter specification to output data specified as "
                    + this.outputs.getClass().getCanonicalName());
        }
        return this;

    }

    /**
     * Specify output parameters by RDF reference
     * 
     * @param rdfReference
     * @return
     */
    public RegistrableOfferingDescriptionChain addOutputData(String rdfReference) {
        this.outputs = new RdfReferenceParameter(rdfReference);
        return this;
    }

    public RegistrableOfferingDescriptionChain addOutputData(Parameter parameter) {
        this.outputs = parameter;
        return this;
    }

    /**
     * Adds a complex output parameter to an offering.
     * 
     * @param name
     * @param rdfAnnotation
     * @param complexParameter
     * @return
     */

    public RegistrableOfferingDescriptionChain addOutputData(String name, String rdfAnnotation,
            ObjectParameter complexParameter) {
        return addOutputData(name, rdfAnnotation, complexParameter, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                false);

    }

    public RegistrableOfferingDescriptionChain addOutputData(String name, String rdfAnnotation,
            ObjectParameter complexParameter, boolean isRequired) {
        return addOutputData(name, rdfAnnotation, complexParameter, LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE,
                isRequired);

    }

    public RegistrableOfferingDescriptionChain addOutputData(String name, String rdfAnnotation,
            ObjectParameter complexParameter, ParameterEncodingType encodingType, boolean isRequired) {

        ObjectMember objectMember = new ObjectMember(name, rdfAnnotation, complexParameter, encodingType, isRequired);

        if (this.outputs instanceof ObjectParameter) {
            ((ObjectParameter) this.outputs).addMember(objectMember);
        } else {
            throw new BridgeIoTException("Cannot add a member parameter specification to output data specified as "
                    + this.outputs.getClass().getCanonicalName());
        }
        return this;
    }

    /**
     * Adds an output parameter to an offering, which is encoded as a body parameter. Useful for provider side
     * integration in integration mode 3.
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public RegistrableOfferingDescriptionChain addOutputDataInBody(String name, String rdfAnnotation,
            ValueType valueType) {

        this.addOutputData(name, rdfAnnotation, valueType, ParameterEncodingType.BODY, false);
        return this;
    }

    /**
     * Adds an output parameter to an offering, which is encoded as a template parameter. Useful for provider side
     * integration in integration mode 3.
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public RegistrableOfferingDescriptionChain addOutputDataInTemplate(String name, String rdfAnnotation,
            ValueType valueType) {

        this.addOutputData(name, rdfAnnotation, valueType, ParameterEncodingType.TEMPLATE, false);
        return this;
    }

    /**
     * Sets the expiration interval at the marketplace in milliseconds
     * 
     * @param intervalInMilli
     * @return
     */
    public RegistrableOfferingDescriptionChain withExpirationInterval(long intervalInMilli) {
        this.setExpirationInterval(intervalInMilli);
        return this;
    }

    /**
     * Sets the expiration interval at the marketplace
     * 
     * @param interval
     * @return
     */
    public RegistrableOfferingDescriptionChain withExpirationInterval(Duration interval) {
        this.setExpirationInterval(interval.getMillis());
        return this;
    }

    public RegistrableOfferingDescriptionChain useOfferingDescription(String offeringId)
            throws InvalidOfferingException, IOException {
        this.getOfferingDescription(offeringId);
        return this;
    }

    /**
     * Overrides the default route for the A1 interface
     *
     * @param route
     * @return
     */
    public RegistrableOfferingDescriptionChain withRoute(String route) {
        this.route = route;
        // Using add means currently set the one and only
        if (this.provider != null) {
            addEndPoint(AccessInterfaceType.BRIDGEIOT_LIB, provider.getBaseUrl() + "/" + route);
        }
        return this;
    }

    /**
     * Overrides the predetermined access interface type
     *
     * @param accessInterfaceType
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessInterfaceType(AccessInterfaceType accessInterfaceType) {
        this.setAccessInterfaceType(accessInterfaceType);
        return this;
    }

    /**
     * Sets the protocol type (only relevant in integration mode 3 [at the moment])
     *
     * @param protocol
     * @return
     */
    public RegistrableOfferingDescriptionChain withProtocol(EndpointType protocol) {
        if (endpoints.isEmpty()) {
            endpoints.add(new EndPoint(protocol, AccessInterfaceType.BRIDGEIOT_LIB, "not set"));
        } else {
            EndPoint endPoint = endpoints.get(0); // TODO CRITICAL Problem
            endPoint.setEndpointType(protocol);
        }
        return this;
    }

    /**
     * Sets the access callback method for Provider Lib hosted offerings
     *
     * @param accessCallback
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessRequestHandler(AccessRequestHandler accessCallback) {
        this.accessRequestHandler = accessCallback;
        return this;
    }

    /**
     * Sets the callback method for filtering access stream outputs
     *
     * @param accessCallback
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessStreamFilterHandler(AccessStreamFilterHandler filterCallback) {
        this.accessStreamFilterHandler = filterCallback;
        return this;
    }

    /**
     * Sets the sample data callback method for Provider Lib hosted offerings
     *
     * @param callback
     * @return
     */
    public RegistrableOfferingDescriptionChain withSampleDataRequestHandler(AccessRequestHandler callback) {
        this.sampleDataAccessRequestHandler = callback;
        if (this.provider != null) {
            this.sampleDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                    this.provider.getBaseUrl() + "/" + Constants.SAMPLEDATA_ROUTE + this.getLocalId());
        }
        return this;
    }

    public RegistrableOfferingDescriptionChain withExternalSampleDataEndpoint(String uri) {
        this.sampleDataAccessRequestHandler = null;
        this.sampleDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, uri);
        return this;
    }

    /**
     * Sets the sample data callback method for Provider Lib hosted offerings
     *
     * @param callback
     * @return
     */
    public RegistrableOfferingDescriptionChain withMetaDataRequestHandler(AccessRequestHandler callback) {
        this.metaDataAccessRequestHandler = callback;
        if (this.provider != null) {
            this.metaDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                    this.provider.getBaseUrl() + "/" + Constants.METADATA_ROUTE + this.getLocalId());
        }
        return this;
    }

    public RegistrableOfferingDescriptionChain withExternalMetaDataEndpoint(String uri) {
        this.metaDataAccessRequestHandler = null;
        this.metaDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, uri);
        return this;
    }

    /**
     * Sets the access stream timeout
     *
     * @param timeout
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessStreamSessionTimeout(long timeout) {
        this.accessStreamSessionTimeout = timeout;
        return this;
    }

    /**
     * Sets the access stream timeout
     *
     * @param interval
     * @return
     */
    public RegistrableOfferingDescriptionChain withAccessStreamSessionTimeout(Duration interval) {
        this.accessStreamSessionTimeout = interval.getMillis();
        return this;
    }

    /**
     * Sets the time period of the offering
     * 
     * @param from
     * @param to
     * @return
     */
    public RegistrableOfferingDescriptionChain withTimePeriod(long from, long to) {
        this.setTimePeriod(TimePeriod.create(from, to));
        return this;
    }

    public RegistrableOfferingDescriptionChain withLiveDataOnly() {
        return withTimePeriod(0L, 0L);
    }

    public RegistrableOfferingDescriptionChain withLiveDataSince(DateTime since) {
        return withTimePeriod(since.getMillis(), 0L);
    }

    /**
     * Sets the time period of the offering
     * 
     * @param from
     * @param to
     * @return
     */
    public RegistrableOfferingDescriptionChain withTimePeriod(DateTime from, DateTime to) {
        this.setTimePeriod(TimePeriod.create(from, to));
        return this;
    }

    /**
     * Sets the time period of the offering
     * 
     * @param period
     * @return
     */
    public RegistrableOfferingDescriptionChain withTimePeriod(TimePeriod period) {
        this.setTimePeriod(period);
        return this;
    }

    /**
     * Sets the server running the gateway service (only relevant in integration mode 2)
     *
     * @param server
     * @return
     */
    public RegistrableOfferingDescriptionChain deployOn(EmbededdedRouteBasedServer server) {
        setServerAndDefaultEndpoint(server);
        return this;
    }

    /**
     * Sets the protocol to HTTP POST for an external offering (only relevant in integration mode 3)
     *
     * @return
     */
    public RegistrableOfferingDescriptionChain asHttpPost() {
        this.specialTreatment = true;
        return withProtocol(EndpointType.HTTP_POST);
    }

    /**
     * Sets the protocol to HTTP GET for an external offering (only relevant in integration mode 3)
     * 
     * @return
     */
    public RegistrableOfferingDescriptionChain asHttpGet() {
        return withProtocol(EndpointType.HTTP_GET);
    }

    public RegistrableOfferingDescriptionChain asHttpPut() {
        return withProtocol(EndpointType.HTTP_PUT);
    }

    public RegistrableOfferingDescriptionChain asCoapGet() {
        return withProtocol(EndpointType.COAP_GET);
    }

    public RegistrableOfferingDescriptionChain asCoapPost() {
        return withProtocol(EndpointType.COAP_POST);
    }

    public RegistrableOfferingDescriptionChain asCoapPut() {
        return withProtocol(EndpointType.COAP_PUT);
    }

    /**
     * Sets the accept type to application/xml for an external offering (only relevant in integration mode 3)
     *
     * @return
     */

    public RegistrableOfferingDescriptionChain acceptsXml() {

        for (EndPoint endPoint : endpoints) {
            endPoint.setAcceptType(MimeType.APPLICATION_XML);
        }
        return this;
    }

    /**
     * Sets the accept type to application/json for an external offering (only relevant in integration mode 3)
     * 
     * @return
     */
    public RegistrableOfferingDescriptionChain acceptsJson() {

        for (EndPoint endPoint : endpoints) {
            endPoint.setAcceptType(MimeType.APPLICATION_JSON);
        }
        return this;
    }

    /**
     * Sets the content type to application/xml for an external offering (only relevant in integration mode 3)
     * 
     * @return
     */
    public RegistrableOfferingDescriptionChain producesXml() {
        this.specialTreatment = true;

        for (EndPoint endPoint : endpoints) {
            endPoint.setContentType(MimeType.APPLICATION_XML);
        }
        return this;
    }

    /**
     * Sets the content type to application/json for an external offering (only relevant in integration mode 3)
     * 
     * @return
     */
    public RegistrableOfferingDescriptionChain producesJson() {

        for (EndPoint endPoint : endpoints) {
            endPoint.setContentType(MimeType.APPLICATION_JSON);
        }
        return this;
    }

    /**
     * Sets the input template for a template-based parameter encoding (only relevant in integration mode 3)
     * 
     * @param requestTemplates
     * @return
     */
    public RegistrableOfferingDescriptionChain usingRequestTemplate(MessageTemplates requestTemplates) {
        this.requestTemplates = requestTemplates;
        return this;
    }

    /**
     * Sets the output template for a template-based parameter encoding (only relevant in integration mode 3)
     * 
     * @param mapping
     * @return
     */
    public RegistrableOfferingDescriptionChain usingResponseMapping(OutputMapping mapping) {
        this.mapping = mapping;
        return this;
    }

    // /**
    // * Sets the output template for a template-based parameter encoding (only relevant in integration mode 3)
    // * @param responseTemplates
    // * @return
    // */
    // public RegistrableOfferingDescriptionChain usingResponseTemplate(MessageTemplates responseTemplates) {
    // this.responseTemplates = responseTemplates;
    // return this;
    // }

}
