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
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm04;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm04Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class SsrIgm04Test {

    private double eps = 1.0e-13;

    @Test
    void testPerfectValueQZSS() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01010100" +                         // IGS Message number: 84 (QZSS)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0011" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000010" +                           // No. of Satellites: 2
                         "000001" +                           // Satellite ID
                         "0011101011111101111111" +           // High Rate Clock Correction
                         "001100" +                           // Satellite ID
                         "0011101011111101111110000";         // High Rate Clock Correction

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(84);

        final SsrIgm04 igm04 = (SsrIgm04) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(2,                            igm04.getData().size());
        assertEquals(SatelliteSystem.QZSS,         igm04.getSatelliteSystem());

        // Verify header
        assertEquals(84,                           igm04.getTypeCode());
        assertEquals(517695.0,                     igm04.getHeader().getSsrEpoch1s(), eps);
        assertEquals(10.0,                         igm04.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            igm04.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            igm04.getHeader().getIodSsr());
        assertEquals(3951,                         igm04.getHeader().getSsrProviderId());
        assertEquals(1,                            igm04.getHeader().getSsrSolutionId());
        assertEquals(2,                            igm04.getHeader().getNumberOfSatellites());

        // Verify data for satellite J204
        final SsrIgm04Data j204 = igm04.getSsrIgm04Data().get("J204").get(0);
        assertEquals(204,                          j204.getSatelliteID());
        assertEquals(96.6526,                      j204.getHighRateClockCorrection(), eps);

        // Verify data for satellite J193
        final SsrIgm04Data j193 = igm04.getSsrIgm04Data().get("J193").get(0);
        assertEquals(193,                          j193.getSatelliteID());
        assertEquals(96.6527,                      j193.getHighRateClockCorrection(), eps);

    }

    @Test
    void testPerfectValueGalileo() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01000000" +                         // IGS Message number: 64 (Galileo)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1010" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000010" +                           // No. of Satellites: 2
                         "000001" +                           // Satellite ID
                         "0011101011111101111111" +           // High Rate Clock Correction
                         "010000" +                           // Satellite ID
                         "0011101011111101111110000";         // High Rate Clock Correction

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(64);

        final SsrIgm04 igm04 = (SsrIgm04) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(2,                            igm04.getData().size());
        assertEquals(SatelliteSystem.GALILEO,      igm04.getSatelliteSystem());

        // Verify header
        assertEquals(64,                           igm04.getTypeCode());
        assertEquals(517695.0,                     igm04.getHeader().getSsrEpoch1s(), eps);
        assertEquals(600.0,                        igm04.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            igm04.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            igm04.getHeader().getIodSsr());
        assertEquals(3951,                         igm04.getHeader().getSsrProviderId());
        assertEquals(1,                            igm04.getHeader().getSsrSolutionId());
        assertEquals(2,                            igm04.getHeader().getNumberOfSatellites());

        // Verify data for satellite E01
        final SsrIgm04Data e01 = igm04.getSsrIgm04Data().get("E01").get(0);
        assertEquals(1,                            e01.getSatelliteID());
        assertEquals(96.6527,                      e01.getHighRateClockCorrection(), eps);

        // Verify data for satellite E16
        final SsrIgm04Data e16 = igm04.getSsrIgm04Data().get("E16").get(0);
        assertEquals(16,                           e16.getSatelliteID());
        assertEquals(96.6526,                      e16.getHighRateClockCorrection(), eps);

    }

    @Test
    void testNullMessage() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "01010100" +                         // IGS Message number: 84 (QZSS)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "000010" +                           // No. of Satellites: 2
                        "000001" +                           // Satellite ID
                        "0011101011111101111111" +           // High Rate Clock Correction
                        "001100" +                           // Satellite ID
                        "0011101011111101111110000";         // High Rate Clock Correction

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm04 igm04 = (SsrIgm04) new IgsSsrMessagesParser(messages).parse(message, false);

       assertNull(igm04);
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
