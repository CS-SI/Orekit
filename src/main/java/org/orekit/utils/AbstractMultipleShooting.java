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
package org.orekit.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

/**
 * Multiple shooting method using only constraints on state vectors of patch points (and possibly on epoch and integration time).
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 * @author Alberto Foss&agrave;
 * @since 10.2
 */
public abstract class AbstractMultipleShooting implements MultipleShooting {

    /** Patch points along the trajectory. */
    private final List<SpacecraftState> patchedSpacecraftStates;

    /** List of Propagators. */
    private final List<NumericalPropagator> propagatorList;

    /** Duration of propagation along each arc. */
    private final double[] propagationTime;

    /** Free components of patch points. */
    private final boolean[] freeCompsMap;

    /** Free epochs of patch points. */
    private final boolean[] freeEpochMap;

    /** Number of free state components. */
    private int nComps;

    /** Number of free arc duration. */
    // TODO add possibility to fix integration time span?
    private final int nDuration;

    /** Number of free epochs. */
    private int nEpoch;

    /** Scale time for update computation. */
    private double scaleTime;

    /** Scale length for update computation. */
    private double scaleLength;

    /** Patch points components which are constrained. */
    private final Map<Integer, Double> mapConstraints;

    /** True if the dynamical system is autonomous.
     * In this case epochs and epochs constraints are omitted from the problem formulation. */
    private final boolean isAutonomous;

    /** Tolerance on the constraint vector. */
    private final double tolerance;

    /** Maximum number of iterations. */
    private final int maxIter;

    /** Expected name of the additional equations. */
    private final String additionalName;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting </p>
     * @param initialGuessList initial patch points to be corrected
     * @param propagatorList list of propagators associated to each patch point
     * @param tolerance convergence tolerance on the constraint vector
     * @param maxIter maximum number of iterations
     * @param isAutonomous true if the dynamical system is autonomous (i.e. not dependent on the epoch)
     * @param additionalName name of the additional equations
     * @since 11.1
     */
    protected AbstractMultipleShooting(final List<SpacecraftState> initialGuessList,
                                       final List<NumericalPropagator> propagatorList,
                                       final double tolerance, final int maxIter,
                                       final boolean isAutonomous, final String additionalName) {

        this.patchedSpacecraftStates = initialGuessList;
        this.propagatorList          = propagatorList;
        this.isAutonomous            = isAutonomous;
        this.additionalName          = additionalName;

        // propagation duration
        final int propagationNumber = initialGuessList.size() - 1;
        propagationTime = new double[propagationNumber];
        for (int i = 0; i < propagationNumber; i++) {
            propagationTime[i] = initialGuessList.get(i + 1).getDate().durationFrom(initialGuessList.get(i).getDate());
        }

        // states components freedom
        this.freeCompsMap = new boolean[6 * initialGuessList.size()];
        Arrays.fill(freeCompsMap, true);

        // epochs freedom
        if (isAutonomous) {
            // epochs omitted from problem formulation
            this.freeEpochMap = new boolean[0];
        } else {
            this.freeEpochMap = new boolean[initialGuessList.size()];
            Arrays.fill(freeEpochMap, true);
        }

        // number of free variables
        this.nComps    = 6 * initialGuessList.size();
        this.nDuration = propagationNumber;
        this.nEpoch    = freeEpochMap.length;

        // convergence criteria
        this.tolerance = tolerance;
        this.maxIter   = maxIter;

        // scaling
        this.scaleTime   = 1.0;
        this.scaleLength = 1.0;

        // all additional constraints must be set afterward
        this.mapConstraints = new HashMap<>();
    }

    /** Get a patch point.
     * @param i index of the patch point
     * @return state of the patch point
     * @since 11.1
     */
    protected SpacecraftState getPatchPoint(final int i) {
        return patchedSpacecraftStates.get(i);
    }

