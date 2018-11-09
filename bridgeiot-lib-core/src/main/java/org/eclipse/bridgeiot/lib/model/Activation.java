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
 * Offering activation
 * 
 *
 */
public class Activation {

    Boolean status = false;
    Long expirationTime = 0L;

    public Activation() {
    }

    public Activation(Boolean status) {
        this.status = status;
    }

    public Activation(Boolean status, Long expirationTime) {
        this.status = status;
        this.expirationTime = expirationTime;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    @Override
    public String toString() {
        return "activation[status=" + this.status.toString() + ", expirationTime=" + this.expirationTime + "]";
    }

}
