/* Copyright 2002-2023 CS GROUP
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

/**
 * Abstract class for integrator using a limited number of variable steps.
 *
 * @param <T> type of the field elements
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractLimitedVariableStepFieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractVariableStepFieldIntegratorBuilder<T> {

    // CHECKSTYLE: stop VisibilityModifier check
    /** Number of steps. */
    protected final int nSteps;
    // CHECKSTYLE: resume VisibilityModifier check

    /**
     * Constructor.
     *
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     * @param nSteps number of steps
     */
    AbstractLimitedVariableStepFieldIntegratorBuilder(final int nSteps, final double minStep,
                                                      final double maxStep, final double dP) {
        super(minStep, maxStep, dP);
        this.nSteps = nSteps;
    }
}
