/* Copyright 2002-2019 CS Systèmes d'Information
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

package org.orekit.utils;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.time.AbsoluteDate;


/**
 * Multiple shooting applicable for either propagation in CR3BP, or in an ephemeris model.
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 */
public class MultipleShooting {

    /** Patch points along the trajectory. */
    private List<SpacecraftState> patchedSpacecraftStates;

    /** Derivatives linked to the Propagators. */
    private final List<AdditionalEquations> additionalEquations;

    /** True if CR3BP is considered (useful for the derivatives computation). */
    private final boolean cr3bp;

    /** List of Propagators. */
    private final List<NumericalPropagator> propagatorList;

    /** Duration of propagation along each arcs. */
    private double[] propagationTime;

    /** True if we consider epoch. */
    private boolean isEpochDependent;

    /** Simple Constructor.
     * <p> Standard constructor using DormandPrince853 integrator for the differential correction </p>
     * @param initialGuessList first guess PVCoordinates of the point to start differential correction
     * @param propagatorList .
     * @param additionalEquations .
     * @param cr3bp CR3BP System considered .
     */
    public MultipleShooting(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                            final List<AdditionalEquations> additionalEquations, final boolean cr3bp) {
        this.patchedSpacecraftStates = initialGuessList;
        this.propagatorList = propagatorList;
        this.additionalEquations = additionalEquations;
        this.cr3bp = cr3bp;

        final int propagationNumber = initialGuessList.size() - 1;
        this.propagationTime = new double[propagationNumber];
        this.isEpochDependent = false;
        for (int i = 0; i < propagationNumber; i++ ) {
            this.propagationTime[i] = patchedSpacecraftStates.get(i + 1).getDate().durationFrom(initialGuessList.get(i).getDate());
        }
    }

