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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.SubscribableOfferingDescriptionCore;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.hamcrest.core.StringContains;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class HelperTest {

    static final String LOCATION_JSON = "{\"coord\":{\"lon\":9.99,\"lat\":49.99}}";
    static final String WEATHER_JSON = "{\"weather\":[{\"id\":800,\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01d\"}, {\"id\":801,\"main\":\"Snow\",\"description\":\"thunder storm\",\"icon\":\"01e\"}]}";
    static final String SENSOR1_JSON = "{\"valueType\":\"temperature\",\"value\":\"5.30\"}";
    static final String SENSOR2_JSON = "{\"valueType\":\"humidity\",\"value\":\"95.30\"}";
    static final String ARRAY_AIR_DATA_JSON = String.format("[%s,%s]", SENSOR1_JSON, SENSOR2_JSON);
    static final String NAMED_ARRAY_AIR_DATA_JSON = String.format("{\"airData\":[%s,%s]}", SENSOR1_JSON, SENSOR2_JSON);
    static final String JSON_MATCHING_OFFERINGS_ITEM1 = "{\"id\":\"id1\",\"activation\":{\"status\":true,\"expirationTime\":1513672278599} }";
    static final String JSON_MATCHING_OFFERINGS_ITEM2 = "{\"id\":\"id2\",\"activation\":{\"status\":false,\"expirationTime\":1513676668599} }";
    static final String JSON_MATCHING_OFFERINGS_ONE = String.format("{\"data\":{\"matchingOfferings\":[%s]}}",
            JSON_MATCHING_OFFERINGS_ITEM1);
    static final String JSON_MATCHING_OFFERINGS_TWO = String.format("{\"data\":{\"matchingOfferings\":[%s,%s]}}",
            JSON_MATCHING_OFFERINGS_ITEM1, JSON_MATCHING_OFFERINGS_ITEM2);

    private static ObjectMapper staticMapper = Helper.mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void unmarshallDiscoverResponseSingleMatch() throws JsonProcessingException {
        // Act
        List<OfferingDescription> descList = Helper.unmarshallDiscoverResponse(JSON_MATCHING_OFFERINGS_ONE,
                OfferingDescription.class);

        // Assert
        assertThat(descList).hasSize(1);
        assertThat(descList.get(0)).hasFieldOrPropertyWithValue("id", "id1").hasFieldOrProperty("activation")
                .extracting("activation.status", "activation.expirationTime").contains(true, 1513672278599L);
    }

    @Test
    public void unmarshallDiscoverResponseTwoMatches() throws JsonProcessingException {
        // Act
        List<OfferingDescription> descList = Helper.unmarshallDiscoverResponse(JSON_MATCHING_OFFERINGS_TWO,
                OfferingDescription.class);

        // Assert
        assertThat(descList).hasSize(2);
        assertThat(descList.get(0)).hasFieldOrPropertyWithValue("id", "id1").hasFieldOrProperty("activation")
                .extracting("activation.status", "activation.expirationTime").contains(true, 1513672278599L);
        assertThat(descList.get(1)).hasFieldOrPropertyWithValue("id", "id2").hasFieldOrProperty("activation")
                .extracting("activation.status", "activation.expirationTime").contains(false, 1513676668599L);
    }

    @Test
    public void unmarshallDiscoverResponseInvalidInput() throws JsonProcessingException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Unmarshalling discover response"));

        // Act
        List<OfferingDescription> descList = Helper.unmarshallDiscoverResponse(LOCATION_JSON,
                OfferingDescription.class);
    }

    @Test
    public void mapToJsonPrettyPrint() throws JsonProcessingException {
        // Arrange
        Map<String, Object> inputMap = createThreeLevelMap("level1", "level2", "level3", "testValue");

        // Act
        String json = Helper.mapToJson(inputMap, true);

        // Assert
        assertThat(json).contains("level3").contains("level2").contains("level1").contains("testValue").contains("\n");
    }

    @Test
    public void mapToJsonUglyPrint() throws JsonProcessingException {
        // Arrange
        Map<String, Object> inputMap = createThreeLevelMap("level1", "level2", "level3", "testValue");

        // Act
        String json = Helper.mapToJson(inputMap, false);

        // Assert
        assertThat(json).contains("level3").contains("level2").contains("level1").contains("testValue")
                .doesNotContain("\n");
    }

    @Test
    public void mapToJsonNullValue() throws JsonProcessingException {
        // Arrange
        Map<String, Object> inputMap = createThreeLevelMap("level1", "level2", "level3a", "testValue", "level3b", null);

        // Act
        String json = Helper.mapToJson(inputMap, true);

        // Assert
        assertThat(json).contains("level3a").contains("level2").contains("level1").contains("testValue")
                .doesNotContain("level3b");
    }

    @Test
    public void mapToJsonNonStringField() throws JsonProcessingException {
        // Arrange
        Map<String, Object> inputMap = createThreeLevelMap("level1", "level2", "level3a", Boolean.TRUE, "level3b",
                Double.MAX_VALUE);

        // Act
        String json = Helper.mapToJson(inputMap, true);

        // Assert
        assertThat(json).contains("level3a").contains("level2").contains("level1")
                .contains(String.valueOf(Boolean.TRUE)).contains("level3b").contains(String.valueOf(Double.MAX_VALUE));
    }

    @Test
    public void objectToJsonPrettyPrint() throws JsonProcessingException {
        // Arrange
        Composite composite = new Composite(new Composite(new Composite("someString")));

        // Act
        String json = Helper.objectToJson(composite, true);

        // Assert
        assertThat(json)
                .containsSequence("{", "composite", ":", "{", "composite", ":", "{", "leaf", ":", "true", "}", ",",
                        "leaf", ":", "false", "}", ",", "leaf", ":", "false", "}")
                .contains("\n").doesNotContain("null");
    }

    @Test
    public void objectToJsonUglyPrint() throws JsonProcessingException {
        // Arrange
        Composite composite = new Composite(new Composite(new Composite("someString")));

        // Act
        String json = Helper.objectToJson(composite, false);

        // Assert
        assertThat(json)
                .containsSequence("{", "composite", ":", "{", "composite", ":", "{", "leaf", ":", "true", "}", ",",
                        "leaf", ":", "false", "}", ",", "leaf", ":", "false", "}")
                .doesNotContain("\n").doesNotContain("null");
    }

    @Test
    public void getPojoAsJsonException() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Cannot create JSON"));
        expectedException.expectCause(isA(JsonProcessingException.class));

        // Arrange a mock that causes a cycle on jackson-side that causes an InvalidDefinitionException
        Object cyclePojo = mock(Object.class);
        // when(cyclePojo.toString()).thenReturn(cyclePojo.getClass().getName());

        // Act
        Helper.getPojoAsJson(cyclePojo);

        // Assert
        // see expectedException Rule
    }

    @Test
    public void getPojoAsJsonSuccess() {
        // Arrange
        Composite pojo = new Composite(new Composite(new Composite("someString")));

        // Act
        String json = Helper.getPojoAsJson(pojo);

        // Assert
        assertThat(json)
                .containsSequence("{", "composite", ":", "{", "composite", ":", "{", "leaf", ":", "true", "}", ",",
                        "leaf", ":", "false", "}", ",", "leaf", ":", "false", "}")
                .contains("\n").doesNotContain("null");
    }

    @Test
    public void getPojoAsJsonCompactException() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Cannot create JSON"));
        expectedException.expectCause(isA(JsonProcessingException.class));

        // Arrange a mock that causes a cycle on jackson-side that causes an InvalidDefinitionException
        Object cyclePojo = mock(Object.class);
        // when(cyclePojo.toString()).thenReturn(cyclePojo.getClass().getName());

        // Act
        Helper.getPojoAsJsonCompact(cyclePojo);

        // Assert
        // see expectedException Rule
    }

    @Test
    public void getPojoAsJsonCompactSuccess() {
        // Arrange
        Composite pojo = new Composite(new Composite(new Composite("someString")));

        // Act
        String json = Helper.getPojoAsJsonCompact(pojo);

        // Assert
        assertThat(json)
                .containsSequence("{", "composite", ":", "{", "composite", ":", "{", "leaf", ":", "true", "}", ",",
                        "leaf", ":", "false", "}", ",", "leaf", ":", "false", "}")
                .doesNotContain("\n").doesNotContain("null");
    }

    @Test
    public void jsonPrettyPrintSuccess() {
        // Arrange
        // nothing to do

        // Act
        String formatted = Helper.jsonPrettyPrint(NAMED_ARRAY_AIR_DATA_JSON);

        // Assert
        assertThat(formatted)
                .containsSequence("{", "airData", ":", "[", "{", "valueType", ":", "temperature", ",", "value", ":",
                        "5.30", "}", ",", "{", "valueType", ":", "humidity", ",", "value", ":", "95.30", "}", "]", "}")
                .contains("\n").contains("    ").doesNotContain("null");
    }

    @Test
    public void listToJsonSuccess() throws JsonProcessingException {
        // Arrange
        List<String> strList = new ArrayList<>();
        strList.add("element1");
        strList.add("element2");
        strList.add("element3");
        strList.add("element4");

        // Act
        String json = Helper.listToJson(strList);

        // Assert
        assertThat(json).contains("element1,").containsSequence("[", "element1", ",", "element2", ",", "element3", ",",
                "element4", "]");

    }

    @Test
    public void listToJsonMixedInput() throws JsonProcessingException {
        // Arrange
        List<String> strList = new ArrayList<>();
        strList.add(null);
        strList.add("");
        strList.add("same");
        strList.add("same");

        // Act
        String json = Helper.listToJson(strList);

        // Assert
        assertThat(json).contains("null,").containsSequence("[", "null", ",", ",", "same", ",", "same", "]");
    }

    @Test
    public void listToJsonComplex() throws JsonProcessingException {
        // Arrange
        List<String> strList = new ArrayList<>();
        strList.add(SENSOR1_JSON);
        strList.add(SENSOR2_JSON);
        strList.add("other");
        strList.add("more");

        // Act
        String json = Helper.listToJson(strList);

        // Assert
        assertThat(json).containsSequence("[", "{", "\"", "valueType", "\"", ":", "\"", "temperature", "\"", ",", "\"",
                "value", "\"", ":", "\"", "5.30", "\"", "}", ",", "{", "\"", "valueType", "\"", ":", "\"", "humidity",
                "\"", ",", "\"", "value", "\"", ":", "\"", "95.30", "\"", "}", ",", "other", ",", "more", "]");
    }

    @Test
    public void constructivePutException() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Illegal field name "));

        // Act
        Helper.constructivePut(new HashMap<String, Object>(), "[", "testValue");

        // Assert
        // see expectedException Rule
    }

    @Test
    public void constructivePutSimple() {
        // Arrange
        Map<String, Object> fieldMap = new HashMap<>();

        // Act
        Helper.constructivePut(fieldMap, "simple", "testValue");

        // Assert
        assertThat(fieldMap.get("simple")).isEqualTo("testValue");
    }

    @Test
    public void constructivePutTwoLevel() {
        // Arrange
        Map<String, Object> fieldMap = new HashMap<>();

        // Act
        Helper.constructivePut(fieldMap, "level1.level2", "testValue");

        // Assert
        assertThat(fieldMap.get("level1")).isInstanceOf(Map.class);
        assertThat(((Map<String, Object>) fieldMap.get("level1")).get("level2")).isEqualTo("testValue");
    }

    @Test
    public void constructivePutTwoLevelPrefilledMap() {
        // Arrange
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("level1", new HashMap<>());

        // Act
        Helper.constructivePut(fieldMap, "level1.level2", "testValue");

        // Assert
        assertThat(fieldMap.get("level1")).isInstanceOf(Map.class);
        assertThat(((Map<String, Object>) fieldMap.get("level1")).get("level2")).isEqualTo("testValue");
    }

    @Test
    public void constructivePutTwoLevelPrefilledMapInvalid() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Value assignment for"));

        // Arrange
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("level1", "notAllowedValue");

        // Act
        Helper.constructivePut(fieldMap, "level1.level2", "testValue");

        // Assert
        // see expectedException Rule
    }

    @Test
    public void remapJsonEmptyDepth() throws IOException {
        // Arrange

        // Act
        String emptyResult = Helper.remapJson(null, null, 0);

        // Assert
        assertThat(emptyResult).isEmpty();
    }

    @Test
    public void remapJsonNonArrayJson() throws IOException {
        // Arrange
        JsonNode node = staticMapper.reader().readTree(LOCATION_JSON);
        OutputMapping outMapping = OutputMapping.create().addNameMapping("coord.lat", "location.latitude")
                .addNameMapping("coord.lon", "location.longitude");

        // Act
        String remappedResult = Helper.remapJson(node, outMapping, 20);

        // Assert
        assertThat(remappedResult).contains("location").contains("latitude").contains("longitude");
    }

    @Test
    public void remapJsonNonEmptyOutputMapping() throws IOException {
        // Arrange
        JsonNode node = staticMapper.reader().readTree(ARRAY_AIR_DATA_JSON);
        OutputMapping outMapping = OutputMapping.create();

        // Act
        String emptyResult = Helper.remapJson(node, outMapping, 20);

        // Assert
        assertThat(emptyResult).containsSequence("[", "{", "}", ",", "{", "}", "]");
    }

    @Test

    public void remapJsonOutputMappingElementTypeSkipped() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Only name based mapping is supported!"));

        // Arrange
        JsonNode node = staticMapper.reader().readTree(ARRAY_AIR_DATA_JSON);
        OutputMapping outMapping = OutputMapping.create().addTypeMapping("myType", "myField");

        // Act
        Helper.remapJson(node, outMapping, 20);

        // Assert
        // see expectedException Rule
    }

    @Test
    @Ignore("Unfortunately, it is only possible to map leaf-filed but no ObjectNode")
    public void remapJsonSimple() throws IOException {
        // Arrange
        JsonNode node = staticMapper.reader().readTree(LOCATION_JSON);
        OutputMapping outMapping = OutputMapping.create().addNameMapping("coord", "coordinate");

        // Act
        String remappedResult = Helper.remapJson(node, outMapping, 20);

        // Assert
        assertThat(remappedResult).contains("coordinate").contains("lat").contains("lon");
    }

    @Test
    public void remapJsonNamedArray() throws IOException {
        // Arrange
        JsonNode node = staticMapper.reader().readTree(NAMED_ARRAY_AIR_DATA_JSON);
        // OutputMapping outMapping = OutputMapping.create()
        // .addNameMapping("airData[0].value", "temperature").addNameMapping("airData[1].value", "humidity");
        OutputMapping outMapping = OutputMapping.create().addArrayMapping("airData", "newArray", OutputMapping.create()
                .addNameMapping("valueType", "sensor.type").addNameMapping("value", "sensor.value"));

        // Act
        String emptyResult = Helper.remapJson(node, outMapping, 20);

        // Assert
        assertThat(emptyResult).contains("newArray").contains("sensor").contains("type").contains("value")
                .contains("temperature").contains("5.30").contains("humidity").contains("95.30");
    }

    @Test
    public void remapJsonArray() throws IOException {
        // Arrange
        JsonNode node = staticMapper.reader().readTree(ARRAY_AIR_DATA_JSON);
        OutputMapping outMapping = OutputMapping.create().addNameMapping("valueType", "sensor.type")
                .addNameMapping("value", "sensor.value");

        // Act
        String emptyResult = Helper.remapJson(node, outMapping, 20);

        // Assert
        assertThat(emptyResult).contains("sensor").contains("type").contains("value").contains("temperature")
                .contains("5.30").contains("humidity").contains("95.30");
    }

    @Test
    public void remapJsonOutputMappingInvalidAndValidMixed() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Only name based mapping is supported!"));

        // Arrange
        JsonNode node = staticMapper.reader().readTree(ARRAY_AIR_DATA_JSON);
        OutputMapping outMapping = OutputMapping.create().addTypeMapping("myType", "myField")
                .addNameMapping("valueType", "sensor.type").addNameMapping("value", "sensor.value");

        // Act
        Helper.remapJson(node, outMapping, 20);

        /// Assert
        // see expectedException Rule
    }

    @Test
    public void remapJsonNonExisting() throws IOException {
        // Arrange
        JsonNode node = staticMapper.reader().readTree(ARRAY_AIR_DATA_JSON);

        OutputMapping outMapping = OutputMapping.create().addNameMapping("", "some");

        // Act
        String emptyResult = Helper.remapJson(node, outMapping, 20);

        // Assert
        assertThat(emptyResult).doesNotContain("sensor").doesNotContain("type").doesNotContain("value")
                .doesNotContain("temperature").doesNotContain("5.30").doesNotContain("humidity")
                .doesNotContain("95.30");
    }

    @Test
    public void removeNameSpacesInXmlSimple() {
        // Arrange

        // Act
        String result = Helper.removeNameSpacesInXml("<test></test>");

        // Assert
        assertThat(result).isEqualTo("<test/>");
    }

    @Test
    public void removeNameSpacesInXmlComplex() {
        // Arrange

        // Act
        String result = Helper.removeNameSpacesInXml("<?xml version=\"1.0\" encoding=\"UTF-8\" "
                + "standalone=\"yes\"?><ns2:dimmableRecordType xsi:nil=\"true\" xmlns:ns2=\"http://www"
                + ".baas-itea3.eu/temperature\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");

        // Assert
        assertThat(result).isEqualTo("<dimmableRecordType nil=\"true\"/>");
    }

    @Test
    public void removeNameSpacesInXmlWithContent() {
        // Arrange

        // Act
        String result = Helper.removeNameSpacesInXml("<content>This is a test</content>");

        // Assert
        assertThat(result).isEqualTo("<content>This is a test</content>");
    }

    @Test
    public void removeNameSpacesInXmlSimpleWithNamespaces() {
        // Arrange

        // Act
        String result = Helper.removeNameSpacesInXml(
                "<test:content xmlns:test=\"http://www.big-iot.org\">This is a test</test:content>");

        // Assert
        assertThat(result).isEqualTo("<content>This is a test</content>");
    }

    @Test
    public void removeNameSpacesInXmlSelfClosing() {
        // Arrange

        // Act
        String result = Helper.removeNameSpacesInXml("<test:content xmlns:test=\"http://www.big-iot.org\"/>");

        // Assert
        assertThat(result).isEqualTo("<content/>");

    }

    @Test
    public void showOfferingDescriptionsEmptyList() {
        // Arrange
        List<OfferingDescription> emptyList = Collections.emptyList();

        // Act
        String prettyPrint = Helper.showOfferingDescriptions(emptyList, Boolean.FALSE);

        // Assert
        assertThat(prettyPrint).containsSequence("Discovered", "0", "offerings");
    }

    @Test
    public void showOfferingDescriptionsSingleElement() {
        // Arrange
        OfferingDescription od1 = mock(OfferingDescription.class);
        when(od1.getName()).thenReturn("element1");
        List<OfferingDescription> twoElementList = Arrays.asList(od1);

        // Act
        String prettyPrint = Helper.showOfferingDescriptions(twoElementList, Boolean.TRUE);

        // Assert
        assertThat(prettyPrint).doesNotContain("offerings").containsSequence("Discovered", "1", "offering",
                "with the following names:", "element1", "Offering description #", "Mock");
    }

    @Test
    public void showOfferingDescriptionsTwoElements() {
        // Arrange
        OfferingDescription od1 = mock(OfferingDescription.class);
        when(od1.getName()).thenReturn("element1");
        OfferingDescription od2 = mock(OfferingDescription.class);
        when(od2.getName()).thenReturn("element2");
        List<OfferingDescription> twoElementList = Arrays.asList(od1, od2);

        // Act
        String prettyPrint = Helper.showOfferingDescriptions(twoElementList, Boolean.FALSE);

        // Assert
        assertThat(prettyPrint).containsSequence("Discovered", "2", "offerings", "with the following names:",
                "element1", "element2");
    }

    @Test
    public void showOfferingDescriptionsMixedType() {
        // Arrange
        OfferingDescription od1 = mock(OfferingDescription.class);
        when(od1.getName()).thenReturn("element1");
        OfferingDescription od2 = mock(RegistrableOfferingDescription.class);
        when(od2.getName()).thenReturn("element2");
        OfferingDescription od3 = mock(SubscribableOfferingDescriptionCore.class);
        when(od3.getName()).thenReturn("element3");
        List<OfferingDescription> twoElementList = Arrays.asList(od1, od2, od3);

        // Act
        String prettyPrint = Helper.showOfferingDescriptions(twoElementList, Boolean.TRUE);

        // Assert
        assertThat(prettyPrint).containsSequence("Discovered", "3", "offerings", "with the following names:",
                "element1", "element2", "element3", "Offering description #1", "Mock for OfferingDescription",
                "Offering description #2", "Mock for RegistrableOfferingDescription", "Offering description #3",
                "Mock for SubscribableOfferingDescriptionCore");
    }

    Map<String, Object> createThreeLevelMap(String keyLevel1, String keyLevel2, String keyLevel3a, Object valueLevel3a,
            String keyLevel3b, Object valueLevel3b) {
        Map<String, Object> level1Map = new HashMap<>();
        Map<String, Object> level2Map = new HashMap<>();
        Map<String, Object> level3Map = new HashMap<>();
        level3Map.put(keyLevel3a, valueLevel3a);
        level3Map.put(keyLevel3b, valueLevel3b);
        level2Map.put(keyLevel2, level3Map);
        level1Map.put(keyLevel1, level2Map);

        return level1Map;
    }

    Map<String, Object> createThreeLevelMap(String keyLevel1, String keyLevel2, String keyLevel3, Object valueLevel3) {
        return createThreeLevelMap(keyLevel1, keyLevel2, keyLevel3, valueLevel3, null, null);
    }

    class Composite {
        Object leafValue;
        Composite composite;

        public Composite(Composite composite) {
            this.composite = composite;
            leafValue = Boolean.FALSE;
        }

        public Composite(Object leafValue) {
            this.leafValue = leafValue;
        }

        public boolean isLeaf() {
            return this.composite == null;
        }

        public Object getComposite() {
            return isLeaf() ? null : this.composite;
        }
    }

}
