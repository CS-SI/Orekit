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
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for GPS civilian messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GPSCivilianNavigationMessageWriter
    extends NavigationMessageWriter<GPSCivilianNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final GPSCivilianNavigationMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(RecordType.EPH, identifier, message, header, writer);

        // EPH MESSAGE LINE - 0
        writeEphLine0( message, writer);

        // EPH MESSAGE LINE - 1
        writer.startLine();
        writer.writeDouble(message.getADot(), RinexNavigationParser.M_PER_S);
        writer.writeDouble(message.getCrs(), Unit.METRE);
        writer.writeDouble(message.getDeltaN0(), RinexNavigationParser.RAD_PER_S);
        writer.writeDouble(message.getM0(), Unit.RADIAN);
        writer.finishLine();

        // EPH MESSAGE LINE - 2
        writer.startLine();
        writer.writeDouble(message.getCuc(), Unit.RADIAN);
        writer.writeDouble(message.getE(), Unit.NONE);
        writer.writeDouble(message.getCus(), Unit.RADIAN);
        writer.writeDouble(message.getSqrtA(), RinexNavigationParser.SQRT_M);
        writer.finishLine();

        // EPH MESSAGE LINE - 3
        writer.startLine();
        writer.writeDouble(message.getTime(), Unit.SECOND);
        writer.writeDouble(message.getCic(), Unit.RADIAN);
        writer.writeDouble(message.getOmega0(), Unit.RADIAN);
        writer.writeDouble(message.getCis(), Unit.RADIAN);
        writer.finishLine();

        // EPH MESSAGE LINE - 4
        writer.startLine();
        writer.writeDouble(message.getI0(), Unit.RADIAN);
        writer.writeDouble(message.getCrc(), Unit.METRE);
        writer.writeDouble(message.getPa(), Unit.RADIAN);
        writer.writeDouble(message.getOmegaDot(), RinexNavigationParser.RAD_PER_S);
        writer.finishLine();

        // EPH MESSAGE LINE - 5
        writer.startLine();
        writer.writeDouble(message.getIDot(), RinexNavigationParser.RAD_PER_S);
        writer.writeDouble(message.getDeltaN0Dot(), RinexNavigationParser.RAD_PER_S2);
        writer.writeInt(message.getUraiNed0());
        writer.writeInt(message.getUraiNed1());
        writer.finishLine();

        // EPH MESSAGE LINE - 6
        writer.startLine();
        writer.writeInt(message.getUraiEd());
        writer.writeInt(message.getSvHealth());
        writer.writeDouble(message.getTGD(), Unit.SECOND);
        writer.writeInt(message.getUraiNed2());
        writer.finishLine();

        // EPH MESSAGE LINE - 7
        writer.startLine();
        writer.writeDouble(message.getIscL1CA(), Unit.SECOND);
        writer.writeDouble(message.getIscL2C(),  Unit.SECOND);
        writer.writeDouble(message.getIscL5I5(), Unit.SECOND);
        writer.writeDouble(message.getIscL5Q5(), Unit.SECOND);
        writer.finishLine();

        // EPH MESSAGE LINE - 8/9
        if (message.isCnv2()) {
            writer.startLine();
            writer.writeDouble(message.getIscL1CD(), Unit.SECOND);
            writer.writeDouble(message.getIscL1CP(), Unit.SECOND);
            writer.finishLine();
        }
        writer.startLine();
        writer.writeDouble(message.getTransmissionTime(), Unit.SECOND);
        writer.writeInt(message.getWeek());
        writer.writeInt(message.getFlags());
        writer.finishLine();

    }

}
