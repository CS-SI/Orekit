/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

/** Enumerate for all the Observation Types for Rinex 2 and 3.
 * For Rinex 2, there is an two-character enumerate composed of the Observation
 * Code (C,P,L,D,S) and the Frequency code (1,2,5,6,7,8).
 * For Rinex 3 there is a three-character enumerate composed of the Observation
 * Code (C,L,D,S), the frequency code (1,2,5,6,7,8) and a final attribute depending
 * on the tracking mode or channel.
 *
 */
public enum RinexFrequency {

    /** Pseudorange GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    C1(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 / GLONASS G2 for Rinex2. */
    C2(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    C5(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E6 for Rinex2. */
    C6(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5b for Rinex2. */
    C7(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5a+b for Rinex2. */
    C8(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 / GLONASS G1 for Rinex2. */
    P1(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 / GLONASS G2 for Rinex2. */
    P2(MeasurementType.PSEUDO_RANGE),

    /** Carrier-phase GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    L1(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 / GLONASS G2 for Rinex2. */
    L2(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    L5(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E6 for Rinex2. */
    L6(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5b for Rinex2. */
    L7(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5a+b for Rinex2. */
    L8(MeasurementType.CARRIER_PHASE),

    /** Doppler GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    D1(MeasurementType.DOPPLER),

    /** Doppler GPS L2 / GLONASS G2 for Rinex2. */
    D2(MeasurementType.DOPPLER),

    /** Doppler GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    D5(MeasurementType.DOPPLER),

    /** Doppler Galileo E6 for Rinex2. */
    D6(MeasurementType.DOPPLER),

    /** Doppler Galileo E5b for Rinex2. */
    D7(MeasurementType.DOPPLER),

    /** Doppler Galileo E5a+b for Rinex2. */
    D8(MeasurementType.DOPPLER),

    /** Doppler GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    S1(MeasurementType.SIGNAL_STRENGTH),

    /** Signal Strength GPS L2 / GLONASS G2 for Rinex2. */
    S2(MeasurementType.SIGNAL_STRENGTH),

    /** Signal Strength GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    S5(MeasurementType.SIGNAL_STRENGTH),

    /** Signal Strength Galileo E6 for Rinex2. */
    S6(MeasurementType.SIGNAL_STRENGTH),

    /** Signal Strength Galileo E5b for Rinex2. */
    S7(MeasurementType.SIGNAL_STRENGTH),

    /** Signal Strength Galileo E5a+b for Rinex2. */
    S8(MeasurementType.SIGNAL_STRENGTH),

    /** Pseudorange Galileo E1 A for Rinex3. */
    C1A(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    C1B(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    C1C(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Beidou B1 I for Rinex3.02. */
    C1I(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    C1L(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 M for Rinex3. */
    C1M(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 P(AS off) / GLONASS G1 P for Rinex3. */
    C1P(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Beidou B1 Q for Rinex3.02. */
    C1Q(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    C1S(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    C1W(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    C1X(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1 Y for Rinex3. */
    C1Y(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    C1Z(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    C2C(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    C2D(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Beidou B1 I for Rinex3.03. */
    C2I(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    C2L(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 M for Rinex3. */
    C2M(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    C2P(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Beidou B1 Q for Rinex3.03. */
    C2Q(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    C2S(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    C2W(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    C2X(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L2 Y for Rinex3. */
    C2Y(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    C5I(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    C5Q(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    C5X(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E6 A PRS for Rinex3. */
    C6A(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E6 B C/NAV CS for Rinex3. */
    C6B(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E6 C no data for Rinex3. */
    C6C(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Beidou B3 I for Rinex3. */
    C6I(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Beidou B3 Q for Rinex3. */
    C6Q(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    C6X(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E6 A+B+C for Rinex3. */
    C6Z(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    C7I(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo Q no data / Beidou B2 Q for Rinex3. */
    C7Q(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    C7X(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5(E5a+E5b) I for Rinex3. */
    C8I(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5(E5a+E5b) Q for Rinex3. */
    C8Q(MeasurementType.PSEUDO_RANGE),

    /** Pseudorange Galileo E5(E5a+E5b) I+Q for Rinex3. */
    C8X(MeasurementType.PSEUDO_RANGE),

    /** Doppler Galileo E1 A for Rinex3. */
    D1A(MeasurementType.DOPPLER),

    /** Doppler Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    D1B(MeasurementType.DOPPLER),

    /** Doppler GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    D1C(MeasurementType.DOPPLER),

    /** Doppler Beidou B1 I for Rinex3. */
    D1I(MeasurementType.DOPPLER),

    /** Doppler GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    D1L(MeasurementType.DOPPLER),

    /** Doppler GPS L2 M for Rinex3. */
    D1M(MeasurementType.DOPPLER),

    /** Doppler GPS L1 codeless for Rinex3. */
    D1N(MeasurementType.DOPPLER),

    /** Doppler GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    D1P(MeasurementType.DOPPLER),

    /** Doppler GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    D1S(MeasurementType.DOPPLER),

    /** Doppler GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    D1W(MeasurementType.DOPPLER),

    /** Doppler GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    D1X(MeasurementType.DOPPLER),

    /** Doppler GPS L1 Y for Rinex3. */
    D1Y(MeasurementType.DOPPLER),

    /** Doppler Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    D1Z(MeasurementType.DOPPLER),

    /** Doppler GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    D2C(MeasurementType.DOPPLER),

    /** Doppler GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    D2D(MeasurementType.DOPPLER),

    /** Doppler Beidou B1 I for Rinex3.03. */
    D2I(MeasurementType.DOPPLER),

    /** Doppler GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    D2L(MeasurementType.DOPPLER),

    /** Doppler GPS L2 M for Rinex3. */
    D2M(MeasurementType.DOPPLER),

    /** Doppler GPS L2 codeless for Rinex3. */
    D2N(MeasurementType.DOPPLER),

    /** Doppler GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    D2P(MeasurementType.DOPPLER),

    /** Doppler Beidou B1 Q for Rinex3.03. */
    D2Q(MeasurementType.DOPPLER),

    /** Doppler GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    D2S(MeasurementType.DOPPLER),

    /** Doppler GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    D2W(MeasurementType.DOPPLER),

    /** Doppler GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    D2X(MeasurementType.DOPPLER),

    /** Doppler GPS L2 Y for Rinex3. */
    D2Y(MeasurementType.DOPPLER),

    /** Doppler GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    D5I(MeasurementType.DOPPLER),

    /** Doppler GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    D5Q(MeasurementType.DOPPLER),

    /** Doppler GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    D5X(MeasurementType.DOPPLER),

    /** Doppler Galileo E6 A PRS for Rinex3. */
    D6A(MeasurementType.DOPPLER),

    /** Doppler Galileo E6 B C/NAV CS for Rinex3. */
    D6B(MeasurementType.DOPPLER),

    /** Doppler Galileo E6 C no data for Rinex3. */
    D6C(MeasurementType.DOPPLER),

    /** Doppler Beidou B3 I for Rinex3. */
    D6I(MeasurementType.DOPPLER),

    /** Doppler Beidou B3 Q for Rinex3. */
    D6Q(MeasurementType.DOPPLER),

    /** Doppler Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    D6X(MeasurementType.DOPPLER),

    /** Doppler Galileo E6 A+B+C for Rinex3. */
    D6Z(MeasurementType.DOPPLER),

    /** Doppler Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    D7I(MeasurementType.DOPPLER),

    /** Doppler Galileo Q no data / Beidou B2 Q for Rinex3. */
    D7Q(MeasurementType.DOPPLER),

    /** Doppler Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    D7X(MeasurementType.DOPPLER),

    /** Doppler Galileo E5(E5a+E5b) I for Rinex3. */
    D8I(MeasurementType.DOPPLER),

    /** Doppler Galileo E5(E5a+E5b) Q for Rinex3. */
    D8Q(MeasurementType.DOPPLER),

    /** Doppler Galileo E5(E5a+E5b) I+Q for Rinex3. */
    D8X(MeasurementType.DOPPLER),

    /** Carrier-phase Galileo E1 A for Rinex3. */
    L1A(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    L1B(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    L1C(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Beidou B1 I for Rinex3. */
    L1I(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    L1L(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 M for Rinex3. */
    L1M(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 codeless for Rinex3. */
    L1N(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    L1P(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    L1S(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    L1W(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    L1X(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1 Y for Rinex3. */
    L1Y(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    L1Z(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    L2C(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    L2D(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Beidou B1 I for Rinex3.03. */
    L2I(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    L2L(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 M for Rinex3. */
    L2M(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 codeless for Rinex3. */
    L2N(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    L2P(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Beidou B1 Q for Rinex3.03. */
    L2Q(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    L2S(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    L2W(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    L2X(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L2 Y for Rinex3. */
    L2Y(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    L5I(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    L5Q(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    L5X(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E6 A PRS for Rinex3. */
    L6A(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E6 B C/NAV CS for Rinex3. */
    L6B(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E6 C no data for Rinex3. */
    L6C(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Beidou B3 I for Rinex3. */
    L6I(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Beidou B3 Q for Rinex3. */
    L6Q(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    L6X(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E6 A+B+C for Rinex3. */
    L6Z(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    L7I(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo Q no data / Beidou B2 Q for Rinex3. */
    L7Q(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    L7X(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5(E5a+E5b) I for Rinex3. */
    L8I(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5(E5a+E5b) Q for Rinex3. */
    L8Q(MeasurementType.CARRIER_PHASE),

    /** Carrier-phase Galileo E5(E5a+E5b) I+Q for Rinex3. */
    L8X(MeasurementType.CARRIER_PHASE),

    /** Signal-strength Galileo E1 A for Rinex3. */
    S1A(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    S1B(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    S1C(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Beidou B1 I for Rinex3. */
    S1I(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    S1L(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 M for Rinex3. */
    S1M(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 codeless for Rinex3. */
    S1N(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    S1P(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    S1S(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    S1W(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    S1X(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1 Y for Rinex3. */
    S1Y(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    S1Z(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    S2C(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    S2D(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Beidou B1 I for Rinex3.03. */
    S2I(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    S2L(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 M for Rinex3. */
    S2M(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 codeless for Rinex3. */
    S2N(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    S2P(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Beidou B1 Q for Rinex3.03. */
    S2Q(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    S2S(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    S2W(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    S2X(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L2 Y for Rinex3. */
    S2Y(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    S5I(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    S5Q(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    S5X(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E6 A PRS for Rinex3. */
    S6A(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E6 B C/NAV CS for Rinex3. */
    S6B(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E6 C no data for Rinex3. */
    S6C(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Beidou B3 I for Rinex3. */
    S6I(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Beidou B3 Q for Rinex3. */
    S6Q(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    S6X(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E6 A+B+C for Rinex3. */
    S6Z(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    S7I(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo Q no data / Beidou B2 Q for Rinex3. */
    S7Q(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    S7X(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E5(E5a+E5b) I for Rinex3. */
    S8I(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E5(E5a+E5b) Q for Rinex3. */
    S8Q(MeasurementType.SIGNAL_STRENGTH),

    /** Signal-strength Galileo E5(E5a+E5b) I+Q for Rinex3. */
    S8X(MeasurementType.SIGNAL_STRENGTH);

    /** Measurement type. */
    private final MeasurementType type;

    /** Simple  constructor.
     * @param type measurement type
     */
    RinexFrequency(final MeasurementType type) {
        this.type = type;
    }

    /** Get the measurement type.
     * @return measurement type
     */
    public MeasurementType getType() {
        return type;
    }

}
