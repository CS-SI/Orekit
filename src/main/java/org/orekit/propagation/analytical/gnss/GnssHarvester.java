/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.AbstractAnalyticalMatricesHarvester;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.utils.DoubleArrayDictionary;

/**
 * Harvester between two-dimensional Jacobian matrices and
 * one-dimensional {@link GNSSPropagator}.
 *
 * @param <O> type of the orbital elements
 * @author Luc Maisonobe
 * @since 13.0
 */
class GnssHarvester<O extends GNSSOrbitalElements<O>> extends AbstractAnalyticalMatricesHarvester {

    /** Propagator bound to this harvester. */
    private final GNSSPropagator<O> propagator;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * @param propagator propagator bound to this harvester
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂I₀,
     *                   if null (which is the most frequent case), assumed to be just the
     *                   conversion from orbit type I to orbit type Y at t₀
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    GnssHarvester(final GNSSPropagator<O> propagator, final String stmName,
                  final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        super(propagator);
        this.propagator = propagator;
        setInitialStm(stmName, initialStm);
        setInitialJacobianColumns(initialJacobianColumns);
    }

    /** {@inheritDoc}
     * <p>
     * As the propagated state is Cartesian for GNSS propagators, this is the Jacobian of the
     * Cartesian coordinates with respect to the GNSS orbital elements representing the state.
     * </p>
     * <p>
     * The elements this implementation differentiates are the ones held by the propagator,
     * hence the returned Jacobian is currently only valid for the propagator initial state.
     * </p>
     */
    @Override
    public RealMatrix getStateJacobianVsBuilderParameters(final SpacecraftState state) {
        return GNSSOrbitalElementsFactory.jacobianWrtParameters(propagator.getOrbitalElements(),
                                                                propagator.getFrame(),
                                                                propagator.getECEF());
    }

    /** {@inheritDoc} */
    @Override
    public AbstractAnalyticalGradientConverter getGradientConverter() {
        return new GnssGradientConverter<>(propagator);
    }

}
