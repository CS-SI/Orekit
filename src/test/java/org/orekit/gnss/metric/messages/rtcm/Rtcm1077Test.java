/*
 * Copyright 2022-2026 Thales Alenia Space Licensed to CS GROUP (CS) under one or more contributor license agreements. See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership. CS licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.orekit.gnss.metric.messages.rtcm;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataContext;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.metric.messages.rtcm.msm.Rtcm1077;
import org.orekit.gnss.metric.messages.rtcm.msm.RtcmMsmCellData;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.utils.IERSConventions;

class Rtcm1077Test {

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    void setUp() {
        final String m =
            // Header
            "010000110101" +                        // Message number: 1077
            "111111111111" +                        // Reference station ID
            "001101110111100001110000100000" +      // Epoch time
            "0" +                                   // Multiple message indicator
            "000" +                                 // Issue of Data Station
            "0000000" +                             // Reserved
            "01" +                                  // Clock steering indicator
            "00" +                                  // External clock indicator
            "0" +                                   // Divergence free smoothing indicator
            "000" +                                 // Smoothing interval
            "1100000000000000000000000000000000000000000000000000000000000000" + // GNSS satellites mask
            "01000000000000000000000000000000" +    // GNSS signals mask
            "11" +                                  // GNSS cell mask

            // Satellite data (2 sats)
            "0100010001000101" +                // 2 * Rough range integer millis
            "00000000" +                        // 2 * Extended satellite data
            "10011011101001001001" +            // 2 * Rough range mod millis
            "1111110111011100000011001110" +    // 2 * Rough phase range rate

            // Signal data (2 cells)
            "1101101100000101110100011010010011111111" +            // 2 * Fine pseudorange
            "111110110010010000101110000010011010001110111110" +    // 2 * Fine phase range
            "10011010011001111011" +                                // 2 * Lock time indicator
            "00" +                                                  // 2 * Half-cycle ambiguity indicator
            "10111101001100110100" +                                // 2 * CNR
            "000110110000001000111010010101" +                      // 2 * Fine phase range rate

            // Padding
            "00000";

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1077);
    }

    @Test
    void testPerfectValue() {
        final DataContext   context  = DataContext.getDefault();
        final Rtcm1077 rtcm1077 = (Rtcm1077) new RtcmMessagesParser(messages,
                                                                    context.getTimeScales(),
                                                                    context.getFrames().getEME2000(),
                                                                    context.getFrames().getITRF(IERSConventions.IERS_2010,
                                                                                                false)).
                                  parse(message, false);

        // Verify header
        Assertions.assertEquals("4095", rtcm1077.getHeader().getReferenceStation());
        Assertions.assertEquals(232660.000, rtcm1077.getHeader().getEpochTime());
        Assertions.assertFalse(rtcm1077.getHeader().getMultipleMessageFlag());
        Assertions.assertEquals(0, rtcm1077.getHeader().getIssueofDataStation());
        Assertions.assertEquals(1, rtcm1077.getHeader().getClockSteeringIndicator());
        Assertions.assertEquals(0, rtcm1077.getHeader().getExternalClockIndicator());
        Assertions.assertFalse(rtcm1077.getHeader().getDivergenceFreeSmoothingIndicator());
        Assertions.assertEquals(0, rtcm1077.getHeader().getSmoothingInterval());
        Assertions.assertEquals(0b1100000000000000000000000000000000000000000000000000000000000000L, rtcm1077.getHeader().getSatellitesMask());
        Assertions.assertEquals(0b01000000000000000000000000000000L, rtcm1077.getHeader().getSignalsMask());
        Assertions.assertEquals(0b11, rtcm1077.getHeader().getCellsMask());
        Assertions.assertEquals(2, rtcm1077.getHeader().getNumberOfSatellites());
        Assertions.assertEquals(1, rtcm1077.getHeader().getNumberOfSignals());
        Assertions.assertEquals(2, rtcm1077.getHeader().getNumberOfCells());

        // Verify signals cells data
        List<RtcmMsmCellData> cells = rtcm1077.getData();
        Assertions.assertEquals(rtcm1077.getHeader().getNumberOfCells(), cells.size());

        // Verify first cell sat data
        Assertions.assertEquals(new SatInSystem("G01"), cells.getFirst().getSatelliteData().getSatellite());
        final double eps = 1.0e-13;
        Assertions.assertEquals(0.068, cells.getFirst().getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.getFirst().getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(6.07421875e-4, cells.getFirst().getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(-137, cells.getFirst().getSatelliteData().getRoughPhaserangeRate());

        // Verify first cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.GPS_1C, cells.getFirst().getSignalData().getSignalId());
        Assertions.assertEquals(0.3457, cells.getFirst().getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(-2.821143716573715e-7, cells.getFirst().getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(-1.4827493578195572e-7, cells.getFirst().getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(617, cells.getFirst().getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(47.25, cells.getFirst().getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.getFirst().getSignalData().getHalfCycleAmbiguityIndicator());
    }

    private byte[] byteArrayFromBinary(String radix2Value) {
        final byte[] array = new byte[radix2Value.length() / 8];
        for(int i = 0; i < array.length; ++i) {
            for(int j = 0; j < 8; ++j) {
                if(radix2Value.charAt(8 * i + j) != '0') {
                    array[i] |= 0x1 << (7 - j);
                }
            }
        }
        return array;
    }

}
