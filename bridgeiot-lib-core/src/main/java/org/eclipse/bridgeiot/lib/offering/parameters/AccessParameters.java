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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.DataSchemaType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ParameterEncodingType;
import org.eclipse.bridgeiot.lib.model.RDFType;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParametersTuple.Name;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for parameters for offering data access. AccessParameters supports name/value pair as well as type/value
 * pairs
 * 
 *
 */
public class AccessParameters {

    private static final Logger logger = LoggerFactory.getLogger(AccessParameters.class);

    // Preferred list instead of map as Access Parameters has a weird key/value concept. It support as key as well as
    // type. AccessTuple separates both parametertypes
    List<AccessParametersTuple> members;

    public AccessParameters() {
        this.members = new LinkedList<>();
    }

    /**
     * Creates an empty parameter container
     * 
     * @return
     */
    public static AccessParameters create() {
        return new AccessParameters();
    }

    /**
     * Adds a name/value pair
     * 
     * @param value
     * @return
     */
    public AccessParameters addNameValue(String parameterName, Object value) {
        members.add(new AccessParametersTuple.Name(parameterName, value));
        return this;
    }

    /**
     * Adds a (RDF-)type/value pair
     * 
     * @param parameterType
     * @param value
     * @return
     */
    @Deprecated
    public AccessParameters addRdfTypeValue(RDFType parameterType, Object value) {
        members.add(new AccessParametersTuple.Type(parameterType.getUri(), value));
        return this;
    }

    /**
     * Adds a (RDF-)type/value pair as URI
     * 
     * @param parameterType
     * @param value
     * @return
     */
    public AccessParameters addRdfTypeValue(String rdfUri, Object value) {
        members.add(new AccessParametersTuple.Type(rdfUri, value));
        return this;
    }

    /**
     * Returns a map with name/value pairs and takes care that it is consistent according to a Offering Description
     * parameter list
     * 
     * @param inputData
     *            Input parameter list of Offering Description
     * @return
     * @throws IllegalAccessParameterException
     */

    // TODO: Optimize visibility
    public Map<String, Object> toNameMap(Parameter inputData, int depth) throws IllegalAccessParameterException {
        if (depth == 0) {
            throw new BridgeIoTException("Recursion depth to deep");
        }

        HashMap<String, Object> map = new HashMap<>();

        if (inputData == null) {
            return map;
        }

        if (inputData instanceof ObjectParameter) {
            ObjectParameter inputDataObject = (ObjectParameter) inputData;
            if (LibConfiguration.ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS
                    && inputData instanceof ObjectParameter
                    && ((ObjectParameter) inputData).getMembers().size() != members.size()) {
                logger.error("Access parameter NUMBER is not equal to input parameter NUMBER");
                throw new IllegalAccessParameterException();
            }

            LinkedList<ObjectMember> inputDataMembersClone = new LinkedList<>(inputDataObject.getMembers());
            LinkedList<AccessParametersTuple> accessParametersClone = new LinkedList<>(this.members);

            for (AccessParametersTuple tuple : accessParametersClone) {

                if (tuple instanceof AccessParametersTuple.Name) {
                    AccessParametersTuple.Name nameTuple = (Name) tuple;

                    boolean found = false;
                    ObjectMember ioDataElement = null;
                    Iterator<ObjectMember> itr = inputDataMembersClone.iterator();
                    while (itr.hasNext()) {
                        ioDataElement = itr.next();

                        if (ioDataElement.getName().equals(nameTuple.getName())) {
                            found = true;
                            itr.remove();
                            break;
                        }
                    }
                    if (!found) {
                        String s = "Access parameter is not defined in offering description";
                        if (LibConfiguration.ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS) {
                            logger.warn(s);
                            throw new IllegalAccessParameterException();
                        } else {
                            logger.warn(s);
                        }
                        continue;
                    }

                    if (nameTuple.getValue() instanceof AccessParameters) {

                        map.put(nameTuple.getName(), ((AccessParameters) nameTuple.getValue())
                                .toNameMap(ioDataElement.getValue(), depth - 1));
                    } else {
                        map.put(nameTuple.getName(), nameTuple.getValue());
                    }

                } else if (tuple instanceof AccessParametersTuple.Type) {
                    AccessParametersTuple.Type typeTuple = (AccessParametersTuple.Type) tuple;
                    boolean found = false;
                    for (ObjectMember ioDataElement : inputDataMembersClone) {
                        if (Helper.normalizeRdfUri(ioDataElement.getRdfUri())
                                .equals(Helper.normalizeRdfUri(typeTuple.getRdfAnnotation()))) {
                            found = true;
                            inputDataMembersClone.remove(ioDataElement);
                            map.put(ioDataElement.getName(), typeTuple.getValue());
                            if (typeTuple.getValue() instanceof AccessParameters) {
                                map.put(ioDataElement.getName(), ((AccessParameters) typeTuple.getValue())
                                        .toNameMap(ioDataElement.getValue(), depth - 1));
                            } else {
                                if (!isValueTypeOkay(ioDataElement.getValue().getType(), typeTuple.getValue())) {
                                    logger.debug(
                                            "Value type is not matching with declare value type in offering Description");
                                    // throw new IllegalAccessParameterException(); // TODO Fix once Value Types are
                                    // provided from Mkpl
                                }
                                map.put(ioDataElement.getName(), typeTuple.getValue());
                            }
                            break;
                        }
                    }
                    if (!found) {
                        logger.error("Cannot map RDF type to parameter name");
                        throw new IllegalAccessParameterException();

                    }
                } else {
                    throw new BridgeIoTException("Unsupported AccessParameterTuple Type");
                }
            }
            if (!inputDataMembersClone.isEmpty()) {
                String s = "Number of access parameters lower than declared input parameters";
                if (LibConfiguration.ACCESS_PARAMETERS_HAVE_TO_MATCH_INPUT_PARAMETERS) {
                    logger.error(s);
                    throw new IllegalAccessParameterException();
                } else {
                    logger.warn(s);
                }
            }
            return map;
        } else {
            throw new BridgeIoTException(
                    "This inputData is not yet supported: " + inputData.getClass().getCanonicalName());
        }
    }

