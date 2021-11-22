/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.numerical;

import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 */
public class MatricesMapper implements MatricesHarvester {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = 6;

    /** Columns names for parameters. */
    private final List<String> columns;

    /** Name. */
    private String name;

    /** Orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Simple constructor.
     * @param name name of the State Transition Matrix additional state
     * @param columns names of the parameters for Jacobian colimns
     * @param orbitType orbit type
     * @param angleType position angle type
     */
    MatricesMapper(final String name, final List<String> columns,
                   final OrbitType orbitType, final PositionAngle angleType) {
        this.orbitType = orbitType;
        this.angleType = angleType;
        this.columns   = columns;
        this.name      = name;
    }

    /** Get the conversion Jacobian between state parameters and parameters used for derivatives.
     * <p>
     * For DSST and TLE propagators, state parameters and parameters used for derivatives are the same,
     * so the Jocabian is simply the identity.
     * </p>
     * <p>
     * For Numerical propagator, parameters used for derivatives are cartesian
     * and they can be different from state parameters because the numerical propagator can accept different type
     * of orbits.
     * </p>
     * @param state spacecraft state
     * @return conversion Jacobian
     */
    private double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];

        // make sure the state is in the desired orbit type
        final Orbit orbit = orbitType.convertType(state.getOrbit());

        // compute the Jacobian, taking the position angle type into account
        orbit.getJacobianWrtCartesian(angleType, dYdC);

        return dYdC;

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState state) {

        // get the conversion Jacobian
        final double[][] dYdC = getConversionJacobian(state);

        // extract the additional state
        final double[] p = state.getAdditionalState(name);

        // compute dYdY0 = dYdC * dCdY0
        final RealMatrix  dYdY0 = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        for (int i = 0; i < STATE_DIMENSION; i++) {
            final double[] rowC = dYdC[i];
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                double sum = 0;
                int pIndex = j;
                for (int k = 0; k < STATE_DIMENSION; ++k) {
                    sum += rowC[k] * p[pIndex];
                    pIndex += STATE_DIMENSION;
                }
                dYdY0.setEntry(i, j, sum);
            }
        }

        return dYdY0;

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState state) {

        if (columns.isEmpty()) {
            return null;
        }

        // get the conversion Jacobian
        final double[][] dYdC = getConversionJacobian(state);

        // compute dYdP = dYdC * dCdP
        final RealMatrix dYdP = MatrixUtils.createRealMatrix(STATE_DIMENSION, columns.size());
        for (int j = 0; j < columns.size(); j++) {
            final double[] p = state.getAdditionalState(columns.get(j));
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                final double[] dYdCi = dYdC[i];
                double sum = 0;
                for (int k = 0; k < STATE_DIMENSION; ++k) {
                    sum += dYdCi[k] * p[k];
                }
                dYdP.setEntry(i, j, sum);
            }
        }

        return dYdP;

    }

}
