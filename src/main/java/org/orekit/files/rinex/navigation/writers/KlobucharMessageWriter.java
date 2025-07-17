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

import org.orekit.files.rinex.navigation.IonosphereKlobucharMessage;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.files.rinex.utils.BaseRinexWriter;

import java.io.IOException;

/** Writer for Klobuchar model ionospheric messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class KlobucharMessageWriter
    implements NavigationMessageWriter<IonosphereKlobucharMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final IonosphereKlobucharMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writer.outputField("> ION", 6, true);
        writer.outputField(identifier, 10, true);
        writer.outputField(message.getNavigationMessageType(), 15, true);
        if (header.getFormatVersion() >= 4.02 && message.getSubType() != null) {
            writer.outputField(message.getSubType(), 19, true);
        }
        writer.finishLine();

        // ION MESSAGE LINE - 0
        writer.writeDate(message.getTransmitTime(), message.getSystem());
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[0].fromSI(message.getAlpha()[0]),
                           42);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[1].fromSI(message.getAlpha()[1]),
                           61);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[2].fromSI(message.getAlpha()[2]),
                           80);
        writer.finishLine();

        // ION MESSAGE LINE - 1
        writer.outputField(' ', 4);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(message.getAlpha()[3]),
                           23);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[0].fromSI(message.getBeta()[0]),
                           42);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[1].fromSI(message.getBeta()[1]),
                           61);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(message.getBeta()[2]),
                           80);

        // ION MESSAGE LINE - 2
        writer.outputField(' ', 4);
        writer.outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT,
                           IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(message.getBeta()[3]),
                           23);

    }

}
