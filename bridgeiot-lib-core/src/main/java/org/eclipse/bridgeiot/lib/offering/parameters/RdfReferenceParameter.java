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

import org.eclipse.bridgeiot.lib.model.RDFReference;

public class RdfReferenceParameter extends ComplexParameter {

    RDFReference rdfReference;

    // Default constructor required by Jackson
    public RdfReferenceParameter() {

    }

    public RdfReferenceParameter(RDFReference rdfReference) {
        super();
        this.rdfReference = rdfReference;
    }

    public RdfReferenceParameter(String rdfReference) {
        super();
        this.rdfReference = new RDFReference(rdfReference);
    }

    public RDFReference getRdfReference() {
        return rdfReference;
    }

    public void setRdfReference(RDFReference rdfReference) {
        this.rdfReference = rdfReference;
    }

}
