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

import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.files.rinex.utils.BaseRinexWriter;
import org.orekit.propagation.analytical.gnss.data.GLONASSFdmaNavigationMessage;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.formatting.FastDecimalFormatter;
import org.orekit.utils.formatting.FastDoubleFormatter;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for GLONASS messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GlonassFdmaNavigationMessageWriter
    extends AbstractEphemerisMessageWriter<GLONASSFdmaNavigationMessage> {

    /** Format for one 5.1 digits float field. */
    public static final FastDoubleFormatter FIVE_ONE_DIGITS_FLOAT = new FastDecimalFormatter(5, 1);

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine0(final GLONASSFdmaNavigationMessage message, final String identifier,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        final DateTimeComponents dtc = message.getEpochToc().getComponents(writer.getTimeScales().getUTC());
        if (header.getFormatVersion() < 3.0) {

            writer.outputField(BaseRinexWriter.TWO_DIGITS_INTEGER, message.getPRN(), 2);
            writer.outputField(' ', 3);
            writer.outputField(BaseRinexWriter.TWO_DIGITS_INTEGER, dtc.getDate().getYear() % 100, 5);
            writer.outputField(' ', 6);
            writer.outputField(BaseRinexWriter.TWO_DIGITS_INTEGER, dtc.getDate().getMonth(), 8);
            writer.outputField(' ', 9);
            writer.outputField(BaseRinexWriter.TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 11);
            writer.outputField(' ', 12);
            writer.outputField(BaseRinexWriter.TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 14);
            writer.outputField(' ', 15);
            writer.outputField(BaseRinexWriter.TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 17);
            writer.outputField(FIVE_ONE_DIGITS_FLOAT, dtc.getTime().getSecond(), 22);

            writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, -message.getTN(),    41);
            writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getGammaN(), 60);
            writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, 0.0,                 79);

        } else {
            writer.outputField(identifier, 4, true);
            writer.writeDate(dtc);
            writer.writeDouble(-message.getTN(),    Unit.SECOND);
            writer.writeDouble(message.getGammaN(), Unit.NONE);
            writer.writeDouble(message.getTime(),   Unit.NONE);
        }

        writer.finishLine();

    }

    /** {@inheritDoc} */
    @Override
    protected void writeField4Line1(final GLONASSFdmaNavigationMessage message, final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getHealth(),  Unit.NONE);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeField4Line2(final GLONASSFdmaNavigationMessage message, final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getFrequencyNumber(), Unit.NONE);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeField4Line3(final GLONASSFdmaNavigationMessage message, final RinexNavigationWriter writer) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final GLONASSFdmaNavigationMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG, and lines 0 to 3
        super.writeMessage(identifier, message, header, writer);

        if (header.getFormatVersion() > 3.045) {
            // EPH MESSAGE LINE - 4
            writer.indentLine(header);
            writer.writeDouble(message.getStatusFlags(), Unit.NONE);
            writer.writeDouble(message.getGroupDelayDifference(), Unit.NONE);
            writer.writeDouble(message.getURA(), Unit.NONE);
            writer.writeDouble(message.getHealthFlags(), Unit.NONE);
            writer.finishLine();
        }

    }

}
