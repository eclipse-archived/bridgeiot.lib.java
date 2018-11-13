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

import org.joda.time.DateTime;

public class TimePeriod implements QueryElement {

    private Long from = 0L;
    private Long to = 0L;

    public TimePeriod() {
    }

    public TimePeriod(Long from, Long to) {
        this.setFrom(from);
        this.setTo(to);
    }

    public TimePeriod(DateTime from, DateTime to) {
        this.setFrom(from.getMillis());
        this.setTo(to.getMillis());
    }

    public static TimePeriod create(Long from, Long to) {
        return new TimePeriod(from, to);
    }

    public static TimePeriod create(DateTime from, DateTime to) {
        return new TimePeriod(from, to);
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getTo() {
        return to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public String toQueryElement() {

        return new StringBuilder().append("temporalExtent: { from: ").append(getFrom()).append(", to: ").append(getTo())
                .append(" }").toString();
    }

}
