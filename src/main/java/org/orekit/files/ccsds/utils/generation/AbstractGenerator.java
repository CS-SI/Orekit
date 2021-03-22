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

/** Base class for both Key-Value Notation and eXtended Markup Language generators for CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractGenerator implements Generator {

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    protected static final Locale STANDARDIZED_LOCALE = Locale.US;

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
    public AbstractGenerator(final Appendable output, final String fileName) {
        this.output   = output;
        this.fileName = fileName;
        this.sections = new ArrayDeque<>();
    }

    /** Append a character sequence to output stream.
     * @param cs character sequence to append
     * @return reference to this
     * @throws IOException if an I/O error occurs.
     */
    protected AbstractGenerator append(final CharSequence cs) throws IOException {
        output.append(cs);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void newLine() throws IOException {
        output.append(NEW_LINE);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final Enum<?> value, final boolean mandatory) throws IOException {
        writeEntry(key, value == null ? null : value.name(), mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final int value, final boolean mandatory) throws IOException {
        writeEntry(key, Integer.toString(value), mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeRawData(final char data) throws IOException {
        output.append(data);
    }

    /** {@inheritDoc} */
    @Override
    public void writeRawData(final CharSequence data) throws IOException {
        append(data);
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        sections.offerLast(name);
    }

    /** {@inheritDoc} */
    @Override
    public String exitSection() throws IOException {
        return sections.pollLast();
    }

    /** Complain about a missing value.
     * @param key the keyword to write
     * @param mandatory if true, triggers en exception, otherwise do nothing
     */
    protected void complain(final String key, final boolean mandatory) {
        if (mandatory) {
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, key, fileName);
        }
    }

}