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

public class Region implements QueryElement {

    BoundingBox boundingBox = null;
    String name = null;

    public Region() {
    }

    public Region(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Region(String name) {
        this.name = name;
    }

    public static Region create(BoundingBox boundingBox) {
        return new Region(boundingBox);
    }

    public static Region create(String name) {
        return new Region(name);
    }

    public static Region create(City city) {
        return new Region(city.getName());
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @JsonProperty("boundary")
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("city")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toQueryElement() {

        StringBuilder stringBuilder = new StringBuilder();

        if (getBoundingBox() != null) {
            stringBuilder.append("spatialExtent: { city: \\\"\\\" ").append(getBoundingBox().toQueryElement())
                    .append(" }");
        } else {
            if (getName() != null) {
                stringBuilder.append("spatialExtent: { city: \\\"").append(getName()).append("\\\" }");
            }
        }

        return stringBuilder.toString();
    }

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Region=");

        if (getBoundingBox() != null) {
            stringBuilder.append("[").append(getBoundingBox().toString()).append("]");
            if (getName() != null) {
                stringBuilder.append(",");
            }
        }

        if (getName() != null) {
            stringBuilder.append("[name:").append(getName()).append("]");
        }

        return stringBuilder.toString();
    }

}
