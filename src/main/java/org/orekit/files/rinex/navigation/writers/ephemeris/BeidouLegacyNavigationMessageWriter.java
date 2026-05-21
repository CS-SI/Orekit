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
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for Beidou legacy messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouLegacyNavigationMessageWriter
    extends AbstractNavigationMessageWriter<BeidouLegacyNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    protected void writeField1Line1(final BeidouLegacyNavigationMessage message,
                                    final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getAODE(), Unit.SECOND);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine5(final BeidouLegacyNavigationMessage message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getIDot(), RinexNavigationParser.RAD_PER_S);
        writer.writeEmpty();
        writer.writeInt(message.getWeek());
        writer.writeEmpty();
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine6(final BeidouLegacyNavigationMessage message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getSvAccuracy(), Unit.METRE);
        writer.writeInt(message.getSatH1());
        writer.writeDouble(message.getTGD1(), Unit.SECOND);
        writer.writeDouble(message.getTGD2(), Unit.SECOND);
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine7(final BeidouLegacyNavigationMessage message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getTransmissionTime(), Unit.SECOND);
        writer.writeDouble(message.getAODC(),             Unit.SECOND);
        writer.finishLine();
    }

}
