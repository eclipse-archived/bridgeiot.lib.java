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

package org.eclipse.bridgeiot.lib.offering.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bridgeiot.lib.model.BridgeIotTypes.MimeType;
import org.eclipse.bridgeiot.lib.serverwrapper.BridgeIotHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Accounting {

    private String offeringId;
    private Map<String, AccountingReport> reportMap = null;
    private static ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(Accounting.class);

    public class AccountingReport {

        private String offeringId;
        private String subscriptionId;
        private String accessSessionId;
        private AccountingRecord accountingRecord;

        public AccountingReport(String offeringId, String subscriptionId, String accessSessionId) {
            this.offeringId = offeringId;
            this.subscriptionId = subscriptionId;
            this.accessSessionId = accessSessionId;
            this.accountingRecord = new AccountingRecord();
        }

        public AccountingReport(String offeringId, String subscriptionId, String accessSessionId,
                AccountingRecord accountingRecord) {
            this.offeringId = offeringId;
            this.subscriptionId = subscriptionId;
            this.accessSessionId = accessSessionId;
            this.accountingRecord = accountingRecord;
        }

        public AccountingRecord getRecord() {
            return this.accountingRecord;
        }

        public String getOfferingId() {
            return this.offeringId;
        }

        public String getSubscriptionId() {
            return this.subscriptionId;
        }

        public String getAccessSessionId() {
            return this.accessSessionId;
        }

        public String toLogString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(this.getOfferingId() + "," + this.getSubscriptionId() + "," + this.getAccessSessionId());
            strBuilder.append("," + this.getRecord().getTsFirstUpdate());
            strBuilder.append("," + this.getRecord().getTsLastUpdate());
            strBuilder.append("," + this.getRecord().getCurrentBytes());
            strBuilder.append("," + this.getRecord().getCurrentDataRecords());
            strBuilder.append("," + this.getRecord().getTotalBytes());
            strBuilder.append("," + this.getRecord().getTotalDataRecords());
            strBuilder.append("," + (this.getRecord().getTsLastUpdate() - this.getRecord().getTsFirstUpdate()));
            return strBuilder.toString();
        }

    }

    protected Accounting(String offeringId) {
        this.offeringId = offeringId;
        this.reportMap = new HashMap<>();
    }

    public static Accounting create(String offeringId) {
        return new Accounting(offeringId);
    }

    public void addEvent(String subscriptionId, String accessSessionId, BridgeIotHttpResponse response) {

        AccountingReport report;
        if (reportMap.containsKey(subscriptionId + accessSessionId)) {
            report = reportMap.get(subscriptionId + accessSessionId);
        } else {
            report = new AccountingReport(this.offeringId, subscriptionId, accessSessionId);
            reportMap.put(subscriptionId + accessSessionId, report);
        }

        // compute # of bytes
        int byteCount = response.getBody().length();

        // compute # of data records
        int recordCount = 0;
        if (response.getHeaders().containsValue(MimeType.APPLICATION_JSON.toString())) {
            recordCount = countJsonArrayElements(response.getBody());
        }
        // TODO: for XML

        report.getRecord().addBytesAndRecords(byteCount, recordCount);
        logger.debug("Add accounting event: # of Bytes = {}; # of Records = {}; SessionId = {}; Subscription = {}",
                byteCount, recordCount, accessSessionId, subscriptionId);
    }

    public void addEvent(String subscriptionId, String accessSessionId, String responseString) {

        AccountingReport report;
        if (reportMap.containsKey(subscriptionId + accessSessionId)) {
            report = reportMap.get(subscriptionId + accessSessionId);
        } else {
            report = new AccountingReport(this.offeringId, subscriptionId, accessSessionId);
            reportMap.put(subscriptionId + accessSessionId, report);
        }

        // compute # of bytes
        int byteCount = responseString.length();

        // compute # of data records
        int recordCount = countJsonArrayElements(responseString);
        // TODO: for XML

        report.getRecord().addBytesAndRecords(byteCount, recordCount);
        logger.debug("Add accounting event: # of Bytes = {}; # of Records = {}; SessionId = {}; Subscription = {}",
                byteCount, recordCount, accessSessionId, subscriptionId);
    }

    public List<AccountingReport> getCurrentReports() {
        List<AccountingReport> reportList = new ArrayList<>();
        for (AccountingReport report : reportMap.values()) {
            reportList.add(new AccountingReport(this.offeringId, report.getSubscriptionId(),
                    report.getAccessSessionId(), report.getRecord().getCurrentRecord()));
        }
        return reportList;
    }

    private int countJsonArrayElements(String json) {
        try {
            JsonNode jsonNode = mapper.reader().readTree(json);
            if (jsonNode == null || !jsonNode.isArray()) {
                return 0;
            }
            return jsonNode.size();
        } catch (IOException e) {
            return 0;
        }
    }

}
