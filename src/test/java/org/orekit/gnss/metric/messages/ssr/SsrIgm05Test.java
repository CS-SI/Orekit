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
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm05;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm05Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;

import java.util.ArrayList;

public class SsrIgm05Test {

    private double eps = 1.0e-13;

    @Test
    public void testPerfectValueBeidou() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01101001" +                         // IGS Message number: 105 (Beidou)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0100" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000001" +                           // No. of Satellites: 1
                         "000001" +                           // Satellite ID
                         "00010" +                            // No. of Biases Processed
                         "00001" +                            // GNSS Signal and Tracking Mode Identifier
                         "00111010111111"+                    // Code Bias
                         "00010" +                            // GNSS Signal and Tracking Mode Identifier
                         "001110101110100";                   // Code Bias

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(105);

        final SsrIgm05 igm05 = (SsrIgm05) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            igm05.getData().size());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,       igm05.getSatelliteSystem());

        // Verify header
        Assertions.assertEquals(105,                          igm05.getTypeCode());
        Assertions.assertEquals(517695.0,                     igm05.getHeader().getSsrEpoch1s(), eps);
        Assertions.assertEquals(15.0,                         igm05.getHeader().getSsrUpdateInterval(), eps);
        Assertions.assertEquals(0,                            igm05.getHeader().getSsrMultipleMessageIndicator());
        Assertions.assertEquals(7,                            igm05.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         igm05.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            igm05.getHeader().getSsrSolutionId());
        Assertions.assertEquals(1,                            igm05.getHeader().getNumberOfSatellites());

        // Verify data for satellite C01
        final SsrIgm05Data c01 = igm05.getSsrIgm05Data().get("C01").get(0);
        Assertions.assertEquals(1,                          c01.getSatelliteID());
        Assertions.assertEquals(2,                          c01.getNumberOfBiasesProcessed());
        Assertions.assertEquals(2,                          c01.getCodeBiases().size());
        Assertions.assertEquals(37.75,                      c01.getCodeBias(1).getCodeBias(), eps);
        Assertions.assertEquals(1,                          c01.getCodeBias(1).getSignalID());
        Assertions.assertEquals(37.70,                      c01.getCodeBias(2).getCodeBias(), eps);
        Assertions.assertEquals(2,                          c01.getCodeBias(2).getSignalID());

    }

    @Test
    public void testPerfectValueGalileo() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01000001" +                         // IGS Message number: 65 (Galileo)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1011" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "000001" +                           // No. of Satellites: 1
                         "001100" +                           // Satellite ID
                         "00010" +                            // No. of Biases Processed
                         "00001" +                            // GNSS Signal and Tracking Mode Identifier
                         "00111010111111"+                    // Code Bias
                         "00010" +                            // GNSS Signal and Tracking Mode Identifier
                         "001110101110100";                   // Code Bias

        final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(65);

        final SsrIgm05 igm05 = (SsrIgm05) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            igm05.getData().size());
        Assertions.assertEquals(SatelliteSystem.GALILEO,      igm05.getSatelliteSystem());

        // Verify header
        Assertions.assertEquals(65,                           igm05.getTypeCode());
        Assertions.assertEquals(517695.0,                     igm05.getHeader().getSsrEpoch1s(), eps);
        Assertions.assertEquals(900.0,                        igm05.getHeader().getSsrUpdateInterval(), eps);
        Assertions.assertEquals(0,                            igm05.getHeader().getSsrMultipleMessageIndicator());
        Assertions.assertEquals(7,                            igm05.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         igm05.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            igm05.getHeader().getSsrSolutionId());
        Assertions.assertEquals(1,                            igm05.getHeader().getNumberOfSatellites());

        // Verify data for satellite E12
        final SsrIgm05Data e12 = igm05.getSsrIgm05Data().get("E12").get(0);
        Assertions.assertEquals(12,                         e12.getSatelliteID());
        Assertions.assertEquals(2,                          e12.getNumberOfBiasesProcessed());
        Assertions.assertEquals(2,                          e12.getCodeBiases().size());
        Assertions.assertEquals(37.75,                      e12.getCodeBias(1).getCodeBias(), eps);
        Assertions.assertEquals(1,                          e12.getCodeBias(1).getSignalID());
        Assertions.assertEquals(37.70,                      e12.getCodeBias(2).getCodeBias(), eps);
        Assertions.assertEquals(2,                          e12.getCodeBias(2).getSignalID());

    }

    @Test
    public void testNullMessage() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "01101001" +                         // IGS Message number: 105 (Beidou)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "000010" +                           // No. of Satellites: 1
                        "000001" +                           // Satellite ID
                        "00010" +                            // No. of Biases Processed
                        "00001" +                            // GNSS Signal and Tracking Mode Identifier
                        "00111010111111"+                    // Code Bias
                        "00010" +                            // GNSS Signal and Tracking Mode Identifier
                        "00111010111010";                    // Code Bias


       final EncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm05 igm05 = (SsrIgm05) new IgsSsrMessagesParser(messages).parse(message, false);

       Assertions.assertNull(igm05);
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
