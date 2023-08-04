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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.AccurateFormatter;
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

    /** Maximum offset for relative dates.
     * @since 12.0
     */
    private final double maxRelativeOffset;

    /** Flag for writing units. */
    private final boolean writeUnits;

    /** Sections stack. */
    private final Deque<String> sections;

    /** Map from SI Units name to CCSDS unit names. */
    private final Map<String, String> siToCcsds;

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     * @param maxRelativeOffset maximum offset in seconds to use relative dates
     * (if a date is too far from reference, it will be displayed as calendar elements)
     * @param writeUnits if true, units must be written
     */
    public AbstractGenerator(final Appendable output, final String outputName,
                             final double maxRelativeOffset, final boolean writeUnits) {
        this.output            = output;
        this.outputName        = outputName;
        this.maxRelativeOffset = maxRelativeOffset;
        this.writeUnits        = writeUnits;
        this.sections          = new ArrayDeque<>();
        this.siToCcsds         = new HashMap<>();
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
    public boolean writeUnits(final Unit unit) {
        return writeUnits &&
               unit != null &&
               !unit.getName().equals(Unit.NONE.getName()) &&
               !unit.getName().equals(Unit.ONE.getName());
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {

        // get out from all sections properly
        while (!sections.isEmpty()) {
            exitSection();
        }

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
    public void writeEntry(final String key, final TimeConverter converter, final AbsoluteDate date,
                           final boolean forceCalendar, final boolean mandatory)
        throws IOException {
        if (date == null) {
            writeEntry(key, (String) null, null, mandatory);
        } else {
            writeEntry(key,
                       forceCalendar ? dateToCalendarString(converter, date) : dateToString(converter, date),
                       null,
                       mandatory);
        }
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

        if (converter.getReferenceDate() != null) {
            final double relative = date.durationFrom(converter.getReferenceDate());
            if (FastMath.abs(relative) <= maxRelativeOffset) {
                // we can use a relative date
                return AccurateFormatter.format(relative);
            }
        }

        // display the date as calendar elements
        return dateToCalendarString(converter, date);

    }

    /** {@inheritDoc} */
    @Override
    public String dateToCalendarString(final TimeConverter converter, final AbsoluteDate date) {
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

    /** {@inheritDoc} */
    @Override
    public String siToCcsdsName(final String siName) {

        if (!siToCcsds.containsKey(siName)) {

            // build a name using only CCSDS syntax
            final StringBuilder builder = new StringBuilder();

            // parse the SI name that may contain fancy features like unicode superscripts, square roots sign…
            final List<PowerTerm> terms = Parser.buildTermsList(siName);

            if (terms == null) {
                builder.append("n/a");
            } else {

                // put the positive exponent first
                boolean first = true;
                for (final PowerTerm term : terms) {
                    if (term.getExponent().getNumerator() >= 0) {
                        if (!first) {
                            builder.append('*');
                        }
                        appendScale(builder, term.getScale());
                        appendBase(builder, term.getBase());
                        appendExponent(builder, term.getExponent());
                        first = false;
                    }
                }

                if (first) {
                    // no positive exponents at all, we add "1" to get something like "1/s"
                    builder.append('1');
                }

                // put the negative exponents last
                for (final PowerTerm term : terms) {
                    if (term.getExponent().getNumerator() < 0) {
                        builder.append('/');
                        appendScale(builder, term.getScale());
                        appendBase(builder, term.getBase());
                        appendExponent(builder, term.getExponent().negate());
                    }
                }

            }

            // put the converted name in the map for reuse
            siToCcsds.put(siName, builder.toString());

        }

        return siToCcsds.get(siName);

    }

    /** Append a scaling factor.
     * @param builder builder to which term must be added
     * @param scale scaling factor
     */
    private void appendScale(final StringBuilder builder, final double scale) {
        final int factor = (int) FastMath.rint(scale);
        if (FastMath.abs(scale - factor) > 1.0e-12) {
            // this should never happen with CCSDS units
            throw new OrekitInternalError(null);
        }
        if (factor != 1) {
            builder.append(factor);
        }
    }

    /** Append a base term.
     * @param builder builder to which term must be added
     * @param base base term
     */
    private void appendBase(final StringBuilder builder, final CharSequence base) {
        if ("°".equals(base) || "◦".equals(base)) {
            builder.append("deg");
        } else {
            builder.append(base);
        }
    }

    /** Append an exponent.
     * @param builder builder to which term must be added
     * @param exponent exponent to add
     */
    private void appendExponent(final StringBuilder builder, final Fraction exponent) {
        if (!exponent.equals(Fraction.ONE)) {
            builder.append("**");
            if (exponent.equals(Fraction.ONE_HALF)) {
                builder.append("0.5");
            } else if (exponent.getNumerator() == 3 && exponent.getDenominator() == 2) {
                builder.append("1.5");
            } else {
                builder.append(exponent);
            }
        }
    }

}

