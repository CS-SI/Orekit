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
import org.orekit.propagation.analytical.gnss.data.AbstractEphemerisMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for ephemeris messages.
 * @param <T> type of the ephemeris messages this writer handles
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractEphemerisMessageWriter<T extends AbstractEphemerisMessage>
    extends NavigationMessageWriter<T> {

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
        writer.indentLine(header);
        writer.writeDouble(message.getX(),       Unit.KILOMETRE);
        writer.writeDouble(message.getXDot(),    RinexNavigationParser.KM_PER_S);
        writer.writeDouble(message.getXDotDot(), RinexNavigationParser.KM_PER_S2);
        writeField4Line1(message, writer);
        writer.finishLine();

        // EPH MESSAGE LINE - 2
        writer.indentLine(header);
        writer.writeDouble(message.getY(),       Unit.KILOMETRE);
        writer.writeDouble(message.getYDot(),    RinexNavigationParser.KM_PER_S);
        writer.writeDouble(message.getYDotDot(), RinexNavigationParser.KM_PER_S2);
        writeField4Line2(message, writer);
        writer.finishLine();

        // EPH MESSAGE LINE - 3
        writer.indentLine(header);
        writer.writeDouble(message.getZ(),       Unit.KILOMETRE);
        writer.writeDouble(message.getZDot(),    RinexNavigationParser.KM_PER_S);
        writer.writeDouble(message.getZDotDot(), RinexNavigationParser.KM_PER_S2);
        writeField4Line3(message, writer);
        writer.finishLine();

    }

    /** Write the EPH MESSAGE LINE - 0.
     * @param message navigation message to write
     * @param identifier identifier
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeEphLine0(T message, String identifier,
                                          RinexNavigationHeader header, RinexNavigationWriter writer)
        throws IOException;

    /** Write field 4 in line 1.
     * @param message navigation message to write
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeField4Line1(T message, RinexNavigationWriter writer)
        throws IOException;

    /** Write field 4 in line 2.
     * @param message navigation message to write
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeField4Line2(T message, RinexNavigationWriter writer)
        throws IOException;

    /** Write field 4 in line 3.
     * @param message navigation message to write
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeField4Line3(T message, RinexNavigationWriter writer)
        throws IOException;

}
