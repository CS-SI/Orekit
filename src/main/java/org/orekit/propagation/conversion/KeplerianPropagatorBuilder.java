/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.conversion;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.orbits.AbstractOrbitFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitalParameterFactory;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;


/** Builder for Keplerian propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class KeplerianPropagatorBuilder
    extends AbstractAnalyticalPropagatorBuilder<KeplerianPropagator, Orbit, AbstractOrbitFactory<Orbit>> {

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @since 14.0
     * @see #KeplerianPropagatorBuilder(OrbitalParameterFactory, AttitudeProvider)
     */
    public KeplerianPropagatorBuilder(final OrbitalParameterFactory<? extends Orbit> factory) {
        this(factory, FrameAlignedProvider.of(factory.getFrame()));
    }

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param attitudeProvider attitude law to use.
     * @since 10.1
     */
    public KeplerianPropagatorBuilder(final OrbitalParameterFactory<? extends Orbit> factory,
                                      final AttitudeProvider attitudeProvider) {
        super((AbstractOrbitFactory<Orbit>) factory, true, attitudeProvider, Propagator.DEFAULT_MASS);
    }

    /** Copy constructor.
     * @param builder builder to copy from
     */
    private KeplerianPropagatorBuilder(final KeplerianPropagatorBuilder builder) {
        this(builder.getOrbitalParameterFactory(), builder.getAttitudeProvider());
    }

    /** {@inheritDoc}. */
    @Override
    public KeplerianPropagatorBuilder clone() {
        // Call to super clone() method to avoid warning
        final KeplerianPropagatorBuilder clonedBuilder = (KeplerianPropagatorBuilder) super.clone();

        // Use copy constructor to unlink orbital drivers
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(clonedBuilder);

        // Set mass
        builder.setMass(getMass());

        // Return cloned builder
        return builder;
    }

    /** {@inheritDoc} */
    public KeplerianPropagator buildPropagator(final double[] normalizedParameters) {
        setParameters(normalizedParameters);
        final KeplerianPropagator propagator =
            new KeplerianPropagator(getOrbitalParameterFactory().createFromDrivers(),
                                    getAttitudeProvider(), getOrbitalParameterFactory().getMu(),
                                    getMass());
        getImpulseManeuvers().forEach(propagator::addEventDetector);
        return propagator;
    }

}
