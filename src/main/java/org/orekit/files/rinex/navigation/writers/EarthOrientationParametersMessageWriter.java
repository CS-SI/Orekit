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
package org.orekit.files.rinex.navigation.writers;

import org.orekit.files.rinex.navigation.EarthOrientationParameterMessage;
import org.orekit.files.rinex.navigation.MessageType;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for Earth Orientation Parameters messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class EarthOrientationParametersMessageWriter
    extends NavigationMessageWriter<EarthOrientationParameterMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final EarthOrientationParameterMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(MessageType.EOP, identifier, message, header, writer);

        // EOP MESSAGE LINE - 0
        writer.writeDate(message.getReferenceEpoch(), message.getSystem());
        writer.writeField(message.getXp(),       Unit.ARC_SECOND);
        writer.writeField(message.getXpDot(),    RinexNavigationParser.AS_PER_DAY);
        writer.writeField(message.getXpDotDot(), RinexNavigationParser.AS_PER_DAY2);
        writer.finishLine();

        // EOP MESSAGE LINE - 1
        writer.startLine();
        writer.outputField(' ', 23);
        writer.writeField(message.getYp(),       Unit.ARC_SECOND);
        writer.writeField(message.getYpDot(),    RinexNavigationParser.AS_PER_DAY);
        writer.writeField(message.getYpDotDot(), RinexNavigationParser.AS_PER_DAY2);
        writer.finishLine();

        // EOP MESSAGE LINE - 2
        writer.startLine();
        writer.writeField(message.getTransmissionTime(), Unit.SECOND);
        writer.writeField(message.getDut1(),             Unit.SECOND);
        writer.writeField(message.getDut1Dot(),          RinexNavigationParser.S_PER_S);
        writer.writeField(message.getDut1DotDot(),       RinexNavigationParser.S_PER_S2);
        writer.finishLine();

    }

}
