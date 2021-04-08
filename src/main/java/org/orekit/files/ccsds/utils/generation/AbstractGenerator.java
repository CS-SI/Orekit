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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.fraction.Fraction;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.ParseTree;
import org.orekit.utils.units.Parser;
import org.orekit.utils.units.PowerTerm;
import org.orekit.utils.units.Unit;

/** Base class for both Key-Value Notation and eXtended Markup Language generators for CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractGenerator implements Generator {

    /** New line separator for output file. */
    private static final char NEW_LINE = '\n';

    /** Destination of generated output. */
    private final Appendable output;

    /** Output name for error messages. */
    private final String outputName;

    /** Flag for writing units. */
    private final boolean writeUnits;

    /** Sections stack. */
    private final Deque<String> sections;

    /** Map from SI Units name to CCSDS unit names. */
    private final Map<String, String> siToCcsds;

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     * @param writeUnits if true, units must be written
     */
    public AbstractGenerator(final Appendable output, final String outputName, final boolean writeUnits) {
        this.output     = output;
        this.outputName = outputName;
        this.writeUnits = writeUnits;
        this.sections   = new ArrayDeque<>();
        this.siToCcsds  = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public String getOutputName() {
        return outputName;
    }

    /** Check if unit must be written.
     * @param unit entry unit
     * @return true if units must be written
     */
    protected boolean writeUnits(final Unit unit) {
        return writeUnits &&
               unit != null &&
               !unit.getName().equals(Unit.NONE.getName()) &&
               !unit.getName().equals(Unit.ONE.getName());
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
    public void writeEntry(final String key, final List<String> value, final boolean mandatory) throws IOException {
        if (value == null || value.isEmpty()) {
            complain(key, mandatory);
        } else {
            final StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (final String v : value) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(v);
                first = false;
            }
            writeEntry(key, builder.toString(), null, mandatory);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final Enum<?> value, final boolean mandatory) throws IOException {
        writeEntry(key, value == null ? null : value.name(), null, mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final TimeConverter converter, final AbsoluteDate date, final boolean mandatory)
        throws IOException {
        writeEntry(key, date == null ? (String) null : dateToString(converter, date), null, mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final double value, final Unit unit, final boolean mandatory) throws IOException {
        writeEntry(key, doubleToString(unit.fromSI(value)), unit, mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final Double value, final Unit unit, final boolean mandatory) throws IOException {
        writeEntry(key, value == null ? (String) null : doubleToString(unit.fromSI(value.doubleValue())), unit, mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final char value, final boolean mandatory) throws IOException {
        writeEntry(key, Character.toString(value), null, mandatory);
    }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(final String key, final int value, final boolean mandatory) throws IOException {
        writeEntry(key, Integer.toString(value), null, mandatory);
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
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD, key, outputName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String doubleToString(final double value) {
        return Double.isNaN(value) ? null : AccurateFormatter.format(value);
    }

    /** {@inheritDoc} */
    @Override
    public String dateToString(final TimeConverter converter, final AbsoluteDate date) {
        final DateTimeComponents dt = converter.components(date);
        return dateToString(dt.getDate().getYear(), dt.getDate().getMonth(), dt.getDate().getDay(),
                            dt.getTime().getHour(), dt.getTime().getMinute(), dt.getTime().getSecond());
    }

    /** {@inheritDoc} */
    @Override
    public String dateToString(final int year, final int month, final int day,
                               final int hour, final int minute, final double seconds) {
        return AccurateFormatter.format(year, month, day, hour, minute, seconds);
    }

    /** {@inheritDoc} */
    @Override
    public String unitsListToString(final List<Unit> units) {

        if (units == null || units.isEmpty()) {
            // nothing to output
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean first = true;
        for (final Unit unit : units) {
            if (!first) {
                builder.append(',');
            }
            builder.append(siToCcsdsName(unit.getName()));
            first = false;
        }
        builder.append(']');
        return builder.toString();

    }

    /** Convert a SI unit name to a CCSDS name.
     * @param siName si unit name
     * @return CCSDS name for the unit
     */
    protected String siToCcsdsName(final String siName) {

        if (!siToCcsds.containsKey(siName)) {

            // build a name using only CCSDS syntax
            final StringBuilder builder = new StringBuilder();

            // parse the SI name that may contain fancy features like unicode superscripts, square roots sign…
            final ParseTree tree = Parser.buildTree(siName);

            if (tree == null) {
                builder.append("n/a");
            } else {
                if (tree.getFactor() != 1) {
                    builder.append(tree.getFactor());
                }
                boolean first = true;
                for (final PowerTerm term : tree.getTerms()) {
                    String   base = term.getBase().toString();
                    Fraction e    = term.getExponent();
                    if (!first) {
                        if (e.getNumerator() < 0) {
                            builder.append('/');
                            e = e.negate();
                        } else {
                            builder.append('*');
                        }
                    }
                    if ("°".equals(base) || "◦".equals(base)) {
                        base = "deg";
                    }
                    builder.append(base);
                    if (!e.equals(Fraction.ONE)) {
                        builder.append("**");
                        builder.append(e.equals(Fraction.ONE_HALF) ? "0.5" : e);
                    }
                    first = false;
                }
            }

            // put the converted name in the map for reuse
            siToCcsds.put(siName, builder.toString());

        }

        return siToCcsds.get(siName);

    }

}
