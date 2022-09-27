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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;

import java.util.Locale;

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
 * propagator, user must use the
 * {@link Propagator#addAdditionalStateProvider(AdditionalStateProvider)}
 * method.
 * <p>
 * For a given propagated spacecraft {@code state}, the propagated state
 * covariance matrix is accessible through the method
 * {@link #getStateCovariance(SpacecraftState)}
 * <p>
 * It is possible to change the covariance frame by using the
 * {@link #changeCovarianceFrame(Orbit, Frame, Frame, RealMatrix, OrbitType, PositionAngle)}
 * method. This method is based on Equation (18) of <i>Covariance
 * Transformations for Satellite Flight Dynamics Operations</i>
 * by David A. Vallado. It is important to highlight that the frames
 * must be inertial frames.
 * <p>
 * Finally, covariance orbit type can be changed using the
 * {@link #changeCovarianceType(Orbit, OrbitType, PositionAngle, OrbitType, PositionAngle, RealMatrix)}
 * method.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.3
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
     * @param harvester matrix harvester as returned by {@code propagator.setupMatricesComputation(stmName, null, null)}
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
        covInit = changeCovarianceType(initialState.getOrbit(),
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
        propCov = changeCovarianceType(state.getOrbit(),
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
     * {@link #changeCovarianceFrame(Orbit, Frame, Frame, RealMatrix, OrbitType, PositionAngle)}
     * method.
     * <p>
     * It is also possible to change the covariance orbit type by
     * using the {@link #changeCovarianceType(Orbit, OrbitType, PositionAngle, OrbitType, PositionAngle, RealMatrix)}
     * method.
     * @param state spacecraft state to which the covariance matrix should correspond
     * @return the state covariance matrix
     * @see #getStateCovariance(SpacecraftState, Frame)
     * @see #getStateCovariance(SpacecraftState, OrbitType, PositionAngle)
     */
    public RealMatrix getStateCovariance(final SpacecraftState state) {

        // Get the current propagated covariance
        final RealMatrix covariance = toRealMatrix(state.getAdditionalState(additionalName));

        // Return the converted covariance
        return covariance;

    }

    /** Get the state covariance matrix (6x6 dimension) expressed in a given frame.
     * <p>
     * The output covariance matrix is expressed in the
     * same orbit type as {@link #getCovarianceOrbitType()}.
     * <p>
     * It is also possible to change the covariance orbit type by
     * using the {@link #changeCovarianceType(Orbit, OrbitType, PositionAngle, OrbitType, PositionAngle, RealMatrix)}
     * method.
     * @param state spacecraft state to which the covariance matrix should correspond
     * @param frame output frame for which the output covariance matrix must be expressed
     *        (must be inertial)
     * @return the state covariance matrix expressed in <code>frame</code>
     * @see #getStateCovariance(SpacecraftState)
     * @see #getStateCovariance(SpacecraftState, OrbitType, PositionAngle)
     */
    public RealMatrix getStateCovariance(final SpacecraftState state, final Frame frame) {

        // Get the current propagated covariance
        final RealMatrix covariance = toRealMatrix(state.getAdditionalState(additionalName));

        // Return the converted covariance
        return changeCovarianceFrame(state.getOrbit(), state.getFrame(), frame, covariance, covOrbitType, covAngleType);

    }

    /**
     * Convert the covariance matrix from a {@link LOFType commonly used local orbital frame} to another
     * {@link LOFType commonly used local orbital frame}
     * <p>
     * The transformation is based on Equation (20) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado".
     * <p>
     * As the frame transformation must be performed with the covariance expressed in Cartesian elements, both the orbit
     * and position angle types of the input covariance must be provided.
     *
     * @param orbit        orbit to which the covariance matrix should correspond
     * @param lofIn        the local orbital frame in which the input covariance matrix is expressed.
     * @param lofOut       the target local orbital frame.
     * @param pivotFrame   the pivot frame (must be inertial).
     * @param inputCov     input covariance
     * @param covOrbitType orbit type of the covariance matrix
     * @param covAngleType position angle type of the covariance matrix (not used if covOrbitType equals
     *                     {@code CARTESIAN})
     * @return the covariance matrix expressed in the target frame
     */
    public static RealMatrix changeCovarianceFrame(final Orbit orbit,
                                                   final LOFType lofIn, final LOFType lofOut, final Frame pivotFrame,
                                                   final RealMatrix inputCov,
                                                   final OrbitType covOrbitType, final PositionAngle covAngleType) {

        // In case the input and output local orbital frame type are the same
        if (lofIn.equals(lofOut)) {
            return inputCov;
        }

        // Pivot frame is inertial
        if (pivotFrame.isPseudoInertial()) {
            // Convert input matrix to Cartesian parameters in input frame
            final RealMatrix cartesianCovarianceIn = changeCovarianceType(orbit, covOrbitType, covAngleType,
                                                                          OrbitType.CARTESIAN, PositionAngle.MEAN,
                                                                          inputCov);

            // Compute rotation matrix from lofIn to frameOut

            /*
            final Rotation rotationFromLofInToFrameOut =
                    lofIn.rotationFromInertial(orbit.getPVCoordinates()).revert();

            System.out.println("rotationFromLofInToFrameOut.revert");
            printMatrix(new BlockRealMatrix(rotationFromLofInToFrameOut.revert().getMatrix()));

            final Rotation rotationFromPivotFrameToLofOut =
                    lofIn.rotationFromInertial(orbit.getPVCoordinates());

            System.out.println("rotationFromPivotFrameToLofOut");
            printMatrix(new BlockRealMatrix(rotationFromPivotFrameToLofOut.getMatrix()));

            final Rotation rotationFromLofInToLofOut =
                    rotationFromPivotFrameToLofOut.compose(rotationFromLofInToFrameOut,
                                                           RotationConvention.VECTOR_OPERATOR);

             */

            final Rotation rotationFromLofInToLofOut = LOFType.rotationFromLOFInToLOFOut(lofIn, lofOut,
                                                                                         orbit.getPVCoordinates(
                                                                                         ));

            // Builds the matrix to perform covariance transformation
            final RealMatrix transformationMatrix = buildTransformationMatrixFromRotation(rotationFromLofInToLofOut);

            System.out.println("transformationMatrix");
            printMatrix(transformationMatrix);

            // Get the Cartesian covariance matrix converted to frameOut
            final RealMatrix cartesianCovarianceOut =
                    transformationMatrix.multiply(cartesianCovarianceIn.multiplyTransposed(transformationMatrix));

            // Convert orbit frame to output frame
            // final Orbit outOrbit = new CartesianOrbit(orbit.getPVCoordinates(frameOut), frameOut, orbit.getMu());

            // Convert output Cartesian matrix to initial orbit type and position angle
            return changeCovarianceType(orbit, OrbitType.CARTESIAN, PositionAngle.MEAN,
                                        covOrbitType, covAngleType, cartesianCovarianceOut);

        }

        // Pivot frame is not inertial
        else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, pivotFrame.getName());
        }
    }

    public static void printMatrix(final RealMatrix covariance) {

        // Create a string builder
        final StringBuilder covToPrint = new StringBuilder();
        for (int row = 0; row < covariance.getRowDimension(); row++) {
            for (int column = 0; column < covariance.getColumnDimension(); column++) {
                covToPrint.append(String.format(Locale.US, "%16.16e", covariance.getEntry(row, column)));
                covToPrint.append(" ");
            }
            covToPrint.append("\n");
        }

        // Print
        System.out.println(covToPrint);

    }

    /**
     * Get the covariance matrix in another orbit type.
     *
     * @param orbit        orbit to which the covariance matrix should correspond
     * @param inOrbitType  initial orbit type of the state covariance matrix
     * @param inAngleType  initial position angle type of the state covariance matrix
     * @param outOrbitType target orbit type of the state covariance matrix
     * @param outAngleType target position angle type of the state covariance matrix
     * @param inputCov     input covariance
     *
     * @return the covariance expressed in the target orbit type with the target position angle
     */
    public static RealMatrix changeCovarianceType(final Orbit orbit,
            final OrbitType inOrbitType, final PositionAngle inAngleType,
            final OrbitType outOrbitType, final PositionAngle outAngleType,
            final RealMatrix inputCov) {

        // Notations:
        // I: Input orbit type
        // O: Output orbit type
        // C: Cartesian parameters

        // Compute dOutputdCartesian
        final double[][] aOC               = new double[STATE_DIMENSION][STATE_DIMENSION];
        final Orbit      orbitInOutputType = outOrbitType.convertType(orbit);
        orbitInOutputType.getJacobianWrtCartesian(outAngleType, aOC);
        final RealMatrix dOdC = new Array2DRowRealMatrix(aOC, false);

        // Compute dCartesiandInput
        final double[][] aCI              = new double[STATE_DIMENSION][STATE_DIMENSION];
        final Orbit      orbitInInputType = inOrbitType.convertType(orbit);
        orbitInInputType.getJacobianWrtParameters(inAngleType, aCI);
        final RealMatrix dCdI = new Array2DRowRealMatrix(aCI, false);

        // Compute dOutputdInput
        final RealMatrix dOdI = dOdC.multiply(dCdI);

        // Conversion of the state covariance matrix in target orbit type
        final RealMatrix outputCov = dOdI.multiply(inputCov.multiplyTransposed(dOdI));

        // Return the converted covariance
        return outputCov;

    }

    private static RealMatrix buildTransformationMatrixFromRotation(final Rotation rotation) {

        final double[][] rotationMatrixData = rotation.getMatrix();

        final RealMatrix transformationMatrix = MatrixUtils.createRealMatrix(6, 6);

        // Fills in the upper left and lower right blocks with the rotation
        transformationMatrix.setSubMatrix(rotationMatrixData, 0, 0);
        transformationMatrix.setSubMatrix(rotationMatrixData, 3, 3);

        return transformationMatrix;
    }

    /**
     * Convert the covariance matrix from a {@link LOFType commonly used local orbital frame} to a {@link Frame frame}.
     * <p>
     * The transformation is based on Equation (20) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado".
     * <p>
     * As the frame transformation must be performed with the covariance expressed in Cartesian elements, both the orbit
     * and position angle types of the input covariance must be provided.
     *
     * @param orbit        orbit to which the covariance matrix should correspond
     * @param lofIn        the local orbital frame in which the input covariance matrix is expressed (must be inertial)
     * @param frameOut     the target frame (must be inertial)
     * @param inputCov     input covariance
     * @param covOrbitType orbit type of the covariance matrix
     * @param covAngleType position angle type of the covariance matrix (not used if covOrbitType equals
     *                     {@code CARTESIAN})
     * @return the covariance matrix expressed in the target frame
     */
    public static RealMatrix changeCovarianceFrame(final Orbit orbit,
                                                   final LOFType lofIn, final Frame frameOut,
                                                   final RealMatrix inputCov,
                                                   final OrbitType covOrbitType, final PositionAngle covAngleType) {

        // Input frame is inertial
        if (frameOut.isPseudoInertial()) {

            // Convert input matrix to Cartesian parameters in input frame
            final RealMatrix cartesianCovarianceIn = changeCovarianceType(orbit, covOrbitType, covAngleType,
                                                                          OrbitType.CARTESIAN, PositionAngle.MEAN,
                                                                          inputCov);

            // Compute rotation matrix from lofIn to frameOut
            final Rotation rotationFromLofInToFrameOut =
                    lofIn.rotationFromInertial(orbit.getPVCoordinates(frameOut)).revert();

            // Builds the matrix to perform covariance transformation
            final RealMatrix transformationMatrix = buildTransformationMatrixFromRotation(rotationFromLofInToFrameOut);

            // Get the Cartesian covariance matrix converted to frameOut
            final RealMatrix cartesianCovarianceOut =
                    transformationMatrix.multiply(cartesianCovarianceIn.multiplyTransposed(transformationMatrix));

            // Convert orbit frame to output frame
            // final Orbit outOrbit = new CartesianOrbit(orbit.getPVCoordinates(frameOut), frameOut, orbit.getMu());

            // Convert output Cartesian matrix to initial orbit type and position angle
            return changeCovarianceType(orbit, OrbitType.CARTESIAN, PositionAngle.MEAN,
                                        covOrbitType, covAngleType, cartesianCovarianceOut);

        }

        // Output frame is not inertial
        else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, frameOut.getName());
        }
    }

    /**
     * Convert the covariance matrix from a {@link Frame frame} to a {@link LOFType commonly used local orbital frame}.
     * <p>
     * The transformation is based on Equation (20) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado".
     * <p>
     * As the frame transformation must be performed with the covariance expressed in Cartesian elements, both the orbit
     * and position angle types of the input covariance must be provided.
     *
     * @param orbit        orbit to which the covariance matrix should correspond
     * @param frameIn      the frame in which the input covariance matrix is expressed (must be inertial)
     * @param lofOut       the target local orbital frame
     * @param inputCov     input covariance
     * @param covOrbitType orbit type of the covariance matrix
     * @param covAngleType position angle type of the covariance matrix (not used if covOrbitType equals
     *                     {@code CARTESIAN})
     * @return the covariance matrix expressed in the target frame
     */
    public static RealMatrix changeCovarianceFrame(final Orbit orbit,
                                                   final Frame frameIn, final LOFType lofOut,
                                                   final RealMatrix inputCov,
                                                   final OrbitType covOrbitType, final PositionAngle covAngleType) {

        // Input frame is inertial
        if (frameIn.isPseudoInertial()) {

            // Convert input matrix to Cartesian parameters in input frame
            final RealMatrix cartesianCovarianceIn = changeCovarianceType(orbit, covOrbitType, covAngleType,
                                                                          OrbitType.CARTESIAN, PositionAngle.MEAN,
                                                                          inputCov);

            // Compute rotation matrix from frameIn to lofOut
            final Rotation rotationFromFrameInToLofOut = lofOut.rotationFromInertial(orbit.getPVCoordinates(frameIn));

            // Builds the matrix to perform covariance transformation
            final RealMatrix transformationMatrix = buildTransformationMatrixFromRotation(rotationFromFrameInToLofOut);

            // Get the Cartesian covariance matrix converted to frameOut
            final RealMatrix cartesianCovarianceOut =
                    transformationMatrix.multiply(cartesianCovarianceIn.multiplyTransposed(transformationMatrix));

            // Convert orbit frame to output frame
            // final Orbit outOrbit = new CartesianOrbit(orbit.getPVCoordinates(frameIn), frameIn, orbit.getMu());

            // Convert output Cartesian matrix to initial orbit type and position angle
            return changeCovarianceType(orbit, OrbitType.CARTESIAN, PositionAngle.MEAN,
                                        covOrbitType, covAngleType, cartesianCovarianceOut);

        }

        // Output frame is not inertial
        else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, frameIn.getName());
        }
    }

    /** Get the covariance matrix in another frame.
     * <p>
     * The transformation is based on Equation (18) of
     * "Covariance Transformations for Satellite Flight Dynamics Operations"
     * by David A. Vallado".
     * <p>
     * As the frame transformation must be performed with the covariance
     * expressed in Cartesian elements, both the orbit and position angle
     * types of the input covariance must be provided.
     * @param orbit orbit to which the covariance matrix should correspond
     * @param frameIn the frame in which the input covariance matrix is expressed (must be inertial)
     * @param frameOut the target frame (must be inertial)
     * @param inputCov input covariance
     * @param covOrbitType orbit type of the covariance matrix
     * @param covAngleType position angle type of the covariance matrix
     *        (not used if covOrbitType equals {@code CARTESIAN})
     * @return the covariance matrix expressed in the target frame
     */
    public static RealMatrix changeCovarianceFrame(final Orbit orbit,
                                                   final Frame frameIn, final Frame frameOut,
                                                   final RealMatrix inputCov,
                                                   final OrbitType covOrbitType, final PositionAngle covAngleType) {

        // Convert input matrix to Cartesian parameters in input frame
        final RealMatrix cartesianCovarianceIn = changeCovarianceType(orbit, covOrbitType, covAngleType,
                                                                      OrbitType.CARTESIAN, PositionAngle.MEAN,
                                                                      inputCov);

        // Get the transform from the covariance frame to the output frame
        final Transform inToOut = frameIn.getTransformTo(frameOut, orbit.getDate());

        // Get the Jacobian of the transform
        final double[][] jacobian = new double[STATE_DIMENSION][STATE_DIMENSION];
        inToOut.getJacobian(CartesianDerivativesFilter.USE_PV, jacobian);

        // Matrix to perform the covariance transformation
        final RealMatrix j = new Array2DRowRealMatrix(jacobian, false);

        // Get the Cartesian covariance matrix converted to frameOut
        final RealMatrix cartesianCovarianceOut = j.multiply(cartesianCovarianceIn.multiplyTransposed(j));

        // Convert orbit frame to output frame
        // FIXME The line below can be safely removed without changing the tests results
        // final Orbit outOrbit = new CartesianOrbit(orbit.getPVCoordinates(frameOut), frameOut, orbit.getMu());

        // Convert output Cartesian matrix to initial orbit type and position angle
        return changeCovarianceType(orbit, OrbitType.CARTESIAN, PositionAngle.MEAN,
                                    covOrbitType, covAngleType, cartesianCovarianceOut);

    }

    /**
     * Get the state covariance matrix (6x6 dimension) expressed in a given orbit type.
     *
     * @param state     spacecraft state to which the covariance matrix should correspond
     * @param orbitType output orbit type
     * @param angleType output position angle (not used if orbitType equals {@code CARTESIAN})
     *
     * @return the state covariance matrix in <code>orbitType</code> and <code>angleType</code>
     *
     * @see #getStateCovariance(SpacecraftState)
     * @see #getStateCovariance(SpacecraftState, Frame)
     */
    public RealMatrix getStateCovariance(final SpacecraftState state, final OrbitType orbitType,
            final PositionAngle angleType) {

        // Get the current propagated covariance
        final RealMatrix covariance = toRealMatrix(state.getAdditionalState(additionalName));

        // Return the converted covariance
        return changeCovarianceType(state.getOrbit(), covOrbitType, covAngleType, orbitType, angleType, covariance);

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
