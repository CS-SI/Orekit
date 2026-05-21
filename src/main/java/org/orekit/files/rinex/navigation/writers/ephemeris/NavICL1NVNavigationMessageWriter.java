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
import org.orekit.propagation.analytical.gnss.data.NavICL1NvNavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for NavIC L1NV messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICL1NVNavigationMessageWriter
    extends CivilianNavigationMessageWriter<NavICL1NvNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final NavICL1NvNavigationMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG, and lines 0 to 7
        super.writeMessage(identifier, message, header, writer);

        // EPH MESSAGE LINE - 8
        writer.indentLine(header);
        writer.writeDouble(message.getTransmissionTime(), Unit.SECOND);
        writer.writeInt(message.getWeek());
        writer.finishLine();

    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine5(final NavICL1NvNavigationMessage message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getIDot(), RinexNavigationParser.RAD_PER_S);
        writer.writeDouble(message.getDeltaN0Dot(), RinexNavigationParser.RAD_PER_S2);
        writer.writeEmpty();
        writer.writeInt(message.getReferenceSignalFlag());
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine6(final NavICL1NvNavigationMessage message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);

        writer.writeInt(message.getUrai());
        writer.writeInt(message.getL1SpsHealth());
        writer.writeDouble(message.getTGD(),    Unit.SECOND);
        writer.writeDouble(message.getTGDSL5(), Unit.SECOND);
        writer.finishLine();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine7(final NavICL1NvNavigationMessage message,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.indentLine(header);
        writer.writeDouble(message.getIscSL1P(),   Unit.SECOND);
        writer.writeDouble(message.getIscL1DL1P(), Unit.SECOND);
        writer.writeDouble(message.getIscL1PS(),   Unit.SECOND);
        writer.writeDouble(message.getIscL1DS(),   Unit.SECOND);
        writer.finishLine();
    }

}
