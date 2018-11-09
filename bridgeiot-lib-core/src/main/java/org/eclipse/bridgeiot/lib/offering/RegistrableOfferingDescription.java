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

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.BridgeIotClientId;
import org.eclipse.bridgeiot.lib.IProvider;
import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.HttpErrorException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessStreamFilterHandler;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.Activation;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Offering Description with methods for registration at the marketplace
 *
 */
public class RegistrableOfferingDescription extends OfferingDescription implements IRegistrable {

    private static final long DEFAULT_OFFERING_EXPIRATION_INTERVAL = 10 * 60 * 1000L; // 10 min

    private static final Logger logger = LoggerFactory.getLogger(RegistrableOfferingDescription.class);
    @JsonIgnore
    protected RegisteredOffering registeredOffering;

    @JsonIgnore
    protected IProvider provider;

    @JsonIgnore
    protected EmbededdedRouteBasedServer server;
    @JsonIgnore
    protected AccessRequestHandler accessRequestHandler;
    @JsonIgnore
    protected String route = "aone";

    @JsonIgnore
    protected boolean specialTreatment = false; // Indicates that the Offering Description won't be understood by the
                                                // marketplace. If true the offering description will be hidden in an
                                                // understood offering description

    @JsonIgnore
    private MarketplaceClient marketplaceClient;
    @JsonIgnore
    private Map<OfferingId, RegisteredOffering> providerOfferingMap;
    private OfferingId offeringId = null;
    @JsonIgnore
    private long expirationInterval = DEFAULT_OFFERING_EXPIRATION_INTERVAL;

    // scheduled executor to re-register the offering prior to expiration
    @JsonIgnore
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @JsonIgnore
    protected AccessStreamFilterHandler accessStreamFilterHandler;
    @JsonIgnore
    protected AccessRequestHandler sampleDataAccessRequestHandler;
    @JsonIgnore
    protected AccessRequestHandler metaDataAccessRequestHandler;
    @JsonIgnore
    protected Long accessStreamSessionTimeout = 0L;

    public RegistrableOfferingDescription() {
    }

    public RegistrableOfferingDescription(String localId, IProvider provider, BridgeIotClientId clientId,
            MarketplaceClient marketplaceClient, Map<OfferingId, RegisteredOffering> providerOfferingMap) {
        super(localId, clientId);
        this.provider = provider;
        this.marketplaceClient = marketplaceClient;
        this.providerOfferingMap = providerOfferingMap;
    }

    /**
     * Registers an Offering at the marketplace and deploys A1 interface if applicable. Returns a RegisteredOffering
     * object for lifecycle operations.
     * 
     * @param localId
     * @return
     * @throws NotRegisteredException
     */
    @Override
    public RegisteredOffering register() throws IncompleteOfferingDescriptionException, NotRegisteredException {

        if (this.getProviderId() == null) {
            throw new BridgeIoTException(
                    "Provider not set! Use 'offeringDescription.register(provider, enpoints);'!!!");
        }

        if (registeredOffering != null) {
            logger.warn("A RegistrableOfferingDescription can be registered only once - skip registration!");
            return registeredOffering;
        }

        registerOnMarketplace();

        switch (getAccessInterfaceType()) {
        case BRIDGEIOT_LIB:
            registeredOffering = new DeployedOffering(this, server);
            break;
        case BRIDGEIOT_PROXY:
        case EXTERNAL:
            registeredOffering = new RegisteredOffering(this);
            break;
        default:
            throw new BridgeIoTException("Unsupported integration mode");
        }

        providerOfferingMap.put(this.offeringId, registeredOffering);

        return registeredOffering;

    }

    public RegisteredOffering register(Provider provider, Endpoints endpoints)
            throws IncompleteOfferingDescriptionException, NotRegisteredException {

        setProvider(provider);
        setProviderId(provider.getClientId().toString());
        setMarketplaceClient(provider.getMarketplaceClient());
        setOfferingMap(provider.getRegisteredOfferingMap());
        setEmbeddedServer(provider.getEmbeddedServer());
        endpoints.updateEndpointsForProvider(provider);
        internalOfferingDescriptionUpdate(endpoints);

        return register();
    }

