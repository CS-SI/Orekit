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

import org.orekit.time.TimeStamped;

/** This interface provides the minimal set of orbital elements needed by the {@link AbstractGNSSPropagator}.
*
* @author Pascal Parraud
*
*/
public interface GNSSOrbitalElements extends TimeStamped {

    /**
     * Gets the PRN number of the GNSS satellite.
     *
     * @return the PRN number of the GNSS satellite
     */
    int getPRN();

    /**
     * Gets the Reference Week of the GNSS orbit.
     *
     * @return the Reference Week of the GNSS orbit within [0, 1024[
     */
    int getWeek();

    /**
     * Gets the Reference Time of the GNSS orbit as a duration from week start.
     *
     * @return the Reference Time of the GNSS orbit (s)
     */
    double getTime();

    /**
     * Gets the Semi-Major Axis.
     *
     * @return the Semi-Major Axis (m)
     */
    double getSma();

    /**
     * Gets the Mean Motion.
     *
     * @return the Mean Motion (rad/s)
     */
    double getMeanMotion();

    /**
     * Gets the Eccentricity.
     *
     * @return the Eccentricity
     */
    double getE();

    /**
     * Gets the Inclination Angle at Reference Time.
     *
     * @return the Inclination Angle at Reference Time (rad)
     */
    double getI0();

    /**
     * Gets the Rate of Inclination Angle.
     *
     * @return the Rate of Inclination Angle (rad/s)
     */
    double getIDot();

    /**
     * Gets the Longitude of Ascending Node of Orbit Plane at Weekly Epoch.
     *
     * @return the Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad)
     */
    double getOmega0();

    /**
     * Gets the Rate of Right Ascension.
     *
     * @return the Rate of Right Ascension (rad/s)
     */
    double getOmegaDot();

    /**
     * Gets the Argument of Perigee.
     *
     * @return the Argument of Perigee (rad)
     */
    double getPa();

    /**
     * Gets the Mean Anomaly at Reference Time.
     *
     * @return the Mean Anomaly at Reference Time (rad)
     */
    double getM0();

    /**
     * Gets the Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude.
     *
     * @return the Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude (rad)
     */
    double getCuc();

    /**
     * Gets the Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude.
     *
     * @return the Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude (rad)
     */
    double getCus();

    /**
     * Gets the Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius.
     *
     * @return the Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius (m)
     */
    double getCrc();

    /**
     * Gets the Amplitude of the Sine Harmonic Correction Term to the Orbit Radius.
     *
     * @return the Amplitude of the Sine Harmonic Correction Term to the Orbit Radius (m)
     */
    double getCrs();

    /**
     * Gets the Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination.
     *
     * @return the Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination (rad)
     */
    double getCic();

    /**
     * Gets the Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination.
     *
     * @return the Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination (rad)
     */
    double getCis();

    /**
     * Gets the Zeroth Order Clock Correction.
     *
     * @return the Zeroth Order Clock Correction (s)
     * @see #getAf1()
     * @see #getAf2()
     * @see #getToc()
     * @since 9.3
     */
    default double getAf0() {
        return 0.0;
    }

    /**
     * Gets the First Order Clock Correction.
     *
     * @return the First Order Clock Correction (s/s)
     * @see #getAf0()
     * @see #getAf2()
     * @see #getToc()
     * @since 9.3
     */
    default double getAf1() {
        return 0.0;
    }

    /**
     * Gets the Second Order Clock Correction.
     *
     * @return the Second Order Clock Correction (s/s²)
     * @see #getAf0()
     * @see #getAf1()
     * @see #getToc()
     * @since 9.3
     */
    default double getAf2() {
        return 0.0;
    }

    /**
     * Gets the clock correction reference time toc.
     *
     * @return the clock correction reference time (s)
     * @see #getAf0()
     * @see #getAf1()
     * @see #getAf2()
     * @since 9.3
     */
    default double getToc() {
        return 0.0;
    }

}
