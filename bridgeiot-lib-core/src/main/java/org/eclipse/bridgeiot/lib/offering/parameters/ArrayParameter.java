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

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;

public class ArrayParameter extends ComplexParameter {

    Parameter items = new TextParameter(); // Default

    public ArrayParameter() {
    }

    public ArrayParameter(Parameter element) {
        this();
        this.items = element;
    }

    public static ArrayParameter withValueType(ValueType valueType) {
        return new ArrayParameter(Parameter.toParameter(valueType));
    }

    public static ArrayParameter withObject(ObjectParameter parameter) {
        return new ArrayParameter(parameter);
    }

    public static ArrayParameter withArray(ArrayParameter parameter) {
        return new ArrayParameter(parameter);
    }

    public Parameter getElement() {
        return items;
    }

    public void setElement(Parameter element) {
        this.items = element;
    }

}
