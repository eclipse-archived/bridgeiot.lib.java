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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccessList implements QueryElement {

    List<String> organizationList = new ArrayList<>();

    public static AccessList create() {
        return new AccessList();
    }

    public static AccessList create(String... organizations) {
        return new AccessList().addOrganization(organizations);
    }

    public AccessList addOrganization(String... organizations) {
        for (String organization : organizations) {
            organizationList.add(organization);
        }
        return this;
    }

    public void clear() {
        organizationList.clear();
    }

    public List<String> getList() {
        return organizationList;
    }

    public String toQueryElement() {
        StringBuilder strBuilder = new StringBuilder().append("accessWhiteList: [");
        Iterator<String> it = organizationList.iterator();
        while (it.hasNext()) {
            String org = it.next();
            strBuilder.append(" \\\"").append(org).append("\\\"");
            if (it.hasNext()) {
                strBuilder.append(",");
            }
        }
        strBuilder.append(" ]");
        return strBuilder.toString();
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder().append("[accessList:[");
        Iterator<String> it = organizationList.iterator();
        while (it.hasNext()) {
            String org = it.next();
            strBuilder.append(org);
            if (it.hasNext()) {
                strBuilder.append(",");
            }
        }
        strBuilder.append("]]");
        return strBuilder.toString();
    }

}
