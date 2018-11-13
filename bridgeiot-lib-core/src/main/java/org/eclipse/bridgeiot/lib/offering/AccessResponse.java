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
package org.eclipse.bridgeiot.lib.offering;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ResponseStatus;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMappingElement;
import org.eclipse.bridgeiot.lib.offering.mapping.ResponseMappingType;
import org.eclipse.bridgeiot.lib.offering.parameters.ArrayParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectMember;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.RdfReferenceParameter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Response of an Bridge.IoT Offering
 */
public class AccessResponse {

    private static final Logger logger = LoggerFactory.getLogger(AccessResponse.class);

    private static ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JodaModule());
    private static XmlMapper xmlMapper = new XmlMapper();

    private JsonNode node;
    private String message; // Replace message by node, only one instance of content!
    private OfferingDescription offeringDescription;
    private ResponseStatus status;

    public AccessResponse(String message, OfferingDescription offeringDescription) {
        this(message, offeringDescription, MimeType.APPLICATION_JSON);
    }

    public AccessResponse(String message, OfferingDescription offeringDescription, MimeType contentType) {
        this.offeringDescription = offeringDescription;
        this.message = message;
        this.node = TypeInitializer.retrieveJsonNode(contentType, message);
    }

    /**
     * Internal initializer used to create an instance of AccessResponse based on the mime-type.
     */
    enum TypeInitializer {
        JSON_TYPE(MimeType.APPLICATION_JSON, "Cannot parse response message as JSON") {
            public JsonNode retrieveJsonNode(String message) throws IOException {
                return jsonMapper.reader().readTree(message);
            }
        },
        XML_TYPE(MimeType.APPLICATION_XML, "Cannot parse XML response on JSON intermediate step") {
            public JsonNode retrieveJsonNode(String message) throws IOException {
                return xmlMapper.readTree(Helper.removeNameSpacesInXml(message).getBytes());
            }
        };

        private String errorMessage;
        private MimeType type;

        private TypeInitializer(MimeType type, String errorMessage) {
            this.errorMessage = errorMessage;
            this.type = type;
        }

        abstract JsonNode retrieveJsonNode(String message) throws IOException;

        static TypeInitializer fromMimeType(MimeType type) {
            for (TypeInitializer iType : TypeInitializer.values()) {
                if (iType.type.equals(type)) {
                    return iType;
                }
            }
            throw new BridgeIoTException("Unsupported content type: " + type.toString());
        }

        static JsonNode retrieveJsonNode(MimeType type, String message) {
            TypeInitializer iType = fromMimeType(type);
            try {
                return iType.retrieveJsonNode(message);
            } catch (IOException e) {
                logger.info(iType.errorMessage);
                throw new BridgeIoTException(iType.errorMessage, e);
            }
        }
    }

    @Override
    public String toString() {
        return message;
    }

    /**
     * Returns response as a data structure for traversing
     * 
     * @return
     */
    public JsonNode asJsonNode() {
        return node;
    }

    /**
     * Returns response as a Json String
     * 
     */
    public String getBody() {
        return message;
    }

    /**
     * Returns response status
     * 
     * @return ResponseStatus
     */
    public ResponseStatus getStatus() {
        return status;
    }

    /**
     * Maps the offering response to pojo class
     * 
     * @param pojoClass
     * @return
     */
    public <T> List<T> map(Class<T> pojoClass) {
        return map(pojoClass, null);
    }

    /**
     * Maps the offering response according to an output mapping
     * 
     * @param pojoClass
     * @param outputMapping
     * @return
     */
    public <T> List<T> map(Class<T> pojoClass, OutputMapping outputMapping) {
        // Merge mappings
        List<OutputMappingElement> mergedOutputMappingList = new ArrayList<>(
                retrieveAnnotationBasedOutputMappings(pojoClass).getList());
        if (outputMapping != null) {
            mergedOutputMappingList.addAll(outputMapping.getList());
        }

        if (mergedOutputMappingList.isEmpty()) {
            return performOneToOneOutputMapping(pojoClass);
        } else if (mergedOutputMappingList.get(0) instanceof OutputMappingElement.Type) {
            return performTypeBasedOutputMapping(pojoClass, mergedOutputMappingList);
        } else { // if output mapping is provided
            return performNameBasedOutputMapping(pojoClass, outputMapping);
        }
    }

    /**
     * Retrieves mapping instructions from annotations of the given pojoClass
     * 
     * @param pojoClass
     *            the type the annotated mapping instructions are taken from
     * @return output-mapping instructions retrieved from the annotations
     */
    static OutputMapping retrieveAnnotationBasedOutputMappings(Class<?> pojoClass) {
        OutputMapping outputMappingPerAnnotation = OutputMapping.create();

        // Translate pojo annotatations to mappings
        Field[] fields = pojoClass.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.isAnnotationPresent(ResponseMappingType.class)) {
                ResponseMappingType annotation = field.getAnnotation(ResponseMappingType.class);
                outputMappingPerAnnotation.addTypeMapping(annotation.value(), field.getName());
            }
        }

        return outputMappingPerAnnotation;
    }

    /**
     * Performs a type-based output-mapping
     * 
     * @param pojoClass
     *            the type used for the results
     * @param mergedOutputMappingList
     *            list of type-based mapping instructions
     * @return list of mapped pojos
     */
    <T> List<T> performTypeBasedOutputMapping(Class<T> pojoClass, List<OutputMappingElement> mergedOutputMappingList) {
        logger.debug("Output Pojo mapping: {}", mergedOutputMappingList);

        if (!node.isArray()) {
            throw new BridgeIoTException("Non-array results not yet supported");
        }

        List<T> list = new ArrayList<>();
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();

        // Find outputMapping in output parameters of offering description
        for (OutputMappingElement mapping : mergedOutputMappingList) {

            if (mapping instanceof OutputMappingElement.Type) {

                if (offeringDescription.getOutputs() instanceof ArrayParameter) {
                    throw new BridgeIoTException("Top Level Array Parameters not yet supported");
                }
                if (offeringDescription.getOutputs() instanceof RdfReferenceParameter) {
                    throw new BridgeIoTException("Top Level RDF Reference Parameters not yet supported");
                }

                ObjectParameter objectInputData = (ObjectParameter) offeringDescription.getOutputs();

                for (ObjectMember ioData : objectInputData.getMembers()) {

                    // TODO URIs RdfType must be normalized
                    String normalizedUri1 = Helper.normalizeRdfUri(ioData.getRdfUri());
                    String normalizedUri2 = Helper.normalizeRdfUri(((OutputMappingElement.Type) mapping).getType());

                    if (normalizedUri1.equals(normalizedUri2)) {
                        mappingMap.put(ioData.getName(), new AccessResponse.MappingIoDataTuple(mapping, ioData));
                        break;
                    }
                }
            } else {
                throw new BridgeIoTException("Sorry! Name mapping cannot yet be combined with type mapping");
            }
        }

        if (node.isArray()) {
            Iterator<JsonNode> elementsItr = node.elements();
            while (elementsItr.hasNext()) {
                JsonNode subNode = elementsItr.next();
                list.add(deepMapping(pojoClass, subNode, mappingMap, 20));
            }
        }

        return list;
    }

    /**
     * Performs a simple one-to-one mapping without mapping instructions
     * 
     * @param pojoClass
     *            the type used for the results
     * @return list of mapped pojos
     */
    <T> List<T> performOneToOneOutputMapping(Class<T> pojoClass) {
        if (!node.isArray()) {
            throw new BridgeIoTException("Non-array results not yet supported");
        }

        CollectionType collectionType = jsonMapper.getTypeFactory().constructCollectionType(List.class, pojoClass);
        try {
            return jsonMapper.readerFor(collectionType).readValue(this.message);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Performs a name-based output-mapping
     * 
     * @param pojoClass
     *            the type used for the results
     * @param outputMapping
     *            name-based mapping instructions
     * @return list of mapped pojos
     */
    <T> List<T> performNameBasedOutputMapping(Class<T> pojoClass, OutputMapping outputMapping) {
        logger.debug("Output Pojo mapping: {}", outputMapping);
        try {
            return Helper.mapJson(jsonMapper, pojoClass, node, outputMapping);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Something went wrong with Object Mapping");
        }
    }

    /**
     * Performs a recursive mapping for the json-node using the given mapping.
     * 
     * @param pojoClass
     *            the type of the returned value
     * @param node
     *            input node to be re-mapped
     * @param mappingMap
     *            mapping instructions
     * @param depth
     * @return mapped object
     */
    <T> T deepMapping(Class<T> pojoClass, JsonNode node, Map<String, MappingIoDataTuple> mappingMap, int depth) {
        T element = null;
        try {
            element = pojoClass.getConstructor().newInstance();

            Iterator<String> fieldNameItr = node.fieldNames();
            while (fieldNameItr.hasNext()) {
                String fieldNameInResponse = fieldNameItr.next();
                Object value = null;
                JsonNode jsonFieldNode = node.get(fieldNameInResponse);

                if (jsonFieldNode.isArray()) {
                    throw new BridgeIoTException("Arrays not yet supported for mapping");
                }

                Field pojoField = null;

                if (mappingMap != null && !mappingMap.isEmpty()) {
                    if (mappingMap.get(fieldNameInResponse) == null) {
                        continue;
                    }
                    try {

                        pojoField = pojoClass
                                .getField(mappingMap.get(fieldNameInResponse).getMapping().getMappedFieldName());
                    } catch (NoSuchFieldException e) {
                        logger.warn("No mapping rule present. Cannot map to the field: " + fieldNameInResponse);
                        continue;
                    }
                } else {
                    try {
                        pojoField = pojoClass.getField(fieldNameInResponse);
                    } catch (NoSuchFieldException e) {
                        logger.warn("No mapping rule present. Cannot map to the field: " + fieldNameInResponse);
                        continue;
                    }
                }

                if (!jsonFieldNode.isNull()) {
                    if (jsonFieldNode.isContainerNode()) {
                        value = (depth > 0) ? deepMapping(pojoField.getType(), jsonFieldNode, null, depth - 1) : null;
                    } else {
                        value = mapSimpleType(jsonFieldNode, pojoField.getType());
                    }
                }
                pojoField.set(element, value);
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }

        return element;
    }

    /**
     * Converts the fieldNode to the given class if possible.
     * 
     * @param fieldNode
     *            to be converted
     * @param clazz
     *            conversion type
     * @return converted value
     */
    static Object mapSimpleType(JsonNode fieldNode, Class<?> clazz) {
        Object mappedResult = null;

        // Set valueType by JsonNodeType as workaround until exchange supports valueTyps
        if (fieldNode.isBoolean() || fieldNode.isNumber() || fieldNode.isTextual() || fieldNode.isNull()
                || isSupportedDateClass(fieldNode, clazz)) {
            try {
                mappedResult = jsonMapper.convertValue(fieldNode, clazz);
            } catch (IllegalArgumentException e) {
                throw new BridgeIoTException("Invalid value type", e);
            }
        } else {
            throw new BridgeIoTException("Unspecified value type");
        }

        return mappedResult;
    }

    /**
     * Checks if the fieldNode is an object and if the given class is a supported Date-Class
     * 
     * @param fieldNode
     *            to be investigated
     * @param clazz
     *            to be checked if it is a supported Date-Class
     * @return <code>true</code> if the fieldNode is an object and the given class is a supported Date-Class - otherwise
     *         <code>false</code>
     */
    static boolean isSupportedDateClass(JsonNode fieldNode, Class<?> clazz) {
        return fieldNode.isObject() && (DateTime.class.equals(clazz) || Date.class.equals(clazz));
    }

    /**
     * Get access respons in pretty print
     * 
     * @return
     */
    public String getPrettyPrint() {
        return Helper.jsonPrettyPrint(this.getBody());
    }

    /**
     * Remap your access response according to your needs. See javadoc of Helper.remapJson(..)
     *
     * 
     * 
     * @param outputMapping
     * @return
     * @throws IOException
     */
    public AccessResponse remap(OutputMapping outputMapping) throws IOException {
        String remapped = Helper.remapJson(node, outputMapping);
        return new AccessResponse(remapped, this.offeringDescription);
    }

    public class MappingIoDataTuple {

        OutputMappingElement mapping;
        ObjectMember member;

        public MappingIoDataTuple(OutputMappingElement mapping, ObjectMember member) {
            this.mapping = mapping;
            this.member = member;
        }

        public OutputMappingElement getMapping() {
            return mapping;
        }

        public ObjectMember getMember() {
            return member;

        }

    }
}
