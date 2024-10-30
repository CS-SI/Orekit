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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.ToleranceProvider;

/**
 * Abstract class for integrator using a limited number of variable steps.
 *
 * @param <T> type of the field elements
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractLimitedVariableStepFieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractVariableStepFieldIntegratorBuilder<T> {

    /** Number of steps. */
    private final int nSteps;

    /**
     * Constructor.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param toleranceProvider integration tolerance provider
     * @param nSteps number of steps
     * @since 13.0
     */
    protected AbstractLimitedVariableStepFieldIntegratorBuilder(final int nSteps, final double minStep,
                                                      final double maxStep, final ToleranceProvider toleranceProvider) {
        super(minStep, maxStep, toleranceProvider);
        this.nSteps = nSteps;
    }

    /**
     * Getter for number of steps.
     * @return nSteps
     * @since 13.0
     */
    protected int getnSteps() {
        return nSteps;
    }
}
