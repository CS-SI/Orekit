/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.analytical.gnss;

/** This interface provides the minimal set of orbital elements needed by the {@link QZSSPropagator}.
 *
 * @see <a href="http://qzss.go.jp/en/technical/download/pdf/ps-is-qzss/is-qzss-pnt-003.pdf?t=1549268771755">
 *       QZSS Interface Specification</a>
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public interface QZSSOrbitalElements extends GNSSOrbitalElements {

    // Constants
    /** WGS 84 value of the Earth's universal gravitational parameter for QZSS user in m³/s². */
    double QZSS_MU = 3.986005e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double QZSS_PI = 3.1415926535898;

    /** Duration of the QZSS week in seconds. */
    double QZSS_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the QZSS cycle. */
    int QZSS_WEEK_NB = 1024;

    /**
     * Gets the Issue Of Data Clock (IODC).
     *
     * @return the Issue Of Data Clock (IODC)
     */
    default int getIODC() {
        return 0;
    }

    /**
     * Gets the Issue Of Data Ephemeris (IODE).
     *
     * @return the Issue Of Data Ephemeris (IODE)
     */
    default int getIODE() {
        return 0;
    }

    /**
     * Gets the estimated group delay differential TGD between SV clock and L1C/A.
     *
     * @return the estimated group delay differential TGD between SV clock and L1C/A (s)
     */
    default double getTGD() {
        return 0.0;
    }

}
