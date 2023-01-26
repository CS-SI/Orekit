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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.time.TimeStamped;

/** This interface provides the minimal set of orbital elements needed by the {@link GNSSPropagator}.
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
     * Gets the Earth's universal gravitational parameter.
     *
     * @return the Earth's universal gravitational parameter
     */
    double getMu();

    /**
     * Gets the mean angular velocity of the Earth of the GNSS model.
     *
     * @return the mean angular velocity of the Earth of the GNSS model
     */
    double getAngularVelocity();

    /**
     * Gets the duration of the GNSS cycle in seconds.
     *
     * @return the duration of the GNSS cycle in seconds
     */
    double getCycleDuration();

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     *  CIO/2010-based ITRF simple EOP} in the default data context.
     * </p><p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * </p>
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(Frames)
     * @see #getPropagator(Frames, AttitudeProvider, Frame, Frame, double)
     * @since 12.0
     */
    @DefaultDataContext
    default GNSSPropagator getPropagator() {
        return new GNSSPropagatorBuilder(this).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     *  CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * @param frames set of frames to use
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(Frames, AttitudeProvider, Frame, Frame, double)
     * @since 12.0
     */
    default GNSSPropagator getPropagator(final Frames frames) {
        return new GNSSPropagatorBuilder(this, frames).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param frames set of frames to use
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(Frames)
     * @since 12.0
     */
    default GNSSPropagator getPropagator(final Frames frames, final AttitudeProvider provider,
                                         final Frame inertial, final Frame bodyFixed, final double mass) {
        return new GNSSPropagatorBuilder(this, frames).attitudeProvider(provider)
                                                      .eci(inertial)
                                                      .ecef(bodyFixed)
                                                      .mass(mass)
                                                      .build();
    }

}
