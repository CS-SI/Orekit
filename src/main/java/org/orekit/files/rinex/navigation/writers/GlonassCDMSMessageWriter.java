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

import org.orekit.files.rinex.navigation.IonosphereGlonassCdmsMessage;
import org.orekit.files.rinex.navigation.MessageType;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for GLONASS ionosphere messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GlonassCDMSMessageWriter
    extends NavigationMessageWriter<IonosphereGlonassCdmsMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final IonosphereGlonassCdmsMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(MessageType.ION, identifier, message, writer);

        // MESSAGE LINE - 0
        writer.writeDate(message.getTransmitTime(), message.getSystem());
        writer.writeField(message.getCA(),    Unit.ONE);
        writer.writeField(message.getCF107(), Unit.ONE);
        writer.writeField(message.getCAP(),   Unit.ONE);
        writer.finishLine();

    }

}
