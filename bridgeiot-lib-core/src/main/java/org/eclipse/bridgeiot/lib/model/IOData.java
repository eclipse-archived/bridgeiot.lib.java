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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input parameter element
 * 
 *
 */

// IOData is replaced by ComplexParameter. However, it is still needed for Marketplace communication
public class IOData {

    private String name;
    private RDFType rdfType;
    private List<IOData> members;
    private ValueType valueType;
    private ParameterEncodingType parameterType;

    public IOData() {
        this.name = "";
        this.rdfType = new RDFType();
        this.members = new LinkedList<>();
        this.valueType = ValueType.NUMBER;
    }

    public IOData(RDFType rdfType, ValueType valueType) {
        this.name = "";
        this.rdfType = rdfType;
        this.members = new LinkedList<>();
        this.valueType = ValueType.NUMBER;
    }

    public IOData(RDFType rdfType, ValueType valueType, List<IOData> members) {
        this.name = "";
        this.rdfType = rdfType;
        this.members = members;
        this.valueType = ValueType.NUMBER;
    }

    public IOData(String name, RDFType rdfType, ValueType valueType, List<IOData> members) {
        this.name = name;
        this.rdfType = rdfType;
        this.members = members;
        this.valueType = valueType;
    }

    public IOData(String name, RDFType rdfType, ValueType valueType) {
        this.name = name;
        this.rdfType = rdfType;
        this.members = new LinkedList<>();
        this.valueType = valueType;
    }

    public IOData(String name, RDFType rdfType) {
        this.name = name;
        this.rdfType = rdfType;
        this.members = new LinkedList<>();
        this.valueType = ValueType.NUMBER;
    }

    public IOData(String name, RDFType rdfType, ValueType valueType, ParameterEncodingType encodingType) {
        this(name, rdfType, valueType);
        this.parameterType = encodingType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RDFType getRdfType() {
        return rdfType;
    }

    @JsonProperty("rdfAnnotation")
    public void setRdfType(RDFType rdfType) {
        this.rdfType = rdfType;
    }

    public List<IOData> getMembers() {
        return members;
    }

    public void setMembers(List<IOData> members) {
        this.members = members;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public ParameterEncodingType getParameterType() {
        return parameterType;
    }

    public void setParameterType(ParameterEncodingType encodingType) {
        this.parameterType = encodingType;
    }

    @Override
    public String toString() {
        return "IOData [name=" + name + ", rdfType=" + rdfType + ", members=" + members + ", valueType=" + valueType
                + ", parameterType=" + parameterType + "]";
    }

    public static ComplexObjectMembers createMembers() {

        return new ComplexObjectMembers();
    }

}
