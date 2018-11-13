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

/**
 * Container Element of AccessParameters
 * 
 *
 */
public abstract class AccessParametersTuple {

    /**
     * Name/value based parameter tuple
     * 
     *
     */
    public static class Name extends AccessParametersTuple {
        private String name;

        public Name(String name, Object value) {
            super(value);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Name [name=" + name + ", value=" + value + "]";
        }

    }

    /**
     * Type/value base parameter tuple
     * 
     *
     */
    public static class Type extends AccessParametersTuple {
        private String rdfAnnotation;

        public Type(String rdfAnnotation, Object value) {
            super(value);
            this.rdfAnnotation = rdfAnnotation;
        }

        public String getRdfAnnotation() {
            return rdfAnnotation;
        }

        @Override
        public String toString() {
            return "Type [rdfAnnotation=" + rdfAnnotation + ", value=" + value + "]";
        }

    }

    Object value;

    public AccessParametersTuple(Object value) {
        super();
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

}
