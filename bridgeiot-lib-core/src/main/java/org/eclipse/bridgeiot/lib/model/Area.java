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

/**
 * Geographical area based on Location
 * 
 */
public class Area {

    Location southWestCorner;
    Location northEastCorner;

    public Area(Location firstCorner, Location secondCorner) {
        super();
        this.northEastCorner = new Location(Math.max(firstCorner.getLatitude(), secondCorner.getLatitude()),
                Math.max(firstCorner.getLongitude(), secondCorner.getLongitude()));
        this.southWestCorner = new Location(Math.min(firstCorner.getLatitude(), secondCorner.getLatitude()),
                Math.min(firstCorner.getLongitude(), secondCorner.getLongitude()));
    }

    public Location getSouthWestCorner() {
        return southWestCorner;
    }

    public Location getNorthEastCorner() {
        return northEastCorner;
    }

    @Override
    public String toString() {
        return "[ " + northEastCorner + " - " + southWestCorner + " ]";
    }

}
