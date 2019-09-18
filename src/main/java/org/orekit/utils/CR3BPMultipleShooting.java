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
 * Multiple shooting method applicable for orbits, either propagation in CR3BP, or in an ephemeris model.
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 */
public class CR3BPMultipleShooting extends AbstractMultipleShooting {

    /** True if CR3BP is considered (useful for the derivatives computation). */
    private final boolean cr3bp;

    /** Number of constraints. */
    private int nrows;

    /** Number of patch points. */
    private int npoints;

    /** Number of free variables. */
    private int ncolumns;

    /** Number of additional constraints. */
    private int nconstraints;

    /** Simple Constructor.
     * <p> Standard constructor using DormandPrince853 integrator for the differential correction </p>
     * @param initialGuessList first guess PVCoordinates of the point to start differential correction
     * @param propagatorList .
     * @param additionalEquations .
     * @param cr3bp CR3BP System considered .
     * @param pointsWithConstraints Patch point with constraints
     * @param constraintsComponents Component of the patch points which are constrained.
     * @param constraints constraints values
     */
    public CR3BPMultipleShooting(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                                 final List<AdditionalEquations> additionalEquations, final boolean cr3bp,
                                 final int[] pointsWithConstraints, final int[] constraintsComponents, final double[] constraints) {
        super(initialGuessList, propagatorList, additionalEquations, pointsWithConstraints, constraintsComponents, constraints);
        this.cr3bp = cr3bp;
        this.npoints = initialGuessList.size();
        this.nconstraints = getConstraints().length;
        this.nrows = 6 * npoints + nconstraints;
        this.ncolumns = 6 * npoints + 1;
    }

    /** Compute the additional state from the additionalEquations.
     *  @param initialState SpacecraftState without the additional state
     *  @param additionalEquation Additional Equations.
     *  @return augmentedSP SpacecraftState with the additional state within.
     */
    @Override
    protected SpacecraftState getAugmentedInitialState(final SpacecraftState initialState,
                                                       final AdditionalEquations additionalEquation) {
        final SpacecraftState augmentedInitialState;
        if (cr3bp) {
            augmentedInitialState =
                            ((STMEquations) additionalEquation).setInitialPhi(initialState);
        } else {
            augmentedInitialState =
                            ((PartialDerivativesEquations) additionalEquation).setInitialJacobians(initialState);
        }
        return augmentedInitialState;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @param propagatedSP propagatedSP
     *  @return patchedSpacecraftStates patchedSpacecraftStates
     */
    public RealMatrix computeJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final int n = npoints;

        final RealMatrix M = MatrixUtils.createRealMatrix(nrows, ncolumns);
        final RealMatrix minusIsix = MatrixUtils.createRealIdentityMatrix(6).scalarMultiply(-1);

        for (int i = 0; i < n - 1; i++) {

            final SpacecraftState finalState = propagatedSP.get(i);
            // The Jacobian matrix has the following form:

            //          [     |     ]
            //          [  A  |  B  ]
            // DF(X) =  [     |     ]
            //          [-----------]
            //          [  C  |  0  ]
            //
            //      [ phi1     -I                                   ]
            //      [         phi2     -I                           ]
            // A =  [                 ....    ....                  ]   6(n-1)x6n
            //      [                         ....     ....         ]
            //      [                                 phin-1    -I  ]
            //
            //      [ xdot1f ]
            //      [ xdot2f ]
            // B =  [  ....  ]   6(n-1)x1
            //      [  ....  ]
            //      [xdotn-1f]
            //
            // C is computing according to additional constraints.
            //

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

            final double[][] pvArray = new double[][] {
                {pv.getVelocity().getX()},
                {pv.getVelocity().getY()},
                {pv.getVelocity().getZ()},
                {pv.getAcceleration().getX()},
                {pv.getAcceleration().getY()},
                {pv.getAcceleration().getZ()}};

            M.setSubMatrix(pvArray, 6 * i, 6 * n);
        }

        M.setSubMatrix(computeAdditionalJacobianMatrix(propagatedSP), 6 * n - 6, 0);

        return M;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @param propagatedSP propagatedSP
     *  @return patchedSpacecraftStates patchedSpacecraftStates
     */
    public double[][] computeAdditionalJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final int n = npoints;
        final double[][] M = new double[6 + nconstraints][ncolumns];

        // The Jacobian matrix has the following form:
        //
        //      [-1  0              0  ...  1  0             ]
        //      [ 0 -1  0           0  ...     1  0          ]
        // C =  [    0 -1  0        0  ...        1  0       ]   7x6n
        //      [       0 -1  0     0  ...           1  0    ]
        //      [          0  -1 0  0  ...              1  0 ]
        //      [          0  0 -1  0  ...              0  1 ]
        //      [ 0  1  0  0  0  0  0  ...  0  0           0 ]
        //

        for (int i = 0; i < 6; i++) {
            M[i][i] = -1;
            M[i][6 * n - 6 + i] = 1;
        }

        final int[] mapConstraints = getConstraintsMap();

        for (int i = 0; i < nconstraints; i++) {
            M[6 + i][mapConstraints[i]] = 1;
        }
        // System.out.println(Arrays.deepToString(M).replace("], ", "]\n").replace("[[", "[").replace("]]", "]"));

        return M;
    }


