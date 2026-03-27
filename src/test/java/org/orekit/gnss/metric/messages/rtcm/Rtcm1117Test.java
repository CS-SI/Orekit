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
import org.orekit.gnss.metric.messages.rtcm.msm.Rtcm1117;
import org.orekit.gnss.metric.messages.rtcm.msm.RtcmMsmCellData;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

class Rtcm1117Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    void setUp() {
        final String m =
            // Header
            "010001011101" +                                                                                      // Message number: 1117
            "001101000101" +                                                                                      // Reference station ID
            "001011111010110000011010000000" +                                                                    // Epoch time
            "0" +                                                                                                 // Multiple message indicator
            "000" +                                                                                               // Issue of Data Station
            "0000000" +                                                                                           // Reserved
            "00" +                                                                                                // Clock steering indicator
            "00" +                                                                                                // External clock indicator
            "0" +                                                                                                 // Divergence free smoothing indicator
            "000" +                                                                                               // Smoothing interval
            "0110000000000000000000000000000000000000000000000000000000000000" +                                  // GNSS satellites mask
            "01000000000000001000000000000000" +                                                                  // GNSS signals mask
            "1111" +                                                                                              // GNSS cell mask
            
            // Satellite data (2 sats)
            "0111101101111110" +                                                                                  // 2 * Rough range integer millis
            "00000000" +                                                                                          // 2 * Extended satellite data
            "11011011110111011100" +                                                                              // 2 * Rough range mod millis
            "0000000100111111111101101011" +                                                                      // 2 * Rough phase range rate
            
            // Signal data (4 cells)
            "00001101000101010000000101111100111010101110100001100000101011101100011111101100" +                  // 4 * Fine pseudorange
            "000010010100010110110100111110100110100010110111000001110110111001011000000001110111000000100010" +  // 4 * Fine phase range
            "1011000000101100000010101011111010101111" +                                                          // 4 * Lock time indicator
            "0000" +                                                                                              // 4 * Half-cycle ambiguity indicator
            "1001001101100111111010111100111100111110" +                                                          // 4 * CNR
            "000100001011110000100001011010111100010000011111100010000101" +                                      // 4 * Fine phase range rate
            
            // Padding
            "000";

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1117);

        Utils.setDataRoot("gnss");
    }

    @Test
    void testPerfectValue() {
        final Rtcm1117 rtcm1117 = (Rtcm1117) new RtcmMessagesParser(messages, DataContext.getDefault().getTimeScales())
                .parse(message, false);

        // Verify header
        Assertions.assertEquals("837", rtcm1117.getHeader().getReferenceStation());
        Assertions.assertEquals(199952.000, rtcm1117.getHeader().getEpochTime());
        // Assertions.assertEquals("2026-03-26T16:20:09.000",
        //         rtcm1117.getHeader().getEpoch(2411).getDate().toString(DataContext.getDefault().getTimeScales().getGPS()));
        Assertions.assertEquals(false, rtcm1117.getHeader().getMultipleMessageFlag());
        Assertions.assertEquals(0, rtcm1117.getHeader().getIssueofDataStation());
        Assertions.assertEquals(0, rtcm1117.getHeader().getClockSteeringIndicator());
        Assertions.assertEquals(0, rtcm1117.getHeader().getExternalClockIndicator());
        Assertions.assertEquals(false, rtcm1117.getHeader().getDivergenceFreeSmoothingIndicator());
        Assertions.assertEquals(0, rtcm1117.getHeader().getSmoothingInterval());
        Assertions.assertEquals(0b0110000000000000000000000000000000000000000000000000000000000000L,
                rtcm1117.getHeader().getSatellitesMask());
        Assertions.assertEquals(0b01000000000000001000000000000000L, rtcm1117.getHeader().getSignalsMask());
        Assertions.assertEquals(0b1111, rtcm1117.getHeader().getCellsMask());
        Assertions.assertEquals(2, rtcm1117.getHeader().getNumberOfSatellites());
        Assertions.assertEquals(2, rtcm1117.getHeader().getNumberOfSignals());
        Assertions.assertEquals(4, rtcm1117.getHeader().getNumberOfCells());

        // Verify signals cells data
        List<RtcmMsmCellData> cells = rtcm1117.getData();
        Assertions.assertEquals(rtcm1117.getHeader().getNumberOfCells(), cells.size());

        // Verify first cell sat data
        Assertions.assertEquals(new SatInSystem("J02"), cells.get(0).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.123, cells.get(0).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(0).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(8.583984375e-4, cells.get(0).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(79, cells.get(0).getSatelliteData().getRoughPhaserangeRate());

        // Verify first cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.QZSS_1C, cells.get(0).getSignalData().getSignalId());
        Assertions.assertEquals(0.2142, cells.get(0).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(9.98079776763916e-8, cells.get(0).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(2.829674631357193e-7, cells.get(0).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(704, cells.get(0).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(36.8125, cells.get(0).getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.get(0).getSignalData().getHalfCycleAmbiguityIndicator());

        // Verify last cell sat data
        Assertions.assertEquals(new SatInSystem("J03"), cells.get(3).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.126, cells.get(3).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(3).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(4.6484375e-4, cells.get(3).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(-149, cells.get(3).getSatelliteData().getRoughPhaserangeRate());

        // Verify last cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.QZSS_2X, cells.get(3).getSignalData().getSignalId());
        Assertions.assertEquals(-0.1915, cells.get(3).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(-1.4881044626235962e-7, cells.get(3).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(2.2699031978845596e-7, cells.get(3).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(687, cells.get(3).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(51.875, cells.get(3).getSignalData().getCnr(), eps);
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
