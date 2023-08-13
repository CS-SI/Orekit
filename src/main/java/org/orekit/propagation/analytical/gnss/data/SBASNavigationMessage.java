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
import org.orekit.propagation.analytical.gnss.SBASPropagator;
import org.orekit.propagation.analytical.gnss.SBASPropagatorBuilder;

/**
 * Container for data contained in a SBAS navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SBASNavigationMessage extends AbstractEphemerisMessage implements SBASOrbitalElements {

    /** Transmission time  of  message  (start  of  the message) in GPS seconds of the week. */
    private double time;

    /** SV clock bias (s). */
    private double aGf0;

    /** SV relative frequency. */
    private double aGf1;

    /** User range accuracy (m). */
    private double ura;

    /** Issue of data navigation (IODN). */
    private int iodn;

    /** Constructor. */
    public SBASNavigationMessage() {
        // Nothing to do ...
    }

    /**
     * Get the propagator corresponding to the navigation message.
     <p>The attitude provider is set by default be aligned with the EME2000 frame.<br>
     * The Earth gravity coefficient is set by default to the
     *  {@link org.orekit.propagation.analytical.gnss.data.GNSSConstants#SBAS_MU SBAS_MU}.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
     * </p><p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * </p>
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(Frames)
     * @see #getPropagator(Frames, AttitudeProvider, Frame, Frame, double, double)
     * @since 12.0
     */
    @DefaultDataContext
    public SBASPropagator getPropagator() {
        return new SBASPropagatorBuilder(this).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>The attitude provider is set by default be aligned with the EME2000 frame.<br>
     * The Earth gravity coefficient is set by default to the
     *  {@link org.orekit.propagation.analytical.gnss.data.GNSSConstants#SBAS_MU SBAS_MU}.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
     * </p>
     * @param frames set of frames to use
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(Frames, AttitudeProvider, Frame, Frame, double, double)
     * @since 12.0
     */
    public SBASPropagator getPropagator(final Frames frames) {
        return new SBASPropagatorBuilder(this, frames).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param frames set of frames to use
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @param mu central attraction coefficient
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(Frames)
     * @since 12.0
     */
    public SBASPropagator getPropagator(final Frames frames, final AttitudeProvider provider,
                                        final Frame inertial, final Frame bodyFixed,
                                        final double mass, final double mu) {
        return new SBASPropagatorBuilder(this, frames).attitudeProvider(provider)
                                                      .eci(inertial)
                                                      .ecef(bodyFixed)
                                                      .mass(mass)
                                                      .mu(mu)
                                                      .build();
    }

    /** {@inheritDoc} */
    @Override
    public int getWeek() {
        // No provided by the SBAS navigation message
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double getTime() {
        return time;
    }

    /**
     * Setter for the reference time of the SBAS orbit in GPS seconds of the week.
     * @param time the time to set
     */
    public void setTime(final double time) {
        this.time = time;
    }

    /** {@inheritDoc} */
    @Override
    public int getIODN() {
        return iodn;
    }

    /**
     * Setter for the issue of data navigation.
     * @param iod the issue of data to set
     */
    public void setIODN(final double iod) {
        // The value is given as a floating number in the navigation message
        this.iodn = (int) iod;
    }


    /** {@inheritDoc} */
    @Override
    public double getAGf0() {
        return aGf0;
    }

    /**
     * Setter for the SV clock bias.
     * @param a0 the SV clock bias to set in seconds
     */
    public void setAGf0(final double a0) {
        this.aGf0 = a0;
    }

    /** {@inheritDoc} */
    @Override
    public double getAGf1() {
        return aGf1;
    }

    /**
     * Setter for the SV relative frequency.
     * @param a1 the SV relative frequency to set
     */
    public void setAGf1(final double a1) {
        this.aGf1 = a1;
    }


    /**
     * Getter for the user range accuray (meters).
     * @return the user range accuracy
     */
    public double getURA() {
        return ura;
    }

    /**
     * Setter for the user range accuracy.
     * @param accuracy the value to set
     */
    public void setURA(final double accuracy) {
        this.ura = accuracy;
    }

}
