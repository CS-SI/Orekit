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
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.AbsolutePartialDerivativesEquations;
//import org.orekit.propagation.numerical.EpochDerivativesEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
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

    /** Number of patch points. */
    private int npoints;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting which can be used with the CR3BP model.</p>
     * @param initialGuessList initial patch points to be corrected.
     * @param propagatorList list of propagators associated to each patch point.
     * @param additionalEquations list of additional equations linked to propagatorList.
     * @param cr3bp boolean if the correction is made in the CR3BP.
     * @param tolerance convergence tolerance on the constraint vector
     */
    public CR3BPMultipleShooting(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                                 final List<AdditionalEquations> additionalEquations, final boolean cr3bp, final double tolerance) {
        super(initialGuessList, propagatorList, additionalEquations, tolerance);
        this.cr3bp = cr3bp;
        this.npoints = initialGuessList.size();
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
                            ((AbsolutePartialDerivativesEquations) additionalEquation).setInitialJacobians(initialState);
//                            ((EpochDerivativesEquations) additionalEquation).setInitialJacobians(initialState);
        }
        return augmentedInitialState;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @param propagatedSP List of propagated SpacecraftStates (patch points)
     *  @return jacobianMatrix Jacobian matrix
     */
    public RealMatrix computeJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final boolean[] freePatchPointMap = getFreePatchPointMap();
        final boolean[] freeEpochMap = getFreeEpochMap();
        final int nFreeEpoch = getNumberOfFreeEpoch();

        final int nrows = getNumberOfConstraints() + nFreeEpoch;
        final int ncolumns = getNumberOfFreeVariables() + nFreeEpoch;
        final int nFree = getNumberOfFreeVariables();

        // Exception should be covered in higher abstract method
        if (ncolumns > nrows) {
            throw new OrekitException(OrekitMessages.MULTIPLE_SHOOTING_UNDERCONSTRAINED, ncolumns, nrows);
        }
        final RealMatrix M = MatrixUtils.createRealMatrix(nrows, ncolumns);

        int index = 0;
        int indexEpoch = nFree;
        for (int i = 0; i < npoints - 1; i++) {

            final SpacecraftState finalState = propagatedSP.get(i);
            // The Jacobian matrix has the following form:

            //          [     |     |     ]
            //          [  A  |  B  |  C  ]
            // DF(X) =  [     |     |     ]
            //          [-----------------]
            //          [  0  |  D  |  E  ]
            //          [-----------------]
            //          [  F  |  0  |  0  ]

            //          [     |     ]
            //          [  A  |  B  ]
            // DF(X) =  [     |     ]
            //          [-----------]
            //          [  C  |  0  ]
            //
            // For a problem with all the components of each patch points is free, A is detailed below :
            //      [ phi1     -I                                   ]
            //      [         phi2     -I                           ]
            // A =  [                 ....    ....                  ]   6(n-1)x6n
            //      [                         ....     ....         ]
            //      [                                 phin-1    -I  ]

            // D is computing according to additional constraints
            // (for now, closed orbit, or a component of a patch point equals to a specified value)
            //

            // If the duration of the propagation of each arc is the same :
            //      [ xdot1f ]
            //      [ xdot2f ]                      [  -1 ]
            // B =  [  ....  ]   6(n-1)x1   and D = [ ... ]
            //      [  ....  ]                      [  -1 ]
            //      [xdotn-1f]

            // Otherwise :
            //      [ xdot1f                                ]
            //      [        xdot2f                         ]
            // B =  [                 ....                  ]   6(n-1)x(n-1) and D = -I
            //      [                        ....           ]
            //      [                             xdotn-1f  ]
            //
            // If the problem is not dependant of the epoch (e.g. CR3BP), the C and E matrices are not computed.
            // Otherwise :
            //      [ -dx1f/dtau1                                           0 ]
            //      [          -dx2f/dtau2                                  0 ]
            // C =  [                       ....                            0 ]   6(n-1)xn
            //      [                               ....                    0 ]
            //      [                                     -dxn-1f/dtaun-1   0 ]
            //
            //      [ -1   1  0                 ]
            //      [     -1   1  0             ]
            // E =  [          ..   ..          ] n-1xn
            //      [               ..   ..     ]
            //      [                    -1   1 ]

            final PVCoordinates pvf = finalState.getPVCoordinates();

            // Get State Transition Matrix phi
            final RealMatrix phi = getStateTransitionMatrix(finalState);

            // Matrix A
            for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
                if (freePatchPointMap[6 * i + j]) { // If this component is free
                    for (int k = 0; k < 6; k++) { // Loop on the 6 component of the patch point constraint
                        M.setEntry(6 * i + k, index, phi.getEntry(k, j));
                    }
                    if (i > 0) {
                        M.setEntry(6 * (i - 1) + j, index, -1);
                    }
                    index++;
                }
            }

            // Matrix B
            final double[][] pvfArray = new double[][] {
                {pvf.getVelocity().getX()},
                {pvf.getVelocity().getY()},
                {pvf.getVelocity().getZ()},
                {pvf.getAcceleration().getX()},
                {pvf.getAcceleration().getY()},
                {pvf.getAcceleration().getZ()}};

            M.setSubMatrix(pvfArray, 6 * i, nFree - 1);

            // Matrix C
            if (freeEpochMap[i]) { // If this component is free
                final double[] derivatives = finalState.getAdditionalState("derivatives");
                final double[][] subC = new double[6][1];
                for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
                    subC[j][0] = -derivatives[derivatives.length - 6 + j];
                }
                M.setSubMatrix(subC, 6 * i, indexEpoch);
                indexEpoch++;
            }
        }

        for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
            if (freePatchPointMap[6 * (npoints - 1) + j]) { // If this component is free
                M.setEntry(6 * (npoints - 2) + j, index, -1);
                index++;
            }
        }


        // Matrices D and E.
        if (nFreeEpoch > 0) {
            final double[][] subDE = computeEpochJacobianMatrix(propagatedSP);
            M.setSubMatrix(subDE, 6 * (npoints - 1), nFree - 1);
        }
        final int nEpoch = nFreeEpoch > 0 ? npoints - 1 : 0;


        // Matrices F.
        final double[][] subF = computeAdditionalJacobianMatrix(propagatedSP);
        if (subF.length > 0) {
            M.setSubMatrix(subF, 6 * (npoints - 1) + nEpoch, 0);
        }

        return M;
    }

    /** Compute a part of the Jacobian matrix from additional constraints.
     *  @param propagatedSP propagatedSP
     *  @return jacobianMatrix Jacobian sub-matrix
     */
    public double[][] computeAdditionalJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final Map<Integer, Double> mapConstraints = getConstraintsMap();

        // Number of additional constraints
        int n = mapConstraints.size();
        if (isClosedOrbit()) {
            n = n + 6;
        }

        final int ncolumns = getNumberOfFreeVariables() - 1;

        final double[][] M = new double[n][ncolumns];

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
        //      [-1  0              0  ...  1  0             ]
        //      [ 0 -1  0           0  ...     1  0          ]
        // C =  [    0 -1  0        0  ...        1  0       ]   7x6n
        //      [       0 -1  0     0  ...           1  0    ]
        //      [          0  -1 0  0  ...              1  0 ]
        //      [          0  0 -1  0  ...              0  1 ]
        //      [ 0  1  0  0  0  0  0  ...  0  0           0 ]
        //

        int j = 0;
        if (isClosedOrbit()) {
            for (int i = 0; i < 6; i++) {
                M[i][i] = -1;
                M[i][ncolumns - 6 + i] = 1;
            }
            j = 6;
        }

        for (int index : mapConstraints.keySet()) {
            M[j][index] = 1;
            j++;
        }
        // System.out.println(Arrays.deepToString(M).replace("], ", "]\n").replace("[[", "[").replace("]]", "]"));

        return M;
    }

    /** Compute a part of the Jacobian matrix with derivatives from epoch.
     *  @param propagatedSP propagatedSP
     *  @return jacobianMatrix Jacobian sub-matrix
     */
    public double[][] computeEpochJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final boolean[] freeEpochMap = getFreeEpochMap();

        final int nFreeEpoch = getNumberOfFreeEpoch();
        final int ncolumns = 1 + nFreeEpoch;
        final int nrows = npoints - 1;

        final double[][] M = new double[nrows][ncolumns];

        // The Jacobian matrix has the following form:

        //      [-1 -1   1  0                 ]
        //      [-1     -1   1  0             ]
        // F =  [..          ..   ..          ]
        //      [..               ..   ..   0 ]
        //      [-1                    -1   1 ]

        int index = 1;
        for (int i = 0; i < nrows; i++) {
            M[i][0] = -1;
            if (freeEpochMap[i]) {
                M[i][index] = -1;
                index++;
            }
            if (freeEpochMap[i + 1]) {
                M[i][index] =  1;
            }
        }

        return M;
    }


    /** Compute the constraint vector of the problem.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fx constraint
     */
    public double[] computeConstraint(final List<SpacecraftState> propagatedSP) {

        // The Constraint vector has the following form :

        //         [ x1f - x2i ]---
        //         [ x2f - x3i ]   |
        // F(X) =  [    ...    ] vectors' equality for a continuous trajectory
        //         [    ...    ]   |
        //         [xn-1f - xni]---
        //         [ d2-(d1+T) ]   continuity between epoch
        //         [    ...    ]   and integration time
        //         [dn-(dn-1+T)]---
        //         [    ...    ] additional
        //         [    ...    ] constraints

        final double[] additionalConstraints = computeAdditionalConstraints(propagatedSP);
        final boolean epoch = getNumberOfFreeEpoch() > 0;

        final int nrows = epoch ? getNumberOfConstraints() + npoints - 1 : getNumberOfConstraints();

        final List<SpacecraftState> patchedSpacecraftStates = getPatchedSpacecraftState();

        final double[] fx = new double[nrows];
        for (int i = 0; i < npoints - 1; i++) {
            final AbsolutePVCoordinates absPvi = patchedSpacecraftStates.get(i + 1).getAbsPVA();
            final AbsolutePVCoordinates absPvf = propagatedSP.get(i).getAbsPVA();
            final double[] ecartPos = absPvf.getPosition().subtract(absPvi.getPosition()).toArray();
            final double[] ecartVel = absPvf.getVelocity().subtract(absPvi.getVelocity()).toArray();
            for (int j = 0; j < 3; j++) {
                fx[6 * i + j] = ecartPos[j];
                fx[6 * i + 3 + j] = ecartVel[j];
            }
        }

//        int index = getNumberOfConstraints();
        int index = 6 * npoints - 6;

        if (epoch) {
            final double[] propagationTime = getPropagationTime();
            for (int i = 0; i < npoints - 1; i++) {
                final double deltaEpoch = patchedSpacecraftStates.get(i + 1).getDate().durationFrom(patchedSpacecraftStates.get(i).getDate());
                fx[index] = deltaEpoch - propagationTime[i];
                index++;
            }
        }

        for (int i = 0; i < additionalConstraints.length; i++) {
//            fx[6 * npoints - 6 + i] = additionalConstraints[i];
            fx[index] = additionalConstraints[i];
            index++;
        }

        return fx;
    }

    /** Compute the additional constraints.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fxAdditionnal additional constraints
     */
    public double[] computeAdditionalConstraints(final List<SpacecraftState> propagatedSP) {

        // The additional constraint vector has the following form :

        //           [ xni - x1i ]----
        //           [ yni - x1i ]    |
        // Fadd(X) = [ zni - x1i ] vector's component
        //           [vxni - vx1i] for a closed orbit
        //           [vyni - vy1i]    |
        //           [vzni - vz1i]----
        //           [ y1i - y1d ]---- other constraints (component of
        //           [    ...    ]    | a patch point eaquals to a
        //           [vz2i - vz2d]----  desired value)

        final Map<Integer, Double> mapConstraints = getConstraintsMap();
        // Number of additional constraints
        int n = mapConstraints.size();
        if (isClosedOrbit()) {
            n = n + 6;
        }

        final List<SpacecraftState> patchedSpacecraftStates = getPatchedSpacecraftState();

        final double[] fxAdditionnal = new double[n];
        int i = 0;

        if (isClosedOrbit()) {

            final int size = patchedSpacecraftStates.size();

            final AbsolutePVCoordinates apv1i = patchedSpacecraftStates.get(0).getAbsPVA();
            final AbsolutePVCoordinates apvni = patchedSpacecraftStates.get(size - 1).getAbsPVA();
//            final AbsolutePVCoordinates apvnf = propagatedSP.get(propagatedSP.size() - 1).getAbsPVA();

            if (cr3bp) {
                fxAdditionnal[0] = apvni.getPosition().getX() - apv1i.getPosition().getX();
                fxAdditionnal[1] = apvni.getPosition().getY() - apv1i.getPosition().getY();
                fxAdditionnal[2] = apvni.getPosition().getZ() - apv1i.getPosition().getZ();
                fxAdditionnal[3] = apvni.getVelocity().getX() - apv1i.getVelocity().getX();
                fxAdditionnal[4] = apvni.getVelocity().getY() - apv1i.getVelocity().getY();
                fxAdditionnal[5] = apvni.getVelocity().getZ() - apv1i.getVelocity().getZ();
            } else {
                final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();

//                final AbsolutePVCoordinates apv0 = syst.getNormalizedAPV(apv1i);
//                final AbsolutePVCoordinates apvn = syst.getNormalizedAPV(apvni);

                final TimeStampedPVCoordinates apv0 = apv1i.getPVCoordinates(syst.getRotatingFrame());
                final TimeStampedPVCoordinates apvn = apvni.getPVCoordinates(syst.getRotatingFrame());

                fxAdditionnal[0] = apvn.getPosition().getX() - apv0.getPosition().getX();
                fxAdditionnal[1] = apvn.getPosition().getY() - apv0.getPosition().getY();
                fxAdditionnal[2] = apvn.getPosition().getZ() - apv0.getPosition().getZ();
                fxAdditionnal[3] = apvn.getVelocity().getX() - apv0.getVelocity().getX();
                fxAdditionnal[4] = apvn.getVelocity().getY() - apv0.getVelocity().getY();
                fxAdditionnal[5] = apvn.getVelocity().getZ() - apv0.getVelocity().getZ();
            }
            i = 6;
        }

        for (int index : mapConstraints.keySet()) {
            final int np = (int) (index / 6);
            final int nc = index % 6;
            final AbsolutePVCoordinates absPv = patchedSpacecraftStates.get(np).getAbsPVA();
            if (nc < 3) {
                fxAdditionnal[i] = absPv.getPosition().toArray()[nc] - mapConstraints.get(index);
            } else {
                fxAdditionnal[i] = absPv.getVelocity().toArray()[nc - 3] -  mapConstraints.get(index);
            }
            i++;
        }
        return fxAdditionnal;
    }

    /** Update the trajectory, and the propagation time.
     *  @param dx correction on the initial vector
     */
    public void updateTrajectory(final RealVector dx) {
        // X = [x1, ..., xn, T1, ..., Tn, d1, ..., dn]
        // X = [x1, ..., xn, T, d2, ..., dn]

        final int n = getNumberOfFreeVariables();

        final boolean epochFree = getNumberOfFreeEpoch() > 0;

        // Update propagation time
        //------------------------------------------------------
        final double[] propagationTime = getPropagationTime();

        final double deltaT = dx.getEntry(n - 1);
        for (int i = 0; i < propagationTime.length; i++) {
            propagationTime[i] = propagationTime[i] - deltaT;
        }

        // Update patch points through SpacecrafStates
        //--------------------------------------------------------------------------------
        final List<SpacecraftState> patchedSpacecraftStates = getPatchedSpacecraftState();
        final boolean[] freePatchPointMap = getFreePatchPointMap();

        int index = 0;

        for (int i = 0; i < patchedSpacecraftStates.size(); i++) {
            // Get delta in position and velocity
            final double[] deltaPV = new double[6];
            for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
                if (freePatchPointMap[6 * i + j]) { // If this component is free (is to be updated)
                    deltaPV[j] = dx.getEntry(index);
                    index++;
                }
            }
            final Vector3D deltaP = new Vector3D(deltaPV[0], deltaPV[1], deltaPV[2]);
            final Vector3D deltaV = new Vector3D(deltaPV[3], deltaPV[4], deltaPV[5]);

//            System.out.println(deltaP);
//            System.out.println(deltaV);

            // Update the PVCoordinates of the patch point
            final AbsolutePVCoordinates currentAPV = patchedSpacecraftStates.get(i).getAbsPVA();
            final Vector3D position = currentAPV.getPosition().subtract(deltaP);
            final Vector3D velocity = currentAPV.getVelocity().subtract(deltaV);
            final PVCoordinates pv = new PVCoordinates(position, velocity);

            //Update epoch in the AbsolutePVCoordinates
            AbsoluteDate epoch = currentAPV.getDate();
            if (i > 0) {
                if (epochFree) {
                    final double deltaEpoch = dx.getEntry(n + i - 1);
                    epoch = patchedSpacecraftStates.get(i).getDate().shiftedBy(-deltaEpoch);
                    epoch = patchedSpacecraftStates.get(i - 1).getDate().shiftedBy(propagationTime[i - 1]);
                } else {
                    epoch = patchedSpacecraftStates.get(i - 1).getDate().shiftedBy(propagationTime[i - 1]);
                }
            }
            final AbsolutePVCoordinates updatedAPV = new AbsolutePVCoordinates(currentAPV.getFrame(), epoch, pv);

            //Update attitude epoch
            Attitude attitude = patchedSpacecraftStates.get(i).getAttitude();
            if (i > 0) {
                final AttitudeProvider attitudeProvider = getPropagatorList().get(i - 1).getAttitudeProvider();
                attitude = attitudeProvider.getAttitude(updatedAPV, epoch, currentAPV.getFrame());
            }

            //Update the SpacecraftState using previously updated attitude and AbsolutePVCoordinates
            patchedSpacecraftStates.set(i, new SpacecraftState(updatedAPV, attitude));
        }
//        System.out.println(dx.getEntry(n - 1));
//        if (epochFree) {
//            System.out.println(dx.getSubVector(n, npoints - 1));
//        }
    }

    /** Compute the STM (State Transition Matrix) of a SpacecraftState.
     *  @param s SpacecraftState
     *  @return phiM phiM
     */
    private RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final Map<String, double[]> map = s.getAdditionalStates();
        RealMatrix phiM = null;
        for (String name : map.keySet()) {
            if ("stmEquations".equals(name) || "derivatives".equals(name)) {
                final int dim = 6;
                final double[][] phi2dA = new double[dim][dim];
                final double[] stm = map.get(name);
                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < 6; j++) {
                        phi2dA[i][j] = stm[dim * i + j];
                    }
                }
                phiM = new Array2DRowRealMatrix(phi2dA, false);
            }
        }
        return phiM;
    }
}