    protected void registerOnMarketplace() throws IncompleteOfferingDescriptionException, NotRegisteredException {

        if (marketplaceClient == null) {
            throw new NotRegisteredException();
        }

        validate();

        // Duplicate inputs and outputs in extension field because their data structure is recursive and dynamic. This
        // and has to be known by the consumer lib when doing the graphql query during the discovery.
        this.extension1 = "{ \"inputs\":" + Helper.getPojoAsJsonCompact(this.inputs) + ", \"outputs\":"
                + Helper.getPojoAsJsonCompact(this.outputs) + "}";

        String registration = GraphQLQueries.getRegistrationString(this);

        logger.info("Registration Request: {}", registration);

        // Register offering on Marketplace
        try {

            Response response = marketplaceClient.request(registration);

            String responseString = response.body().string();

            if (!response.isSuccessful()) {
                logger.error("ERROR: Registration request to eXchange was not successful! \n  ---> Response: {}",
                        responseString);
                throw new HttpErrorException(response.code(), responseString);
            }
            response.close();

            OfferingDescription od = Helper.unmarshallSingleFromQueryResponse("addOffering", responseString,
                    OfferingDescription.class);

            // If registration was successful, add a unique ID to the offering and return to the caller
            this.setId(od.getId());
            this.setActivation(new Activation(od.getActivation().getStatus(), od.getActivation().getExpirationTime()));

            logger.info("Registration Response: id={}, expirationTime={}", this.getId(),
                    Helper.unixTimeToJoda(this.getActivation().getExpirationTime()));

            this.offeringId = new OfferingId(od.getId());

            // Create schedule for timely re-registration - to avoid expiration of the offering
            Long timeToReregister = Math.max(this.getActivation().getExpirationTime() - new Date().getTime() - 60000L,
                    1000);
            executor.schedule(registrationRunnable, timeToReregister, TimeUnit.MILLISECONDS);

        } catch (IOException | HttpErrorException e) {

            logger.error("ERROR: Registration failed ... try again in 30 seconds! \n ---> Exception: {}",
                    e.getMessage());
            logger.error(e.getMessage(), e);

            executor.schedule(registrationRunnable, 30, TimeUnit.SECONDS);

        } catch (BridgeIoTException e) {

            logger.error(
                    "ERROR: JSONException when processing Offering registration response - Offering Description is invalid!");
            throw new NotRegisteredException();

        }

    }

    protected void getOfferingDescription(String offeringId) throws InvalidOfferingException, IOException {

        OfferingDescription fetchedOfferingDescription = marketplaceClient.getOfferingDescription(offeringId);

        this.updateOfferingDescription(fetchedOfferingDescription);

    }

    /**
     * Deregisters corresponding offering at the marketplace
     */

    @Override
    public void deregister() {

        // stop re-registration
        executor.shutdownNow();

        deactivate();

        providerOfferingMap.remove(getOfferingId());

    }

    protected void validate() throws IncompleteOfferingDescriptionException {
        if (this.endpoints.isEmpty()) {
            throw new IncompleteOfferingDescriptionException();
        }
    }

    /**
     * Deactivates the corresponding inactive offering at the marketplace
     * 
     */
    protected void deactivate() {

        String deactivation = GraphQLQueries.getDeactivationString(this.getId());

        logger.info("Deactivation Request: {}", deactivation);

        try {

            Response response = marketplaceClient.request(deactivation);
            response.close();
            if (getActivation() != null) {
                getActivation().setStatus(false);
            }

        } catch (IOException e) {
            logger.error("Registration failed (OfferingId = {}): {}", this.getId(), e.getMessage());
        }

    }

    public OfferingId getOfferingId() {
        return offeringId;
    }

    /**
     * internal
     * 
     */
    // Refactor in order to reduce visibility
    public void setMarketplaceClient(MarketplaceClient marketplaceClient) {
        this.marketplaceClient = marketplaceClient;
    }

