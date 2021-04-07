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

import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Generator for eXtended Markup Language CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class XmlGenerator extends AbstractGenerator {

    /** Default number of space for each indentation level. */
    public static final int DEFAULT_INDENT = 2;

    /** XML prolog. */
    private static final String PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n";

    /** Root element start tag. */
    private static final String ROOT_START = "<%s id=\"%s\" version=\"%.1f\">%n";

    /** Element end tag. */
    private static final String START_TAG = "<%s>%n";

    /** Element end tag. */
    private static final String END_TAG = "</%s>%n";

    /** Leaf element format without units. */
    private static final String LEAF_WITHOUT_UNITS = "<%s>%s</%s>%n";

    /** Leaf element format with units. */
    private static final String LEAF_WITH_UNITS = "<%s units=\"%s\">%s</%s>%n";

    /** User defined parameter element format. */
    private static final String USER_DEFINED = "<%s %s=\"%s\">%s</%s>%n";

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
     * @param writeUnits if true, units must be written
     * @see #DEFAULT_INDENT
     */
    public XmlGenerator(final Appendable output, final int indentation,
                        final String outputName, final boolean writeUnits) {
        super(output, outputName, writeUnits);
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
    public void startMessage(final String root, final String messageTypeKey, final double version) throws IOException {
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, PROLOG));
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, ROOT_START,
                                   root, messageTypeKey, version));
        ++level;
    }

    /** {@inheritDoc} */
    @Override
    public void endMessage(final String root) throws IOException {
        --level;
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, END_TAG,
                                   root));
    }

    /** {@inheritDoc} */
    @Override
    public void writeComments(final List<String> comments) throws IOException {
        for (final String comment : comments) {
            writeEntry(COMMENT, comment, null, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeUserDefined(final String parameter, final String value) throws IOException {
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, USER_DEFINED,
                                   UserDefined.USER_DEFINED_XML_TAG,
                                   UserDefined.USER_DEFINED_XML_ATTRIBUTE,
                                   parameter,
                                   value,
                                   UserDefined.USER_DEFINED_XML_TAG));
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final String value, final Unit unit, final boolean mandatory) throws IOException {
        if (value == null) {
            complain(key, mandatory);
        } else {
            indent();
            if (writeUnits(unit)) {
                writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF_WITH_UNITS,
                                           key, siToCcsdsName(unit.getName()), value, key));
            } else {
                writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF_WITHOUT_UNITS,
                                           key, value, key));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, START_TAG, name));
        ++level;
        super.enterSection(name);
    }

    /** {@inheritDoc} */
    @Override
    public String exitSection() throws IOException {
        final String name = super.exitSection();
        --level;
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, END_TAG, name));
        return name;
    }

    /** Indent line.
     * @throws IOException if an I/O error occurs.
     */
    private void indent() throws IOException {
        for (int i = 0; i < level * indentation; ++i) {
            writeRawData(' ');
        }
    }

}
