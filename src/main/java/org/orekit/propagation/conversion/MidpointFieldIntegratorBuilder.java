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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.nonstiff.MidpointFieldIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;

/**
 * Builder for MidpointFieldIntegrator.
 *
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 * @since 12.0
 * @param <T> type of the field elements
 */
public class MidpointFieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractFixedStepFieldIntegratorBuilder<T, MidpointFieldIntegrator<T>>
        implements FieldExplicitRungeKuttaIntegratorBuilder<T> {

    /**
     * Constructor.
     *
     * @param step step size (s)
     *
     * @see MidpointFieldIntegrator
     */
    public MidpointFieldIntegratorBuilder(final double step) {
        super(step);
    }

    /**
     * Constructor using a "fielded" step.
     * <p>
     * <b>WARNING : Given "fielded" step must be using the same field as the one that will be used when calling
     * {@link #buildIntegrator}</b>
     *
     * @param step step size (s)
     *
     * @see MidpointFieldIntegrator
     */
    public MidpointFieldIntegratorBuilder(final T step) {
        super(step);
    }

    /** {@inheritDoc} */
    @Override
    public MidpointFieldIntegrator<T> buildIntegrator(final Field<T> field, final Orbit orbit,
                                                      final OrbitType orbitType, final PositionAngleType angleType) {
        return new MidpointFieldIntegrator<>(field, getFieldStep(field));
    }

    /** {@inheritDoc} */
    @Override
    public MidpointIntegratorBuilder toODEIntegratorBuilder() {
        return new MidpointIntegratorBuilder(getStep());
    }
}
