/* Copyright 2002-2021 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.files.ccsds.utils.generation;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.utils.FileFormat;

/** Generator for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KvnGenerator implements Generator {

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    private static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for all key/value pair lines. **/
    private static final String KV_FORMAT = "%s = %s%n";

    /** String format used for all comment lines. **/
    private static final String COMMENT_FORMAT = "COMMENT %s%n";

    /** Start suffix for sections. */
    private static final String START = "_START";

    /** Stop suffix for sections. */
    private static final String STOP = "_STOP";

    /** New line separator for output file. */
    private static final char NEW_LINE = '\n';

    /** Destination of generated output. */
    private final Appendable output;

    /** File name for error messages. */
    private final String fileName;

    /** Sections stack. */
    private final Deque<String> sections;

    /** Simple constructor.
     * @param output destination of generated output
     * @param fileName file name for error messages
     */
    public KvnGenerator(final Appendable output, final String fileName) {
        this.output   = output;
        this.fileName = fileName;
        this.sections = new ArrayDeque<>();
    }

    /** {@inheritDoc} */
    @Override
    public FileFormat getFormat() {
        return FileFormat.KVN;
    }

    /** {@inheritDoc} */
    @Override
    public void startMessage(final String messageTypeKey, final double version) throws IOException {
        writeEntry(messageTypeKey, String.format(STANDARDIZED_LOCALE, "%.1f", version), true);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void writeComments(final CommentsContainer comments) throws IOException {
        for (final String comment : comments.getComments()) {
            output.append(String.format(STANDARDIZED_LOCALE, COMMENT_FORMAT, comment));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final String value, final boolean mandatory) throws IOException {
        if (value == null) {
            if (mandatory) {
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, key, fileName);
            }
        } else {
            output.append(String.format(STANDARDIZED_LOCALE, KV_FORMAT, key, value));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEmptyLine() throws IOException {
        output.append(NEW_LINE);
    }

    /** {@inheritDoc} */
    @Override
    public void writeRawData(final char data) throws IOException {
        output.append(data);
    }

    /** {@inheritDoc} */
    @Override
    public void writeRawData(final CharSequence data) throws IOException {
        output.append(data);
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        output.append(name).append(START).append(NEW_LINE);
        sections.offerLast(name);
    }

    /** {@inheritDoc} */
    @Override
    public void exitSection() throws IOException {
        output.append(sections.pollLast()).append(STOP).append(NEW_LINE);
    }

}
