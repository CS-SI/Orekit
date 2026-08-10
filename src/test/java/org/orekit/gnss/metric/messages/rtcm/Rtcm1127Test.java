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
import org.orekit.gnss.metric.messages.rtcm.msm.Rtcm1127;
import org.orekit.gnss.metric.messages.rtcm.msm.RtcmMsmCellData;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.utils.IERSConventions;

class Rtcm1127Test {

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    void setUp() {
        final String m =
            // Header
            "010001100111" +                                                      // Message number: 1127
            "000001011100" +                                                      // Reference station ID
            "011000000111011000100111001000" +                                    // Epoch time
            "0" +                                                                 // Multiple message indicator
            "000" +                                                               // Issue of Data Station
            "0000000" +                                                           // Reserved
            "01" +                                                                // Clock steering indicator
            "00" +                                                                // External clock indicator
            "0" +                                                                 // Divergence free smoothing indicator
            "000" +                                                               // Smoothing interval
            "0000000000000000000010000001000000000000000000000000000000000000" +  // GNSS satellites mask
            "01000000000000000000000000000000" +                                  // GNSS signals mask
            "11" +                                                                // GNSS cell mask

            // Satellite data (2 sats)
            "0100110101010101" +                                                  // 2 * Rough range integer millis
            "00000000" +                                                          // 2 * Extended satellite data
            "10111011010010100100" +                                              // 2 * Rough range mod millis
            "0000010111101011111000011000" +                                      // 2 * Rough phase range rate

            // Signal data (2 cells)
            "0001100111110100100000010000001100101110" +                          // 2 * Fine pseudorange
            "000001100110000010111111000001000011011110100100" +                  // 2 * Fine phase range
            "01010000000101000000" +                                              // 2 * Lock time indicator
            "00" +                                                                // 2 * Half-cycle ambiguity indicator
            "11000010001010011100" +                                              // 2 * CNR
            "000011111010011111010110101010" +                                    // 2 * Fine phase range rate

            // Padding
            "00000";

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1127);

        Utils.setDataRoot("gnss");
    }

    @Test
    void testPerfectValue() {
        final DataContext   context  = DataContext.getDefault();
        final Rtcm1127 rtcm1127 = (Rtcm1127) new RtcmMessagesParser(messages,
                                                                    context.getTimeScales(),
                                                                    context.getFrames().getEME2000(),
                                                                    context.getFrames().getITRF(IERSConventions.IERS_2010,
                                                                                                false)).
                                       parse(message, false);

        // Verify header
        Assertions.assertEquals("92", rtcm1127.getHeader().getReferenceStation());
        Assertions.assertEquals(404589.000, rtcm1127.getHeader().getEpochTime());
        Assertions.assertEquals(false, rtcm1127.getHeader().getMultipleMessageFlag());
        Assertions.assertEquals(0, rtcm1127.getHeader().getIssueofDataStation());
        Assertions.assertEquals(1, rtcm1127.getHeader().getClockSteeringIndicator());
        Assertions.assertEquals(0, rtcm1127.getHeader().getExternalClockIndicator());
        Assertions.assertEquals(false, rtcm1127.getHeader().getDivergenceFreeSmoothingIndicator());
        Assertions.assertEquals(0, rtcm1127.getHeader().getSmoothingInterval());
        Assertions.assertEquals(0b0000000000000000000010000001000000000000000000000000000000000000L,
                rtcm1127.getHeader().getSatellitesMask());
        Assertions.assertEquals(0b01000000000000000000000000000000L, rtcm1127.getHeader().getSignalsMask());
        Assertions.assertEquals(0b11, rtcm1127.getHeader().getCellsMask());
        Assertions.assertEquals(2, rtcm1127.getHeader().getNumberOfSatellites());
        Assertions.assertEquals(1, rtcm1127.getHeader().getNumberOfSignals());
        Assertions.assertEquals(2, rtcm1127.getHeader().getNumberOfCells());

        // Verify signals cells data
        List<RtcmMsmCellData> cells = rtcm1127.getData();
        Assertions.assertEquals(rtcm1127.getHeader().getNumberOfCells(), cells.size());

        // Verify first cell sat data
        Assertions.assertEquals(new SatInSystem("C21"), cells.getFirst().getSatelliteData().getSatellite());
        final double eps = 1.0e-13;
        Assertions.assertEquals(0.077, cells.getFirst().getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.getFirst().getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(7.314453125e-4, cells.getFirst().getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(378, cells.getFirst().getSatelliteData().getRoughPhaserangeRate());

        // Verify first cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.BDS_2I, cells.getFirst().getSignalData().getSignalId());
        Assertions.assertEquals(0.2003, cells.getFirst().getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(1.980215311050415e-7, cells.getFirst().getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(1.946385018527508e-7, cells.getFirst().getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(320, cells.getFirst().getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(48.5, cells.getFirst().getSignalData().getCnr(), eps);
        Assertions.assertEquals(false, cells.getFirst().getSignalData().getHalfCycleAmbiguityIndicator());

        // Verify last cell sat data
        Assertions.assertEquals(new SatInSystem("C28"), cells.get(1).getSatelliteData().getSatellite());
        Assertions.assertEquals(0.085, cells.get(1).getSatelliteData().getIntMillisRoughRange(), eps);
        Assertions.assertEquals(0, cells.get(1).getSatelliteData().getExtendedSatelliteData());
        Assertions.assertEquals(1.6015625e-4, cells.get(1).getSatelliteData().getModMillisRoughRange(), eps);
        Assertions.assertEquals(-488, cells.get(1).getSatelliteData().getRoughPhaserangeRate());

        // Verify last cell sig data
        Assertions.assertEquals(RtcmMsmSignalId.BDS_2I, cells.get(1).getSignalData().getSignalId());
        Assertions.assertEquals(-0.2646, cells.get(1).getSignalData().getFinePhaserangeRate(), eps);
        Assertions.assertEquals(1.23586505651474e-7, cells.get(1).getSignalData().getFinePseudorange(), eps);
        Assertions.assertEquals(1.2870319187641144e-7, cells.get(1).getSignalData().getFinePhaserange(), eps);
        Assertions.assertEquals(320, cells.get(1).getSignalData().getLockTimeIndicator());
        Assertions.assertEquals(41.75, cells.get(1).getSignalData().getCnr(), eps);
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
