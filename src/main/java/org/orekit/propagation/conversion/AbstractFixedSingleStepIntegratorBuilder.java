/* Copyright 2022-2025 Romain Serra
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

import org.hipparchus.ode.nonstiff.RungeKuttaIntegrator;

/**
 * Abstract class for fixed-step, single-step integrator builder.
 *
 * @param <T> field type
 * @see org.hipparchus.ode.nonstiff.RungeKuttaIntegrator
 * @since 13.0
 * @author Romain Serra
 */
public abstract class AbstractFixedSingleStepIntegratorBuilder<T extends RungeKuttaIntegrator>
        extends AbstractIntegratorBuilder<T> implements ExplicitRungeKuttaIntegratorBuilder {

    /** Default step-size. */
    private final double step;

    /**
     * Constructor.
     * @param step default step-size
     */
    protected AbstractFixedSingleStepIntegratorBuilder(final double step) {
        this.step = step;
    }

    /**
     * Getter for the step size.
     * @return step
     */
    public double getStep() {
        return step;
    }
}
