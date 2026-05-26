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
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.metric.messages.rtcm.msm.Rtcm1097;
import org.orekit.gnss.metric.messages.rtcm.msm.RtcmMsmCellData;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

class Rtcm1097Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    void setUp() {
        final String m =
            "010001001001" +                                                                                      // Message number: 1097
            "111111111111" +                                                                                      // Reference station ID
            "011000000011110000101011011000" +                                                                    // Epoch time
            "0" +                                                                                                 // Multiple message indicator
            "000" +                                                                                               // Issue of Data Station
            "0000000" +                                                                                           // Reserved
            "01" +                                                                                                // Clock steering indicator
            "00" +                                                                                                // External clock indicator
            "0" +                                                                                                 // Divergence free smoothing indicator
            "000" +                                                                                               // Smoothing interval
            "0000001100000000000000000000000000000000000000000000000000000000" +                                  // GNSS satellites mask
            "00001000001000000000000000000000" +                                                                  // GNSS signals mask
            "1111" +                                                                                              // GNSS cell mask

            // Satellite data (2 sats)
            "0101010101011010" +                                                                                  // 2 * Rough range integer millis
            "00000000" +                                                                                          // 2 * Extended satellite data
            "11111110101100001101" +                                                                              // 2 * Rough range mod millis
            "1111110011000000000100000101" +                                                                      // 2 * Rough phase range rate

            // Signal data (4 cells)
            "11110110111110101011111111000011001010000000001101000011001000000101100101011101" +                  // 4 * Fine pseudorange
            "111111100000011111100010111111110110011001100100000000001110101111011100000000011001110111100101" +  // 4 * Fine phase range
            "0110101010011010101001101010100110101001" +                                                          // 4 * Lock time indicator
            "0000" +                                                                                              // 4 * Half-cycle ambiguity indicator
            "1010010100101100010010111100001000001100" +                                                          // 4 * CNR
            "111100110110010111101101011011111101100100110111101010101000" +                                      // 4 * Fine phase range rate

            // Padding
            "000";

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1097);

        Utils.setDataRoot("gnss");
    }

    @Test
    void testPerfectValue() {
        final Rtcm1097 rtcm1097 = (Rtcm1097) new RtcmMessagesParser(messages, DataContext.getDefault().getTimeScales())
                .parse(message, false);

        // Verify header
        Assertions.assertEquals("4095", rtcm1097.getHeader().getReferenceStation());
        Assertions.assertEquals(403639.000, rtcm1097.getHeader().getEpochTime());
        Assertions.assertEquals(false, rtcm1097.getHeader().getMultipleMessageFlag());
        Assertions.assertEquals(0, rtcm1097.getHeader().getIssueofDataStation());
        Assertions.assertEquals(1, rtcm1097.getHeader().getClockSteeringIndicator());
        Assertions.assertEquals(0, rtcm1097.getHeader().getExternalClockIndicator());
        Assertions.assertEquals(false, rtcm1097.getHeader().getDivergenceFreeSmoothingIndicator());
        Assertions.assertEquals(0, rtcm1097.getHeader().getSmoothingInterval());
        Assertions.assertEquals(0b0000001100000000000000000000000000000000000000000000000000000000L,
                rtcm1097.getHeader().getSatellitesMask());
        Assertions.assertEquals(0b00001000001000000000000000000000L, rtcm1097.getHeader().getSignalsMask());
        Assertions.assertEquals(0b1111, rtcm1097.getHeader().getCellsMask());
        Assertions.assertEquals(2, rtcm1097.getHeader().getNumberOfSatellites());
        Assertions.assertEquals(2, rtcm1097.getHeader().getNumberOfSignals());
        Assertions.assertEquals(4, rtcm1097.getHeader().getNumberOfCells());

        // Verify signals cells data
        List<RtcmMsmCellData> cells = rtcm1097.getData();
        Assertions.assertEquals(rtcm1097.getHeader().getNumberOfCells(), cells.size());

        // Verify first cell sat data
        Assertions.assertEquals(new SatInSystem("E07"), cells.get(0).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.085, cells.get(0).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(0).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(9.94140625e-4, cells.get(0).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(-208, cells.get(0).getSatelliteData().getRoughPhaserangeRate());

        // Verify first cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.GAL_1X, cells.get(0).getSignalData().getSignalId());
        Assertions.assertEquals(-0.1614, cells.get(0).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(-6.882287561893463e-8, cells.get(0).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(-6.009545177221298e-8, cells.get(0).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(426, cells.get(0).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(41.25, cells.get(0).getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.get(0).getSignalData().getHalfCycleAmbiguityIndicator());

        // Verify last cell sat data
        Assertions.assertEquals(new SatInSystem("E08"), cells.get(3).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.090, cells.get(3).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(3).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(7.626953125e-4, cells.get(3).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(261, cells.get(3).getSatelliteData().getRoughPhaserangeRate());

        // Verify last cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.GAL_6X, cells.get(3).getSignalData().getSignalId());
        Assertions.assertEquals(-0.1368, cells.get(3).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(4.2611733078956604e-8, cells.get(3).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(4.934007301926613e-8, cells.get(3).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(425, cells.get(3).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(32.75, cells.get(3).getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.get(3).getSignalData().getHalfCycleAmbiguityIndicator());
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