    /** Set a component of a patch point to free or not.
     * @param patchIndex Patch point index (zero-based)
     * @param componentIndex Index of the component to be constrained (zero-based)
     * @param isFree constraint value
     */
    public void setPatchPointComponentFreedom(final int patchIndex, final int componentIndex, final boolean isFree) {
        if (freeCompsMap[6 * patchIndex + componentIndex] != isFree) {
            freeCompsMap[6 * patchIndex + componentIndex] = isFree;
            nComps += isFree ? 1 : -1;
        }
    }

    /** Set the epoch of a patch point to free or not.
     * @param patchIndex Patch point index (zero-based)
     * @param isFree constraint value
     */
    public void setEpochFreedom(final int patchIndex, final boolean isFree) {
        if (freeEpochMap[patchIndex] != isFree) {
            freeEpochMap[patchIndex] = isFree;
            nEpoch += isFree ? 1 : -1;
        }
    }

    /** Set the scale time.
     * @param scaleTime scale time in seconds
     */
    public void setScaleTime(final double scaleTime) {
        this.scaleTime = scaleTime;
    }

    /** Set the scale length.
     * @param scaleLength scale length in meters
     */
    public void setScaleLength(final double scaleLength) {
        this.scaleLength = scaleLength;
    }

    /** Add a constraint on one component of one patch point.
     * @param patchIndex Patch point index (zero-based)
     * @param componentIndex Index of the component which is constrained (zero-based)
     * @param constraintValue constraint value
     */
    public void addConstraint(final int patchIndex, final int componentIndex, final double constraintValue) {
        mapConstraints.put(patchIndex * 6 + componentIndex, constraintValue);
    }

