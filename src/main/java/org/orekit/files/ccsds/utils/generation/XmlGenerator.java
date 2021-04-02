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
import java.util.List;

import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.utils.AccurateFormatter;

/** Generator for eXtended Markup Language CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class XmlGenerator extends AbstractGenerator {

    /** Default number of space for each indentation level. */
    public static final int DEFAULT_INDENT = 2;

    /** XML prolog. */
    private static final String PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /** Root element start tag. */
    private static final String ROOT_START = "<%s id=\"%s\" version=\"%.1f\">";

    /** Element end tag. */
    private static final String START_TAG = "<%s>";

    /** Element end tag. */
    private static final String END_TAG = "</%s>";

    /** Leaf element format. */
    private static final String LEAF = "<%s>%s</%s>";

    /** Comment key. */
    private static final String COMMENT = "COMMENT";

    /** Indentation size. */
    private final int indentation;

    /** Nesting level. */
    private int level;

    /** Simple constructor.
     * @param output destination of generated output
     * @param indentation number of space for each indentation level
     * @param outputName output name for error messages
     * @see #DEFAULT_INDENT
     */
    public XmlGenerator(final Appendable output, final int indentation, final String outputName) {
        super(output, outputName);
        this.indentation = indentation;
        this.level       = 0;
    }

    /** {@inheritDoc} */
    @Override
    public FileFormat getFormat() {
        return FileFormat.XML;
    }

    /** {@inheritDoc} */
    @Override
    public void startMessage(final String messageTypeKey, final double version) throws IOException {
        writeRawData(PROLOG);
        newLine();
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, ROOT_START,
                                   messageTypeKey, messageTypeKey, version));
        newLine();
        ++level;
    }

    /** {@inheritDoc} */
    @Override
    public void endMessage(final String messageTypeKey) throws IOException {
        --level;
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, END_TAG,
                                   messageTypeKey));
        newLine();
    }

    /** {@inheritDoc} */
    @Override
    public void writeComments(final List<String> comments) throws IOException {
        for (final String comment : comments) {
            writeEntry(COMMENT, comment, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final String value, final boolean mandatory) throws IOException {
        if (value == null) {
            complain(key, mandatory);
        } else {
            indent();
            append(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF, key, value, key));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        indent();
        append(String.format(AccurateFormatter.STANDARDIZED_LOCALE, START_TAG, name));
        ++level;
        super.enterSection(name);
    }

    /** {@inheritDoc} */
    @Override
    public String exitSection() throws IOException {
        final String name = super.exitSection();
        --level;
        indent();
        append(String.format(AccurateFormatter.STANDARDIZED_LOCALE, END_TAG, name));
        return name;
    }

    /** Indent line.
     */
    private void indent() throws IOException {
        for (int i = 0; i < level * indentation; ++i) {
            writeRawData(' ');
        }
    }

}
