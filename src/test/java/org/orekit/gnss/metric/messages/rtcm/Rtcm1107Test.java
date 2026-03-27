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
import org.orekit.gnss.metric.messages.rtcm.msm.Rtcm1107;
import org.orekit.gnss.metric.messages.rtcm.msm.RtcmMsmCellData;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

class Rtcm1107Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    void setUp() {
        final String m =
            // Header
            "010001010011" +                                                                                      // Message number: 1107
            "000001011100" +                                                                                      // Reference station ID
            "011000000110101100101010101000" +                                                                    // Epoch time
            "0" +                                                                                                 // Multiple message indicator
            "000" +                                                                                               // Issue of Data Station
            "0000000" +                                                                                           // Reserved
            "01" +                                                                                                // Clock steering indicator
            "00" +                                                                                                // External clock indicator
            "0" +                                                                                                 // Divergence free smoothing indicator
            "000" +                                                                                               // Smoothing interval
            "0001000100000000000000000000000000000000000000000000000000000000" +                                  // GNSS satellites mask
            "01000000000000000000000100000000" +                                                                  // GNSS signals mask
            "1111" +                                                                                              // GNSS cell mask
            
            // Satellite data (2 sats)
            "0111111110000100" +                                                                                  // 2 * Rough range integer millis
            "00000000" +                                                                                          // 2 * Extended satellite data
            "01010010101010010001" +                                                                              // 2 * Rough range mod millis
            "0000000100001000000000110111" +                                                                      // 2 * Rough phase range rate
            
            // Signal data (4 cells)
            "01001000100101101000000111110010011111101111001110101000001111111101101100101011" +                  // 4 * Fine pseudorange
            "000100110100000000101111000001111111111100101000111111100110110110111100111111111110001110010111" +  // 4 * Fine phase range
            "0110100001010100000001101000010110100001" +                                                          // 4 * Lock time indicator
            "0000" +                                                                                              // 4 * Half-cycle ambiguity indicator
            "1010010000110011000010001000001010100000" +                                                          // 4 * CNR
            "110111000100011110110110000000000101000100101000100110010011" +                                      // 4 * Fine phase range rate
            
            // Padding
            "000";

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1107);

        Utils.setDataRoot("gnss");
    }

    @Test
    void testPerfectValue() {
        final Rtcm1107 rtcm1107 = (Rtcm1107) new RtcmMessagesParser(messages, DataContext.getDefault().getTimeScales())
                .parse(message, false);

        // Verify header
        Assertions.assertEquals("92", rtcm1107.getHeader().getReferenceStation());
        Assertions.assertEquals(404409.000, rtcm1107.getHeader().getEpochTime());
        Assertions.assertEquals("2026-03-26T16:20:09.000",
                rtcm1107.getHeader().getEpoch(2411).getDate().toString(DataContext.getDefault().getTimeScales().getGPS()));
        Assertions.assertEquals(false, rtcm1107.getHeader().getMultipleMessageFlag());
        Assertions.assertEquals(0, rtcm1107.getHeader().getIssueofDataStation());
        Assertions.assertEquals(1, rtcm1107.getHeader().getClockSteeringIndicator());
        Assertions.assertEquals(0, rtcm1107.getHeader().getExternalClockIndicator());
        Assertions.assertEquals(false, rtcm1107.getHeader().getDivergenceFreeSmoothingIndicator());
        Assertions.assertEquals(0, rtcm1107.getHeader().getSmoothingInterval());
        Assertions.assertEquals(0b0001000100000000000000000000000000000000000000000000000000000000L,
                rtcm1107.getHeader().getSatellitesMask());
        Assertions.assertEquals(0b01000000000000000000000100000000L, rtcm1107.getHeader().getSignalsMask());
        Assertions.assertEquals(0b1111, rtcm1107.getHeader().getCellsMask());
        Assertions.assertEquals(2, rtcm1107.getHeader().getNumberOfSatellites());
        Assertions.assertEquals(2, rtcm1107.getHeader().getNumberOfSignals());
        Assertions.assertEquals(4, rtcm1107.getHeader().getNumberOfCells());

        // Verify signals cells data
        List<RtcmMsmCellData> cells = rtcm1107.getData();
        Assertions.assertEquals(rtcm1107.getHeader().getNumberOfCells(), cells.size());

        // Verify first cell sat data
        Assertions.assertEquals(new SatInSystem("S23"), cells.get(0).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.127, cells.get(0).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(0).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(3.22265625e-4, cells.get(0).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(66, cells.get(0).getSatelliteData().getRoughPhaserangeRate());

        // Verify first cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.SBAS_1C, cells.get(0).getSignalData().getSignalId());
        Assertions.assertEquals(-0.4573, cells.get(0).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(5.538016557693481e-7, cells.get(0).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(5.874852649867535e-7, cells.get(0).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(417, cells.get(0).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(41.0, cells.get(0).getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.get(0).getSignalData().getHalfCycleAmbiguityIndicator());

        // Verify last cell sat data
        Assertions.assertEquals(new SatInSystem("S27"), cells.get(3).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.132, cells.get(3).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(3).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(6.416015625e-4, cells.get(3).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(55, cells.get(3).getSatelliteData().getRoughPhaserangeRate());

        // Verify last cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.SBAS_5X, cells.get(3).getSignalData().getSignalId());
        Assertions.assertEquals(0.2451, cells.get(3).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(-1.7562881112098694e-8, cells.get(3).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(-3.3867545425891876e-9, cells.get(3).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(417, cells.get(3).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(42.0, cells.get(3).getSignalData().getCnr(), eps);
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