    /**
     * internal
     * 
     */
    // Refactor in order to reduce visibility
    @JsonIgnore
    public MarketplaceClient getMarketplaceClient() {
        return this.marketplaceClient;
    }

    /**
     * internal
     * 
     */
    // Refactor in order to reduce visibility
    public void setOfferingMap(Map<OfferingId, RegisteredOffering> offerings) {
        this.providerOfferingMap = offerings;
    }

    /**
     * Return route of A1 interface if applicable
     * 
     * @return
     */
    public String getRoute() {
        return route;
    }

    public IProvider getProvider() {
        return provider;
    }

    public void setProvider(IProvider provider) {
        this.provider = provider;
    }

    protected void setEmbeddedServer(EmbededdedRouteBasedServer server) {
        this.server = server;
    }

    protected AccessRequestHandler getAccessRequestHandler() {
        return accessRequestHandler;
    }

    protected AccessRequestHandler getSampleDataAccessRequestHandler() {
        return sampleDataAccessRequestHandler;
    }

    protected AccessRequestHandler getMetaDataAccessRequestHandler() {
        return metaDataAccessRequestHandler;
    }

    /**
     * Sets the server running the gateway service (only relevant in integration mode 2)
     *
     * @param server
     * @return
     */
    public void setServerAndDefaultEndpoint(EmbededdedRouteBasedServer server) {
        if (endpoints.isEmpty()) {
            endpoints.add(new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB, server.getBaseUrl()));
        }
        endpoints.get(0).setEndpointType(EndpointType.HTTP_GET); // TODO: Currently only HTTP supported - set as default
        this.server = server;
    }

    public void internalOfferingDescriptionUpdate(Endpoints eps) {
        this.route = eps.getRoute();
        this.accessRequestHandler = eps.getAccessRequestHandler();
        if (eps.getAccessRequestEndpoint() != null) {
            this.endpoints.add(0, eps.getAccessRequestEndpoint());
            this.setAccessInterfaceType(eps.getAccessRequestEndpoint().getAccessInterfaceType());
        }
        this.accessStreamFilterHandler = eps.getAccessStreamFilterHandler();
        this.metaDataAccessRequestHandler = eps.getMetaDataAccessRequestHandler();
        this.metaDataEnpoint = eps.getMetaDataEnpoint();
        this.sampleDataAccessRequestHandler = eps.getSampleDataAccessRequestHandler();
        this.sampleDataEnpoint = eps.getSampleDataEnpoint();
    }

    /**
     * internal
     * 
     */
    @JsonIgnore
    public long getExpirationInterval() {
        return expirationInterval;
    }

    protected void setExpirationInterval(long interval) {
        this.expirationInterval = interval;
    }

    public RegistrableOfferingDescription cloneForOtherProvider() {
        return cloneForOtherProvider(this.getLocalId());
    }

    public RegistrableOfferingDescriptionChain cloneForOtherProvider(String localId) {
        RegistrableOfferingDescriptionChain clonedOfferingDescription = new RegistrableOfferingDescriptionChain();
        clonedOfferingDescription.setLocalId(localId);
        clonedOfferingDescription.updateOfferingDescription(this);
        clonedOfferingDescription.provider = null;
        clonedOfferingDescription.registeredOffering = null;
        return clonedOfferingDescription;
    }

    protected void terminate() {
        // stop re-registration
        executor.shutdownNow();
    }

    // Needed to force termination of all executor threads (even if the Consumer/Provider does not call the .terminate()
    // method)
    @Override
    protected void finalize() throws Throwable {
        try {
            terminate();
        } finally {
            super.finalize();
        }
    }

    private Runnable registrationRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                registerOnMarketplace();
            } catch (IncompleteOfferingDescriptionException e) {
                logger.error("Re-registration of Offering with ID: {} failed!", offeringId.getValue());
                logger.error(e.getMessage(), e);
            } catch (NotRegisteredException e) {
                logger.error("Re-registration of Offering with ID failed due to being not registered at marketplace!");
                logger.error(e.getMessage(), e);
            }
        }
    };

}
