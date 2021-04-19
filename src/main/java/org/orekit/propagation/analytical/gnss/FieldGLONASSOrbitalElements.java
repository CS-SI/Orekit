/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.RealFieldElement;
import org.orekit.gnss.GLONASSAlmanac;
import org.orekit.gnss.GLONASSEphemeris;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.time.FieldTimeStamped;

/**
 * This interface provides the minimal set of orbital elements needed by the
 * {@link GLONASSAnalyticalPropagator} and the
 * {@link GLONASSNumericalPropagator}.
 * <p>
 * Because input data are different between numerical and analytical GLONASS
 * propagators the methods present in this interface are implemented by default.
 * Depending if the user wants to use a {@link GLONASSNumericalPropagator} or a
 * {@link GLONASSAnalyticalPropagator} he can create an instance of a
 * {@link GLONASSEphemeris} or {@link GLONASSAlmanac}.
 * </p>
 *
 * @see <a href=
 *      "http://russianspacesystems.ru/wp-content/uploads/2016/08/ICD-GLONASS-CDMA-General.-Edition-1.0-2016.pdf">
 *      GLONASS Interface Control Document</a>
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public interface FieldGLONASSOrbitalElements<T extends RealFieldElement<T>>
    extends
    FieldTimeStamped<T> {

    // Constants
    /**
     * Value of the Earth's universal gravitational parameter for GLONASS user
     * in m³/s².
     */
    double GLONASS_MU = 3.986004418e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double GLONASS_PI = 3.14159265358979;

    /**
     * Get the number of the current day in a four year interval.
     *
     * @return the number of the current day in a four year interval
     */
    int getNa();

    /**
     * Get the number of the current four year interval.
     *
     * @return the number of the current four year interval
     */
    int getN4();

    /**
     * Get the Reference Time.
     *
     * @return the Reference Time (s)
     */
    T getTime();

    /**
     * Get the longitude of ascending node of orbit.
     *
     * @return the longitude of ascending node of orbit (rad)
     */
    T getLambda();

    /**
     * Get the Eccentricity.
     *
     * @return the Eccentricity
     */
    T getE();

    /**
     * Get the Argument of Perigee.
     *
     * @return the Argument of Perigee (rad)
     */
    T getPa();

    /**
     * Get the correction to the mean value of inlination.
     *
     * @return the correction to the mean value of inlination (rad)
     */
    T getDeltaI();

    /**
     * Get the correction to the mean value of Draconian period.
     *
     * @return the correction to the mean value of Draconian period (s)
     */
    T getDeltaT();

    /**
     * Get the rate of change of Draconian period.
     *
     * @return the rate of change of Draconian period
     */
    T getDeltaTDot();

    /**
     * Get the relative deviation of predicted satellite carrier frequency from
     * nominal value.
     *
     * @return the relative deviation of predicted satellite carrier frequency
     *         from nominal value
     */
    T getGammaN();

    /**
     * Get the correction to the satellite time relative to GLONASS system time.
     *
     * @return the correction to the satellite time relative to GLONASS system
     *         time (s)
     */
    T getTN();

    /**
     * Get the ECEF-X component of satellite velocity vector in PZ-90 datum.
     *
     * @return the the ECEF-X component of satellite velocity vector in PZ-90
     *         datum (m/s)
     */
    T getXDot();

    /**
     * Get the ECEF-X component of satellite coordinates in PZ-90 datum.
     *
     * @return the ECEF-X component of satellite coordinates in PZ-90 datum (m)
     */
    T getX();

    /**
     * Get the GLONASS ECEF-X component of satellite acceleration vector in
     * PZ-90 datum.
     *
     * @return the GLONASS ECEF-X component of satellite acceleration vector in
     *         PZ-90 datum (m/s²)
     */
    T getXDotDot();

    /**
     * Get the ECEF-Y component of satellite velocity vector in PZ-90 datum.
     *
     * @return the ECEF-Y component of satellite velocity vector in PZ-90 datum
     *         (m/s)
     */
    T getYDot();

    /**
     * Get the ECEF-Y component of satellite coordinates in PZ-90 datum.
     *
     * @return the ECEF-Y component of satellite coordinates in PZ-90 datum (m)
     */
    T getY();

    /**
     * Get the GLONASS ECEF-Y component of satellite acceleration vector in
     * PZ-90 datum.
     *
     * @return the GLONASS ECEF-Y component of satellite acceleration vector in
     *         PZ-90 datum (m/s²)
     */
    T getYDotDot();

    /**
     * Get the ECEF-Z component of satellite velocity vector in PZ-90 datum.
     *
     * @return the the ECEF-Z component of satellite velocity vector in PZ-90
     *         datum (m/s)
     */
    T getZDot();

    /**
     * Get the ECEF-Z component of satellite coordinates in PZ-90 datum.
     *
     * @return the ECEF-Z component of satellite coordinates in PZ-90 datum (m)
     */
    T getZ();

    /**
     * Get the GLONASS ECEF-Z component of satellite acceleration vector in
     * PZ-90 datum.
     *
     * @return the GLONASS ECEF-Z component of satellite acceleration vector in
     *         PZ-90 datum (m/s²)
     */
    T getZDotDot();

    /**
     * Gets the GLONASS Issue Of Data (IOD).
     *
     * @return the IOD
     */
    int getIOD();
}
