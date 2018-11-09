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

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Description element in Offering Description
 * 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class Description implements Cloneable {

    private String name;
    private String domain;
    private RDFType rdfType;

    public Description(String name, String domain, RDFType rdfType) {
        this.name = name;
        this.domain = domain;
        this.rdfType = rdfType;
    }

    public Description(String name, RDFType rdfType) {
        this.name = name;
        this.rdfType = rdfType;
    }

    public Description(String name) {
        this.name = name;
    }

    public Description() {
        super();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setRdfType(RDFType rdfType) {
        this.rdfType = rdfType;
    }

    public RDFType getRdfType() {
        return this.rdfType;
    }

    @Override
    public String toString() {
        String response = "description[name=" + this.name;
        if (this.rdfType != null) {
            response += ", " + this.rdfType.toString();
        }
        response += "]";
        return response;
    }

    public abstract Description clone();
}
