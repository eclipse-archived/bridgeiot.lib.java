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

package org.eclipse.bridgeiot.lib.offering.parameters;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DateTimeParameter extends Parameter {

    @JsonProperty("maxDateTime")
    DateTime maximum;
    @JsonProperty("minDateTime")
    DateTime minimum;

    public DateTimeParameter() {
    }

    public DateTime getMaximum() {
        return maximum;
    }

    public void setMaximum(DateTime maximum) {
        this.maximum = maximum;
    }

    public DateTime getMinimum() {
        return minimum;
    }

    public void setMinimum(DateTime minimum) {
        this.minimum = minimum;
    }

}
