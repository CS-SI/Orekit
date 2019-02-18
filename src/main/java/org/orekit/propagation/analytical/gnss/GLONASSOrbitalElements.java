/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.gnss;

import org.orekit.time.TimeStamped;

/** This interface provides the minimal set of orbital elements needed by the {@link GLONASSPropagator}.
*
* @see <a href="http://russianspacesystems.ru/wp-content/uploads/2016/08/ICD-GLONASS-CDMA-General.-Edition-1.0-2016.pdf">
*       GLONASS Interface Control Document</a>
*
* @author Bryan Cazabonne
*
*/
public interface GLONASSOrbitalElements extends TimeStamped {

    // Constants
    /** Value of the Earth's universal gravitational parameter for GLONASS user in m³/s². */
    double GLONASS_MU = 3.986004418e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double GLONASS_PI = 3.14159265358979;

    /**
     * Get the Reference Time of the almanac.
     *
     * @return the Reference Time of the almanac (s)
     */
    double getTime();

    /**
     * Get the longitude of ascending node of orbit.
     *
     * @return the longitude of ascending node of orbit (rad)
     */
    double getLambda();

    /**
     * Get the Eccentricity.
     *
     * @return the Eccentricity
     */
    double getE();

    /**
     * Get the Argument of Perigee.
     *
     * @return the Argument of Perigee (rad)
     */
    double getPa();

    /**
     * Get the correction to the mean value of inlination.
     *
     * @return the correction to the mean value of inlination (rad)
     */
    double getDeltaI();

    /**
     * Get the correction to the mean value of Draconian period.
     *
     * @return the correction to the mean value of Draconian period (s)
     */
    double getDeltaT();

    /**
     * Get the rate of change of Draconian period.
     *
     * @return the rate of change of Draconian period
     */
    double getDeltaTDot();

    /**
     * Get the relative deviation of predicted satellite corrier frequency from nominal value.
     *
     * @return the relative deviation of predicted satellite corrier frequency from nominal value
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

}
