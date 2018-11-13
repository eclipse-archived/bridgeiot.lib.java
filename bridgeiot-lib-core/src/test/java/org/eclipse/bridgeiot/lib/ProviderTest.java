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
package org.eclipse.bridgeiot.lib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.exceptions.IncompleteOfferingDescriptionException;
import org.eclipse.bridgeiot.lib.exceptions.InvalidOfferingException;
import org.eclipse.bridgeiot.lib.exceptions.NotRegisteredException;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.EndPoint;
import org.eclipse.bridgeiot.lib.offering.DeployedOffering;
import org.eclipse.bridgeiot.lib.offering.OfferingDescription;
import org.eclipse.bridgeiot.lib.offering.OfferingId;
import org.eclipse.bridgeiot.lib.offering.RegisteredOffering;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescription;
import org.eclipse.bridgeiot.lib.offering.RegistrableOfferingDescriptionChain;
import org.eclipse.bridgeiot.lib.serverwrapper.EmbededdedRouteBasedServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProviderTest {

    private final static boolean SUCCESS = true;
    private final static boolean FAILED = false;

    private final static String ACTIVATE_OFFERING_QUERY_RESP_TEMPLATE = "{\"data\":{\"activateOffering\":{\"id\":\"%s\", \"endpoints\":[ {\"uri\" : \"https://test.com\"}, {\"uri\" : \"localhost\"}]}}}";
    private final static String INVALID_ACTIVATE_OFFERING_QUERY_RESP_TEMPLATE = "{\"data\":{\"activateOffering\":{\"id\":\"%s\", \"unexpected\":[ {\"data\" : \"inThisQuery\"}, {\"asWell\" : \"unexpected\"}]}}}";

    Provider providerUnderTest;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    public Map<OfferingId, RegisteredOffering> offeringMap;

    @Before
    public void initialize() {
        providerUnderTest = new Provider("someId", "http://localhost:8080");
    }

    @Test
    public void authenticateMarketplaceClient() throws IOException {
        // Arrange
        MarketplaceClient clientMock = mock(MarketplaceClient.class);

        // Act
        providerUnderTest.authenticate("someSecret", clientMock);
        providerUnderTest.terminate();

        // Assert
        verify(clientMock, times(1)).authenticate();
        verify(clientMock, times(1)).close(); // check it terminated

    }

    @Test
    public void registerRegistrableOfferingDescription()
            throws IncompleteOfferingDescriptionException, NotRegisteredException {
        // Arrange
        RegistrableOfferingDescription registerableDescMock = mock(RegistrableOfferingDescription.class);
        OfferingId id = new OfferingId("someId");
        RegisteredOffering regOfferingMock = mock(RegisteredOffering.class);
        when(regOfferingMock.getOfferingId()).thenReturn(id);
        when(registerableDescMock.register()).thenReturn(regOfferingMock);

        // Act
        RegisteredOffering regOffering = providerUnderTest.register(registerableDescMock);

        // Assert
        verify(registerableDescMock, times(1)).register();
        assertThat(providerUnderTest.registeredOfferingMap.get(id)).isEqualTo(regOffering);
    }

    @Test
    public void activateRegistedOfferingActivationNotSuccessfulException() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Registration failed!");
        expectedException.expectCause(isA(BridgeIoTException.class));

        // Arrange
        MarketplaceClient clientMock = mock(MarketplaceClient.class);
        when(clientMock.request(anyString())).thenReturn(mock(Response.class));
        OfferingId id = new OfferingId("someId");

        // Act
        Provider.activate(id, clientMock);

        // Assert
        // see expectedException rule
    }

    @Test
    public void activateRegistedOfferingUnmarshallJsonException() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Registration failed!");
        expectedException.expectCause(isA(BridgeIoTException.class));

        // Arrange
        Response response = createResponseMock("any", SUCCESS);
        MarketplaceClient clientMock = mock(MarketplaceClient.class);
        when(clientMock.request(anyString())).thenReturn(response);
        OfferingId id = new OfferingId("someId");

        // Act
        Provider.activate(id, clientMock);

        // Assert
        // see expectedException rule
    }

    @Test
    public void activateRegistedOfferingMappingException() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Registration failed!");
        expectedException.expectCause(isA(BridgeIoTException.class));

        // Arrange
        Response response = createResponseMock(INVALID_ACTIVATE_OFFERING_QUERY_RESP_TEMPLATE, SUCCESS);
        MarketplaceClient clientMock = mock(MarketplaceClient.class);
        when(clientMock.request(anyString())).thenReturn(response);
        OfferingId id = new OfferingId("someId");

        // Act
        Provider.activate(id, clientMock);

        // Assert
        // see expectedException rule
    }

    @Test
    public void registerAlreadyActivatedRegistedOfferingSuccessful() throws IOException {
        // Arrange
        Response response = createResponseMock(ACTIVATE_OFFERING_QUERY_RESP_TEMPLATE, SUCCESS);
        MarketplaceClient clientMock = mock(MarketplaceClient.class);
        when(clientMock.request(anyString())).thenReturn(response);
        OfferingId id = new OfferingId("someId");

        RegistrableOfferingDescriptionChain activatedOfferingDescription = Provider.activate(id, clientMock);
        RegistrableOfferingDescriptionChain activatedOfferingDescSpy = spy(activatedOfferingDescription);

        // Act
        providerUnderTest.authenticate("someSecret", clientMock);
        providerUnderTest.register(id, null, null, activatedOfferingDescSpy);

        // Assert
        verify(activatedOfferingDescSpy, times(1)).setMarketplaceClient(clientMock);
        verify(activatedOfferingDescSpy, times(1)).setOfferingMap(providerUnderTest.registeredOfferingMap);
        verify(activatedOfferingDescSpy, never()).getAccessInterfaceType(); // verify that we are a non-A1 offering
    }

    @Test
    public void registerAlreadyActivatedRegistedOfferingUnsupportedIntegrationMode() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("Unsupported integration mode");

        // Arrange
        OfferingId id = new OfferingId("someId");
        AccessRequestHandler requestHandlerMock = mock(AccessRequestHandler.class);
        EmbededdedRouteBasedServer serverMock = mock(EmbededdedRouteBasedServer.class);
        RegistrableOfferingDescriptionChain activatedOfferingDescMock = mock(RegistrableOfferingDescriptionChain.class);
        when(activatedOfferingDescMock.getAccessInterfaceType()).thenReturn(AccessInterfaceType.UNSPECIFIED);
        when(activatedOfferingDescMock.getEndpoints()).thenReturn(Arrays.asList(new EndPoint(EndpointType.HTTP_GET,
                AccessInterfaceType.BRIDGEIOT_LIB, "http://test.this.url.de/magicRoute")));

        // Act
        providerUnderTest.register(id, requestHandlerMock, serverMock, activatedOfferingDescMock);

        // Assert
        // see expectedException rule
    }

    @Test
    public void registerAlreadyActivatedRegistedOfferingExternalMode() throws IOException {
        // Arrange
        OfferingId id = new OfferingId("someId");
        AccessRequestHandler requestHandlerMock = mock(AccessRequestHandler.class);
        EmbededdedRouteBasedServer serverMock = mock(EmbededdedRouteBasedServer.class);
        RegistrableOfferingDescriptionChain activatedOfferingDescMock = mock(RegistrableOfferingDescriptionChain.class);
        when(activatedOfferingDescMock.getAccessInterfaceType()).thenReturn(AccessInterfaceType.EXTERNAL);
        when(activatedOfferingDescMock.getEndpoints()).thenReturn(Arrays.asList(new EndPoint(EndpointType.HTTP_GET,
                AccessInterfaceType.BRIDGEIOT_LIB, "http://test.this.url.de/magicRoute")));

        // Act
        providerUnderTest.register(id, requestHandlerMock, serverMock, activatedOfferingDescMock);

        // Assert
        assertThat(providerUnderTest.registeredOfferingMap.get(id)).isNotInstanceOf(DeployedOffering.class);
        assertThat(providerUnderTest.registeredOfferingMap.get(id)).isInstanceOf(RegisteredOffering.class);
    }

    @Test
    public void registerRegistedOfferingIncompleteException() throws IOException {
        // Setup expected exception rule
        expectedException.expect(BridgeIoTException.class);
        expectedException.expectMessage("OfferingDescription was incomplete");

        // Arrange
        OfferingId id = new OfferingId("someId");
        AccessRequestHandler requestHandlerMock = mock(AccessRequestHandler.class);
        EmbededdedRouteBasedServer serverMock = mock(EmbededdedRouteBasedServer.class);
        RegistrableOfferingDescriptionChain activatedOfferingDescMock = mock(RegistrableOfferingDescriptionChain.class);

        // Act
        providerUnderTest.register(id, requestHandlerMock, serverMock, activatedOfferingDescMock);

        // Assert
        // see expectedException rule
    }

    @Test
    public void registerAlreadyActivatedRegistedOfferingLibMode() throws IOException {
        // Arrange
        OfferingId id = new OfferingId("someId");
        AccessRequestHandler requestHandlerMock = mock(AccessRequestHandler.class);
        EmbededdedRouteBasedServer serverMock = mock(EmbededdedRouteBasedServer.class);
        RegistrableOfferingDescriptionChain activatedOfferingDescMock = mock(RegistrableOfferingDescriptionChain.class);
        when(activatedOfferingDescMock.getAccessInterfaceType()).thenReturn(AccessInterfaceType.BRIDGEIOT_LIB);
        when(activatedOfferingDescMock.getEndpoints()).thenReturn(Arrays.asList(new EndPoint(EndpointType.HTTP_GET,
                AccessInterfaceType.BRIDGEIOT_LIB, "http://test.this.url.de/magicRoute")));

        // Act
        providerUnderTest.register(id, requestHandlerMock, serverMock, activatedOfferingDescMock);

        // Assert
        assertThat(providerUnderTest.registeredOfferingMap.get(id)).isInstanceOf(DeployedOffering.class);
    }

    @Test
    public void calculateRouteUsingBigRouteMagic() {
        // Arrange
        String urlUnderTest = "http://test.this.url.de/findMeWithMagic";
        RegistrableOfferingDescriptionChain activatedOfferingDescription = mock(
                RegistrableOfferingDescriptionChain.class);
        when(activatedOfferingDescription.getEndpoints()).thenReturn(
                Arrays.asList(new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB, urlUnderTest)));

        // Act
        String urlAfterMagic = Provider.calculateRoute(activatedOfferingDescription);

        // Assert
        assertThat(urlAfterMagic).isEqualTo("findMeWithMagic");
    }

    @Test
    public void createOfferingDescriptionFromOfferingIdSuccess() throws InvalidOfferingException, IOException {
        // Arrange
        String someId = "someIdString";
        OfferingDescription offeringDescMock = mock(OfferingDescription.class);
        when(offeringDescMock.getId()).thenReturn(someId);
        MarketplaceClient clientMock = mock(MarketplaceClient.class);
        when(clientMock.getOfferingDescription(someId)).thenReturn(offeringDescMock);

        // Act
        providerUnderTest.authenticate("someSecret", clientMock);
        RegistrableOfferingDescriptionChain offeringDescriptionFromOfferingId = providerUnderTest
                .createOfferingDescriptionFromOfferingId(someId);

        // Assert
        assertThat(offeringDescriptionFromOfferingId.getId()).isEqualTo(someId);
    }

    @Test(expected = InvalidOfferingException.class)
    public void createOfferingDescriptionFromOfferingIdInvalidOfferingException()
            throws InvalidOfferingException, IOException {
        // Arrange
        String someId = "someIdString";
        MarketplaceClient clientMock = mock(MarketplaceClient.class);
        when(clientMock.getOfferingDescription(someId)).thenThrow(InvalidOfferingException.class);

        // Act
        providerUnderTest.authenticate("someSecret", clientMock);
        providerUnderTest.createOfferingDescriptionFromOfferingId(someId);

        // Assert
        // see expected Exception
    }

    @Test
    public void deregisterNothingToDo() {
        // Arrange
        OfferingId id = new OfferingId("someId");
        providerUnderTest.registeredOfferingMap = offeringMap;

        // Act
        providerUnderTest.deregister(id);

        // Assert
        verify(providerUnderTest.registeredOfferingMap, times(1)).containsKey(id);
        verify(providerUnderTest.registeredOfferingMap, never()).get(id);
    }

    @Test
    public void deregisterRemoveOffering() {
        // Arrange
        OfferingId id = new OfferingId("someId");
        providerUnderTest.registeredOfferingMap = offeringMap;
        when(offeringMap.containsKey(id)).thenReturn(true);
        when(offeringMap.get(id)).thenReturn(mock(RegisteredOffering.class));

        // Act
        providerUnderTest.deregister(id);

        // Assert
        verify(providerUnderTest.registeredOfferingMap, times(1)).containsKey(id);
        verify(providerUnderTest.registeredOfferingMap, times(1)).get(id);
        verify(providerUnderTest.registeredOfferingMap, times(1)).remove(id);
    }

    private static Response createResponseMock(String body, boolean isSuccessful) throws IOException {
        Response respMock = mock(Response.class);
        ResponseBody bodyMock = mock(ResponseBody.class);
        when(bodyMock.string()).thenReturn(body);
        when(respMock.body()).thenReturn(bodyMock);
        when(respMock.isSuccessful()).thenReturn(isSuccessful);

        return respMock;
    }

}
