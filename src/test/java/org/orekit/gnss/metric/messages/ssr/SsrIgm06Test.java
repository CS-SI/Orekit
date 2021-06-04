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
package org.orekit.gnss.metric.messages.ssr;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm06;
import org.orekit.gnss.metric.messages.ssr.igm.SsrIgm06Data;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessages;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;

public class SsrIgm06Test {

    private double eps = 1.0e-13;

    @Test
    public void testPerfectValueSBAS() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01111110" +                         // IGS Message number: 126 (SBAS)
                         "01111110011000111111" +             // Epoch Time 1s
                         "0101" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "0" +                                // Dispersive Bias Consistency Indicator
                         "0" +                                // MW Consistency Indicator
                         "000001" +                           // No. of Satellites: 1
                         "000001" +                           // Satellite ID
                         "00001" +                            // No. of Biases Processed
                         "001100010" +                        // Yaw Angle
                         "01001010"+                          // Yaw Rate 
                         "00001" +                            // GNSS Signal and Tracking Mode Identifier
                         "1" +                                // Signal Integer Indicator
                         "10" +                               // Signals Wide-Lane Integer Indicator
                         "0000" +                             // Signal Discontinuity Counter
                         "001110101110100110100000";          // Phase Bias

        final EncodedMessage message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(126);

        final SsrIgm06 igm06 = (SsrIgm06) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assert.assertEquals(1,                            igm06.getData().size());
        Assert.assertEquals(SatelliteSystem.SBAS,         igm06.getSatelliteSystem());

        // Verify header
        Assert.assertEquals(126,                          igm06.getTypeCode());
        Assert.assertEquals(517695.0,                     igm06.getHeader().getSsrEpoch1s(), eps);
        Assert.assertEquals(30.0,                         igm06.getHeader().getSsrUpdateInterval(), eps);
        Assert.assertEquals(0,                            igm06.getHeader().getSsrMultipleMessageIndicator());
        Assert.assertEquals(7,                            igm06.getHeader().getIodSsr());
        Assert.assertEquals(3951,                         igm06.getHeader().getSsrProviderId());
        Assert.assertEquals(1,                            igm06.getHeader().getSsrSolutionId());
        Assert.assertEquals(1,                            igm06.getHeader().getNumberOfSatellites());
        Assert.assertFalse(igm06.getHeader().isMelbourneWubbenaConsistencyMaintained());
        Assert.assertFalse(igm06.getHeader().isConsistencyMaintained());