    /** {@inheritDoc} */
    @Override
    public List<SpacecraftState> compute() {

        int iter = 0; // number of iteration
        double fxNorm;

        while (iter < maxIter) {

            iter++;

            // propagation (multi-threading see PropagatorsParallelizer)
            final List<SpacecraftState> propagatedSP = propagatePatchedSpacecraftState();

            // constraints computation
            final RealVector fx = MatrixUtils.createRealVector(computeConstraint(propagatedSP));

            // convergence check
            fxNorm = fx.getNorm();
            // System.out.printf(Locale.US, "Iter: %3d Error: %.16e%n", iter, fxNorm);
            if (fxNorm < tolerance) {
                break;
            }

            // Jacobian matrix computation
            final RealMatrix M = computeJacobianMatrix(propagatedSP);

            // correction computation using minimum norm approach
            // (i.e. minimize difference between solutions from successive iterations,
            //  in other word try to stay close to initial guess. This is *not* a least-squares solution)
            // see equation 3.12 in Pavlak's thesis
            final RealMatrix MMt = M.multiplyTransposed(M);
            RealVector dx;
            try {
                dx = M.transpose().operate(new LUDecomposition(MMt, 0.0).getSolver().solve(fx));
            } catch (MathIllegalArgumentException e) {
                dx = M.transpose().operate(new QRDecomposition(MMt, 0.0).getSolver().solve(fx));
            }

            // trajectory update
            updateTrajectory(dx);
        }

        return patchedSpacecraftStates;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @param propagatedSP List of propagated SpacecraftStates (patch points)
     *  @return jacobianMatrix Jacobian matrix
     */
    private RealMatrix computeJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        // The Jacobian matrix has the following form:
        //
        //          [     |     |     ]
        //          [  A  |  B  |  C  ]
        // DF(X) =  [     |     |     ]
        //          [-----------------]
        //          [  0  |  D  |  E  ]
        //          [-----------------]
        //          [  F  |  0  |  0  ]
        //
        // For a problem in which all the components of each patch points are free, A is detailed below:
        //
        //      [ phi1     -I                                   ]
        //      [         phi2     -I                           ]
        // A =  [                 ....    ....                  ]   6(n-1)x6n
        //      [                         ....     ....         ]
        //      [                                 phin-1    -I  ]
        //
        // If the duration of the propagation of each arc is the same:
        //
        //      [ xdot1f ]
        //      [ xdot2f ]                      [  -1 ]
        // B =  [  ....  ]   6(n-1)x1   and D = [ ... ]
        //      [  ....  ]                      [  -1 ]
        //      [xdotn-1f]
        //
        // Otherwise:
        //
        //      [ xdot1f                                ]
        //      [        xdot2f                         ]
        // B =  [                 ....                  ]   6(n-1)x(n-1) and D = -I
        //      [                        ....           ]
        //      [                             xdotn-1f  ]
        //
        // If the problem is not dependant on the epoch (e.g. CR3BP), the C, D and E matrices are not computed.
        //
        // Otherwise:
        //      [ dx1f/dtau1                                           0 ]
        //      [          dx2f/dtau2                                  0 ]
        // C =  [                      ....                            0 ]   6(n-1)xn
        //      [                              ....                    0 ]
        //      [                                     dxn-1f/dtaun-1   0 ]
        //
        //      [ -1   1  0                 ]
        //      [     -1   1  0             ]
        // E =  [          ..   ..          ] n-1xn
        //      [               ..   ..     ]
        //      [                    -1   1 ]
        //
        // F is computed according to additional constraints
        // (for now, closed orbit, or a component of a patch point equals to a specified value)

        final int nArcs    = patchedSpacecraftStates.size() - 1;
        final double scaleVel = scaleLength / scaleTime;
        final double scaleAcc = scaleVel / scaleTime;
        final RealMatrix M = MatrixUtils.createRealMatrix(getNumberOfConstraints(), getNumberOfFreeVariables());

        int index = 0; // first column index for matrix A
        int indexDuration = nComps; // first column index for matrix B
        int indexEpoch = indexDuration + nDuration; // first column index for matrix C
        for (int i = 0; i < nArcs; i++) {

            final SpacecraftState finalState = propagatedSP.get(i);

            // PV coordinates and state transition matrix at final time
            final PVCoordinates pvf = finalState.getPVCoordinates();
            final double[][] phi    = getStateTransitionMatrix(finalState); // already scaled

            // Matrix A
            for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
                if (freeCompsMap[6 * i + j]) { // If this component is free
                    for (int k = 0; k < 6; k++) { // Loop on the 6 component of the patch point constraint
                        M.setEntry(6 * i + k, index, phi[k][j]);
                    }
                    if (i > 0) {
                        M.setEntry(6 * (i - 1) + j, index, -1.0);
                    }
                    index++;
                }
            }

            // Matrix B
            final double[][] pvfArray = new double[][] {
                {pvf.getVelocity().getX() / scaleVel},
                {pvf.getVelocity().getY() / scaleVel},
                {pvf.getVelocity().getZ() / scaleVel},
                {pvf.getAcceleration().getX() / scaleAcc},
                {pvf.getAcceleration().getY() / scaleAcc},
                {pvf.getAcceleration().getZ() / scaleAcc}};

            M.setSubMatrix(pvfArray, 6 * i, indexDuration);
            indexDuration++;

            // Matrix C
            // there is a typo in Pavlak's thesis, equations 3.48-3.49:
            // the sign in front of the partials of the states with respect to epochs should be plus
            if (!isAutonomous) {
                if (freeEpochMap[i]) { // If this component is free
                    final double[] derivatives = finalState.getAdditionalState(additionalName);
                    final double[][] subC = new double[6][1];
                    for (int j = 0; j < 3; j++) {
                        subC[j][0] = derivatives[derivatives.length - 6 + j] / scaleVel;
                        subC[j + 3][0] = derivatives[derivatives.length - 3 + j] / scaleAcc;
                    }
                    M.setSubMatrix(subC, 6 * i, indexEpoch);
                    indexEpoch++;
                }
            }
        }

