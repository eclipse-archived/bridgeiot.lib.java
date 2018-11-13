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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.configuration.LibConfiguration;
import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.IOData;
import org.eclipse.bridgeiot.lib.model.RDFType;
import org.eclipse.bridgeiot.lib.offering.AccessResponse.TypeInitializer;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMapping;
import org.eclipse.bridgeiot.lib.offering.mapping.OutputMappingElement;
import org.eclipse.bridgeiot.lib.offering.mapping.ResponseMappingType;
import org.eclipse.bridgeiot.lib.offering.parameters.NumberParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectMember;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@RunWith(MockitoJUnitRunner.class)
public class AccessResponseTest {

    static final String JSON_MSG = "{ \"testJsonKey\": \"this JSON is valid\" }";
    static final String JSON_TESTPOJO_MSG = "{ \"testStatus\": \"this JSON is valid\" }";
    static final String JSON_ARRAY_TESTPOJO_MSG = "[{ \"testStatus\": \"this JSON is valid\" }]";

    private static ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JodaModule());;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void typeInitializerUnknownType() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(StringContains.containsString("Unsupported content type: "));

        // Act
        TypeInitializer.fromMimeType(MimeType.TEXT_PLAIN);

        // Assert
        // see expectedException rule
    }

    @Test
    public void typeInitializerJsonType() {
        // Act
        TypeInitializer iType = TypeInitializer.fromMimeType(MimeType.APPLICATION_JSON);

        // Assert
        assertThat(iType).isEqualTo(TypeInitializer.JSON_TYPE);
    }

    @Test
    public void typeInitializerRetrieveJsonException() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Cannot parse response message as JSON");

        // Act
        TypeInitializer.retrieveJsonNode(MimeType.APPLICATION_JSON, "[fail");

        // Assert
        // see expectedException rule
    }

    @Test
    public void typeInitializerRetrieveJsonSuccess() {
        // Act
        JsonNode jsonNode = TypeInitializer.retrieveJsonNode(MimeType.APPLICATION_JSON, JSON_MSG);

        // Assert
        assertThat(jsonNode).isNotNull().isNotEmpty();
        assertThat(jsonNode.get("testJsonKey")).isNotNull();
        assertThat(jsonNode.get("testJsonKey").asText()).isEqualTo("this JSON is valid");
    }

    @Test
    public void typeInitializerRetrieveXmlException() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Cannot parse XML response on JSON intermediate step");

        // Act
        TypeInitializer.retrieveJsonNode(MimeType.APPLICATION_XML, "<fail>");

        // Assert
        // see expectedException rule
    }

    @Test
    public void typeInitializerRetrieveXmlSuccess() {
        // Act
        JsonNode jsonNode = TypeInitializer.retrieveJsonNode(MimeType.APPLICATION_XML,
                "<root> <testValidXml>this XML is valid</testValidXml> </root>");

        // Assert
        assertThat(jsonNode).isNotNull().isNotEmpty();
        assertThat(jsonNode.get("testValidXml")).isNotNull();
        assertThat(jsonNode.get("testValidXml").asText()).isEqualTo("this XML is valid");
    }

    @Test
    public void mapWithoutOutputMappingInvalidJson() {
        // Arrange
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        AccessResponse accessResp = new AccessResponse(JSON_ARRAY_TESTPOJO_MSG, offeringDescMock);

        // Act
        List<NoSuchMethodExceptionCauser> result = accessResp.map(NoSuchMethodExceptionCauser.class);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void mapWithoutOutputMappingNonArrayFails() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Non-array results not yet supported");

        // Arrange
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        AccessResponse accessResp = new AccessResponse(JSON_TESTPOJO_MSG, offeringDescMock);

        // Act
        accessResp.map(TestPojo.class);

        // Assert
        // see expectedException rule
    }

    @Test
    public void mapWithoutOutputMappingNonArraySuccess() {
        // Arrange
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        AccessResponse accessResp = new AccessResponse(JSON_ARRAY_TESTPOJO_MSG, offeringDescMock);

        // Act
        List<TestPojo> result = accessResp.map(TestPojo.class);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).testStatus).isEqualTo("this JSON is valid");
    }

    @Test
    public void mapWithoutOutputMappingPojo() throws JsonProcessingException {
        // Arrange
        TestPojo pojo = TestPojo.filloutSimplePojo(new TestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));

        // Act
        List<TestPojo> result = accessResp.map(TestPojo.class);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull().isEqualToComparingFieldByField(pojo);
    }

    @Test
    public void mapWithoutOutputMappingAnnotatedPojo() throws JsonProcessingException {
        // Arrange
        ObjectParameter complexParameter = AnnotatedTestPojo.createComplexObject();

        OfferingDescription offeringDescription = mock(OfferingDescription.class);
        when(offeringDescription.getOutputs()).thenReturn(complexParameter);
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, offeringDescription);

        // Act
        List<AnnotatedTestPojo> result = accessResp.map(AnnotatedTestPojo.class);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull().isEqualToComparingFieldByField(pojo);
    }

    @Test
    public void mapWithTypeBasedOutputMappingNonArrayFails() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Non-array results not yet supported");

        // Arrange
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        AccessResponse accessResp = new AccessResponse(JSON_TESTPOJO_MSG, offeringDescMock);
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addTypeMapping("datex:distanceFromParkingSpace",
                "testDistance");

        // Act
        accessResp.map(TestPojo.class, outMapping);

        // Assert
        // see expectedException rule
    }

    @Test
    public void mapWithTypeBasedOutputMappingAnnotatedPojo() throws JsonProcessingException {
        // Arrange response
        ObjectParameter complexParameter = AnnotatedTestPojo.createComplexObject();

        OfferingDescription offeringDescription = mock(OfferingDescription.class);
        when(offeringDescription.getOutputs()).thenReturn(complexParameter);
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, offeringDescription);

        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addTypeMapping("datex:distanceFromParkingSpace",
                "testDistance");

        // Act
        List<AnnotatedTestPojo> result = accessResp.map(AnnotatedTestPojo.class, outMapping);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull().isEqualToComparingFieldByField(pojo);
    }

    @Test
    public void mapWithTypeBasedOutputMappingAnnotatedPojos() throws JsonProcessingException {

        // Arrange response
        ObjectParameter complexParameter = AnnotatedTestPojo.createComplexObject();

        OfferingDescription offeringDescription = mock(OfferingDescription.class);
        when(offeringDescription.getOutputs()).thenReturn(complexParameter);

        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        AnnotatedTestPojo pojo2 = AnnotatedTestPojo.filloutLocatedPojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo, pojo2));
        AccessResponse accessResp = new AccessResponse(json, offeringDescription);
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addTypeMapping("datex:distanceFromParkingSpace",
                "testDistance");

        // Act
        List<AnnotatedTestPojo> result = accessResp.map(AnnotatedTestPojo.class, outMapping);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull().isEqualToComparingFieldByField(pojo);
        assertThat(result.get(1)).isNotNull().isEqualToIgnoringGivenFields(pojo2, "testCoordinate");
        assertThat(result.get(1).testCoordinate).isNotNull().isEqualToComparingFieldByField(pojo2.testCoordinate);
    }

    @Test
    public void mapWithMixedNameAndTypeBasedOutputMappingAnnotatedPojoException() throws JsonProcessingException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Sorry! Name mapping cannot yet be combined with type mapping");

        ObjectParameter complexParameter = AnnotatedTestPojo.createComplexObject();

        OfferingDescription offeringDescription = mock(OfferingDescription.class);
        when(offeringDescription.getOutputs()).thenReturn(complexParameter);

        // Arrange response
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, offeringDescription);
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create()
                .addTypeMapping("datex:distanceFromParkingSpace", "testDistance")
                .addNameMapping("sourceNamePath", "destinationNamePath");

        // Act
        accessResp.map(AnnotatedTestPojo.class, outMapping);

        // Assert
        // see expectedException rule
    }

    @Test
    public void mapWithMixedTypeAndNameBasedOutputMappingAnnotatedPojoException() throws JsonProcessingException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Only name based mapping is supported!");

        // Arrange response
        TestPojo pojo = TestPojo.filloutSimplePojo(new TestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addNameMapping("sourceNamePath", "destinationNamePath")
                .addTypeMapping("datex:distanceFromParkingSpace", "testDistance");

        // Act
        accessResp.map(TestPojo.class, outMapping);

        // Assert
        // see expectedException rule
    }

    @Test
    public void mapWithTypeBasedOutputMappingWithRdfTypesInOfferingDescription() throws JsonProcessingException {
        // Arrange mapping

        ObjectParameter complexParameter = AnnotatedTestPojo.createComplexObject();

        OfferingDescription offeringDescription = mock(OfferingDescription.class);
        when(offeringDescription.getOutputs()).thenReturn(complexParameter);

        // Arrange response
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, offeringDescription);
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addTypeMapping("datex:distanceFromParkingSpace",
                "testDistance");

        // Act
        List<AnnotatedTestPojo> result = accessResp.map(AnnotatedTestPojo.class, outMapping);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull().isEqualToComparingFieldByField(pojo);
    }

    @Test
    public void mapWithNameBasedOutputMappingPojo() throws JsonProcessingException {
        // Arrange response
        TestPojo pojo = TestPojo.filloutLocatedPojo(new TestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addNameMapping("testStatus", "testStatus");

        // Act
        List<TestPojo> result = accessResp.map(TestPojo.class, outMapping);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(0).testStatus).isEqualTo(pojo.testStatus);
        assertThat(result.get(0).testDistance).isNotEqualTo(pojo.testDistance);
    }

    @Test
    public void mapWithNameBasedOutputMappingPojoNonFieldException() throws JsonProcessingException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Something went wrong with Object Mapping");

        // Arrange response
        TestPojo pojo = TestPojo.filloutLocatedPojo(new TestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addNameMapping("testCoordinate", "testCoordinate");

        // Act
        accessResp.map(TestPojo.class, outMapping);

        // Assert
        // see expectedException rule
    }

    @Test
    public void mapWithNameBasedOutputMappingDottedMappingPojo() throws JsonProcessingException {
        // Arrange response
        TestPojo pojo = TestPojo.filloutGeosPojo(new TestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addNameMapping("testStatus", "testStatus")
                .addNameMapping("geos[0].latitude", "testCoordinate.latitude")
                .addNameMapping("geos[0].longitude", "testCoordinate.longitude");

        // Act
        List<TestPojo> result = accessResp.map(TestPojo.class, outMapping);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(0).testCoordinate.latitude).isEqualTo(pojo.geos[0].latitude);
        assertThat(result.get(0).testCoordinate.longitude).isEqualTo(pojo.geos[0].longitude);
    }

    @Test
    public void mapWithNameBasedOutputMappingPojoWithArray() throws JsonProcessingException {
        // Arrange response
        TestPojo pojo = TestPojo.filloutGeosPojo(new TestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addArrayMapping("geos", "geos",
                OutputMapping.create().addNameMapping("latitude", "latitude").addNameMapping("longitude", "longitude"));

        // Act
        List<TestPojo> result = accessResp.map(TestPojo.class, outMapping);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(0).geos[0].latitude).isEqualTo(pojo.geos[0].latitude);
        assertThat(result.get(0).geos[0].longitude).isEqualTo(pojo.geos[0].longitude);
        assertThat(result.get(0).geos[1].latitude).isEqualTo(pojo.geos[1].latitude);
        assertThat(result.get(0).geos[1].longitude).isEqualTo(pojo.geos[1].longitude);
    }

    @Test
    public void mapWithNameBasedOutputMappingPojoConversionException() throws JsonProcessingException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Something went wrong with Object Mapping");

        // Arrange
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        AccessResponse accessResp = new AccessResponse(JSON_ARRAY_TESTPOJO_MSG, offeringDescMock);
        // Arrange OutputMapping
        OutputMapping outMapping = OutputMapping.create().addNameMapping("testStatus", "testStatus");

        // Act
        accessResp.map(NoSuchMethodExceptionCauser.class, outMapping);

        // Assert
        // see expectedException rule
    }

    @Test
    public void deepMappingSimpleUnmapped() throws JsonProcessingException {
        // Arrange response
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();

        // Act
        AnnotatedTestPojo result = accessResp.deepMapping(AnnotatedTestPojo.class, accessResp.asJsonNode().get(0),
                mappingMap, 20);

        // Assert
        assertThat(result).isNotNull().isEqualToComparingFieldByField(pojo);
    }

    @Test
    public void deepMappingComplexUnmapped() throws JsonProcessingException {
        // Arrange response
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutLocatedPojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();

        // Act
        AnnotatedTestPojo result = accessResp.deepMapping(AnnotatedTestPojo.class, accessResp.asJsonNode().get(0),
                mappingMap, 20);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.testCoordinate).isNotNull().isEqualToComparingFieldByField(pojo.testCoordinate);
    }

    @Test
    public void deepMappingSimpleInputIsOutput() throws JsonProcessingException {
        // Arrange response
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();
        ObjectMember statusField = AnnotatedTestPojo.createStatusMember();
        ObjectMember distanceField = AnnotatedTestPojo.createDistanceMember();
        putMappingTuple(accessResp, mappingMap, statusField.getName(), statusField);
        putMappingTuple(accessResp, mappingMap, distanceField.getName(), distanceField);

        // Act
        AnnotatedTestPojo result = accessResp.deepMapping(AnnotatedTestPojo.class, accessResp.asJsonNode().get(0),
                mappingMap, 20);

        // Assert
        assertThat(result).isNotNull().isEqualToComparingFieldByField(pojo);
    }

    @Test
    public void deepMappingComplexInputIsOutput() throws JsonProcessingException {
        // Arrange response
        AnnotatedTestPojo pojo = AnnotatedTestPojo.filloutLocatedPojo(new AnnotatedTestPojo());
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();
        ObjectMember statusField = AnnotatedTestPojo.createStatusMember();
        ObjectMember distanceField = AnnotatedTestPojo.createDistanceMember();
        ObjectMember coordinatesField = AnnotatedTestPojo.createCoordinatesMember();
        putMappingTuple(accessResp, mappingMap, statusField.getName(), statusField);
        putMappingTuple(accessResp, mappingMap, distanceField.getName(), distanceField);
        putMappingTuple(accessResp, mappingMap, coordinatesField.getName(), coordinatesField);

        // Act
        AnnotatedTestPojo result = accessResp.deepMapping(AnnotatedTestPojo.class, accessResp.asJsonNode().get(0),
                mappingMap, 20);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.testCoordinate).isNotNull().isEqualToComparingFieldByField(pojo.testCoordinate);
    }

    @Test
    public void deepMappingSimpleResponseToPojoMapped() throws JsonProcessingException {

        // Arrange response

        OfferingDescription offeringDescription = mock(OfferingDescription.class);

        String responseJson = "[{\"schema:geoCoordinates\":null,\"datex:distanceFromParkingSpace\":200.0,\"datex:parkingSpaceStatus\":\"valid\"}]";
        AccessResponse accessResp = new AccessResponse(responseJson, offeringDescription);
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();
        putMappingTuple(accessResp, mappingMap, "datex:parkingSpaceStatus", AnnotatedTestPojo.createStatusMember());
        putMappingTuple(accessResp, mappingMap, "datex:distanceFromParkingSpace",
                AnnotatedTestPojo.createDistanceMember());

        // Act
        AnnotatedTestPojo result = accessResp.deepMapping(AnnotatedTestPojo.class, accessResp.asJsonNode().get(0),
                mappingMap, 20);

        // Assert
        assertThat(result).isNotNull()
                .isEqualToComparingFieldByField(AnnotatedTestPojo.filloutSimplePojo(new AnnotatedTestPojo()));
    }

    @Test
    public void deepMappingComplexResponseToPojoMapped() throws JsonProcessingException {
        // Arrange response
        String responseJson = "[{\"schema:geoCoordinates\":{\"latitude\":49.9,\"longitude\":9.99},\"datex:distanceFromParkingSpace\":200.0,\"datex:parkingSpaceStatus\":\"valid\"}]";
        AccessResponse accessResp = new AccessResponse(responseJson, mock(OfferingDescription.class));
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();
        putMappingTuple(accessResp, mappingMap, "datex:parkingSpaceStatus", AnnotatedTestPojo.createStatusMember());
        putMappingTuple(accessResp, mappingMap, "datex:distanceFromParkingSpace",
                AnnotatedTestPojo.createDistanceMember());
        putMappingTuple(accessResp, mappingMap, "schema:geoCoordinates", AnnotatedTestPojo.createCoordinatesMember());

        // Act
        AnnotatedTestPojo result = accessResp.deepMapping(AnnotatedTestPojo.class, accessResp.asJsonNode().get(0),
                mappingMap, 20);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.testCoordinate).isNotNull().isEqualToComparingFieldByField(
                AnnotatedTestPojo.filloutLocatedPojo(new AnnotatedTestPojo()).testCoordinate);
    }

    @Test
    public void deepMappingNoSuchMethodException() throws JsonProcessingException {
        // Arrange response
        AccessResponse accessResp = new AccessResponse("[]", mock(OfferingDescription.class));

        // Act
        NoSuchMethodExceptionCauser result = accessResp.deepMapping(NoSuchMethodExceptionCauser.class, null, null, 20);

        // Assert
        assertThat(result).isNull(); // exception is silently consumed and null returned instead.
    }

    @Test
    public void deepMappingArrayNotSupportedException() throws JsonProcessingException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Arrays not yet supported for mapping");

        // Arrange response
        String json = jsonMapper.writerFor(List.class)
                .writeValueAsString(Arrays.asList(TestPojo.filloutGeosPojo(new TestPojo())));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        // Arrange mapping
        Map<String, AccessResponse.MappingIoDataTuple> mappingMap = new HashMap<>();

        // Act
        accessResp.deepMapping(TestPojo.class, accessResp.asJsonNode().get(0), mappingMap, 20);

        // Assert
        // see expectedException rule
    }

    @Test
    public void retrieveAnnotationBasedOutputMappingsNone() {
        // Act
        OutputMapping result = AccessResponse.retrieveAnnotationBasedOutputMappings(TestPojo.class);

        // Assert
        assertThat(result.getList()).isEmpty();
    }

    @Test
    public void retrieveAnnotationBasedOutputMappingsWith() {
        // Act
        OutputMapping result = AccessResponse.retrieveAnnotationBasedOutputMappings(AnnotatedTestPojo.class);

        // Assert
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    public void mapSimpleTypeMultiSuccess() throws JsonProcessingException, NoSuchFieldException, SecurityException {
        // Arrange json
        BunchOfTypes pojo = new BunchOfTypes();
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        JsonNode node = accessResp.asJsonNode().get(0);

        // Act
        Object mappedBoolean = AccessResponse.mapSimpleType(node.findValue("someBooleanValue"),
                BunchOfTypes.class.getField("someBooleanValue").getType());
        Object mappedBooleanObject = AccessResponse.mapSimpleType(node.findValue("someBooleanObjectValue"),
                BunchOfTypes.class.getField("someBooleanObjectValue").getType());
        Object mappedFloat = AccessResponse.mapSimpleType(node.findValue("someFloatValue"),
                BunchOfTypes.class.getField("someFloatValue").getType());
        Object mappedFloatObject = AccessResponse.mapSimpleType(node.findValue("someFloatObjectValue"),
                BunchOfTypes.class.getField("someFloatObjectValue").getType());
        Object mappedDouble = AccessResponse.mapSimpleType(node.findValue("someDoubleValue"),
                BunchOfTypes.class.getField("someDoubleValue").getType());
        Object mappedDoubleObject = AccessResponse.mapSimpleType(node.findValue("someDoubleObjectValue"),
                BunchOfTypes.class.getField("someDoubleObjectValue").getType());
        Object mappedByte = AccessResponse.mapSimpleType(node.findValue("someByteValue"),
                BunchOfTypes.class.getField("someByteValue").getType());
        Object mappedByteObject = AccessResponse.mapSimpleType(node.findValue("someByteObjectValue"),
                BunchOfTypes.class.getField("someByteObjectValue").getType());
        Object mappedShort = AccessResponse.mapSimpleType(node.findValue("someShortValue"),
                BunchOfTypes.class.getField("someShortValue").getType());
        Object mappedShortObject = AccessResponse.mapSimpleType(node.findValue("someShortObjectValue"),
                BunchOfTypes.class.getField("someShortObjectValue").getType());
        Object mappedInteger = AccessResponse.mapSimpleType(node.findValue("someIntegerValue"),
                BunchOfTypes.class.getField("someIntegerValue").getType());
        Object mappedIntegerObject = AccessResponse.mapSimpleType(node.findValue("someIntegerObjectValue"),
                BunchOfTypes.class.getField("someIntegerObjectValue").getType());
        Object mappedLong = AccessResponse.mapSimpleType(node.findValue("someLongValue"),
                BunchOfTypes.class.getField("someLongValue").getType());
        Object mappedLongObject = AccessResponse.mapSimpleType(node.findValue("someLongObjectValue"),
                BunchOfTypes.class.getField("someLongObjectValue").getType());
        Object mappedNullObject = AccessResponse.mapSimpleType(node.findValue("someNullValue"),
                BunchOfTypes.class.getField("someNullValue").getType());
        Object mappedJodaObject = AccessResponse.mapSimpleType(node.findValue("someJodaDateTimeValue"),
                BunchOfTypes.class.getField("someJodaDateTimeValue").getType());
        Object mappedDateObject = AccessResponse.mapSimpleType(node.findValue("someDateValue"),
                BunchOfTypes.class.getField("someDateValue").getType());

        // Assert
        assertThat(mappedBoolean).isInstanceOf(Boolean.class).isEqualTo(true);
        assertThat(mappedBooleanObject).isInstanceOf(Boolean.class).isEqualTo(Boolean.FALSE);
        assertThat(mappedFloat).isInstanceOf(Float.class).isEqualTo(1234.1234f);
        assertThat(mappedFloatObject).isInstanceOf(Float.class).isEqualTo(Float.valueOf(2345.2345f));
        assertThat(mappedDouble).isInstanceOf(Double.class).isEqualTo(12345.12345);
        assertThat(mappedDoubleObject).isInstanceOf(Double.class).isEqualTo(Double.valueOf(23456.23456));
        assertThat(mappedByte).isInstanceOf(Byte.class).isEqualTo((byte) 12);
        assertThat(mappedByteObject).isInstanceOf(Byte.class).isEqualTo(Byte.valueOf((byte) 23));
        assertThat(mappedShort).isInstanceOf(Short.class).isEqualTo((short) 123);
        assertThat(mappedShortObject).isInstanceOf(Short.class).isEqualTo(Short.valueOf((short) 234));
        assertThat(mappedInteger).isInstanceOf(Integer.class).isEqualTo(12345);
        assertThat(mappedIntegerObject).isInstanceOf(Integer.class).isEqualTo(23456);
        assertThat(mappedLong).isInstanceOf(Long.class).isEqualTo(1234567890l);
        assertThat(mappedLongObject).isInstanceOf(Long.class).isEqualTo(Long.MAX_VALUE);
        assertThat(mappedNullObject).isNull();
        assertThat(mappedJodaObject).isInstanceOf(org.joda.time.DateTime.class).isEqualTo(pojo.someJodaDateTimeValue);
        assertThat(mappedDateObject).isInstanceOf(java.util.Date.class).isEqualTo(new java.util.Date(1l));
    }

    @Test
    public void mapSimpleTypeInvalidTypeException()
            throws JsonProcessingException, NoSuchFieldException, SecurityException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Invalid value type");

        BunchOfTypes pojo = new BunchOfTypes();
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, mock(OfferingDescription.class));
        JsonNode node = accessResp.asJsonNode().get(0);

        AccessResponse.mapSimpleType(node.findValue("someDateValue"), TestPojo.class);

        // Assert
        // see expectedException rule
    }

    @Test
    public void mapSimpleTypeUnspecifiedTypeException()
            throws JsonProcessingException, NoSuchFieldException, SecurityException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Unspecified value type");

        OfferingDescription offeringDescription = mock(OfferingDescription.class);

        BunchOfTypes pojo = new BunchOfTypes();
        String json = jsonMapper.writerFor(List.class).writeValueAsString(Arrays.asList(pojo));
        AccessResponse accessResp = new AccessResponse(json, offeringDescription);
        JsonNode node = accessResp.asJsonNode().get(0);

        AccessResponse.mapSimpleType(node.findValue("noMappingSupported"),
                BunchOfTypes.class.getField("noMappingSupported").getType());

        // Assert
        // see expectedException rule
    }

    static Map<String, AccessResponse.MappingIoDataTuple> putMappingTuple(AccessResponse accessResp,
            Map<String, AccessResponse.MappingIoDataTuple> map, String key, ObjectMember dataForValue) {
        map.put(key, accessResp.new MappingIoDataTuple(
                new OutputMappingElement.Type(dataForValue.getRdfUri(), dataForValue.getName()), dataForValue));
        return map;
    }

    public static class BunchOfTypes {
        public boolean someBooleanValue = true;
        public Boolean someBooleanObjectValue = Boolean.FALSE;
        public float someFloatValue = 1234.1234f;
        public Float someFloatObjectValue = 2345.2345f;
        public double someDoubleValue = 12345.12345;
        public Double someDoubleObjectValue = 23456.23456;
        public byte someByteValue = 12;
        public Byte someByteObjectValue = 23;
        public short someShortValue = 123;
        public Short someShortObjectValue = 234;
        public int someIntegerValue = 12345;
        public Integer someIntegerObjectValue = 23456;
        public long someLongValue = 1234567890;
        public Long someLongObjectValue = Long.MAX_VALUE;
        public Object someNullValue = null;
        public org.joda.time.DateTime someJodaDateTimeValue = org.joda.time.DateTime.parse("2018-01-16T10:53:36.958Z");
        public java.util.Date someDateValue = new java.util.Date(1l);
        public Object noMappingSupported = new TestPojo();
    }

    public static class TestPojo {

        public static class Coordinate {
            public double latitude;
            public double longitude;

            @Override
            public String toString() {
                return "Coordinate [latitude=" + latitude + ", longitude=" + longitude + "]";
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                long temp;
                temp = Double.doubleToLongBits(latitude);
                result = prime * result + (int) (temp ^ (temp >>> 32));
                temp = Double.doubleToLongBits(longitude);
                result = prime * result + (int) (temp ^ (temp >>> 32));
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                Coordinate other = (Coordinate) obj;
                if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
                    return false;
                if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
                    return false;
                return true;
            }

        }

        public TestPojo.Coordinate testCoordinate;
        public TestPojo.Coordinate[] geos;
        public double testDistance;
        public String testStatus;

        @Override
        public String toString() {
            return "TestPojo [testCoordinate=" + testCoordinate + ", testDistance=" + testDistance + ", testStatus="
                    + testStatus + "]";
        }

        static <T extends TestPojo> T filloutSimplePojo(T pojo) {
            pojo.testDistance = 200.0;
            pojo.testStatus = "valid";
            return pojo;
        }

        static <T extends TestPojo> T filloutLocatedPojo(T pojo) {
            filloutSimplePojo(pojo);
            pojo.testCoordinate = new TestPojo.Coordinate();
            pojo.testCoordinate.latitude = 49.9;
            pojo.testCoordinate.longitude = 9.99;
            return pojo;
        }

        static <T extends TestPojo> T filloutGeosPojo(T pojo) {
            filloutSimplePojo(pojo);
            TestPojo.Coordinate geo1 = new TestPojo.Coordinate();
            geo1.latitude = 49.9;
            geo1.longitude = 9.99;
            TestPojo.Coordinate geo2 = new TestPojo.Coordinate();
            geo2.latitude = 48.8;
            geo2.longitude = 8.88;
            pojo.geos = new TestPojo.Coordinate[] { geo1, geo2 };
            return pojo;
        }
    }

    public static class AnnotatedTestPojo {

        @ResponseMappingType("schema:geoCoordinates")
        public TestPojo.Coordinate testCoordinate;
        @ResponseMappingType("datex:distanceFromParkingSpace")
        public double testDistance;
        @ResponseMappingType("datex:parkingSpaceStatus")
        public String testStatus;

        public static IOData createStatusTuple() {
            return new IOData("testStatus", new RDFType("datex:parkingSpaceStatus"), ValueType.TEXT);
        }

        public static ObjectParameter createComplexObject() {
            ObjectMember statusField = AnnotatedTestPojo.createStatusMember();
            ObjectMember distanceField = AnnotatedTestPojo.createDistanceMember();
            ObjectMember coordinatesField = AnnotatedTestPojo.createCoordinatesMember();

            return ObjectParameter.create().addMember(statusField).addMember(distanceField).addMember(coordinatesField);
        }

        public static ObjectMember createStatusMember() {
            return new ObjectMember("testStatus", "datex:parkingSpaceStatus", ValueType.TEXT,
                    LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE, true);
        }

        public static IOData createDistanceTuple() {
            return new IOData("testDistance", new RDFType("datex:distanceFromParkingSpace"), ValueType.NUMBER);
        }

        public static ObjectMember createDistanceMember() {
            return new ObjectMember("testDistance", "datex:distanceFromParkingSpace", ValueType.NUMBER,
                    LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE, true);
        }

        public static IOData createCoordinatesTuple() {
            return new IOData("testCoordinate", new RDFType("schema:geoCoordinates"));
        }

        public static ObjectMember createCoordinatesMember() {
            return new ObjectMember("testCoordinate", "schema:geoCoordinates",
                    ObjectParameter.create()
                            .addMember("latitude", "schema:latitude", new NumberParameter(-90.0, 90.0), true)
                            .addMember("longitude", "schema:longitude", new NumberParameter(-180.0, 180.0), true),
                    LibConfiguration.DEFAULT_PARAMETER_ENCODING_TYPE, true);
        }

        @Override
        public String toString() {
            return "AnnotatedTestPojo [testCoordinate=" + testCoordinate + ", testDistance=" + testDistance
                    + ", testStatus=" + testStatus + "]";
        }

        static AnnotatedTestPojo filloutSimplePojo(AnnotatedTestPojo pojo) {
            pojo.testDistance = 200.0;
            pojo.testStatus = "valid";
            // TestPojo.Coordinate coordinate = new TestPojo.Coordinate();
            // coordinate.latitude=47.99;
            // coordinate.longitude=12.01;
            // pojo.testCoordinate = coordinate;
            return pojo;
        }

        static AnnotatedTestPojo filloutLocatedPojo(AnnotatedTestPojo pojo) {
            filloutSimplePojo(pojo);
            pojo.testCoordinate = new TestPojo.Coordinate();
            pojo.testCoordinate.latitude = 49.9;
            pojo.testCoordinate.longitude = 9.99;
            return pojo;
        }
    }

    public static class NoSuchMethodExceptionCauser {
        private NoSuchMethodExceptionCauser(String nothing) {
            // private constructor causes exception
        }
    }

}
