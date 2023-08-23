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
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm01;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm01Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrDataField;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;


public class SsrIgm01Test {

    private double eps = 1.0e-13;

    @Test
    public void testPerfectValueGPS() {

        final String m = "010000100100" +                 // RTCM Message number: 1060
                         "001" +                          // IGS SSR version
                         "00010101" +                     // IGS Message number: 21 (GPS)
                         "01111110011000111111" +         // Epoch Time 1s
                         "0010" +                         // SSR Update Interval
                         "0" +                            // Multiple Message Indicator
                         "0111" +                         // IOD SSR
                         "0000111101101111" +             // SSR Provider ID
                         "0001" +                         // SSR Solution ID
                         "0" +                            // Global/Regional CRS Indicator
                         "000001" +                       // No. of Satellites
                         "001100" +                       // Satellite ID
                         "10000100" +                     // GNSS IOD
                         "0000101011111101111111" +       // Delta Radial
                         "01001010111111011111" +         // Delta Along-Track
                         "01001010111111011111" +         // Delta Cross-Track
                         "000010101111110111111" +        // Dot Delta Radial
                         "0100101011111101111" +          // Dot Delta Along-Track
                         "010010101111110111100";         // Dot Delta Cross-Track

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(21);

        final SsrIgm01 igm01 = (SsrIgm01) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            igm01.getData().size());
        Assertions.assertEquals(SatelliteSystem.GPS,          igm01.getSatelliteSystem());

        // Verify header
        Assertions.assertEquals(21,                           igm01.getTypeCode());
        Assertions.assertEquals(517695.0,                     igm01.getHeader().getSsrEpoch1s(), eps);
        Assertions.assertEquals(5.0,                          igm01.getHeader().getSsrUpdateInterval(), eps);
        Assertions.assertEquals(0,                            igm01.getHeader().getSsrMultipleMessageIndicator());
        Assertions.assertEquals(7,                            igm01.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         igm01.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            igm01.getHeader().getSsrSolutionId());
        Assertions.assertEquals(0,                            igm01.getHeader().getCrsIndicator());
        Assertions.assertEquals(1,                            igm01.getHeader().getNumberOfSatellites());

