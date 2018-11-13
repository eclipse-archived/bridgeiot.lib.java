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
package org.eclipse.bridgeiot.lib.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMappingElement;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

@SuppressWarnings("JavadocReference")
public class Helper {

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    private static final String unmarshallDiscoveryErrorMsg = "Unmarshalling discover response from eXchange failed!";
    private static final String unmarshallQueryErrorMsg = "Marketplace communication problem. Cannot unmarshal response!";

    static ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL)
            .disable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Convenience function for String.format() using US locale
     */
    public static String formatString(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }

    static DecimalFormatSymbols usStyle = new DecimalFormatSymbols(Locale.US);

    public static final DecimalFormat df00 = new DecimalFormat("00", usStyle);
    public static final DecimalFormat df000 = new DecimalFormat("000", usStyle);
    public static final DecimalFormat df0000 = new DecimalFormat("0000", usStyle);
    public static final DecimalFormat df0_0000 = new DecimalFormat("#.####", usStyle);
    public static final int Second = 1000;
    public static final int Minute = 60 * Second;
    public static final int Hour = 60 * Minute;

    private Helper() {
    }

    /**
     * Unmarshall discovery response to retrieve List of Offering elements.
     * 
     * @param jsonString
     * @param tClass
     * @return
     */
    public static <T> List<T> unmarshallDiscoverResponse(String jsonString, Class<T> tClass) {
        try {
            JsonNode rootNode = mapper.reader().readTree(jsonString);
            String rootName = rootNode.fieldNames().next();
            JsonNode matchingOffersNode = rootNode.get(rootName).get("matchingOfferings");
            if (matchingOffersNode == null) {
                throw new BridgeIoTException(unmarshallDiscoveryErrorMsg);
            }
            return mapper.reader().forType(mapper.getTypeFactory().constructCollectionType(ArrayList.class, tClass))
                    .readValue(matchingOffersNode);
        } catch (IOException e) {
            logger.error(unmarshallDiscoveryErrorMsg);
            throw new BridgeIoTException(unmarshallDiscoveryErrorMsg, e);
        }

    }

    /**
     * Return marketplace response data as list of domain objects.
     * 
     * @param queryName
     * @param jsonString
     * @param tClass
     * 
     * @return
     */
    public static <T> List<T> unmarshallArrayFromQueryResponse(String queryName, String jsonString, Class<T> tClass) {
        try {
            JsonNode rootNode = mapper.reader().readTree(jsonString);
            String rootName = rootNode.fieldNames().next();
            JsonNode queryResult = rootNode.get(rootName).get(queryName);
            if (queryResult == null) {
                throw new BridgeIoTException(unmarshallQueryErrorMsg);
            }
            return mapper.reader().forType(mapper.getTypeFactory().constructCollectionType(ArrayList.class, tClass))
                    .readValue(queryResult);

        } catch (IOException e) {
            logger.error(unmarshallQueryErrorMsg);
            throw new BridgeIoTException(unmarshallQueryErrorMsg, e);
        }
    }

    /**
     * Return marketplace response data as domain object.
     * 
     * @param queryName
     * @param jsonString
     * @param tClass
     * 
     * @return
     */
    public static <T extends Object> T unmarshallSingleFromQueryResponse(String queryName, String jsonString,
            Class<T> tClass) {
        try {
            JsonNode rootNode = mapper.reader().readTree(jsonString);
            String rootName = rootNode.fieldNames().next();
            JsonNode queryResult = rootNode.get(rootName).get(queryName);
            if (queryResult == null) {
                throw new BridgeIoTException(unmarshallQueryErrorMsg);
            }
            return mapper.reader().forType(tClass).readValue(queryResult);

        } catch (IOException e) {
            logger.error(unmarshallQueryErrorMsg);
            throw new BridgeIoTException(unmarshallQueryErrorMsg, e);
        }
    }

    /**
     * Pretty print of list of Offering Description
     * 
     * @param l
     * @return
     */
    @Deprecated
    public static void showOfferingDescriptions(List<SubscribableOfferingDescriptionCore> l) {
        if (logger.isInfoEnabled()) {
            logger.info(showOfferingDescriptions(l, false));
        }
    }

    /**
     * Pretty print of list of Offering Description
     * 
     * @param l
     * @return
     */
    public static <T extends OfferingDescription> String showOfferingDescriptions(List<T> l, boolean detailed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered ").append(l.size()).append(l.size() != 1 ? " offerings" : " offering");
        if (!l.isEmpty()) {
            sb.append(" with the following names: ");
            for (int i = 0; i < l.size(); i++) {
                sb.append(i > 0 ? ", " : "").append(l.get(i).getName());
            }
            if (detailed) {
                for (int i = 0; i < l.size(); i++) {
                    sb.append("\nOffering description #").append(i + 1).append("\n").append(l.get(i).toString());
                }
            }
        }

        return sb.toString();
    }

    public static double round(double d, int i) {
        if (i >= 0) {
            double f = Math.pow(10, i);

            return Math.round(d * f) / f;
        } else {
            return Double.NaN;
        }
    }

    public static String formatDate(String format, DateTime date) {
        return date.toString(format);
    }

    public static void printDeltaTime(DateTime start) {
        printDeltaTime("", start);
    }

    public static void printDeltaTime(String prefix, DateTime start) {
        logger.info("{} DELTA TIME = {}ms", prefix, new Duration(start, DateTime.now()).getMillis());
    }

    public static DateTime unixTimeMillisToJoda(Long expirationTime) {
        return new DateTime(expirationTime);
    }

    public static DateTime unixTimeToJoda(Long expirationTime) {
        return unixTimeMillisToJoda(expirationTime * 1000);
    }

    public static InputStream getResource(String resourceName) {
        if (resourceName.charAt(0) != '/') {
            throw new BridgeIoTException("Take care that resourceName starts with a /");
        }
        return Helper.class.getResourceAsStream(resourceName);
    }

    /**
     * Serializes the given map to json-string.
     * 
     * @param map
     *            to be serialized
     * @param prettyPrint
     *            flag to indicated if serialized json-string should be pretty-printed or compact.
     * @return json-string
     * @throws JsonProcessingException
     */
    public static String mapToJson(Map<String, Object> map, boolean prettyPrint) throws JsonProcessingException {
        return objectToJson((Object) map, prettyPrint);
    }

    /**
     * Serializes the given pojo to json-string.
     * 
     * @param pojo
     *            to be serialized
     * @param prettyPrint
     *            flag to indicated if serialized json-string should be pretty-printed or compact.
     * @return json-string
     * @throws JsonProcessingException
     */
    public static String objectToJson(Object pojo, boolean prettyPrint) throws JsonProcessingException {
        ObjectWriter writer;
        if (prettyPrint)
            writer = mapper.writer(SerializationFeature.INDENT_OUTPUT);
        else {
            writer = mapper.writer();
        }
        return writer.writeValueAsString(pojo);
    }

    /**
     * Serialized the given pojo to pretty-printed json-string
     * 
     * @param pojo
     *            to be serialized
     * @return pretty-printed json-string
     */
    public static String getPojoAsJson(Object pojo) {
        try {
            return objectToJson(pojo, true);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Cannot create JSON", e);
        }
    }

    /**
     * Serialized the given pojo to compact json-string
     * 
     * @param pojo
     *            to be serialized
     * @return compact json-string
     */
    public static String getPojoAsJsonCompact(Object pojo) {
        try {
            return objectToJson(pojo, false);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Cannot create JSON", e);
        }
    }

    /**
     * Pretty print a json-string
     * 
     * @param body
     *            compact json-string
     * @return pretty-printed json-sting
     */
    public static String jsonPrettyPrint(String body) {
        ObjectMapper mapper = new ObjectMapper();

        String pretty;
        try {
            Object json = mapper.readValue(body, Object.class);
            pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (IOException e) {
            throw new BridgeIoTException("Error when pretty-printing JSON string");
        }

        return pretty;

    }

    @SuppressWarnings("unchecked")
    public static void constructivePut(Map<String, Object> map, String dottedFieldName, Object value) {
        if (dottedFieldName.contains("[")) {
            throw new BridgeIoTException("Illegal field name " + dottedFieldName);
        }
        String[] hierarchy = dottedFieldName.split("\\.");
        Map<String, Object> subMap = map;
        for (int i = 0; i < hierarchy.length; i++) {
            if (i == hierarchy.length - 1) {
                subMap.put(hierarchy[i], value);
            } else if (!subMap.containsKey(hierarchy[i])) {
                Map<String, Object> nextLevelMap = new HashMap<>();
                subMap.put(hierarchy[i], nextLevelMap);
                subMap = nextLevelMap;
            } else {
                Object nextLevel = subMap.get(hierarchy[i]);
                try {
                    subMap = (Map<String, Object>) nextLevel;
                } catch (ClassCastException e) {
                    throw new BridgeIoTException(
                            String.format("Value assignment for %s is not allowed (no leaf-field)", nextLevel), e);
                }
            }
        }
    }

    public static String listToJson(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
            sb.append(iterator.next() + (iterator.hasNext() ? "," : ""));
        }
        return sb.append("]").toString();
    }

    // @formatter:off
    /**
     * 
     * Remap a JSON body by renaming and filtering keys and flattening arrays
     * 
     * For example:
     * 
     * Consider the following input json structure:
     *
     * {
     *      "Envelope": {
     *          "Body": {
     *              "inquireAllResponse": {
     *                  "code": 0,
     *                  "update": "2017-10-05T10:41:49.510Z",
     *                  "systemRef": "0x678758",
     *                  "dataList": {
     *                      "ds": [{
     *                          "timestamp": "2017-10-05T10:41:18.400Z",
     *                          "value": "23.12",
     *                          "status": "okay"
     *                      }, {
     *                          "timestamp": "2017-10-05T10:41:28.500Z",
     *                          "status": "okay",
     *                          "value": "24.56"
     *                      }, {
     *                          "timestamp": "2017-10-05T10:41:38.600Z",
     *                          "status": "okay",
     *                          "value": "22.22"
     *                      }
     *                      ]
     *                  }
     *              }
     *          }
     *      }
     *  }
     * 
     * 
     * and the outputMapping
     * 
     * OutputMapping
     *      .create()
     *      .addNameMapping("Envelope.Body.inquireAllResponse.code", "errorCode")
     *      .addNameMapping("Envelope.Body.inquireAllResponse.read", "lastUpdate")
     *      .addArrayMapping("Envelope.Body.inquireAllResponse.dataList.ds", "list", OutputMapping
     *          .create()
     *          .addNameMapping("timestamp", "timestamp")
     *          .addNameMapping("value", "value")
     *      );
     * 
     * The remapped result is
     * 
     *  [{
     *      "lastUpdate": "",
     *      "errorCode": "0",
     *      "list": [{
     *          "value": "23.12",
     *          "timestamp": "2017-10-05T10:41:18.400Z"
     *      }, {
     *          "value": "24.56",
     *          "timestamp": "2017-10-05T10:41:28.500Z"
     *      }, {
     *          "value": "22.22",
     *          "timestamp": "2017-10-05T10:41:38.600Z"
     *      }
     *      ]
     *  }
     *  ]
     *
     * @param jsonString
     * @param outputMapping
     * @return
     * @throws IOException
     */
    // @formatter:on
    public static String remapJson(String jsonString, OutputMapping outputMapping) throws IOException {
        JsonNode node = mapper.reader().readTree(jsonString);
        return remapJson(node, outputMapping);
    }

    /**
     * @param node
     *            the json-node to be re-mapped
     * @param outputMapping
     *            instructions for the mapping
     * @return string representation of the remapped json.
     * @throws IOException
     */
    public static String remapJson(JsonNode node, OutputMapping outputMapping) throws IOException {
        return remapJson(node, outputMapping, LibConfiguration.JSON_MAPPING_DEPTH);
    }

    /**
     * @param jsonMapper
     *            reference to the jsonMapper for de-serialization.
     * @param pojoClass
     *            the type the json-string should be de-serialized to
     * @param node
     *            the json-node to be mapped
     * @param outputMapping
     *            instructions for the mapping
     * @return list of pojos of type pojoClass de-serialized from the given json-node.
     * @throws IOException
     */
    public static <T> List<T> mapJson(ObjectMapper jsonMapper, Class<T> pojoClass, JsonNode node,
            OutputMapping outputMapping) throws IOException {
        return mapJson(jsonMapper, pojoClass, node, outputMapping, 20);
    }

    /*
     * For internal use. Gives control to tune the mapping-depth.
     */
    static String remapJson(JsonNode node, OutputMapping outputMapping, int depth) throws IOException {
        if (depth == 0) {
            return "";
        }
        List<String> collectedJson = collectRemapJson(node, outputMapping, new RemapStringCollector(), depth);
        return Helper.listToJson(collectedJson);
    }

    /*
     * For internal use. Gives control to tune the mapping-depth.
     */
    static <T> List<T> mapJson(ObjectMapper jsonMapper, Class<T> pojoClass, JsonNode node, OutputMapping outputMapping,
            int depth) throws IOException {
        return collectRemapJson(node, outputMapping, new RemapObjectCollector<T>(jsonMapper, pojoClass), depth);
    }

    /*
     * For internal use. Performs the recursive re-mapping using a specific collector.
     */
    static <T> List<T> collectRemapJson(JsonNode node, OutputMapping outputMapping, RemapCollector<T> collector,
            int depth) throws IOException {
        int n = node.isArray() ? node.size() : 1;
        for (int i = 0; i < n; i++) {
            Map<String, Object> outputAsMap = new HashMap<>();
            for (OutputMappingElement mapping : outputMapping.getList()) {

                if (!(mapping instanceof OutputMappingElement.Name)) {
                    logger.warn("Only name based mapping is supported!");
                    throw new BridgeIoTException("Only name based mapping is supported!");
                }

                String slashedPath = "/" + ((OutputMappingElement.Name) mapping).getSourceNamePath() //
                        .replaceAll("\\.", "/") //
                        .replaceAll("\\[", "/") //
                        .replaceAll("\\]", "");

                slashedPath = node.isArray() ? "/" + i + slashedPath : slashedPath;

                JsonNode leaf = node.at(slashedPath);

                Object mappedValue;
                if (leaf.isArray()) {
                    CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class,
                            Object.class);
                    mappedValue = mapper.readerFor(collectionType)
                            .readValue(remapJson(leaf, ((OutputMappingElement.Array) mapping).getMembers(), depth - 1));
                } else {
                    mappedValue = leaf.asText();
                }
                Helper.constructivePut(outputAsMap, mapping.getMappedFieldName(), mappedValue);

            }
            collector.addElement(Helper.mapToJson(outputAsMap, false));
        }

        return collector.collected;
    }

    @Deprecated
    public static String remapJsonBeforeOct(JsonNode node, OutputMapping outputMapping) throws JsonProcessingException {
        List<String> list = new LinkedList<>();
        int n = node.isArray() ? node.size() : 1;
        for (int i = 0; i < n; i++) {
            HashMap<String, Object> outputAsMap = new HashMap<>();
            for (OutputMappingElement mapping : outputMapping.getList()) {

                if (!(mapping instanceof OutputMappingElement.Name)) {
                    throw new BridgeIoTException(
                            "Output mapping has to be either name or type based. No combination allowed (yet)");
                }

                String slashedPath = "/" + ((OutputMappingElement.Name) mapping).getSourceNamePath() //
                        .replaceAll("\\.", "/") //
                        .replaceAll("\\[", "/") //
                        .replaceAll("\\]", "");

                slashedPath = node.isArray() ? "/" + i + slashedPath : slashedPath;

                JsonNode leaf = node.at(slashedPath);

                Helper.constructivePut(outputAsMap, mapping.getMappedFieldName(), leaf.asText());
            }
            list.add(Helper.mapToJson(outputAsMap, false));
        }

        return Helper.listToJson(list);
    }

    /**
     * Removes the name-spaces from a xml-string
     * 
     * @param xmlString
     *            to be reduced
     * @return xml-string without name-spaces
     */
    public static String removeNameSpacesInXml(String xmlString) {
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setValidating(false);
        f.setXIncludeAware(false);
        f.setNamespaceAware(true);
        try {
            SAXParser sp = f.newSAXParser();
            DropNamespaces ds = new DropNamespaces();
            sp.parse(new InputSource(new StringReader(xmlString)), ds);
            return ds.result();
        } catch (SAXException | IOException e) {
            logger.error("Failed to parse XML, returning untreated.", e);
            return xmlString;
        } catch (ParserConfigurationException e) {
            logger.error("Invalid XML parser configuration.", e);
            throw new BridgeIoTException("Invalid XML parser configuration.", e);
        }
    }

    private static class DropNamespaces extends DefaultHandler {

        private final StringBuilder out = new StringBuilder();
        private String lastTag;
        private boolean hasContent;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            // Close previous tag.
            out.append(String.format("<%s", localName));
            for (int i = 0; i < attributes.getLength(); i++) {
                // Drop namespace declarations.
                if (attributes.getLocalName(i).contains("xmlns:")) {
                    continue;
                }
                out.append(String.format(" %s=\"%s\"", attributes.getLocalName(i), attributes.getValue(i)));
            }
            hasContent = false;
            lastTag = qName;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // No differently named tag in between
            if (lastTag.equals(qName) && !hasContent)
                out.append("/>");
            else
                out.append(String.format("</%s>", localName));
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            out.append(">");
            out.append(ch, start, length);
            hasContent = true;
        }

        public String result() {
            return out.toString();
        }
    }

    /**
     * Abstract collector used in the json-remapping. It offers possibility to store the mapping results in different
     * data-structures (json string or converted pojos)
     * 
     * @param <T>
     *            the type that should be collected.
     */
    abstract static class RemapCollector<T> {
        protected List<T> collected = new ArrayList<>();

        /**
         * Add Element to the collector.
         * 
         * @param json
         *            to be collected.
         */
        public abstract void addElement(String json);

        /**
         * Get collection.
         * 
         * @return collection.
         */
        public List<T> getCollected() {
            return collected;
        }
    }

    /**
     * Collects elements as string.
     */
    static class RemapStringCollector extends RemapCollector<String> {
        @Override
        public void addElement(String jsonElement) {
            collected.add(jsonElement);
        }
    }

    /**
     * Collects elements as pojos of a given type. Therefore the collected json-strings needs to be de-serialized.
     * 
     * @param <T>
     *            the type of pojos that should be collected.
     */
    static class RemapObjectCollector<T> extends RemapCollector<T> {
        ObjectMapper jsonMapper;
        Class<T> pojoClass;

        /**
         * @param jsonMapper
         *            reference to the json-mapper for de-serialization.
         * @param pojoClass
         *            the type of pojos that should be collected.
         */
        public RemapObjectCollector(ObjectMapper jsonMapper, Class<T> pojoClass) {
            this.jsonMapper = jsonMapper;
            this.pojoClass = pojoClass;
        }

        @Override
        public void addElement(String jsonElement) {
            T converted;
            try {
                converted = jsonMapper.readerFor(pojoClass).readValue(jsonElement);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new BridgeIoTException("Something went wrong with Object Mapping");
            }
            collected.add(converted);
        }
    }

    public static boolean isBehindProxy(String proxyHost) throws UnknownHostException, IOException {
        proxyHost = proxyHost.replaceFirst(".*//", "");
        return InetAddress.getByName(proxyHost).isReachable(1000);
    }

    public static Proxy ifProxyGetProxy(String proxyHost, int proxyPort) throws UnknownHostException, IOException {
        proxyHost = proxyHost.replaceFirst(".*//", "");
        return isBehindProxy(proxyHost) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort))
                : null;
    }

    public static String normalizeRdfUri(String uri) {
        String normalizedUri = !uri.toLowerCase().startsWith("http://") && !uri.toLowerCase().startsWith("https://")
                ? "http://" + uri.toLowerCase()
                : uri.toLowerCase();
        return normalizedUri.replaceAll("schema[:,//]", "schema.org/");
    }

    public static String convertJsonToGraphQl(String json) {
        return json.replaceAll("\"(\\w*?)\":", "$1:");

    }
}