/* Copyright 2022-2025 Thales Alenia Space
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
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for SBAS messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SBASNavigationMessageWriter
    extends NavigationMessageWriter<SBASNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final SBASNavigationMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(RecordType.EPH, identifier, message, header, writer);

        // EPH MESSAGE LINE - 0
        writer.startLine();
        writer.writeDate(message.getEpochToc(), SatelliteSystem.SBAS);
        writer.writeDouble(message.getAGf0(), Unit.SECOND);
        writer.writeDouble(message.getAGf1(), RinexNavigationParser.S_PER_S);
        writer.writeDouble(message.getTime(), Unit.SECOND);
        writer.finishLine();


        // EPH MESSAGE LINE - 1
        writer.startLine();
        writer.writeDouble(message.getX(),       Unit.KILOMETRE);
        writer.writeDouble(message.getXDot(),    RinexNavigationParser.KM_PER_S);
        writer.writeDouble(message.getXDotDot(), RinexNavigationParser.KM_PER_S2);
        writer.writeDouble(message.getHealth(),  Unit.NONE);
        writer.finishLine();

        // EPH MESSAGE LINE - 2
        writer.startLine();
        writer.writeDouble(message.getY(),       Unit.KILOMETRE);
        writer.writeDouble(message.getYDot(),    RinexNavigationParser.KM_PER_S);
        writer.writeDouble(message.getYDotDot(), RinexNavigationParser.KM_PER_S2);
        writer.writeDouble(message.getURA(),     Unit.NONE);
        writer.finishLine();

        // EPH MESSAGE LINE - 3
        writer.startLine();
        writer.writeDouble(message.getZ(),       Unit.KILOMETRE);
        writer.writeDouble(message.getZDot(),    RinexNavigationParser.KM_PER_S);
        writer.writeDouble(message.getZDotDot(), RinexNavigationParser.KM_PER_S2);
        writer.writeDouble(message.getIODN(),    Unit.NONE);
        writer.finishLine();

    }

}