        // Verify data for satellite S120
        final SsrIgm06Data s120 = igm06.getSsrIgm06Data().get("S120").get(0);
        Assert.assertEquals(120,                        s120.getSatelliteID());
        Assert.assertEquals(1,                          s120.getNumberOfBiasesProcessed());
        Assert.assertEquals(1,                          s120.getPhaseBiases().size());
        Assert.assertEquals(98.0,                       s120.getYawAngle() * 256.0 / GNSSConstants.GNSS_PI, eps);
        Assert.assertEquals(74.0,                       s120.getYawRate() * 8192.0 / GNSSConstants.GNSS_PI, eps);
        Assert.assertEquals(1,                          s120.getPhaseBias(1).getSignalID(), eps);
        Assert.assertEquals(2,                          s120.getPhaseBias(1).getSignalWideLaneIntegerIndicator());
        Assert.assertEquals(0,                          s120.getPhaseBias(1).getDiscontinuityCounter());
        Assert.assertEquals(24.1306,                    s120.getPhaseBias(1).getPhaseBias(), eps);
        Assert.assertTrue(s120.getPhaseBias(1).isSignalInteger());

    }

    @Test
    public void testPerfectValueGalileo() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                         "001" +                              // IGS SSR version
                         "01000010" +                         // IGS Message number: 66 (Galileo)
                         "01111110011000111111" +             // Epoch Time 1s
                         "1100" +                             // SSR Update Interval
                         "0" +                                // Multiple Message Indicator
                         "0111" +                             // IOD SSR
                         "0000111101101111" +                 // SSR Provider ID
                         "0001" +                             // SSR Solution ID
                         "1" +                                // Dispersive Bias Consistency Indicator
                         "1" +                                // MW Consistency Indicator
                         "000001" +                           // No. of Satellites: 1
                         "000001" +                           // Satellite ID
                         "00001" +                            // No. of Biases Processed
                         "001100010" +                        // Yaw Angle
                         "01001010"+                          // Yaw Rate 
                         "00001" +                            // GNSS Signal and Tracking Mode Identifier
                         "0" +                                // Signal Integer Indicator
                         "10" +                               // Signals Wide-Lane Integer Indicator
                         "0000" +                             // Signal Discontinuity Counter
                         "001110101110100110100000";          // Phase Bias

        final EncodedMessage message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
        message.start();

        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(66);

        final SsrIgm06 igm06 = (SsrIgm06) new IgsSsrMessagesParser(messages).parse(message, false);

        // Verify size
        Assert.assertEquals(1,                            igm06.getData().size());
        Assert.assertEquals(SatelliteSystem.GALILEO,      igm06.getSatelliteSystem());

        // Verify header
        Assert.assertEquals(66,                           igm06.getTypeCode());
        Assert.assertEquals(517695.0,                     igm06.getHeader().getSsrEpoch1s(), eps);
        Assert.assertEquals(1800.0,                       igm06.getHeader().getSsrUpdateInterval(), eps);
        Assert.assertEquals(0,                            igm06.getHeader().getSsrMultipleMessageIndicator());
        Assert.assertEquals(7,                            igm06.getHeader().getIodSsr());
        Assert.assertEquals(3951,                         igm06.getHeader().getSsrProviderId());
        Assert.assertEquals(1,                            igm06.getHeader().getSsrSolutionId());
        Assert.assertEquals(1,                            igm06.getHeader().getNumberOfSatellites());
        Assert.assertTrue(igm06.getHeader().isMelbourneWubbenaConsistencyMaintained());
        Assert.assertTrue(igm06.getHeader().isConsistencyMaintained());

        // Verify data for satellite E01
        final SsrIgm06Data e01 = igm06.getSsrIgm06Data().get("E01").get(0);
        Assert.assertEquals(1,                          e01.getSatelliteID());
        Assert.assertEquals(1,                          e01.getNumberOfBiasesProcessed());
        Assert.assertEquals(1,                          e01.getPhaseBiases().size());
        Assert.assertEquals(98.0,                       e01.getYawAngle() * 256.0 / GNSSConstants.GNSS_PI, eps);
        Assert.assertEquals(74.0,                       e01.getYawRate() * 8192.0 / GNSSConstants.GNSS_PI, eps);
        Assert.assertEquals(1,                          e01.getPhaseBias(1).getSignalID(), eps);
        Assert.assertEquals(2,                          e01.getPhaseBias(1).getSignalWideLaneIntegerIndicator());
        Assert.assertEquals(0,                          e01.getPhaseBias(1).getDiscontinuityCounter());
        Assert.assertEquals(24.1306,                    e01.getPhaseBias(1).getPhaseBias(), eps);
        Assert.assertFalse(e01.getPhaseBias(1).isSignalInteger());

    }

    @Test
    public void testNullMessage() {

        final String m = "010000100100" +                     // RTCM Message number: 1060
                        "001" +                              // IGS SSR version
                        "01101001" +                         // IGS Message number: 126 (SBAS)
                        "01111110011000111111" +             // Epoch Time 1s
                        "0101" +                             // SSR Update Interval
                        "0" +                                // Multiple Message Indicator
                        "0111" +                             // IOD SSR
                        "0000111101101111" +                 // SSR Provider ID
                        "0001" +                             // SSR Solution ID
                        "0" +                                // Dispersive Bias Consistency Indicator
                        "0" +                                // MW Consistency Indicator
                        "000001" +                           // No. of Satellites: 1
                        "000001" +                           // Satellite ID
                        "00001" +                            // No. of Biases Processed
                        "001100010" +                        // Yaw Angle
                        "01001010"+                          // Yaw Rate 
                        "00001" +                            // GNSS Signal and Tracking Mode Identifier
                        "1" +                                // Signal Integer Indicator
                        "10" +                               // Signals Wide-Lane Integer Indicator
                        "0000" +                             // Signal Discontinuity Counter
                        "001110101110100110100000";          // Phase Bias


       final EncodedMessage message = new ByteArrayEncodedMessages(byteArrayFromBinary(m));
       message.start();

       ArrayList<Integer> messages = new ArrayList<>();
       messages.add(9999999);

       final SsrIgm06 igm06 = (SsrIgm06) new IgsSsrMessagesParser(messages).parse(message, false);

       Assert.assertNull(igm06);
    }

    @Test
    public void testEmptyMessage() {
        try {
            final byte[] array = new byte[0];
            final EncodedMessage emptyMessage = new ByteArrayEncodedMessages(array);
            new IgsSsrMessagesParser(new ArrayList<Integer>()).parse(emptyMessage, false);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, oe.getSpecifier());
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
