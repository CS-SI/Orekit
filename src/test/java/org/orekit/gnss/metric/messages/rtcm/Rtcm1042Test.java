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
package org.orekit.gnss.metric.messages.rtcm;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1042;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1042Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.time.GNSSDate;

import java.util.ArrayList;

public class Rtcm1042Test {

    private double eps = 1.0e-15;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testParseMessage() {

        final String m = "010000010010" +                     // Message number: 1042
                        "001100" +                           // Satellite ID
                        "1111111011101" +                    // Week Number
                        "0001" +                             // SV Accuracy
                        "01011101111101" +                   // IDOT
                        "10100" +                            // AODE
                        "00001111011011111" +                // toc
                        "01111111000" +                      // af2
                        "0000101011111101110010" +           // af1
                        "010010101111110111111111" +         // af0
                        "10100" +                            // AODC
                        "000000000000000000" +               // Crs
                        "0111111011001111" +                 // DELTA n
                        "00000110110011111011100110011011" + // M0
                        "000000000000000000" +               // Cuc
                        "00010011111101111000111000011001" + // ecc
                        "000000000000000000" +               // Cus
                        "10100001000011000111111111111111" + // A^(1/2)
                        "10001000111000111" +                // toe
                        "000000000000000000" +               // Cic
                        "00011100011100000111111000111111" + // OMEGA0
                        "000000000000000000" +               // Cis
                        "00101000001111100011110011110000" + // i0
                        "000000000000000000" +               // Crc
                        "00001100001111100011110011110000" + // Argument of perigee
                        "111111111011111111110100" +         // OMEGADOT
                        "0001001111" +                       // tGD1
                        "0111001111" +                       // tGD2
                        "0" +                                // SV Health
                        "0";                                 // Reserved

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(1042);

        final Rtcm1042                rtcm1042      = (Rtcm1042) new RtcmMessagesParser(messages).parse(message, false);
        final Rtcm1042Data            ephemerisData = rtcm1042.getEphemerisData();
        final BeidouLegacyNavigationMessage beidouMessage = ephemerisData.getBeidouNavigationMessage();

        // Verify propagator initialization
        final GNSSPropagator propagator = new GNSSPropagatorBuilder(beidouMessage).build();
        Assertions.assertNotNull(propagator);
        Assertions.assertEquals(0.0, beidouMessage.getDate().
                            durationFrom(new GNSSDate(beidouMessage.getWeek(), beidouMessage.getTime(), SatelliteSystem.BEIDOU).getDate()), eps);

        // Verify message number
        Assertions.assertEquals(1042,                   rtcm1042.getTypeCode());
        Assertions.assertEquals(1,                      rtcm1042.getData().size());

        // Verify navigation message
        Assertions.assertEquals(12,                     beidouMessage.getPRN());
        Assertions.assertEquals(8157,                   beidouMessage.getWeek());
        Assertions.assertEquals(2.1475894557210572E-9,  beidouMessage.getIDot(),               eps);
        Assertions.assertEquals(20,                     beidouMessage.getAODE(),               eps);
        Assertions.assertEquals(1.3769368E-17,          beidouMessage.getAf2(),                eps);
        Assertions.assertEquals(1.5994495E-10,          beidouMessage.getAf1(),                eps);
        Assertions.assertEquals(5.721448687836528E-4,   beidouMessage.getAf0(),                eps);
        Assertions.assertEquals(20,                     beidouMessage.getAODC());
        Assertions.assertEquals(0.0,                    beidouMessage.getCrs(),                eps);
        Assertions.assertEquals(1.4587496546628753E-4,  beidouMessage.getMeanMotion(),         eps);
        Assertions.assertEquals(0.1671775426328288,     beidouMessage.getM0(),                 eps);
        Assertions.assertEquals(0.0,                    beidouMessage.getCuc(),                eps);
        Assertions.assertEquals(0.03899807028938085,    beidouMessage.getE(),                  eps);
        Assertions.assertEquals(0.0,                    beidouMessage.getCus(),                eps);
        Assertions.assertEquals(5153.562498092651,      FastMath.sqrt(beidouMessage.getSma()), eps);
        Assertions.assertEquals(560696.0,               beidouMessage.getTime(),               eps);
        Assertions.assertEquals(0.0,                    beidouMessage.getCic(),                eps);
        Assertions.assertEquals(0.0,                    beidouMessage.getCis(),                eps);
        Assertions.assertEquals(0.987714701321906,      beidouMessage.getI0(),                 eps);
        Assertions.assertEquals(0.0,                    beidouMessage.getCrc(),                eps);
        Assertions.assertEquals(0.30049130834913723,    beidouMessage.getPa(),                 eps);
        Assertions.assertEquals(-5.855958209879004E-9,  beidouMessage.getOmegaDot(),           eps);
        Assertions.assertEquals(0.6980085385373721,     beidouMessage.getOmega0(),             eps);
        Assertions.assertEquals(7.9E-9,                 beidouMessage.getTGD1(),               eps);
        Assertions.assertEquals(4.63E-8,                beidouMessage.getTGD2(),               eps);

        // Verify other data
        Assertions.assertEquals(12,                     ephemerisData.getSatelliteID());
        Assertions.assertEquals(0.0,                    ephemerisData.getSvHealth(),        eps);
        Assertions.assertEquals(63224,                  ephemerisData.getBeidouToc(),       eps);
        Assertions.assertEquals(ephemerisData.getAccuracyProvider().getAccuracy(), beidouMessage.getSvAccuracy(), eps);

    }

    @Test
    public void testNullMessage() {

        final String m = "010000010010" +                     // Message number: 1042
                        "001100" +                           // Satellite ID
                        "1111111011101" +                    // Week Number
                        "0001" +                             // SV Accuracy
                        "01011101111101" +                   // IDOT
                        "10100" +                            // AODE
                        "00001111011011111" +                // toc
                        "01111111000" +                      // af2
                        "0000101011111101110010" +           // af1
                        "010010101111110111111111" +         // af0
                        "10100" +                            // AODC
                        "000000000000000000" +               // Crs
                        "0111111011001111" +                 // DELTA n
                        "00000110110011111011100110011011" + // M0
                        "000000000000000000" +               // Cuc
                        "00010011111101111000111000011001" + // ecc
                        "000000000000000000" +               // Cus
                        "10100001000011000111111111111111" + // A^(1/2)
                        "10001000111000111" +                // toe
                        "000000000000000000" +               // Cic
                        "00011100011100000111111000111111" + // OMEGA0
                        "000000000000000000" +               // Cis
                        "00101000001111100011110011110000" + // i0
                        "000000000000000000" +               // Crc
                        "00001100001111100011110011110000" + // Argument of perigee
                        "111111111011111111110100" +         // OMEGADOT
                        "0001001111" +                       // tGD1
                        "0111001111" +                       // tGD2
                        "0" +                                // SV Health
                        "0";                                 // Reserved

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final Rtcm1042 rtcm1042 = (Rtcm1042) new RtcmMessagesParser(messages).parse(message, false);

       Assertions.assertNull(rtcm1042);
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