    /** Return the list of corrected patch points.
     *  An optimizer is better suited for this problem
     * @return patchedSpacecraftStates patchedSpacecraftStates
     */
    public List<SpacecraftState> compute() {
        int iter = 0; // number of iteration

        final int n = patchedSpacecraftStates.size();

        double fxNorm = 0;

        do {
//            System.out.println("Itération " + (iter + 1));
//            System.out.println(Arrays.toString(propagationTime));

            final List<SpacecraftState> propagatedSP = propagatePatchedSpacecraftState(); // multi threading see PropagatorsParallelizer
            final RealMatrix M = computeJacobianMatrix(propagatedSP);
            final RealVector fx = computeConstraint(propagatedSP);

//            System.out.println("DF(X)");
//            for (int i = 0; i < M.getRowDimension(); i++) {
//                System.out.println(Arrays.toString(M.getRow(i)));
//            }
//            System.out.println("F(X)");
//            System.out.println(Arrays.toString(fx.toArray()));

            // Solve linear system
            final RealVector B = M.transpose().operate(fx);
            final RealMatrix MtM = M.transpose().multiply(M);

            // Numerical trouble with Cholesky for matrix decomposition
            //final MatrixDecomposer decomposer = new CholeskyDecomposer(1.0e-15, 1.0e-15);
            // final RealVector dx = decomposer.decompose(MtM).solve(B);
            final RealVector dx = MatrixUtils.inverse(MtM).operate(B);

            fxNorm = fx.getNorm() / fx.getDimension();
//            System.out.println("DF(X)^T*DF(X)");
//            System.out.println(Arrays.deepToString(MtM.getData()));
//
//            System.out.println("DeltaX");
//            System.out.println(Arrays.toString(dx.toArray()));
//
//            System.out.println("||F(X)|| = " + fxNorm);

            // Apply correction from the free variable vector to all the variables (propagation time, pacthSpaceraftState)
            updateTrajectory(dx);
            propagationTime = MatrixUtils.createRealVector(propagationTime).subtract(dx.getSubVector(6 * n, n - 1)).toArray();

            iter++;

        } while (fxNorm > 1E-7 & iter < 30); // Converge within 1E-8 tolerance and under 5 iterations

        return patchedSpacecraftStates;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @param propagatedSP propagatedSP
     *  @return patchedSpacecraftStates patchedSpacecraftStates
     */
    private RealMatrix computeJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final int n = patchedSpacecraftStates.size();
        int nrows = 6 * (n - 1);
        int ncolumns = 7 * n - 1;
        if (isEpochDependent) {
            nrows = nrows + n - 1; //7*n-1;
            ncolumns = ncolumns + n; //8*n-1;
        }

        final RealMatrix M = MatrixUtils.createRealMatrix(nrows, ncolumns);
        final RealMatrix minusIsix = MatrixUtils.createRealIdentityMatrix(6).scalarMultiply(-1);

        for (int i = 0; i < n - 1; i++) {

            final SpacecraftState finalState = propagatedSP.get(i);
            // The Jacobian matrix have the following form:

            //          [     |     |     ]
            //          [  A  |  B  |  C  ]
            // DF(X) =  [     |     |     ]
            //          [-----------------]
            //          [  0  | -I  |  D  ]
            //
            //      [ phi1     -I                                   ]
            //      [         phi2     -I                           ]
            // A =  [                 ....    ....                  ]   6(n-1)x6n
            //      [                         ....     ....         ]
            //      [                                 phin-1    -I  ]
            //
            //      [ xdot1f                                ]
            //      [        xdot2f                         ]
            // B =  [                 ....                  ]   6(n-1)x(n-1)
            //      [                        ....           ]
            //      [                             xdotn-1f  ]
            //
            //      [ -dx1f/dtau1                                           0 ]
            //      [          -dx2f/dtau2                                  0 ]
            // C =  [                       ....                            0 ]   6(n-1)xn
            //      [                               ....                    0 ]
            //      [                                     -dxn-1f/dtaun-1   0 ]
            //
            //      [ -1   1  0                 ]
            //      [     -1   1  0             ]
            // D =  [          ..   ..          ] n-1xn
            //      [               ..   ..     ]
            //      [                    -1   1 ]

            final PVCoordinates pv = finalState.getPVCoordinates();

            // Get State Transition Matrix phi
            final RealMatrix phi = getStateTransitionMatrix(finalState);

            M.setSubMatrix(phi.getData(), 6 * i, 6 * i);
            M.setSubMatrix(minusIsix.getData(), 6 * i, 6 * (i + 1));

            if (isEpochDependent) {
                //M.setSubMatrix(new double[][] {"dx/dtau", 6*i,  7*n + i - 1);
                M.setEntry(6 * (n - 1) + i, 6 * n + i, -1);
                M.setEntry(6 * (n - 1) + i, 7 * n + i - 1, -1);
                M.setEntry(6 * (n - 1) + i, 7 * n + i, 1);
            }

            final double[][] pvArray = new double[][] {
                {pv.getVelocity().getX()},
                {pv.getVelocity().getY()},
                {pv.getVelocity().getZ()},
                {pv.getAcceleration().getX()},
                {pv.getAcceleration().getY()},
                {pv.getAcceleration().getZ()}};

            M.setSubMatrix(pvArray, 6 * i, 6 * n + i);

        }
        return M;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @return propagatedSP propagated SpacecraftStates
     */
    private List<SpacecraftState> propagatePatchedSpacecraftState() {

        final int n = patchedSpacecraftStates.size();

        final ArrayList<SpacecraftState> propagatedSP = new ArrayList<SpacecraftState>(n);

        for (int i = 0; i < n - 1; i++) {

            // SpacecraftState initialization
            final SpacecraftState initialState = patchedSpacecraftStates.get(i);

            final SpacecraftState augmentedInitialState;

            if (cr3bp) {
                // Additional equations initialization
                augmentedInitialState =
                                ((STMEquations) additionalEquations.get(i)).setInitialPhi(initialState);
            } else {
                augmentedInitialState =
                                ((PartialDerivativesEquations) additionalEquations.get(i)).setInitialJacobians(initialState);
            }

            // Propagator initialization
            propagatorList.get(i).setInitialState(augmentedInitialState);

            final double integrationTime = propagationTime[i];

            // Propagate until trajectory crosses XZ Plane
            final SpacecraftState finalState =
                            propagatorList.get(i).propagate(initialState.getDate().shiftedBy(integrationTime));

            propagatedSP.add(finalState);
        }

        return propagatedSP;
    }

    /** Compute the constraint of the problem.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fx constraint
     */
    private RealVector computeConstraint(final List<SpacecraftState> propagatedSP) {
        final int n = patchedSpacecraftStates.size();

        int size = 6 * (n - 1);
        if (isEpochDependent) {
            size = size + n - 1; //7*n-1;
        }
        final RealVector fx = MatrixUtils.createRealVector(size);
        for (int i = 0; i < n - 1; i++) {
            final AbsolutePVCoordinates absPvi = patchedSpacecraftStates.get(i + 1).getAbsPVA();
            final AbsolutePVCoordinates absPvf = propagatedSP.get(i).getAbsPVA();
            final Vector3D ecartPos = absPvf.getPosition().subtract(absPvi.getPosition());
            final Vector3D ecartVel = absPvf.getVelocity().subtract(absPvi.getVelocity());
            fx.setSubVector(6 * i, MatrixUtils.createRealVector(ecartPos.toArray()));
            fx.setSubVector(6 * i + 3, MatrixUtils.createRealVector(ecartVel.toArray()));
            if (isEpochDependent) {
                fx.setEntry(6 * (n - 1) + i, absPvi.getDate().durationFrom(absPvi.getDate()) - propagationTime[i]);
            }
        }
        return fx;
    }

    /** Update the trajectory.
     *  @param dx correction on the initial vector
     */
    private void updateTrajectory(final RealVector dx) {
        final int n = patchedSpacecraftStates.size();

        for (int i = 0; i < n; i++) {
            final AbsolutePVCoordinates currentAPV = patchedSpacecraftStates.get(i).getAbsPVA();
            final Vector3D deltaP = new Vector3D(dx.getSubVector(6 * i, 3).toArray());
            final Vector3D deltaV = new Vector3D(dx.getSubVector(6 * i + 3, 3).toArray());
            final Vector3D position = currentAPV.getPosition().subtract(deltaP);
            final Vector3D velocity = currentAPV.getVelocity().subtract(deltaV);
            final PVCoordinates pv = new PVCoordinates(position, velocity);
            AbsoluteDate epoch = currentAPV.getDate();
            if (isEpochDependent) {
                epoch = epoch.shiftedBy(dx.getEntry(7 * n - 7 + i));
            }
            final AbsolutePVCoordinates updatedAPV = new AbsolutePVCoordinates(currentAPV.getFrame(), epoch, pv);
            patchedSpacecraftStates.set(i, new SpacecraftState(updatedAPV, patchedSpacecraftStates.get(i).getAttitude()));
        }
    }

    /** Compute the STM of a SpacecraftState.
     *  @param s SpacecraftState
     *  @return phiM phiM
     */
    private RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final String derivativesName;
        if (cr3bp) {
            derivativesName = "stmEquations";
        } else {
            derivativesName = "derivatives";
        }
        final int dim = 6;
        final double[][] phi2dA = new double[dim][dim];
        final double[] stm = s.getAdditionalState(derivativesName);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < 6; j++) {
                phi2dA[i][j] = stm[dim * i + j];
            }
        }
        final RealMatrix phiM = new Array2DRowRealMatrix(phi2dA, false);
        return phiM;
    }
}
