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
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.files.rinex.utils.BaseRinexWriter;

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
        writeTypeSvMsg(MessageType.EOP, identifier, message, writer);

        // EOP MESSAGE LINE - 0
        writer.writeDate(message.getReferenceEpoch(), message.getSystem());
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getXp(),       42);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getXpDot(),    61);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getXpDotDot(), 80);
        writer.finishLine();

        // EOP MESSAGE LINE - 1
        writer.outputField(' ', 23);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getYp(),       42);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getYpDot(),    61);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getYpDotDot(), 80);

        // EOP MESSAGE LINE - 2
        writer.outputField(' ', 1);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getTransmissionTime(), 20);
        writer.outputField(' ', 21);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getDut1(), 40);
        writer.outputField(' ', 41);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getDut1Dot(), 60);
        writer.outputField(' ', 61);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, message.getDut1DotDot(), 80);

    }

}
