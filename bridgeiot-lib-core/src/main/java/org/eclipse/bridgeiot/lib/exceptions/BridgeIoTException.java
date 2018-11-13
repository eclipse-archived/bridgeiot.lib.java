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

public class BridgeIoTException extends RuntimeException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message. If you are wrapping another exception, consider using
     * {@link #BridgeIoTException(String, Throwable)}.
     *
     * @param message
     *            error message describing the cause of this exception.
     */
    public BridgeIoTException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and the cause.
     *
     * @param message
     *            error message describing what happened.
     * @param cause
     *            root exception that caused this exception.
     */
    public BridgeIoTException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with the cause. Consider using {@link #BridgeIoTException(String, Throwable)}.
     *
     * @param cause
     *            root exception that caused this exception.
     */
    public BridgeIoTException(Throwable cause) {
        super(cause);
    }
}
