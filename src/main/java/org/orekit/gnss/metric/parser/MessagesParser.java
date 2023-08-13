/* Copyright 2002-2023 CS GROUP
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
package org.orekit.gnss.metric.parser;

import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.messages.ParsedMessage;

/** Parser for IGS encoded messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class MessagesParser {

    /** Set of needed messages. */
    private final List<Integer> messages;

    /**
     * Constructor.
     * @param messages list of needed messages
     */
    public MessagesParser(final List<Integer> messages) {
        this.messages = messages;
    }

    /** Parse one message.
     * @param message encoded message to parse
     * @param ignoreUnknownMessageTypes if true, unknown messages types are silently ignored
     * @return parsed message, or null if parse not possible and {@code ignoreUnknownMessageTypes} is true
     */
    public ParsedMessage parse(final EncodedMessage message, final boolean ignoreUnknownMessageTypes) {

        try {

            // get the message number as a String
            final String messageNumberString = parseMessageNumber(message);
            final int    messageNumberInt    = Integer.parseInt(messageNumberString);

            // get the message parser for the extracted message number
            final MessageType messageType = getMessageType(messageNumberString);

            // if set to 0, notification will be triggered regardless of message type
            if (messages.contains(0)) {
                return messageType.parse(message, messageNumberInt);
            }

            // parse one message
            return messages.contains(messageNumberInt) ? messageType.parse(message, messageNumberInt) : null;

        } catch (OrekitException oe) {
            if (ignoreUnknownMessageTypes &&
                            oe.getSpecifier() == OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER) {
                // message is unknown but we ignore it
                return null;
            } else {
                // throw an exception
                throw oe;
            }
        }

    }

    /** Parse the message number.
     * @param message encoded message to parse
     * @return the message number
     */
    protected abstract String parseMessageNumber(EncodedMessage message);

    /** Get the message type corresponding to the message number.
     * @param messageNumber String reprensentation of the message number
     * @return the message type
     */
    protected abstract MessageType getMessageType(String messageNumber);

}
