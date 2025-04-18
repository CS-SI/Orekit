/* Copyright 2002-2025 CS GROUP
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
package org.orekit.gnss;

import java.util.HashMap;
import java.util.Map;

/** Enumerate for all the Observation Types for Rinex 2 and 3.
 * For Rinex 2, there is an two-character enumerate composed of the Observation
 * Code (C,P,L,D,S) and the Frequency code (1,2,5,6,7,8).
 * For Rinex 3 there is a three-character enumerate composed of the Observation
 * Code (C,L,D,S), the frequency code (1,2,5,6,7,8) and a final attribute depending
 * on the tracking mode or channel.
 *
 */
public enum PredefinedObservationType implements ObservationType {

    /** Pseudorange GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    C1(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01),

    /** Pseudorange GPS L2 / GLONASS G2 / Beidou B02 for Rinex2. */
    C2(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B02),

    /** Pseudorange GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    C5(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05),

    /** Pseudorange Galileo E6 / Beidou B03 for Rinex2. */
    C6(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.E06, PredefinedGnssSignal.B03),

    /** Pseudorange Galileo E5b / Beidou B02 for Rinex2. */
    C7(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Pseudorange Galileo E5a+b for Rinex2. */
    C8(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.E08),

    /** Pseudorange GPS L1 / GLONASS G1 for Rinex2. */
    P1(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01),

    /** Pseudorange GPS L2 / GLONASS G2 for Rinex2. */
    P2(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Carrier-phase GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    L1(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01),

    /** Carrier-phase GPS L2 / GLONASS G2 / Beidou B02 for Rinex2. */
    L2(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B02),

    /** Carrier-phase GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    L5(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05),

    /** Carrier-phase Galileo E6 / Beidou B03 for Rinex2. */
    L6(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.E06, PredefinedGnssSignal.C07),

    /** Carrier-phase Galileo E5b / Beidou B02 for Rinex2. */
    L7(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Carrier-phase Galileo E5a+b for Rinex2. */
    L8(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.E08),

    /** Carrier-phase GPS L1 C/A / GLONASS G1 C/A for Rinex2. */
    LA(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01),

    /** Carrier-phase GPS L1C for Rinex2. */
    LB(MeasurementType.CARRIER_PHASE, SignalCode.L, PredefinedGnssSignal.G01),

    /** Carrier-phase GPS L2C for Rinex2. */
    LC(MeasurementType.CARRIER_PHASE, SignalCode.L, PredefinedGnssSignal.G02),

    /** Carrier-phase GLONASS G2 for Rinex2. */
    LD(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.R02),

    /** Doppler GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    D1(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01),

    /** Doppler GPS L2 / GLONASS G2 / Beidou BO2 for Rinex2. */
    D2(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B02),

    /** Doppler GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    D5(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05),

    /** Doppler Galileo E6 / Beidou B03 for Rinex2. */
    D6(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.E06, PredefinedGnssSignal.C07),

    /** Doppler Galileo E5b / Beidou B02 for Rinex2. */
    D7(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Doppler Galileo E5a+b for Rinex2. */
    D8(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.E08),

    /** Doppler GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    S1(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01),

    /** Signal Strength GPS L2 / GLONASS G2 / Beidou B02 for Rinex2. */
    S2(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B02),

    /** Signal Strength GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    S5(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05),

    /** Signal Strength Galileo E6 / Beidou B03 for Rinex2. */
    S6(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.E06, PredefinedGnssSignal.C07),

    /** Signal Strength Galileo E5b / Beidou B02 for Rinex2. */
    S7(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Signal Strength Galileo E5a+b for Rinex2. */
    S8(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.E08),

    /** Pseudorange Galileo E1 A / Beidou B1A for Rinex3. */
    C1A(MeasurementType.PSEUDO_RANGE, SignalCode.A, PredefinedGnssSignal.E01, PredefinedGnssSignal.B1A),

    /** Pseudorange Galileo E1 I/NAV OS/CS/SoL / QZSS geo signal for Rinex3. */
    C1B(MeasurementType.PSEUDO_RANGE, SignalCode.B, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01),

    /** Pseudorange GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    C1C(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01, PredefinedGnssSignal.J01),

    /** Pseudorange Beidou B1 Data / NavIC L1 Data. */
    C1D(MeasurementType.PSEUDO_RANGE, SignalCode.D, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Pseudorange QZSS L1 C/B for Rinex4. */
    C1E(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.J01),

    /** Pseudorange Beidou B1 I for Rinex3.02. */
    C1I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.B01),

    /** Pseudorange GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3, Beidou B1A for Rinex3.03. */
    C1L(MeasurementType.PSEUDO_RANGE, SignalCode.L, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Pseudorange GPS L1 M for Rinex3. */
    C1M(MeasurementType.PSEUDO_RANGE, SignalCode.M, PredefinedGnssSignal.G01),

    /** Pseudorange GPS L1 P(AS off) / GLONASS G1 P / Beidou C1 Pilot / NavIC L1 Pilot. */
    C1P(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Pseudorange Beidou B1 Q for Rinex3.02. */
    C1Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.B01),

    /** Pseudorange GPS L1 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    C1R(MeasurementType.PSEUDO_RANGE, SignalCode.M, PredefinedGnssSignal.G01),

    /** Pseudorange GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3, Beidou B1A for Rinex3.03. */
    C1S(MeasurementType.PSEUDO_RANGE, SignalCode.S, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Pseudorange GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    C1W(MeasurementType.PSEUDO_RANGE, SignalCode.W, PredefinedGnssSignal.G01),

    /** Pseudorange GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) / Beidou B1 Data+Pilot / NavIC L1 Data+Pilot. */
    C1X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.G01, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Pseudorange GPS L1 Y for Rinex3. */
    C1Y(MeasurementType.PSEUDO_RANGE, SignalCode.Y, PredefinedGnssSignal.G01),

    /** Pseudorange Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3, Beidou B1A for Rinex3.03. */
    C1Z(MeasurementType.PSEUDO_RANGE, SignalCode.Z, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Pseudorange GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    C2C(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Pseudorange GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    C2D(MeasurementType.PSEUDO_RANGE, SignalCode.D, PredefinedGnssSignal.G02),

    /** Pseudorange Beidou B1 I for Rinex3.03. */
    C2I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.B01),

    /** Pseudorange GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    C2L(MeasurementType.PSEUDO_RANGE, SignalCode.L, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Pseudorange GPS L2 M for Rinex3. */
    C2M(MeasurementType.PSEUDO_RANGE, SignalCode.M, PredefinedGnssSignal.G02),

    /** Pseudorange GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    C2P(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Pseudorange Beidou B1 Q for Rinex3.03. */
    C2Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.B01),

    /** Pseudorange GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    C2R(MeasurementType.PSEUDO_RANGE, SignalCode.M, PredefinedGnssSignal.G02),

    /** Pseudorange GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    C2S(MeasurementType.PSEUDO_RANGE, SignalCode.S, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Pseudorange GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    C2W(MeasurementType.PSEUDO_RANGE, SignalCode.W, PredefinedGnssSignal.G02),

    /** Pseudorange GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3, Beidou B1 I+Q for Rinex3.03. */
    C2X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02, PredefinedGnssSignal.B01),

    /** Pseudorange GPS L2 Y for Rinex3. */
    C2Y(MeasurementType.PSEUDO_RANGE, SignalCode.Y, PredefinedGnssSignal.G02),

    /** Pseudorange GLONASS G3 I for Rinex3. */
    C3I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.R03),

    /** Pseudorange GLONASS G3 Q for Rinex3. */
    C3Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.R03),

    /** Pseudorange GLONASS G3 I+Q for Rinex3. */
    C3X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.R03),

    /** Pseudorange GLONASS G1a L1OCd for Rinex3. */
    C4A(MeasurementType.PSEUDO_RANGE, SignalCode.A, PredefinedGnssSignal.R04),

    /** Pseudorange GLONASS G1a L1OCp for Rinex3. */
    C4B(MeasurementType.PSEUDO_RANGE, SignalCode.B, PredefinedGnssSignal.R04),

    /** Pseudorange GLONASS G1a L1OCd+L1OCd for Rinex3. */
    C4X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.R04),

    /** Pseudorange NavIC L5 A for Rinex3. */
    C5A(MeasurementType.PSEUDO_RANGE, SignalCode.A, PredefinedGnssSignal.I05),

    /** Pseudorange NavIC L5 B for Rinex3. */
    C5B(MeasurementType.PSEUDO_RANGE, SignalCode.B, PredefinedGnssSignal.I05),

    /** Pseudorange NavIC L5 C for Rinex3. */
    C5C(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.I05),

    /** Pseudorange QZSS L5 D / Beidou B2a Data for Rinex3. */
    C5D(MeasurementType.PSEUDO_RANGE, SignalCode.D, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Pseudorange GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    C5I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Pseudorange QZSS L5 P / Beidou B2a Pilot for Rinex3. */
    C5P(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Pseudorange GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    C5Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Pseudorange GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q / NavIC L5 B+C / Beidou B2a Data+Pilot for Rinex3. */
    C5X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05, PredefinedGnssSignal.I05, PredefinedGnssSignal.B2A),

    /** Pseudorange QZSS L5 D+P for Rinex3. */
    C5Z(MeasurementType.PSEUDO_RANGE, SignalCode.Z, PredefinedGnssSignal.J05),

    /** Pseudorange Galileo E6 A PRS / GLONASS G2a L2CSI / Beidou B3A for Rinex3. */
    C6A(MeasurementType.PSEUDO_RANGE, SignalCode.A, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06, PredefinedGnssSignal.B03),

    /** Pseudorange Galileo E6 B C/NAV CS / GLONASS G2a L2OCp for Rinex3. */
    C6B(MeasurementType.PSEUDO_RANGE, SignalCode.B, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06),

    /** Pseudorange Galileo E6 C no data for Rinex3. */
    C6C(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.E06),

    /** Pseudorange Beidou B3A for Rinex3. */
    C6D(MeasurementType.PSEUDO_RANGE, SignalCode.D, PredefinedGnssSignal.B3A),

    /** Pseudorange QZSS L6E for Rinex3. */
    C6E(MeasurementType.PSEUDO_RANGE, SignalCode.E, PredefinedGnssSignal.J06),

    /** Pseudorange Beidou B3 I for Rinex3. */
    C6I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.B03),

    /** Pseudorange QZSS LEX(6) L for Rinex3. */
    C6L(MeasurementType.PSEUDO_RANGE, SignalCode.L, PredefinedGnssSignal.J06),

    /** Pseudorange Beidou B3A for Rinex3. */
    C6P(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.B3A),

    /** Pseudorange Beidou B3 Q for Rinex3. */
    C6Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.B03),

    /** Pseudorange QZSS LEX(6) S for Rinex3. */
    C6S(MeasurementType.PSEUDO_RANGE, SignalCode.S, PredefinedGnssSignal.J06),

    /** Pseudorange Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q / GLONASS G2a L2CSI+L2OCp for Rinex3. */
    C6X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B03, PredefinedGnssSignal.R06),

