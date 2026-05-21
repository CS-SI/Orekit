/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.time.TimeScales;

/**
 * Base class for GNSS almanacs.
 * @param <O> type of the orbital elements
 * @author Pascal Parraud
 * @since 11.0
 */
public abstract class AbstractAlmanac<O extends AbstractAlmanac<O>> extends CommonGnssData<O> {

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav, weeks
     *                        are always according to GPS)
     */
    protected AbstractAlmanac(final double mu, final double angularVelocity, final int weeksInCycle,
                              final TimeScales timeScales, final SatelliteSystem system) {
        super(mu, angularVelocity, weeksInCycle, timeScales, system);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends AbstractAlmanac<A>> AbstractAlmanac(final FieldAbstractAlmanac<T, A> original) {
        super(original);
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the inertialframe.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.
     * </p>
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(AttitudeProvider, Frame, Frame, double)
     * @since 14.0
     */
    public GNSSPropagator getPropagator(final Frame inertial, final Frame bodyFixed) {
        return getPropagator(new FrameAlignedProvider(inertial), inertial, bodyFixed, Propagator.DEFAULT_MASS);
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(Frame, Frame)
     * @since 14.0
     */
    public GNSSPropagator getPropagator(final AttitudeProvider provider,
                                        final Frame inertial, final Frame bodyFixed, final double mass) {
        final GNSSPropagatorBuilder builder = new GNSSPropagatorBuilder(this, inertial, bodyFixed);
        builder.setAttitudeProvider(provider);
        builder.setMass(mass);
        return builder.buildPropagator();
    }

}
