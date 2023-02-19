/* Copyright 2002-2023 CS GROUP
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
import org.orekit.utils.units.Unit;

/** Generator for eXtended Markup Language CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class XmlGenerator extends AbstractGenerator {

    /** Default number of space for each indentation level. */
    public static final int DEFAULT_INDENT = 2;

    /** Name of the units attribute. */
    public static final String UNITS = "units";

    /** NDM/XML version 3 location.
     * @since 12.0
     */
    public static final String NDM_XML_V3_SCHEMA_LOCATION = "https://sanaregistry.org/r/ndmxml_unqualified/ndmxml-3.0.0-master-3.0.xsd";

    /** XML prolog. */
    private static final String PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n";

    /** Root element start tag, with schema.
     * @since 12.0
     */
    private static final String ROOT_START_WITH_SCHEMA = "<%s xmlns:xsi=\"%s\" xsi:noNamespaceSchemaLocation=\"%s\" id=\"%s\" version=\"%.1f\">%n";

    /** Root element start tag, without schema.
     * @since 12.0
     */
    private static final String ROOT_START_WITHOUT_SCHEMA = "<%s id=\"%s\" version=\"%.1f\">%n";

    /** Constant for XML schema instance.
     * @since 12.0
     */
    private static final String XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /** Element end tag.
     * @since 12.0
     */
    private static final String START_TAG_WITH_SCHEMA = "<%s xmlns:xsi=\"%s\" xsi:noNamespaceSchemaLocation=\"%s\">%n";

    /** Element end tag.
     * @since 12.0
     */
    private static final String START_TAG_WITHOUT_SCHEMA = "<%s>%n";

    /** Element end tag. */
    private static final String END_TAG = "</%s>%n";

    /** Leaf element format without attributes. */
    private static final String LEAF_0_ATTRIBUTES = "<%s>%s</%s>%n";

    /** Leaf element format with one attribute. */
    private static final String LEAF_1_ATTRIBUTE = "<%s %s=\"%s\">%s</%s>%n";

    /** Leaf element format with two attributes. */
    private static final String LEAF_2_ATTRIBUTES = "<%s %s=\"%s\" %s=\"%s\">%s</%s>%n";

    /** Comment key. */
    private static final String COMMENT = "COMMENT";

    /** Schema location. */
    private final String schemaLocation;

    /** Indentation size. */
    private final int indentation;

    /** Nesting level. */
    private int level;

    /** Simple constructor.
     * @param output destination of generated output
     * @param indentation number of space for each indentation level
     * @param outputName output name for error messages
     * @param maxRelativeOffset maximum offset in seconds to use relative dates
     * (if a date is too far from reference, it will be displayed as calendar elements)
     * @param writeUnits if true, units must be written
     * @param schemaLocation schema location to use, may be null
     * @see #DEFAULT_INDENT
     * @see #NDM_XML_V3_SCHEMA_LOCATION
     * @throws IOException if an I/O error occurs.
     */
    public XmlGenerator(final Appendable output, final int indentation,
                        final String outputName, final double maxRelativeOffset,
                        final boolean writeUnits, final String schemaLocation) throws IOException {
        super(output, outputName, maxRelativeOffset, writeUnits);
        this.schemaLocation = schemaLocation;
        this.indentation    = indentation;
        this.level          = 0;
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, PROLOG));
    }

    /** {@inheritDoc} */
    @Override
    public FileFormat getFormat() {
        return FileFormat.XML;
    }

    /** {@inheritDoc} */
    @Override
    public void startMessage(final String root, final String messageTypeKey, final double version) throws IOException {
        indent();
        if (schemaLocation == null || level > 0) {
            writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, ROOT_START_WITHOUT_SCHEMA,
                                       root, messageTypeKey, version));
        } else {
            writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, ROOT_START_WITH_SCHEMA,
                                       root, XMLNS_XSI, schemaLocation, messageTypeKey, version));
        }
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

    /** Write an element with one attribute.
     * @param name tag name
     * @param value element value
     * @param attributeName attribute name
     * @param attributeValue attribute value
     * @throws IOException if an I/O error occurs.
     */
    public void writeOneAttributeElement(final String name, final String value,
                                         final String attributeName, final String attributeValue)
        throws IOException {
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF_1_ATTRIBUTE,
                                   name, attributeName, attributeValue, value, name));
    }

    /** Write an element with two attributes.
     * @param name tag name
     * @param value element value
     * @param attribute1Name attribute 1 name
     * @param attribute1Value attribute 1 value
     * @param attribute2Name attribute 2 name
     * @param attribute2Value attribute 2 value
     * @throws IOException if an I/O error occurs.
     */
    public void writeTwoAttributesElement(final String name, final String value,
                                          final String attribute1Name, final String attribute1Value,
                                          final String attribute2Name, final String attribute2Value)
        throws IOException {
        indent();
        writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF_2_ATTRIBUTES,
                                   name,
                                   attribute1Name, attribute1Value, attribute2Name, attribute2Value,
                                   value, name));
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final String value, final Unit unit, final boolean mandatory) throws IOException {
        if (value == null) {
            complain(key, mandatory);
        } else {
            indent();
            if (writeUnits(unit)) {
                writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF_1_ATTRIBUTE,
                                           key, UNITS, siToCcsdsName(unit.getName()), value, key));
            } else {
                writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, LEAF_0_ATTRIBUTES,
                                           key, value, key));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        indent();
        if (schemaLocation != null && level == 0) {
            // top level tag for ndm messages (it is called before enterMessage)
            writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, START_TAG_WITH_SCHEMA,
                                       name, XMLNS_XSI, schemaLocation));
        } else {
            writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, START_TAG_WITHOUT_SCHEMA, name));
        }
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
