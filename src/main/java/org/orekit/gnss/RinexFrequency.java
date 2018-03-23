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
    C1,

    /** Pseudorange GPS L2 / GLONASS G2 for Rinex2. */
    C2,

    /** Pseudorange GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    C5,

    /** Pseudorange Galileo E6 for Rinex2. */
    C6,

    /** Pseudorange Galileo E5b for Rinex2. */
    C7,

    /** Pseudorange Galileo E5a+b for Rinex2. */
    C8,

    /** Pseudorange GPS L1 / GLONASS G1 for Rinex2. */
    P1,

    /** Pseudorange GPS L2 / GLONASS G2 for Rinex2. */
    P2,

    /** Carrier-phase GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    L1,

    /** Carrier-phase GPS L2 / GLONASS G2 for Rinex2. */
    L2,

    /** Carrier-phase GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    L5,

    /** Carrier-phase Galileo E6 for Rinex2. */
    L6,

    /** Carrier-phase Galileo E5b for Rinex2. */
    L7,

    /** Carrier-phase Galileo E5a+b for Rinex2. */
    L8,

    /** Doppler GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    D1,

    /** Doppler GPS L2 / GLONASS G2 for Rinex2. */
    D2,

    /** Doppler GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    D5,

    /** Doppler Galileo E6 for Rinex2. */
    D6,

    /** Doppler Galileo E5b for Rinex2. */
    D7,

    /** Doppler Galileo E5a+b for Rinex2. */
    D8,

    /** Doppler GPS L1 / GLONASS G1 / Galileo E2-L1-E1 / SBAS L1 for Rinex2. */
    S1,

    /** Signal Strength GPS L2 / GLONASS G2 for Rinex2. */
    S2,

    /** Signal Strength GPS L5 / Galileo E5a / SBAS L5 for Rinex2. */
    S5,

    /** Signal Strength Galileo E6 for Rinex2. */
    S6,

    /** Signal Strength Galileo E5b for Rinex2. */
    S7,

    /** Signal Strength Galileo E5a+b for Rinex2. */
    S8,

    /** Pseudorange Galileo E1 A for Rinex3. */
    C1A,

    /** Pseudorange Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    C1B,

    /** Pseudorange GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    C1C,

    /** Pseudorange Beidou B1 I for Rinex3.02. */
    C1I,

    /** Pseudorange GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    C1L,

    /** Pseudorange GPS L1 M for Rinex3. */
    C1M,

    /** Pseudorange GPS L1 P(AS off) / GLONASS G1 P for Rinex3. */
    C1P,

    /** Pseudorange Beidou B1 Q for Rinex3.02. */
    C1Q,

    /** Pseudorange GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    C1S,

    /** Pseudorange GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    C1W,

    /** Pseudorange GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    C1X,

    /** Pseudorange GPS L1 Y for Rinex3. */
    C1Y,

    /** Pseudorange Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    C1Z,

    /** Pseudorange GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    C2C,

    /** Pseudorange GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    C2D,

    /** Pseudorange Beidou B1 I for Rinex3.03. */
    C2I,

    /** Pseudorange GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    C2L,

    /** Pseudorange GPS L2 M for Rinex3. */
    C2M,

    /** Pseudorange GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    C2P,

    /** Pseudorange Beidou B1 Q for Rinex3.03. */
    C2Q,

    /** Pseudorange GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    C2S,

    /** Pseudorange GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    C2W,

    /** Pseudorange GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    C2X,

    /** Pseudorange GPS L2 Y for Rinex3. */
    C2Y,

    /** Pseudorange GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    C5I,

    /** Pseudorange GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    C5Q,

    /** Pseudorange GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    C5X,

    /** Pseudorange Galileo E6 A PRS for Rinex3. */
    C6A,

    /** Pseudorange Galileo E6 B C/NAV CS for Rinex3. */
    C6B,

    /** Pseudorange Galileo E6 C no data for Rinex3. */
    C6C,

    /** Pseudorange Beidou B3 I for Rinex3. */
    C6I,

    /** Pseudorange Beidou B3 Q for Rinex3. */
    C6Q,

    /** Pseudorange Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    C6X,

    /** Pseudorange Galileo E6 A+B+C for Rinex3. */
    C6Z,

    /** Pseudorange Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    C7I,

    /** Pseudorange Galileo Q no data / Beidou B2 Q for Rinex3. */
    C7Q,

    /** Pseudorange Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    C7X,

    /** Pseudorange Galileo E5(E5a+E5b) I for Rinex3. */
    C8I,

    /** Pseudorange Galileo E5(E5a+E5b) Q for Rinex3. */
    C8Q,

    /** Pseudorange Galileo E5(E5a+E5b) I+Q for Rinex3. */
    C8X,

    /** Doppler Galileo E1 A for Rinex3. */
    D1A,

    /** Doppler Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    D1B,

    /** Doppler GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    D1C,

    /** Doppler Beidou B1 I for Rinex3. */
    D1I,

    /** Doppler GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    D1L,

    /** Doppler GPS L2 M for Rinex3. */
    D1M,

    /** Doppler GPS L1 codeless for Rinex3. */
    D1N,

    /** Doppler GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    D1P,

    /** Doppler GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    D1S,

    /** Doppler GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    D1W,

    /** Doppler GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    D1X,

    /** Doppler GPS L1 Y for Rinex3. */
    D1Y,

    /** Doppler Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    D1Z,

    /** Doppler GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    D2C,

    /** Doppler GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    D2D,

    /** Doppler Beidou B1 I for Rinex3.03. */
    D2I,

    /** Doppler GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    D2L,

    /** Doppler GPS L2 M for Rinex3. */
    D2M,

    /** Doppler GPS L2 codeless for Rinex3. */
    D2N,

    /** Doppler GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    D2P,

    /** Doppler Beidou B1 Q for Rinex3.03. */
    D2Q,

    /** Doppler GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    D2S,

    /** Doppler GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    D2W,

    /** Doppler GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    D2X,

    /** Doppler GPS L2 Y for Rinex3. */
    D2Y,

    /** Doppler GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    D5I,

    /** Doppler GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    D5Q,

    /** Doppler GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    D5X,

    /** Doppler Galileo E6 A PRS for Rinex3. */
    D6A,

    /** Doppler Galileo E6 B C/NAV CS for Rinex3. */
    D6B,

    /** Doppler Galileo E6 C no data for Rinex3. */
    D6C,

    /** Doppler Beidou B3 I for Rinex3. */
    D6I,

    /** Doppler Beidou B3 Q for Rinex3. */
    D6Q,

    /** Doppler Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    D6X,

    /** Doppler Galileo E6 A+B+C for Rinex3. */
    D6Z,

    /** Doppler Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    D7I,

    /** Doppler Galileo Q no data / Beidou B2 Q for Rinex3. */
    D7Q,

    /** Doppler Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    D7X,

    /** Doppler Galileo E5(E5a+E5b) I for Rinex3. */
    D8I,

    /** Doppler Galileo E5(E5a+E5b) Q for Rinex3. */
    D8Q,

    /** Doppler Galileo E5(E5a+E5b) I+Q for Rinex3. */
    D8X,

    /** Carrier-phase Galileo E1 A for Rinex3. */
    L1A,

    /** Carrier-phase Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    L1B,

    /** Carrier-phase GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    L1C,

    /** Carrier-phase Beidou B1 I for Rinex3. */
    L1I,

    /** Carrier-phase GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    L1L,

    /** Carrier-phase GPS L2 M for Rinex3. */
    L1M,

    /** Carrier-phase GPS L1 codeless for Rinex3. */
    L1N,

    /** Carrier-phase GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    L1P,

    /** Carrier-phase GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    L1S,

    /** Carrier-phase GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    L1W,

    /** Carrier-phase GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    L1X,

    /** Carrier-phase GPS L1 Y for Rinex3. */
    L1Y,

    /** Carrier-phase Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    L1Z,

    /** Carrier-phase GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    L2C,

    /** Carrier-phase GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    L2D,

    /** Carrier-phase Beidou B1 I for Rinex3.03. */
    L2I,

    /** Carrier-phase GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    L2L,

    /** Carrier-phase GPS L2 M for Rinex3. */
    L2M,

    /** Carrier-phase GPS L2 codeless for Rinex3. */
    L2N,

    /** Carrier-phase GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    L2P,

    /** Carrier-phase Beidou B1 Q for Rinex3.03. */
    L2Q,

    /** Carrier-phase GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    L2S,

    /** Carrier-phase GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    L2W,

    /** Carrier-phase GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    L2X,

    /** Carrier-phase GPS L2 Y for Rinex3. */
    L2Y,

    /** Carrier-phase GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    L5I,

    /** Carrier-phase GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    L5Q,

    /** Carrier-phase GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    L5X,

    /** Carrier-phase Galileo E6 A PRS for Rinex3. */
    L6A,

    /** Carrier-phase Galileo E6 B C/NAV CS for Rinex3. */
    L6B,

    /** Carrier-phase Galileo E6 C no data for Rinex3. */
    L6C,

    /** Carrier-phase Beidou B3 I for Rinex3. */
    L6I,

    /** Carrier-phase Beidou B3 Q for Rinex3. */
    L6Q,

    /** Carrier-phase Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    L6X,

    /** Carrier-phase Galileo E6 A+B+C for Rinex3. */
    L6Z,

    /** Carrier-phase Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    L7I,

    /** Carrier-phase Galileo Q no data / Beidou B2 Q for Rinex3. */
    L7Q,

    /** Carrier-phase Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    L7X,

    /** Carrier-phase Galileo E5(E5a+E5b) I for Rinex3. */
    L8I,

    /** Carrier-phase Galileo E5(E5a+E5b) Q for Rinex3. */
    L8Q,

    /** Carrier-phase Galileo E5(E5a+E5b) I+Q for Rinex3. */
    L8X,

    /** Signal-strength Galileo E1 A for Rinex3. */
    S1A,

    /** Signal-strength Galileo E1 I/NAV OS/CS/SoL for Rinex3. */
    S1B,

    /** Signal-strength GPS L1 C/A / GLONASS G1 C/A / Galileo E1 C / SBAS L1 C/A / QZSS L1 C/A for Rinex3. */
    S1C,

    /** Signal-strength Beidou B1 I for Rinex3. */
    S1I,

    /** Signal-strength GPS L1 L1C(P) / QZSS L1 L1C(P) for Rinex3. */
    S1L,

    /** Signal-strength GPS L2 M for Rinex3. */
    S1M,

    /** Signal-strength GPS L1 codeless for Rinex3. */
    S1N,

    /** Signal-strength GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    S1P,

    /** Signal-strength GPS L1 L1C(D) / QZSS L1 L1C(D) for Rinex3. */
    S1S,

    /** Signal-strength GPS L1 Z-tracking and similar (AS on) for Rinex3. */
    S1W,

    /** Signal-strength GPS L1 L1C (D+P) / Galileo E1 B+C / QZSS L1 L1C(D+P) for Rinex3. */
    S1X,

    /** Signal-strength GPS L1 Y for Rinex3. */
    S1Y,

    /** Signal-strength Galileo E1 C1Z A+B+C / QZSS L1 L1-SAIF for Rinex3. */
    S1Z,

    /** Signal-strength GPS L2 C/A / GLONASS G2 C/A for Rinex3. */
    S2C,

    /** Signal-strength GPS L1(C/A)+(P2-P1) (semi-codeless) for Rinex3. */
    S2D,

    /** Signal-strength Beidou B1 I for Rinex3.03. */
    S2I,

    /** Signal-strength GPS L2 L2C(L) / QZSS L2 L2C(2) for Rinex3. */
    S2L,

    /** Signal-strength GPS L2 M for Rinex3. */
    S2M,

    /** Signal-strength GPS L2 codeless for Rinex3. */
    S2N,

    /** Signal-strength GPS L2 P(AS off) / GLONASS G2 P for Rinex3. */
    S2P,

    /** Signal-strength Beidou B1 Q for Rinex3.03. */
    S2Q,

    /** Signal-strength GPS L2 L2C(M) / QZSS L2 L2C(M) for Rinex3. */
    S2S,

    /** Signal-strength GPS L2 Z-tracking and similar (AS on) for Rinex3. */
    S2W,

    /** Signal-strength GPS L2 L2C (M+L) / QZSS L2 L2C(M+L) for Rinex3. */
    S2X,

    /** Signal-strength GPS L2 Y for Rinex3. */
    S2Y,

    /** Signal-strength GPS L5 I/ Galileo E5a F/NAV OS / SBAS L5 I / QZSS L5 I for Rinex3. */
    S5I,

    /** Signal-strength GPS L5 Q/ Galileo E5a Q / SBAS L5 Q / QZSS L5 Q for Rinex3. */
    S5Q,

    /** Signal-strength GPS L5 I+Q/ Galileo E5a I+Q / SBAS L5 I+Q / QZSS L5 I+Q for Rinex3. */
    S5X,

    /** Signal-strength Galileo E6 A PRS for Rinex3. */
    S6A,

    /** Signal-strength Galileo E6 B C/NAV CS for Rinex3. */
    S6B,

    /** Signal-strength Galileo E6 C no data for Rinex3. */
    S6C,

    /** Signal-strength Beidou B3 I for Rinex3. */
    S6I,

    /** Signal-strength Beidou B3 Q for Rinex3. */
    S6Q,

    /** Signal-strength Galileo E6 B+C / QZSS LEX(6) S+L / Beidou B3 I+Q for Rinex3. */
    S6X,

    /** Signal-strength Galileo E6 A+B+C for Rinex3. */
    S6Z,

    /** Signal-strength Galileo E5b I I/NAV OS/CS/SoL / Beidou B2 I for Rinex3. */
    S7I,

    /** Signal-strength Galileo Q no data / Beidou B2 Q for Rinex3. */
    S7Q,

    /** Signal-strength Galileo E5b I+Q / Beidou B2 I+Q for Rinex3. */
    S7X,

    /** Signal-strength Galileo E5(E5a+E5b) I for Rinex3. */
    S8I,

    /** Signal-strength Galileo E5(E5a+E5b) Q for Rinex3. */
    S8Q,

    /** Signal-strength Galileo E5(E5a+E5b) I+Q for Rinex3. */
    S8X;

}
