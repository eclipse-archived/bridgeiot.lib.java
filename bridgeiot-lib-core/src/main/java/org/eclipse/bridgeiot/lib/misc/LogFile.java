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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogFile {

    private static final Logger logger = LoggerFactory.getLogger(LogFile.class);

    private LogFile() {
    }

    public static synchronized void writeAccountingReport(String clientId, List<AccountingReport> reportList) {

        String fileName = "accounting" + File.separator + clientId + ".log";

        File file = new File(fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileWriter fw = new FileWriter(fileName, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw)) {
            for (AccountingReport report : reportList) {
                pw.println(report.toLogString());
            }
        } catch (IOException e) {
            logger.error("ERROR: Writing to Accouning Log File {}", fileName);
        }

    }

}
