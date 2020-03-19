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

package org.orekit.propagation.numerical.cr3bp;

import java.util.List;
import java.util.Map;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AbstractMultipleShooting;

/**
 * Multiple shooting method applicable for orbits, either propagation in CR3BP, or in an ephemeris model.
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 */
public class CR3BPMultipleShooter extends AbstractMultipleShooting {

    /** Number of patch points. */
    private int npoints;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting which can be used with the CR3BP model.</p>
     * @param initialGuessList initial patch points to be corrected.
     * @param propagatorList list of propagators associated to each patch point.
     * @param additionalEquations list of additional equations linked to propagatorList.
     * @param arcDuration initial guess of the duration of each arc.
     * @param tolerance convergence tolerance on the constraint vector
     */
    public CR3BPMultipleShooter(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                                 final List<AdditionalEquations> additionalEquations, final double arcDuration, final double tolerance) {
        super(initialGuessList, propagatorList, additionalEquations, arcDuration, tolerance);
        this.npoints = initialGuessList.size();
    }

    /** {@inheritDoc} */
    protected SpacecraftState getAugmentedInitialState(final SpacecraftState initialState,
                                                       final AdditionalEquations additionalEquation) {
        return ((STMEquations) additionalEquation).setInitialPhi(initialState);
    }

    /** {@inheritDoc} */
    public double[][] computeAdditionalJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final Map<Integer, Double> mapConstraints = getConstraintsMap();

        final boolean isClosedOrbit = isClosedOrbit();

        // Number of additional constraints
        final int n = mapConstraints.size() + (isClosedOrbit ? 6 : 0);

        final int ncolumns = getNumberOfFreeVariables() - 1;

        final double[][] M = new double[n][ncolumns];

        int k = 0;
        if (isClosedOrbit) {
            // The Jacobian matrix has the following form:
            //
            //      [-1  0              0  ...  1  0             ]
            //      [ 0 -1  0           0  ...     1  0          ]
            // C =  [    0 -1  0        0  ...        1  0       ]
            //      [       0 -1  0     0  ...           1  0    ]
            //      [          0  -1 0  0  ...              1  0 ]
            //      [          0  0 -1  0  ...              0  1 ]

            for (int i = 0; i < 6; i++) {
                M[i][i] = -1;
                M[i][ncolumns - 6 + i] = 1;
            }
            k = 6;
        }

        for (int index : mapConstraints.keySet()) {
            M[k][index] = 1;
            k++;
        }
        return M;
    }

    /** {@inheritDoc} */
    public double[][] computeEpochJacobianMatrix(final List<SpacecraftState> propagatedSP) {
        final int nFreeEpoch = getNumberOfFreeEpoch();
        // Rows and columns dimensions
        final int ncolumns   = 1 + nFreeEpoch;
        final int nrows      = npoints - 1;
        // Return an empty array
    	return new double[nrows][ncolumns];
    }

    /** {@inheritDoc} */
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
        final boolean isClosedOrbit = isClosedOrbit();
        // Number of additional constraints
        final int n = mapConstraints.size() + (isClosedOrbit ? 6 : 0);

        final List<SpacecraftState> patchedSpacecraftStates = getPatchedSpacecraftState();

        final double[] fxAdditionnal = new double[n];
        int i = 0;

        if (isClosedOrbit) {

            final AbsolutePVCoordinates apv1i = patchedSpacecraftStates.get(0).getAbsPVA();
            final AbsolutePVCoordinates apvni = patchedSpacecraftStates.get(npoints - 1).getAbsPVA();

            fxAdditionnal[0] = apvni.getPosition().getX() - apv1i.getPosition().getX();
            fxAdditionnal[1] = apvni.getPosition().getY() - apv1i.getPosition().getY();
            fxAdditionnal[2] = apvni.getPosition().getZ() - apv1i.getPosition().getZ();
            fxAdditionnal[3] = apvni.getVelocity().getX() - apv1i.getVelocity().getX();
            fxAdditionnal[4] = apvni.getVelocity().getY() - apv1i.getVelocity().getY();
            fxAdditionnal[5] = apvni.getVelocity().getZ() - apv1i.getVelocity().getZ();

            i = 6;
        }

        for (int index : mapConstraints.keySet()) {
            final int np = index / 6;
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

    /** {@inheritDoc} */
    protected RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final Map<String, double[]> map = s.getAdditionalStates();
        RealMatrix phiM = null;
        for (String name : map.keySet()) {
            if ("stmEquations".equals(name)) {
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
