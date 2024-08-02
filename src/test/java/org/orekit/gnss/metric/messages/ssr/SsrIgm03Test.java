/* Copyright 2002-2024 CS GROUP
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
package org.orekit.gnss.metric.messages.ssr;

import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm03;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm03Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class SsrIgm03Test {

    private double eps = 1.0e-13;

    @Test
    void testPerfectValueGalileo() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "00111111" +                         // IGS Message number: 63 (Galileo)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0001" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "0" +                                // Global/Regional CRS Indicator
                         "000001" +                           // No. of Satellites
                         "001100" +                           // Satellite ID
                         "10000100" +                         // GNSS IOD
                         "0000101011111101111111" +           // Delta Radial
                         "01001010111111011111" +             // Delta Along-Track
                         "01001010111111011111" +             // Delta Cross-Track
                         "000010101111110111111" +            // Dot Delta Radial
                         "0100101011111101111" +              // Dot Delta Along-Track
                         "0100101011111101111" +              // Dot Delta Cross-Track
                         "0011101011111101111111" +           // Delta Clock C0
                         "001110101111110111111" +            // Delta Clock C1
                         "0011101011111101111111000110000";   // Delta Clock C2

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(63);

        final SsrIgm03 igm03 = (SsrIgm03) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(1,                            igm03.getData().size());
        assertEquals(SatelliteSystem.GALILEO,      igm03.getSatelliteSystem());

        // Verify header
        assertEquals(63,                           igm03.getTypeCode());
        assertEquals(517695.0,                     igm03.getHeader().getSsrEpoch1s(), eps);
        assertEquals(2.0,                          igm03.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            igm03.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            igm03.getHeader().getIodSsr());
        assertEquals(3951,                         igm03.getHeader().getSsrProviderId());
        assertEquals(1,                            igm03.getHeader().getSsrSolutionId());
        assertEquals(0,                            igm03.getHeader().getCrsIndicator());
        assertEquals(1,                            igm03.getHeader().getNumberOfSatellites());

        // Verify data for satellite E12
        final SsrIgm03Data e12 = igm03.getSsrIgm03Data().get("E12").get(0);
        assertEquals(12,                           e12.getSatelliteID());
        assertEquals(132,                          e12.getGnssIod());
        assertEquals(18.0095,                      e12.getOrbitCorrection().getDeltaOrbitRadial(),        eps);
        assertEquals(122.8668,                     e12.getOrbitCorrection().getDeltaOrbitAlongTrack(),    eps);
        assertEquals(122.8668,                     e12.getOrbitCorrection().getDeltaOrbitCrossTrack(),    eps);
        assertEquals(0.090047,                     e12.getOrbitCorrection().getDotOrbitDeltaRadial(),     eps);
        assertEquals(0.614332,                     e12.getOrbitCorrection().getDotOrbitDeltaAlongTrack(), eps);
        assertEquals(0.614332,                     e12.getOrbitCorrection().getDotOrbitDeltaCrossTrack(), eps);
        assertEquals(96.6527,                      e12.getClockCorrection().getDeltaClockC0(),            eps);
        assertEquals(0.483263,                     e12.getClockCorrection().getDeltaClockC1(),            eps);
        assertEquals(0.61857734,                   e12.getClockCorrection().getDeltaClockC2(),            eps);

    }

    @Test
    void testPerfectValueGPS() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "00010111" +                         // IGS Message number: 23 (GPS)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1001" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "0" +                                // Global/Regional CRS Indicator
                         "000001" +                           // No. of Satellites
                         "000001" +                           // Satellite ID
                         "10000100" +                         // GNSS IOD
                         "0000101011111101111111" +           // Delta Radial
                         "01001010111111011111" +             // Delta Along-Track
                         "01001010111111011111" +             // Delta Cross-Track
                         "000010101111110111111" +            // Dot Delta Radial
                         "0100101011111101111" +              // Dot Delta Along-Track
                         "0100101011111101111" +              // Dot Delta Cross-Track
                         "0011101011111101111111" +           // Delta Clock C0
                         "001110101111110111111" +            // Delta Clock C1
                         "0011101011111101111111000110000";   // Delta Clock C2

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(23);

        final SsrIgm03 igm03 = (SsrIgm03) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(1,                            igm03.getData().size());
        assertEquals(SatelliteSystem.GPS,          igm03.getSatelliteSystem());

        // Verify header
        assertEquals(23,                           igm03.getTypeCode());
        assertEquals(517695.0,                     igm03.getHeader().getSsrEpoch1s(), eps);
        assertEquals(300.0,                        igm03.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            igm03.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            igm03.getHeader().getIodSsr());
        assertEquals(3951,                         igm03.getHeader().getSsrProviderId());
        assertEquals(1,                            igm03.getHeader().getSsrSolutionId());
        assertEquals(0,                            igm03.getHeader().getCrsIndicator());
        assertEquals(1,                            igm03.getHeader().getNumberOfSatellites());

        // Verify data for satellite G01
        final SsrIgm03Data g01 = igm03.getSsrIgm03Data().get("G01").get(0);
        assertEquals(1,                            g01.getSatelliteID());
        assertEquals(132,                          g01.getGnssIod());
        assertEquals(18.0095,                      g01.getOrbitCorrection().getDeltaOrbitRadial(),         eps);
        assertEquals(122.8668,                     g01.getOrbitCorrection().getDeltaOrbitAlongTrack(),    eps);
        assertEquals(122.8668,                     g01.getOrbitCorrection().getDeltaOrbitCrossTrack(),    eps);
        assertEquals(0.090047,                     g01.getOrbitCorrection().getDotOrbitDeltaRadial(),     eps);
        assertEquals(0.614332,                     g01.getOrbitCorrection().getDotOrbitDeltaAlongTrack(), eps);
        assertEquals(0.614332,                     g01.getOrbitCorrection().getDotOrbitDeltaCrossTrack(), eps);
        assertEquals(96.6527,                      g01.getClockCorrection().getDeltaClockC0(),            eps);
        assertEquals(0.483263,                     g01.getClockCorrection().getDeltaClockC1(),            eps);
        assertEquals(0.61857734,                   g01.getClockCorrection().getDeltaClockC2(),            eps);

    }

    @Test
    void testNullMessage() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "00111111" +                         // IGS Message number: 63 (Galileo)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "0" +                                // Global/Regional CRS Indicator
                        "000001" +                           // No. of Satellites
                        "001100" +                           // Satellite ID
                        "10000100" +                         // GNSS IOD
                        "0000101011111101111111" +           // Delta Radial
                        "01001010111111011111" +             // Delta Along-Track
                        "01001010111111011111" +             // Delta Cross-Track
                        "000010101111110111111" +            // Dot Delta Radial
                        "0100101011111101111" +              // Dot Delta Along-Track
                        "0100101011111101111" +              // Dot Delta Cross-Track
                        "0011101011111101111111" +           // Delta Clock C0
                        "001110101111110111111" +            // Delta Clock C1
                        "0011101011111101111111000110000";   // Delta Clock C2

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm03 igm03 = (SsrIgm03) new IgsSsrMessagesParser(messages).parse(message, false);

       assertNull(igm03);
    }

    @Test
    void testEmptyMessage() {
        try {
            final byte[] array = new byte[0];
            final EncodedMessage emptyMessage = new ByteArrayEncodedMessage(array);
            new IgsSsrMessagesParser(new ArrayList<Integer>()).parse(emptyMessage, false);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, oe.getSpecifier());
        }

    }

    private byte[] byteArrayFromBinary(String radix2Value) {
        final byte[] array = new byte[radix2Value.length() / 8];
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                if (radix2Value.charAt(8 * i + j) != '0') {
                    array[i] |= 0x1 << (7 - j);
                }
            }
        }
        return array;
    }

}
