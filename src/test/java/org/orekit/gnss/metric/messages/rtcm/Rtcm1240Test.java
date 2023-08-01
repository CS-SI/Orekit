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
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1240;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmOrbitCorrectionData;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

public class Rtcm1240Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    public void setUp() {

        final String m = "010011011000" +                       // Message Number: 1240
                         "00001111110011000111" +               // Galileo Epoch Time 1s
                         "0101" +                               // SSR Update Interval
                         "0" +                                  // Multiple Message Indicator
                         "0" +                                  // Satellite Reference Datum
                         "0111" +                               // IOD SSR
                         "0000111101101111" +                   // SSR Provider ID
                         "0001" +                               // SSR Solution ID
                         "000001" +                             // No. of Satellites
                         "000001" +                             // Satellite ID
                         "0010000100" +                         // Galileo IOD
                         "0000101011111101111111" +             // Delta Radial
                         "01001010111111011111" +               // Delta Along-Track
                         "01001010111111011111" +               // Delta Cross-Track
                         "000010101111110111111" +              // Dot Delta Radial
                         "0100101011111101111" +                // Dot Delta Along-Track
                         "010010101111110111100000";            // Dot Delta Cross-Track

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1240);

    }

    @Test
    public void testPerfectValue() {
        final Rtcm1240 rtcm1240 = (Rtcm1240) new RtcmMessagesParser(messages).parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            rtcm1240.getData().size());

        // Verify header
        Assertions.assertEquals(1240,                         rtcm1240.getTypeCode());
        Assertions.assertEquals(64711.0,                      rtcm1240.getHeader().getEpochTime1s(), eps);
        Assertions.assertEquals(30.0,                         rtcm1240.getHeader().getSsrUpdateInterval().getUpdateInterval(), eps);
        Assertions.assertEquals(0,                            rtcm1240.getHeader().getMultipleMessageIndicator());
        Assertions.assertEquals(7,                            rtcm1240.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         rtcm1240.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            rtcm1240.getHeader().getSsrSolutionId());
        Assertions.assertEquals(1,                            rtcm1240.getHeader().getNumberOfSatellites());

        // Verify data for satellite E01
        final RtcmOrbitCorrectionData e01 = rtcm1240.getDataMap().get("E01").get(0);
        Assertions.assertEquals(1,                            e01.getSatelliteID());
        Assertions.assertEquals(132,                          e01.getGnssIod());
        Assertions.assertEquals(18.0095,                      e01.getOrbitCorrection().getDeltaOrbitRadial(),        eps);
        Assertions.assertEquals(122.8668,                     e01.getOrbitCorrection().getDeltaOrbitAlongTrack(),    eps);
        Assertions.assertEquals(122.8668,                     e01.getOrbitCorrection().getDeltaOrbitCrossTrack(),    eps);
        Assertions.assertEquals(0.090047,                     e01.getOrbitCorrection().getDotOrbitDeltaRadial(),     eps);
        Assertions.assertEquals(0.614332,                     e01.getOrbitCorrection().getDotOrbitDeltaAlongTrack(), eps);
        Assertions.assertEquals(0.614332,                     e01.getOrbitCorrection().getDotOrbitDeltaCrossTrack(), eps);
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
