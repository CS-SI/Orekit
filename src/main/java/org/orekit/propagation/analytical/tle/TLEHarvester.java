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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.AbstractAnalyticalMatricesHarvester;
import org.orekit.utils.DoubleArrayDictionary;

/** Harvester between two-dimensional Jacobian matrices and
 * one-dimensional {@link TLEPropagator}.
 * @author Thomas Paulet
 * @author Bryan Cazabonne
 * @since 11.1
 */
class TLEHarvester extends AbstractAnalyticalMatricesHarvester {

    /** Propagator bound to this harvester. */
    private final TLEPropagator propagator;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * @param propagator propagator bound to this harvester
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    TLEHarvester(final TLEPropagator propagator, final String stmName,
                 final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        super(propagator, stmName, initialStm, initialJacobianColumns);
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractAnalyticalGradientConverter getGradientConverter() {
        return new TLEGradientConverter(propagator);
    }

}
