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
import org.orekit.gnss.metric.messages.rtcm.msm.Rtcm1087;
import org.orekit.gnss.metric.messages.rtcm.msm.RtcmMsmCellData;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

class Rtcm1087Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    void setUp() {
        final String m =
            // Header
            "010000111111" +                                                      // Message number: 1087
            "111111111111" +                                                      // Reference station ID
            "111100001000001010011111101000" +                                    // Epoch time
            "0" +                                                                 // Multiple message indicator
            "000" +                                                               // Issue of Data Station
            "0000000" +                                                           // Reserved
            "01" +                                                                // Clock steering indicator
            "00" +                                                                // External clock indicator
            "0" +                                                                 // Divergence free smoothing indicator
            "000" +                                                               // Smoothing interval
            "0000000000001100000000000000000000000000000000000000000000000000" +  // GNSS satellites mask
            "01000000000000000000000000000000" +                                  // GNSS signals mask
            "11" +                                                                // GNSS cell mask
            
            // Satellite data (2 sats)
            "0100110001001101" +                                                  // 2 * Rough range integer millis
            "01010000" +                                                          // 2 * Extended satellite data
            "00100111100001111111" +                                              // 2 * Rough range mod millis
            "0000100101001011111111010000" +                                      // 2 * Rough phase range rate
            
            // Signal data (2 cells)
            "0010100111101100100100111110110101110110" +                          // 2 * Fine pseudorange
            "000010101001011000101110000011111101110001111000" +                  // 2 * Fine phase range
            "01010010010101011001" +                                              // 2 * Lock time indicator
            "00" +                                                                // 2 * Half-cycle ambiguity indicator
            "10011001001100010100" +                                              // 2 * CNR
            "111100011101001000101011111111" +                                    // 2 * Fine phase range rate
            
            // Padding
            "00000" +                                                             // Padding
            "";

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1087);

        Utils.setDataRoot("gnss");
    }

    @Test
    void testPerfectValue() {
        final Rtcm1087 rtcm1087 = (Rtcm1087) new RtcmMessagesParser(messages, DataContext.getDefault().getTimeScales())
                .parse(message, false);

        // Verify header
        Assertions.assertEquals("4095", rtcm1087.getHeader().getReferenceStation());
        Assertions.assertEquals(69249.000, rtcm1087.getHeader().getEpochTime());
        Assertions.assertEquals(7, rtcm1087.getHeader().getDayOfWeek()); // Unknown 
        Assertions.assertEquals("2026-03-26T16:14:27.000",
                rtcm1087.getHeader().getEpoch(816, 8).getDate().toString(DataContext.getDefault().getTimeScales().getGPS()));
        Assertions.assertEquals(false, rtcm1087.getHeader().getMultipleMessageFlag());
        Assertions.assertEquals(0, rtcm1087.getHeader().getIssueofDataStation());
        Assertions.assertEquals(1, rtcm1087.getHeader().getClockSteeringIndicator());
        Assertions.assertEquals(0, rtcm1087.getHeader().getExternalClockIndicator());
        Assertions.assertEquals(false, rtcm1087.getHeader().getDivergenceFreeSmoothingIndicator());
        Assertions.assertEquals(0, rtcm1087.getHeader().getSmoothingInterval());
        Assertions.assertEquals(0b0000000000001100000000000000000000000000000000000000000000000000L,
                rtcm1087.getHeader().getSatellitesMask());
        Assertions.assertEquals(0b01000000000000000000000000000000L, rtcm1087.getHeader().getSignalsMask());
        Assertions.assertEquals(0b11, rtcm1087.getHeader().getCellsMask());
        Assertions.assertEquals(2, rtcm1087.getHeader().getNumberOfSatellites());
        Assertions.assertEquals(1, rtcm1087.getHeader().getNumberOfSignals());
        Assertions.assertEquals(2, rtcm1087.getHeader().getNumberOfCells());

        // Verify signals cells data
        List<RtcmMsmCellData> cells = rtcm1087.getData();
        Assertions.assertEquals(rtcm1087.getHeader().getNumberOfCells(), cells.size());

        // Verify first cell sat data
        Assertions.assertEquals(new SatInSystem("R13"), cells.get(0).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.076, cells.get(0).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(-2, cells.get(0).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(1.54296875e-4, cells.get(0).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(594, cells.get(0).getSatelliteData().getRoughPhaserangeRate());

        // Verify first cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.GLO_1C, cells.get(0).getSignalData().getSignalId());
        Assertions.assertEquals(-0.1815, cells.get(0).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(3.1985528767108917e-7, cells.get(0).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(3.2307859510183334e-7, cells.get(0).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(329, cells.get(0).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(38.25, cells.get(0).getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.get(0).getSignalData().getHalfCycleAmbiguityIndicator());

        // Verify last cell sat data
        Assertions.assertEquals(new SatInSystem("R14"), cells.get(1).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.077, cells.get(1).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(-7, cells.get(1).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(1.240234375e-4, cells.get(1).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(-48, cells.get(1).getSatelliteData().getRoughPhaserangeRate());

        // Verify last cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.GLO_1C, cells.get(1).getSignalData().getSignalId());
        Assertions.assertEquals(0.2815, cells.get(1).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(4.794411361217499e-7, cells.get(1).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(4.840455949306488e-7, cells.get(1).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(345, cells.get(1).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(49.25, cells.get(1).getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.get(1).getSignalData().getHalfCycleAmbiguityIndicator());
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
