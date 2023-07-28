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

import org.orekit.propagation.analytical.gnss.GLONASSAnalyticalPropagator;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.time.TimeStamped;

/** This interface provides the minimal set of orbital elements needed by the {@link GLONASSAnalyticalPropagator} and
 * the {@link GLONASSNumericalPropagator}.
 * <p>
 * Because input data are different between numerical and analytical GLONASS propagators the
 * methods present in this interface are implemented by default.
 * Depending if the user wants to use a {@link GLONASSNumericalPropagator} or a {@link GLONASSAnalyticalPropagator}
 * he can create an instance of a {@link GLONASSEphemeris} or {@link GLONASSAlmanac}.
 * </p>
 *
 * @see <a href="http://russianspacesystems.ru/wp-content/uploads/2016/08/ICD-GLONASS-CDMA-General.-Edition-1.0-2016.pdf">
 *       GLONASS Interface Control Document</a>
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public interface GLONASSOrbitalElements extends TimeStamped {

    /**
     * Get the number of the current day in a four year interval.
     *
     * @return the number of the current day in a four year interval
     */
    default int getNa() {
        return 0;
    }

    /**
     * Get the number of the current four year interval.
     *
     * @return the number of the current four year interval
     */
    default int getN4() {
        return 0;
    }

    /**
     * Get the Reference Time.
     *
     * @return the Reference Time (s)
     */
    default double getTime() {
        return 0.0;
    }

    /**
     * Get the longitude of ascending node of orbit.
     *
     * @return the longitude of ascending node of orbit (rad)
     */
    default double getLambda() {
        return 0.0;
    }

    /**
     * Get the Eccentricity.
     *
     * @return the Eccentricity
     */
    default double getE() {
        return 0.0;
    }

    /**
     * Get the Argument of Perigee.
     *
     * @return the Argument of Perigee (rad)
     */
    default double getPa() {
        return 0.0;
    }

    /**
     * Get the correction to the mean value of inclination.
     *
     * @return the correction to the mean value of inclination (rad)
     */
    default double getDeltaI() {
        return 0.0;
    }

    /**
     * Get the correction to the mean value of Draconian period.
     *
     * @return the correction to the mean value of Draconian period (s)
     */
    default double getDeltaT() {
        return 0.0;
    }

    /**
     * Get the rate of change of Draconian period.
     *
     * @return the rate of change of Draconian period
     */
    default double getDeltaTDot() {
        return 0.0;
    }

    /**
     * Get the relative deviation of predicted satellite carrier frequency from nominal value.
     *
     * @return the relative deviation of predicted satellite carrier frequency from nominal value
     */
    default double getGammaN() {
        return 0.0;
    }

    /**
     * Get the correction to the satellite time relative to GLONASS system time.
     *
     * @return the correction to the satellite time relative to GLONASS system time (s)
     *
     */
    default double getTN() {
        return 0.0;
    }

    /**
     * Get the ECEF-X component of satellite velocity vector in PZ-90 datum.
     *
     * @return the the ECEF-X component of satellite velocity vector in PZ-90 datum (m/s)
     */
    default double getXDot() {
        return 0.0;
    }

    /**
     * Get the ECEF-X component of satellite coordinates in PZ-90 datum.
     *
     * @return the ECEF-X component of satellite coordinates in PZ-90 datum (m)
     */
    default double getX() {
        return 0.0;
    }

    /**
     * Get the GLONASS ECEF-X component of satellite acceleration vector in PZ-90 datum.
     *
     * @return the GLONASS ECEF-X component of satellite acceleration vector in PZ-90 datum (m/s²)
     */
    default double getXDotDot() {
        return 0.0;
    }

    /**
     * Get the ECEF-Y component of satellite velocity vector in PZ-90 datum.
     *
     * @return the ECEF-Y component of satellite velocity vector in PZ-90 datum (m/s)
     */
    default double getYDot() {
        return 0.0;
    }

    /**
     * Get the ECEF-Y component of satellite coordinates in PZ-90 datum.
     *
     * @return the ECEF-Y component of satellite coordinates in PZ-90 datum (m)
     */
    default double getY() {
        return 0.0;
    }

    /**
     * Get the GLONASS ECEF-Y component of satellite acceleration vector in PZ-90 datum.
     *
     * @return the GLONASS ECEF-Y component of satellite acceleration vector in PZ-90 datum (m/s²)
     */
    default double getYDotDot() {
        return 0.0;
    }

    /**
     * Get the ECEF-Z component of satellite velocity vector in PZ-90 datum.
     *
     * @return the the ECEF-Z component of satellite velocity vector in PZ-90 datum (m/s)
     */
    default double getZDot() {
        return 0.0;
    }

    /**
     * Get the ECEF-Z component of satellite coordinates in PZ-90 datum.
     *
     * @return the ECEF-Z component of satellite coordinates in PZ-90 datum (m)
     */
    default double getZ() {
        return 0.0;
    }

    /**
     * Get the GLONASS ECEF-Z component of satellite acceleration vector in PZ-90 datum.
     *
     * @return the GLONASS ECEF-Z component of satellite acceleration vector in PZ-90 datum (m/s²)
     */
    default double getZDotDot() {
        return 0.0;
    }

    /**
     * Gets the GLONASS Issue Of Data (IOD).
     *
     * @return the IOD
     */
    default int getIOD() {
        return 0;
    }

}
