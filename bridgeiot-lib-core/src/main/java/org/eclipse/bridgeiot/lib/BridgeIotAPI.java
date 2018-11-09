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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.eclipse.bridgeiot.lib.misc.Constants;
import org.eclipse.bridgeiot.lib.misc.GraphQLQueries;
import org.eclipse.bridgeiot.lib.misc.Helper;
import org.eclipse.bridgeiot.lib.misc.HttpClient;
import org.eclipse.bridgeiot.lib.misc.LogFile;
import org.eclipse.bridgeiot.lib.misc.MarketplaceClient;
import org.eclipse.bridgeiot.lib.offering.internal.Accounting.AccountingReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the base class of the Bridge.IoT API, it brings authentication functionality.
 * 
 */
public abstract class BridgeIotAPI {

    protected BridgeIotClientId clientId;
    protected String marketplaceUri;
    protected MarketplaceClient marketplaceClient;
    protected String consumerCertFilename = null;
    protected String providerCertFilename = null;
    protected ArrayList<AccountingReport> accountingReportList;

    // scheduled executor to re-register the offering prior to expiration
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final int MINIMUM_ACCOUNTING_INTERVAL = 60; // in seconds = 60 secs
    private static final int ACCOUNTING_REPORT_TIMEOUT = 5000; // in milliseconds = 5 seconds

    private static final Logger logger = LoggerFactory.getLogger(BridgeIotAPI.class);

    protected String clientSecret;

    public BridgeIotAPI(BridgeIotClientId clientId, String marketplaceUri) {
        this.clientId = clientId;
        this.marketplaceUri = marketplaceUri;
        accountingReportList = new ArrayList<>();

        // start Executor Service for accounting reports
        executor.scheduleAtFixedRate(accountingRunnable, MINIMUM_ACCOUNTING_INTERVAL, MINIMUM_ACCOUNTING_INTERVAL,
                TimeUnit.SECONDS);
    }

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     *            API Key for authentication at the marketplace
     * @throws IOException
     */
    public BridgeIotAPI authenticate(String clientSecret) throws IOException {
        return authenticate(clientSecret, MarketplaceClient.create(this.marketplaceUri, this.clientId, clientSecret));
    }

    /**
     * Authenticates instance at the Marketplace. The client secret can be specified either by the withClientSecret
     * method or by configuration based object creation
     * 
     * @throws IOException
     */
    public BridgeIotAPI authenticate() throws IOException {
        if (clientSecret != null) {
            return authenticate(clientSecret);
        } else {
            throw new BridgeIoTException(
                    "Client secret not set - use withClientSecret() method or respectively specify according key in your property file");
        }
    }

    /**
     * Authenticates instance at the Marketplace.
     * 
     * @param clientSecret
     *            API Key for authentication at the marketplace
     * @param marketplaceClient
     * @throws IOException
     */
    BridgeIotAPI authenticate(String clientSecret, MarketplaceClient marketplaceClient) throws IOException {
        this.marketplaceClient = marketplaceClient;
        this.marketplaceClient.authenticate();
        return this;
    }

    /**
     * Terminate the Consumer or Provider instance.
     */
    public void terminate() {

        // Stop periodic accounting executor
        executor.shutdownNow();

        // Trigger final accounting report
        Thread accountingThread = new Thread(accountingRunnable);
        accountingThread.start();
        try {
            accountingThread.join(ACCOUNTING_REPORT_TIMEOUT);
        } catch (InterruptedException e) {
            logger.error("ERROR: Could not sent final accounting report!");
        }

    }

    // Reduce visibility
    public String getProviderCertFile() {
        return this.providerCertFilename;
    }

    // Reduce visibility
    public MarketplaceClient getMarketplaceClient() {
        return this.marketplaceClient;
    }

    /**
     * Get client Id
     * 
     * @return client Id
     */
    public BridgeIotClientId getClientId() {
        return this.clientId;
    }

    /**
     * Set a web proxy host and port
     * 
     * @param host
     * @param port
     */
    public void setProxy(String host, int port) {
        if (host != null && !host.isEmpty()) {
            HttpClient.setDefaultProxy(host, port);
        }
    }

