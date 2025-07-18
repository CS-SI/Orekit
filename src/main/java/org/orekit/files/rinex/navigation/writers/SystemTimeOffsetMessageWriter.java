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

import org.orekit.files.rinex.navigation.MessageType;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.files.rinex.navigation.SystemTimeOffsetMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for System time offset messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SystemTimeOffsetMessageWriter
    extends NavigationMessageWriter<SystemTimeOffsetMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final SystemTimeOffsetMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(MessageType.STO, identifier, message, header, writer);

        // EPOCH / SYSTEM CORR TYPE / SBAS ID / UTC ID
        writer.writeDate(message.getReferenceEpoch(), message.getSystem());
        writer.outputField(' ', 24);
        writer.outputField(message.getReferenceTimeSystem().getTwoLettersCode(), 26, true);
        writer.outputField(message.getDefinedTimeSystem().getTwoLettersCode(),   43, true);
        writer.outputField(message.getSbasId() == null ? "" : message.getSbasId().name(), 62, true);
        writer.outputField(message.getUtcId()  == null ? "" : message.getUtcId().getId(), 80, true);
        writer.finishLine();

        // STO MESSAGE LINE - 1
        writer.startLine();
        writer.writeField(message.getTransmissionTime(), Unit.SECOND);
        writer.writeField(message.getA0(),               Unit.SECOND);
        writer.writeField(message.getA1(),               RinexNavigationParser.S_PER_S);
        writer.writeField(message.getA2(),               RinexNavigationParser.S_PER_S2);
        writer.finishLine();

    }

}
