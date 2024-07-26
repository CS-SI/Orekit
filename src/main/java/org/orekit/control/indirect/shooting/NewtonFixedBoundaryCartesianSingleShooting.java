/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.shooting;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.propagation.FieldSpacecraftState;

/**
 * Class for indirect single shooting methods with Cartesian coordinates for fixed time fixed boundary.
 * Update is the classical Newton-Raphson one.
 *
 * @author Romain Serra
 * @since 12.2
 */
public class NewtonFixedBoundaryCartesianSingleShooting extends AbstractFixedBoundaryCartesianSingleShooting {

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param boundaryConditions boundary conditions as {@link FixedTimeCartesianBoundaryStates}
     * @param convergenceChecker convergence checker
     */
    public NewtonFixedBoundaryCartesianSingleShooting(final ShootingPropagationSettings propagationSettings,
                                                      final FixedTimeCartesianBoundaryStates boundaryConditions,
                                                      final CartesianBoundaryConditionChecker convergenceChecker) {
        super(propagationSettings, boundaryConditions, convergenceChecker);
    }

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param boundaryConditions boundary conditions as {@link FixedTimeBoundaryOrbits}
     * @param convergenceChecker convergence checker
     */
    public NewtonFixedBoundaryCartesianSingleShooting(final ShootingPropagationSettings propagationSettings,
                                                      final FixedTimeBoundaryOrbits boundaryConditions,
                                                      final CartesianBoundaryConditionChecker convergenceChecker) {
        super(propagationSettings, boundaryConditions, convergenceChecker);
    }

    /** {@inheritDoc} */
    @Override
    protected double[] updateAdjoint(final double[] originalInitialAdjoint,
                                     final FieldSpacecraftState<Gradient> fieldTerminalState) {
        // form defects and their Jacobian matrix
        final double[] defects = new double[originalInitialAdjoint.length];
        final double[][] defectsJacobianData = new double[defects.length][defects.length];
        final FieldVector3D<Gradient> fieldTerminalPosition = fieldTerminalState.getPVCoordinates().getPosition();
        final FieldVector3D<Gradient> fieldTerminalVelocity = fieldTerminalState.getPVCoordinates().getVelocity();
        final Vector3D terminalPosition = fieldTerminalPosition.toVector3D();
        final Vector3D terminalVelocity = fieldTerminalVelocity.toVector3D();
        final Vector3D targetPosition = getTerminalCartesianState().getPosition();
        final Vector3D targetVelocity = getTerminalCartesianState().getVelocity();
        defects[0] = terminalPosition.getX() - targetPosition.getX();
        defectsJacobianData[0] = fieldTerminalPosition.getX().getGradient();
        defects[1] = terminalPosition.getY() - targetPosition.getY();
        defectsJacobianData[1] = fieldTerminalPosition.getY().getGradient();
        defects[2] = terminalPosition.getZ() - targetPosition.getZ();
        defectsJacobianData[2] = fieldTerminalPosition.getZ().getGradient();
        defects[3] = terminalVelocity.getX() - targetVelocity.getX();
        defectsJacobianData[3] = fieldTerminalVelocity.getX().getGradient();
        defects[4] = terminalVelocity.getY() - targetVelocity.getY();
        defectsJacobianData[4] = fieldTerminalVelocity.getY().getGradient();
        defects[5] = terminalVelocity.getZ() - targetVelocity.getZ();
        defectsJacobianData[5] = fieldTerminalVelocity.getZ().getGradient();
        if (originalInitialAdjoint.length != 6) {
            final String adjointName = getPropagationSettings().getAdjointDerivativesProvider()
                .buildFieldAdditionalDerivativesProvider(fieldTerminalPosition.getX().getField()).getName();
            final Gradient terminalMassAdjoint = fieldTerminalState.getAdditionalState(adjointName)[6];
            defects[6] = terminalMassAdjoint.getValue();
            defectsJacobianData[6] = terminalMassAdjoint.getGradient();
        }
        // apply Newton's formula
        final double[] correction = computeCorrection(defects, defectsJacobianData);
        final double[] correctedAdjoint = originalInitialAdjoint.clone();
        for (int i = 0; i < correction.length; i++) {
            correctedAdjoint[i] += correction[i];
        }
        return correctedAdjoint;
    }

    /**
     * Compute Newton-Raphson correction.
     * @param defects defects
     * @param defectsJacobianData Jacobian matrix of defects w.r.t. shooting variables
     * @return correction to shooting variables
     */
    private double[] computeCorrection(final double[] defects, final double[][] defectsJacobianData) {
        final RealMatrix defectsJacobian = MatrixUtils.createRealMatrix(defectsJacobianData);
        final DecompositionSolver solver = new LUDecomposition(defectsJacobian).getSolver();
        final RealVector negatedDefects = MatrixUtils.createRealVector(defects).mapMultiply(-1);
        return solver.solve(negatedDefects).toArray();
    }
}
