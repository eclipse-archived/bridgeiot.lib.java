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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes;
import org.eclipse.bridgeiot.lib.query.elements.PriceFilterTest;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.EndpointContext;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@RunWith(MockitoJUnitRunner.class)
public class CoapClientTest {

    private static final Logger logger = LoggerFactory.getLogger(PriceFilterTest.class);

    private CoapClient clientUnderTest;

    @Before
    public void initialize() {
        NetworkConfig.createStandardWithoutFile();
        clientUnderTest = new CoapClient();
    }

    @Test
    public void testDoAdvancedPutInternalClient() {
        // Arrange

        // Act
        Request req = Request.newPut();
        req.setSourceContext(mock(EndpointContext.class));
        req.setResponse(Response.createResponse(req, ResponseCode.CREATED));
        CoapResponse resp = clientUnderTest.doAdvanced("coap://localhost:123", BridgeIotTypes.MimeType.APPLICATION_XML,
                BridgeIotTypes.MimeType.APPLICATION_XML, "some body", req);

        // Assert
        logger.info(req.toString());
        assertThat(resp).isNotNull();

    }

    @Test
    public void testDoAdvancedPostInternalClient() {
        // Arrange

        // Act
        Request req = Request.newPost();
        req.setSourceContext(mock(EndpointContext.class));
        req.setResponse(Response.createResponse(req, ResponseCode.CREATED));
        CoapResponse resp = clientUnderTest.doAdvanced("coap://localhost:123", BridgeIotTypes.MimeType.APPLICATION_XML,
                BridgeIotTypes.MimeType.APPLICATION_XML, "some body", req);

        // Assert
        logger.info(req.toString());
        assertThat(resp).isNotNull();

    }

    @Test
    public void testDoAdvancedPut() {
        // Arrange
        CoapClient clientSpy = spy(clientUnderTest);
        org.eclipse.californium.core.CoapClient mockedInternalClient = mock(
                org.eclipse.californium.core.CoapClient.class);
        CoapResponse mockedResp = mock(CoapResponse.class);
        doReturn(mockedInternalClient).when(clientSpy).getInternalClient();
        when(mockedInternalClient.advanced(any(Request.class))).thenReturn(mockedResp);

        // Act
        Request reqSpy = spy(Request.newPut());
        doReturn(reqSpy).when(reqSpy).setURI("coap://localhost");
        CoapResponse resp = clientSpy.doAdvanced("coap://localhost", BridgeIotTypes.MimeType.APPLICATION_XML,
                BridgeIotTypes.MimeType.APPLICATION_XML, "some body", reqSpy);

        // Assert
        verify(reqSpy, times(1)).setPayload(anyString());
        verify(clientSpy, times(1)).getInternalClient();
        verify(mockedInternalClient, times(1)).advanced(any(Request.class));
        logger.info(reqSpy.toString());
        assertThat(resp).isEqualTo(mockedResp);

    }

    @Test
    public void testDoAdvancedPost() {
        // Arrange
        CoapClient clientSpy = spy(clientUnderTest);
        org.eclipse.californium.core.CoapClient mockedInternalClient = mock(
                org.eclipse.californium.core.CoapClient.class);
        CoapResponse mockedResp = mock(CoapResponse.class);
        doReturn(mockedInternalClient).when(clientSpy).getInternalClient();
        when(mockedInternalClient.advanced(any(Request.class))).thenReturn(mockedResp);

        // Act
        Request reqSpy = spy(Request.newPost());
        doReturn(reqSpy).when(reqSpy).setURI("coap://localhost");
        CoapResponse resp = clientSpy.doAdvanced("coap://localhost", BridgeIotTypes.MimeType.APPLICATION_JSON,
                BridgeIotTypes.MimeType.APPLICATION_XML, "some body", reqSpy);

        // Assert
        verify(reqSpy, times(1)).setPayload(anyString());
        verify(clientSpy, times(1)).getInternalClient();
        verify(mockedInternalClient, times(1)).advanced(any(Request.class));
        logger.info(reqSpy.toString());
        assertThat(resp).isEqualTo(mockedResp);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoPutIllegalAcceptMimeType() {
        // Arrange
        CoapClient clientSpy = spy(clientUnderTest);
        org.eclipse.californium.core.CoapClient mockedInternalClient = mock(
                org.eclipse.californium.core.CoapClient.class);
        when(clientSpy.getInternalClient()).thenReturn(mockedInternalClient);

        // Act
        clientSpy.doPut("coap://localhost", BridgeIotTypes.MimeType.TEXT_PLAIN, BridgeIotTypes.MimeType.APPLICATION_XML,
                "some body");

        // Assert
        // see expect Annotation
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoPutIllegalContentMimeType() {
        // Arrange
        CoapClient clientSpy = spy(clientUnderTest);
        org.eclipse.californium.core.CoapClient mockedInternalClient = mock(
                org.eclipse.californium.core.CoapClient.class);
        when(clientSpy.getInternalClient()).thenReturn(mockedInternalClient);

        // Act
        clientSpy.doPut("coap://localhost", BridgeIotTypes.MimeType.APPLICATION_UNDEFINED,
                BridgeIotTypes.MimeType.TEXT_PLAIN, "some body");

        // Assert
        // see expect Annotation
    }

}
