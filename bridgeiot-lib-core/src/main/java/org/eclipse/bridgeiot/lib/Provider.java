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
package org.eclipse.bridgeiot.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.offering.DeployedOffering;
import org.eclipse.bridgeiot.lib.offering.Endpoints;
import org.eclipse.bridgeiot.lib.offering.OfferingId;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescriptionChain;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables basic lifecycle operations on offerings for providers.
 *
 */
public class Provider extends BridgeIotAPI implements IProvider {

    private static final Logger logger = LoggerFactory.getLogger(Provider.class);

    protected EmbededdedRouteBasedServer server = null;
    protected Map<OfferingId, RegisteredOffering> registeredOfferingMap = new HashMap<>();
    protected String baseUrl = null;

    /**
     * Instantiates the Provider instance
     * 
     */
    public Provider(String providerId, String marketplaceUrl) {
        super(new BridgeIotClientId(providerId), marketplaceUrl);
        logger.info("Setting up provider {} with marketplace at {}", providerId, marketplaceUrl);
    }

    /**
     * Instantiates the Provider instance
     * 
     */
    public static Provider create(String providerId, String marketplaceUri) {
        return new Provider(providerId, marketplaceUri);
    }

    /**
     * Instantiates the Provider from configuration file
     * 
     * See the example using all supported fields
     * 
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public static Provider create(String fileName) throws FileNotFoundException {
        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load(fileName);

        return Provider.create(prop.PROVIDER_ID, prop.MARKETPLACE_URI).withProxy(prop.PROXY, prop.PROXY_PORT)
                .withProxyBypass(prop.PROXY_BYPASS).withClientSecret(prop.PROVIDER_SECRET);
    }

    public Provider withProxyBypass(String host) {
        this.addProxyBypass(host);
        return this;
    }

    public Provider withProxy(String host, int port) {
        this.setProxy(host, port);
        return this;
    }

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     *            API Key for authentication at the marketplace
     * @throws IOException
     */
    @Override
    public Provider authenticate(String clientSecret) throws IOException {
        return (Provider) super.authenticate(clientSecret);
    }

    /**
     * Authenticates instance at the Marketplace. The client secret can be specified either by the withClientSecret
     * method or by configuration based object creation
     * 
     * @throws IOException
     */
    @Override
    public Provider authenticate() throws IOException {
        return (Provider) super.authenticate();
    }

    /**
     * Terminates the Provider instance deregisters all offering and stops all deployed A1 Access Interfaces.
     * 
     */
    @Override
    public void terminate() {

        super.terminate();

        // Deregister all registered offerings
        for (RegisteredOffering offering : registeredOfferingMap.values()) {
            offering.deregister();
        }
        registeredOfferingMap.clear();

        marketplaceClient.close();
    }

    /**
     * Registers a new offering description at the Marketplace. Depending on the implementation, it may initiate an A1
     * interface to the offering. accessRequestHandler is called upon receiving an access request. Returns a unique ID
     * referencing the registered offering for further lifecycle operations.
     * 
     */
    @Override
    public RegisteredOffering register(RegistrableOfferingDescription offeringDescription)
            throws IncompleteOfferingDescriptionException, NotRegisteredException {

        if (offeringDescription.getProvider() == null) {
            offeringDescription.setProvider(this);
            offeringDescription.setProviderId(getClientId().toString());
            offeringDescription.setMarketplaceClient(getMarketplaceClient());
            offeringDescription.setOfferingMap(this.registeredOfferingMap);
        }
        RegisteredOffering offering = offeringDescription.register();
        registeredOfferingMap.put(offering.getOfferingId(), offering);
        return offering;
    }

    public RegisteredOffering register(RegistrableOfferingDescription offeringDescription, Endpoints endpoints)
            throws IncompleteOfferingDescriptionException, NotRegisteredException {

        endpoints.updateEndpointsForProvider(this);
        offeringDescription.internalOfferingDescriptionUpdate(endpoints);
        return register(offeringDescription);
    }

    /**
     * Activates an offering, which is already registered at the marketplace. It is assumed here that the
     * OfferingDescription has already been created using the web portal of the marketplace. No A1 Interface is
     * deployed.
     */
    @Override
    public void register(OfferingId offeringId) {
        register(offeringId, null, null);
    }

    /**
     * Activates an offering, which is already registered at the marketplace. It is assumed here that the
     * OfferingDescription has already been created using the web portal of the marketplace. It initiate anA1 interface
     * to the offering via the server argument. accessRequestHandler is called upon receiving an access request.
     * 
     */
    @Override
    public void register(OfferingId offeringId, AccessRequestHandler accessRequestHandler,
            EmbededdedRouteBasedServer server) {
        register(offeringId, accessRequestHandler, server, activate(offeringId, marketplaceClient));
    }

