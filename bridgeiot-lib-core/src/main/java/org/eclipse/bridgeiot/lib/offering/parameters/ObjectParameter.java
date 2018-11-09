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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.IOData;

public class ObjectParameter extends ComplexParameter {

    private static final ParameterEncodingType DEFAULT_PARAMETER_ENCODING_TYPE = ParameterEncodingType.QUERY;

    List<ObjectMember> members = new LinkedList<>();

    protected ObjectParameter() {
    }

    public static ObjectParameter from(List<IOData> ioDataList) {
        ObjectParameter objectParameter = ObjectParameter.create();

        for (IOData ioData : ioDataList) {

            if (ioData.getMembers() != null && !ioData.getMembers().isEmpty()) {
                throw new BridgeIoTException(
                        "Creation of ObjectParameter from complex IOData Elements should be actually not needed");
            }

            objectParameter.addMember(ioData.getName(), ioData.getRdfType().getUri(), ioData.getValueType());

        }
        return objectParameter;
    }

    public static ObjectParameter create() {
        return new ObjectParameter();
    }

    public List<ObjectMember> getMembers() {
        return members;
    }

    public void setMembers(List<ObjectMember> members) {
        this.members = members;
    }

    /**
     * Add an optional simple parameter
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, ValueType valueType) {
        return this.addMember(name, rdfAnnotation, valueType, DEFAULT_PARAMETER_ENCODING_TYPE, false);
    }

    /**
     * Add a simple parameter in the request, which is either required or not
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, ValueType valueType, boolean isRequired) {
        return this.addMember(name, rdfAnnotation, valueType, DEFAULT_PARAMETER_ENCODING_TYPE, isRequired);
    }

    /**
     * Add an optional simple parameter with a specific encoding in IM 3
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, ValueType valueType,
            ParameterEncodingType encodingType) {

        return addMember(name, rdfAnnotation, valueType, encodingType, false);
    }

    /**
     * Add a simple parameter in the request with a specific encoding in IM 3, and which is either required or not
     * 
     * @param name
     * @param rdfAnnotation
     * @param valueType
     * @param encodingType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, ValueType valueType,
            ParameterEncodingType encodingType, boolean isRequired) {
        ObjectMember objectMember = new ObjectMember(name, rdfAnnotation, valueType, encodingType, isRequired);

        return this.addMember(objectMember);
    }

    /**
     * Add an optional complex parameter
     * 
     * @param name
     * @param rdfAnnotation
     * @param parameter
     * @param encodingType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, Parameter parameter) {
        return addMember(name, rdfAnnotation, parameter, DEFAULT_PARAMETER_ENCODING_TYPE, false);
    }

    /**
     * Add a complex parameter in the request, which is either required or not
     * 
     * @param name
     * @param rdfAnnotation
     * @param parameter
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, Parameter parameter, boolean isRequired) {
        return addMember(name, rdfAnnotation, parameter, DEFAULT_PARAMETER_ENCODING_TYPE, isRequired);
    }

    /**
     * Add an optional complex parameter with a specific encoding in IM 3
     * 
     * @param name
     * @param rdfAnnotation
     * @param parameter
     * @param encodingType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, Parameter parameter,
            ParameterEncodingType encodingType) {
        return addMember(name, rdfAnnotation, parameter, encodingType, false);
    }

    /**
     * Add a complex parameter in the request with a specific encoding in IM 3, which is either required or not
     * 
     * @param name
     * @param rdfAnnotation
     * @param parameter
     * @param encodingType
     * @return
     */
    public ObjectParameter addMember(String name, String rdfAnnotation, Parameter parameter,
            ParameterEncodingType encodingType, boolean isRequired) {
        ObjectMember objectMember = new ObjectMember(name, rdfAnnotation, parameter, encodingType, isRequired);
        return addMember(objectMember);
    }

    public ObjectParameter addMember(ObjectMember member) {
        members.add(member);
        return this;

    }

    @Override
    public String toString() {
        return "ObjectParameter [members=" + members + "]";
    }

}
