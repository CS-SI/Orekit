/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.conversion.ClassicalRungeKuttaFieldIntegratorBuilder;
import org.orekit.propagation.conversion.ClassicalRungeKuttaIntegratorBuilder;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;

/**
 * Integration settings using the classical Runge-Kutta 4 scheme.
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator
 */
public class ClassicalRungeKuttaIntegrationSettings implements ShootingIntegrationSettings {

    /** Step-size for integrator builders. */
    private final double step;

    /**
     * Constructor.
     * @param step step-size for integrator builder
     */
    public ClassicalRungeKuttaIntegrationSettings(final double step) {
        this.step = step;
    }

    /** {@inheritDoc} */
    @Override
    public ODEIntegratorBuilder getIntegratorBuilder() {
        return new ClassicalRungeKuttaIntegratorBuilder(step);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldODEIntegratorBuilder<T> getFieldIntegratorBuilder(final Field<T> field) {
        return new ClassicalRungeKuttaFieldIntegratorBuilder<>(field.getZero().newInstance(step));
    }
}
