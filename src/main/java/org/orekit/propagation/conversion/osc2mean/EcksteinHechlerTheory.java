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
package org.orekit.propagation.conversion.osc2mean;

import org.hipparchus.CalculusFieldElement;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;

/**
 * Eckstein-Hechler theory for osculating to mean orbit conversion.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public class EcksteinHechlerTheory implements MeanTheory {

    /** Theory used for converting from osculating to mean orbit. */
    public static final String THEORY = "Eckstein-Hechler";

    /** Unnormalized spherical harmonics provider. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /**
     * Constructor.
     * @param provider unnormalized spherical harmonics provider
     */
    public EcksteinHechlerTheory(final UnnormalizedSphericalHarmonicsProvider provider) {
        this.provider = provider;
    }

    /**
     * Constructor.
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     */
    public EcksteinHechlerTheory(final double referenceRadius,
                                 final double mu,
                                 final double c20,
                                 final double c30,
                                 final double c40,
                                 final double c50,
                                 final double c60) {
        this(GravityFieldFactory.getUnnormalizedProvider(referenceRadius, mu,
                                                         TideSystem.UNKNOWN,
                                                         new double[][] { { 0 }, { 0 }, { c20 }, { c30 }, { c40 }, { c50 }, { c60 } },
                                                         new double[][] { { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 } }));
    }

    /** {@inheritDoc} */
    @Override
    public String getTheoryName() {
        return THEORY;
    }

    /** {@inheritDoc} */
    @Override
    public double getReferenceRadius() {
        return provider.getAe();
    };

    /** {@inheritDoc} */
    @Override
    public Orbit meanToOsculating(final Orbit mean) {
        final EcksteinHechlerPropagator propagator =
                        new EcksteinHechlerPropagator(mean, provider, PropagationType.MEAN);
        return propagator.getOsculatingCircularOrbit(mean.getDate());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> meanToOsculating(final FieldOrbit<T> mean) {
        final FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<>(mean, provider, PropagationType.MEAN);
        return propagator.getOsculatingCircularOrbit(mean.getDate());
    }

    /** Post-treatment of the converted mean orbit.
     * <p>The mean orbit returned is circular.</p>
     * @param osculating the osculating orbit to be converted
     * @param mean the converted mean orbit
     * @return postprocessed mean orbit
     */
    @Override
    public Orbit postprocessing(final Orbit osculating, final Orbit mean) {
        return OrbitType.CIRCULAR.convertType(mean);
    }

    /** Post-treatment of the converted mean orbit.
     * <p>The mean orbit returned is circular.</p>
     * @param <T> type of the field elements
     * @param osculating the osculating orbit to be converted
     * @param mean the converted mean orbit
     * @return postprocessed mean orbit
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> postprocessing(final FieldOrbit<T> osculating,
                                                                            final FieldOrbit<T> mean) {
        return OrbitType.CIRCULAR.convertType(mean);
    }
}
