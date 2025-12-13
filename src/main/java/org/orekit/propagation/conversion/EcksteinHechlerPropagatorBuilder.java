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
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.AbstractOrbitFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;

/** Builder for Eckstein-Hechler propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class EcksteinHechlerPropagatorBuilder
    extends AbstractAnalyticalPropagatorBuilder<EcksteinHechlerPropagator, Orbit, AbstractOrbitFactory<Orbit>> {

    /** Provider for un-normalized coefficients. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param provider for un-normalized zonal coefficients
     * @since 14.0
     * @see #EcksteinHechlerPropagatorBuilder(AbstractOrbitFactory,
     * UnnormalizedSphericalHarmonicsProvider, AttitudeProvider)
     */
    public EcksteinHechlerPropagatorBuilder(final AbstractOrbitFactory<? extends Orbit> factory,
                                            final UnnormalizedSphericalHarmonicsProvider provider) {
        this(factory, provider, FrameAlignedProvider.of(factory.getFrame()));
    }

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param provider for un-normalized zonal coefficients
     * @param attitudeProvider attitude law to use.
     * @since 14.0
     */
    public EcksteinHechlerPropagatorBuilder(final AbstractOrbitFactory<? extends Orbit> factory,
                                            final UnnormalizedSphericalHarmonicsProvider provider,
                                            final AttitudeProvider attitudeProvider) {
        super((AbstractOrbitFactory<Orbit>) factory, true, attitudeProvider, Propagator.DEFAULT_MASS);
        factory.setMu(provider.getMu());
        this.provider = provider;
    }

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param tideSystem tide system
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @since 14.0
     * @see #EcksteinHechlerPropagatorBuilder(AbstractOrbitFactory,
     * UnnormalizedSphericalHarmonicsProvider, AttitudeProvider)
     */
    public EcksteinHechlerPropagatorBuilder(final AbstractOrbitFactory<? extends Orbit> factory,
                                            final double referenceRadius,
                                            final double mu,
                                            final TideSystem tideSystem,
                                            final double c20,
                                            final double c30,
                                            final double c40,
                                            final double c50,
                                            final double c60) {
        this(factory,
             GravityFieldFactory.getUnnormalizedProvider(referenceRadius, mu, tideSystem,
                                                         new double[][] {
                                                             {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 c20
                                                             }, {
                                                                 c30
                                                             }, {
                                                                 c40
                                                             }, {
                                                                 c50
                                                             }, {
                                                                 c60
                                                             }
                                                         }, new double[][] {
                                                             {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }
                                                         }));
    }

    /** {@inheritDoc}. */
    @Override
    public EcksteinHechlerPropagatorBuilder clone() {
        // Call to super clone() method to avoid warning
        final EcksteinHechlerPropagatorBuilder clonedBuilder = (EcksteinHechlerPropagatorBuilder) super.clone();

        // Use cloned builder to unlink orbital drivers
        final EcksteinHechlerPropagatorBuilder builder =
            new EcksteinHechlerPropagatorBuilder((AbstractOrbitFactory<? extends Orbit>) clonedBuilder.getOrbitalParameterFactory().clone(),
                                                 clonedBuilder.provider,
                                                 clonedBuilder.getAttitudeProvider());

        // Set mass
        builder.setMass(getMass());

        // Return cloned builder
        return builder;
    }

    /** {@inheritDoc} */
    public EcksteinHechlerPropagator buildPropagator(final double[] normalizedParameters) {
        setParameters(normalizedParameters);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(getOrbitalParameterFactory().createFromDrivers(),
                                          getAttitudeProvider(), getMass(), provider);
        getImpulseManeuvers().forEach(propagator::addEventDetector);
        return propagator;
    }

}