    /** Pseudorange Galileo E6 A+B+C / QZSS L6(D+E) / Beidou B3A for Rinex3. */
    C6Z(MeasurementType.PSEUDO_RANGE, SignalCode.Z, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B3A),

    /** Pseudorange Beidou B2b Data for Rinex3. */
    C7D(MeasurementType.PSEUDO_RANGE, SignalCode.D, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Pseudorange Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    C7I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Pseudorange Beidou B2b Pilot for Rinex3. */
    C7P(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Pseudorange Galileo Q no data / Beidou B2 Q for Rinex3. */
    C7Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Pseudorange Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    C7X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Pseudorange Beidou B2b Data+Pilot for Rinex3. */
    C7Z(MeasurementType.PSEUDO_RANGE, SignalCode.Z, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Pseudorange Beidou B2(B2a+B2b) Data for Rinex3. */
    C8D(MeasurementType.PSEUDO_RANGE, SignalCode.D, PredefinedGnssSignal.B08),

    /** Pseudorange Galileo E5(E5a+E5b) I for Rinex3. */
    C8I(MeasurementType.PSEUDO_RANGE, SignalCode.I, PredefinedGnssSignal.E08),

    /** Pseudorange Beidou B2(B2a+B2b) Pilot for Rinex3. */
    C8P(MeasurementType.PSEUDO_RANGE, SignalCode.P, PredefinedGnssSignal.B08),

    /** Pseudorange Galileo E5(E5a+E5b) Q for Rinex3. */
    C8Q(MeasurementType.PSEUDO_RANGE, SignalCode.Q, PredefinedGnssSignal.E08),

    /** Pseudorange Galileo E5(E5a+E5b) I+Q / Beidou B2(B2a+B2b) Data+Pilot for Rinex3. */
    C8X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.E08, PredefinedGnssSignal.B08),

    /** Pseudorange NavIC S A for Rinex3. */
    C9A(MeasurementType.PSEUDO_RANGE, SignalCode.A, PredefinedGnssSignal.I09),

    /** Pseudorange NavIC S B for Rinex3. */
    C9B(MeasurementType.PSEUDO_RANGE, SignalCode.B, PredefinedGnssSignal.I09),

    /** Pseudorange NavIC S C for Rinex3. */
    C9C(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.I09),

    /** Pseudorange NavIC S B+C for Rinex3. */
    C9X(MeasurementType.PSEUDO_RANGE, SignalCode.X, PredefinedGnssSignal.I09),

    /** Pseudorange for Rinex3. */
    C0(MeasurementType.PSEUDO_RANGE, SignalCode.CODELESS),

    /** Pseudorange GPS L1 C/A / GLONASS G1 C/A for Rinex2. */
    CA(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01),

    /** Pseudorange GPS L1C for Rinex2. */
    CB(MeasurementType.PSEUDO_RANGE, SignalCode.L, PredefinedGnssSignal.G01),

    /** Pseudorange GPS L2C for Rinex2. */
    CC(MeasurementType.PSEUDO_RANGE, SignalCode.L, PredefinedGnssSignal.G02),

    /** Pseudorange GLONASS G2 for Rinex2. */
    CD(MeasurementType.PSEUDO_RANGE, SignalCode.C, PredefinedGnssSignal.R02),

    /** Doppler Galileo E1 A / Beidou B1 B1A for Rinex3. */
    D1A(MeasurementType.DOPPLER, SignalCode.A, PredefinedGnssSignal.E01, PredefinedGnssSignal.B1A),

    /** Doppler Galileo E1 I/NAV OS/CS/SoL / QZSS geo signal for Rinex3. */
    D1B(MeasurementType.DOPPLER, SignalCode.B, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01),

    /** Doppler GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    D1C(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01, PredefinedGnssSignal.J01),

    /** Doppler Beidou B1 Data / NavIC L1 Data. */
    D1D(MeasurementType.DOPPLER, SignalCode.D, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Doppler QZSS L1 C/B for Rinex4. */
    D1E(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.J01),

    /** Doppler Beidou B1 I for Rinex3. */
    D1I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.B01),

    /** Doppler GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3, Beidou B1A for Rinex3.03. */
    D1L(MeasurementType.DOPPLER, SignalCode.L, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Doppler GPS L2 M for Rinex3. */
    D1M(MeasurementType.DOPPLER, SignalCode.M, PredefinedGnssSignal.G02),

    /** Doppler GPS L1 codeless / Beidou B1 codeless for Rinex3. */
    D1N(MeasurementType.DOPPLER, SignalCode.CODELESS, PredefinedGnssSignal.G01, PredefinedGnssSignal.B1A),

    /** Doppler GPS L2 P(AS off) / GLONASS G2 P / Beidou B1 Pilot / NavIC L1 Pilot. */
    D1P(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Doppler GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    D1R(MeasurementType.DOPPLER, SignalCode.M, PredefinedGnssSignal.G02),

    /** Doppler GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3, Beidou B1A for Rinex3.03. */
    D1S(MeasurementType.DOPPLER, SignalCode.S, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Doppler GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    D1W(MeasurementType.DOPPLER, SignalCode.W, PredefinedGnssSignal.G01),

    /** Doppler GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) / Beidou B1 Data+Pilot / NavIC L1 Data+Pilot. */
    D1X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.G01, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Doppler GPS L1 Y for Rinex3. */
    D1Y(MeasurementType.DOPPLER, SignalCode.Y, PredefinedGnssSignal.G01),

    /** Doppler Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3, Beidou B1A for Rinex3.03. */
    D1Z(MeasurementType.DOPPLER, SignalCode.Z, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Doppler GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    D2C(MeasurementType.DOPPLER, SignalCode.C,  PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Doppler GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    D2D(MeasurementType.DOPPLER, SignalCode.D, PredefinedGnssSignal.G02),

    /** Doppler Beidou B1 I for Rinex3.03. */
    D2I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.B01),

    /** Doppler GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    D2L(MeasurementType.DOPPLER, SignalCode.L, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Doppler GPS L2 M for Rinex3. */
    D2M(MeasurementType.DOPPLER, SignalCode.M, PredefinedGnssSignal.G02),

    /** Doppler GPS L2 codeless for Rinex3. */
    D2N(MeasurementType.DOPPLER, SignalCode.CODELESS, PredefinedGnssSignal.G02),

    /** Doppler GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    D2P(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Doppler Beidou B1 Q for Rinex3.03. */
    D2Q(MeasurementType.DOPPLER, SignalCode.Q, PredefinedGnssSignal.B01),

    /** Doppler GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    D2R(MeasurementType.DOPPLER, SignalCode.M, PredefinedGnssSignal.G02),

    /** Doppler GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    D2S(MeasurementType.DOPPLER, SignalCode.S, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Doppler GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    D2W(MeasurementType.DOPPLER, SignalCode.W, PredefinedGnssSignal.G02),

    /** Doppler GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3, Beidou B1 I+Q for Rinex3.03. */
    D2X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02, PredefinedGnssSignal.B01),

    /** Doppler GPS L2 Y for Rinex3. */
    D2Y(MeasurementType.DOPPLER, SignalCode.Y, PredefinedGnssSignal.G02),

    /** Doppler GLONASS G3 I for Rinex3. */
    D3I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.R03),

    /** Doppler GLONASS G3 Q for Rinex3. */
    D3Q(MeasurementType.DOPPLER, SignalCode.Q, PredefinedGnssSignal.R03),

    /** Doppler GLONASS G3 I+Q for Rinex3. */
    D3X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.R03),

    /** Doppler GLONASS G1a L1OCd for Rinex3. */
    D4A(MeasurementType.DOPPLER, SignalCode.A, PredefinedGnssSignal.R04),

    /** Doppler GLONASS G1a L1OCp for Rinex3. */
    D4B(MeasurementType.DOPPLER, SignalCode.B, PredefinedGnssSignal.R04),

    /** Doppler GLONASS G1a L1OCd+L1OCd for Rinex3. */
    D4X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.R04),

    /** Doppler NavIC L5 A for Rinex3. */
    D5A(MeasurementType.DOPPLER, SignalCode.A, PredefinedGnssSignal.I05),

    /** Doppler NavIC L5 B for Rinex3. */
    D5B(MeasurementType.DOPPLER, SignalCode.B, PredefinedGnssSignal.I05),

    /** Doppler NavIC L5 C for Rinex3. */
    D5C(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.I05),

    /** Doppler QZSS L5 D / Beidou B2a Data for Rinex3. */
    D5D(MeasurementType.DOPPLER, SignalCode.D, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Doppler GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    D5I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Doppler QZSS L5 P / Beidou B2a Pilot for Rinex3. */
    D5P(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Doppler GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    D5Q(MeasurementType.DOPPLER, SignalCode.Q, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Doppler GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q / NavIC L5 B+C / Beidou B2a Data+Pilot for Rinex3. */
    D5X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05, PredefinedGnssSignal.I05, PredefinedGnssSignal.B2A),

    /** Doppler QZSS L5 D+P for Rinex3. */
    D5Z(MeasurementType.DOPPLER, SignalCode.Z, PredefinedGnssSignal.J05),

    /** Doppler Galileo E6 A PRS / GLONASS L2CSI / Beidou B3A for Rinex3. */
    D6A(MeasurementType.DOPPLER, SignalCode.A, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06, PredefinedGnssSignal.B03),

    /** Doppler Galileo E6 B C/NAV CS / GLONASS L2OCp for Rinex3. */
    D6B(MeasurementType.DOPPLER, SignalCode.B, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06),

    /** Doppler Galileo E6 C no data for Rinex3. */
    D6C(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.E06),

    /** Doppler Beidou B3A for Rinex3. */
    D6D(MeasurementType.DOPPLER, SignalCode.D, PredefinedGnssSignal.B3A),

    /** Doppler QZSS L6E for Rinex3. */
    D6E(MeasurementType.DOPPLER, SignalCode.E, PredefinedGnssSignal.J06),

    /** Doppler Beidou B3 I for Rinex3. */
    D6I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.B03),

    /** Doppler QZSS LEX(6) L for Rinex3. */
    D6L(MeasurementType.DOPPLER, SignalCode.L, PredefinedGnssSignal.J06),

    /** Doppler Beidou B3A for Rinex3. */
    D6P(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.B3A),

    /** Doppler Beidou B3 Q for Rinex3. */
    D6Q(MeasurementType.DOPPLER, SignalCode.Q, PredefinedGnssSignal.B03),

    /** Doppler QZSS LEX(6) S for Rinex3. */
    D6S(MeasurementType.DOPPLER, SignalCode.S, PredefinedGnssSignal.J06),

    /** Doppler Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q / GLONASS G2a L2CSI+L2OCp for Rinex3. */
    D6X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B03, PredefinedGnssSignal.R06),

    /** Doppler Galileo E6 A+B+C / QZSS L6(D+E) / Beidou B3A for Rinex3. */
    D6Z(MeasurementType.DOPPLER, SignalCode.Z, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B3A),

    /** Doppler Beidou B2b Data for Rinex3. */
    D7D(MeasurementType.DOPPLER, SignalCode.D, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Doppler Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    D7I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Doppler Beidou B2b Pilot for Rinex3. */
    D7P(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Doppler Galileo Q no data / Beidou B2 Q for Rinex3. */
    D7Q(MeasurementType.DOPPLER, SignalCode.Q, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Doppler Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    D7X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Doppler Beidou B2b Data+Pilot for Rinex3. */
    D7Z(MeasurementType.DOPPLER, SignalCode.Z, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Doppler Beidou B2(B2a+B2b) Data for Rinex3. */
    D8D(MeasurementType.DOPPLER, SignalCode.D, PredefinedGnssSignal.B08),

    /** Doppler Galileo E5(E5a+E5b) I for Rinex3. */
    D8I(MeasurementType.DOPPLER, SignalCode.I, PredefinedGnssSignal.E08),

    /** Doppler Beidou B2(B2a+B2b) Pilot for Rinex3. */
    D8P(MeasurementType.DOPPLER, SignalCode.P, PredefinedGnssSignal.B08),

    /** Doppler Galileo E5(E5a+E5b) Q for Rinex3. */
    D8Q(MeasurementType.DOPPLER, SignalCode.Q, PredefinedGnssSignal.E08),

    /** Doppler Galileo E5(E5a+E5b) I+Q / B2(B2a+B2b) Data+Pilot for Rinex3. */
    D8X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.E08, PredefinedGnssSignal.B08),

    /** Doppler NavIC S A for Rinex3. */
    D9A(MeasurementType.DOPPLER, SignalCode.A, PredefinedGnssSignal.I09),

    /** Doppler NavIC S B for Rinex3. */
    D9B(MeasurementType.DOPPLER, SignalCode.B, PredefinedGnssSignal.I09),

    /** Doppler NavIC S C for Rinex3. */
    D9C(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.I09),

    /** Doppler NavIC S B+C for Rinex3. */
    D9X(MeasurementType.DOPPLER, SignalCode.X, PredefinedGnssSignal.I09),

    /** Doppler for Rinex3. */
    D0(MeasurementType.DOPPLER, SignalCode.CODELESS),

    /** Doppler GPS L1 C/A / GLONASS G1 C/A for Rinex2. */
    DA(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01),

    /** Doppler GPS L1C for Rinex2. */
    DB(MeasurementType.DOPPLER, SignalCode.L, PredefinedGnssSignal.G01),

    /** Doppler GPS L2C for Rinex2. */
    DC(MeasurementType.DOPPLER, SignalCode.L, PredefinedGnssSignal.G02),

    /** Doppler GLONASS G2 for Rinex2. */
    DD(MeasurementType.DOPPLER, SignalCode.C, PredefinedGnssSignal.R02),

    /** Carrier-phase Galileo E1 A / Beidou B1 B1A for Rinex3. */
    L1A(MeasurementType.CARRIER_PHASE, SignalCode.A, PredefinedGnssSignal.E01, PredefinedGnssSignal.B1A),

    /** Carrier-phase Galileo E1 I/NAV OS/CS/SoL / QZSS geo signal for Rinex3. */
    L1B(MeasurementType.CARRIER_PHASE, SignalCode.B, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01),

    /** Carrier-phase GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    L1C(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01, PredefinedGnssSignal.J01),

    /** Carrier-phase Beidou B1 Data / NavIC L1 Data. */
    L1D(MeasurementType.CARRIER_PHASE, SignalCode.D, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Carrier-phase QZSS L1 C/B for Rinex4. */
    L1E(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.J01),

    /** Carrier-phase Beidou B1 I for Rinex3. */
    L1I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.B01),

    /** Carrier-phase GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3, Beidou B1A for Rinex3.03. */
    L1L(MeasurementType.CARRIER_PHASE, SignalCode.L, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Carrier-phase GPS L2 M for Rinex3. */
    L1M(MeasurementType.CARRIER_PHASE, SignalCode.M, PredefinedGnssSignal.G02),

    /** Carrier-phase GPS L1 codeless for Rinex3. */
    L1N(MeasurementType.CARRIER_PHASE, SignalCode.CODELESS, PredefinedGnssSignal.G01),

    /** Carrier-phase GPS L2 P(AS off) / GLONASS G2 P / Beidou B1 Pilot / NavIC L1 Pilot. */
    L1P(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Carrier-phase GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    L1R(MeasurementType.CARRIER_PHASE, SignalCode.M, PredefinedGnssSignal.G02),

    /** Carrier-phase GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3, Beidou B1A for Rinex3.03. */
    L1S(MeasurementType.CARRIER_PHASE, SignalCode.S, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Carrier-phase GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    L1W(MeasurementType.CARRIER_PHASE, SignalCode.W, PredefinedGnssSignal.G01),

    /** Carrier-phase GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) / Beidou B1 Data+Pilot / NavIC L1 Data+Pilot. */
    L1X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.G01, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Carrier-phase GPS L1 Y for Rinex3. */
    L1Y(MeasurementType.CARRIER_PHASE, SignalCode.Y, PredefinedGnssSignal.G01),

    /** Carrier-phase Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3, Beidou B1A for Rinex3.03. */
    L1Z(MeasurementType.CARRIER_PHASE, SignalCode.Z, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Carrier-phase GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    L2C(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Carrier-phase GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    L2D(MeasurementType.CARRIER_PHASE, SignalCode.D, PredefinedGnssSignal.G02),

    /** Carrier-phase Beidou B1 I for Rinex3.03. */
    L2I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.B01),

    /** Carrier-phase GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    L2L(MeasurementType.CARRIER_PHASE, SignalCode.L, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Carrier-phase GPS L2 M for Rinex3. */
    L2M(MeasurementType.CARRIER_PHASE, SignalCode.M, PredefinedGnssSignal.G02),

    /** Carrier-phase GPS L2 codeless. */
    L2N(MeasurementType.CARRIER_PHASE, SignalCode.CODELESS, PredefinedGnssSignal.G02),

    /** Carrier-phase GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    L2P(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Carrier-phase Beidou B1 Q for Rinex3.03. */
    L2Q(MeasurementType.CARRIER_PHASE, SignalCode.Q, PredefinedGnssSignal.B01),

    /** Carrier-phase GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    L2R(MeasurementType.CARRIER_PHASE, SignalCode.M, PredefinedGnssSignal.G02),

    /** Carrier-phase GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    L2S(MeasurementType.CARRIER_PHASE, SignalCode.S, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Carrier-phase GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    L2W(MeasurementType.CARRIER_PHASE, SignalCode.W, PredefinedGnssSignal.G02),

    /** Carrier-phase GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3, Beidou B1 I+Q for Rinex3.03. */
    L2X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02, PredefinedGnssSignal.B01),

    /** Carrier-phase GPS L2 Y for Rinex3. */
    L2Y(MeasurementType.CARRIER_PHASE, SignalCode.Y, PredefinedGnssSignal.G02),

    /** Carrier-phase GLONASS G3 I for Rinex3. */
    L3I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.R03),

    /** Carrier-phase GLONASS G3 Q for Rinex3. */
    L3Q(MeasurementType.CARRIER_PHASE, SignalCode.Q, PredefinedGnssSignal.R03),

    /** Carrier-phase GLONASS G3 I+Q for Rinex3. */
    L3X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.R03),

    /** Carrier-phase GLONASS G1a L1OCd for Rinex3. */
    L4A(MeasurementType.CARRIER_PHASE, SignalCode.A, PredefinedGnssSignal.R04),

    /** Carrier-phase GLONASS G1a L1OCp for Rinex3. */
    L4B(MeasurementType.CARRIER_PHASE, SignalCode.B, PredefinedGnssSignal.R04),

    /** Carrier-phase GLONASS G1a L1OCd+L1OCd for Rinex3. */
    L4X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.R04),

    /** Carrier-phase NavIC L5 A for Rinex3. */
    L5A(MeasurementType.CARRIER_PHASE, SignalCode.A, PredefinedGnssSignal.I05),

    /** Carrier-phase NavIC L5 B for Rinex3. */
    L5B(MeasurementType.CARRIER_PHASE, SignalCode.B, PredefinedGnssSignal.I05),

    /** Carrier-phase NavIC L5 C for Rinex3. */
    L5C(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.I05),

    /** Carrier-phase QZSS L5 / Beidou B2a Data D for Rinex3. */
    L5D(MeasurementType.CARRIER_PHASE, SignalCode.D, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Carrier-phase GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    L5I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Carrier-phase QZSS L5 P / Beidou B2a Pilot for Rinex3. */
    L5P(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Carrier-phase GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    L5Q(MeasurementType.CARRIER_PHASE, SignalCode.Q, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Carrier-phase GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q / NavIC L5 B+C / Beidou B2a Data+Pilot for Rinex3. */
    L5X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05, PredefinedGnssSignal.I05, PredefinedGnssSignal.B2A),

    /** Carrier-phase QZSS L5 D+P for Rinex3. */
    L5Z(MeasurementType.CARRIER_PHASE, SignalCode.Z, PredefinedGnssSignal.J05),

    /** Carrier-phase Galileo E6 A PRS / GLONASS G2a L2CSI / Beidou B3A for Rinex3. */
    L6A(MeasurementType.CARRIER_PHASE, SignalCode.A, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06, PredefinedGnssSignal.B03),

    /** Carrier-phase Galileo E6 B C/NAV CS / GLONASS G2a L2OCp for Rinex3. */
    L6B(MeasurementType.CARRIER_PHASE, SignalCode.B, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06),

    /** Carrier-phase Galileo E6 C no data for Rinex3. */
    L6C(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.E06),

    /** Carrier-phase Beidou B3A for Rinex3. */
    L6D(MeasurementType.CARRIER_PHASE, SignalCode.D, PredefinedGnssSignal.B3A),

    /** Carrier-phase QZSS L6E for Rinex3. */
    L6E(MeasurementType.CARRIER_PHASE, SignalCode.E, PredefinedGnssSignal.J06),

    /** Carrier-phase Beidou B3 I for Rinex3. */
    L6I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.B03),

    /** Carrier-phase QZSS LEX(6) L for Rinex3. */
    L6L(MeasurementType.CARRIER_PHASE, SignalCode.L, PredefinedGnssSignal.J06),

    /** Carrier-phase Beidou B3A for Rinex3. */
    L6P(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.B3A),

    /** Carrier-phase Beidou B3 Q for Rinex3. */
    L6Q(MeasurementType.CARRIER_PHASE, SignalCode.Q, PredefinedGnssSignal.B03),

    /** Carrier-phase QZSS LEX(6) S for Rinex3. */
    L6S(MeasurementType.CARRIER_PHASE, SignalCode.S, PredefinedGnssSignal.J06),

    /** Carrier-phase Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q / GLONASS G2a L2CSI+L2OCp for Rinex3. */
    L6X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B03, PredefinedGnssSignal.R06),

    /** Carrier-phase Galileo E6 A+B+C / QZSS L6(D+E) / Beidou B3A for Rinex3. */
    L6Z(MeasurementType.CARRIER_PHASE, SignalCode.Z, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B3A),

    /** Carrier-phase Beidou B2b Data for Rinex3. */
    L7D(MeasurementType.CARRIER_PHASE, SignalCode.D, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Carrier-phase Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    L7I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Carrier-phase Beidou B2b Pilot for Rinex3. */
    L7P(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Carrier-phase Galileo Q no data / Beidou B2 Q for Rinex3. */
    L7Q(MeasurementType.CARRIER_PHASE, SignalCode.Q, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Carrier-phase Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    L7X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Carrier-phase Beidou B2b Data+Pilot for Rinex3. */
    L7Z(MeasurementType.CARRIER_PHASE, SignalCode.Z, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Carrier-phase Beidou B2(B2a+B2b) Data for Rinex3. */
    L8D(MeasurementType.CARRIER_PHASE, SignalCode.D, PredefinedGnssSignal.B08),

    /** Carrier-phase Galileo E5(E5a+E5b) I for Rinex3. */
    L8I(MeasurementType.CARRIER_PHASE, SignalCode.I, PredefinedGnssSignal.E08),

    /** Carrier-phase Beidou B2(B2a+B2b) Pilot for Rinex3. */
    L8P(MeasurementType.CARRIER_PHASE, SignalCode.P, PredefinedGnssSignal.B08),

    /** Carrier-phase Galileo E5(E5a+E5b) Q for Rinex3. */
    L8Q(MeasurementType.CARRIER_PHASE, SignalCode.Q, PredefinedGnssSignal.E08),

    /** Carrier-phase Galileo E5(E5a+E5b) I+Q / Beidou B2(B2a+B2b) Data+Pilot for Rinex3. */
    L8X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.E08, PredefinedGnssSignal.B08),

    /** Carrier-phase NavIC S A for Rinex3. */
    L9A(MeasurementType.CARRIER_PHASE, SignalCode.A, PredefinedGnssSignal.I09),

    /** Carrier-phase NavIC S B for Rinex3. */
    L9B(MeasurementType.CARRIER_PHASE, SignalCode.B, PredefinedGnssSignal.I09),

    /** Carrier-phase NavIC S C for Rinex3. */
    L9C(MeasurementType.CARRIER_PHASE, SignalCode.C, PredefinedGnssSignal.I09),

    /** Carrier-phase NavIC S B+C for Rinex3. */
    L9X(MeasurementType.CARRIER_PHASE, SignalCode.X, PredefinedGnssSignal.I09),

    /** Carrier-phase for Rinex3. */
    L0(MeasurementType.CARRIER_PHASE, SignalCode.CODELESS),

    /** Signal-strength Galileo E1 A / Beidou B1 B1A for Rinex3. */
    S1A(MeasurementType.SIGNAL_STRENGTH, SignalCode.A, PredefinedGnssSignal.E01, PredefinedGnssSignal.B1A),

    /** Signal-strength Galileo E1 I/NAV OS/CS/SoL / QZSS geo signal for Rinex3. */
    S1B(MeasurementType.SIGNAL_STRENGTH, SignalCode.B, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01),

    /** Signal-strength GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    S1C(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01, PredefinedGnssSignal.E01, PredefinedGnssSignal.S01, PredefinedGnssSignal.J01),

    /** Signal-strength Beidou B1 Data / NavIC L1 Data. */
    S1D(MeasurementType.SIGNAL_STRENGTH, SignalCode.D, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Signal-strength QZSS L1 C/B for Rinex3. */
    S1E(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.J01),

    /** Signal-strength Beidou B1 I for Rinex3. */
    S1I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I, PredefinedGnssSignal.B01),

    /** Signal-strength GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3, Beidou B1A for Rinex3.03. */
    S1L(MeasurementType.SIGNAL_STRENGTH, SignalCode.L, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Signal-strength GPS L2 M for Rinex3. */
    S1M(MeasurementType.SIGNAL_STRENGTH, SignalCode.M, PredefinedGnssSignal.G02),

    /** Signal-strength GPS L1 codeless / Beidou B1 codeless for Rinex3. */
    S1N(MeasurementType.SIGNAL_STRENGTH, SignalCode.CODELESS, PredefinedGnssSignal.G01, PredefinedGnssSignal.B1A),

    /** Signal-strength GPS L2 P(AS off) / GLONASS G2 P / Beidou B1 Pilot / NavIC L1 Pilot. */
    S1P(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Signal-strength GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    S1R(MeasurementType.SIGNAL_STRENGTH, SignalCode.M, PredefinedGnssSignal.G02),

    /** Signal-strength GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3, Beidou B1A for Rinex3.03. */
    S1S(MeasurementType.SIGNAL_STRENGTH, SignalCode.S, PredefinedGnssSignal.G01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Signal-strength GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    S1W(MeasurementType.SIGNAL_STRENGTH, SignalCode.W, PredefinedGnssSignal.G01),

    /** Signal-strength GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) / Beidou B1 Data+Pilot / NavIC L1 Data+Pilot. */
    S1X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.G01, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A, PredefinedGnssSignal.I01),

    /** Signal-strength GPS L1 Y for Rinex3. */
    S1Y(MeasurementType.SIGNAL_STRENGTH, SignalCode.Y, PredefinedGnssSignal.G01),

    /** Signal-strength Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3, Beidou B1A for Rinex3.03. */
    S1Z(MeasurementType.SIGNAL_STRENGTH, SignalCode.Z, PredefinedGnssSignal.E01, PredefinedGnssSignal.J01, PredefinedGnssSignal.B1A),

    /** Signal-strength GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    S2C(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Signal-strength GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    S2D(MeasurementType.SIGNAL_STRENGTH, SignalCode.D, PredefinedGnssSignal.G02),

    /** Signal-strength Beidou B1 I for Rinex3.03. */
    S2I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I,  PredefinedGnssSignal.B01),

    /** Signal-strength GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    S2L(MeasurementType.SIGNAL_STRENGTH, SignalCode.L, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Signal-strength GPS L2 M for Rinex3. */
    S2M(MeasurementType.SIGNAL_STRENGTH, SignalCode.M, PredefinedGnssSignal.G02),

    /** Signal-strength GPS L2 codeless for Rinex3. */
    S2N(MeasurementType.SIGNAL_STRENGTH, SignalCode.CODELESS, PredefinedGnssSignal.G02),

    /** Signal-strength GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    S2P(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.G02, PredefinedGnssSignal.R02),

    /** Signal-strength Beidou B1 Q for Rinex3.03. */
    S2Q(MeasurementType.SIGNAL_STRENGTH, SignalCode.Q, PredefinedGnssSignal.B01),

    /** Signal-strength GPS L2 M (RMP antenna) for Rinex4.01.
     * @since 13.0
     */
    S2R(MeasurementType.SIGNAL_STRENGTH, SignalCode.M, PredefinedGnssSignal.G02),

    /** Signal-strength GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    S2S(MeasurementType.SIGNAL_STRENGTH, SignalCode.S, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02),

    /** Signal-strength GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    S2W(MeasurementType.SIGNAL_STRENGTH, SignalCode.W, PredefinedGnssSignal.G02),

    /** Signal-strength GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3, Beidou B1 I+Q for Rinex3.03. */
    S2X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.G02, PredefinedGnssSignal.J02, PredefinedGnssSignal.B01),

    /** Signal-strength GPS L2 Y for Rinex3. */
    S2Y(MeasurementType.SIGNAL_STRENGTH, SignalCode.Y, PredefinedGnssSignal.G02),

    /** Signal-strength GLONASS G3 I for Rinex3. */
    S3I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I, PredefinedGnssSignal.R03),

    /** Signal-strength GLONASS G3 Q for Rinex3. */
    S3Q(MeasurementType.SIGNAL_STRENGTH, SignalCode.Q, PredefinedGnssSignal.R03),

    /** Signal-strength GLONASS G3 I+Q for Rinex3. */
    S3X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.R03),

    /** Signal-strength GLONASS G1a L1OCd for Rinex3. */
    S4A(MeasurementType.SIGNAL_STRENGTH, SignalCode.A, PredefinedGnssSignal.R04),

    /** Signal-strength GLONASS G1a L1OCp for Rinex3. */
    S4B(MeasurementType.SIGNAL_STRENGTH, SignalCode.B, PredefinedGnssSignal.R04),

    /** Signal-strength GLONASS G1a L1OCd+L1OCd for Rinex3. */
    S4X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.R04),

    /** Signal-strength NavIC L5 A for Rinex3. */
    S5A(MeasurementType.SIGNAL_STRENGTH, SignalCode.A, PredefinedGnssSignal.I05),

    /** Signal-strength NavIC L5 B for Rinex3. */
    S5B(MeasurementType.SIGNAL_STRENGTH, SignalCode.B, PredefinedGnssSignal.I05),

    /** Signal-strength NavIC L5 C for Rinex3. */
    S5C(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.I05),

    /** Signal-strength QZSS L5 D / Beidou B2a Data for Rinex3. */
    S5D(MeasurementType.SIGNAL_STRENGTH, SignalCode.D, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Signal-strength GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    S5I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Signal-strength QZSS L5 P / Beidou B2a Pilot for Rinex3. */
    S5P(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.J05, PredefinedGnssSignal.B2A),

    /** Signal-strength GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    S5Q(MeasurementType.SIGNAL_STRENGTH, SignalCode.Q, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05),

    /** Signal-strength GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q / NavIC L5 B+C / Beidou B2a Data+Pilot for Rinex3. */
    S5X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.G05, PredefinedGnssSignal.E05, PredefinedGnssSignal.S05, PredefinedGnssSignal.J05, PredefinedGnssSignal.I05, PredefinedGnssSignal.B2A),

    /** Signal-strength QZSS L5 D+P for Rinex3. */
    S5Z(MeasurementType.SIGNAL_STRENGTH, SignalCode.Z, PredefinedGnssSignal.J05),

    /** Signal-strength Galileo E6 A PRS / GLONASS G2a L2CSI/ Beidou B3A for Rinex3. */
    S6A(MeasurementType.SIGNAL_STRENGTH, SignalCode.A, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06, PredefinedGnssSignal.B03),

    /** Signal-strength Galileo E6 B C/NAV CS / GLONASS G2a L2OCp for Rinex3. */
    S6B(MeasurementType.SIGNAL_STRENGTH, SignalCode.B, PredefinedGnssSignal.E06, PredefinedGnssSignal.R06),

    /** Signal-strength Galileo E6 C no data for Rinex3. */
    S6C(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.E06),

    /** Signal-strength Beidou B3A for Rinex3. */
    S6D(MeasurementType.SIGNAL_STRENGTH, SignalCode.D, PredefinedGnssSignal.B3A),

    /** Signal-strength QZSS L6E for Rinex3. */
    S6E(MeasurementType.SIGNAL_STRENGTH, SignalCode.E, PredefinedGnssSignal.J06),

    /** Signal-strength Beidou B3 I for Rinex3. */
    S6I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I, PredefinedGnssSignal.B03),

    /** Signal-strength QZSS LEX(6) L for Rinex3. */
    S6L(MeasurementType.SIGNAL_STRENGTH, SignalCode.L, PredefinedGnssSignal.J06),

    /** Signal-strength Beidou B3A for Rinex3. */
    S6P(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.B3A),

    /** Signal-strength Beidou B3 Q for Rinex3. */
    S6Q(MeasurementType.SIGNAL_STRENGTH, SignalCode.Q, PredefinedGnssSignal.B03),

    /** Signal-strength QZSS LEX(6) S for Rinex3. */
    S6S(MeasurementType.SIGNAL_STRENGTH, SignalCode.S, PredefinedGnssSignal.J06),

    /** Signal-strength Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q / GLONASS G2a L2CSI+L2OCp for Rinex3. */
    S6X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B03, PredefinedGnssSignal.R06),

    /** Signal-strength Galileo E6 A+B+C / QZSS L6(D+E) / Beidou B3A for Rinex3. */
    S6Z(MeasurementType.SIGNAL_STRENGTH, SignalCode.Z, PredefinedGnssSignal.E06, PredefinedGnssSignal.J06, PredefinedGnssSignal.B3A),

    /** Signal-strength Beidou B2b Data for Rinex3. */
    S7D(MeasurementType.SIGNAL_STRENGTH, SignalCode.D, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Signal-strength Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    S7I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Signal-strength Beidou B2b Pilot for Rinex3. */
    S7P(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Signal-strength Galileo Q no data / Beidou B2 Q for Rinex3. */
    S7Q(MeasurementType.SIGNAL_STRENGTH, SignalCode.Q, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Signal-strength Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    S7X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.E07, PredefinedGnssSignal.B02),

    /** Signal-strength Beidou B2b Data+Pilot for Rinex3. */
    S7Z(MeasurementType.SIGNAL_STRENGTH, SignalCode.Z, PredefinedGnssSignal.B02, PredefinedGnssSignal.B2B),

    /** Signal-strength Beidou B2(B2a+B2b) Data for Rinex3. */
    S8D(MeasurementType.SIGNAL_STRENGTH, SignalCode.D, PredefinedGnssSignal.B08),

    /** Signal-strength Galileo E5(E5a+E5b) I for Rinex3. */
    S8I(MeasurementType.SIGNAL_STRENGTH, SignalCode.I, PredefinedGnssSignal.E08),

    /** Signal-strength Beidou B2(B2a+B2b) Pilot for Rinex3. */
    S8P(MeasurementType.SIGNAL_STRENGTH, SignalCode.P, PredefinedGnssSignal.B08),

    /** Signal-strength Galileo E5(E5a+E5b) for Rinex3. */
    S8Q(MeasurementType.SIGNAL_STRENGTH, SignalCode.Q, PredefinedGnssSignal.E08),

    /** Signal-strength Galileo E5(E5a+E5b) I+Q / Beidou B2(B2a+B2b) Data+Pilot for Rinex3. */
    S8X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.E08, PredefinedGnssSignal.B08),

    /** Signal-strength NavIC S A for Rinex3. */
    S9A(MeasurementType.SIGNAL_STRENGTH, SignalCode.A, PredefinedGnssSignal.I09),

    /** Signal-strength NavIC S B for Rinex3. */
    S9B(MeasurementType.SIGNAL_STRENGTH, SignalCode.B, PredefinedGnssSignal.I09),

    /** Signal-strength NavIC S C for Rinex3. */
    S9C(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.I09),

    /** Signal-strength NavIC S B+C for Rinex3. */
    S9X(MeasurementType.SIGNAL_STRENGTH, SignalCode.X, PredefinedGnssSignal.I09),

    /** Signal-strength for Rinex3. */
    S0(MeasurementType.SIGNAL_STRENGTH, SignalCode.CODELESS),

    /** Signal-strength GPS L1 C/A / GLONASS G1 C/A for Rinex2. */
    SA(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.G01, PredefinedGnssSignal.R01),

    /** Signal-strength GPS L1C for Rinex2. */
    SB(MeasurementType.SIGNAL_STRENGTH, SignalCode.L, PredefinedGnssSignal.G01),

    /** Signal-strength GPS L2C for Rinex2. */
    SC(MeasurementType.SIGNAL_STRENGTH, SignalCode.L, PredefinedGnssSignal.G02),

    /** Signal-strength GLONASS G2 for Rinex2. */
    SD(MeasurementType.SIGNAL_STRENGTH, SignalCode.C, PredefinedGnssSignal.R02);

    /** Measurement type. */
    private final MeasurementType type;

    /** Signal code. */
    private final SignalCode code;

    /** Map of frequencies. */
    private final Map<SatelliteSystem, PredefinedGnssSignal> frequencies;

    /** Simple  constructor.
     * @param type measurement type
     * @param code signal code
     * @param frequencies compatible frequencies
     */
    PredefinedObservationType(final MeasurementType type, final SignalCode code, final PredefinedGnssSignal... frequencies) {
        this.type = type;
        this.code = code;
        this.frequencies = new HashMap<>(frequencies.length);
        for (final PredefinedGnssSignal f : frequencies) {
            this.frequencies.put(f.getSatelliteSystem(), f);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name();
    }

    /** {@inheritDoc} */
    @Override
    public MeasurementType getMeasurementType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public SignalCode getSignalCode() {
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public GnssSignal getSignal(final SatelliteSystem system) {
        return frequencies.get(system);
    }

}
