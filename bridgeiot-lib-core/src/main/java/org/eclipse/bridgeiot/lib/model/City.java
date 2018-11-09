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

public class City implements QueryElement {

    public String name;

    City() {
    }

    City(String name) {
        this.name = name;
    }

    public static City create(String name) {
        return new City(name);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toQueryElement() {
        return new StringBuilder().append("city: ").append(getName()).toString();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("City=").append(getName()).toString();
    }

}
