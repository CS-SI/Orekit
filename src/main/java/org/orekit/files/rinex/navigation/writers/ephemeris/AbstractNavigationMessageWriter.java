/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.files.rinex.navigation.writers.ephemeris;

import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.files.rinex.navigation.writers.NavigationMessageWriter;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.formatting.FastDecimalFormatter;
import org.orekit.utils.formatting.FastDoubleFormatter;
import org.orekit.utils.formatting.FastLongFormatter;
import org.orekit.utils.formatting.FastScientificFormatter;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Base writer for abstract navigation messages.
 * @param <T> type of the navigation messages this writer handles
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractNavigationMessageWriter<T extends AbstractNavigationMessage<T>>
    extends NavigationMessageWriter<T> {

    /** Format for one 2 digits integer field. */
    private static final FastLongFormatter TWO_DIGITS_INTEGER = new FastLongFormatter(2, false);

    /** Format for one 3 digits integer field. */
    private static final FastLongFormatter THREE_DIGITS_INTEGER = new FastLongFormatter(3, false);

    /** Format for one 5.1 float field. */
    private static final FastDoubleFormatter FIVE_ONE_FLOAT = new FastDecimalFormatter(5, 1);

    /** Format for one 19 float field. */
    private static final FastDoubleFormatter NINETEEN_FLOAT = new FastScientificFormatter(19);

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final T message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        if (header.getFormatVersion() >= 4.0) {
            // TYPE / SV / MSG
            writeTypeSvMsg(RecordType.EPH, identifier, message, header, writer);
        }

        // EPH MESSAGE LINE - 0
        writeEphLine0(message, identifier, header, writer);

        // EPH MESSAGE LINE - 1
        writeEphLine1(message, header, writer);

        // EPH MESSAGE LINE - 2
        writeEphLine2(message, header, writer);

        // EPH MESSAGE LINE - 3
        writeEphLine3(message, header, writer);

        // EPH MESSAGE LINE - 4
        writeEphLine4(message, header, writer);

        // EPH MESSAGE LINE - 5
        writeEphLine5(message, header, writer);

        // EPH MESSAGE LINE - 6
        writeEphLine6(message, header, writer);

        // EPH MESSAGE LINE - 7
        writeEphLine7(message, header, writer);

    }

    /** Write the EPH MESSAGE LINE - 0.
     * @param message navigation message to write
     * @param identifier identifier
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected void writeEphLine0(final AbstractNavigationMessage<?> message, final String identifier,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        if (header.getFormatVersion() < 3.0) {
            // Rinex 2 supports only Glonass and GPS
            final TimeScale ts = message.getSystem() == SatelliteSystem.GLONASS ?
                                 writer.getTimeScales().getGLONASS() :
                                 writer.getTimeScales().getGPS();
            final DateTimeComponents dtc = message.getEpochToc().getComponents(ts);
            writer.outputField(TWO_DIGITS_INTEGER, message.getPRN(),                2);
            writer.outputField(THREE_DIGITS_INTEGER, dtc.getDate().getYear() % 100, 5);
            writer.outputField(THREE_DIGITS_INTEGER, dtc.getDate().getMonth(),      8);
            writer.outputField(THREE_DIGITS_INTEGER, dtc.getDate().getDay(),       11);
            writer.outputField(THREE_DIGITS_INTEGER, dtc.getTime().getHour(),      14);
            writer.outputField(THREE_DIGITS_INTEGER, dtc.getTime().getMinute(),    17);
            writer.outputField(FIVE_ONE_FLOAT,       dtc.getTime().getSecond(),    22);
            writer.outputField(NINETEEN_FLOAT,       message.getAf0(),             41);
            writer.outputField(NINETEEN_FLOAT,       message.getAf1(),             60);
            writer.outputField(NINETEEN_FLOAT,       message.getAf2(),             79);
        } else {
            writer.outputField(identifier, 3, true);
            writer.outputField(' ', 4);
            writer.writeDate(message.getEpochToc(), message.getSystem());
            writer.writeDouble(message.getAf0(), Unit.SECOND);
            writer.writeDouble(message.getAf1(), RinexNavigationParser.S_PER_S);
            writer.writeDouble(message.getAf2(), RinexNavigationParser.S_PER_S2);
        }
        writer.finishLine();
    }

    /** Write the EPH MESSAGE LINE - 1.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected void writeEphLine1(final T message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writeField1Line1(message, writer);
        writer.writeDouble(message.getCrs(), Unit.METRE);
        writer.writeDouble(message.getDeltaN0(), RinexNavigationParser.RAD_PER_S);
        writer.writeDouble(message.getM0(), Unit.RADIAN);
        writer.finishLine();
    }

    /** Write field 1 in line 1.
     * @param message navigation message to write
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeField1Line1(T message, RinexNavigationWriter writer)
        throws IOException;

    /** Write the EPH MESSAGE LINE - 2.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected void writeEphLine2(final T message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getCuc(), Unit.RADIAN);
        writer.writeDouble(message.getE(), Unit.NONE);
        writer.writeDouble(message.getCus(), Unit.RADIAN);
        writer.writeDouble(message.getSqrtA(), RinexNavigationParser.SQRT_M);
        writer.finishLine();
    }

    /** Write the EPH MESSAGE LINE - 3.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected void writeEphLine3(final T message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getTime(), Unit.SECOND);
        writer.writeDouble(message.getCic(), Unit.RADIAN);
        writer.writeDouble(message.getOmega0(), Unit.RADIAN);
        writer.writeDouble(message.getCis(), Unit.RADIAN);
        writer.finishLine();
    }

    /** Write the EPH MESSAGE LINE - 4.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected void writeEphLine4(final T message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getI0(), Unit.RADIAN);
        writer.writeDouble(message.getCrc(), Unit.METRE);
        writer.writeDouble(message.getPa(), Unit.RADIAN);
        writer.writeDouble(message.getOmegaDot(), RinexNavigationParser.RAD_PER_S);
        writer.finishLine();
    }

    /** Write the EPH MESSAGE LINE - 5.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeEphLine5(T message,
                                          RinexNavigationHeader header, RinexNavigationWriter writer)
        throws IOException;

    /** Write the EPH MESSAGE LINE - 6.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeEphLine6(T message,
                                          RinexNavigationHeader header, RinexNavigationWriter writer)
        throws IOException;

    /** Write the EPH MESSAGE LINE - 7.
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeEphLine7(T message,
                                          RinexNavigationHeader header, RinexNavigationWriter writer)
        throws IOException;

}