    /** Compute the constraint of the problem.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fx constraint
     */
    public double[] computeConstraint(final List<SpacecraftState> propagatedSP) {

        // The Constraint vector has the following form :

        //         [ x1f - x2i ]---
        //         [ x2f - x3i ]   |
        // F(X) =  [    ...    ] vectors
        //         [    ...    ]   |
        //         [xn-1f - xni]---
        //         [    ...    ] additionnal
        //         [    ...    ] constraints

        final int n = npoints;

        final List<SpacecraftState> patchedSpacecraftStates = getPacthedSpacecraftState();

        final double[] fx = new double[nrows];
        for (int i = 0; i < n - 1; i++) {
            final AbsolutePVCoordinates absPvi = patchedSpacecraftStates.get(i + 1).getAbsPVA();
            final AbsolutePVCoordinates absPvf = propagatedSP.get(i).getAbsPVA();
            final double[] ecartPos = absPvf.getPosition().subtract(absPvi.getPosition()).toArray();
            final double[] ecartVel = absPvf.getVelocity().subtract(absPvi.getVelocity()).toArray();
            for (int j = 0; j < 3; j++) {
                fx[6 * i + j] = ecartPos[j];
                fx[6 * i + 3 + j] = ecartVel[j];
            }
        }

        final double[] additionalConstraints = computeAdditionalConstraints(propagatedSP);

        for (int i = 0; i < additionalConstraints.length; i++) {
            fx[6 * n - 6 + i] = additionalConstraints[i];
        }

        return fx;
    }

    /** Compute the constraint of the problem.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fx constraint
     */
    public double[] computeAdditionalConstraints(final List<SpacecraftState> propagatedSP) {

        // The additional constraint vector has the following form (for a closed orbit) :

        //           [ xni - x1i ]----
        //           [ yni - x1i ]    |
        // Fadd(X) = [ zni - x1i ] vector's
        //           [vxni - vx1i] component
        //           [vzni - vz1i]    |
        //           [ y1i - y1d ]----

        final int n = 6 + nconstraints;

        final List<SpacecraftState> patchedSpacecraftStates = getPacthedSpacecraftState();

        final int size = patchedSpacecraftStates.size();

        final double[] fxAdditionnal = new double[n];
        final AbsolutePVCoordinates absPv1 = patchedSpacecraftStates.get(0).getAbsPVA();
        final AbsolutePVCoordinates absPvn = patchedSpacecraftStates.get(size - 1).getAbsPVA();

        fxAdditionnal[0] = absPvn.getPosition().getX() - absPv1.getPosition().getX();
        fxAdditionnal[1] = absPvn.getPosition().getY() - absPv1.getPosition().getY();
        fxAdditionnal[2] = absPvn.getPosition().getZ() - absPv1.getPosition().getZ();
        fxAdditionnal[3] = absPvn.getVelocity().getX() - absPv1.getVelocity().getX();
        fxAdditionnal[4] = absPvn.getVelocity().getY() - absPv1.getVelocity().getY();
        fxAdditionnal[5] =  absPvn.getVelocity().getZ() - absPv1.getVelocity().getZ();

        final int[] mapConstraints = getConstraintsMap();
        final double[] constraints = getConstraints();

        for (int i = 0; i < nconstraints; i++) {
            final int np = (int) (mapConstraints[i] / 6);
            final int nc = mapConstraints[i] % 6;
            final AbsolutePVCoordinates absPv = patchedSpacecraftStates.get(np).getAbsPVA();
            if (nc < 3) {
                fxAdditionnal[6 + i] = absPv.getPosition().toArray()[nc] - constraints[i];
            } else {
                fxAdditionnal[6 + i] = absPv.getVelocity().toArray()[nc - 3] - constraints[i];
            }
        }

        return fxAdditionnal;
    }

    /** Update the trajectory.
     *  @param dx correction on the initial vector
     */
    public void updateTrajectory(final RealVector dx) {

        final double[] propagationTime = getPropagationTime();

        final List<SpacecraftState> patchedSpacecraftStates = getPacthedSpacecraftState();
        final int n = patchedSpacecraftStates.size();

        for (int i = 0; i < n; i++) {
            final AbsolutePVCoordinates currentAPV = patchedSpacecraftStates.get(i).getAbsPVA();
            final Vector3D deltaP = new Vector3D(dx.getSubVector(6 * i, 3).toArray());
            final Vector3D deltaV = new Vector3D(dx.getSubVector(6 * i + 3, 3).toArray());
            final Vector3D position = currentAPV.getPosition().subtract(deltaP);
            final Vector3D velocity = currentAPV.getVelocity().subtract(deltaV);
            final PVCoordinates pv = new PVCoordinates(position, velocity);
            final AbsoluteDate epoch = currentAPV.getDate();
            final AbsolutePVCoordinates updatedAPV = new AbsolutePVCoordinates(currentAPV.getFrame(), epoch, pv);
            patchedSpacecraftStates.set(i, new SpacecraftState(updatedAPV, patchedSpacecraftStates.get(i).getAttitude()));
        }

        final double deltaT = propagationTime[0] - dx.getEntry(6 * n);

        for (int i = 0; i < propagationTime.length; i++) {
            propagationTime[i] = deltaT;
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
