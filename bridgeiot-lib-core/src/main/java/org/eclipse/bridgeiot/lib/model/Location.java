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
package org.eclipse.bridgeiot.lib.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Location as a 2D Location based on latitude and longitude
 * 
 */
public class Location implements QueryElement {

    private double latitude;
    private double longitude;

    public Location() {
        // Needed for jackson deserializing
    }

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Location create(double latitude, double longitude) {
        return new Location(latitude, longitude);
    }

    public double getLatitude() {
        return latitude;
    }

    @JsonProperty("lat")
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @JsonProperty("lng")
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toQueryElement() {

        return new StringBuilder().append("lat:").append(getLatitude()).append(", lng:").append(this.getLongitude())
                .toString();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("lat:").append(getLatitude()).append(",lng:").append(this.getLongitude())
                .toString();
    }

}
