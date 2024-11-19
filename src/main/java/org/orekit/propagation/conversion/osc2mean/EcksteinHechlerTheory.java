/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.Field;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Eckstein-Hechler theory for osculating to mean orbit conversion.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public class EcksteinHechlerTheory implements MeanTheory {

    /** Theory used for converting from osculating to mean orbit. */
    private static final String THEORY = "Eckstein-Hechler";

    /** Unnormalized spherical harmonics provider. */
    private UnnormalizedSphericalHarmonicsProvider provider;

    /**
     * Constructor.
     * @param provider unnormalized spherical harmonics provider
     */
    public EcksteinHechlerTheory(final UnnormalizedSphericalHarmonicsProvider provider) {
        this.provider = provider;
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
        return propagator.propagateOrbit(mean.getDate());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> meanToOsculating(final FieldOrbit<T> mean) {

        final FieldAbsoluteDate<T> date = mean.getDate();
        final Field<T> field = date.getField();

        final FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<>(mean, provider, PropagationType.MEAN);
        return propagator.propagateOrbit(date, propagator.getParameters(field, date));
    }
}
