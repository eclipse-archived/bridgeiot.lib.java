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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class JsonObject {

    private static ObjectMapper mapper = new ObjectMapper();

    private JsonNode internalJson;

    /**
     * Constructor.
     * 
     * @param rawJson
     *            Json-String
     */
    public JsonObject(String rawJson) {
        read(rawJson);
    }

    private JsonObject(JsonNode jsonNode) {
        internalJson = jsonNode;
    }

    private JsonObject(JsonObject... jsonObjects) {
        ArrayNode an = mapper.getNodeFactory().arrayNode();
        for (JsonObject jo : jsonObjects) {
            an.add(jo.getInternalJson());
        }
        internalJson = an;
    }

    private JsonNode getInternalJson() {
        return internalJson;
    }

    /**
     * Reads the raw Json-String and converts it to an internal object structure hold by this JsonObject.
     * 
     * @param rawJson
     *            Json-String
     */
    private void read(String rawJson) {
        try {
            internalJson = mapper.reader().readTree(rawJson);
        } catch (IOException e) {
            throw new BridgeIoTException("Read-processing of Json-string failed!", e);
        }
    }

    /**
     * Writes the Json object structure hold by this JsonObject back to a Json string.
     * 
     * @return rawJson Json-String
     */
    public String write() {
        try {
            return mapper.writer().writeValueAsString(internalJson);
        } catch (IOException e) {
            throw new BridgeIoTException("Write-processing to Json-string failed!", e);
        }
    }

    /**
     * Indicates whether this JsonObject represents an array or not.
     * 
     * @return <code>true</code> if this JsonObject represents an array - <code>false</code> otherwise.
     */
    public boolean isJsonArray() {
        return internalJson.isArray();
    }

    /**
     * Returns the representation of this JsonObject as a list of JsonObjects. <br>
     * In case this JsonObject is a json-array, each of its elements is packaged as JsonObject and added to the
     * result-list. <br>
     * In case this JsonObject is <b>not</b> a json-array, this JsonObject is added to the result-list.
     * 
     * @return
     */
    public List<JsonObject> getJsonArrayAsList() {
        List<JsonObject> result = new ArrayList<>();
        if (internalJson.isArray()) {
            Iterator<JsonNode> iterator = internalJson.elements();
            while (iterator.hasNext()) {
                result.add(new JsonObject(iterator.next()));
            }
        } else {
            result.add(this);
        }
        return result;
    }

    public static JsonObject createJsonArray(JsonObject... jsonObjects) {
        return new JsonObject(jsonObjects);
    }

    public static JsonObject createJsonArray(List<JsonObject> jsonObjectList) {
        return new JsonObject(jsonObjectList.toArray(new JsonObject[jsonObjectList.size()]));
    }

}
