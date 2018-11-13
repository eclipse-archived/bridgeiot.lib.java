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
 * Information element
 * 
 *
 */
public class Information extends Description {

    public Information(String name, String domain, RDFType rdfType) {
        super(name, domain, rdfType);
    }

    public Information(String name, RDFType rdfType) {
        super(name, rdfType);
    }

    public Information(String name, String rdfTypeUri) {
        super(name, new RDFType(rdfTypeUri));
    }

    public Information(String name, String rdfTypeName, String rdfTypeUri) {
        super(name, new RDFType(rdfTypeName, rdfTypeUri));
    }

    public Information(String name) {
        super(name);
    }

    public Information() {
        super();
    }

    @Override
    public Description clone() {
        return new Information(getName(), getDomain(), getRdfType());
    }

}
