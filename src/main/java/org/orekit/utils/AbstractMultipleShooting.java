/* Copyright 2002-2020 CS GROUP
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

/**
 * Multiple shooting method using only constraints on state vectors of patch points (and possibly on epoch and integration time).
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 * @since 10.2
 */
public abstract class AbstractMultipleShooting implements MultipleShooting {

    /** Patch points along the trajectory. */
    private List<SpacecraftState> patchedSpacecraftStates;

    /** Derivatives linked to the Propagators. */
    private final List<AdditionalEquations> additionalEquations;

    /** List of Propagators. */
    private final List<NumericalPropagator> propagatorList;

    /** Duration of propagation along each arcs. */
    private double[] propagationTime;

    /** Free components of patch points. */
    private boolean[] freePatchPointMap;

    /** Free epoch of patch points. */
    private boolean[] freeEpochMap;

    /** Number of free variables. */
    private int nFree;

    /** Number of free epoch. */
    private int nEpoch;

    /** Number of constraints. */
    private int nConstraints;

    /** Patch points components which are constrained. */
    private Map<Integer, Double> mapConstraints;

    /** True if orbit is closed. */
    private boolean isClosedOrbit;

    /** Tolerance on the constraint vector. */
    private double tolerance;

    /** Expected name of the additional equations. */
    private final String additionalName;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting </p>
     * @param initialGuessList initial patch points to be corrected.
     * @param propagatorList list of propagators associated to each patch point.
     * @param additionalEquations list of additional equations linked to propagatorList.
     * @param arcDuration initial guess of the duration of each arc.
     * @param tolerance convergence tolerance on the constraint vector.
     * @param additionalName name of the additional equations
     */
    protected AbstractMultipleShooting(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                                       final List<AdditionalEquations> additionalEquations, final double arcDuration,
                                       final double tolerance, final String additionalName) {
        this.patchedSpacecraftStates = initialGuessList;
        this.propagatorList = propagatorList;
        this.additionalEquations = additionalEquations;
        this.additionalName = additionalName;
        // Should check if propagatorList.size() = initialGuessList.size() - 1
        final int propagationNumber = initialGuessList.size() - 1;
        this.propagationTime = new double[propagationNumber];
        for (int i = 0; i < propagationNumber; i++ ) {
            this.propagationTime[i] = arcDuration;
        }

        // All the patch points are set initially as free variables
        this.freePatchPointMap = new boolean[6 * initialGuessList.size()]; // epoch
        for (int i = 0; i < freePatchPointMap.length; i++) {
            freePatchPointMap[i] = true;
        }

        //Except the first one, the epochs of the patch points are set free.
        this.freeEpochMap = new boolean[initialGuessList.size()];
        freeEpochMap[0] = false;
        for (int i = 1; i < freeEpochMap.length; i++) {
            freeEpochMap[i] = true;
        }
        this.nEpoch = initialGuessList.size() - 1;

        this.nConstraints = 6 * propagationNumber;
        this.nFree = 6 * initialGuessList.size() + 1;

        this.tolerance = tolerance;

        // All the additional constraints must be set afterward
        this.mapConstraints = new HashMap<>();
    }

    /** Set a component of a patch point to free or not.
     * @param patchNumber Patch point with constraint
     * @param componentIndex Component of the patch points which are constrained.
     * @param isFree constraint value
     */
    public void setPatchPointComponentFreedom(final int patchNumber, final int componentIndex, final boolean isFree) {
        if (freePatchPointMap[6 * (patchNumber - 1) +  componentIndex] != isFree ) {
            final int eps = isFree ? 1 : -1;
            nFree = nFree + eps;
            freePatchPointMap[6 * (patchNumber - 1) +  componentIndex] = isFree;
        }
    }

    /** Add a constraint on one component of one patch point.
     * @param patchNumber Patch point with constraint
     * @param componentIndex Component of the patch points which are constrained.
     * @param constraintValue constraint value
     */
    public void addConstraint(final int patchNumber, final int componentIndex, final double constraintValue) {
        final int contraintIndex = (patchNumber - 1) * 6 + componentIndex;
        if (!mapConstraints.containsKey(contraintIndex)) {
            nConstraints++;
        }
        mapConstraints.put(contraintIndex, constraintValue);
    }

