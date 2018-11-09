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

import java.util.LinkedList;
import java.util.List;

public class OutputMapping {

    LinkedList<OutputMappingElement> list = new LinkedList<>();

    private OutputMapping() {

    }

    public static OutputMapping create() {
        return new OutputMapping();
    }

    public OutputMapping addTypeMapping(String type, String fieldName) {
        list.add(new OutputMappingElement.Type(type, fieldName));
        return this;
    }

    public OutputMapping addNameMapping(String sourceNamePath, String destinationNamePath) {
        list.add(new OutputMappingElement.Name(sourceNamePath, destinationNamePath));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("OutputMapping:\n");

        for (OutputMappingElement outputMappingTuple : list) {
            builder.append(outputMappingTuple.toString());
            builder.append("\n");
        }

        return builder.toString();
    }

    public List<OutputMappingElement> getList() {
        return list;
    }

    public OutputMapping addArrayMapping(String sourceNamePath, String destinationNamePath, OutputMapping members) {
        list.add(new OutputMappingElement.Array(sourceNamePath, destinationNamePath, members));
        return this;
    }

}
