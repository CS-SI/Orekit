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
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class SsrIm201Test {

    private double eps = 1.0e-13;

    @Test
    void testPerfectValue() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "11001001" +                         // IGS Message number: 201 (VTEC)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0101" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000000001" +                        // VTEC Quality Indicator
                         "00" +                               // Number of Ionospheric Layers: 1
                         "01000001" +                         // Height of ionospheric layer
                         "0010" +                             // Spherical Harmonics Degree
                         "0001" +                             // Spherical Harmonics Order
                         "0100101000101101"+                  // C00
                         "0101011001101101"+                  // C10
                         "0110101010001100"+                  // C20
                         "0100001100101111"+                  // C30
                         "0100101011101101"+                  // C11
                         "0100101000111110"+                  // C21
                         "0010100110101101"+                  // C31
                         "0000111101101101"+                  // C22
                         "0111101000101100"+                  // C32
                         "0111101110101101"+                  // S11
                         "0000101000101100"+                  // S21
                         "0110101000101001"+                  // S31
                         "0110101000100100"+                  // S22
                         "001100100010110100000";             // S32

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(201);

        final SsrIm201 im201 = (SsrIm201) new IgsSsrMessagesParser(messages).parse(message, false);

        // Ionospheric model
        assertNotNull(im201.getIonosphericModel());

        // Verify size
        assertEquals(1,                            im201.getData().size());

        // Verify header
        assertEquals(201,                          im201.getTypeCode());
        assertEquals(517695.0,                     im201.getHeader().getSsrEpoch1s(), eps);
        assertEquals(30.0,                         im201.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            im201.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            im201.getHeader().getIodSsr());
        assertEquals(3951,                         im201.getHeader().getSsrProviderId());
        assertEquals(1,                            im201.getHeader().getSsrSolutionId());
        assertEquals(0.05,                         im201.getHeader().getVtecQualityIndicator(), eps);
        assertEquals(1,                            im201.getHeader().getNumberOfIonosphericLayers());

        // Verify data
        final SsrIm201Data data = im201.getData().get(0);
        final double[][] cnm = data.getCnm();
        final double[][] snm = data.getSnm();
        assertEquals(650000,            data.getHeightIonosphericLayer(), eps);
        assertEquals(3,                 data.getSphericalHarmonicsDegree());
        assertEquals(2,                 data.getSphericalHarmonicsOrder());
        assertEquals(94.945,            cnm[0][0], eps);
        assertEquals(110.625,           cnm[1][0], eps);
        assertEquals(136.380,           cnm[2][0], eps);
        assertEquals(85.995,            cnm[3][0], eps);
        assertEquals(0.0,               cnm[0][1], eps);
        assertEquals(95.905,            cnm[1][1], eps);
        assertEquals(95.030,            cnm[2][1], eps);
        assertEquals(53.345,            cnm[3][1], eps);
        assertEquals(0.0,               cnm[0][2], eps);
        assertEquals(0.0,               cnm[1][2], eps);
        assertEquals(19.745,            cnm[2][2], eps);
        assertEquals(156.380,           cnm[3][2], eps);
        assertEquals(0.0,               snm[0][0], eps);
        assertEquals(0.0,               snm[1][0], eps);
        assertEquals(0.0,               snm[2][0], eps);
        assertEquals(0.0,               snm[3][0], eps);
        assertEquals(0.0,               snm[0][1], eps);
        assertEquals(158.305,           snm[1][1], eps);
        assertEquals(13.020,            snm[2][1], eps);
        assertEquals(135.885,           snm[3][1], eps);
        assertEquals(0.0,               snm[0][2], eps);
        assertEquals(0.0,               snm[1][2], eps);
        assertEquals(135.860,           snm[2][2], eps);
        assertEquals(64.225,            snm[3][2], eps);

    }

    @Test
    void testPerfectValue2() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "11001001" +                         // IGS Message number: 201 (VTEC)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1110" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000000001" +                        // VTEC Quality Indicator
                         "00" +                               // Number of Ionospheric Layers: 1
                         "01000001" +                         // Height of ionospheric layer
                         "0010" +                             // Spherical Harmonics Degree
                         "0001" +                             // Spherical Harmonics Order
                         "0100101000101101"+                  // C00
                         "0101011001101101"+                  // C10
                         "0110101010001100"+                  // C20
                         "0100001100101111"+                  // C30
                         "0100101011101101"+                  // C11
                         "0100101000111110"+                  // C21
                         "0010100110101101"+                  // C31
                         "0000111101101101"+                  // C22
                         "0111101000101100"+                  // C32
                         "0111101110101101"+                  // S11
                         "0000101000101100"+                  // S21
                         "0110101000101001"+                  // S31
                         "0110101000100100"+                  // S22
                         "001100100010110100000";             // S32

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(201);

        final SsrIm201 im201 = (SsrIm201) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        assertEquals(1,                            im201.getData().size());

        // Verify header
        assertEquals(201,                          im201.getTypeCode());
        assertEquals(517695.0,                     im201.getHeader().getSsrEpoch1s(), eps);
        assertEquals(7200.0,                       im201.getHeader().getSsrUpdateInterval(), eps);
        assertEquals(0,                            im201.getHeader().getSsrMultipleMessageIndicator());
        assertEquals(7,                            im201.getHeader().getIodSsr());
        assertEquals(3951,                         im201.getHeader().getSsrProviderId());
        assertEquals(1,                            im201.getHeader().getSsrSolutionId());
        assertEquals(0.05,                         im201.getHeader().getVtecQualityIndicator(), eps);
        assertEquals(1,                            im201.getHeader().getNumberOfIonosphericLayers());

        // Verify data
        final SsrIm201Data data = im201.getData().get(0);
        final double[][] cnm = data.getCnm();
        final double[][] snm = data.getSnm();
        assertEquals(650000,            data.getHeightIonosphericLayer(), eps);
        assertEquals(3,                 data.getSphericalHarmonicsDegree());
        assertEquals(2,                 data.getSphericalHarmonicsOrder());
        assertEquals(94.945,            cnm[0][0], eps);
        assertEquals(110.625,           cnm[1][0], eps);
        assertEquals(136.380,           cnm[2][0], eps);
        assertEquals(85.995,            cnm[3][0], eps);
        assertEquals(0.0,               cnm[0][1], eps);
        assertEquals(95.905,            cnm[1][1], eps);
        assertEquals(95.030,            cnm[2][1], eps);
        assertEquals(53.345,            cnm[3][1], eps);
        assertEquals(0.0,               cnm[0][2], eps);
        assertEquals(0.0,               cnm[1][2], eps);
        assertEquals(19.745,            cnm[2][2], eps);
        assertEquals(156.380,           cnm[3][2], eps);
        assertEquals(0.0,               snm[0][0], eps);
        assertEquals(0.0,               snm[1][0], eps);
        assertEquals(0.0,               snm[2][0], eps);
        assertEquals(0.0,               snm[3][0], eps);
        assertEquals(0.0,               snm[0][1], eps);
        assertEquals(158.305,           snm[1][1], eps);
        assertEquals(13.020,            snm[2][1], eps);
        assertEquals(135.885,           snm[3][1], eps);
        assertEquals(0.0,               snm[0][2], eps);
        assertEquals(0.0,               snm[1][2], eps);
        assertEquals(135.860,           snm[2][2], eps);
        assertEquals(64.225,            snm[3][2], eps);

    }

    @Test
    void testNullMessage() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "11001001" +                         // IGS Message number: 201 (VTEC)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "000000001" +                        // VTEC Quality Indicator
                        "00" +                               // Number of Ionospheric Layers: 1
                        "01000001" +                         // Height of ionospheric layer
                        "0010" +                             // Spherical Harmonics Degree
                        "0001" +                             // Spherical Harmonics Order
                        "0100101000101101"+                  // C00
                        "0101011001101101"+                  // C10
                        "0110101010001100"+                  // C20
                        "0100001100101111"+                  // C30
                        "0100101011101101"+                  // C11
                        "0100101000111110"+                  // C21
                        "0010100110101101"+                  // C31
                        "0000111101101101"+                  // C22
                        "0111101000101100"+                  // C32
                        "0111101110101101"+                  // S11
                        "0000101000101100"+                  // S21
                        "0110101000101001"+                  // S31
                        "0110101000100100"+                  // S22
                        "001100100010110100000";             // S32


       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIm201 im201 = (SsrIm201) new IgsSsrMessagesParser(messages).parse(message, false);

       assertNull(im201);
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
