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

/** This interface provides the minimal set of orbital elements needed by the {@link IRNSSPropagator}.
*
* @see "Indian Regiona Navigation Satellite System, Signal In Space ICD
*       for standard positioning service, version 1.1"
*
* @author Bryan Cazabonne
* @since 10.1
*
*/
public interface IRNSSOrbitalElements extends GNSSOrbitalElements {

    /** WGS 84 value of the Earth's universal gravitational parameter for IRNSS user in m³/s². */
    double IRNSS_MU = 3.986005e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double IRNSS_PI = 3.1415926535898;

    /** Duration of the IRNSS week in seconds. */
    double IRNSS_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the IRNSS cycle. */
    int IRNSS_WEEK_NB = 1024;

    /**
     * Gets the Issue Of Data Ephemeris and Clock (IODEC).
     *
     * @return the Issue Of Data Ephemeris and Clock (IODEC)
     */
    default int getIODEC() {
        return 0;
    }

    /**
     * Gets the estimated group delay differential TGD for L5-S correction.
     *
     * @return the estimated group delay differential TGD for L5-S correction (s)
     */
    default double getTGD() {
        return 0.0;
    }

}
