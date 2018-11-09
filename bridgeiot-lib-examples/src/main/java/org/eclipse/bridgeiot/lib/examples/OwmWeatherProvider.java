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

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.ProviderSpark;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.misc.BridgeIotProperties;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.misc.HttpClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.LicenseType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.PricingModel;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.Price.Euros;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for using Bridge.IoT API as a Provider.
 * 
 * An offering is created and registered for the weather service of Open Weather Map. Please visit
 * http://openweathermap.org and obtain a free API key.
 */
public class OwmWeatherProvider {

    private static final String OPEN_WEATHER_DATA_BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String API_KEY = "1a828452b77d286811b69bb957252a9a"; // INSERT Open Weather Map API KEY HERE!

    private static final Logger logger = LoggerFactory.getLogger(OwmWeatherProvider.class);

    public static void main(String[] args)
            throws IncompleteOfferingDescriptionException, IOException, NotRegisteredException {

        HttpClient httpClient = HttpClient.createHttpClient();

        // Load example properties file
        BridgeIotProperties prop = BridgeIotProperties.load("example.properties");

        ProviderSpark provider = ProviderSpark.create(prop.PROVIDER_ID, prop.MARKETPLACE_URI, prop.PROVIDER_DNS_NAME,
                8081);

        // provider.setProxy("127.0.0.1", 3128); //Enable this line if you are behind a proxy
        // provider.addProxyBypass("172.17.17.100"); //Enable this line and the addresses for internal hosts

        // Authenticate provider instance on the marketplace
        provider.authenticate(prop.PROVIDER_SECRET);

        // Construct Offering Description of your Offering incrementally
        RegistrableOfferingDescription offeringDescription = provider.createOfferingDescription("owdWeatherOnLocation")
                .withName("Weather on Location").withCategory("urn:proposed:Miscellaneous")
                .addInputData("latitude", "schema:latitude", ValueType.NUMBER)
                .addInputData("longitude", "schema:longitude", ValueType.NUMBER)
                .addOutputData("value", "schema:random", ValueType.NUMBER).inRegion("Stuttgart")
                .withPrice(Euros.amount(0.001)).withPricingModel(PricingModel.PER_ACCESS)
                .withLicenseType(LicenseType.OPEN_DATA_LICENSE)
                // Below is actually Offering specific
                // .withRoute("weatherOnLocation")
                .withAccessRequestHandler((od, d, sub, cons) -> {
                    double longitude;
                    double latitude;

                    if (!d.containsKey("longitude"))
                        return BridgeIotHttpResponse.error().withBody("Parameter longitude missing").withStatus(422)
                                .asType("text/plain");
                    longitude = new Double((String) d.get("longitude"));

                    if (!d.containsKey("latitude"))
                        return BridgeIotHttpResponse.error().withBody("Parameter latitude missing").withStatus(422)
                                .asJsonType();
                    ;
                    latitude = new Double((String) d.get("latitude"));

                    Response response;
                    try {
                        response = httpClient.get(OPEN_WEATHER_DATA_BASE_URL + "?lat=" + latitude + "&lon=" + longitude
                                + "&APPID=" + API_KEY);
                        if (response.isSuccessful()) {
                            String jsonString = response.body().string();

                            String remappedJsonString = Helper.remapJson(jsonString,
                                    OutputMapping.create().addNameMapping("coord.lat", "location.lat")
                                            .addNameMapping("coord.lon", "location.lng")
                                            .addNameMapping("weather[0].description", "description")
                                            .addNameMapping("main.temp", "temperature")
                                            .addNameMapping("main.temp_max", "maxTemperature")
                                            .addNameMapping("main.humidity", "humidity")
                                            .addNameMapping("main.pressure", "pressure")
                                            .addNameMapping("wind.speed", "windSpeed")
                                            .addNameMapping("wind.deg", "windDirection"));

                            return BridgeIotHttpResponse.okay().withBody(remappedJsonString).asJsonType();
                        }
                        logger.error(response.body().string());
                        return BridgeIotHttpResponse.error().withBody("Backend access fails (" + response.code() + ")")
                                .withStatus(500).asType("text/plain");
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                        return BridgeIotHttpResponse.error().withBody("Backend access fails on connect").withStatus(500)
                                .asJsonType();
                    }

                });

        // Register OfferingDescription on Marketplace - this will create a local endpoint based on the embedded Spark
        // Web server
        RegisteredOffering offering = offeringDescription.register();

        // Run until user presses the ENTER key
        System.out.println(">>>>>>  Terminate ExampleProvider by pressing ENTER  <<<<<<");
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
        keyboard.close();

        logger.info("Deregister Offering");

        // Deregister the Offering from the Marketplace
        offering.deregister();

        // Terminate the Provider instance
        provider.terminate();

    }

}
