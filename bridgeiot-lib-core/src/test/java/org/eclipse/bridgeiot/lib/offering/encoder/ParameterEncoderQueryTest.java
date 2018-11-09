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
package org.eclipse.bridgeiot.lib.offering.encoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.HttpUrl;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(MockitoJUnitRunner.class)
public class ParameterEncoderQueryTest {

    ParameterEncoderQuery encoderUnderTest;

    @Mock
    Map<String, Object> parametersMapMock;

    @Mock
    Set<String> keySetMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void initialize() {
        encoderUnderTest = new ParameterEncoderQuery();
    }

    @Test
    public void encodeFaultyType() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Encode has to be called with a map");

        // Act
        encoderUnderTest.encode("");

        // Assert
        // see expectedException rule
    }

    @Test
    public void encodeEmptyMap() {
        // Arrange
        when(keySetMock.isEmpty()).thenReturn(true);
        when(parametersMapMock.keySet()).thenReturn(keySetMock);

        // Act
        encoderUnderTest.encode(parametersMapMock);

        // Assert
        verify(keySetMock, times(1)).isEmpty();
        verify(keySetMock, never()).iterator();
    }

    @Test
    public void encodeFaultyKeyType() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage(
                "Map encoded access parameters should have String typed keys. Disable this exception and try if the code still works");

        // Arrange
        Map<Integer, Object> faultyKeyTypeMap = new HashMap<>();
        faultyKeyTypeMap.put(23, Arrays.asList(22, 23, 24));

        // Act
        encoderUnderTest.encode(faultyKeyTypeMap);

        // Assert
        // see expectedException rule
    }

    @Test
    public void encodeNonComplexValueTypeList() {
        // Arrange
        HttpUrl.Builder urlBuilderMock = mock(HttpUrl.Builder.class);
        Map<String, Object> listComplexValueTypeMap = new HashMap<>();
        listComplexValueTypeMap.put("666", "nonComplex");

        // Act
        String result = ParameterEncoderQuery.encodeQuery(listComplexValueTypeMap, urlBuilderMock);

        // Assert
        verify(urlBuilderMock, never()).addQueryParameter(eq(Constants.COMPLEX_PARAMETER_KEY), anyString());
        assertThat(result).contains("666").contains("nonComplex");
    }

    @Test
    public void encodeIsComplexValueTypeList() {
        // Arrange
        HttpUrl httpUrlMock = mock(HttpUrl.class);
        HttpUrl.Builder urlBuilderMock = mock(HttpUrl.Builder.class);
        when(urlBuilderMock.build()).thenReturn(httpUrlMock);
        Map<String, Object> listComplexValueTypeMap = new HashMap<>();
        listComplexValueTypeMap.put("23", Arrays.asList(22, 23, 24));

        // Act
        ParameterEncoderQuery.encodeQuery(listComplexValueTypeMap, urlBuilderMock);

        // Assert
        verify(urlBuilderMock, times(1)).addQueryParameter(eq(Constants.COMPLEX_PARAMETER_KEY), anyString());
    }

    @Test
    public void encodeComplexValueTypeCausesJsonParseException() {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Canot serialize access parameters");
        expectedException.expectCause(isA(JsonProcessingException.class));
        // nasty object that causes exception
        Object mockItem = mock(Object.class);
        // Arrange
        HttpUrl.Builder urlBuilderMock = mock(HttpUrl.Builder.class);
        Map<String, Object> listComplexValueTypeMap = new HashMap<>();
        listComplexValueTypeMap.put("23", Arrays.asList(mockItem, mockItem, mockItem));

        // Act
        ParameterEncoderQuery.encodeQuery(listComplexValueTypeMap, urlBuilderMock);

        // Assert
        verify(urlBuilderMock, times(1)).addQueryParameter(eq(Constants.COMPLEX_PARAMETER_KEY), anyString());
    }

    @Test
    public void encodeIsComplexValueTypeListSpyedBuilder() {
        // Arrange
        HttpUrl.Builder urlBuilderSpy = spy(new HttpUrl.Builder().scheme("http").host("localhost"));
        Map<String, Object> listComplexValueTypeMap = new HashMap<>();
        listComplexValueTypeMap.put("23", new int[] { 32, 33, 34 });

        // Act
        String result = ParameterEncoderQuery.encodeQuery(listComplexValueTypeMap, urlBuilderSpy);

        // Assert
        verify(urlBuilderSpy, times(1)).addQueryParameter(eq(Constants.COMPLEX_PARAMETER_KEY), anyString());
        assertThat(result).contains("23").contains("32").contains("33").contains("34");
    }

    @Test
    public void encodeWithFilledMapComplexValueType() {
        // Arrange
        HttpUrl.Builder urlBuilderSpy = spy(new HttpUrl.Builder().scheme("http").host("localhost"));
        Map<String, Object> listComplexValueTypeMap = new HashMap<>();
        listComplexValueTypeMap.put("1", new Integer[] { 32, 33, 34 });
        listComplexValueTypeMap.put("2", new Integer[] { 42, 43, 44 });
        listComplexValueTypeMap.put("3", new Integer[] { 52, 53, 54 });
        listComplexValueTypeMap.put("4", new Integer[] { 62, 63, 64 });

        // Act
        String result = ParameterEncoderQuery.encodeQuery(listComplexValueTypeMap, urlBuilderSpy);

        // Assert
        verify(urlBuilderSpy, times(1)).addQueryParameter(eq(Constants.COMPLEX_PARAMETER_KEY), anyString());
        assertThat(result).contains("1").contains("42").contains("53").contains("64");
    }

    @Test
    public void encodeWithComplexValueTypeMap() {
        // some map
        Map<Object, Object> filledMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            filledMap.put(String.valueOf(i + 100), i + 200);
        }
        // Arrange
        HttpUrl.Builder urlBuilderSpy = spy(new HttpUrl.Builder().scheme("http").host("localhost"));
        Map<String, Object> listComplexValueTypeMap = new HashMap<>();
        listComplexValueTypeMap.put("count", new HashMap<Object, Object>());
        listComplexValueTypeMap.put("more", filledMap);

        // Act
        String result = ParameterEncoderQuery.encodeQuery(listComplexValueTypeMap, urlBuilderSpy);

        // Assert
        verify(urlBuilderSpy, times(1)).addQueryParameter(eq(Constants.COMPLEX_PARAMETER_KEY), anyString());
        assertThat(result).contains("count").contains("more").contains("101").contains("209");
    }

}
