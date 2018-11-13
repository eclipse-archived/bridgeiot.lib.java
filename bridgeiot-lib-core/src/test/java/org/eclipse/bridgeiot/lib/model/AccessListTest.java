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
public class AccessListTest {

    final static Logger logger = LoggerFactory.getLogger(AccessListTest.class);

    AccessList accessListUnderTest;

    static final String ORG1 = "Organization1";
    static final String ORG2 = "Organization2";
    static final String ORG3 = "Organization3";

    static final String EmptyAccessListGraphQLString = "accessWhiteList: [ ]";
    static final String ORG1AccessListGraphQLString = "accessWhiteList: [ \\\"" + ORG1 + "\\\" ]";
    static final String ORG13AccessListGraphQLString = "accessWhiteList: [ \\\"" + ORG1 + "\\\", \\\"" + ORG2
            + "\\\", \\\"" + ORG3 + "\\\" ]";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void createEmptyAccessList() {
        accessListUnderTest = AccessList.create();

        logger.info("accessListUnderTest.toQueryElement: {}", accessListUnderTest.toQueryElement());
        logger.info("TestString:                         {}", EmptyAccessListGraphQLString);
        assertThat(accessListUnderTest.toQueryElement()).isEqualTo(EmptyAccessListGraphQLString);
    }

    @Test
    public void createOrg1AccessList() {
        accessListUnderTest = AccessList.create().addOrganization(ORG1);

        logger.info("accessListUnderTest.toQueryElement: {}", accessListUnderTest.toQueryElement());
        logger.info("TestString:                         {}", ORG1AccessListGraphQLString);
        assertThat(accessListUnderTest.toQueryElement()).isEqualTo(ORG1AccessListGraphQLString);
    }

    @Test
    public void createOrg13AccessList() {
        accessListUnderTest = AccessList.create(ORG1, ORG2, ORG3);

        logger.info("accessListUnderTest.toQueryElement: {}", accessListUnderTest.toQueryElement());
        logger.info("TestString:                         {}", ORG13AccessListGraphQLString);
        assertThat(accessListUnderTest.toQueryElement()).isEqualTo(ORG13AccessListGraphQLString);
    }

}
