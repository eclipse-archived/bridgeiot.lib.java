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
package org.eclipse.bridgeiot.lib.misc;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.eclipse.bridgeiot.lib.model.Area;
import org.eclipse.bridgeiot.lib.model.Location;

/**
 * Convenience functions for random number generation based on Apache Commons Math
 */
public class Random {

    public static final String TEST = "TEST";

    private static final RandomDataGenerator RANDOM_GEN = init();

    private static RandomDataGenerator init() {

        final RandomDataGenerator random = new RandomDataGenerator();

        return random;
    }

    /**
     * Generates a double in an interval with uniform distribution
     *
     * @param lower
     * @param upper
     * @return
     */
    public static double getNextUniform(double lower, double upper) {
        return RANDOM_GEN.nextUniform(lower, upper);
    }

    /**
     * Generates a long in an interval with uniform distribution
     *
     * @param lower
     * @param upper
     * @return
     */
    public static long getNextUniform(long lower, long upper) {
        return (long) Math.floor(RANDOM_GEN.nextUniform(lower, upper));
    }

    /**
     * Generates a random boolean
     *
     * @return
     */
    public static boolean nextBoolean() {
        return getNextUniform(0, 1) == 1 ? true : false;
    }

    /**
     * Generates a random location in an spatial area.
     *
     * @param area
     * @return
     */
    public static Location getRandomPositionInside(Area area) {
        return getRandomPositionInside(area, 6);
    }

    /**
     * Generates a random location in an spatial area with a fixed precision
     *
     * @param area
     * @param digits
     * @return
     */
    public static Location getRandomPositionInside(Area area, int digits) {
        final double x = RANDOM_GEN.nextUniform(0, 1.0, true);
        final double y = RANDOM_GEN.nextUniform(0, 1.0, true);
        final double latitude = Helper.round(
                area.getSouthWestCorner().getLatitude()
                        + (area.getNorthEastCorner().getLatitude() - area.getSouthWestCorner().getLatitude()) * y,
                digits);
        final double longitude = Helper.round(
                area.getSouthWestCorner().getLongitude()
                        + (area.getNorthEastCorner().getLongitude() - area.getSouthWestCorner().getLongitude()) * x,
                digits);

        return new Location(latitude, longitude);
    }
}