    /**
     * Add a host on the no-proxy list
     * 
     * @param host
     */
    public void addProxyBypass(String host) {
        if (host != null && !host.isEmpty()) {
            HttpClient.addDefaultProxyBypass(host);
        }
    }

    /**
     * Remove a host on the no-proxy list
     * 
     * @param host
     */
    public void removeProxyBypass(String host) {
        HttpClient.removeDefaultProxyBypass(host);
    }

    public void addAccountingReports(List<AccountingReport> reportList) {
        for (AccountingReport report : reportList) {
            if ((report != null) && (report.getRecord().getTsLastUpdate() > report.getRecord().getTsLastReport())) {
                synchronized (accountingReportList) {
                    accountingReportList.add(report);
                }
            }
        }
    }

    public List<AccountingReport> getCurrentAccountingReports() {
        prepareAccountingReport();
        return accountingReportList;
    }

    protected abstract void prepareAccountingReport();

    protected void sendAccountingReport() {
        if (!accountingReportList.isEmpty()) {
            synchronized (accountingReportList) {
                // process accountingReportList
                printAccountingLog();
                sendAccountingReportToMarketplace();
                LogFile.writeAccountingReport(this.clientId.value, accountingReportList);
                accountingReportList.clear();
            }
        }
    }

    /*
     * Send Accounting Report to Marketplace (internal function)
     * 
     */
    private void sendAccountingReportToMarketplace() {

        // check if there is at least one accounting report that is not unknown
        boolean onlyUnknownSubscriptions = true;
        for (AccountingReport report : accountingReportList) {
            if (!report.getSubscriptionId().equals(Constants.UNKNOWN_SUBSCRIPTION_ID)) {
                onlyUnknownSubscriptions = false;
            }
        }

        // return if all report are for unknown subscriptions
        if (onlyUnknownSubscriptions) {
            return;
        }

        String accountingString = GraphQLQueries.getAccountingReportString(accountingReportList);
        logger.info("Accounting Report: {}", accountingString);

        // Send Accounting Report to Marketplace
        try {

            Response response = marketplaceClient.request(accountingString);

            if (!response.isSuccessful()) {
                throw new BridgeIoTException("Activation request to eXchange was not successful!");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Accounting Report Response: {}", response.body().string());
            }
            response.close();

        } catch (IOException | BridgeIoTException e) {
            logger.error(e.getMessage(), e);
            throw new BridgeIoTException("Registration failed!", e);
        }

    }

    protected Runnable accountingRunnable = new Runnable() {
        @Override
        public void run() {
            prepareAccountingReport();
            sendAccountingReport();
        }
    };

    protected void printAccountingLog() {
        if (accountingReportList.size() > 0) {
            logger.info(">>> Accounting Report");
            for (AccountingReport report : accountingReportList) {
                logger.info(">>>    AccessSessionId: {} - Subscription: {}", report.getAccessSessionId(),
                        report.getSubscriptionId());
                logger.info(
                        ">>>         # Bytes = {}; # DataRecords = {}; TotalBytes = {}; TotalDataRecords = {}; SessionDuration = {}",
                        report.getRecord().getCurrentBytes(), report.getRecord().getCurrentDataRecords(),
                        report.getRecord().getTotalBytes(), report.getRecord().getTotalDataRecords(),
                        report.getRecord().getTsLastUpdate() - report.getRecord().getTsFirstUpdate());
            }
        }
    }

    /**
     * Enables proxy configuration only if it is required. This is useful if connections are sometimes from inside a
     * proxy-gated network
     * 
     * @param proxyHost
     * @param proxyPort
     * @return
     */
    public BridgeIotAPI withAutoProxy(String proxyHost, int proxyPort) {
        try {

            if (Helper.isBehindProxy(proxyHost)) {
                setProxy(proxyHost, proxyPort);
                logger.info("You are behind a proxy. Using default proxy at " + proxyHost + ".");

            } else {
                logger.info("You are not behind a proxy.");
            }

        } catch (IOException e) {
            throw new BridgeIoTException("Cannot auto-detect proxy");
        }
        return this;
    }

    public BridgeIotAPI withClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

}