    private boolean isValueTypeOkay(DataSchemaType dataSchemaType, Object value) {

        if (dataSchemaType == null)
            return true;
        if (dataSchemaType.name().equalsIgnoreCase("text") && value instanceof String
                || dataSchemaType.name().equalsIgnoreCase("date")
                        && (value instanceof DateTime || value instanceof Date || value instanceof String)
                || dataSchemaType.name().equalsIgnoreCase("datetime")
                        && (value instanceof DateTime || value instanceof Date || value instanceof String)
                || dataSchemaType.name().equalsIgnoreCase("time")
                        && (value instanceof DateTime || value instanceof Date || value instanceof String)
                || dataSchemaType.name().equalsIgnoreCase("number") && (Number.class.isInstance(value))
                || dataSchemaType.name().equalsIgnoreCase("boolean")
                        && (Number.class.isInstance(value) || Boolean.class.isInstance(value)))
            return true;

        return false;
    }

    /**
     * Select those Access Parameters in name map which are marked with the specified parameterType (URL, query, body,
     * or template) in ioData. Template marked parameters will be later replace a placeholder in the template (by
     * value). The return map is here the placeholder (from iodata) plus the value Object.
     * 
     * @param parametersMap
     * @param inputData
     * @param parameterType
     * @return
     */
    public static Map<String, Object> getParametersForEncoding(Map<String, Object> parametersMap, Parameter inputData,
            ParameterEncodingType parameterType) {

        if (inputData instanceof ArrayParameter) {
            throw new BridgeIoTException("Top Level Array Parameters not yet supported");
        }
        if (inputData instanceof RdfReferenceParameter) {
            throw new BridgeIoTException("Top Level RDF Reference Parameters not yet supported");
        }

        ObjectParameter objectInputData = (ObjectParameter) inputData;
        Map<String, Object> reducedMap = new HashMap<>();
        for (ObjectMember member : objectInputData.getMembers()) {

            if (member.getEncodingType() == parameterType) {
                if (member.getValue() != null && member.getValue() instanceof ComplexParameter
                        && (parameterType == ParameterEncodingType.ROUTE || parameterType == ParameterEncodingType.QUERY
                                || parameterType == ParameterEncodingType.TEMPLATE)) {
                    throw new BridgeIoTException(
                            "Complex type not supported for " + parameterType.toString() + " in integration mode 3");
                }
                if (!parametersMap.containsKey(member.getName())) {
                    logger.info("Parameter \"" + member.getName()
                            + "\" not found in parameter map. Guess it is intentionally");
                }
                reducedMap.put(member.getName(), parametersMap.get(member.getName()));
            }

        }

        return reducedMap;
    }

    public List<AccessParametersTuple> getMembers() {
        return members;
    }

    public void setMembers(List<AccessParametersTuple> members) {
        this.members = members;
    }

}
