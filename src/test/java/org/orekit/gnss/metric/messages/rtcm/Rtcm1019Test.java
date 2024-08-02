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
package org.orekit.gnss.metric.messages.rtcm;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1019;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1019Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmDataField;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.time.GNSSDate;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class Rtcm1019Test {

    private double eps = 1.0e-15;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    void testParseMessage() {

        final String m = "001111111011" +                     // Message number: 1019
                        "001100" +                           // Satellite ID
                        "1111111011" +                       // Week Number
                        "0001" +                             // SV Accuracy
                        "11" +                               // GPS CODE ON L2
                        "01011101111101" +                   // IDOT
                        "10000100" +                         // IODE
                        "0000111101101111" +                 // toc
                        "01111111" +                         // af2
                        "0000101011111101" +                 // af1
                        "0100101011111101111111" +           // af0
                        "1010110111" +                       // IODC
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
                        "00000011" +                         // tGD
                        "000000" +                           // SV Health
                        "0" +                                // L2 P data flag
                        "0";                                 // Fit Interval

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(1019);

        final Rtcm1019             rtcm1019      = (Rtcm1019) new RtcmMessagesParser(messages).parse(message, false);
        final Rtcm1019Data         ephemerisData = rtcm1019.getEphemerisData();
        final GPSLegacyNavigationMessage gpsMessage    = ephemerisData.getGpsNavigationMessage();

        // Verify propagator initialization
        final GNSSPropagator propagator = new GNSSPropagatorBuilder(gpsMessage).build();
        assertNotNull(propagator);
        assertEquals(0.0, gpsMessage.getDate().
                            durationFrom(new GNSSDate(gpsMessage.getWeek(), gpsMessage.getTime(), SatelliteSystem.GPS).getDate()), eps);

        // Verify message number
        assertEquals(1019,                   rtcm1019.getTypeCode());
        assertEquals(1,                      rtcm1019.getData().size());

        // Verify navigation message
        assertEquals(12,                     gpsMessage.getPRN());
        assertEquals(1019,                   gpsMessage.getWeek());
        assertEquals(2.1475894557210572E-9,  gpsMessage.getIDot(),               eps);
        assertEquals(132,                    gpsMessage.getIODE(),               eps);
        assertEquals(3.524958E-15,           gpsMessage.getAf2(),                eps);
        assertEquals(3.1980107E-10,          gpsMessage.getAf1(),                eps);
        assertEquals(5.721445195376873E-4,   gpsMessage.getAf0(),                eps);
        assertEquals(695,                    gpsMessage.getIODC());
        assertEquals(0.0,                    gpsMessage.getCrs(),                eps);
        assertEquals(1.458749761151065E-4,   gpsMessage.getMeanMotion(),         eps);
        assertEquals(0.1671775426328288,     gpsMessage.getM0(),                 eps);
        assertEquals(0.0,                    gpsMessage.getCuc(),                eps);
        assertEquals(0.03899807028938085,    gpsMessage.getE(),                  eps);
        assertEquals(0.0,                    gpsMessage.getCus(),                eps);
        assertEquals(5153.562498092651,      FastMath.sqrt(gpsMessage.getSma()), eps);
        assertEquals(560688.0,               gpsMessage.getTime(),               eps);
        assertEquals(0.0,                    gpsMessage.getCic(),                eps);
        assertEquals(0.0,                    gpsMessage.getCis(),                eps);
        assertEquals(0.987714701321906,      gpsMessage.getI0(),                 eps);
        assertEquals(0.0,                    gpsMessage.getCrc(),                eps);
        assertEquals(0.30049130834913723,    gpsMessage.getPa(),                 eps);
        assertEquals(-5.855958209879004E-9,  gpsMessage.getOmegaDot(),           eps);
        assertEquals(0.6980085385373721,     gpsMessage.getOmega0(),             eps);
        assertEquals(1.3969839E-9,           gpsMessage.getTGD(),                eps);
        assertEquals(0.0,                    gpsMessage.getSvHealth(),           eps);

        // Verify other data
        assertEquals(12,                     ephemerisData.getSatelliteID());
        assertEquals(63216,                  ephemerisData.getGpsToc(),          eps);
        assertEquals(3,                      ephemerisData.getGpsCodeOnL2());
        assertEquals(0,                      ephemerisData.getGpsFitInterval());
        assertTrue(ephemerisData.getGpsL2PDataFlag());
        assertEquals(ephemerisData.getAccuracyProvider().getAccuracy(), gpsMessage.getSvAccuracy(), eps);

    }

    @Test
    void testNullMessage() {

        final String m = "001111111011" +                     // Message number: 1019
                        "001100" +                           // Satellite ID
                        "1111111011" +                       // Week Number
                        "0001" +                             // SV Accuracy
                        "11" +                               // GPS CODE ON L2
                        "01011101111101" +                   // IDOT
                        "10000100" +                         // IODE
                        "0000111101101111" +                 // toc
                        "01111111" +                         // af2
                        "0000101011111101" +                 // af1
                        "0100101011111101111111" +           // af0
                        "1010110111" +                       // IODC
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
                        "00000011" +                         // tGD
                        "000000" +                           // SV Health
                        "0" +                                // L2 P data flag
                        "0";                                 // Fit Interval

       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final Rtcm1019 rtcm1019 = (Rtcm1019) new RtcmMessagesParser(messages).parse(message, false);

       assertNull(rtcm1019);
    }

    @Test
    void testDF103() {
        final String m = "1111111111111111";
        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        assertFalse(RtcmDataField.DF103.booleanValue(message));
    }

    @Test
    void testEmptyMessage() {
        try {
            final byte[] array = new byte[0];
            final EncodedMessage emptyMessage = new ByteArrayEncodedMessage(array);
            new RtcmMessagesParser(new ArrayList<Integer>()).parse(emptyMessage, false);
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
