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
import org.orekit.utils.FieldPVCoordinates;

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
        final double reciprocalScalePosition = 1. / getScalePositionDefects();
        final double reciprocalScaleVelocity = 1. / getScaleVelocityDefects();
        final FieldPVCoordinates<Gradient> terminalPV = fieldTerminalState.getPVCoordinates();
        final FieldVector3D<Gradient> fieldScaledTerminalPosition = terminalPV.getPosition().scalarMultiply(reciprocalScalePosition);
        final FieldVector3D<Gradient> fieldScaledTerminalVelocity = terminalPV.getVelocity().scalarMultiply(reciprocalScaleVelocity);
        final Vector3D terminalScaledPosition = fieldScaledTerminalPosition.toVector3D();
        final Vector3D terminalScaledVelocity = fieldScaledTerminalVelocity.toVector3D();
        final Vector3D targetScaledPosition = getTerminalCartesianState().getPosition().scalarMultiply(reciprocalScalePosition);
        final Vector3D targetScaledVelocity = getTerminalCartesianState().getVelocity().scalarMultiply(reciprocalScaleVelocity);
        defects[0] = terminalScaledPosition.getX() - targetScaledPosition.getX();
        defectsJacobianData[0] = fieldScaledTerminalPosition.getX().getGradient();
        defects[1] = terminalScaledPosition.getY() - targetScaledPosition.getY();
        defectsJacobianData[1] = fieldScaledTerminalPosition.getY().getGradient();
        defects[2] = terminalScaledPosition.getZ() - targetScaledPosition.getZ();
        defectsJacobianData[2] = fieldScaledTerminalPosition.getZ().getGradient();
        defects[3] = terminalScaledVelocity.getX() - targetScaledVelocity.getX();
        defectsJacobianData[3] = fieldScaledTerminalVelocity.getX().getGradient();
        defects[4] = terminalScaledVelocity.getY() - targetScaledVelocity.getY();
        defectsJacobianData[4] = fieldScaledTerminalVelocity.getY().getGradient();
        defects[5] = terminalScaledVelocity.getZ() - targetScaledVelocity.getZ();
        defectsJacobianData[5] = fieldScaledTerminalVelocity.getZ().getGradient();
        if (originalInitialAdjoint.length != 6) {
            final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
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
