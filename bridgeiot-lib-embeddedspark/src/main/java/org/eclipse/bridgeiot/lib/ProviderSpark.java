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

import org.eclipse.bridgeiot.lib.embeddedspark.EmbeddedSpark;
import org.eclipse.bridgeiot.lib.embeddedspark.ServerOptionsSpark;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescriptionChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderSpark extends Provider {

    private static final Logger logger = LoggerFactory.getLogger(ProviderSpark.class);

    public ProviderSpark(String providerId, String marketplaceUri, String localDomain, int localPort) {
        super(providerId, marketplaceUri);
        server = new EmbeddedSpark(localDomain, localPort, Constants.DEFAULT_BASE_ROUTE,
                ServerOptionsSpark.defaultOptions);
        server.start();

        setBaseUrl(new StringBuilder().append("https://").append(localDomain).append(":").append(localPort).append("/")
                .append(Constants.DEFAULT_BASE_ROUTE).toString());
    }

    /**
     * Instantiates the Provider instance
     * 
     */
    public static ProviderSpark create(String providerId, String marketplaceUri, String localDomain, int localPort) {
        return new ProviderSpark(providerId, marketplaceUri, localDomain, localPort);
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

    public static ProviderSpark create(String fileName) throws FileNotFoundException {
        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load(fileName);

        return ProviderSpark.create(prop.PROVIDER_ID, prop.MARKETPLACE_URI, prop.PROVIDER_DNS_NAME, prop.PROVIDER_PORT)
                .withProxy(prop.PROXY, prop.PROXY_PORT).withProxyBypass(prop.PROXY_BYPASS)
                .withClientSecret(prop.PROVIDER_SECRET);
    }

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     *            API Key for authentication at the marketplace
     * @throws IOException
     */
    @Override
    public ProviderSpark authenticate(String clientSecret) throws IOException {
        return (ProviderSpark) super.authenticate(clientSecret);
    }

    /**
     * Authenticates instance at the Marketplace. The client secret can be specified either by the withClientSecret
     * method or by configuration based object creation
     * 
     * @throws IOException
     */
    @Override
    public ProviderSpark authenticate() throws IOException {
        return (ProviderSpark) super.authenticate();
    }

    /**
     * Creates a basic offering description for registration at the marketplace.
     * 
     * @return
     */
    @Override
    public RegistrableOfferingDescriptionChain createOfferingDescription(String localId) {
        RegistrableOfferingDescriptionChain registrableOfferingDescriptionChain = new RegistrableOfferingDescriptionChain(
                localId, this, this.clientId, this.marketplaceClient, registeredOfferingMap);

        return registrableOfferingDescriptionChain.withRoute(localId).deployOn(server);
    }

    @Override
    public RegisteredOffering register(RegistrableOfferingDescription offeringDescription)
            throws IncompleteOfferingDescriptionException, NotRegisteredException {

        if (offeringDescription.getProvider() == null) {
            offeringDescription.setProvider(this);
            offeringDescription.setProviderId(getClientId().toString());
            offeringDescription.setMarketplaceClient(getMarketplaceClient());
            offeringDescription.setOfferingMap(this.registeredOfferingMap);
            offeringDescription.setServerAndDefaultEndpoint(server);
        }
        RegisteredOffering offering = offeringDescription.register();
        registeredOfferingMap.put(offering.getOfferingId(), offering);
        return offering;
    }

    /**
     * Retrieves the offering description from the marketplace referenced by the offering ID.
     * 
     * @param offeringId
     * @return
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
    public void terminate() {
        super.terminate();
        server.stop();
    }

    /**
     * Enables proxy configuration only if it is required. This is useful if connections are sometimes from inside a
     * proxy-gated network
     * 
     * @param proxyHost
     * @param proxyPort
     * @return
     */
    @Override
    public ProviderSpark withAutoProxy(String proxyHost, int proxyPort) {
        return (ProviderSpark) super.withAutoProxy(proxyHost, proxyPort);
    }

    @Override
    public ProviderSpark withProxyBypass(String host) {

        this.addProxyBypass(host);
        return this;
    }

    @Override
    public ProviderSpark withProxy(String host, int port) {
        this.setProxy(host, port);
        return this;
    }

    @Override
    public ProviderSpark withClientSecret(String clientSecret) {

        return (ProviderSpark) super.withClientSecret(clientSecret);
    }

}
