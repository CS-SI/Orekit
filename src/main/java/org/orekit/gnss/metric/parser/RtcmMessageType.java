/* Copyright 2002-2021 CS GROUP
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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.messages.ParsedMessage;

/** Enum containing the supported RTCM messages types.
*
* @author Luc Maisonobe
* @author Bryan Cazabonne
*
* @see "RTCM STANDARD 10403.3, DIFFERENTIAL GNSS (GLOBAL NAVIGATION SATELLITE SYSTEMS) SERVICES – VERSION 3, October 2016."
*
* @since 11.0
*/
public enum RtcmMessageType implements MessageType {

    // RTCM messages are not currently supported in Orekit
    // Contributions are welcome

    /** Null message. */
    NULL_MESSAGE("9999") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {
            // This message does not exist, it should be never called
            return null;
        }

    };

    /** Codes map. */
    private static final Map<Pattern, RtcmMessageType> CODES_MAP = new HashMap<>();
    static {
        for (final RtcmMessageType type : values()) {
            CODES_MAP.put(type.getPattern(), type);
        }
    }

    /** Message pattern (i.e. allowed message numbers). */
    private final Pattern pattern;

    /** Simple constructor.
     * @param messageRegex message regular expression
     */
    RtcmMessageType(final String messageRegex) {
        this.pattern = Pattern.compile(messageRegex);
    }

    /** Get the message number.
     * @return message number
     */
    public Pattern getPattern() {
        return pattern;
    }

    /** Get the message type corresponding to a message number.
     * @param rtcmNumber message number
     * @return the message type corresponding to the message number
     */
    public static RtcmMessageType getMessageType(final String rtcmNumber) {
        // Try to find a match with an existing message type
        for (Map.Entry<Pattern, RtcmMessageType> rtcmEntry : CODES_MAP.entrySet()) {
            // Matcher
            final Matcher matcher = rtcmEntry.getKey().matcher(rtcmNumber);
            // Check the match !
            if (matcher.matches()) {
                // return the message type
                return rtcmEntry.getValue();
            }
        }
        // No match found
        throw new OrekitException(OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER, rtcmNumber);
    }

}
