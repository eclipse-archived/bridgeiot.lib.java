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

import java.io.IOException;
import java.util.List;

import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.offering.OfferingId;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescriptionChain;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;

/**
 * Enables basic lifecycle operations on offerings for providers.
 *
 */
public interface IProvider {

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     * @throws IOException
     */

    Provider authenticate(String clientSecret) throws IOException;

    /**
     * Registers a new offering description at the Marketplace. Depending on the implementation, it may initiate an A1
     * interface to the offering. accessRequestHandler is called upon receiving an access request. Returns a unique ID
     * referencing the registered offering for further lifecycle operations.
     * 
     * @param offeringDescription
     * @return
     * @throws IncompleteOfferingDescriptionException
     * @throws NotRegisteredException
     */
    RegisteredOffering register(RegistrableOfferingDescription offeringDescription)
            throws IncompleteOfferingDescriptionException, NotRegisteredException;

    /**
     * Activates an offering, which is already registered at the marketplace. It is assumed here that the
     * OfferingDescription has already been created using the web portal of the marketplace. No A1 Interface is
     * deployed.
     * 
     * @param offeringId
     */
    void register(OfferingId offeringId);

    /**
     * Activates an offering, which is already registered at the marketplace. It is assumed here that the
     * OfferingDescription has already been created using the web portal of the marketplace. It initiate anA1 interface
     * to the offering via the server argument. accessRequestHandler is called upon receiving an access request.
     * 
     * @param offeringId
     * @param accessRequestHandler
     * @param server
     */
    void register(OfferingId offeringId, AccessRequestHandler accessRequestHandler, EmbededdedRouteBasedServer server);

    /**
     * Adds an Accounting Report to the list for later reporting
     * 
     * @param report
     */
    void addAccountingReports(List<AccountingReport> reportList);

    /**
     * Deregisters an offering descripton from the Marketplace. If an A1 interface is deployed, it will be also
     * deactivated.
     * 
     * @param offeringDescription
     */
    void deregister(RegistrableOfferingDescription offeringDescription);

    /**
     * Deregisters an offering descripton from the Marketplace based on the OfferingID. If an A1 interface is deployed,
     * it will be also deactivated.
     * 
     * @param offeringId
     */
    void deregister(OfferingId offeringId);

    /**
     * Terminates the Provider instance deregisters all offering and stops all deployed A1 Access Interfaces.
     * 
     */
    void terminate();

    /**
     * Fetch base URL of the Provider
     * 
     */
    String getBaseUrl();

    /**
     * Creates a basic offering description for registration at the marketplace.
     *
     * @param localId
     * @return
     */
    RegistrableOfferingDescriptionChain createOfferingDescription(String localId);

    /**
     * Retrieves the offering description from the marketplace referenced by the offering ID.
     * 
     * @param offeringId
     * @return
     */
    RegistrableOfferingDescriptionChain createOfferingDescriptionFromOfferingId(String offeringId)
            throws InvalidOfferingException, IOException;

}
