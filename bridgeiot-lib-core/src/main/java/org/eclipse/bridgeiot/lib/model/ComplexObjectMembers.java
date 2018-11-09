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

import java.util.Collection;
import java.util.LinkedList;

@Deprecated
public class ComplexObjectMembers {

    private String name;
    private RDFType rdfType;
    private String valueType;

    Collection<ComplexObjectMembers> members = new LinkedList<>();

    public ComplexObjectMembers(String name, RDFType rdfType, String valueType) {
        this.name = name;
        this.rdfType = rdfType;
        this.valueType = valueType;
    }

    public ComplexObjectMembers(String name, RDFType rdfType, ComplexObjectMembers members) {
        this.name = name;
        this.rdfType = rdfType;
        this.members = members.getMembers();
    }

    public ComplexObjectMembers() {
    }

    // Helper for parameter simple
    private ComplexObjectMembers addData(String name, RDFType rdfType, String valueType) {
        this.members.add(new ComplexObjectMembers(name, rdfType, valueType));
        return this;
    }

    // Helper for parameter complex
    private ComplexObjectMembers addData(String name, RDFType rdfType, ComplexObjectMembers members) {
        this.members.add(new ComplexObjectMembers(name, rdfType, members));
        return this;
    }

    // Add simple input parameter
    public ComplexObjectMembers addInputData(String name, RDFType rdfType, String valueType) {
        return addData(name, rdfType, valueType);
    }

    // Add complex input parameter
    public ComplexObjectMembers addInputData(String name, RDFType rdfType, ComplexObjectMembers members) {
        return addData(name, rdfType, members);
    }

    // Add simple output parameter
    public ComplexObjectMembers addOutputData(String name, RDFType rdfType, String valueType) {
        return addData(name, rdfType, valueType);
    }

    // Add complex output parameter
    public ComplexObjectMembers addOutputData(String name, RDFType rdfType, ComplexObjectMembers members) {
        return addData(name, rdfType, members);
    }

    protected Collection<ComplexObjectMembers> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "ComplexObjectMembers [name=" + name + ", rdfType=" + rdfType + ", valueType=" + valueType + ", members="
                + members + "]";
    }

}
