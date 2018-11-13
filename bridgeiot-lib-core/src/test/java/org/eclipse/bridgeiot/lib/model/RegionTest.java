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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class RegionTest {

    final static Logger logger = LoggerFactory.getLogger(RegionTest.class);

    Region regionUnderTest;

    static final double LAT = 41.123;
    static final double LNG = 9.234;
    static final String cityName = "Stuttgart";
    static final Location L1 = Location.create(LAT, LNG);
    static final Location L2 = Location.create(LAT, LNG);
    static final BoundingBox box = BoundingBox.create(L1, L2);
    static final String RegionGraphQLString_City = "spatialExtent: { city: \\\"" + cityName + "\\\" }";
    static final String RegionGraphQLString_BoundingBox = "spatialExtent: { city: \\\"\\\" boundary: { l1: { lat:"
            + String.valueOf(LAT) + ", lng:" + String.valueOf(LNG) + " }, " + "l2: { lat:" + String.valueOf(LAT)
            + ", lng:" + String.valueOf(LNG) + " } } }";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void createBoundingBox() {
        regionUnderTest = Region.create(box);
        assertThat(regionUnderTest.getBoundingBox()).isEqualTo(box);
        assertThat(regionUnderTest.getBoundingBox().getL1()).isEqualTo(L1);
        assertThat(regionUnderTest.getBoundingBox().getL1().getLatitude()).isEqualTo(LAT);
        assertThat(regionUnderTest.getBoundingBox().getL2()).isEqualTo(L2);
        assertThat(regionUnderTest.getBoundingBox().getL2().getLongitude()).isEqualTo(LNG);

        logger.info("regionUnderTest.toGraphQLString: {}", regionUnderTest.toQueryElement());
        logger.info("TestString:                      {}", RegionGraphQLString_BoundingBox);
        assertThat(regionUnderTest.toQueryElement()).isEqualTo(RegionGraphQLString_BoundingBox);
    }

    @Test
    public void createCity() {
        regionUnderTest = Region.create(cityName);
        assertThat(regionUnderTest.getName()).isEqualTo(cityName);

        logger.info("regionUnderTest.toGraphQLString: {}", regionUnderTest.toQueryElement());
        logger.info("TestString:                      {}", RegionGraphQLString_City);
        assertThat(regionUnderTest.toQueryElement()).isEqualTo(RegionGraphQLString_City);
    }

}
