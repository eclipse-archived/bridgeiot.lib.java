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
package org.eclipse.bridgeiot.lib.examples;

import java.io.IOException;
import java.util.Scanner;

import org.eclipse.bridgeiot.lib.Provider;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;

public class StarWarsProvider {

    public static void main(String[] args)
            throws IOException, IncompleteOfferingDescriptionException, NotRegisteredException {

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        Provider provider = new Provider(prop.PROVIDER_ID, prop.MARKETPLACE_URI);

        // provider.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // provider.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate provider on the marketplace
        provider.authenticate(prop.PROVIDER_SECRET);

        RegistrableOfferingDescription offeringDescription = provider.createOfferingDescription("starWarsOffering")
                .withName("Star Wars Registry").withCategory("urn:proposed:Miscellaneous").inRegion("Knossa")
                .withPricingModel(PricingModel.PER_ACCESS).withLicenseType(LicenseType.OPEN_DATA_LICENSE)
                .addInputDataInRoute("resourceType", "schema:resourceType", ValueType.TEXT)
                .addInputDataInRoute("resourceId", "schema:resourceId", ValueType.NUMBER).asHttpGet().acceptsJson()
                .producesJson().onExternalEndpoint("http://swapi.co/api/@@resourceType@@/@@resourceId@@");

        System.out.println(Helper.getPojoAsJson(offeringDescription));

        RegisteredOffering registeredOffering = offeringDescription.register();

        // Run until user input is obtained
        System.out.println(">>>>>>  Terminate ExampleProvider by pressing ENTER  <<<<<<");
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
        keyboard.close();

        // Deregister your offering form Marketplace
        // provider.deregister(offeringDescription);
        // or
        registeredOffering.deregister();

        // Terminate provider instance
        provider.terminate();
    }

}
