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

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.TimeScale;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for SBAS messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SBASNavigationMessageWriter
    extends AbstractEphemerisMessageWriter<SBASNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    protected void writeEphLine0(final SBASNavigationMessage message, final String identifier,
                                 final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {
        writer.outputField(identifier, 4, true);

        // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
        final int version100 = (int) FastMath.rint(header.getFormatVersion() * 100);
        final TimeScale timeScale = (version100 == 301) ?
                                    writer.getTimeScales().getUTC() :
                                    writer.getTimeScales().getGPS();
        writer.writeDate(message.getEpochToc().getComponents(timeScale));
        writer.writeDouble(message.getAGf0(), Unit.SECOND);
        writer.writeDouble(message.getAGf1(), RinexNavigationParser.S_PER_S);
        writer.writeDouble(message.getTime(), Unit.SECOND);
        writer.finishLine();
    }

        /** {@inheritDoc} */
    @Override
    protected void writeField4Line1(final SBASNavigationMessage message, final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getHealth(),  Unit.NONE);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeField4Line2(final SBASNavigationMessage message, final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getURA(),     Unit.NONE);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeField4Line3(final SBASNavigationMessage message, final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getIODN(),    Unit.NONE);
    }

}
