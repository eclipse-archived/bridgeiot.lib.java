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

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;

public class ObjectMember {

    String name;
    String rdfUri;
    Parameter value;
    ParameterEncodingType encodingType;
    private boolean required;

    protected ObjectMember() {

    }

    public ObjectMember(String name, String rdfUri, ValueType valueType, ParameterEncodingType encodingType,
            boolean required) {
        this(name, rdfUri, Parameter.toParameter(valueType), encodingType, required);
    }

    public ObjectMember(String name, String rdfAnnotation, Parameter parameter, ParameterEncodingType encodingType,
            boolean required) {
        super();
        this.name = name;
        this.rdfUri = rdfAnnotation;
        this.value = parameter;
        this.encodingType = encodingType;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getRdfUri() {
        return rdfUri;
    }

    public Parameter getValue() {
        return value;
    }

    public ParameterEncodingType getEncodingType() {
        return encodingType;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "ObjectMember [name=" + name + ", rdfUri=" + rdfUri + ", value=" + value + ", required=" + required
                + ", encodingType=" + encodingType + "]";
    }

}
