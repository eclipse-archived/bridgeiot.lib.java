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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.eclipse.bridgeiot.lib.exceptions.BridgeIoTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables template based message generation with keywords
 */
public class FilloutTemplate {

    private static final Logger logger = LoggerFactory.getLogger(FilloutTemplate.class);

    private static final int MAX_TEMPLATE_SIZE = 2048;

    private static final String ESCAPE_SEQUENCE = "%%";
    Map<String, String> kvp;
    String template = null;

    private boolean removeWhitespace = false;
    private boolean filledOut = false;

    /**
     * Constructor with a filename for template and map for keyword and value combinations
     *
     * @param fileName
     * @param kvp
     */

    public FilloutTemplate(String defaultTemplate, Map<String, String> kvp) {
        this(defaultTemplate, null, kvp);
    }

    public FilloutTemplate(String defaultTemplate, String fileName, Map<String, String> kvp) {

        checkKVP(kvp);

        template = defaultTemplate;

        if (fileName != null) {
            final File file = new File(fileName);
            if (file.exists()) {
                try {
                    template = readFile(fileName);
                } catch (final IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }

        }

        this.kvp = kvp;
    }

    public FilloutTemplate(Map<String, String> kvp, String resourceName, boolean removeWhitespace) {
        final InputStream inputStream = Helper.getResource(resourceName);
        checkKVP(kvp);
        this.removeWhitespace = removeWhitespace;
        try {
            template = readInputStream(inputStream);
            if (removeWhitespace) {
                template = template.trim().replaceAll("(\\r|\\n)", "");
            }
            this.kvp = kvp;
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void checkKVP(Map<String, String> kvp) {
        for (final String key : kvp.keySet()) {
            if (key.startsWith(ESCAPE_SEQUENCE)) {
                throw new BridgeIoTException("FilloutTemplate: Escape Sequence should be ommited in hash map " + key);
            }
            if (key.length() == 0) {
                throw new BridgeIoTException("FilloutTemplate: Empty Key String " + key);
            }
        }
    }

    /**
     * Fills out template and returns it as a string
     *
     * @return
     */
    public String fillout() {
        if (filledOut) {
            throw new BridgeIoTException("Template already filled out");
        }
        filledOut = true;
        for (final Entry<String, String> entry : kvp.entrySet()) {
            template = template.replaceAll(ESCAPE_SEQUENCE + entry.getKey(),
                    Matcher.quoteReplacement(entry.getValue()));
        }

        if (template.contains("" + ESCAPE_SEQUENCE)) {
            throw new BridgeIoTException("Fillout template: key token left over");
        }

        if (removeWhitespace) {
            template = template.trim().replaceAll("(\\r|\\n)", "");
        }
        return template;

    }

    private static String readFile(String path) throws IOException {
        final File file = new File(path);
        final FileInputStream inputStream = new FileInputStream(file);
        return readInputStream(inputStream);
    }

    private static String readInputStream(InputStream inputStream) {
        final java.util.Scanner scanner = new java.util.Scanner(inputStream);
        scanner.useDelimiter("\\A");
        final String string = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return string;
    }

}