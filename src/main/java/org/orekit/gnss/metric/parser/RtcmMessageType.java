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
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1019;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1019Data;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.utils.AccuracyProvider;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.utils.UserRangeAccuracy;
import org.orekit.gnss.navigation.GPSNavigationMessage;
import org.orekit.propagation.analytical.gnss.GPSOrbitalElements;

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

    /** GPS Ephemeris message. */
    RTCM_1019("1019") {

        /** {@inheritDoc} */
        @Override
        public ParsedMessage parse(final EncodedMessage encodedMessage,
                                   final int messageNumber) {

            // Initialize data container and navigation message
            final Rtcm1019Data         rtcm1019Data  = new Rtcm1019Data();
            final GPSNavigationMessage gpsNavMessage = new GPSNavigationMessage();

            // Set the satellite ID
            final int gpsId = RtcmDataField.DF009.intValue(encodedMessage);
            rtcm1019Data.setSatelliteID(gpsId);

            // Week number
            final int gpsWeekNumber = RtcmDataField.DF076.intValue(encodedMessage);
            gpsNavMessage.setWeek(gpsWeekNumber);

            // Accuracy provider
            final AccuracyProvider gpsProvider = new UserRangeAccuracy(RtcmDataField.DF077.intValue(encodedMessage));
            rtcm1019Data.setAccuracyProvider(gpsProvider);
            gpsNavMessage.setSvAccuracy(gpsProvider.getAccuracy());

            // GPS Code on L2
            rtcm1019Data.setGpsCodeOnL2(RtcmDataField.DF078.intValue(encodedMessage));

            // Fill navigation message
            gpsNavMessage.setPRN(gpsId);
            gpsNavMessage.setIDot(RtcmDataField.DF079.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setIODE(RtcmDataField.DF071.intValue(encodedMessage));
            rtcm1019Data.setGpsToc(RtcmDataField.DF081.doubleValue(encodedMessage));
            gpsNavMessage.setAf2(RtcmDataField.DF082.doubleValue(encodedMessage));
            gpsNavMessage.setAf1(RtcmDataField.DF083.doubleValue(encodedMessage));
            gpsNavMessage.setAf0(RtcmDataField.DF084.doubleValue(encodedMessage));
            gpsNavMessage.setIODC(RtcmDataField.DF085.intValue(encodedMessage));
            gpsNavMessage.setCrs(RtcmDataField.DF086.doubleValue(encodedMessage));
            gpsNavMessage.setDeltaN(RtcmDataField.DF087.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setM0(RtcmDataField.DF088.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setCuc(RtcmDataField.DF089.doubleValue(encodedMessage));
            gpsNavMessage.setE(RtcmDataField.DF090.doubleValue(encodedMessage));
            gpsNavMessage.setCus(RtcmDataField.DF091.doubleValue(encodedMessage));
            gpsNavMessage.setSqrtA(RtcmDataField.DF092.doubleValue(encodedMessage));
            gpsNavMessage.setToe(RtcmDataField.DF093.doubleValue(encodedMessage));
            gpsNavMessage.setCic(RtcmDataField.DF094.doubleValue(encodedMessage));
            gpsNavMessage.setOmega0(RtcmDataField.DF095.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setCis(RtcmDataField.DF096.doubleValue(encodedMessage));
            gpsNavMessage.setI0(RtcmDataField.DF097.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setCrc(RtcmDataField.DF098.doubleValue(encodedMessage));
            gpsNavMessage.setPa(RtcmDataField.DF099.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setOmegaDot(RtcmDataField.DF100.doubleValue(encodedMessage) * GPSOrbitalElements.GPS_PI);
            gpsNavMessage.setTGD(RtcmDataField.DF101.doubleValue(encodedMessage));
            gpsNavMessage.setSvHealth(RtcmDataField.DF102.intValue(encodedMessage));

            // Set the navigation message
            rtcm1019Data.setGpsNavigationMessage(gpsNavMessage);

            // L2 P data flag and fit interval
            rtcm1019Data.setGpsL2PDataFlag(RtcmDataField.DF103.booleanValue(encodedMessage));
            rtcm1019Data.setGpsFitInterval(RtcmDataField.DF137.intValue(encodedMessage));

            // Return the parsed message
            return new Rtcm1019(1019, rtcm1019Data);

        }

    },

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
