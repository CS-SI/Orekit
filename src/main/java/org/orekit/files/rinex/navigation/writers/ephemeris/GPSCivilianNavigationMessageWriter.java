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

import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Writer for GPS civilian messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GPSCivilianNavigationMessageWriter
    extends CivilianNavigationMessageWriter<GPSCivilianNavigationMessage> {

    /** {@inheritDoc} */
    @Override
    public void writeMessage(final String identifier, final GPSCivilianNavigationMessage message,
                             final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG, and lines 0 to 7
        super.writeMessage(identifier, message, header, writer);

        // EPH MESSAGE LINE - 8/9
        if (message.isCnv2()) {
            writer.indentLine(header);
            writer.writeDouble(message.getIscL1CD(), Unit.SECOND);
            writer.writeDouble(message.getIscL1CP(), Unit.SECOND);
            writer.finishLine();
        }
        writer.indentLine(header);
        writer.writeDouble(message.getTransmissionTime(), Unit.SECOND);
        writer.writeInt(message.getWeek());
        writer.writeInt(message.getFlags());
        writer.finishLine();

    }

}
