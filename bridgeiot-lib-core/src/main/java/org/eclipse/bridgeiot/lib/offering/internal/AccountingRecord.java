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

import java.util.Date;

public class AccountingRecord {

    private Long tsFirstUpdate;
    private Long tsLastUpdate;
    private Long tsLastReport;
    private Long tsCurrentReport;
    private Long currentAccesses;
    private Long totalAccesses;
    private Long currentBytes;
    private Long totalBytes;
    private Long currentDataRecords;
    private Long totalDataRecords;

    public AccountingRecord() {
        tsFirstUpdate = new Date().getTime() - 1; // ensure that the first update (that may be triggered in same msec
                                                  // comes later)
        tsLastReport = tsFirstUpdate;
        tsLastUpdate = 0L;
        tsCurrentReport = 0L;
        currentBytes = 0L;
        totalBytes = 0L;
        currentDataRecords = 0L;
        totalDataRecords = 0L;
        currentAccesses = 0L;
        totalAccesses = 0L;
    }

    public AccountingRecord(AccountingRecord record) {
        this.tsFirstUpdate = record.tsFirstUpdate;
        this.tsLastUpdate = record.tsLastUpdate;
        this.tsLastReport = record.tsLastReport;
        this.tsCurrentReport = record.tsCurrentReport;
        this.currentBytes = record.currentBytes;
        this.totalBytes = record.totalBytes;
        this.currentDataRecords = record.currentDataRecords;
        this.totalDataRecords = record.totalDataRecords;
        this.currentAccesses = record.currentAccesses;
        this.totalAccesses = record.totalAccesses;
    }

    public synchronized void addAccesses() {
        currentAccesses += 1;
        totalAccesses += 1;
        tsLastUpdate = new Date().getTime();
    }

    public synchronized void addBytes(int bytes) {
        currentBytes += bytes;
        totalBytes += bytes;
        tsLastUpdate = new Date().getTime();
    }

    public synchronized void addDataRecords(int records) {
        currentDataRecords += records;
        totalDataRecords += records;
        tsLastUpdate = new Date().getTime();
    }

    public synchronized void addBytesAndRecords(int bytes, int records) {
        currentAccesses += 1;
        totalAccesses += 1;
        currentBytes += bytes;
        totalBytes += bytes;
        currentDataRecords += records;
        totalDataRecords += records;
        tsLastUpdate = new Date().getTime();
    }

    public synchronized AccountingRecord getCurrentRecord() {
        AccountingRecord clone = new AccountingRecord(this);
        this.tsLastReport = new Date().getTime();
        this.tsLastUpdate = this.tsLastReport;
        clone.tsCurrentReport = this.tsLastReport;
        this.currentAccesses = 0L;
        this.currentBytes = 0L;
        this.currentDataRecords = 0L;
        return clone;
    }

    public Long getTsFirstUpdate() {
        return tsFirstUpdate;
    }

    public Long getTsLastUpdate() {
        return tsLastUpdate;
    }

    public Long getTsLastReport() {
        return tsLastReport;
    }

    public Long getTsCurrentReport() {
        return tsCurrentReport;
    }

    public Long getCurrentAccesses() {
        return currentAccesses;
    }

    public Long getTotalAccesses() {
        return totalAccesses;
    }

    public Long getCurrentBytes() {
        return currentBytes;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public Long getCurrentDataRecords() {
        return currentDataRecords;
    }

    public Long getTotalDataRecords() {
        return totalDataRecords;
    }

}
