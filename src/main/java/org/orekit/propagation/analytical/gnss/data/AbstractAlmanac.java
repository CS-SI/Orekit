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
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

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
     * @param type            type (null if not a navigation message)
     */
    protected AbstractAlmanac(final double mu, final double angularVelocity, final int weeksInCycle,
                              final TimeScales timeScales, final SatelliteSystem system,
                              final String type) {
        super(mu, angularVelocity, weeksInCycle, timeScales, system, type);
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

    /** Get the propagator corresponding to the navigation message.
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
    public GNSSPropagator<O> getPropagator(final Frame inertial, final Frame bodyFixed) {
        return getPropagator(new FrameAlignedProvider(inertial), inertial, bodyFixed, Propagator.DEFAULT_MASS);
    }

    /** Get the propagator corresponding to the navigation message.
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(Frame, Frame)
     * @since 14.0
     */
    public GNSSPropagator<O> getPropagator(final AttitudeProvider provider,
                                           final Frame inertial, final Frame bodyFixed, final double mass) {

        // message-specific configuration
        final GNSSPropagatorBuilder<O> builder = builder(inertial, bodyFixed);

        // generic orbital parameters
        final ParameterDriversList oDrivers = builder.getOrbitalParameterFactory().getOrbitalParametersDrivers();
        oDrivers.findByName(GNSSOrbitalElements.SEMI_MAJOR_AXIS).setValue(getSma());
        oDrivers.findByName(GNSSOrbitalElements.ECCENTRICITY).setValue(getE());
        oDrivers.findByName(GNSSOrbitalElements.INCLINATION).setValue(getI0());
        oDrivers.findByName(GNSSOrbitalElements.ARGUMENT_OF_PERIGEE).setValue(getPa());
        oDrivers.findByName(GNSSOrbitalElements.NODE_LONGITUDE).setValue(getOmega0());
        oDrivers.findByName(GNSSOrbitalElements.MEAN_ANOMALY).setValue(getM0());
        for (final ParameterDriver driver : oDrivers.getDrivers()) {
            driver.setSelected(true);
        }

        // propagation parameters
        final ParameterDriversList pDrivers = builder.getPropagationParametersDrivers();
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.TIME).setValue(getTime());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.A_DOT).setValue(getADot());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.DELTA_N0).setValue(getDeltaN0());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.DELTA_N0_DOT).setValue(getDeltaN0Dot());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.INCLINATION_RATE).setValue(getIDot());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.LONGITUDE_RATE).setValue(getOmegaDot());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.LATITUDE_COSINE).setValue(getCuc());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.LATITUDE_SINE).setValue(getCus());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.RADIUS_COSINE).setValue(getCrc());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.RADIUS_SINE).setValue(getCrs());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.INCLINATION_COSINE).setValue(getCic());
        pDrivers.findByName(GNSSOrbitalElementsDriversProvider.INCLINATION_SINE).setValue(getCis());
        for (final ParameterDriver driver : pDrivers.getDrivers()) {
            driver.setSelected(true);
        }

        builder.setAttitudeProvider(provider);
        builder.setMass(mass);

        final GNSSPropagator<O> propagator = builder.buildPropagator();

        // set up non-propagation elements
        propagator.getOrbitalElements().copyNonKeplerian(this);

        return propagator;

    }

    /** Build the propagator builder.
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @return propagator builder
     * @since 14.0
     */
    public abstract GNSSPropagatorBuilder<O> builder(Frame inertial, Frame bodyFixed);

}