        // Verify data for satellite G12
        final SsrIgm01Data g12 = igm01.getSsrIgm01Data().get("G12").get(0);
        Assertions.assertEquals(12,                           g12.getSatelliteID());
        Assertions.assertEquals(132,                          g12.getGnssIod());
        Assertions.assertEquals(18.0095,                      g12.getOrbitCorrection().getDeltaOrbitRadial(),        eps);
        Assertions.assertEquals(122.8668,                     g12.getOrbitCorrection().getDeltaOrbitAlongTrack(),    eps);
        Assertions.assertEquals(122.8668,                     g12.getOrbitCorrection().getDeltaOrbitCrossTrack(),    eps);
        Assertions.assertEquals(0.090047,                     g12.getOrbitCorrection().getDotOrbitDeltaRadial(),     eps);
        Assertions.assertEquals(0.614332,                     g12.getOrbitCorrection().getDotOrbitDeltaAlongTrack(), eps);
        Assertions.assertEquals(0.614332,                     g12.getOrbitCorrection().getDotOrbitDeltaCrossTrack(), eps);

    }

    @Test
    public void testPerfectValueGalileo() {

        final String m = "010000100100" +                 // RTCM Message number: 1060
                         "001" +                          // IGS SSR version
                         "00111101" +                     // IGS Message number: 61 (Galileo)
                         "01111110011000111111" +         // Epoch Time 1s
                         "0111" +                         // SSR Update Interval
                         "0" +                            // Multiple Message Indicator
                         "0111" +                         // IOD SSR
                         "0000111101101111" +             // SSR Provider ID
                         "0001" +                         // SSR Solution ID
                         "0" +                            // Global/Regional CRS Indicator
                         "000001" +                       // No. of Satellites
                         "000001" +                       // Satellite ID
                         "10000100" +                     // GNSS IOD
                         "0000101011111101111111" +       // Delta Radial
                         "01001010111111011111" +         // Delta Along-Track
                         "01001010111111011111" +         // Delta Cross-Track
                         "000010101111110111111" +        // Dot Delta Radial
                         "0100101011111101111" +          // Dot Delta Along-Track
                         "010010101111110111100";         // Dot Delta Cross-Track

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(0);

        final SsrIgm01 igm01 = (SsrIgm01) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            igm01.getData().size());
        Assertions.assertEquals(SatelliteSystem.GALILEO,      igm01.getSatelliteSystem());

        // Verify header
        Assertions.assertEquals(61,                           igm01.getTypeCode());
        Assertions.assertEquals(517695.0,                     igm01.getHeader().getSsrEpoch1s(), eps);
        Assertions.assertEquals(120.0,                        igm01.getHeader().getSsrUpdateInterval(), eps);
        Assertions.assertEquals(0,                            igm01.getHeader().getSsrMultipleMessageIndicator());
        Assertions.assertEquals(7,                            igm01.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         igm01.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            igm01.getHeader().getSsrSolutionId());
        Assertions.assertEquals(0,                            igm01.getHeader().getCrsIndicator());
        Assertions.assertEquals(1,                            igm01.getHeader().getNumberOfSatellites());

        // Verify data for satellite E01
        final SsrIgm01Data e01 = igm01.getSsrIgm01Data().get("E01").get(0);
        Assertions.assertEquals(1,                            e01.getSatelliteID());
        Assertions.assertEquals(132,                          e01.getGnssIod());
        Assertions.assertEquals(18.0095,                      e01.getOrbitCorrection().getDeltaOrbitRadial(),        eps);
        Assertions.assertEquals(122.8668,                     e01.getOrbitCorrection().getDeltaOrbitAlongTrack(),    eps);
        Assertions.assertEquals(122.8668,                     e01.getOrbitCorrection().getDeltaOrbitCrossTrack(),    eps);
        Assertions.assertEquals(0.090047,                     e01.getOrbitCorrection().getDotOrbitDeltaRadial(),     eps);
        Assertions.assertEquals(0.614332,                     e01.getOrbitCorrection().getDotOrbitDeltaAlongTrack(), eps);
        Assertions.assertEquals(0.614332,                     e01.getOrbitCorrection().getDotOrbitDeltaCrossTrack(), eps);

    }

    @Test
    public void testNullMessage() {
        final String m = "010000100100" +                 // RTCM Message number: 1060
                        "001" +                          // IGS SSR version
                        "11111111" +                     // IGS Message number: 21 (GPS)
                        "01111110011000111111" +         // Epoch Time 1s
                        "0101" +                         // SSR Update Interval
                        "0" +                            // Multiple Message Indicator
                        "0111" +                         // IOD SSR
                        "0000111101101111" +             // SSR Provider ID
                        "0001" +                         // SSR Solution ID
                        "0" +                            // Global/Regional CRS Indicator
                        "000001" +                       // No. of Satellites
                        "001100" +                       // Satellite ID
                        "10000100" +                     // GNSS IOD
                        "0000101011111101111111" +       // Delta Radial
                        "01001010111111011111" +         // Delta Along-Track
                        "01001010111111011111" +         // Delta Cross-Track
                        "000010101111110111111" +        // Dot Delta Radial
                        "0100101011111101111" +          // Dot Delta Along-Track
                        "010010101111110111100";         // Dot Delta Cross-Track

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm01 igm01 = (SsrIgm01) new IgsSsrMessagesParser(messages).parse(message, true);

       Assertions.assertNull(igm01);
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

    @Test
    public void testDefaultSsrUpdateInterval() {
        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary("1111111111111111"));
        Assertions.assertEquals(10800, IgsSsrDataField.IDF004.intValue(message));
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
