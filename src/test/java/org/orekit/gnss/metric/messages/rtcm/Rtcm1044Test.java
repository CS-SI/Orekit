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
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1044;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1044Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.time.GNSSDate;

import java.util.ArrayList;

public class Rtcm1044Test {

    private double eps = 9.0e-10;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testParseMessage() {

        final String m = "010000010100" +                     // Message number: 1044
                        "1100" +                             // Satellite ID
                        "0000111101101111" +                 // toc
                        "01111111" +                         // af2
                        "0000101011111101" +                 // af1
                        "0100101011111101111111" +           // af0
                        "10000100" +                         // IODE
                        "0000000000000000" +                 // Crs
                        "0111111011001111" +                 // DELTA n
                        "00000110110011111011100110011011" + // M0
                        "0000000000000000" +                 // Cuc
                        "00010011111101111000111000011001" + // ecc
                        "0000000000000000" +                 // Cus
                        "10100001000011000111111111111111" + // A^(1/2)
                        "1000100011100011" +                 // toe
                        "0000000000000000" +                 // Cic
                        "00011100011100000111111000111111" + // OMEGA0
                        "0000000000000000" +                 // Cis
                        "00101000001111100011110011110000" + // i0
                        "0000000000000000" +                 // Crc
                        "00001100001111100011110011110000" + // Argument of perigee
                        "111111111011111111110100" +         // OMEGADOT
                        "01011101111101" +                   // IDOT
                        "10" +                               // QZSS CODE ON L2
                        "0110111001" +                       // Week number
                        "0001" +                             // SV Accuracy
                        "000000" +                           // SV Health
                        "00000011" +                         // tGD
                        "1010110111" +                       // IODC
                        "0" +                                // Fit Interval
                        "000";                               // Reserved


        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(1044);

        final Rtcm1044              rtcm1044      = (Rtcm1044) new RtcmMessagesParser(messages).parse(message, false);
        final Rtcm1044Data          ephemerisData = rtcm1044.getEphemerisData();
        final QZSSLegacyNavigationMessage qzssMessage   = ephemerisData.getQzssNavigationMessage();

        // Verify propagator initialization
        final GNSSPropagator propagator = new GNSSPropagatorBuilder(qzssMessage).build();
        Assertions.assertNotNull(propagator);
        Assertions.assertEquals(0.0, qzssMessage.getDate().
                            durationFrom(new GNSSDate(qzssMessage.getWeek(), qzssMessage.getTime(), SatelliteSystem.QZSS).getDate()), eps);

        // Verify message number
        Assertions.assertEquals(1044,                   rtcm1044.getTypeCode());
        Assertions.assertEquals(1,                      rtcm1044.getData().size());

        // Verify navigation message
        Assertions.assertEquals(204,                    qzssMessage.getPRN());
        Assertions.assertEquals(441,                    qzssMessage.getWeek());
        Assertions.assertEquals(2.1475894557210572E-9,  qzssMessage.getIDot(),               eps);
        Assertions.assertEquals(132,                    qzssMessage.getIODE(),               eps);
        Assertions.assertEquals(3.524958E-15,           qzssMessage.getAf2(),                eps);
        Assertions.assertEquals(3.1980107E-10,          qzssMessage.getAf1(),                eps);
        Assertions.assertEquals(5.721445195376873E-4,   qzssMessage.getAf0(),                eps);
        Assertions.assertEquals(695,                    qzssMessage.getIODC());
        Assertions.assertEquals(0.0,                    qzssMessage.getCrs(),                eps);
        Assertions.assertEquals(1.4587496546628753E-4,  qzssMessage.getMeanMotion(),         eps);
        Assertions.assertEquals(0.1671775426328288,     qzssMessage.getM0(),                 eps);
        Assertions.assertEquals(0.0,                    qzssMessage.getCuc(),                eps);
        Assertions.assertEquals(0.0389980711042881,     qzssMessage.getE(),                  eps);
        Assertions.assertEquals(0.0,                    qzssMessage.getCus(),                eps);
        Assertions.assertEquals(5153.562498092651,      FastMath.sqrt(qzssMessage.getSma()), eps);
        Assertions.assertEquals(560688.0,               qzssMessage.getTime(),               eps);
        Assertions.assertEquals(0.0,                    qzssMessage.getCic(),                eps);
        Assertions.assertEquals(0.0,                    qzssMessage.getCis(),                eps);
        Assertions.assertEquals(0.987714701321906,      qzssMessage.getI0(),                 eps);
        Assertions.assertEquals(0.0,                    qzssMessage.getCrc(),                eps);
        Assertions.assertEquals(0.30049130834913723,    qzssMessage.getPa(),                 eps);
        Assertions.assertEquals(-5.855958209879004E-9,  qzssMessage.getOmegaDot(),           eps);
        Assertions.assertEquals(0.6980085385373721,     qzssMessage.getOmega0(),             eps);
        Assertions.assertEquals(1.3969839E-9,           qzssMessage.getTGD(),                eps);

        // Verify other data
        Assertions.assertEquals(204,                    ephemerisData.getSatelliteID());
        Assertions.assertEquals(63216.0,                ephemerisData.getQzssToc(), eps);
        Assertions.assertEquals(2,                      ephemerisData.getQzssCodeOnL2());
        Assertions.assertEquals(0,                      ephemerisData.getQzssFitInterval());
        Assertions.assertEquals(ephemerisData.getAccuracyProvider().getAccuracy(), qzssMessage.getSvAccuracy(), eps);

    }

    @Test
    public void testNullMessage() {

        final String m = "010000010100" +                     // Message number: 1044
                        "1100" +                             // Satellite ID
                        "0000111101101111" +                 // toc
                        "01111111" +                         // af2
                        "0000101011111101" +                 // af1
                        "0100101011111101111111" +           // af0
                        "10000100" +                         // IODE
                        "0000000000000000" +                 // Crs
                        "0111111011001111" +                 // DELTA n
                        "00000110110011111011100110011011" + // M0
                        "0000000000000000" +                 // Cuc
                        "00010011111101111000111000011001" + // ecc
                        "0000000000000000" +                 // Cus
                        "10100001000011000111111111111111" + // A^(1/2)
                        "1000100011100011" +                 // toe
                        "0000000000000000" +                 // Cic
                        "00011100011100000111111000111111" + // OMEGA0
                        "0000000000000000" +                 // Cis
                        "00101000001111100011110011110000" + // i0
                        "0000000000000000" +                 // Crc
                        "00001100001111100011110011110000" + // Argument of perigee
                        "111111111011111111110100" +         // OMEGADOT
                        "01011101111101" +                   // IDOT
                        "10" +                               // QZSS CODE ON L2
                        "0110111001" +                       // Week number
                        "0001" +                             // SV Accuracy
                        "000000" +                           // SV Health
                        "00000011" +                         // tGD
                        "1010110111" +                       // IODC
                        "0" +                                // Fit Interval
                        "000";                               // Reserved

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final Rtcm1044 rtcm1044 = (Rtcm1044) new RtcmMessagesParser(messages).parse(message, false);

       Assertions.assertNull(rtcm1044);
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
