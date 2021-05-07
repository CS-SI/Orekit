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
package org.orekit.gnss.metric.messages.rtcm;

import java.util.ArrayList;

import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1020;
import org.orekit.gnss.metric.messages.rtcm.ephemeris.Rtcm1020Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessages;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmDataField;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.propagation.numerical.GLONASSNumericalPropagatorBuilder;
import org.orekit.time.GLONASSDate;

public class Rtcm1020Test {

    private double eps = 1.0e-16;

    @Before
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testParseMessage() {

        final String m = "001111111100" +                     // Message number: 1020
                        "001100" +                           // Satellite ID
                        "10001" +                            // Channel number
                        "1" +                                // Cn word
                        "0" +                                // Health availability indicator
                        "01" +                               // Word P1
                        "001100011011" +                     // tk
                        "0" +                                // Bn word
                        "1" +                                // P2 flag
                        "0011101" +                          // tb
                        "000000111110000011111010" +         // xn first derivative
                        "000011111110000011111010010" +      // xn
                        "01101" +                            // xn second derivative
                        "100000111110000011111010" +         // yn first derivative
                        "100011111110000011111010010" +      // yn
                        "11101" +                            // yn second derivative
                        "110000111110000011111010" +         // zn first derivative
                        "101011111110000011111010010" +      // zn
                        "01101" +                            // zn second derivative
                        "0" +                                // P3 flag
                        "00111100101" +                      // GLONASS γn (tb)
                        "11" +                               // Time Operation Mode
                        "0" +                                // ln (third string)
                        "0011101011001110101011" +           // τn (tb)
                        "01110" +                            // Δτn
                        "01110" +                            // En
                        "1" +                                // P4 Flag
                        "1110" +                             // Ft
                        "10011011010" +                      // Nt
                        "01" +                               // GLONASS-M Flag
                        "1" +                                // Availability of additional data
                        "10011011010" +                      // NA
                        "00001100001111100011110011110000" + // τc
                        "00111" +                            // N4
                        "0011011100111101010111" +           // τGPS
                        "0" +                                // ln (fifth string)
                        "0000000";                           // Reserved

        final EncodedMessage message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(1020);

        final Rtcm1020                 rtcm1020       = (Rtcm1020) new RtcmMessagesParser(messages).parse(message, false);
        final Rtcm1020Data             ephemerisData  = rtcm1020.getEphemerisData();
        final GLONASSNavigationMessage glonassMessage = ephemerisData.getGlonassNavigationMessage();

        // Verify propagator initialization
        final GLONASSNumericalPropagator propagator = new GLONASSNumericalPropagatorBuilder(new ClassicalRungeKuttaIntegrator(60.0), glonassMessage, true).build();
        Assert.assertNotNull(propagator);
        Assert.assertEquals(0.0, glonassMessage.getDate().
                            durationFrom(new GLONASSDate(ephemerisData.getNt(), ephemerisData.getN4(), glonassMessage.getTime()).getDate()), eps);

        Assert.assertEquals(12,                             glonassMessage.getPRN());
        Assert.assertEquals(10,                             glonassMessage.getFrequencyNumber());
        Assert.assertFalse(ephemerisData.isHealthAvailable());
        Assert.assertEquals(1800,                           ephemerisData.getP1());
        Assert.assertEquals(22410.0,                        ephemerisData.getTk(),    eps);
        Assert.assertEquals(0,                              ephemerisData.getBN());
        Assert.assertEquals(1,                              ephemerisData.getP2());
        Assert.assertEquals(26100.0,                        glonassMessage.getTime(), eps);
        Assert.assertEquals(242.42592,                      glonassMessage.getXDot(),          2.0e-6);
        Assert.assertEquals(1.21071935E-5,                  glonassMessage.getXDotDot(),       2.0e-6);
        Assert.assertEquals(4064977.5390625,                glonassMessage.getX(),             eps);
        Assert.assertEquals(-242.42592,                     glonassMessage.getYDot(),          2.0e-6);
        Assert.assertEquals(-1.21071935E-5,                 glonassMessage.getYDotDot(),       2.0e-6);
        Assert.assertEquals(-4064977.5390625,               glonassMessage.getY(),             eps);
        Assert.assertEquals(-4242.426,                      glonassMessage.getZDot(),          2.5e-4);
        Assert.assertEquals(1.21071935E-5,                  glonassMessage.getZDotDot(),       2.0e-6);
        Assert.assertEquals(-1.22569775390625E7,            glonassMessage.getZ(),             eps);
        Assert.assertEquals(4,                              ephemerisData.getP3());
        Assert.assertEquals(3,                              ephemerisData.getP());
        Assert.assertEquals(0,                              ephemerisData.getLNThirdString());
        Assert.assertEquals(14,                             ephemerisData.getEn());
        Assert.assertEquals(1.30385160446367E-8,            ephemerisData.getDeltaTN(), eps);
        Assert.assertEquals(1,                              ephemerisData.getP4());
        Assert.assertEquals(14,                             ephemerisData.getFT());
        Assert.assertEquals(1,                              ephemerisData.getM());
        Assert.assertTrue(ephemerisData.areAdditionalDataAvailable());
        Assert.assertEquals(1242,                           ephemerisData.getNA());
        Assert.assertEquals(0.095649354159832,              ephemerisData.getTauC(), eps);
        Assert.assertEquals(4.214453510940075E-4,           ephemerisData.getTauGps(), eps);
        Assert.assertEquals(0,                              ephemerisData.getLNFifthString());

    }

