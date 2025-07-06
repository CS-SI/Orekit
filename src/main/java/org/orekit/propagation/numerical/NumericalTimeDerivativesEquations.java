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
package org.orekit.propagation.numerical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;

import java.util.Arrays;
import java.util.List;

/**
 * First-order differential model representing the equations of motion.
 *
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.1
 * @see NumericalPropagator
 */
class NumericalTimeDerivativesEquations implements TimeDerivativesEquations {

    /** Derivatives array. */
    private final double[] yDot;

    /** Orbit type of coordinates. */
    private final OrbitType orbitType;

    /** Position angle type of coordinates. */
    private final PositionAngleType positionAngleType;

    /** Forces list. */
    private final List<ForceModel> forceModels;

    /** Jacobian of the orbital parameters with respect to the Cartesian parameters. */
    private double[][] coordinatesJacobian;

    /** Current state. */
    private SpacecraftState currentState;

    /**
     * Constructor.
     * @param orbitType orbit type used in equations
     * @param positionAngleType angle type used in equations if applicable
     * @param forceModels forces
     */
    NumericalTimeDerivativesEquations(final OrbitType orbitType, final PositionAngleType positionAngleType,
                                      final List<ForceModel> forceModels) {
        this.orbitType = orbitType;
        this.positionAngleType = positionAngleType;
        this.forceModels = forceModels;
        this.yDot     = new double[7];
        this.coordinatesJacobian = new double[6][6];
        // default value for Jacobian is identity
        for (int i = 0; i < coordinatesJacobian.length; ++i) {
            Arrays.fill(coordinatesJacobian[i], 0.0);
            coordinatesJacobian[i][i] = 1.0;
        }
    }

    /**
     * Getter for the force models.
     * @return forces
     */
    List<ForceModel> getForceModels() {
        return forceModels;
    }

    /**
     * Setter for the coordinates' Jacobian matrix.
     * @param coordinatesJacobian matrix
     */
    void setCoordinatesJacobian(final double[][] coordinatesJacobian) {
        this.coordinatesJacobian = coordinatesJacobian;
    }

    /**
     * Setter for the current state.
     * @param currentState state
     */
    void setCurrentState(final SpacecraftState currentState) {
        this.currentState = currentState;
    }

    /**
     * Compute first-order, time derivatives a.k.a. rates.
     * @param state state where to evaluate derivatives
     * @return derivatives
     */
    double[] computeTimeDerivatives(final SpacecraftState state) {
        currentState = state;
        Arrays.fill(yDot, 0.0);

        // compute the contributions of all perturbing forces,
        // using the Kepler contribution at the end since
        // NewtonianAttraction is always the last instance in the list
        for (final ForceModel forceModel : forceModels) {
            forceModel.addContribution(state, this);
        }

        if (orbitType == null) {
            // position derivative is velocity, and was not added above in the force models
            // (it is added when orbit type is non-null because NewtonianAttraction considers it)
            final Vector3D velocity = state.getPVCoordinates().getVelocity();
            yDot[0] += velocity.getX();
            yDot[1] += velocity.getY();
            yDot[2] += velocity.getZ();
        }

        return yDot.clone();
    }

    /** {@inheritDoc} */
    @Override
    public void addKeplerContribution(final double mu) {
        if (orbitType == null) {
            // if mu is neither 0 nor NaN, we want to include Newtonian acceleration
            if (mu > 0) {
                // velocity derivative is Newtonian acceleration
                final Vector3D position = currentState.getPosition();
                final double r2         = position.getNormSq();
                final double coeff      = -mu / (r2 * FastMath.sqrt(r2));
                yDot[3] += coeff * position.getX();
                yDot[4] += coeff * position.getY();
                yDot[5] += coeff * position.getZ();
            }

        } else {
            // propagation uses regular orbits
            orbitType.convertType(currentState.getOrbit()).addKeplerContribution(positionAngleType, mu, yDot);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addNonKeplerianAcceleration(final Vector3D gamma) {
        for (int i = 0; i < 6; ++i) {
            final double[] jRow = coordinatesJacobian[i];
            yDot[i] += jRow[3] * gamma.getX() + jRow[4] * gamma.getY() + jRow[5] * gamma.getZ();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addMassDerivative(final double q) {
        if (q > 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
        }
        yDot[6] += q;
    }
}
