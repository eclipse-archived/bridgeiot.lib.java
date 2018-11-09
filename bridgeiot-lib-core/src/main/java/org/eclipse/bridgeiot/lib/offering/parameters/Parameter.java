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

package org.eclipse.bridgeiot.lib.offering.parameters;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.DataSchemaType;
import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = ComplexParameter.class, name = "object"),
        @JsonSubTypes.Type(value = DateTimeParameter.class, name = "datetime"),
        @JsonSubTypes.Type(value = NumberParameter.class, name = "number"),
        @JsonSubTypes.Type(value = TextParameter.class, name = "text"),
        @JsonSubTypes.Type(value = BooleanParameter.class, name = "boolean"),
        @JsonSubTypes.Type(value = IntegerParameter.class, name = "integer"),
        @JsonSubTypes.Type(value = UndefinedParameter.class, name = "undefined") })
public abstract class Parameter {

    protected static final Logger logger = LoggerFactory.getLogger(Parameter.class);

    DataSchemaType type;

    // Required by Jackson
    public Parameter() {

    }

    public DataSchemaType getType() {
        return type;
    }

    public void setType(DataSchemaType type) {
        this.type = type;
    }

    public static Parameter toParameter(ValueType valueType) {
        Parameter parameter = null;
        switch (valueType) {
        case INTEGER:
            parameter = new IntegerParameter();
            break;
        case NUMBER:
            parameter = new NumberParameter();
            break;
        case TEXT:
            parameter = new TextParameter();
            break;
        case DATETIME:
            parameter = new DateTimeParameter();
            break;
        default:
            parameter = new UndefinedParameter();
        }
        return parameter;
    }

}
