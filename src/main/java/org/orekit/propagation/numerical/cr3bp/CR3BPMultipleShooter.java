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
package org.orekit.propagation.numerical.cr3bp;

import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AbstractMultipleShooting;

/**
 * Multiple shooting method applicable for orbits, either propagation in CR3BP, or in an ephemeris model.
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 * @author Alberto Foss&agrave;
 */
public class CR3BPMultipleShooter extends AbstractMultipleShooting {

    /** Name of the derivatives. */
    private static final String STM = "stmEquations";

    /** Derivatives linked to the Propagators.
     * @since 11.1
     */
    private final List<STMEquations> stmEquations;

    /** True if orbit is closed. */
    private boolean isClosedOrbit;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting which can be used with the CR3BP model. </p>
     * @param initialGuessList initial patch points to be corrected
     * @param propagatorList list of propagators associated to each patch point
     * @param stmEquations list of additional derivatives providers linked to propagatorList
     * @param tolerance convergence tolerance on the constraint vector
     * @param maxIter maximum number of iterations
     */
    public CR3BPMultipleShooter(final List<SpacecraftState> initialGuessList,
                                final List<NumericalPropagator> propagatorList,
                                final List<STMEquations> stmEquations,
                                final double tolerance, final int maxIter) {
        super(initialGuessList, propagatorList, tolerance, maxIter, true, STM);
        this.stmEquations = stmEquations;
    }

    /** {@inheritDoc} */
    protected SpacecraftState getAugmentedInitialState(final int i) {
        return stmEquations.get(i).setInitialPhi(getPatchPoint(i));
    }

    /** {@inheritDoc} */
    @Override
    public void setEpochFreedom(final int patchIndex, final boolean isFree) {
        throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED);
    }

    /** {@inheritDoc} */
    @Override
    public void setScaleLength(final double scaleLength) {
        throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED);
    }

    /** {@inheritDoc} */
    @Override
    public void setScaleTime(final double scaleTime) {
        throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED);
    }

    /** Set the constraint of a closed orbit or not.
     *  @param isClosed true if orbit should be closed
     */
    public void setClosedOrbitConstraint(final boolean isClosed) {
        this.isClosedOrbit = isClosed;
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeAdditionalJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final Map<Integer, Double> mapConstraints = getConstraintsMap();
        final boolean[] freeCompsMap              = getFreeCompsMap();

        // Number of additional constraints
        final int nRows = mapConstraints.size() + (isClosedOrbit ? 6 : 0);
        final int nCols = getNumberOfFreeComponents();

        final double[][] M = new double[nRows][nCols];

        int j = 0;
        if (isClosedOrbit) {
            // The Jacobian matrix has the following form:
            //
            //      [-1  0              0  ...  1  0             ]
            //      [ 0 -1  0           0  ...     1  0          ]
            // F =  [    0 -1  0        0  ...        1  0       ]
            //      [       0 -1  0     0  ...           1  0    ]
            //      [          0 -1  0  0  ...              1  0 ]
            //      [          0  0 -1  0  ...              0  1 ]

            int index = 0;
            for (int i = 0; i < 6; i++) {
                if (freeCompsMap[i]) {
                    M[i][index] = -1.0;
                    index++;
                }
            }
            index = nCols - 6;
            for (int i = 0; i < 6; i++) {
                if (freeCompsMap[nCols - 6 + i]) {
                    M[i][index] = 1.0;
                    index++;
                }
            }
            j = 6;
        }

        for (int k : mapConstraints.keySet()) {
            M[j][k] = 1.0;
            j++;
        }

        return M;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] computeAdditionalConstraints(final List<SpacecraftState> propagatedSP) {

        // The additional constraint vector has the following form :

        //           [ xni - x1i ]----
        //           [ yni - x1i ]    |
        // Fadd(X) = [ zni - x1i ] vector's component
        //           [vxni - vx1i] for a closed orbit
        //           [vyni - vy1i]    |
        //           [vzni - vz1i]----
        //           [ y1i - y1d ]---- other constraints (component of
        //           [    ...    ]    | a patch point equals to a
        //           [vz2i - vz2d]----  desired value)

        final Map<Integer, Double> mapConstraints           = getConstraintsMap();
        final List<SpacecraftState> patchedSpacecraftStates = getPatchedSpacecraftState();

        final double[] fxAdditional = new double[mapConstraints.size() + (isClosedOrbit ? 6 : 0)];
        int i = 0;

        if (isClosedOrbit) {

            final AbsolutePVCoordinates apv1i = patchedSpacecraftStates.get(0).getAbsPVA();
            final AbsolutePVCoordinates apvni = patchedSpacecraftStates.get(patchedSpacecraftStates.size() - 1).getAbsPVA();

            fxAdditional[0] = apvni.getPosition().getX() - apv1i.getPosition().getX();
            fxAdditional[1] = apvni.getPosition().getY() - apv1i.getPosition().getY();
            fxAdditional[2] = apvni.getPosition().getZ() - apv1i.getPosition().getZ();
            fxAdditional[3] = apvni.getVelocity().getX() - apv1i.getVelocity().getX();
            fxAdditional[4] = apvni.getVelocity().getY() - apv1i.getVelocity().getY();
            fxAdditional[5] = apvni.getVelocity().getZ() - apv1i.getVelocity().getZ();

            i = 6;
        }

        // Update additional constraints
        updateAdditionalConstraints(i, fxAdditional);
        return fxAdditional;
    }

    /** {@inheritDoc} */
    @Override
    protected int getNumberOfConstraints() {
        return super.getNumberOfConstraints() + (isClosedOrbit ? 6 : 0);
    }

}
