/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;

/**
 * Additional state provider for state covariance matrix.
 * <p>
 * This additional state provider allows computing a propagated
 * covariance matrix based on a user defined input state covariance
 * matrix. The computation of the propagated covariance matrix uses
 * the State Transition Matrix between the propagated spacecraft state
 * and the initial state. As a result, the user must define the name
 * {@link #stmName of the provider for the State Transition Matrix}.
 * <p>
 * As the State Transition Matrix and the input state covariance
 * matrix can be expressed in different orbit types, the user must
 * specify both orbit types when building the covariance provider.
 * In addition, the position angle used in both matrices must also
 * be specified.
 * <p>
 * In order to add this additional state provider to an orbit
 * propagtor, user must use the
 * {@link Propagator#addAdditionalStateProvider(AdditionalStateProvider)}
 * method.
 * <p>
 * For a given propagated spacecraft {@code state}, the propagated state
 * covariance matrix is accessible through the method
 * {@link #getStateCovariance(SpacecraftState)}
 * <p>
 * It is possible to change the covariance frame by using the
 * {@link #changeCovarianceFrame(AbsoluteDate, Frame, Frame, RealMatrix)}
 * method. This method is based on Equation (16) of <i>Covariance
 * Transformations for Satellite Flight Dynamics Operations</i>
 * by David A. Vallado.
 * <p>
 * Finally, covariance orbit type can be changed using the
 * {@link #changeCovarianceType(SpacecraftState, OrbitType, PositionAngle, OrbitType, PositionAngle, RealMatrix)}
 * method.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.2
 */
public class StateCovarianceMatrixProvider implements AdditionalStateProvider {

    /** Dimension of the state. */
    private static final int STATE_DIMENSION = 6;

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Matrix harvester to access the State Transition Matrix. */
    private final MatricesHarvester harvester;

    /** Name of the additional state. */
    private final String additionalName;

    /** Orbit type used for the State Transition Matrix. */
    private final OrbitType stmOrbitType;

    /** Position angle used for State Transition Matrix. */
    private final PositionAngle stmAngleType;

    /** Orbit type for the covariance matrix. */
    private final OrbitType covOrbitType;

    /** Position angle used for the covariance matrix. */
    private final PositionAngle covAngleType;

    /** Initial state covariance matrix. */
    private RealMatrix covInit;

    /**
     * Constructor.
     * @param additionalName name of the additional state
     * @param stmName name of the state for State Transition Matrix
     * @param harvester matrix harvester has returned by {@code propagator.setupMatricesComputation(stmName, null, null)}
     * @param stmOrbitType orbit type used for the State Transition Matrix computation
     * @param stmAngleType position angle used for State Transition Matrix computation
     *        (not used if stmOrbitType equals {@code CARTESIAN})
     * @param covInit initial state covariance matrix (6x6 dimension)
     * @param covOrbitType orbit type for the covariance matrix
     * @param covAngleType position angle used for the covariance matrix
     *        (not used if covOrbitType equals {@code CARTESIAN})
     */
    public StateCovarianceMatrixProvider(final String additionalName, final String stmName,
                                         final MatricesHarvester harvester,
                                         final OrbitType stmOrbitType, final PositionAngle stmAngleType,
                                         final RealMatrix covInit,
                                         final OrbitType covOrbitType, final PositionAngle covAngleType) {
        // Initialize fields
        this.additionalName = additionalName;
        this.stmName        = stmName;
        this.harvester      = harvester;
        this.covInit        = covInit;
        this.stmOrbitType   = stmOrbitType;
        this.stmAngleType   = stmAngleType;
        this.covOrbitType   = covOrbitType;
        this.covAngleType   = covAngleType;
    }

    /** Get the orbit type in which the covariance matrix is expressed.
     * @return the orbit type
     */
    public OrbitType getCovarianceOrbitType() {
        return covOrbitType;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return additionalName;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        // Convert the initial covariance matrix in the same orbit type as the STM
        covInit = changeCovarianceType(initialState,
                                       covOrbitType, covAngleType,
                                       stmOrbitType, stmAngleType,
                                       covInit);
    }

    /** {@inheritDoc}
     * <p>
     * The covariance matrix can be computed only if the State Transition Matrix state is available.
     * </p>
     */
    @Override
    public boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalState(stmName);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {

        // State transition matrix for the input state
        final RealMatrix dYdY0 = harvester.getStateTransitionMatrix(state);

        // Compute the propagated covariance matrix
        RealMatrix propCov = dYdY0.multiply(covInit.multiplyTransposed(dYdY0));
        // Update to the user defined type
        propCov = changeCovarianceType(state,
                                       stmOrbitType, stmAngleType,
                                       covOrbitType, covAngleType,
                                       propCov);

        // Return the propagated covariance matrix
        return toArray(propCov);

    }

