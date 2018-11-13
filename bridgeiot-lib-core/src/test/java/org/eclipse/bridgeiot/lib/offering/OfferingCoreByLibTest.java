/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Bosch Software Innovations GmbH, Robert Bosch GmbH, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.bridgeiot.lib.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.eclipse.bridgeiot.lib.IConsumer;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonActivatedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.AccessToNonSubscribedOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalAccessParameterException;
import org.eclipse.bridgeiot.lib.exceptions.IllegalEndpointException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseFailureHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessResponseSuccessHandler;
import org.eclipse.bridgeiot.lib.misc.HttpClient;
import org.eclipse.bridgeiot.lib.model.Activation;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.offering.parameters.AccessParameters;
import org.eclipse.bridgeiot.lib.offering.parameters.ComplexParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.NumberParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.ObjectParameter;
import org.eclipse.bridgeiot.lib.offering.parameters.Parameter;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class OfferingCoreByLibTest {

    OfferingCoreByLib offeringUnderTest;

    public static final String superUniqueValue = "thisIsASuperUniqueStringThatCanBeUsedForMatchingValue";
    public static final String superUniqueKey = "thisIsASuperUniqueStringThatCanBeUsedForMatchingKey";

    public static final String supermanUrl = "http://superman.com";
    public static final String someAccessToken = "someAccessToken";
    public static final String someAccessSessionId = "0123456789";
    public static final String authorizationKey = "Authorization";
    public static final String accessSessionKey = "AccessSessionId";
    public static final String authorizationValuePart = "Bearer ";
    public static final String authorizationValueFixture = "Bearer " + someAccessToken;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    Map<String, Object> parametersMapMock;

    @Mock
    List<EndPoint> endpointsMock;

    @Before
    public void initialize() throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        SubscribableOfferingDescriptionCore offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, null);
        // Object Under Test
        offeringUnderTest = new OfferingCoreByLib(offeringDescriptionMock, someAccessToken);
    }

    @Test
    public void prepareUrlWithParametersEmptyParams() {
        // Act
        String nonDecoratedSupermanUrl = OfferingCoreByLib.prepareUrlWithParameters(supermanUrl, null);
        // Assert
        assertThat(nonDecoratedSupermanUrl).isEqualTo(supermanUrl);
    }

    @Test
    public void prepareUrlWithParametersWithParams() throws IllegalAccessParameterException {
        // Arrange
        ComplexParameter parameter = createInputParameters(superUniqueKey);

        // Act
        String nonDecoratedSupermanUrl = OfferingCoreByLib.prepareUrlWithParameters(supermanUrl,
                createAccessParamsFixture(superUniqueKey, superUniqueValue).toNameMap(parameter, 20));
        // Assert
        assertThat(nonDecoratedSupermanUrl).contains(supermanUrl).contains(superUniqueKey).contains(superUniqueValue);
    }

    @Test
    public void prepareHeaderWithAccessTokenNonLibCaller() {
        // Act
        Map<String, String> noHeaders = OfferingCoreByLib.prepareHeaderWithAccessToken(AccessInterfaceType.EXTERNAL,
                someAccessToken, null);
        // Assert
        assertThat(noHeaders).isNull();
    }

    @Test
    public void prepareHeaderWithAccessTokenLibCaller() {
        // Act
        Map<String, String> noHeaders = OfferingCoreByLib
                .prepareHeaderWithAccessToken(AccessInterfaceType.BRIDGEIOT_LIB, someAccessToken, someAccessSessionId);
        // Assert
        assertThat(noHeaders).isNotNull().containsEntry(authorizationKey, authorizationValueFixture);
        assertThat(noHeaders).isNotNull().containsEntry(accessSessionKey, someAccessSessionId);
    }

    @Test(expected = IncompleteOfferingDescriptionException.class)
    public void createHttpClientNullEndpointsIncompleteException()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescriptionMock.getEndpoints()).thenReturn(null);

        // Act
        OfferingCoreByLib.createHttpClient(offeringDescriptionMock, null);

        // Assert
        // see expect Annotation
    }

    @Test(expected = IncompleteOfferingDescriptionException.class)
    public void createHttpClientEmptyEndpointsIncompleteException()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescriptionMock.getEndpoints()).thenReturn(new ArrayList<EndPoint>());

        // Act
        OfferingCoreByLib.createHttpClient(offeringDescriptionMock, null);

        // Assert
        // see expect Annotation
    }

    @Test(expected = IllegalEndpointException.class)
    public void createHttpClientIllegalEndpointException()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescriptionMock.getEndpoints()).thenReturn(Arrays.asList(new EndPoint(EndpointType.COAP_GET,
                AccessInterfaceType.BRIDGEIOT_LIB, "nonHttp://invalid.protocol.com/endpoint")));

        // Act
        OfferingCoreByLib.createHttpClient(offeringDescriptionMock, null);

        // Assert
        // see expect Annotation
    }

    @Test
    public void createHttpClientHttpEndpoint() throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        EndPoint endPointSpy = spy(
                new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB, "http://some.url.com/endpoint"));
        when(offeringDescriptionMock.getEndpoints()).thenReturn(Arrays.asList(endPointSpy));

        // Act
        HttpClient httpClient = OfferingCoreByLib.createHttpClient(offeringDescriptionMock, null);

        // Assert
        assertThat(httpClient).isNotNull();
        assertThat(verify(endPointSpy, times(1)).isSecured()).isFalse();
        assertThat(endPointSpy.getEndpointType().isHTTP()).isTrue();
    }

    @Test
    public void createHttpClientHttpsEndpointDefaultPem()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        EndPoint endPointSpy = spy(new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                "https://some.url.com/endpoint"));
        when(offeringDescriptionMock.getEndpoints()).thenReturn(Arrays.asList(endPointSpy));

        // Act
        HttpClient httpClient = OfferingCoreByLib.createHttpClient(offeringDescriptionMock, null);

        // Assert
        assertThat(httpClient).isNotNull();
        assertThat(verify(endPointSpy, times(1)).isSecured()).isFalse();
        assertThat(endPointSpy.getEndpointType().isHTTP()).isTrue();
    }

    @Test
    public void createHttpClientHttpsEndpointCustomPemCertNotFound()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Setup expected exception rule
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Keyfile not found");
        expectedException.expectCause(Matchers.isA(FileNotFoundException.class));

        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescriptionMock.getEndpoints()).thenReturn(Arrays.asList(new EndPoint(EndpointType.HTTP_GET,
                AccessInterfaceType.BRIDGEIOT_LIB, "https://some.url.com/endpoint")));

        // Act
        HttpClient httpClient = OfferingCoreByLib.createHttpClient(offeringDescriptionMock, "someInvalidFileName");

        // Assert
        // see expectedException Rule
        assertThat(httpClient).isNull();
    }

    @Test
    public void createHttpClientHttpsEndpointCustomPemCertSuccess()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException {
        // Arrange
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        EndPoint endPointSpy = spy(new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                "https://some.url.com/endpoint"));
        when(offeringDescriptionMock.getEndpoints()).thenReturn(Arrays.asList(endPointSpy));

        // Act
        HttpClient httpClient = OfferingCoreByLib.createHttpClient(offeringDescriptionMock,
                "src/main/resources/keystore/bigiot-lib-cert.pem");

        // Assert
        assertThat(httpClient).isNotNull();
        assertThat(verify(endPointSpy, times(1)).isSecured()).isFalse();
        assertThat(endPointSpy.getEndpointType().isHTTP()).isTrue();
    }

    @Test(expected = AccessToNonActivatedOfferingException.class)
    public void validateOfferingDescriptionIsNull() throws AccessToNonActivatedOfferingException {
        // Arrange
        // nothing to do

        // Act
        OfferingCoreByLib.validate(null);

        // Assert
        // see expect Annotation
    }

    @Test(expected = AccessToNonActivatedOfferingException.class)
    public void validateOfferingDescriptionActivationIsNull() throws AccessToNonActivatedOfferingException {
        // Arrange
        OfferingDescription offeringDescriptionMock = mock(OfferingDescription.class);
        when(offeringDescriptionMock.getActivation()).thenReturn(null);

        // Act
        OfferingCoreByLib.validate(offeringDescriptionMock);

        // Assert
        // see expect Annotation
    }

    @Test(expected = AccessToNonActivatedOfferingException.class)
    public void validateOfferingDescriptionActivationStatusIsFalse() throws AccessToNonActivatedOfferingException {
        // Arrange
        Activation activationMock = mock(Activation.class);
        when(activationMock.getStatus()).thenReturn(Boolean.FALSE);
        OfferingDescription offeringDescriptionMock = mock(OfferingDescription.class);
        when(offeringDescriptionMock.getActivation()).thenReturn(activationMock);

        // Act
        OfferingCoreByLib.validate(offeringDescriptionMock);

        // Assert
        // see expect Annotation
    }

    @Test
    public void validateOfferingDescriptionSuccess() throws AccessToNonActivatedOfferingException {
        // Arrange
        Activation activationMock = mock(Activation.class);
        when(activationMock.getStatus()).thenReturn(Boolean.TRUE);
        OfferingDescription offeringDescriptionMock = mock(OfferingDescription.class);
        when(offeringDescriptionMock.getActivation()).thenReturn(activationMock);

        // Act
        OfferingCoreByLib.validate(offeringDescriptionMock);

        // Assert
        // nothing to do
    }

    @Test
    public void accessOneTimeInternalParameterMappingFails() throws IllegalAccessParameterException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = mock(OfferingDescription.class);
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Arrange AccessParameters
        AccessParameters accessParametersMock = mock(AccessParameters.class);
        when(accessParametersMock.toNameMap(ArgumentMatchers.<ObjectParameter>any(), anyInt()))
                .thenThrow(IllegalAccessParameterException.class);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParametersMock, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).isNotNull().isEmpty();
        verify(offeringDescriptionMock, times(1)).getEndpoints();
    }

    @Test
    public void accessOneTimeInternalNoEndpoint() throws IllegalAccessParameterException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = mock(OfferingDescription.class);
        when(endpointsMock.isEmpty()).thenReturn(true);
        when(offeringDescriptionMock.getEndpoints()).thenReturn(endpointsMock);
        // Arrange AccessParameters
        AccessParameters accessParametersMock = createAccessParamsMock(parametersMapMock);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(mock(HttpClient.class), offeringDescriptionMock,
                accessParametersMock, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).isNotNull().isEmpty();
        verify(offeringDescriptionMock, times(1)).getEndpoints();
        verify(endpointsMock, times(1)).isEmpty();
        verify(endpointsMock, never()).get(0);
        assertThat(response).isNotNull().isEmpty();
    }

    @Test
    public void accessOneTimeInternalValidationFails() throws IllegalAccessParameterException, IOException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = createOfferingDescriptionMock(Boolean.FALSE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, null);
        EndPoint endpointMock = mock(EndPoint.class);
        when(offeringDescriptionMock.getEndpoints()).thenReturn(Arrays.asList(endpointMock));
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Arrange AccessParameters
        AccessParameters accessParametersMock = createAccessParamsMock(parametersMapMock);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParametersMock, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).isEmpty();
        verify(endpointMock, never()).getUri();
    }

    @Test
    public void accessOneTimeInternalInvalidUri() throws IllegalAccessParameterException, IOException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.EXTERNAL, EndpointType.HTTP_GET, "^thisIsNot^_valid_Uri", null);
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Arrange AccessParameters
        AccessParameters accessParametersMock = createAccessParamsMock(parametersMapMock);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParametersMock, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).isEmpty();
        verify(parametersMapMock, never()).isEmpty();
        verify(offeringDescriptionMock, never()).getAccessInterfaceType();
    }

    @Test
    public void accessOneTimeInternalWithAccessParams() throws IllegalAccessParameterException, IOException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, superUniqueKey);
        // Arrange HttpClient
        Response respMockKeyMatches = createResponseMock(superUniqueValue, Boolean.TRUE);
        HttpClient httpClientMock = mock(HttpClient.class);
        when(httpClientMock.get(contains(superUniqueKey), ArgumentMatchers.<Map<String, String>>any()))
                .thenReturn(respMockKeyMatches);
        // Arrange AccessParameters
        AccessParameters accessParameters = createAccessParamsFixture(superUniqueKey, superUniqueValue);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParameters, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).contains(superUniqueValue).doesNotContain("trivial");
        verify(offeringDescriptionMock, times(1)).getEndpoints();
        verify(httpClientMock, times(1)).get(contains(superUniqueKey),
                (Map<String, String>) argThat(Matchers.hasEntry(authorizationKey, authorizationValueFixture)));
    }

    @Test
    public void accessOneTimeInternalSkipAuthorizationHeaders() throws IllegalAccessParameterException, IOException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.EXTERNAL, EndpointType.HTTP_GET, supermanUrl, superUniqueKey);
        // Arrange HttpClient
        Response respMockKeyMatches = createResponseMock(superUniqueValue, Boolean.TRUE);
        HttpClient httpClientMock = mock(HttpClient.class);
        when(httpClientMock.get(contains(superUniqueKey), ArgumentMatchers.<Map<String, String>>any()))
                .thenReturn(respMockKeyMatches);
        // Arrange AccessParameters
        AccessParameters accessParameters = createAccessParamsFixture(superUniqueKey, superUniqueValue);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParameters, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).contains(superUniqueValue);
        verify(offeringDescriptionMock, times(1)).getEndpoints();
        verify(httpClientMock, never()).get(contains(superUniqueKey),
                (Map<String, String>) argThat(Matchers.hasEntry(authorizationKey, authorizationValueFixture)));
    }

    @Test
    public void accessOneTimeInternalHttpGetFailed() throws IllegalAccessParameterException, IOException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.EXTERNAL, EndpointType.HTTP_GET, supermanUrl, null);
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        when(httpClientMock.get(anyString(), ArgumentMatchers.<Map<String, String>>any())).thenThrow(IOException.class);
        // Arrange AccessParameters
        AccessParameters accessParameters = createAccessParamsFixture(superUniqueKey, superUniqueValue);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParameters, someAccessSessionId, someAccessToken);

        // Assert
        verify(httpClientMock, times(1)).get(anyString(), nullable(Map.class));
        assertThat(response).isEmpty();
    }

    @Test
    public void accessOneTimeInternalSuccess() throws IllegalAccessParameterException, IOException {
        // Arrange OfferingDescription
        OfferingDescription offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, null);
        // Arrange HttpClient
        Response respMock = createResponseMock("someResponseBody", Boolean.TRUE);
        HttpClient httpClientMock = mock(HttpClient.class);
        when(httpClientMock.get(anyString(), ArgumentMatchers.<Map<String, String>>any())).thenReturn(respMock);
        // Arrange AccessParameters
        AccessParameters accessParametersMock = mock(AccessParameters.class);
        when(accessParametersMock.toNameMap(ArgumentMatchers.<Parameter>any(), anyInt())).thenReturn(parametersMapMock);

        // Act
        String response = OfferingCoreByLib.accessOneTimeInternal(httpClientMock, offeringDescriptionMock,
                accessParametersMock, someAccessSessionId, someAccessToken);

        // Assert
        assertThat(response).contains("someResponseBody");
        verify(offeringDescriptionMock, times(1)).getEndpoints();
        verify(parametersMapMock, times(1)).isEmpty();
    }

    @Test(expected = IllegalAccessParameterException.class)
    public void accessOneTimeParameterMappingFails()
            throws IllegalEndpointException, IncompleteOfferingDescriptionException, IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        // Arrange AccessParameters
        AccessParameters accessParametersMock = mock(AccessParameters.class);
        when(accessParametersMock.toNameMap(ArgumentMatchers.<Parameter>any(), anyInt()))
                .thenThrow(IllegalAccessParameterException.class);

        // Act
        offeringUnderTest.accessOneTime(accessParametersMock, null, null);

        // Assert
        // see expected exception
    }

    @Test(expected = AccessToNonActivatedOfferingException.class)
    public void accessOneTimeNoEndpoint()
            throws IllegalAccessParameterException, IllegalEndpointException, IncompleteOfferingDescriptionException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        // Object Under Test: clear list
        when(offeringUnderTest.getOfferingDescription().getEndpoints()).thenReturn(new ArrayList<EndPoint>());

        // Act
        offeringUnderTest.accessOneTime(createAccessParamsMock(parametersMapMock), null, null);

        // Assert
        // see expected exception
    }

    @Test(expected = AccessToNonActivatedOfferingException.class)
    public void accessOneTimeValidationFails() throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        // Object Under Test: deactivate offering
        when(offeringUnderTest.getOfferingDescription().getActivation().getStatus()).thenReturn(Boolean.FALSE);

        // Act
        offeringUnderTest.accessOneTime(createAccessParamsMock(parametersMapMock), null, null);

        // Assert
        // see expected exception
    }

    @Test
    public void accessOneTimeInvalidUri() throws IllegalAccessParameterException, IOException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        // Setup expected exception rule
        expectedException.expect(AccessToNonActivatedOfferingException.class);
        expectedException.expectCause(Matchers.isA(URISyntaxException.class));

        // Object Under Test: replace endpoint with an invalid one
        when(offeringUnderTest.getOfferingDescription().getEndpoints()).thenReturn(Arrays
                .asList(new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, "^thisIsNot^_valid_Uri")));

        // Act
        offeringUnderTest.accessOneTime(createAccessParamsMock(parametersMapMock), null, null);

        // Assert
        // see expectedException rule
    }

    @Test
    public void accessOneTimeWithAccessParams() throws IllegalAccessParameterException, IOException,
            IllegalEndpointException, IncompleteOfferingDescriptionException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException {

        // Arrange OfferingDescription
        SubscribableOfferingDescriptionCore offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, superUniqueKey);

        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Object Under Test: setup with mocked httpClient
        OfferingCoreByLib oUnderTest = new OfferingCoreByLib(offeringDescriptionMock, someAccessToken, httpClientMock);

        // Act
        oUnderTest.accessOneTime(createAccessParamsFixture(superUniqueKey, superUniqueValue), null, null);

        // Assert
        verify(httpClientMock, times(1)).get(contains(superUniqueKey),
                (Map<String, String>) argThat(Matchers.hasEntry(authorizationKey, authorizationValueFixture)),
                any(Callback.class));
    }

    private static ComplexParameter createInputParameters(String specialKey) {

        ObjectParameter geoCoordinates = ObjectParameter.create()
                .addMember("latitude", "schema:latitude", new NumberParameter(-90.0, 90.0), true)
                .addMember("longitude", "schema:longitude", new NumberParameter(-180.0, 180.0), true);
        if (specialKey != null) {
            geoCoordinates.addMember(specialKey, "schema:specialKeyValue", ValueType.TEXT);
        }
        ObjectParameter objectParameter = ObjectParameter.create().addMember("areaSpecification",
                "schema:areaSpecification",
                ObjectParameter.create().addMember("geoCoordinates", "schema:geoCoordinates", geoCoordinates)
                        .addMember("radius", "schema:radius", ValueType.NUMBER));
        return objectParameter;
    }

    @Test
    public void accessOneTimeCallbackOnResponseSuccess()
            throws IllegalAccessParameterException, AccessToNonActivatedOfferingException,
            AccessToNonSubscribedOfferingException, InterruptedException, ExecutionException {
        // Arrange OfferingDescription
        SubscribableOfferingDescriptionCore offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, superUniqueKey);
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Arrange Success Handler
        AccessResponseSuccessHandler successHandler = mock(AccessResponseSuccessHandler.class);
        // Arrange Callback
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws IOException {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 2 && arguments[2] != null) {
                    Callback callback = (Callback) arguments[2];
                    callback.onResponse(null,
                            createResponseMock("{\"someResponseBody\":\"with value\"}", Boolean.TRUE));
                }
                return null;
            }
        }).when(httpClientMock).get(contains(superUniqueKey),
                (Map<String, String>) argThat(Matchers.hasEntry(authorizationKey, authorizationValueFixture)),
                any(Callback.class));
        // Object Under Test: setup with mocked httpClient
        OfferingCoreByLib oUnderTest = new OfferingCoreByLib(offeringDescriptionMock, someAccessToken, httpClientMock);

        // Act
        oUnderTest.accessOneTime(createAccessParamsFixture(superUniqueKey, superUniqueValue), successHandler, null);

        // Assert
        verify(successHandler, times(1)).processResponseOnSuccess(any(IOfferingCore.class), any(AccessResponse.class));
    }

    @Test
    public void accessOneTimeCallbackOnResponseFailure() throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {

        // Fails because of unsed Mocks
        // 14:40:08.206 INFO org.eclipse.bridgeiot.lib.misc.BridgeIotProperties - The Lib configuration is read from a
        // custom properties file: (org.eclipse.bridgeiot.lib.custom_configuration)
        // [MockitoHint] OfferingCoreByLibTest.accessOneTimeCallbackOnResponseFailure (see javadoc for MockitoHint):
        // [MockitoHint] 1. Unused... -> at
        // org.eclipse.bridgeiot.lib.offering.OfferingCoreByLibTest.accessOneTimeCallbackOnResponseFailure(OfferingCoreByLibTest.java:630)
        // [MockitoHint] ...args ok? -> at
        // org.eclipse.bridgeiot.lib.offering.OfferingCoreByLib.accessOneTimeWithSessionId(OfferingCoreByLib.java:271)

        // Arrange OfferingDescription
        SubscribableOfferingDescriptionCore offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, superUniqueKey);
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Arrange Failure Handler
        AccessResponseFailureHandler failureHandler = mock(AccessResponseFailureHandler.class);
        // Arrange Callback
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws IOException {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 2 && arguments[2] != null) {
                    Callback callback = (Callback) arguments[2];
                    callback.onResponse(null, createResponseMock("someResponseBody", Boolean.FALSE));
                }
                return null;
            }
        }).when(httpClientMock).get(contains(superUniqueKey),
                (Map<String, String>) argThat(Matchers.hasEntry(authorizationKey, authorizationValueFixture)),
                any(Callback.class));
        // Object Under Test: setup with mocked httpClient
        OfferingCoreByLib oUnderTest = new OfferingCoreByLib(offeringDescriptionMock, someAccessToken, httpClientMock);

        // Act
        oUnderTest.accessOneTime(createAccessParamsFixture(superUniqueKey, superUniqueValue), null, failureHandler);

        // Assert
        verify(failureHandler, times(1)).processResponseOnFailure(any(IOfferingCore.class), any(AccessResponse.class));
    }

    @Test
    public void accessOneTimeCallbackOnFailure() throws IllegalAccessParameterException,
            AccessToNonActivatedOfferingException, AccessToNonSubscribedOfferingException {
        // Arrange OfferingDescription
        SubscribableOfferingDescriptionCore offeringDescriptionMock = createOfferingDescriptionMock(Boolean.TRUE,
                AccessInterfaceType.BRIDGEIOT_LIB, EndpointType.HTTP_GET, supermanUrl, superUniqueKey);
        // Arrange HttpClient
        HttpClient httpClientMock = mock(HttpClient.class);
        // Arrange Failure Handler
        AccessResponseFailureHandler failureHandler = mock(AccessResponseFailureHandler.class);
        // Arrange Callback
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws IOException {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 2 && arguments[2] != null) {
                    Callback callback = (Callback) arguments[2];
                    callback.onFailure(null, new IOException());
                }
                return null;
            }
        }).when(httpClientMock).get(contains(superUniqueKey),
                (Map<String, String>) argThat(Matchers.hasEntry(authorizationKey, authorizationValueFixture)),
                any(Callback.class));
        // Object Under Test: setup with mocked httpClient
        OfferingCoreByLib oUnderTest = new OfferingCoreByLib(offeringDescriptionMock, someAccessToken, httpClientMock);

        // Act
        oUnderTest.accessOneTime(createAccessParamsFixture(superUniqueKey, superUniqueValue), null, failureHandler);

        // Assert
        verify(failureHandler, times(1)).processResponseOnFailure(any(IOfferingCore.class), any(AccessResponse.class));
    }

    private static Response createResponseMock(String body, boolean isSuccessful) throws IOException {
        Response respMock = mock(Response.class);
        ResponseBody bodyMock = mock(ResponseBody.class);
        when(bodyMock.string()).thenReturn(body);
        when(respMock.body()).thenReturn(bodyMock);
        when(respMock.message()).thenReturn("{}");
        when(respMock.isSuccessful()).thenReturn(isSuccessful);

        return respMock;
    }

    private static SubscribableOfferingDescriptionCore createOfferingDescriptionMock(boolean isActivated,
            AccessInterfaceType accessType, EndpointType endpointType, String uri, String superUniqueKey) {
        Activation activationMock = mock(Activation.class);
        when(activationMock.getStatus()).thenReturn(isActivated);
        IConsumer consumerMock = mock(IConsumer.class);
        SubscribableOfferingDescriptionCore offeringDescriptionMock = mock(SubscribableOfferingDescriptionCore.class);
        when(offeringDescriptionMock.getActivation()).thenReturn(activationMock);
        when(offeringDescriptionMock.getAccessInterfaceType()).thenReturn(accessType);
        when(offeringDescriptionMock.getEndpoints())
                .thenReturn(Arrays.asList(new EndPoint(endpointType, accessType, uri)));
        when(offeringDescriptionMock.getConsumer()).thenReturn(consumerMock);
        when(offeringDescriptionMock.getInputs()).thenReturn(createInputParameters(superUniqueKey));
        return offeringDescriptionMock;
    }

    private static AccessParameters createAccessParamsMock(Map<String, Object> parametersMapMock)
            throws IllegalAccessParameterException {
        AccessParameters accessParametersMock = mock(AccessParameters.class);
        when(accessParametersMock.toNameMap(ArgumentMatchers.<ObjectParameter>any(), anyInt()))
                .thenReturn(parametersMapMock);
        return accessParametersMock;
    }

    private static AccessParameters createAccessParamsFixture(String key, String value) {
        return AccessParameters.create().addNameValue("areaSpecification",
                AccessParameters.create()
                        .addNameValue("geoCoordinates",
                                AccessParameters.create().addNameValue("latitude", 50.22)
                                        .addNameValue("longitude", 8.11).addNameValue(key, value))
                        .addNameValue("radius", 777));
    }
}