    /** Set the epoch a patch point to free or not.
     * @param patchNumber Patch point
     * @param isFree constraint value
     */
    public void setEpochFreedom(final int patchNumber, final boolean isFree) {
        if (freeEpochMap[patchNumber - 1] != isFree) {
            freeEpochMap[patchNumber - 1] = isFree;
            final int eps = isFree ? 1 : -1;
            nEpoch = nEpoch + eps;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<SpacecraftState> compute() {

        if (nFree > nConstraints) {
            throw new OrekitException(OrekitMessages.MULTIPLE_SHOOTING_UNDERCONSTRAINED, nFree, nConstraints);
        }

        int iter = 0; // number of iteration

        double fxNorm = 0;

        do {

            final List<SpacecraftState> propagatedSP = propagatePatchedSpacecraftState(); // multi threading see PropagatorsParallelizer
            final RealMatrix M = computeJacobianMatrix(propagatedSP);
            final RealVector fx = MatrixUtils.createRealVector(computeConstraint(propagatedSP));

            // Solve linear system
            final RealMatrix MMt = M.multiply(M.transpose());
            final RealVector dx  = M.transpose().multiply(MatrixUtils.inverse(MMt)).operate(fx);

            // Apply correction from the free variable vector to all the variables (propagation time, pacthSpaceraftState)
            updateTrajectory(dx);

            fxNorm = fx.getNorm() / fx.getDimension();

            iter++;

        } while (fxNorm > tolerance && iter < 1); // Converge within tolerance and under 10 iterations

        return patchedSpacecraftStates;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @param propagatedSP List of propagated SpacecraftStates (patch points)
     *  @return jacobianMatrix Jacobian matrix
     */
    private RealMatrix computeJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final int npoints         = patchedSpacecraftStates.size();
        final int epochConstraint = nEpoch == 0 ? 0 : npoints - 1;
        final int nrows    = getNumberOfConstraints() + epochConstraint;
        final int ncolumns = getNumberOfFreeVariables() + nEpoch;

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
            final double[][] phi = getStateTransitionMatrix(finalState);

            // Matrix A
            for (int j = 0; j < 6; j++) { // Loop on 6 component of the patch point
                if (freePatchPointMap[6 * i + j]) { // If this component is free
                    for (int k = 0; k < 6; k++) { // Loop on the 6 component of the patch point constraint
                        M.setEntry(6 * i + k, index, phi[k][j]);
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
        if (nEpoch > 0) {
            final double[][] subDE = computeEpochJacobianMatrix(propagatedSP);
            M.setSubMatrix(subDE, 6 * (npoints - 1), nFree - 1);
        }

        // Matrices F.
        final double[][] subF = computeAdditionalJacobianMatrix(propagatedSP);
        if (subF.length > 0) {
            M.setSubMatrix(subF, 6 * (npoints - 1) + epochConstraint, 0);
        }

        return M;
    }

    /** Compute the constraint vector of the problem.
     *  @param propagatedSP propagated SpacecraftState
     *  @return constraint vector
     */
    private double[] computeConstraint(final List<SpacecraftState> propagatedSP) {

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

        final int npoints = patchedSpacecraftStates.size();

        final double[] additionalConstraints = computeAdditionalConstraints(propagatedSP);
        final boolean epoch = getNumberOfFreeEpoch() > 0;

        final int nrows = epoch ? getNumberOfConstraints() + npoints - 1 : getNumberOfConstraints();

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

        int index = 6 * (npoints - 1);

        if (epoch) {
            for (int i = 0; i < npoints - 1; i++) {
                final double deltaEpoch = patchedSpacecraftStates.get(i + 1).getDate().durationFrom(patchedSpacecraftStates.get(i).getDate());
                fx[index] = deltaEpoch - propagationTime[i];
                index++;
            }
        }

        for (int i = 0; i < additionalConstraints.length; i++) {
            fx[index] = additionalConstraints[i];
            index++;
        }

        return fx;
    }


    /** Update the trajectory, and the propagation time.
     *  @param dx correction on the initial vector
     */
    private void updateTrajectory(final RealVector dx) {
        // X = [x1, ..., xn, T1, ..., Tn, d1, ..., dn]
        // X = [x1, ..., xn, T, d2, ..., dn]

        final int n = getNumberOfFreeVariables();

        final boolean epochFree = getNumberOfFreeEpoch() > 0;

        // Update propagation time
        //------------------------------------------------------
        final double deltaT = dx.getEntry(n - 1);
        for (int i = 0; i < propagationTime.length; i++) {
            propagationTime[i] = propagationTime[i] - deltaT;
        }

        // Update patch points through SpacecrafStates
        //--------------------------------------------------------------------------------

        int index = 0;
        int indexEpoch = 0;

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

            // Update the PVCoordinates of the patch point
            final AbsolutePVCoordinates currentAPV = patchedSpacecraftStates.get(i).getAbsPVA();
            final Vector3D position = currentAPV.getPosition().subtract(deltaP);
            final Vector3D velocity = currentAPV.getVelocity().subtract(deltaV);
            final PVCoordinates pv = new PVCoordinates(position, velocity);

            //Update epoch in the AbsolutePVCoordinates
            AbsoluteDate epoch = currentAPV.getDate();
            if (epochFree) {
                if (freeEpochMap[i]) {
                    final double deltaEpoch = dx.getEntry(n + indexEpoch);
                    epoch = epoch.shiftedBy(-deltaEpoch);
                    indexEpoch++;
                }
            } else {
                if (i > 0) {
                    epoch = patchedSpacecraftStates.get(i - 1).getDate().shiftedBy(propagationTime[i - 1]);
                }
            }
            final AbsolutePVCoordinates updatedAPV = new AbsolutePVCoordinates(currentAPV.getFrame(), epoch, pv);

            //Update attitude epoch
            // Last point does not have an associated propagator. The previous one is then selected.
            final int iAttitude = i < getPropagatorList().size() ? i : getPropagatorList().size() - 1;
            final AttitudeProvider attitudeProvider = getPropagatorList().get(iAttitude).getAttitudeProvider();
            final Attitude attitude = attitudeProvider.getAttitude(updatedAPV, epoch, currentAPV.getFrame());

            //Update the SpacecraftState using previously updated attitude and AbsolutePVCoordinates
            patchedSpacecraftStates.set(i, new SpacecraftState(updatedAPV, attitude));
        }

    }

    /** Compute the Jacobian matrix of the problem.
     *  @return propagatedSP propagated SpacecraftStates
     */
    private List<SpacecraftState> propagatePatchedSpacecraftState() {

        final int n = patchedSpacecraftStates.size() - 1;

        final ArrayList<SpacecraftState> propagatedSP = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {

            // SpacecraftState initialization
            final SpacecraftState initialState = patchedSpacecraftStates.get(i);

            final SpacecraftState augmentedInitialState = getAugmentedInitialState(initialState, additionalEquations.get(i));

            // Propagator initialization
            propagatorList.get(i).setInitialState(augmentedInitialState);

            final double integrationTime = propagationTime[i];

            // Propagate trajectory
            final SpacecraftState finalState = propagatorList.get(i).propagate(initialState.getDate().shiftedBy(integrationTime));

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
        final Map<String, double[]> map = s.getAdditionalStates();
        // Initialize state transition matrix
        final int        dim  = 6;
        final double[][] phiM = new double[dim][dim];

        // Loop on entry set
        for (final Map.Entry<String, double[]> entry : map.entrySet()) {
            // Extract entry name
            final String name = entry.getKey();
            if (additionalName.equals(name)) {
                final double[] stm = entry.getValue();
                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < 6; j++) {
                        phiM[i][j] = stm[dim * i + j];
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

        final boolean[] map = getFreeEpochMap();

        final int nFreeEpoch = getNumberOfFreeEpoch();
        final int ncolumns = 1 + nFreeEpoch;
        final int nrows = patchedSpacecraftStates.size() - 1;

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
            if (map[i]) {
                M[i][index] = -1;
                index++;
            }
            if (map[i + 1]) {
                M[i][index] =  1;
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
        for (final Map.Entry<Integer, Double> entry : getConstraintsMap().entrySet()) {
            // Extract entry values
            final int    key   = entry.getKey();
            final double value = entry.getValue();
            final int np = key / 6;
            final int nc = key % 6;
            final AbsolutePVCoordinates absPv = getPatchedSpacecraftState().get(np).getAbsPVA();
            if (nc < 3) {
                fxAdditional[iter] = absPv.getPosition().toArray()[nc] - value;
            } else {
                fxAdditional[iter] = absPv.getVelocity().toArray()[nc - 3] -  value;
            }
            iter++;
        }
    }

    /** Compute the additional constraints.
     *  @param propagatedSP propagated SpacecraftState
     *  @return fxAdditionnal additional constraints
     */
    protected abstract double[] computeAdditionalConstraints(List<SpacecraftState> propagatedSP);

    /** Compute a part of the Jacobian matrix from additional constraints.
     *  @param propagatedSP propagatedSP
     *  @return jacobianMatrix Jacobian sub-matrix
     */
    protected abstract double[][] computeAdditionalJacobianMatrix(List<SpacecraftState> propagatedSP);


    /** Compute the additional state from the additionalEquations.
     *  @param initialState SpacecraftState without the additional state
     *  @param additionalEquations2 Additional Equations.
     *  @return augmentedSP SpacecraftState with the additional state within.
     */
    protected abstract SpacecraftState getAugmentedInitialState(SpacecraftState initialState,
                                                                AdditionalEquations additionalEquations2);



    /** Set the constraint of a closed orbit or not.
     *  @param isClosed true if orbit should be closed
     */
    public void setClosedOrbitConstraint(final boolean isClosed) {
        if (this.isClosedOrbit != isClosed) {
            nConstraints = nConstraints + (isClosed ? 6 : -6);
            this.isClosedOrbit = isClosed;
        }
    }

    /** Get the number of free variables.
     * @return the number of free variables
     */
    protected int getNumberOfFreeVariables() {
        return nFree;
    }

    /** Get the number of free epoch.
     * @return the number of free epoch
     */
    protected int getNumberOfFreeEpoch() {
        return nEpoch;
    }

    /** Get the number of constraints.
     * @return the number of constraints
     */
    protected int getNumberOfConstraints() {
        return nConstraints;
    }

    /** Get the flags representing the free components of patch points.
     * @return an array of flags representing the free components of patch points
     */
    protected boolean[] getFreePatchPointMap() {
        return freePatchPointMap;
    }

    /** Get the flags representing the free epoch of patch points.
     * @return an array of flags representing the free epoch of patch points
     */
    protected boolean[] getFreeEpochMap() {
        return freeEpochMap;
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
    protected List<NumericalPropagator> getPropagatorList() {
        return propagatorList;
    }

    /** Get he flag representing if the orbit is closed.
     * @return true if orbit is closed
     */
    protected boolean isClosedOrbit() {
        return isClosedOrbit;
    }

}