    @Test
    public void testNullMessage() {

        final String m = "001111111100" +                     // Message number: 1020
                        "001100" +                           // Satellite ID
                        "10001" +                            // Channel number
                        "1" +                                // Cn word
                        "0" +                                // Health availability indicator
                        "01" +                               // Word P1
                        "001100011011" +                     // tk
                        "0" +                                // Bn word
                        "1" +                                // P2 flag
                        "0011101" +                          // tb
                        "000000111110000011111010" +         // xn first derivative
                        "000011111110000011111010010" +      // xn
                        "01101" +                            // xn second derivative
                        "100000111110000011111010" +         // yn first derivative
                        "100011111110000011111010010" +      // yn
                        "11101" +                            // yn second derivative
                        "110000111110000011111010" +         // zn first derivative
                        "101011111110000011111010010" +      // zn
                        "01101" +                            // zn second derivative
                        "0" +                                // P3 flag
                        "00111100101" +                      // GLONASS γn (tb)
                        "11" +                               // Time Operation Mode
                        "0" +                                // ln (third string)
                        "0011101011001110101011" +           // τn (tb)
                        "01110" +                            // Δτn
                        "01110" +                            // En
                        "1" +                                // P4 Flag
                        "1110" +                             // Ft
                        "10011011010" +                      // Nt
                        "01" +                               // GLONASS-M Flag
                        "1" +                                // Availability of additional data
                        "10011011010" +                      // NA
                        "00001100001111100011110011110000" + // τc
                        "00111" +                            // N4
                        "0011011100111101010111" +           // τGPS
                        "0" +                                // ln (fifth string)
                        "0000000";                           // Reserved

       final EncodedMessage message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final Rtcm1020 rtcm1020 = (Rtcm1020) new RtcmMessagesParser(messages).parse(message, false);

       Assert.assertNull(rtcm1020);
    }

    @Test
    public void testAdditionalDataFields() {
        String m = "11111111111111111";
        EncodedMessage message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
        Assert.assertTrue(RtcmDataField.DF105.booleanValue(message));
        m = "11111111111111111";
        message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
        Assert.assertEquals(5, RtcmDataField.DF120.intValue(message));
        m = "00000000000000000";
        message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
        Assert.assertFalse(RtcmDataField.DF131.booleanValue(message));
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