    /** Get the state covariance matrix (6x6 dimension).
     * <p>
     * The output covariance matrix is expressed in the
     * same orbit type as {@link #getCovarianceOrbitType()}.
     * <p>
     * It is possible to change the covariance frame by using the
     * {@link #changeCovarianceFrame(AbsoluteDate, Frame, Frame, RealMatrix)}
     * method.
     * <p>
     * It is also possible to change the covariance orbit type by
     * using the {@link #changeCovarianceType(SpacecraftState, OrbitType, PositionAngle,
     *                                        OrbitType, PositionAngle, RealMatrix)}
     * method.
     * @param state spacecraft state to which the covariance matrix should correspond
     * @return the state covariance matrix
     */
    public RealMatrix getStateCovariance(final SpacecraftState state) {

        // Get the current propagated covariance
        final RealMatrix covariance = toRealMatrix(state.getAdditionalState(additionalName));

        // Return the converted covariance
        return covariance;

    }

    /** Get the covariance matrix in another frame.
     * <p>
     * The transformation is based on Equation (16) of
     * "Covariance Transformations for Satellite Flight Dynamics Operations"
     * by David A. Vallado.
     * </p>
     * @param date covariance epoch
     * @param frameIn the frame in which the input covariance matrix is expressed
     * @param frameOut the target frame
     * @param inputCov input covariance
     * @return the covariance matrix expressed in the target frame
     */
    public static RealMatrix changeCovarianceFrame(final AbsoluteDate date,
                                                   final Frame frameIn, final Frame frameOut,
                                                   final RealMatrix inputCov) {

        // Get the transform from the covariance frame to the output frame
        final Transform inToOut = frameIn.getTransformTo(frameOut, date);

        // Get the rotation from the transform
        final double[][] rotation = inToOut.getRotation().getMatrix();

        // Matrix to perform the covariance transformation
        final RealMatrix j = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);

        // Fill transformation matrix
        j.setSubMatrix(rotation, 0, 0);
        j.setSubMatrix(rotation, 3, 3);

        // Returns the covariance matrix converted to frameOut
        return j.multiply(inputCov.multiplyTransposed(j));

    }

    /** Get the covariance matrix in another orbit type.
     * @param state spacecraft state to which the covariance matrix should correspond
     * @param inOrbitType initial orbit type of the state covariance matrix
     * @param inAngleType initial position angle type of the state covariance matrix
     * @param outOrbitType target orbit type of the state covariance matrix
     * @param outAngleType target position angle type of the state covariance matrix
     * @param inputCov input covariance
     * @return the covariance expressed in the target orbit type with the
     *         target position angle
     */
    public static RealMatrix changeCovarianceType(final SpacecraftState state,
                                                  final OrbitType inOrbitType, final PositionAngle inAngleType,
                                                  final OrbitType outOrbitType, final PositionAngle outAngleType,
                                                  final RealMatrix inputCov) {

        // Notations:
        // I: Input orbit type
        // O: Output orbit type
        // C: Cartesian parameters

        // Compute dOutputdCartesian
        final double[][] aOC = new double[STATE_DIMENSION][STATE_DIMENSION];
        final Orbit orbitInOutputType = outOrbitType.convertType(state.getOrbit());
        orbitInOutputType.getJacobianWrtCartesian(outAngleType, aOC);
        final RealMatrix dOdC = new Array2DRowRealMatrix(aOC, false);

        // Compute dCartesiandInput
        final double[][] aCI = new double[STATE_DIMENSION][STATE_DIMENSION];
        final Orbit orbitInInputType = inOrbitType.convertType(state.getOrbit());
        orbitInInputType.getJacobianWrtParameters(inAngleType, aCI);
        final RealMatrix dCdI = new Array2DRowRealMatrix(aCI, false);

        // Compute dOutputdInput
        final RealMatrix dOdI = dOdC.multiply(dCdI);

        // Conversion of the state covariance matrix in target orbit type
        final RealMatrix outputCov = dOdI.multiply(inputCov.multiplyTransposed(dOdI));

        // Return the converted covariance
        return outputCov;

    }

    /** Convert an array to a matrix (6x6 dimension).
     * @param array input array
     * @return the corresponding matrix
     */
    private RealMatrix toRealMatrix(final double[] array) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                matrix.setEntry(i, j, array[index++]);
            }
        }
        return matrix;
    }

    /** Set the covariance data into an array.
     * @param covariance covariance matrix
     * @return an array containing the covariance data
     */
    private double[] toArray(final RealMatrix covariance) {
        final double[] array = new double[STATE_DIMENSION * STATE_DIMENSION];
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                array[index++] = covariance.getEntry(i, j);
            }
        }
        return array;
    }

}
