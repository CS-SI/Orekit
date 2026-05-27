/* Copyright 2022-2026 Thales Alenia Space
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

package org.orekit.gnss.metric.messages.rtcm.msm.headers;

import org.orekit.gnss.PredefinedObservationType;

/**
 * Enumeration of RTCM MSM signal identifiers and their associated observation types.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public enum RtcmMsmSignalId {
    /** As per DF395 definition, if an ID is noted as Reserved, the decoding software should decode the observables, but refrain from using them. */
    RESERVED(null, null, null, null),

    /** GPS L1 C/A MSM signal (code C1C, phase L1C, Doppler D1C, SNR S1C). */
    GPS_1C(PredefinedObservationType.C1C, PredefinedObservationType.L1C, PredefinedObservationType.D1C, PredefinedObservationType.S1C),
    /** GPS L1 P(Y) MSM signal (code C1P, phase L1P, Doppler D1P, SNR S1P). */
    GPS_1P(PredefinedObservationType.C1P, PredefinedObservationType.L1P, PredefinedObservationType.D1P, PredefinedObservationType.S1P),
    /** GPS L1 W (P(Y) encrypted) MSM signal (code C1W, phase L1W, Doppler D1W, SNR S1W). */
    GPS_1W(PredefinedObservationType.C1W, PredefinedObservationType.L1W, PredefinedObservationType.D1W, PredefinedObservationType.S1W),
    /** GPS L2 C/A MSM signal (code C2C, phase L2C, Doppler D2C, SNR S2C). */
    GPS_2C(PredefinedObservationType.C2C, PredefinedObservationType.L2C, PredefinedObservationType.D2C, PredefinedObservationType.S2C),
    /** GPS L2 P(Y) MSM signal (code C2P, phase L2P, Doppler D2P, SNR S2P). */
    GPS_2P(PredefinedObservationType.C2P, PredefinedObservationType.L2P, PredefinedObservationType.D2P, PredefinedObservationType.S2P),
    /** GPS L2 W (P(Y) encrypted) MSM signal (code C2W, phase L2W, Doppler D2W, SNR S2W). */
    GPS_2W(PredefinedObservationType.C2W, PredefinedObservationType.L2W, PredefinedObservationType.D2W, PredefinedObservationType.S2W),
    /** GPS L2C(M) MSM signal (code C2S, phase L2S, Doppler D2S, SNR S2S). */
    GPS_2S(PredefinedObservationType.C2S, PredefinedObservationType.L2S, PredefinedObservationType.D2S, PredefinedObservationType.S2S),
    /** GPS L2C(L) MSM signal (code C2L, phase L2L, Doppler D2L, SNR S2L). */
    GPS_2L(PredefinedObservationType.C2L, PredefinedObservationType.L2L, PredefinedObservationType.D2L, PredefinedObservationType.S2L),
    /** GPS L2C(M+L) MSM signal (code C2X, phase L2X, Doppler D2X, SNR S2X). */
    GPS_2X(PredefinedObservationType.C2X, PredefinedObservationType.L2X, PredefinedObservationType.D2X, PredefinedObservationType.S2X),
    /** GPS L5 I MSM signal (code C5I, phase L5I, Doppler D5I, SNR S5I). */
    GPS_5I(PredefinedObservationType.C5I, PredefinedObservationType.L5I, PredefinedObservationType.D5I, PredefinedObservationType.S5I),
    /** GPS L5 Q MSM signal (code C5Q, phase L5Q, Doppler D5Q, SNR S5Q). */
    GPS_5Q(PredefinedObservationType.C5Q, PredefinedObservationType.L5Q, PredefinedObservationType.D5Q, PredefinedObservationType.S5Q),
    /** GPS L5 I+Q MSM signal (code C5X, phase L5X, Doppler D5X, SNR S5X). */
    GPS_5X(PredefinedObservationType.C5X, PredefinedObservationType.L5X, PredefinedObservationType.D5X, PredefinedObservationType.S5X),
    /** GPS L1C(D) MSM signal (code C1S, phase L1S, Doppler D1S, SNR S1S). */
    GPS_1S(PredefinedObservationType.C1S, PredefinedObservationType.L1S, PredefinedObservationType.D1S, PredefinedObservationType.S1S),
    /** GPS L1C(P) MSM signal (code C1L, phase L1L, Doppler D1L, SNR S1L). */
    GPS_1L(PredefinedObservationType.C1L, PredefinedObservationType.L1L, PredefinedObservationType.D1L, PredefinedObservationType.S1L),
    /** GPS L1C(D+P) MSM signal (code C1X, phase L1X, Doppler D1X, SNR S1X). */
    GPS_1X(PredefinedObservationType.C1X, PredefinedObservationType.L1X, PredefinedObservationType.D1X, PredefinedObservationType.S1X),

    /** Galileo E1 C/A MSM signal (code C1C, phase L1C, Doppler D1C, SNR S1C). */
    GAL_1C(PredefinedObservationType.C1C, PredefinedObservationType.L1C, PredefinedObservationType.D1C, PredefinedObservationType.S1C),
    /** Galileo E1 A PRS MSM signal (code C1A, phase L1A, Doppler D1A, SNR S1A). */
    GAL_1A(PredefinedObservationType.C1A, PredefinedObservationType.L1A, PredefinedObservationType.D1A, PredefinedObservationType.S1A),
    /** Galileo E1 B I/NAV MSM signal (code C1B, phase L1B, Doppler D1B, SNR S1B). */
    GAL_1B(PredefinedObservationType.C1B, PredefinedObservationType.L1B, PredefinedObservationType.D1B, PredefinedObservationType.S1B),
    /** Galileo E1 B+C MSM signal (code C1X, phase L1X, Doppler D1X, SNR S1X). */
    GAL_1X(PredefinedObservationType.C1X, PredefinedObservationType.L1X, PredefinedObservationType.D1X, PredefinedObservationType.S1X),
    /** Galileo E1 A+B+C MSM signal (code C1Z, phase L1Z, Doppler D1Z, SNR S1Z). */
    GAL_1Z(PredefinedObservationType.C1Z, PredefinedObservationType.L1Z, PredefinedObservationType.D1Z, PredefinedObservationType.S1Z),
    /** Galileo E6 C MSM signal (code C6C, phase L6C, Doppler D6C, SNR S6C). */
    GAL_6C(PredefinedObservationType.C6C, PredefinedObservationType.L6C, PredefinedObservationType.D6C, PredefinedObservationType.S6C),
    /** Galileo E6 A PRS MSM signal (code C6A, phase L6A, Doppler D6A, SNR S6A). */
    GAL_6A(PredefinedObservationType.C6A, PredefinedObservationType.L6A, PredefinedObservationType.D6A, PredefinedObservationType.S6A),
    /** Galileo E6 B MSM signal (code C6B, phase L6B, Doppler D6B, SNR S6B). */
    GAL_6B(PredefinedObservationType.C6B, PredefinedObservationType.L6B, PredefinedObservationType.D6B, PredefinedObservationType.S6B),
    /** Galileo E6 B+C MSM signal (code C6X, phase L6X, Doppler D6X, SNR S6X). */
    GAL_6X(PredefinedObservationType.C6X, PredefinedObservationType.L6X, PredefinedObservationType.D6X, PredefinedObservationType.S6X),
    /** Galileo E6 A+B+C MSM signal (code C6Z, phase L6Z, Doppler D6Z, SNR S6Z). */
    GAL_6Z(PredefinedObservationType.C6Z, PredefinedObservationType.L6Z, PredefinedObservationType.D6Z, PredefinedObservationType.S6Z),
    /** Galileo E5b I MSM signal (code C7I, phase L7I, Doppler D7I, SNR S7I). */
    GAL_7I(PredefinedObservationType.C7I, PredefinedObservationType.L7I, PredefinedObservationType.D7I, PredefinedObservationType.S7I),
    /** Galileo E5b Q MSM signal (code C7Q, phase L7Q, Doppler D7Q, SNR S7Q). */
    GAL_7Q(PredefinedObservationType.C7Q, PredefinedObservationType.L7Q, PredefinedObservationType.D7Q, PredefinedObservationType.S7Q),
    /** Galileo E5b I+Q MSM signal (code C7X, phase L7X, Doppler D7X, SNR S7X). */
    GAL_7X(PredefinedObservationType.C7X, PredefinedObservationType.L7X, PredefinedObservationType.D7X, PredefinedObservationType.S7X),
    /** Galileo E5(E5a+E5b) I MSM signal (code C8I, phase L8I, Doppler D8I, SNR S8I). */
    GAL_8I(PredefinedObservationType.C8I, PredefinedObservationType.L8I, PredefinedObservationType.D8I, PredefinedObservationType.S8I),
    /** Galileo E5(E5a+E5b) Q MSM signal (code C8Q, phase L8Q, Doppler D8Q, SNR S8Q). */
    GAL_8Q(PredefinedObservationType.C8Q, PredefinedObservationType.L8Q, PredefinedObservationType.D8Q, PredefinedObservationType.S8Q),
    /** Galileo E5(E5a+E5b) I+Q MSM signal (code C8X, phase L8X, Doppler D8X, SNR S8X). */
    GAL_8X(PredefinedObservationType.C8X, PredefinedObservationType.L8X, PredefinedObservationType.D8X, PredefinedObservationType.S8X),
    /** Galileo E5a I MSM signal (code C5I, phase L5I, Doppler D5I, SNR S5I). */
    GAL_5I(PredefinedObservationType.C5I, PredefinedObservationType.L5I, PredefinedObservationType.D5I, PredefinedObservationType.S5I),
    /** Galileo E5a Q MSM signal (code C5Q, phase L5Q, Doppler D5Q, SNR S5Q). */
    GAL_5Q(PredefinedObservationType.C5Q, PredefinedObservationType.L5Q, PredefinedObservationType.D5Q, PredefinedObservationType.S5Q),
    /** Galileo E5a I+Q MSM signal (code C5X, phase L5X, Doppler D5X, SNR S5X). */
    GAL_5X(PredefinedObservationType.C5X, PredefinedObservationType.L5X, PredefinedObservationType.D5X, PredefinedObservationType.S5X),

    /** GLONASS G1 C/A MSM signal (code C1C, phase L1C, Doppler D1C, SNR S1C). */
    GLO_1C(PredefinedObservationType.C1C, PredefinedObservationType.L1C, PredefinedObservationType.D1C, PredefinedObservationType.S1C),
    /** GLONASS G1 P MSM signal (code C1P, phase L1P, Doppler D1P, SNR S1P). */
    GLO_1P(PredefinedObservationType.C1P, PredefinedObservationType.L1P, PredefinedObservationType.D1P, PredefinedObservationType.S1P),
    /** GLONASS G2 C/A MSM signal (code C2C, phase L2C, Doppler D2C, SNR S2C). */
    GLO_2C(PredefinedObservationType.C2C, PredefinedObservationType.L2C, PredefinedObservationType.D2C, PredefinedObservationType.S2C),
    /** GLONASS G2 P MSM signal (code C2P, phase L2P, Doppler D2P, SNR S2P). */
    GLO_2P(PredefinedObservationType.C2P, PredefinedObservationType.L2P, PredefinedObservationType.D2P, PredefinedObservationType.S2P),

    /** SBAS L1 C/A MSM signal (code C1C, phase L1C, Doppler D1C, SNR S1C). */
    SBAS_1C(PredefinedObservationType.C1C, PredefinedObservationType.L1C, PredefinedObservationType.D1C, PredefinedObservationType.S1C),
    /** SBAS L5 I MSM signal (code C5I, phase L5I, Doppler D5I, SNR S5I). */
    SBAS_5I(PredefinedObservationType.C5I, PredefinedObservationType.L5I, PredefinedObservationType.D5I, PredefinedObservationType.S5I),
    /** SBAS L5 Q MSM signal (code C5Q, phase L5Q, Doppler D5Q, SNR S5Q). */
    SBAS_5Q(PredefinedObservationType.C5Q, PredefinedObservationType.L5Q, PredefinedObservationType.D5Q, PredefinedObservationType.S5Q),
    /** SBAS L5 I+Q MSM signal (code C5X, phase L5X, Doppler D5X, SNR S5X). */
    SBAS_5X(PredefinedObservationType.C5X, PredefinedObservationType.L5X, PredefinedObservationType.D5X, PredefinedObservationType.S5X),

    /** QZSS L1 C/A MSM signal (code C1C, phase L1C, Doppler D1C, SNR S1C). */
    QZSS_1C(PredefinedObservationType.C1C, PredefinedObservationType.L1C, PredefinedObservationType.D1C, PredefinedObservationType.S1C),
    /** QZSS L1 L1C(D) MSM signal (code C1S, phase L1S, Doppler D1S, SNR S1S). */
    QZSS_1S(PredefinedObservationType.C1S, PredefinedObservationType.L1S, PredefinedObservationType.D1S, PredefinedObservationType.S1S),
    /** QZSS L1 L1C(P) MSM signal (code C1L, phase L1L, Doppler D1L, SNR S1L). */
    QZSS_1L(PredefinedObservationType.C1L, PredefinedObservationType.L1L, PredefinedObservationType.D1L, PredefinedObservationType.S1L),
    /** QZSS L1 L1C(D+P) MSM signal (code C1X, phase L1X, Doppler D1X, SNR S1X). */
    QZSS_1X(PredefinedObservationType.C1X, PredefinedObservationType.L1X, PredefinedObservationType.D1X, PredefinedObservationType.S1X),
    /** QZSS L2 L2C(M) MSM signal (code C2S, phase L2S, Doppler D2S, SNR S2S). */
    QZSS_2S(PredefinedObservationType.C2S, PredefinedObservationType.L2S, PredefinedObservationType.D2S, PredefinedObservationType.S2S),
    /** QZSS L2 L2C(L) MSM signal (code C2L, phase L2L, Doppler D2L, SNR S2L). */
    QZSS_2L(PredefinedObservationType.C2L, PredefinedObservationType.L2L, PredefinedObservationType.D2L, PredefinedObservationType.S2L),
    /** QZSS L2 L2C(M+L) MSM signal (code C2X, phase L2X, Doppler D2X, SNR S2X). */
    QZSS_2X(PredefinedObservationType.C2X, PredefinedObservationType.L2X, PredefinedObservationType.D2X, PredefinedObservationType.S2X),
    /** QZSS L5 I MSM signal (code C5I, phase L5I, Doppler D5I, SNR S5I). */
    QZSS_5I(PredefinedObservationType.C5I, PredefinedObservationType.L5I, PredefinedObservationType.D5I, PredefinedObservationType.S5I),
    /** QZSS L5 Q MSM signal (code C5Q, phase L5Q, Doppler D5Q, SNR S5Q). */
    QZSS_5Q(PredefinedObservationType.C5Q, PredefinedObservationType.L5Q, PredefinedObservationType.D5Q, PredefinedObservationType.S5Q),
    /** QZSS L5 I+Q MSM signal (code C5X, phase L5X, Doppler D5X, SNR S5X). */
    QZSS_5X(PredefinedObservationType.C5X, PredefinedObservationType.L5X, PredefinedObservationType.D5X, PredefinedObservationType.S5X),
    /** QZSS L6 S MSM signal (code C6S, phase L6S, Doppler D6S, SNR S6S). */
    QZSS_6S(PredefinedObservationType.C6S, PredefinedObservationType.L6S, PredefinedObservationType.D6S, PredefinedObservationType.S6S),
    /** QZSS L6 L MSM signal (code C6L, phase L6L, Doppler D6L, SNR S6L). */
    QZSS_6L(PredefinedObservationType.C6L, PredefinedObservationType.L6L, PredefinedObservationType.D6L, PredefinedObservationType.S6L),
    /** QZSS L6 S+L MSM signal (code C6X, phase L6X, Doppler D6X, SNR S6X). */
    QZSS_6X(PredefinedObservationType.C6X, PredefinedObservationType.L6X, PredefinedObservationType.D6X, PredefinedObservationType.S6X),

    /** BeiDou B1 I MSM signal (code C2I, phase L2I, Doppler D2I, SNR S2I). */
    BDS_2I(PredefinedObservationType.C2I, PredefinedObservationType.L2I, PredefinedObservationType.D2I, PredefinedObservationType.S2I),
    /** BeiDou B1 Q MSM signal (code C2Q, phase L2Q, Doppler D2Q, SNR S2Q). */
    BDS_2Q(PredefinedObservationType.C2Q, PredefinedObservationType.L2Q, PredefinedObservationType.D2Q, PredefinedObservationType.S2Q),
    /** BeiDou B1 I+Q MSM signal (code C2X, phase L2X, Doppler D2X, SNR S2X). */
    BDS_2X(PredefinedObservationType.C2X, PredefinedObservationType.L2X, PredefinedObservationType.D2X, PredefinedObservationType.S2X),
    /** BeiDou B3 I MSM signal (code C6I, phase L6I, Doppler D6I, SNR S6I). */
    BDS_6I(PredefinedObservationType.C6I, PredefinedObservationType.L6I, PredefinedObservationType.D6I, PredefinedObservationType.S6I),
    /** BeiDou B3 Q MSM signal (code C6Q, phase L6Q, Doppler D6Q, SNR S6Q). */
    BDS_6Q(PredefinedObservationType.C6Q, PredefinedObservationType.L6Q, PredefinedObservationType.D6Q, PredefinedObservationType.S6Q),
    /** BeiDou B3 I+Q MSM signal (code C6X, phase L6X, Doppler D6X, SNR S6X). */
    BDS_6X(PredefinedObservationType.C6X, PredefinedObservationType.L6X, PredefinedObservationType.D6X, PredefinedObservationType.S6X),
    /** BeiDou B2 I MSM signal (code C7I, phase L7I, Doppler D7I, SNR S7I). */
    BDS_7I(PredefinedObservationType.C7I, PredefinedObservationType.L7I, PredefinedObservationType.D7I, PredefinedObservationType.S7I),
    /** BeiDou B2 Q MSM signal (code C7Q, phase L7Q, Doppler D7Q, SNR S7Q). */
    BDS_7Q(PredefinedObservationType.C7Q, PredefinedObservationType.L7Q, PredefinedObservationType.D7Q, PredefinedObservationType.S7Q),
    /** BeiDou B2 I+Q MSM signal (code C7X, phase L7X, Doppler D7X, SNR S7X). */
    BDS_7X(PredefinedObservationType.C7X, PredefinedObservationType.L7X, PredefinedObservationType.D7X, PredefinedObservationType.S7X);

    /** Observation type for code measurements. */
    private final PredefinedObservationType codeType;

    /** Observation type for carrier phase measurements. */
    private final PredefinedObservationType phaseType;

    /** Observation type for Doppler measurements. */
    private final PredefinedObservationType dopplerType;

    /** Observation type for signal strength measurements. */
    private final PredefinedObservationType signalStrengthType;

    /**
     * Simple constructor.
     * @param codeType observation type for code measurements
     * @param phaseType observation type for carrier phase measurements
     * @param dopplerType observation type for Doppler measurements
     * @param signalStrengthType observation type for signal strength measurements
     */
    RtcmMsmSignalId(final PredefinedObservationType codeType, final PredefinedObservationType phaseType,
            final PredefinedObservationType dopplerType, final PredefinedObservationType signalStrengthType) {
        this.codeType = codeType;
        this.phaseType = phaseType;
        this.dopplerType = dopplerType;
        this.signalStrengthType = signalStrengthType;
    }

    /**
     * Get the observation type for code measurements.
     * @return observation type for code measurements
     */
    public PredefinedObservationType getCodeType() {
        return codeType;
    }

    /**
     * Get the observation type for carrier phase measurements.
     * @return observation type for carrier phase measurements
     */
    public PredefinedObservationType getPhaseType() {
        return phaseType;
    }

    /**
     * Get the observation type for Doppler measurements.
     * @return observation type for Doppler measurements
     */
    public PredefinedObservationType getDopplerType() {
        return dopplerType;
    }

    /**
     * Get the observation type for signal strength measurements.
     * @return observation type for signal strength measurements
     */
    public PredefinedObservationType getSignalStrengthType() {
        return signalStrengthType;
    }
}
