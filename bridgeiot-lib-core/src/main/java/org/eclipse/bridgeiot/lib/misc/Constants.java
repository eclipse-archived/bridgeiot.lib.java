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

import org.joda.time.Duration;

public class Constants {

    /**
     * The constructor is private as this is a constants class
     */
    private Constants() {

    }

    public static final String discoverySubUrl = "/graphql";
    public static final Duration syncFeedInterval = Duration.standardSeconds(5);
    public static final String COMPLEX_PARAMETER_KEY = "_jep";
    public static final String HTTP = null;
    public static final String nl = System.getProperty("line.separator");
    public static final String wwwFolder = "www";
    public static final String wwwBigIot = "bigiot";
    public static final String UNKNOWN_SUBSCRIPTION_ID = "unknown";
    public static final String DEFAULT_BASE_ROUTE = "bigiot/access";
    public static final String SAMPLEDATA_ROUTE = "sampledata/";
    public static final String METADATA_ROUTE = "metadata/";
    public static final String DOUBLE_QUOTE_ESCAPE = "$";
    public static final String BACKSLASH_ESCAPE = "\u00A7";

}
