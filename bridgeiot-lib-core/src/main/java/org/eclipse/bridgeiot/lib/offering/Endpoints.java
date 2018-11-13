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
 * Denis Kramer     (Bosch Software Innovations GmbH)
 * Stefan Schmid    (Robert Bosch GmbH)
 * Andreas Ziller   (Siemens AG)
 */
package org.eclipse.bridgeiot.lib.offering;

import org.eclipse.bridgeiot.lib.IProvider;
import org.eclipse.bridgeiot.lib.handlers.AccessRequestHandler;
import org.eclipse.bridgeiot.lib.handlers.AccessStreamFilterHandler;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.AccessInterfaceType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.EndpointType;
import org.eclipse.bridgeiot.lib.model.EndPoint;

public class Endpoints {

    public enum EndpointTypes {
        ACCESSREQUEST, ACCESSSTREAM, SAMPLEDATA, METADATA
    }

    private String route;

    private AccessRequestHandler accessRequestHandler = null;
    private AccessStreamFilterHandler accessStreamFilterHandler = null;
    private AccessRequestHandler sampleDataAccessRequestHandler = null;
    private AccessRequestHandler metaDataAccessRequestHandler = null;

    // List<EndPoint> endpointList = new ArrayList();
    private EndPoint accessRequestEndpoint = null;
    private EndPoint sampleDataEnpoint = null;
    private EndPoint metaDataEnpoint = null;

    public Endpoints(OfferingDescription offeringDescription) {
        this.route = offeringDescription.getLocalId();
    }

    public static Endpoints create(OfferingDescription od) {
        return new Endpoints(od);
    }

    public Endpoints withAccessRequestHandler(AccessRequestHandler callback) {
        this.accessRequestHandler = callback;
        return this;
    }

    public Endpoints withAccessStreamFilterHandler(AccessStreamFilterHandler filterCallback) {
        this.accessStreamFilterHandler = filterCallback;
        return this;
    }

    public Endpoints withEndpoint(AccessInterfaceType accessInterfaceType, String uri) {
        this.accessRequestEndpoint = new EndPoint(EndpointType.HTTP_GET, accessInterfaceType, uri);
        return this;
    }

    public Endpoints withEndpointUri(String uri) {
        this.accessRequestEndpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_PROXY, uri);
        return this;
    }

    public Endpoints withSampleDataRequestHandler(AccessRequestHandler callback) {
        this.sampleDataAccessRequestHandler = callback;
        return this;
    }

    public Endpoints withExternalSampleDataEndpoint(String uri) {
        this.sampleDataAccessRequestHandler = null;
        this.sampleDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, uri);
        return this;
    }

    public Endpoints withMetaDataRequestHandler(AccessRequestHandler callback) {
        this.metaDataAccessRequestHandler = callback;
        return this;
    }

    public Endpoints withExternalMetaDataEndpoint(String uri) {
        this.metaDataAccessRequestHandler = null;
        this.metaDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.EXTERNAL, uri);
        return this;
    }

    public Endpoints withRoute(String route) {
        this.route = route;
        return this;
    }

    public void updateEndpointsForProvider(IProvider provider) {
        if (this.accessRequestEndpoint == null) {
            this.accessRequestEndpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                    provider.getBaseUrl() + "/" + this.route);
        }
        if (this.metaDataAccessRequestHandler != null) {
            this.metaDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                    provider.getBaseUrl() + "/" + Constants.METADATA_ROUTE + this.route);
        }
        if (this.sampleDataAccessRequestHandler != null) {
            this.sampleDataEnpoint = new EndPoint(EndpointType.HTTP_GET, AccessInterfaceType.BRIDGEIOT_LIB,
                    provider.getBaseUrl() + "/" + Constants.SAMPLEDATA_ROUTE + this.route);
        }
    }

    public AccessRequestHandler getAccessRequestHandler() {
        return accessRequestHandler;
    }

    public void setAccessRequestHandler(AccessRequestHandler accessRequestHandler) {
        this.accessRequestHandler = accessRequestHandler;
    }

    public AccessStreamFilterHandler getAccessStreamFilterHandler() {
        return accessStreamFilterHandler;
    }

    public void setAccessStreamFilterHandler(AccessStreamFilterHandler accessStreamFilterHandler) {
        this.accessStreamFilterHandler = accessStreamFilterHandler;
    }

    public AccessRequestHandler getSampleDataAccessRequestHandler() {
        return sampleDataAccessRequestHandler;
    }

    public void setSampleDataAccessRequestHandler(AccessRequestHandler sampleDataAccessRequestHandler) {
        this.sampleDataAccessRequestHandler = sampleDataAccessRequestHandler;
    }

    public AccessRequestHandler getMetaDataAccessRequestHandler() {
        return metaDataAccessRequestHandler;
    }

    public void setMetaDataAccessRequestHandler(AccessRequestHandler metaDataAccessRequestHandler) {
        this.metaDataAccessRequestHandler = metaDataAccessRequestHandler;
    }

    public EndPoint getAccessRequestEndpoint() {
        return accessRequestEndpoint;
    }

    public void setAccessRequestEndpoint(EndPoint accessRequestEndpoint) {
        this.accessRequestEndpoint = accessRequestEndpoint;
    }

    public EndPoint getSampleDataEnpoint() {
        return sampleDataEnpoint;
    }

    public void setSampleDataEnpoint(EndPoint sampleDataEnpoint) {
        this.sampleDataEnpoint = sampleDataEnpoint;
    }

    public EndPoint getMetaDataEnpoint() {
        return metaDataEnpoint;
    }

    public void setMetaDataEnpoint(EndPoint metaDataEnpoint) {
        this.metaDataEnpoint = metaDataEnpoint;
    }

    public String getRoute() {
        return route;
    }

}
