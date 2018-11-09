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

public abstract class BridgeIoTCheckedException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     * @param cause
     */
    public BridgeIoTCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public BridgeIoTCheckedException(String message) {
        super(message);
    }

    protected String getErrorMsg() {
        return super.getMessage();
    }

}
