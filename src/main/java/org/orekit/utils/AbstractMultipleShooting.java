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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;

/**
 * Multiple shooting method using only constraints on state vectors of patch points (and possibly on epoch and integration time).
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
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

    /** Number of free variables. */
    private int nFree;

    /** Number of constraints. */
    private int nConstraints;

    /** Pacth points components which are constrained. */
    private Map<Integer, Double> mapConstraints;

    /** True if orbit is closed. */
    private boolean isClosedOrbit;

    /** Tolerance on the constraint vector. */
    private double tolerance;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting </p>
     * @param initialGuessList initial patch points to be corrected.
     * @param propagatorList list of propagators associated to each patch point.
     * @param additionalEquations list of additional equations linked to propagatorList.
     * @param tolerance convergence tolerance on the constraint vector.
     */
    protected AbstractMultipleShooting(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                                       final List<AdditionalEquations> additionalEquations, final double tolerance) {
        this.patchedSpacecraftStates = initialGuessList;
        this.propagatorList = propagatorList;
        this.additionalEquations = additionalEquations;
        // Should check if propagatorList.size() = initialGuessList.size() - 1
        final int propagationNumber = initialGuessList.size() - 1;
        this.propagationTime = new double[propagationNumber];
        for (int i = 0; i < propagationNumber; i++ ) {
            this.propagationTime[i] = patchedSpacecraftStates.get(i + 1).getDate().durationFrom(initialGuessList.get(i).getDate());
        }

        // All the patch points are set initially as free variables
        this.freePatchPointMap = new boolean[6 * initialGuessList.size()];
        for (int i = 0; i < freePatchPointMap.length; i++) {
            freePatchPointMap[i] = true;
        }

        this.nConstraints = 6 * propagationNumber;
        this.nFree = 6 * initialGuessList.size() + 1; // T (common propagation time) is a free variable

        this.tolerance = tolerance;

        // All the constraints must be set afterward
        this.mapConstraints = new HashMap<>();
        this.isClosedOrbit = false;
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

    /** Set the constraint of a closed orbit or not.
     *  @param isClosed true if orbit should be closed
     */
    public void setClosedOrbitConstraint(final boolean isClosed) {
        if (this.isClosedOrbit != isClosed) {
            final int eps = isClosed ? 1 : -1;
            nConstraints = nConstraints + eps * 6;
            this.isClosedOrbit = isClosed;
        }
    }

    /** Return the list of corrected patch points.
     *  An optimizer is better suited for this problem
     * @return patchedSpacecraftStates corrected trajectory
     */
    public List<SpacecraftState> compute() {

        if (nFree > nConstraints) {
            throw new OrekitException(OrekitMessages.MULTIPLE_SHOOTING_UNDERCONSTRAINED, nFree, nConstraints);
        }

        int iter = 0; // number of iteration

        double fxNorm = 0;

        do {
            //            System.out.println("Itération " + (iter + 1));
            //            System.out.println(Arrays.toString(propagationTime));

            final List<SpacecraftState> propagatedSP = propagatePatchedSpacecraftState(); // multi threading see PropagatorsParallelizer
            final RealMatrix M = computeJacobianMatrix(propagatedSP);
            final RealVector fx = MatrixUtils.createRealVector(computeConstraint(propagatedSP));

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

            // Apply correction from the free variable vector to all the variables (propagation time, pacthSpaceraftState)
            updateTrajectory(dx);

            fxNorm = fx.getNorm() / fx.getDimension();

            //            System.out.println("DF(X)^T*DF(X)");
            //            System.out.println(Arrays.deepToString(MtM.getData()));
            //
            //            System.out.println("DeltaX");
            //            System.out.println(Arrays.toString(dx.toArray()));
            //
            //            System.out.println("||F(X)|| = " + fxNorm);

            iter++;

        } while (fxNorm > tolerance & iter < 10); // Converge within 1E-8 tolerance and under 10 iterations

        return patchedSpacecraftStates;
    }

    /** Compute the Jacobian matrix of the problem.
     *  @return propagatedSP propagated SpacecraftStates
     */
    public List<SpacecraftState> propagatePatchedSpacecraftState() {

        final int n = patchedSpacecraftStates.size() - 1;

        final ArrayList<SpacecraftState> propagatedSP = new ArrayList<SpacecraftState>(n);

        for (int i = 0; i < n; i++) {

            // SpacecraftState initialization
            final SpacecraftState initialState = patchedSpacecraftStates.get(i);

            final SpacecraftState augmentedInitialState = getAugmentedInitialState(initialState, additionalEquations.get(i));

            // Propagator initialization
            propagatorList.get(i).setInitialState(augmentedInitialState);

            final double integrationTime = propagationTime[i];
            //            final long startTime = System.currentTimeMillis(); // Useful to plot computation duration

            // Propagate trajectory
            final SpacecraftState finalState =
                            propagatorList.get(i).propagate(initialState.getDate().shiftedBy(integrationTime));
            //            System.out.println(System.currentTimeMillis()-startTime);

            propagatedSP.add(finalState);
        }

        return propagatedSP;
    }

    /** Compute the additional state from the additionalEquations.
     *  @param sp SpacecraftState without the additional state
     *  @param additionalEquation Additional Equations.
     *  @return augmentedSP SpacecraftState with the additional state within.
     */
    protected SpacecraftState getAugmentedInitialState(final SpacecraftState sp,
                                                       final AdditionalEquations additionalEquation) {

        final SpacecraftState augmentedSP =
                        ((PartialDerivativesEquations) additionalEquation).setInitialJacobians(sp);

        return augmentedSP;
    }

    protected boolean isClosedOrbit() {
        return isClosedOrbit;
    }

    protected int getNumberOfFreeVariables() {
        return nFree;
    }

    protected int getNumberOfConstraints() {
        return nConstraints;
    }

    protected boolean[] getFreePatchPointMap() {
        return freePatchPointMap;
    }

    protected Map<Integer, Double> getConstraintsMap() {
        return mapConstraints;
    }

    protected List<SpacecraftState> getPacthedSpacecraftState() {
        return patchedSpacecraftStates;
    }

    protected double[] getPropagationTime() {
        return propagationTime;
    }
}