    /**
     * @param offeringId
     * @param accessRequestHandler
     * @param server
     * @param activatedOfferingDescription
     */
    void register(OfferingId offeringId, AccessRequestHandler accessRequestHandler, EmbededdedRouteBasedServer server,
            RegistrableOfferingDescriptionChain activatedOfferingDescription) {

        activatedOfferingDescription.setMarketplaceClient(this.marketplaceClient);
        activatedOfferingDescription.setOfferingMap(this.registeredOfferingMap);
        activatedOfferingDescription.withAccessRequestHandler(accessRequestHandler);
        activatedOfferingDescription.deployOn(server);

        if (activatedOfferingDescription.getEndpoints().isEmpty()) {
            // TODO: Merge with IncompleteOfferingDescriptionException as soon as Exception handling is clear; decide to
            // use validate() method of OfferingDescription
            throw new BridgeIoTException("OfferingDescription was incomplete");
        }

        // BIG ROUTE MAGIC - FIX THIS HERE ONCE ENDPOINT MANAGEMENT IS READY
        String route = calculateRoute(activatedOfferingDescription);

        activatedOfferingDescription.withRoute(route);

        // Return if provider has a non-A1 offering
        if (accessRequestHandler == null && server == null) {
            return;
        }

        RegisteredOffering registeredOffering = null;
        switch (activatedOfferingDescription.getAccessInterfaceType()) {
        case BRIDGEIOT_LIB:
            registeredOffering = new DeployedOffering(activatedOfferingDescription, server);
            break;
        case BRIDGEIOT_PROXY:
        case EXTERNAL:
            registeredOffering = new RegisteredOffering(activatedOfferingDescription);
            break;
        case UNSPECIFIED:
            throw new BridgeIoTException("Unsupported integration mode");
        }

        registeredOfferingMap.put(offeringId, registeredOffering);

    }

    // BIG ROUTE MAGIC - FIX THIS HERE ONCE ENDPOINT MANAGEMENT IS READY
    static String calculateRoute(RegistrableOfferingDescriptionChain activatedOfferingDescription) {
        String uri = activatedOfferingDescription.getEndpoints().get(0).getUri();
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    /*
     * Redundant to register. Used as internal function shouldn't be exposed to outside.
     * 
     */
    static RegistrableOfferingDescriptionChain activate(OfferingId offeringId, MarketplaceClient marketplaceClient) {
        String activationString = GraphQLQueries.getActivationStringFullResponse(offeringId.getValue());
        logger.info("Activation Request: {}", activationString);

        try {

            Response response = marketplaceClient.request(activationString);

            if (!response.isSuccessful()) {
                throw new BridgeIoTException("Activation request to eXchange was not successful!");
            }

            String responseString = response.body().string();
            response.close();

            return Helper.unmarshallSingleFromQueryResponse("activateOffering", responseString,
                    RegistrableOfferingDescriptionChain.class);

        } catch (IOException | BridgeIoTException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Registration failed!", e);
        }
    }

    /**
     * Deactivates an offering on the marketplace per offering id. The offering cannot be discovered by consumers
     * anymore.
     */
    static void deactivate(OfferingId offeringId, MarketplaceClient marketplaceClient) {

        String deactivation = GraphQLQueries.getDeactivationString(offeringId.getValue());
        logger.info("Deactivation Request: {}", deactivation);

        // Deactivate offering on Marketplace"
        try {
            Response response = marketplaceClient.request(deactivation);
            response.close();
        } catch (IOException e) {
            logger.error("Registration failed {}", e.getMessage());
        }

    }

    /**
     * Deactivates an OfferingDescription on the marketplace. The offering cannot be discovered by consumers anymore.
     */
    @Deprecated
    protected void deactivate(RegistrableOfferingDescription offeringDescription) {
        offeringDescription.deregister();
    }

    /**
     * Deregisters an offering. The offering is removed from the marketplace.
     * 
     */
    @Override
    public void deregister(RegistrableOfferingDescription offeringDescription) {
        deregister(offeringDescription.getOfferingId());
    }

    /**
     * Deregisters an offering per offering ID. The offering is removed from the marketplace.
     * 
     */
    @Override
    public void deregister(OfferingId offeringId) {
        if (registeredOfferingMap.containsKey(offeringId)) {
            registeredOfferingMap.get(offeringId).deregister();
            registeredOfferingMap.remove(offeringId);
        }
    }

    /**
     * Creates a basic offering description for registration at the marketplace.
     * 
     */
    @Override
    public RegistrableOfferingDescriptionChain createOfferingDescription(String localId) {
        return new RegistrableOfferingDescriptionChain(localId, this, this.clientId, this.marketplaceClient,
                this.registeredOfferingMap);
    }

    public Map<OfferingId, RegisteredOffering> getRegisteredOfferingMap() {
        return this.registeredOfferingMap;
    }

    public EmbededdedRouteBasedServer getEmbeddedServer() {
        return this.server;
    }

    protected void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    /**
     * Retrieves the offering description from the marketplace referenced by the offering ID.
     * 
     * @throws IOException
     * @throws InvalidOfferingException
     */
    @Override
    public RegistrableOfferingDescriptionChain createOfferingDescriptionFromOfferingId(String offeringId)
            throws InvalidOfferingException, IOException {
        RegistrableOfferingDescriptionChain registrableOfferingDescriptionChain = createOfferingDescription("");
        return registrableOfferingDescriptionChain.useOfferingDescription(offeringId);
    }

    @Override
    protected void prepareAccountingReport() {
        for (RegisteredOffering offering : registeredOfferingMap.values()) {
            if (offering instanceof DeployedOffering) {
                addAccountingReports(((DeployedOffering) offering).getAccountingReports());
            }
        }
    }

    @Override
    public Provider withAutoProxy(String proxyHost, int proxyPort) {
        return (Provider) super.withAutoProxy(proxyHost, proxyPort);
    }

    @Override
    public Provider withClientSecret(String clientSecret) {

        return (Provider) super.withClientSecret(clientSecret);
    }

}
