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

import com.fasterxml.jackson.annotation.JsonProperty;

public class NumberParameter extends Parameter {

    @JsonProperty("minNumber")
    Double minimum = null;
    @JsonProperty("maxNumber")
    Double maximum = null;

    public NumberParameter() {
    }

    public static NumberParameter create(Double minimum, Double maximum) {
        return new NumberParameter(minimum, maximum);
    }

    public NumberParameter(Double minimum, Double maximum) {
        this();
        this.minimum = minimum < maximum ? minimum : maximum;
        this.maximum = minimum < maximum ? maximum : minimum;
    }

    public Double getMaximum() {
        return maximum;
    }

    public void setMaximum(Double maximum) {
        this.maximum = maximum;
    }

    public Double getMinimum() {
        return minimum;
    }

    public void setMinimum(Double minimum) {
        this.minimum = minimum;
    }

}
