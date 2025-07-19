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

import org.orekit.files.rinex.navigation.IonosphereBDGIMMessage;
import org.orekit.files.rinex.navigation.MessageType;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;

import java.io.IOException;

/** Writer for BDGIM model ionospheric messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BDGIMMessageWriter
    extends NavigationMessageWriter<IonosphereBDGIMMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final IonosphereBDGIMMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(MessageType.ION, identifier, message, header, writer);

        // ION MESSAGE LINE - 0
        writer.writeDate(message.getTransmitTime(), message.getSystem());
        writer.writeDouble(message.getAlpha()[0], RinexNavigationParser.TEC);
        writer.writeDouble(message.getAlpha()[1], RinexNavigationParser.TEC);
        writer.writeDouble(message.getAlpha()[2], RinexNavigationParser.TEC);
        writer.finishLine();

        // ION MESSAGE LINE - 1
        writer.startLine();
        writer.writeDouble(message.getAlpha()[3], RinexNavigationParser.TEC);
        writer.writeDouble(message.getAlpha()[4], RinexNavigationParser.TEC);
        writer.writeDouble(message.getAlpha()[5], RinexNavigationParser.TEC);
        writer.writeDouble(message.getAlpha()[6], RinexNavigationParser.TEC);
        writer.finishLine();

        // ION MESSAGE LINE - 2
        writer.startLine();
        writer.writeDouble(message.getAlpha()[7], RinexNavigationParser.TEC);
        writer.writeDouble(message.getAlpha()[8], RinexNavigationParser.TEC);
        writer.finishLine();

    }

}
