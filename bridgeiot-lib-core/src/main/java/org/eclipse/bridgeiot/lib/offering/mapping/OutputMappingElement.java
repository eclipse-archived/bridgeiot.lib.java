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
package org.eclipse.bridgeiot.lib.offering.mapping;

public abstract class OutputMappingElement {

    public static class Array extends OutputMappingElement.Name {

        private OutputMapping members;

        public Array(String sourceNamePath, String destinationNamePath, OutputMapping members) {
            super(sourceNamePath, destinationNamePath);
            this.members = members;
        }

        public OutputMapping getMembers() {
            return members;
        }

    }

    public static class Name extends OutputMappingElement {

        private String sourceNamePath;

        public Name(String sourceNamePath, String destinationNamePath) {
            super(destinationNamePath);
            this.sourceNamePath = sourceNamePath;
        }

        public String getSourceNamePath() {
            return sourceNamePath;
        }

        @Override
        public String toString() {
            return "Name [sourceNamePath=" + sourceNamePath + ", mappedFieldName=" + mappedFieldName + "]";
        }

    }

    public static class Type extends OutputMappingElement {

        private String elementType;

        public Type(String type, String fieldName) {
            super(fieldName);
            this.elementType = type;
        }

        public String getType() {
            return elementType;
        }

        @Override
        public String toString() {
            return "" + elementType + " -> " + mappedFieldName;
        }

    }

    String mappedFieldName;

    public OutputMappingElement(String fieldName) {
        super();
        this.mappedFieldName = fieldName;
    }

    public String getMappedFieldName() {
        return mappedFieldName;
    }

}
