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

import org.orekit.files.rinex.navigation.IonosphereAij;
import org.orekit.files.rinex.navigation.IonosphereNavICNeQuickNMessage;
import org.orekit.files.rinex.navigation.MessageType;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for NavIC NeQuick N messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICNeQuickNMessageWriter
    extends NavigationMessageWriter<IonosphereNavICNeQuickNMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final IonosphereNavICNeQuickNMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writeTypeSvMsg(MessageType.ION, identifier, message, header, writer);

        // ION MESSAGE LINE - 0
        writer.writeDate(message.getTransmitTime(), message.getSystem());
        writer.writeField(message.getIOD(), Unit.ONE);
        writer.finishLine();

        // ION MESSAGE LINE - 1
        writer.startLine();
        writer.writeField(message.getRegion1().getAi0(), IonosphereAij.SFU);
        writer.writeField(message.getRegion1().getAi1(), IonosphereAij.SFU_PER_DEG);
        writer.writeField(message.getRegion1().getAi2(), IonosphereAij.SFU_PER_DEG2);
        writer.writeField(message.getRegion1().getIDF(), Unit.ONE);
        writer.finishLine();

        // ION MESSAGE LINE - 2
        writer.startLine();
        writer.writeField(message.getRegion1().getLonMin(),   Unit.DEGREE);
        writer.writeField(message.getRegion1().getLonMax(),   Unit.DEGREE);
        writer.writeField(message.getRegion1().getModipMin(), Unit.DEGREE);
        writer.writeField(message.getRegion1().getModipMax(), Unit.DEGREE);
        writer.finishLine();

        // ION MESSAGE LINE - 3
        writer.startLine();
        writer.writeField(message.getRegion2().getAi0(), IonosphereAij.SFU);
        writer.writeField(message.getRegion2().getAi1(), IonosphereAij.SFU_PER_DEG);
        writer.writeField(message.getRegion2().getAi2(), IonosphereAij.SFU_PER_DEG2);
        writer.writeField(message.getRegion2().getIDF(), Unit.ONE);
        writer.finishLine();

        // ION MESSAGE LINE - 4
        writer.startLine();
        writer.writeField(message.getRegion2().getLonMin(),   Unit.DEGREE);
        writer.writeField(message.getRegion2().getLonMax(),   Unit.DEGREE);
        writer.writeField(message.getRegion2().getModipMin(), Unit.DEGREE);
        writer.writeField(message.getRegion2().getModipMax(), Unit.DEGREE);
        writer.finishLine();

        // ION MESSAGE LINE - 5
        writer.startLine();
        writer.writeField(message.getRegion3().getAi0(), IonosphereAij.SFU);
        writer.writeField(message.getRegion3().getAi1(), IonosphereAij.SFU_PER_DEG);
        writer.writeField(message.getRegion3().getAi2(), IonosphereAij.SFU_PER_DEG2);
        writer.writeField(message.getRegion3().getIDF(), Unit.ONE);
        writer.finishLine();

        // ION MESSAGE LINE - 6
        writer.startLine();
        writer.writeField(message.getRegion3().getLonMin(),   Unit.DEGREE);
        writer.writeField(message.getRegion3().getLonMax(),   Unit.DEGREE);
        writer.writeField(message.getRegion3().getModipMin(), Unit.DEGREE);
        writer.writeField(message.getRegion3().getModipMax(), Unit.DEGREE);
        writer.finishLine();

    }

}
