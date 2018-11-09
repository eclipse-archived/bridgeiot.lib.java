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

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class TimePeriodTest {

    final static Logger logger = LoggerFactory.getLogger(TimePeriodTest.class);

    TimePeriod timePeriodUnderTest;

    static final long FROM = 2323452434L;
    static final long TO = 234545345L;
    static final String TimePeriodGraphQLString = "temporalExtent: { from: " + FROM + ", to: " + TO + " }";

    static final DateTime FROM_DATE = new DateTime(2017, 1, 1, 0, 0, 0);
    static final DateTime TO_DATE = new DateTime();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void createTimePeriod() {
        timePeriodUnderTest = TimePeriod.create(FROM, TO);
        assertThat(timePeriodUnderTest.getFrom()).isEqualTo(FROM);
        assertThat(timePeriodUnderTest.getTo()).isEqualTo(TO);
        logger.info("timePeriodUnderTest.toGraphQLString: {}", timePeriodUnderTest.toQueryElement());
        logger.info("TestString:                          {}", TimePeriodGraphQLString);
        assertThat(timePeriodUnderTest.toQueryElement()).isEqualTo(TimePeriodGraphQLString);
    }

    @Test
    public void createTimePeriodWithDate() {
        logger.info("FROM DATE: long = {}, string = {}", FROM_DATE.getMillis(), FROM_DATE.toString());
        logger.info("TO DATE:   long = {}, string = {}", TO_DATE.getMillis(), TO_DATE.toString());
        timePeriodUnderTest = TimePeriod.create(FROM_DATE, TO_DATE);
        assertThat(timePeriodUnderTest.getFrom()).isEqualTo(FROM_DATE.getMillis());
        assertThat(timePeriodUnderTest.getTo()).isEqualTo(TO_DATE.getMillis());
    }

}
