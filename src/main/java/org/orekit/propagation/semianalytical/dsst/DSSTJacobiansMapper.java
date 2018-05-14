/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst;

import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see org.orekit.propagation.semianalytical.DSSTPartialDerivativesEquations
 * @see org.orekit.propagation.semianalytical.DSSTPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class DSSTJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6.
     * @since 9.0
     */
    public static final int STATE_DIMENSION = 6;

    /** Orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param orbitType orbit type
     * @param angleType position angle type
     */
    DSSTJacobiansMapper(final String name, final ParameterDriversList parameters,
                    final OrbitType orbitType, final PositionAngle angleType) {

        super(name, parameters);
        this.orbitType  = orbitType;
        this.angleType  = angleType;
    }

    /** Get the conversion Jacobian between state parameters and Cartesian parameters.
     * @param state spacecraft state
     * @return conversion Jacobian
     */
    protected double[][] getJacobianConversion(final SpacecraftState state) {

        final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];

        // make sure the state is in the desired orbit type
        final Orbit orbit = orbitType.convertType(state.getOrbit());

        // compute the Jacobian, taking the position angle type into account
        orbit.getJacobianWrtCartesian(angleType, dYdC);

        return dYdC;

    }

}
