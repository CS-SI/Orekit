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
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1045;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1045Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.time.GNSSDate;

import java.util.ArrayList;

public class Rtcm1045Test {

    private double eps = 8.2e-10;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testParseMessage() {

        final String m = "010000010101" +                    // Message Number: 1045
                        "001100" +                           // Satellite ID
                        "111111101111" +                     // Week Number
                        "1000010000" +                       // IODNav
                        "01011110" +                         // SISA
                        "01011101111101" +                   // IDOT
                        "00001111011011" +                   // toc
                        "011111" +                           // af2
                        "000010101111110111011" +            // af1
                        "0100101011111101111111101010011" +  // af0
                        "0000000000000000" +                 // Crs
                        "0111111011001111" +                 // DELTA n
                        "00000110110011111011100110011011" + // M0
                        "0000000000000000" +                 // Cuc
                        "00010011111101111000111000011001" + // ecc
                        "0000000000000000" +                 // Cus
                        "10100001000011000111111111111111" + // A^(1/2)
                        "10001000111011" +                   // toe
                        "0000000000000000" +                 // Cic
                        "00011100011100000111111000111111" + // OMEGA0
                        "0000000000000000" +                 // Cis
                        "00101000001111100011110011110000" + // i0
                        "0000000000000000" +                 // Crc
                        "00001100001111100011110011110000" + // Argument of perigee
                        "111111111011111111110100" +         // OMEGADOT
                        "0001101101" +                       // BGD
                        "00" +                               // E5a SIGNAL Health Status
                        "0" +                                // E5a Data Validity Status
                        "0000000";                           // Reserved


        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(1045);

        final Rtcm1045                 rtcm1045      = (Rtcm1045) new RtcmMessagesParser(messages).parse(message, false);
        final Rtcm1045Data             ephemerisData = rtcm1045.getEphemerisData();
        final GalileoNavigationMessage galileoMessage   = ephemerisData.getGalileoNavigationMessage();

        // Verify propagator initialization
        final GNSSPropagator propagator = new GNSSPropagatorBuilder(galileoMessage).build();
        Assertions.assertNotNull(propagator);
        Assertions.assertEquals(0.0, galileoMessage.getDate().
                            durationFrom(new GNSSDate(galileoMessage.getWeek(), galileoMessage.getTime(), SatelliteSystem.GALILEO).getDate()), eps);

        // Verify message number
        Assertions.assertEquals(1045,                   rtcm1045.getTypeCode());
        Assertions.assertEquals(1,                      rtcm1045.getData().size());

        // Verify navigation message
        Assertions.assertEquals(12,                     galileoMessage.getPRN());
        Assertions.assertEquals(4079,                   galileoMessage.getWeek());
        Assertions.assertEquals(2.1475894557210572E-9,  galileoMessage.getIDot(),               eps);
        Assertions.assertEquals(528,                    galileoMessage.getIODNav(),             eps);
        Assertions.assertEquals(3.3776428E-17,          galileoMessage.getAf2(),                eps);
        Assertions.assertEquals(1.279588E-9,            galileoMessage.getAf1(),                eps);
        Assertions.assertEquals(0.036617268982809,      galileoMessage.getAf0(),                eps);
        Assertions.assertEquals(0.0,                    galileoMessage.getCrs(),                eps);
        Assertions.assertEquals(1.4587496546628753E-4,  galileoMessage.getMeanMotion(),         eps);
        Assertions.assertEquals(0.1671775426328288,     galileoMessage.getM0(),                 eps);
        Assertions.assertEquals(0.0,                    galileoMessage.getCuc(),                eps);
        Assertions.assertEquals(0.0389980711042881,     galileoMessage.getE(),                  eps);
        Assertions.assertEquals(0.0,                    galileoMessage.getCus(),                eps);
        Assertions.assertEquals(5153.562498092651,      FastMath.sqrt(galileoMessage.getSma()), eps);
        Assertions.assertEquals(525780.0,               galileoMessage.getTime(),               eps);
        Assertions.assertEquals(0.0,                    galileoMessage.getCic(),                eps);
        Assertions.assertEquals(0.0,                    galileoMessage.getCis(),                eps);
        Assertions.assertEquals(0.987714701321906,      galileoMessage.getI0(),                 eps);
        Assertions.assertEquals(0.0,                    galileoMessage.getCrc(),                eps);
        Assertions.assertEquals(0.30049130834913723,    galileoMessage.getPa(),                 eps);
        Assertions.assertEquals(-5.855958209879004E-9,  galileoMessage.getOmegaDot(),           eps);
        Assertions.assertEquals(0.6980085385373721,     galileoMessage.getOmega0(),             eps);
        Assertions.assertEquals(2.537854E-8,            galileoMessage.getBGDE1E5a(),           eps);

        // Verify other data
        Assertions.assertEquals(12,                     ephemerisData.getSatelliteID());
        Assertions.assertEquals(59220.0,                ephemerisData.getGalileoToc(), eps);
        Assertions.assertEquals(0,                      ephemerisData.getGalileoDataValidityStatus());
        Assertions.assertEquals(ephemerisData.getAccuracyProvider().getAccuracy(), galileoMessage.getSisa(), eps);

    }

    @Test
    public void testNullMessage() {

        final String m = "010000010101" +                    // Message Number: 1045
                        "001100" +                           // Satellite ID
                        "111111101111" +                     // Week Number
                        "1000010000" +                       // IODNav
                        "01011110" +                         // SISA
                        "01011101111101" +                   // IDOT
                        "00001111011011" +                   // toc
                        "011111" +                           // af2
                        "000010101111110111011" +            // af1
                        "0100101011111101111111101010011" +  // af0
                        "0000000000000000" +                 // Crs
                        "0111111011001111" +                 // DELTA n
                        "00000110110011111011100110011011" + // M0
                        "0000000000000000" +                 // Cuc
                        "00010011111101111000111000011001" + // ecc
                        "0000000000000000" +                 // Cus
                        "10100001000011000111111111111111" + // A^(1/2)
                        "10001000111011" +                   // toe
                        "0000000000000000" +                 // Cic
                        "00011100011100000111111000111111" + // OMEGA0
                        "0000000000000000" +                 // Cis
                        "00101000001111100011110011110000" + // i0
                        "0000000000000000" +                 // Crc
                        "00001100001111100011110011110000" + // Argument of perigee
                        "111111111011111111110100" +         // OMEGADOT
                        "0001101101" +                       // BGD
                        "00" +                               // E5a SIGNAL Health Status
                        "0" +                                // E5a Data Validity Status
                        "0000000";                           // Reserved

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final Rtcm1045 rtcm1045 = (Rtcm1045) new RtcmMessagesParser(messages).parse(message, false);

       Assertions.assertNull(rtcm1045);
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
