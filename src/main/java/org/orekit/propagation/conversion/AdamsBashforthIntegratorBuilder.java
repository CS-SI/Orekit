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

import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.orekit.propagation.ToleranceProvider;

/** Builder for AdamsBashforthIntegrator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class AdamsBashforthIntegratorBuilder extends AbstractVariableStepIntegratorBuilder {

    /** Number of steps. */
    private final int nSteps;

    /** Build a new instance using default integration tolerances.
     * @param nSteps number of steps
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param dP position error (m)
     * @see AdamsBashforthIntegrator
     */
    public AdamsBashforthIntegratorBuilder(final int nSteps, final double minStep,
                                           final double maxStep, final double dP) {
        super(minStep, maxStep, getDefaultToleranceProvider(dP));
        this.nSteps  = nSteps;
    }

    /** Build a new instance.
     * @param nSteps number of steps
     * @param minStep minimum step size (s)
     * @param maxStep maximum step size (s)
     * @param toleranceProvider integration tolerance provider
     *
     * @since 13.0
     * @see AdamsBashforthIntegrator
     */
    public AdamsBashforthIntegratorBuilder(final int nSteps, final double minStep,
                                           final double maxStep, final ToleranceProvider toleranceProvider) {
        super(minStep, maxStep, toleranceProvider);
        this.nSteps  = nSteps;
    }

    /** {@inheritDoc} */
    @Override
    protected AdamsBashforthIntegrator buildIntegrator(final double[][] tolerances) {
        return new AdamsBashforthIntegrator(nSteps, getMinStep(), getMaxStep(), tolerances[0], tolerances[1]);
    }

}
