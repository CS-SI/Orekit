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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.EpochDerivativesEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AbstractMultipleShooting;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

/**
 * Multiple shooting method applicable for orbits, either propagation in CR3BP, or in an ephemeris model.
 * @see "TRAJECTORY DESIGN AND ORBIT MAINTENANCE STRATEGIES IN MULTI-BODY DYNAMICAL REGIMES by Thomas A. Pavlak, Purdue University"
 * @author William Desprats
 */
public class LibrationOrbitMultipleShooter extends AbstractMultipleShooting {

    /** CR3BPSystem in which the Libration orbit is considered. */
    private final CR3BPSystem cr3bp;

    /** Number of patch points. */
    private int npoints;

    /** Lagrangian Point from which the Libration orbit is associated. */
    private LagrangianPoints lPoint;

    /** Simple Constructor.
     * <p> Standard constructor for multiple shooting which can be used with the CR3BP model.</p>
     * @param initialGuessList initial patch points to be corrected.
     * @param propagatorList list of propagators associated to each patch point.
     * @param additionalEquations list of additional equations linked to propagatorList.
     * @param arcDuration initial guess of the duration of each arc.
     * @param cr3bp CR3BPSystem in which the Libration orbit is considered.
     * @param lPoint Lagrangian Point from which the Libration orbit is associated.
     * @param tolerance convergence tolerance on the constraint vector
     */
    public LibrationOrbitMultipleShooter(final List<SpacecraftState> initialGuessList, final List<NumericalPropagator> propagatorList,
                                          final List<AdditionalEquations> additionalEquations, final double arcDuration,
                                          final CR3BPSystem cr3bp, final LagrangianPoints lPoint, final double tolerance) {
        super(initialGuessList, propagatorList, additionalEquations, arcDuration, tolerance);
        this.cr3bp = cr3bp;
        this.lPoint = lPoint;
        this.npoints = initialGuessList.size();
        setClosedOrbitConstraint(true);
    }

    /** {@inheritDoc} */
    protected SpacecraftState getAugmentedInitialState(final SpacecraftState initialState,
                                                       final AdditionalEquations additionalEquation) {
        return ((EpochDerivativesEquations) additionalEquation).setInitialJacobians(initialState);
    }

    /** {@inheritDoc} */
    public double[][] computeAdditionalJacobianMatrix(final List<SpacecraftState> propagatedSP) {

        final Map<Integer, Double> mapConstraints = getConstraintsMap();

        // Number of additional constraints
        final int n = 6 + mapConstraints.size();

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



        final boolean[] freePoints = getFreePatchPointMap();
        final List<SpacecraftState> patchPoints = getPatchedSpacecraftState();

        final Frame outputFrame = cr3bp.getRotatingFrame();

        final AbsolutePVCoordinates apv1 = patchPoints.get(0).getAbsPVA();
        final AbsolutePVCoordinates apvn = patchPoints.get(npoints - 1).getAbsPVA();
        final Transform t1 = apv1.getFrame().getTransformTo(outputFrame, apv1.getDate());
        final Transform t2 = apvn.getFrame().getTransformTo(outputFrame, apvn.getDate());

        final PVCoordinates[] pvs = new PVCoordinates[6];
        pvs[0] = new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO);
        pvs[1] = new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO);
        pvs[2] = new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO);
        pvs[3] = new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_I);
        pvs[4] = new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_J);
        pvs[5] = new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_K);

        int index = 0;
        for (int i = 0; i < 6; i++) {
            if (freePoints[i]) {
                final PVCoordinates pv1i = t1.transformPVCoordinates(pvs[i]);
                for (int j = 0; j < 3; j++) {
                    M[  j  ][index] = -pv1i.getPosition().toArray()[j];
                    M[j + 3][index] = -pv1i.getVelocity().toArray()[j];
                }
                index++;
            }
        }

        index = ncolumns - 1;
        for (int i = 0; i < 6; i++) { // filling from the end
            if (freePoints[freePoints.length - 1 - i]) {
                final PVCoordinates pvni = t2.transformPVCoordinates(pvs[5 - i]);
                for (int j = 0; j < 3; j++) {
                    M[  j  ][index] = pvni.getPosition().toArray()[j];
                    M[j + 3][index] = pvni.getVelocity().toArray()[j];

                }
                index--;
            }
        }

        int k = 6;
        for (int indexC : mapConstraints.keySet()) {
            M[k][indexC] = 1;
            k++;
        }

        return M;
    }

    /** {@inheritDoc} */
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
        // Number of additional constraints
        final int n = 6 + mapConstraints.size();

        final List<SpacecraftState> patchedSpacecraftStates = getPatchedSpacecraftState();

        final double[] fxAdditionnal = new double[n];

        final AbsolutePVCoordinates apv1i = patchedSpacecraftStates.get(0).getAbsPVA();
        final AbsolutePVCoordinates apvni = patchedSpacecraftStates.get(npoints - 1).getAbsPVA();

        final PVCoordinates apv0 = getPVLframe(apv1i);
        final PVCoordinates apvn = getPVLframe(apvni);


        fxAdditionnal[0] = apvn.getPosition().getX() - apv0.getPosition().getX();
        fxAdditionnal[1] = apvn.getPosition().getY() - apv0.getPosition().getY();
        fxAdditionnal[2] = apvn.getPosition().getZ() - apv0.getPosition().getZ();
        fxAdditionnal[3] = apvn.getVelocity().getX() - apv0.getVelocity().getX();
        fxAdditionnal[4] = apvn.getVelocity().getY() - apv0.getVelocity().getY();
        fxAdditionnal[5] = apvn.getVelocity().getZ() - apv0.getVelocity().getZ();

        int i  = 6;
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

    /** Compute the PVCoordinates in the instantaneous rotating frame centered in lPoint.
     *  @param apv AbsolutePVCoordinates
     *  @return pv PVCoordinates
     */
    public PVCoordinates getPVLframe(final AbsolutePVCoordinates apv) {
        final Frame rotatingFrame = cr3bp.getRotatingFrame();
        final AbsoluteDate date = apv.getDate();
        final Transform t = apv.getFrame().getTransformTo(rotatingFrame, date);

        final PVCoordinates pvL = new PVCoordinates(cr3bp.getLPosition(lPoint), new Vector3D(0, 0, 0));
        final AbsolutePVCoordinates apvL = new AbsolutePVCoordinates(rotatingFrame, date, pvL);
        final AbsolutePVCoordinates apvLReal = cr3bp.getRealAPV(apvL, date, apv.getFrame());

        final Vector3D p = apv.getPosition().subtract(apvLReal.getPosition());
        final Vector3D v = apv.getVelocity().subtract(apvLReal.getVelocity());

        return t.transformPVCoordinates(new PVCoordinates(p, v));
    }

    /** {@inheritDoc} */
    protected RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        final Map<String, double[]> map = s.getAdditionalStates();
        RealMatrix phiM = null;
        for (String name : map.keySet()) {
            if ("derivatives".equals(name)) {
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
