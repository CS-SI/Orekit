/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.propagation.analytical.gnss.SBASPropagator;
import org.orekit.time.TimeStamped;

/** This interface provides the minimal set of orbital elements needed by the {@link SBASPropagator}.
*
* @author Bryan Cazabonne
* @since 10.1
*
*/
public interface SBASOrbitalElements extends TimeStamped {

    /**
     * Gets the PRN number of the SBAS satellite.
     *
     * @return the PRN number of the SBAS satellite
     */
    int getPRN();

    /**
     * Gets the Reference Week of the SBAS orbit.
     *
     * @return the Reference Week of the SBAS orbit
     */
    int getWeek();

    /**
     * Gets the Reference Time of the SBAS orbit in GPS seconds of the week.
     *
     * @return the Reference Time of the SBAS orbit (s)
     */
    double getTime();

    /**
     * Get the ECEF-X component of satellite coordinates.
     *
     * @return the ECEF-X component of satellite coordinates (m)
     */
    double getX();

    /**
     * Get the ECEF-X component of satellite velocity vector.
     *
     * @return the the ECEF-X component of satellite velocity vector (m/s)
     */
    double getXDot();

    /**
     * Get the ECEF-X component of satellite acceleration vector.
     *
     * @return the GLONASS ECEF-X component of satellite acceleration vector (m/s²)
     */
    double getXDotDot();

    /**
     * Get the ECEF-Y component of satellite coordinates.
     *
     * @return the ECEF-Y component of satellite coordinates (m)
     */
    double getY();

    /**
     * Get the ECEF-Y component of satellite velocity vector.
     *
     * @return the ECEF-Y component of satellite velocity vector (m/s)
     */
    double getYDot();

    /**
     * Get the ECEF-Y component of satellite acceleration vector.
     *
     * @return the ECEF-Y component of satellite acceleration vector (m/s²)
     */
    double getYDotDot();

    /**
     * Get the ECEF-Z component of satellite coordinates.
     *
     * @return the ECEF-Z component of satellite coordinates (m)
     */
    double getZ();

    /**
     * Get the ECEF-Z component of satellite velocity vector.
     *
     * @return the the ECEF-Z component of satellite velocity vector (m/s)
     */
    double getZDot();

    /**
     * Get the ECEF-Z component of satellite acceleration vector.
     *
     * @return the ECEF-Z component of satellite acceleration vector (m/s²)
     */
    double getZDotDot();

    /**
     * Gets the Issue Of Data Navigation (IODN).
     *
     * @return the IODN
     */
    default int getIODN() {
        return 0;
    }

    /**
     * Gets the Zeroth Order Clock Correction.
     *
     * @return the Zeroth Order Clock Correction (s)
     */
    default double getAGf0() {
        return 0.0;
    }

    /**
     * Gets the First Order Clock Correction.
     *
     * @return the First Order Clock Correction (s/s)
     */
    default double getAGf1() {
        return 0.0;
    }

    /**
     * Gets the clock correction reference time toc.
     *
     * @return the clock correction reference time (s)
     */
    default double getToc() {
        return 0.0;
    }

}
