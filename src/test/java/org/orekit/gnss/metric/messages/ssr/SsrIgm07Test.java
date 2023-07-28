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
package org.orekit.gnss.metric.messages.ssr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm07;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm07Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;

public class SsrIgm07Test {

    private double eps = 1.0e-13;

    @Test
    public void testPerfectValueGPS() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "00011011" +                         // IGS Message number: 27 (GPS)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0110" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000010" +                           // No. of Satellites: 2
                         "000001" +                           // Satellite ID
                         "010111" +                           // SSR URA
                         "001100" +                           // Satellite ID
                         "11110000";                          // SSR URA

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(27);

        final SsrIgm07 igm07 = (SsrIgm07) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(2,                            igm07.getData().size());
        Assertions.assertEquals(SatelliteSystem.GPS,          igm07.getSatelliteSystem());

        // Verify header
        Assertions.assertEquals(27,                           igm07.getTypeCode());
        Assertions.assertEquals(517695.0,                     igm07.getHeader().getSsrEpoch1s(), eps);
        Assertions.assertEquals(60.0,                         igm07.getHeader().getSsrUpdateInterval(), eps);
        Assertions.assertEquals(0,                            igm07.getHeader().getSsrMultipleMessageIndicator());
        Assertions.assertEquals(7,                            igm07.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         igm07.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            igm07.getHeader().getSsrSolutionId());
        Assertions.assertEquals(2,                            igm07.getHeader().getNumberOfSatellites());

        // Verify data for satellite G01
        final SsrIgm07Data g1 = igm07.getSsrIgm07Data().get("G01").get(0);
        Assertions.assertEquals(1,                         g1.getSatelliteID());
        Assertions.assertEquals(23.0,                      g1.getSsrUra(), eps);

        // Verify data for satellite G12
        final SsrIgm07Data g12 = igm07.getSsrIgm07Data().get("G12").get(0);
        Assertions.assertEquals(12,                         g12.getSatelliteID());
        Assertions.assertEquals(60.0,                       g12.getSsrUra(), eps);

    }

    @Test
    public void testPerfectValueGalileo() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01000011" +                         // IGS Message number: 67 (Galileo)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1101" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000010" +                           // No. of Satellites: 2
                         "000001" +                           // Satellite ID
                         "010111" +                           // SSR URA
                         "001100" +                           // Satellite ID
                         "11110000";                          // SSR URA

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(67);

        final SsrIgm07 igm07 = (SsrIgm07) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(2,                            igm07.getData().size());
        Assertions.assertEquals(SatelliteSystem.GALILEO,      igm07.getSatelliteSystem());

        // Verify header
        Assertions.assertEquals(67,                           igm07.getTypeCode());
        Assertions.assertEquals(517695.0,                     igm07.getHeader().getSsrEpoch1s(), eps);
        Assertions.assertEquals(3600.0,                       igm07.getHeader().getSsrUpdateInterval(), eps);
        Assertions.assertEquals(0,                            igm07.getHeader().getSsrMultipleMessageIndicator());
        Assertions.assertEquals(7,                            igm07.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         igm07.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            igm07.getHeader().getSsrSolutionId());
        Assertions.assertEquals(2,                            igm07.getHeader().getNumberOfSatellites());

        // Verify data for satellite E01
        final SsrIgm07Data e01 = igm07.getSsrIgm07Data().get("E01").get(0);
        Assertions.assertEquals(1,                         e01.getSatelliteID());
        Assertions.assertEquals(23.0,                      e01.getSsrUra(), eps);

        // Verify data for satellite E12
        final SsrIgm07Data e12 = igm07.getSsrIgm07Data().get("E12").get(0);
        Assertions.assertEquals(12,                         e12.getSatelliteID());
        Assertions.assertEquals(60.0,                       e12.getSsrUra(), eps);

    }

    @Test
    public void testNullMessage() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "00011011" +                         // IGS Message number: 27 (GPS)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "000010" +                           // No. of Satellites: 2
                        "000001" +                           // Satellite ID
                        "01111111" +                         // SSR URA
                        "001100" +                           // Satellite ID
                        "1111000000";                        // SSR URA

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm07 igm07 = (SsrIgm07) new IgsSsrMessagesParser(messages).parse(message, false);

       Assertions.assertNull(igm07);
    }

    @Test
    public void testEmptyMessage() {
        try {
            final byte[] array = new byte[0];
            final EncodedMessage emptyMessage = new ByteArrayEncodedMessage(array);
            new IgsSsrMessagesParser(new ArrayList<Integer>()).parse(emptyMessage, false);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, oe.getSpecifier());
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
