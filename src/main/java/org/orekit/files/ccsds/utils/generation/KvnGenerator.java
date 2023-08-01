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

import org.hipparchus.util.FastMath;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Generator for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KvnGenerator extends AbstractGenerator {

    /** Comment keyword. */
    private static final String COMMENT = "COMMENT";

    /** Start suffix for sections. */
    private static final String START = "_START";

    /** Stop suffix for sections. */
    private static final String STOP = "_STOP";

    /** String format used for all key/value pair lines. **/
    private final String kvFormat;

    /** Column number for aligning units. */
    private final int unitsColumn;

    /** String format used for all comment lines. **/
    private final String commentFormat;

    /** Simple constructor.
     * @param output destination of generated output
     * @param paddingWidth padding width for aligning the '=' sign
     * (not counting the extra blank added before the '=' sign)
     * @param outputName output name for error messages
     * @param maxRelativeOffset maximum offset in seconds to use relative dates
     * (if a date is too far from reference, it will be displayed as calendar elements)
     * @param unitsColumn columns number for aligning units (if negative or zero, units are not output)
     * @see org.orekit.files.ccsds.ndm.tdm.TdmWriter#KVN_PADDING_WIDTH     TdmWriter.KVN_PADDING_WIDTH
     * @see org.orekit.files.ccsds.ndm.adm.aem.AemWriter#KVN_PADDING_WIDTH AemWriter.KVN_PADDING_WIDTH
     * @see org.orekit.files.ccsds.ndm.adm.apm.ApmWriter#KVN_PADDING_WIDTH ApmWriter.KVN_PADDING_WIDTH
     * @see org.orekit.files.ccsds.ndm.odm.opm.OpmWriter#KVN_PADDING_WIDTH OpmWriter.KVN_PADDING_WIDTH
     * @see org.orekit.files.ccsds.ndm.odm.omm.OmmWriter#KVN_PADDING_WIDTH OmmWriter.KVN_PADDING_WIDTH
     * @see org.orekit.files.ccsds.ndm.odm.oem.OemWriter#KVN_PADDING_WIDTH OemWriter.KVN_PADDING_WIDTH
     * @see org.orekit.files.ccsds.ndm.odm.ocm.OcmWriter#KVN_PADDING_WIDTH OcmWriter.KVN_PADDING_WIDTH
     */
    public KvnGenerator(final Appendable output, final int paddingWidth,
                        final String outputName, final double maxRelativeOffset,
                        final int unitsColumn) {
        super(output, outputName, maxRelativeOffset, unitsColumn > 0);
        kvFormat = "%-" + FastMath.max(1, paddingWidth) + "s = %s";
        final StringBuilder builder = new StringBuilder(COMMENT);
        builder.append(' ');
        while (builder.length() < paddingWidth + 3) {
            builder.append(' ');
        }
        builder.append("%s%n");
        this.unitsColumn   = unitsColumn;
        this.commentFormat = builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public FileFormat getFormat() {
        return FileFormat.KVN;
    }

    /** {@inheritDoc} */
    @Override
    public void startMessage(final String root, final String messageTypeKey, final double version) throws IOException {
        writeEntry(messageTypeKey, String.format(AccurateFormatter.STANDARDIZED_LOCALE, "%.1f", version), null, true);
    }

    /** {@inheritDoc} */
    @Override
    public void endMessage(final String root) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void writeComments(final List<String> comments) throws IOException {
        for (final String comment : comments) {
            writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, commentFormat, comment));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final String value, final Unit unit, final boolean mandatory) throws IOException {
        if (value == null) {
            complain(key, mandatory);
        } else {
            final String s = String.format(AccurateFormatter.STANDARDIZED_LOCALE, kvFormat, key, value);
            writeRawData(s);
            if (writeUnits(unit)) {
                for (int column = s.length(); column < unitsColumn; ++column) {
                    writeRawData(' ');
                }
                writeRawData('[');
                writeRawData(siToCcsdsName(unit.getName()));
                writeRawData(']');
            }
            newLine();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enterSection(final String name) throws IOException {
        writeRawData(name);
        writeRawData(START);
        newLine();
        super.enterSection(name);
    }

    /** {@inheritDoc} */
    @Override
    public String exitSection() throws IOException {
        final String name = super.exitSection();
        writeRawData(name);
        writeRawData(STOP);
        newLine();
        return name;
    }

}
