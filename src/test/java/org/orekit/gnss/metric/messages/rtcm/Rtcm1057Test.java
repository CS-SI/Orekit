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

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1057;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmOrbitCorrectionData;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

public class Rtcm1057Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    public void setUp() {

        final String m = "010000100001" +                      // Message number: 1057
                         "01111110011000111111" +              // GPS Epoch Time 1s
                         "0101" +                              // SSR Update Interval
                         "0" +                                 // Multiple Message Indicator
                         "0" +                                 // Satellite Reference Datum
                         "0111" +                              // IOD SSR
                         "0000111101101111" +                  // SSR Provider ID
                         "0001" +                              // SSR Solution ID
                         "000001" +                            // No. of Satellites
                         "000001" +                            // Satellite ID
                         "10000100" +                          // IODE
                         "0000101011111101111111" +            // Delta Radial
                         "01001010111111011111" +              // Delta Along-Track
                         "01001010111111011111" +              // Delta Cross-Track
                         "000010101111110111111" +             // Dot Delta Radial
                         "0100101011111101111" +               // Dot Delta Along-Track
                         "01001010111111011110000000";         // Dot Delta Cross-Track

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1057);

    }

    @Test
    public void testPerfectValue() {
        final Rtcm1057 rtcm1057 = (Rtcm1057) new RtcmMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            rtcm1057.getData().size());

        // Verify header
        Assertions.assertEquals(1057,                         rtcm1057.getTypeCode());
        Assertions.assertEquals(517695.0,                     rtcm1057.getHeader().getEpochTime1s(), eps);
        Assertions.assertEquals(30.0,                         rtcm1057.getHeader().getSsrUpdateInterval().getUpdateInterval(), eps);
        Assertions.assertEquals(0,                            rtcm1057.getHeader().getMultipleMessageIndicator());
        Assertions.assertEquals(7,                            rtcm1057.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         rtcm1057.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            rtcm1057.getHeader().getSsrSolutionId());
        Assertions.assertEquals(1,                            rtcm1057.getHeader().getNumberOfSatellites());

        // Verify data for satellite G01
        final RtcmOrbitCorrectionData g01 = rtcm1057.getDataMap().get("G01").get(0);
        Assertions.assertEquals(1,                            g01.getSatelliteID());
        Assertions.assertEquals(132,                          g01.getGnssIod());
        Assertions.assertEquals(18.0095,                      g01.getOrbitCorrection().getDeltaOrbitRadial(),        eps);
        Assertions.assertEquals(122.8668,                     g01.getOrbitCorrection().getDeltaOrbitAlongTrack(),    eps);
        Assertions.assertEquals(122.8668,                     g01.getOrbitCorrection().getDeltaOrbitCrossTrack(),    eps);
        Assertions.assertEquals(0.090047,                     g01.getOrbitCorrection().getDotOrbitDeltaRadial(),     eps);
        Assertions.assertEquals(0.614332,                     g01.getOrbitCorrection().getDotOrbitDeltaAlongTrack(), eps);
        Assertions.assertEquals(0.614332,                     g01.getOrbitCorrection().getDotOrbitDeltaCrossTrack(), eps);
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
