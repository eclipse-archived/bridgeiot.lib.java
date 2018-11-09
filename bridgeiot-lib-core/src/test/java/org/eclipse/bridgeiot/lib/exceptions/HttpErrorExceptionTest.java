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
package org.eclipse.bridgeiot.lib.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HttpErrorExceptionTest {

    private final static String ANY_JSON = "{\"data\":{\"addOfferingQuery\":{\"id\":\"superman\"}}}";

    @Test
    public void checkErrorMessageWithStatusAndResponse() {
        HttpErrorException exceptionUnderTest = new HttpErrorException(666, ANY_JSON);

        assertThat(exceptionUnderTest.getErrorMsg()).contains("666").contains("Response body is").contains("\n")
                .contains(ANY_JSON);
    }

    @Test
    public void checkErrorMessageWithStatusNoResponse() {
        HttpErrorException exceptionUnderTest = new HttpErrorException(666, null);

        assertThat(exceptionUnderTest.getErrorMsg()).contains("666").doesNotContain("Response body is")
                .doesNotContain("\n");
    }

    @Test
    public void checkErrorMessageWithStatusAndResponseAndThrowable() {
        HttpErrorException exceptionUnderTest = new HttpErrorException(666, ANY_JSON, new Throwable());

        assertThat(exceptionUnderTest.getMessage()).contains("666").contains("Response body is").contains("\n")
                .contains(ANY_JSON);
    }

}
