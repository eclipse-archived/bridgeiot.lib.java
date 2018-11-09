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
package org.eclipse.bridgeiot.lib.model;

import com.fasterxml.jackson.annotation.JsonValue;

public class BridgeIotTypes {

    public enum PricingModel {
        FREE, PER_ACCESS, PER_MESSAGE, PER_MONTH, PER_BYTE
    }

    public enum LicenseType {
        CREATIVE_COMMONS, //
        OPEN_DATA_LICENSE, //
        NON_COMMERCIAL_DATA_LICENSE, PROJECT_INTERNAL_USE_ONLY
    }

    public enum FormatType {
        FORMAT_JSONLD, //
        FORMAT_XML
    }

    public enum EndpointType {
        HTTP_GET, //
        HTTP_POST, //
        HTTP_PUT, //
        COAP_GET, //
        COAP_POST, //
        COAP_PUT, //
        WEBSOCKET, MQTT, AMQP;

        public boolean isHTTP() {
            return this == HTTP_GET || this == HTTP_POST || this == HTTP_PUT;
        }

        public boolean isCOAP() {
            return this == COAP_GET || this == COAP_POST || this == COAP_PUT;
        }

        public boolean isGet() {
            return this == HTTP_GET || this == COAP_GET;
        }

        public boolean isPut() {
            return this == HTTP_PUT || this == COAP_PUT;
        }

        public boolean isPost() {
            return this == HTTP_POST || this == COAP_POST;
        }
    }

    public enum FunctionType {
        ACCESS, //
        FEED, //
        TASK, //
        EVENT
    }

    public enum AccessInterfaceType {
        UNSPECIFIED("UNSPECIFIED"), //
        BRIDGEIOT_LIB("BIGIOT_LIB"), // supported by marketplace
        BRIDGEIOT_PROXY("BIGIOT_PROXY"), // supported by marketplace
        EXTERNAL("EXTERNAL"); // supported by marketplace

        private final String typeName;

        private AccessInterfaceType(String typeName) {
            this.typeName = typeName;
        }

        public boolean equalsName(String otherName) {
            return typeName.equals(otherName);
        }

        @JsonValue
        public String getName() {
            return this.typeName;
        }

        @Override
        public String toString() {
            return this.typeName;
        }
    }

    public enum FeedTypes {
        ASYNC, //
        SYNC
    }

    public enum ResponseStatus {
        okay(200), //
        failure(500);

        int code;

        private ResponseStatus(int code) {
            this.code = code;
        }

        @JsonValue
        public int getCode() {
            return code;
        }

    }

    public enum MimeType {
        APPLICATION_XML("application/xml"), //
        APPLICATION_JSON("application/json"), //
        TEXT_PLAIN("text/plain"),
        // Used when we don't care about the format, such as in GET requests.
        APPLICATION_UNDEFINED("application/octet-stream");

        private final String name;

        private MimeType(String name) {
            this.name = name;
        }

        public boolean equalsName(String other) {
            return name.equals(other);
        }

        @Override
        @JsonValue
        public String toString() {
            return this.name;
        }

    }

    /**
     * ValueType is for the user and does not contain an Object type
     * 
     *
     */
    public enum ValueType {
        BOOLEAN("boolean"), DATETIME("datetime"), INTEGER("integer"), NUMBER("number"), TEXT("text"), UNDEFINED(
                "undefined");

        private final String name;

        private ValueType(String name) {
            this.name = name;
        }

        public boolean equalsName(String other) {
            return name.equals(other);
        }

        @JsonValue
        public String toString() {
            return this.name;
        }

    }

    /**
     * DataSchemaType is the internal representation of a parameter type including a complex type
     *
     */
    public enum DataSchemaType {
        NUMBER("number"), INTEGER("integer"), TEXT("text"), DATETIME("datetime"), OBJECT("object"), BOOLEAN("boolean"), // Must
                                                                                                                        // be
                                                                                                                        // uppercase
                                                                                                                        // due
                                                                                                                        // to
                                                                                                                        // the
                                                                                                                        // keyword
                                                                                                                        // boolean
        ARRAY("array"), REFERENCE("reference");

        private final String name;

        private DataSchemaType(String name) {
            this.name = name;
        }

        public boolean equalsName(String other) {
            return name.equals(other);
        }

        @JsonValue
        public String toString() {
            return this.name;
        }

    }

    /**
     * Specify how a parameter is encoded for a provider side integrated offering (integration mode 3)
     *
     */
    public enum ParameterEncodingType {
        QUERY("query"), ROUTE("route"), BODY("body"), TEMPLATE("template");

        private final String name;

        private ParameterEncodingType(String name) {
            this.name = name;
        }

        public boolean equalsName(String other) {
            return name.equals(other);
        }

        @JsonValue
        public String toString() {
            return this.name;
        }

    }
}