        // complete Matrix A
        for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
            if (freeCompsMap[6 * nArcs + j]) { // If this component is free
                M.setEntry(6 * (nArcs - 1) + j, index, -1.0);
                index++;
            }
        }

        // Matrices D and E
        if (!isAutonomous) {
            final double[][] subDE = computeEpochJacobianMatrix(propagatedSP);
            M.setSubMatrix(subDE, 6 * nArcs, nComps);
        }

        // Matrix F
        final double[][] subF = computeAdditionalJacobianMatrix(propagatedSP);
        if (subF.length > 0) {
            M.setSubMatrix(subF, isAutonomous ? 6 * nArcs : 7 * nArcs, 0);
        }

        return M;
    }

    /** Compute the constraint vector of the problem.
     *  @param propagatedSP propagated SpacecraftState
     *  @return constraint vector
     */
    private double[] computeConstraint(final List<SpacecraftState> propagatedSP) {

        // The Constraint vector has the following form :
        //
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

        final int nPoints     = patchedSpacecraftStates.size();
        final double scaleVel = scaleLength / scaleTime;
        final double[] fx     = new double[getNumberOfConstraints()];

        // state continuity
        for (int i = 0; i < nPoints - 1; i++) {
            final AbsolutePVCoordinates absPvi = patchedSpacecraftStates.get(i + 1).getAbsPVA();
            final AbsolutePVCoordinates absPvf = propagatedSP.get(i).getAbsPVA();
            final double[] deltaPos = absPvf.getPosition().subtract(absPvi.getPosition()).toArray();
            final double[] deltaVel = absPvf.getVelocity().subtract(absPvi.getVelocity()).toArray();
            for (int j = 0; j < 3; j++) {
                fx[6 * i + j]     = deltaPos[j] / scaleLength;
                fx[6 * i + 3 + j] = deltaVel[j] / scaleVel;
            }
        }

        int index = 6 * (nPoints - 1);

        // epoch constraints
        if (!isAutonomous) {
            for (int i = 0; i < nPoints - 1; i++) {
                final double deltaEpoch = patchedSpacecraftStates.get(i + 1).getDate()
                                         .durationFrom(patchedSpacecraftStates.get(i).getDate());
                fx[index] = (deltaEpoch - propagationTime[i]) / scaleTime;
                index++;
            }
        }

        // additional constraints
        final double[] additionalConstraints = computeAdditionalConstraints(propagatedSP);
        for (double constraint : additionalConstraints) {
            fx[index] = constraint;
            index++;
        }

        return fx;
    }

    /** Update the trajectory, and the propagation time.
     *  @param dx correction on the initial vector
     */
    private void updateTrajectory(final RealVector dx) {
        // X = [x1, ..., xn, T1, ..., Tn, d1, ..., dn]

        final double scaleVel = scaleLength / scaleTime;

        // Update propagation time
        int indexDuration = nComps;
        for (int i = 0; i < nDuration; i++) {
            propagationTime[i] -= dx.getEntry(indexDuration) * scaleTime;
            indexDuration++;
        }

        // Update patch points through SpacecraftStates
        int index = 0;
        int indexEpoch = nComps + nDuration;

        for (int i = 0; i < patchedSpacecraftStates.size(); i++) {

            // Get delta in position and velocity
            final double[] deltaPV = new double[6];
            for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
                if (freeCompsMap[6 * i + j]) { // If this component is free (is to be updated)
                    deltaPV[j] = dx.getEntry(index);
                    index++;
                }
            }
            final Vector3D deltaP = new Vector3D(deltaPV[0], deltaPV[1], deltaPV[2]).scalarMultiply(scaleLength);
            final Vector3D deltaV = new Vector3D(deltaPV[3], deltaPV[4], deltaPV[5]).scalarMultiply(scaleVel);

            // Update the PVCoordinates of the patch point
            final AbsolutePVCoordinates currentAPV = patchedSpacecraftStates.get(i).getAbsPVA();
            final Vector3D position = currentAPV.getPosition().subtract(deltaP);
            final Vector3D velocity = currentAPV.getVelocity().subtract(deltaV);
            final PVCoordinates pv  = new PVCoordinates(position, velocity);

            // Update epoch in the AbsolutePVCoordinates
            AbsoluteDate epoch = currentAPV.getDate();
            if (!isAutonomous) {
                if (freeEpochMap[i]) {
                    epoch = epoch.shiftedBy(-dx.getEntry(indexEpoch) * scaleTime);
                    indexEpoch++;
                }
            } else {
                // for autonomous systems we arbitrarily fix the date of the first patch point
                if (i > 0) {
                    epoch = patchedSpacecraftStates.get(i - 1).getDate().shiftedBy(propagationTime[i - 1]);
                }
            }
            final AbsolutePVCoordinates updatedAPV = new AbsolutePVCoordinates(currentAPV.getFrame(), epoch, pv);

            // Update attitude epoch
            // Last point does not have an associated propagator. The previous one is then selected.
            final int iAttitude = i < getPropagatorList().size() ? i : getPropagatorList().size() - 1;
            final AttitudeProvider attitudeProvider = getPropagatorList().get(iAttitude).getAttitudeProvider();
            final Attitude attitude = attitudeProvider.getAttitude(updatedAPV, epoch, currentAPV.getFrame());

            // Update the SpacecraftState using previously updated attitude and AbsolutePVCoordinates
            patchedSpacecraftStates.set(i, new SpacecraftState(updatedAPV, attitude));
        }

    }

    /** Propagate the patch point states.
     *  @return propagatedSP propagated SpacecraftStates
     */
    private List<SpacecraftState> propagatePatchedSpacecraftState() {

        final int nArcs = patchedSpacecraftStates.size() - 1;
        final ArrayList<SpacecraftState> propagatedSP = new ArrayList<>(nArcs);

        for (int i = 0; i < nArcs; i++) {

            // SpacecraftState initialization
            final SpacecraftState augmentedInitialState = getAugmentedInitialState(i);

            // Propagator initialization
            propagatorList.get(i).setInitialState(augmentedInitialState);

            // Propagate trajectory
            final AbsoluteDate target = augmentedInitialState.getDate().shiftedBy(propagationTime[i]);
            final SpacecraftState finalState = propagatorList.get(i).propagate(target);

            propagatedSP.add(finalState);
        }

        return propagatedSP;
    }

    /** Get the state transition matrix.
     * @param s current spacecraft state
     * @return the state transition matrix
     */
    private double[][] getStateTransitionMatrix(final SpacecraftState s) {
        // Additional states
        final DoubleArrayDictionary dictionary = s.getAdditionalStatesValues();
        // Initialize state transition matrix
        final int        dim  = 6;
        final double[][] phiM = new double[dim][dim];

        // Loop on entry set
        for (final DoubleArrayDictionary.Entry entry : dictionary.getData()) {
            // Extract entry name
            final String name = entry.getKey();
            if (additionalName.equals(name)) {
                final double[] stm = entry.getValue();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {

                        // partials of final position w.r.t. initial position
                        phiM[i][j] = stm[6 * i + j];

                        // partials of final position w.r.t. initial velocity
                        phiM[i][j + 3] = stm[6 * i + j + 3] / scaleTime;

                        // partials of final velocity w.r.t. initial position
                        phiM[i + 3][j] = stm[6 * i + j + 18] * scaleTime;

                        // partials of final velocity w.r.t. initial velocity
                        phiM[i + 3][j + 3] = stm[6 * i + j + 21];
                    }
                }
            }
        }

        // Return state transition matrix
        return phiM;
    }

    /** Compute a part of the Jacobian matrix with derivatives from epoch.
     * The CR3BP is a time invariant problem. The derivatives w.r.t. epoch are zero.
     *  @param propagatedSP propagatedSP
     *  @return jacobianMatrix Jacobian sub-matrix
     */
    protected double[][] computeEpochJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final int nRows    = patchedSpacecraftStates.size() - 1;
        final double[][] M = new double[nRows][nDuration + nEpoch];

        // The D and E sub-matrices have the following form:
        //
        // D = -I
        //
        //     [-1 +1  0          ]
        //     [   -1 +1  0       ]
        // F = [      .. ..       ]
        //     [         .. ..  0 ]
        //     [            -1 +1 ]

        int index = nDuration;
        for (int i = 0; i < nRows; i++) {

            // components of D matrix
            M[i][i] = -1.0;

            // components of E matrix
            if (freeEpochMap[i]) {
                M[i][index] = -1.0;
                index++;
            }
            if (freeEpochMap[i + 1]) {
                M[i][index] = 1.0;
            }
        }

        return M;
    }

    /** Update the array of additional constraints.
     * @param startIndex start index
     * @param fxAdditional array of additional constraints
     */
    protected void updateAdditionalConstraints(final int startIndex, final double[] fxAdditional) {
        int iter = startIndex;
        final double scaleVel = scaleLength / scaleTime;
        for (final Map.Entry<Integer, Double> entry : getConstraintsMap().entrySet()) {
            // Extract entry values
            final int    key   = entry.getKey();
            final double value = entry.getValue();
            final int np = key / 6;
            final int nc = key % 6;
            final AbsolutePVCoordinates absPv = getPatchedSpacecraftState().get(np).getAbsPVA();
            if (nc < 3) {
                fxAdditional[iter] = (absPv.getPosition().toArray()[nc] - value) / scaleLength;
            } else {
                fxAdditional[iter] = (absPv.getVelocity().toArray()[nc - 3] -  value) / scaleVel;
            }
            iter++;
        }
    }

    /** Compute the additional constraints.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fxAdditional additional constraints
     */
    protected abstract double[] computeAdditionalConstraints(List<SpacecraftState> propagatedSP);

    /** Compute a part of the Jacobian matrix from additional constraints.
     *  @param propagatedSP propagatedSP
     *  @return jacobianMatrix Jacobian sub-matrix
     */
    protected abstract double[][] computeAdditionalJacobianMatrix(List<SpacecraftState> propagatedSP);

    /** Compute the additional state from the additionalEquations.
     *  @param i index of the state
     *  @return augmentedSP SpacecraftState with the additional state within.
     *  @since 11.1
     */
    protected abstract SpacecraftState getAugmentedInitialState(int i);

    /** Get the number of free state components.
     * @return number of free components
     */
    protected int getNumberOfFreeComponents() {
        return nComps;
    }

    /** Get the total number of constraints.
     * @return the total number of constraints
     */
    protected int getNumberOfConstraints() {
        final int nArcs = patchedSpacecraftStates.size() - 1;
        return (isAutonomous ? 6 * nArcs : 7 * nArcs) + mapConstraints.size();
    }

    /** Get the map of free state components.
     * @return map of free state components
     */
    protected boolean[] getFreeCompsMap() {
        return freeCompsMap;
    }

    /** Get the map of patch points components which are constrained.
     * @return a map of patch points components which are constrained
     */
    protected Map<Integer, Double> getConstraintsMap() {
        return mapConstraints;
    }

    /** Get the list of patched spacecraft states.
     * @return a list of patched spacecraft states
     */
    protected List<SpacecraftState> getPatchedSpacecraftState() {
        return patchedSpacecraftStates;
    }

    /** Get the list of propagators.
     * @return a list of propagators
     */
    private List<NumericalPropagator> getPropagatorList() {
        return propagatorList;
    }

    /** Get the number of free variables.
     * @return the number of free variables
     */
    private int getNumberOfFreeVariables() {
        return nComps + nDuration + nEpoch;
    }

}
