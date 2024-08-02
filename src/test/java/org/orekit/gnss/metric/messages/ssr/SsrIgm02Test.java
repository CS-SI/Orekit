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
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm02;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm02Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class SsrIgm02Test {

    private double eps = 1.0e-13;

    @Test
    void testPerfectValueGlonass() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "00101010" +                         // IGS Message number: 42 (Glonass)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0000" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000001" +                           // No. of Satellites
                         "001100" +                           // Satellite ID
                         "0011101011111101111111" +           // Delta Clock C0
                         "001110101111110111111" +            // Delta Clock C1
                         "001110101111110111111100011000000"; // Delta Clock C2

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(42);

        final SsrIgm02 igm02 = (SsrIgm02) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(1,                            igm02.getData().size());
        assertEquals(SatelliteSystem.GLONASS,      igm02.getSatelliteSystem());

        // Verify header
        assertEquals(42,                           igm02.getTypeCode());
        assertEquals(517695.0,                     igm02.getHeader().getSsrEpoch1s(), eps);
        assertEquals(1.0,                          igm02.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            igm02.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            igm02.getHeader().getIodSsr());
        assertEquals(3951,                         igm02.getHeader().getSsrProviderId());
        assertEquals(1,                            igm02.getHeader().getSsrSolutionId());
        assertEquals(1,                            igm02.getHeader().getNumberOfSatellites());

        // Verify data for satellite G12
        final SsrIgm02Data r12 = igm02.getSsrIgm02Data().get("R12").get(0);
        assertEquals(12,                           r12.getSatelliteID());
        assertEquals(96.6527,                      r12.getClockCorrection().getDeltaClockC0(), eps);
        assertEquals(0.483263,                     r12.getClockCorrection().getDeltaClockC1(), eps);
        assertEquals(0.61857734,                   r12.getClockCorrection().getDeltaClockC2(), eps);

    }

    @Test
    void testPerfectValueGalileo() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "00111110" +                         // IGS Message number: 62 (Galileo)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1000" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000001" +                           // No. of Satellites
                         "000001" +                           // Satellite ID
                         "0011101011111101111111" +           // Delta Clock C0
                         "001110101111110111111" +            // Delta Clock C1
                         "001110101111110111111100011000000"; // Delta Clock C2

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(62);

        final SsrIgm02 igm02 = (SsrIgm02) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(1,                            igm02.getData().size());
        assertEquals(SatelliteSystem.GALILEO,      igm02.getSatelliteSystem());

        // Verify header
        assertEquals(62,                           igm02.getTypeCode());
        assertEquals(517695.0,                     igm02.getHeader().getSsrEpoch1s(), eps);
        assertEquals(240.0,                        igm02.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            igm02.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            igm02.getHeader().getIodSsr());
        assertEquals(3951,                         igm02.getHeader().getSsrProviderId());
        assertEquals(1,                            igm02.getHeader().getSsrSolutionId());
        assertEquals(1,                            igm02.getHeader().getNumberOfSatellites());

        // Verify data for satellite E01
        final SsrIgm02Data e01 = igm02.getSsrIgm02Data().get("E01").get(0);
        assertEquals(1,                            e01.getSatelliteID());
        assertEquals(96.6527,                      e01.getClockCorrection().getDeltaClockC0(), eps);
        assertEquals(0.483263,                     e01.getClockCorrection().getDeltaClockC1(), eps);
        assertEquals(0.61857734,                   e01.getClockCorrection().getDeltaClockC2(), eps);

    }

    @Test
    void testNullMessage() {

       final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "00101010" +                         // IGS Message number: 42 (Glonass)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "000001" +                           // No. of Satellites
                        "001100" +                           // Satellite ID
                        "0011101011111101111111" +           // Delta Clock C0
                        "001110101111110111111" +            // Delta Clock C1
                        "001110101111110111111100011000000"; // Delta Clock C2

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm02 igm02 = (SsrIgm02) new IgsSsrMessagesParser(messages).parse(message, false);

       assertNull(igm02);
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
