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
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.NavigationMessage;
import org.orekit.utils.units.Unit;

import java.io.IOException;

/** Base class for navigation messages writers.
 * @param <T> type of the navigation messages this writer handles
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class NavigationMessageWriter<T extends NavigationMessage> {

    /** Write the TYPE / SV / MSG line.
     * @param type message type
     * @param identifier identifier
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    public void writeTypeSvMsg(final MessageType type, final String identifier, final T message,
                               final RinexNavigationHeader header, final RinexNavigationWriter writer)
        throws IOException {

        // TYPE / SV / MSG
        writer.outputField(type.getPrefix(), 6, true);
        writer.outputField(identifier, 10, true);
        writer.outputField(message.getNavigationMessageType(), 15, true);
        if (header.getFormatVersion() >= 4.02 && message.getNavigationMessageSubType() != null) {
            writer.outputField(message.getNavigationMessageSubType(), 19, true);
        }
        writer.finishLine();

    }

    /** Write the EPH MESSAGE LINE - 0.
     * @param message navigation message to write
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    public void writeEphLine0(final AbstractNavigationMessage<?> message, final RinexNavigationWriter writer)
        throws IOException {
        writer.startLine();
        writer.writeDate(message.getEpochToc(), message.getSystem());
        writer.writeDouble(message.getAf0(), Unit.SECOND);
        writer.writeDouble(message.getAf1(), RinexNavigationParser.S_PER_S);
        writer.writeDouble(message.getAf2(), RinexNavigationParser.S_PER_S2);
        writer.finishLine();
    }

    /** Write a navigation message.
     * @param identifier identifier
     * @param message navigation message to write
     * @param header file header
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    public abstract void writeMessage(String identifier, T message,
                                      RinexNavigationHeader header, RinexNavigationWriter writer) throws IOException;

}
