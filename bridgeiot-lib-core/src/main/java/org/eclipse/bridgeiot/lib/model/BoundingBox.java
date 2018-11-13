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

public class BoundingBox implements QueryElement {

    Location l1 = null;
    Location l2 = null;

    public BoundingBox() {
    }

    public BoundingBox(Location l1, Location l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    public static BoundingBox create(Location l1, Location l2) {
        return new BoundingBox(l1, l2);
    }

    public Location getL1() {
        return l1;
    }

    public void setL1(Location l1) {
        this.l1 = l1;
    }

    public Location getL2() {
        return l2;
    }

    public void setL2(Location l2) {
        this.l2 = l2;
    }

    @Override
    public String toQueryElement() {

        return new StringBuilder().append("boundary: { l1: { ").append(getL1().toQueryElement()).append(" }, l2: { ")
                .append(getL2().toQueryElement()).append(" } }").toString();
    }

    @Override
    public String toString() {

        return new StringBuilder().append("BoundingBox=[").append(getL1().toString()).append("],[")
                .append(getL2().toString()).append("]").toString();
    }

}
