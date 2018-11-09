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
package org.eclipse.bridgeiot.lib.examples.types;

import org.eclipse.bridgeiot.lib.model.Location;

public class Weather {

    public Location location;

    public double temperature;

    public double maxTemperature;

    public double humidity;

    public double pressure;

    public double windSpeed;

    public double windDirection;

    public String description;

    public Weather() {
        // Needed for Jackson deserializing
    }

    @Override
    public String toString() {
        return "Weather at " + location + " with temperature=" + temperature + ", maxTemperature=" + maxTemperature
                + ", humidity=" + humidity + ", pressure=" + pressure + ", windSpeed=" + windSpeed + ", windDirection="
                + windDirection + ", description=" + description;
    }

}
