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

import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;

/** Builder for EulerIntegrator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class EulerIntegratorBuilder extends AbstractFixedSingleStepIntegratorBuilder<EulerIntegrator> {

    /** Build a new instance.
     * @param step step size (s)
     * @see EulerIntegrator
     */
    public EulerIntegratorBuilder(final double step) {
        super(step);
    }

    /** {@inheritDoc} */
    @Override
    public EulerIntegrator buildIntegrator(final Orbit orbit, final OrbitType orbitType,
                                           final PositionAngleType angleType) {
        return new EulerIntegrator(getStep());
    }

}
