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

import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.propagation.numerical.GLONASSNumericalPropagatorBuilder;

/**
 * Container for data contained in a Glonass navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class GLONASSNavigationMessage extends AbstractEphemerisMessage implements GLONASSOrbitalElements {

    /** Message frame time. */
    private double time;

    /** SV clock bias. */
    private double tauN;

    /** SV relative frequency bias. */
    private double gammaN;

    /** Frequency number. */
    private int frequencyNumber;

    /** Status flags.
     * @since 12.0
     */
    private int statusFlags;

    /** Health flags.
     * @since 12.0
     */
    private int healthFlags;

    /** Group Delay Difference (s).
     * @since 12.0
     */
    private double groupDelayDifference;

    /** User range accuracy (m).
     * @since 12.0
     */
    private double ura;

    /** Constructor. */
    public GLONASSNavigationMessage() {
        // Nothing to do ...
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>The attitude provider is set by default to EME2000 aligned in the
     *  default data context.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the
     *  {@link DataContext#getDefault() default data context}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * </p>
     * @param step integration step in seconds
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(double, DataContext)
     * @see #getPropagator(double, DataContext, AttitudeProvider, Frame, double)
     * @since 12.0
     */
    @DefaultDataContext
    public GLONASSNumericalPropagator getPropagator(final double step) {
        return new GLONASSNumericalPropagatorBuilder(new ClassicalRungeKuttaIntegrator(step),
                                                     this, isAccAvailable()).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>The attitude provider is set by default to EME2000 aligned in the
     *  default data context.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the
     *  {@link DataContext#getDefault() default data context}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * </p>
     * @param step integration step in seconds
     * @param context data context
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(double)
     * @see #getPropagator(double, DataContext, AttitudeProvider, Frame, double)
     * @since 12.0
     */
    public GLONASSNumericalPropagator getPropagator(final double step, final DataContext context) {
        return new GLONASSNumericalPropagatorBuilder(new ClassicalRungeKuttaIntegrator(step),
                                                     this, isAccAvailable(), context).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param step integration step in seconds
     * @param context data context
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(double)
     * @see #getPropagator(double, DataContext)
     * @since 12.0
     */
    public GLONASSNumericalPropagator getPropagator(final double step, final DataContext context,
                                                    final AttitudeProvider provider, final Frame inertial,
                                                    final double mass) {
        return new GLONASSNumericalPropagatorBuilder(new ClassicalRungeKuttaIntegrator(step),
                                                     this, isAccAvailable(), context).attitudeProvider(provider)
                                                                                     .eci(inertial)
                                                                                     .mass(mass)
                                                                                     .build();
    }

    /** {@inheritDoc} */
    @Override
    public double getTN() {
        return tauN;
    }

    /**
     * Setter for the SV clock bias.
     * @param tn the SV clock bias
     */
    public void setTauN(final double tn) {
        this.tauN = tn;
    }

    /** {@inheritDoc} */
    @Override
    public double getGammaN() {
        return gammaN;
    }

    /**
     * Setter for the SV relative frequency bias.
     * @param gammaN the SV relative frequency bias.
     */
    public void setGammaN(final double gammaN) {
        this.gammaN = gammaN;
    }

    /**
     * Getter for the frequency number.
     * @return the frequency number
     */
    public int getFrequencyNumber() {
        return frequencyNumber;
    }

    /**
     * Setter for the frequency number.
     * @param frequencyNumber the number to set
     */
    public void setFrequencyNumber(final double frequencyNumber) {
        this.frequencyNumber = (int) frequencyNumber;
    }

    /** {@inheritDoc} */
    @Override
    public double getTime() {
        return time;
    }

    /**
     * Setter for the message frame time.
     * @param time the time to set
     */
    public void setTime(final double time) {
        this.time = time;
    }

    /** Get status flags.
     * @return status flags
     * @since 12.0
     */
    public int getStatusFlags() {
        return statusFlags;
    }

    /** Set status flag.
     * @param statusFlags status flag (parsed as a double)
     * @since 12.0
     */
    public void setStatusFlags(final double statusFlags) {
        this.statusFlags = (int) FastMath.rint(statusFlags);
    }

    /** Set health flag.
     * @param healthFlags health flag (parsed as a double)
     * @since 12.0
     */
    public void setHealthFlags(final double healthFlags) {
        this.healthFlags = Double.isNaN(healthFlags) ? 15 : (int) FastMath.rint(healthFlags);
    }

    /** Get health flags.
     * @return health flags
     * @since 12.0
     */
    public int getHealthFlags() {
        return healthFlags;
    }

    /** Get group delay difference.
     * @return group delay difference
     * @since 12.0
     */
    public double getGroupDelayDifference() {
        return groupDelayDifference;
    }

    /** Set group delay difference.
     * @param groupDelayDifference group delay difference
     * @since 12.0
     */
    public void setGroupDelayDifference(final double groupDelayDifference) {
        this.groupDelayDifference = Double.isNaN(groupDelayDifference) ?
                                    0.999999999999e+09 :
                                    groupDelayDifference;
    }

    /**
     * Getter for the user range accuray (meters).
     * @return the user range accuracy
     * @since 12.0
     */
    public double getURA() {
        return ura;
    }

    /**
     * Setter for the user range accuracy.
     * @param accuracy the value to set
     * @since 12.0
     */
    public void setURA(final double accuracy) {
        this.ura = accuracy;
    }

    /**
     * Check if the acceleration is available in the navigation message.
     * @return true if the acceleration is available
     */
    private boolean isAccAvailable() {
        return getXDotDot() != 0.0 || getYDotDot() != 0.0 || getZDotDot() != 0.0;
    }

}
