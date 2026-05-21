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
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for Beidou civilian messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouCivilianNavigationMessageWriter
    extends AbstractNavigationMessageWriter<BeidouCivilianNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    protected void writeField1Line1(final BeidouCivilianNavigationMessage message,
                                    final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getADot(), RinexNavigationParser.M_PER_S);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine5(final BeidouCivilianNavigationMessage message,
                                 final RinexNavigationHeader header,
                                 final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getIDot(), RinexNavigationParser.RAD_PER_S);
        writer.writeDouble(message.getDeltaN0Dot(), RinexNavigationParser.RAD_PER_S2);
        writer.writeInt(message.getSatelliteType().getIntegerId());
        writer.writeDouble(message.getTime(), Unit.SECOND);
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine6(final BeidouCivilianNavigationMessage message,
                                 final RinexNavigationHeader header,
                                 final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeInt(message.getSisaiOe());
        writer.writeInt(message.getSisaiOcb());
        writer.writeInt(message.getSisaiOc1());
        writer.writeInt(message.getSisaiOc2());
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine7(final BeidouCivilianNavigationMessage message,
                                 final RinexNavigationHeader header,
                                 final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        if (BeidouCivilianNavigationMessage.CNV1.equals(message.getNavigationMessageType())) {
            writer.writeDouble(message.getIscB1CD(), Unit.SECOND);
            writer.writeEmpty();
            writer.writeDouble(message.getTgdB1Cp(), Unit.SECOND);
            writer.writeDouble(message.getTgdB2ap(), Unit.SECOND);
        } else if (BeidouCivilianNavigationMessage.CNV2.equals(message.getNavigationMessageType())) {
            writer.writeEmpty();
            writer.writeDouble(message.getIscB2AD(), Unit.SECOND);
            writer.writeDouble(message.getTgdB1Cp(), Unit.SECOND);
            writer.writeDouble(message.getTgdB2ap(), Unit.SECOND);
        } else {
            writeSismaiHealthIntegrity(message, writer);
        }
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final BeidouCivilianNavigationMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG, and lines 0 to 7
        super.writeMessage(identifier, message, header, writer);

        if (BeidouCivilianNavigationMessage.CNV3.equals(message.getNavigationMessageType())) {
            // EPH MESSAGE LINE - 8
            writer.indentLine(header);
            writer.writeDouble(message.getTransmissionTime(), Unit.SECOND);
            writer.finishLine();
        } else {

            // EPH MESSAGE LINE - 8
            writer.indentLine(header);
            writeSismaiHealthIntegrity(message, writer);
            writer.finishLine();

            // EPH MESSAGE LINE - 9
            writer.indentLine(header);
            writer.writeDouble(message.getTransmissionTime(), Unit.SECOND);
            writer.writeEmpty();
            writer.writeEmpty();
            writer.writeInt(message.getIODE());
            writer.finishLine();

        }

    }

    /** Write the SISMAI/Health/integrity line.
     * @param message navigation message to write
     * @param writer global file writer
     * @exception  IOException if an I/O error occurs.
     */
    private void writeSismaiHealthIntegrity(final BeidouCivilianNavigationMessage message,
                                            final RinexNavigationWriter writer) throws IOException {
        writer.writeInt(message.getSismai());
        writer.writeInt(message.getHealth());
        writer.writeInt(message.getIntegrityFlags());
        writer.writeInt(message.getIODC());
    }

}
