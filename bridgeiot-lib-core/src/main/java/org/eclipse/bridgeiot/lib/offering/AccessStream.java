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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AccessStream {

    private ConcurrentLinkedQueue<JsonObject> queue = new ConcurrentLinkedQueue<>();
    private long lastAccess;

    private static Long DEFAULT_ACCESS_TIMEOUT = 10 * 60 * 1000L; // 10 min

    public AccessStream() {
        setLastAccess();
    }

    public static AccessStream create() {
        return new AccessStream();
    }

    public AccessStream clear() {
        queue.clear();
        return this;
    }

    public AccessStream flush() {
        return clear();
    }

    public AccessStream offer(JsonObject jsonObject) {
        if (jsonObject.isJsonArray()) {
            for (JsonObject jo : jsonObject.getJsonArrayAsList()) {
                queue.offer(jo);
            }
        } else {
            queue.offer(jsonObject);
        }
        return this;
    }

    public AccessStream offerList(List<JsonObject> jsonList) {
        for (JsonObject jo : jsonList) {
            queue.offer(jo);
        }
        return this;
    }

    public JsonObject poll() {
        setLastAccess();
        return queue.poll();
    }

    public List<JsonObject> pollAll() {
        setLastAccess();
        JsonObject jsonObject;
        List<JsonObject> jsonList = new ArrayList<>();
        while ((jsonObject = queue.poll()) != null) {
            jsonList.add(jsonObject);
        }
        return jsonList;
    }

    public boolean hasExpired() {
        return hasExpired(DEFAULT_ACCESS_TIMEOUT);
    }

    public boolean hasExpired(Long timeout) {
        if (timeout == 0L) {
            timeout = DEFAULT_ACCESS_TIMEOUT;
        }

        return new Date().getTime() > lastAccess + timeout;
    }

    @Override
    public AccessStream clone() {
        AccessStream clone = new AccessStream();
        Iterator<JsonObject> it = queue.iterator();
        while (it.hasNext()) {
            clone.offer(it.next());
        }
        lastAccess = new Date().getTime();
        return clone;
    }

    private void setLastAccess() {
        lastAccess = new Date().getTime();
    }

}
