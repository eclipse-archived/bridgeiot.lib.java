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

public class HttpErrorException extends BridgeIoTCheckedException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    int status;
    String response;

    private static final String ERROR_MSG = "Got a %d HTTP code.%s";

    private static final String ERROR_MSG_OPT = " Response body is \n%s";

    /**
     * Constructor with status and response
     * 
     * @param status
     * @param response
     */
    public HttpErrorException(int status, String response) {
        super(String.format(ERROR_MSG, status, response != null ? String.format(ERROR_MSG_OPT, response) : ""));
        this.response = response;
        this.status = status;
    }

    /**
     * Constructor with status, response and cause
     * 
     * @param status
     * @param response
     * @param cause
     */
    public HttpErrorException(int status, String response, Throwable cause) {
        super(String.format(ERROR_MSG, status, response != null ? String.format(ERROR_MSG_OPT, response) : ""), cause);
        this.response = response;
        this.status = status;
    }

}
