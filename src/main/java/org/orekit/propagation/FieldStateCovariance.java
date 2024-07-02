/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.linear.Array2DRowFieldMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.CartesianDerivativesFilter;

/**
 * This class is the representation of a covariance matrix at a given date.
 * <p>
 * Currently, the covariance only represents the orbital elements.
 * <p>
 * It is possible to change the covariance frame by using the {@link #changeCovarianceFrame(FieldOrbit, Frame)} or
 * {@link #changeCovarianceFrame(FieldOrbit, LOF)} method. These methods are based on Equations (18) and (20) of
 * <i>Covariance Transformations for Satellite Flight Dynamics Operations</i> by David A. SVallado.
 * <p>
 * Finally, covariance orbit type can be changed using the
 * {@link #changeCovarianceType(FieldOrbit, OrbitType, PositionAngleType) changeCovarianceType(FieldOrbit, OrbitType,
 * PositionAngle)} method.
 * </p>
 *
 * @author Bryan Cazabonne
 * @author Vincent Cucchietti
 * @since 12.0
 * @param <T> type of the field elements
 */
public class FieldStateCovariance<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T> {

    /** State dimension. */
    private static final int STATE_DIMENSION = 6;

    /** Default position angle for covariance expressed in cartesian elements. */
    private static final PositionAngleType DEFAULT_POSITION_ANGLE = PositionAngleType.TRUE;

    /** Orbital covariance [6x6]. */
    private final FieldMatrix<T> orbitalCovariance;

    /** Covariance frame (can be null if LOF is defined). */
    private final Frame frame;

    /** Covariance LOF type (can be null if frame is defined). */
    private final LOF lof;

    /** Covariance epoch. */
    private final FieldAbsoluteDate<T> epoch;

    /** Covariance orbit type. */
    private final OrbitType orbitType;

    /** Covariance position angle type (not used if orbitType is CARTESIAN). */
    private final PositionAngleType angleType;

    /**
     * Constructor.
     *
     * @param orbitalCovariance 6x6 orbital parameters covariance
     * @param epoch epoch of the covariance
     * @param lof covariance LOF type
     */
    public FieldStateCovariance(final FieldMatrix<T> orbitalCovariance, final FieldAbsoluteDate<T> epoch,
                                final LOF lof) {
        this(orbitalCovariance, epoch, null, lof, OrbitType.CARTESIAN, DEFAULT_POSITION_ANGLE);
    }

    /**
     * Constructor.
     *
     * @param orbitalCovariance 6x6 orbital parameters covariance
     * @param epoch epoch of the covariance
     * @param covarianceFrame covariance frame (inertial or Earth fixed)
     * @param orbitType orbit type of the covariance (CARTESIAN if covarianceFrame is not pseudo-inertial)
     * @param angleType position angle type of the covariance (not used if orbitType is CARTESIAN)
     */
    public FieldStateCovariance(final FieldMatrix<T> orbitalCovariance, final FieldAbsoluteDate<T> epoch,
                                final Frame covarianceFrame,
                                final OrbitType orbitType, final PositionAngleType angleType) {
        this(orbitalCovariance, epoch, covarianceFrame, null, orbitType, angleType);
    }

    /**
     * Private constructor.
     *
     * @param orbitalCovariance 6x6 orbital parameters covariance
     * @param epoch epoch of the covariance
     * @param covarianceFrame covariance frame (inertial or Earth fixed)
     * @param lof covariance LOF type
     * @param orbitType orbit type of the covariance
     * @param angleType position angle type of the covariance (not used if orbitType is CARTESIAN)
     */
    private FieldStateCovariance(final FieldMatrix<T> orbitalCovariance, final FieldAbsoluteDate<T> epoch,
                                 final Frame covarianceFrame, final LOF lof,
                                 final OrbitType orbitType, final PositionAngleType angleType) {

        StateCovariance.checkFrameAndOrbitTypeConsistency(covarianceFrame, orbitType);

        this.orbitalCovariance = orbitalCovariance;
        this.epoch             = epoch;
        this.frame             = covarianceFrame;
        this.lof               = lof;
        this.orbitType         = orbitType;
        this.angleType         = angleType;

    }

    /** {@inheritDoc}. */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return epoch;
    }

    /**
     * Get the covariance matrix.
     *
     * @return the covariance matrix
     */
    public FieldMatrix<T> getMatrix() {
        return orbitalCovariance;
    }

    /**
     * Get the covariance orbit type.
     *
     * @return the covariance orbit type
     */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /**
     * Get the covariance angle type.
     *
     * @return the covariance angle type
     */
    public PositionAngleType getPositionAngleType() {
        return angleType;
    }

    /**
     * Get the covariance frame.
     *
     * @return the covariance frame (can be null)
     *
     * @see #getLOF()
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * Get the covariance LOF type.
     *
     * @return the covariance LOF type (can be null)
     *
     * @see #getFrame()
     */
    public LOF getLOF() {
        return lof;
    }

    /**
     * Get the covariance matrix in another orbit type.
     * <p>
     * The covariance orbit type <b>cannot</b> be changed if the covariance
     * matrix is expressed in a {@link LOF local orbital frame} or a
     * non-pseudo inertial frame.
     * <p>
     * As this type change uses the jacobian matrix of the transformation, it introduces a linear approximation.
     * Hence, the current covariance matrix <b>will not exactly match</b> the new linearized case and the
     * distribution will not follow a generalized Gaussian distribution anymore.
     * <p>
     * This is based on equation (1) to (6) from "Vallado, D. A. (2004). Covariance transformations for satellite flight
     * dynamics operations."
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param outOrbitType target orbit type of the state covariance matrix
     * @param outAngleType target position angle type of the state covariance matrix
     *
     * @return a new covariance state, expressed in the target orbit type with the target position angle
     *
     * @see #changeCovarianceFrame(FieldOrbit, Frame)
     */
    public FieldStateCovariance<T> changeCovarianceType(final FieldOrbit<T> orbit, final OrbitType outOrbitType,
                                                        final PositionAngleType outAngleType) {

        // Handle case where the covariance is already expressed in the output type
        if (outOrbitType == orbitType && (outAngleType == angleType || outOrbitType == OrbitType.CARTESIAN)) {
            if (lof == null) {
                return new FieldStateCovariance<>(orbitalCovariance, epoch, frame, orbitType, angleType);
            }
            else {
                return new FieldStateCovariance<>(orbitalCovariance, epoch, lof);
            }
        }

        // Check if the covariance is expressed in a celestial body frame
        if (frame != null) {

            // Check if the covariance is defined in an inertial frame
            if (frame.isPseudoInertial()) {
                return changeTypeAndCreate(orbit, epoch, frame, orbitType, angleType, outOrbitType, outAngleType,
                                           orbitalCovariance);
            }

            // The covariance is not defined in an inertial frame. The orbit type cannot be changed
            throw new OrekitException(OrekitMessages.CANNOT_CHANGE_COVARIANCE_TYPE_IF_DEFINED_IN_NON_INERTIAL_FRAME);

        }

        // The covariance is not expressed in a celestial body frame. The orbit type cannot be changed
        throw new OrekitException(OrekitMessages.CANNOT_CHANGE_COVARIANCE_TYPE_IF_DEFINED_IN_LOF);

    }

    /**
     * Get the covariance in a given local orbital frame.
     * <p>
     * Changing the covariance frame is a linear process, this method does not introduce approximation unless a change
     * in covariance orbit type is required.
     * <p>
     * This is based on equation (18) to (20) "from Vallado, D. A. (2004). Covariance transformations for satellite
     * flight dynamics operations."
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param lofOut output local orbital frame
     *
     * @return a new covariance state, expressed in the output local orbital frame
     */
    public FieldStateCovariance<T> changeCovarianceFrame(final FieldOrbit<T> orbit, final LOF lofOut) {

        // Verify current covariance frame
        if (lof != null) {

            // Change the covariance local orbital frame
            return changeFrameAndCreate(orbit, epoch, lof, lofOut, orbitalCovariance);

        } else {

            // Covariance is expressed in celestial body frame
            return changeFrameAndCreate(orbit, epoch, frame, lofOut, orbitalCovariance, orbitType, angleType);

        }

    }

    /**
     * Get the covariance in the output frame.
     * <p>
     * Changing the covariance frame is a linear process, this method does not introduce approximation unless a change
     * in covariance orbit type is required.
     * <p>
     * This is based on equation (18) to (20) "from Vallado, D. A. (2004). Covariance transformations for satellite
     * flight dynamics operations."
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param frameOut output frame
     *
     * @return a new covariance state, expressed in the output frame
     */
    public FieldStateCovariance<T> changeCovarianceFrame(final FieldOrbit<T> orbit, final Frame frameOut) {

        // Verify current covariance frame
        if (lof != null) {

            // Covariance is expressed in local orbital frame
            return changeFrameAndCreate(orbit, epoch, lof, frameOut, orbitalCovariance);

        } else {

            // Change covariance frame
            return changeFrameAndCreate(orbit, epoch, frame, frameOut, orbitalCovariance, orbitType, angleType);

        }

    }

    /**
     * Get a time-shifted covariance matrix.
     * <p>
     * The shifting model is a linearized, Keplerian one. In other words, it is based on a state transition matrix that
     * is computed assuming Keplerian motion.
     * <p>
     * Shifting is <em>not</em> intended as a replacement for proper covariance propagation, but should be sufficient
     * for small time shifts or coarse accuracy.
     *
     * @param field to which the elements belong
     * @param orbit orbit to which the covariance matrix should correspond
     * @param dt time shift in seconds
     *
     * @return a new covariance state, shifted with respect to the instance
     */
    public FieldStateCovariance<T> shiftedBy(final Field<T> field, final FieldOrbit<T> orbit, final T dt) {

        // Shifted orbit
        final FieldOrbit<T> shifted = orbit.shiftedBy(dt);

        // State covariance expressed in celestial body frame
        if (frame != null) {

            // State covariance expressed in a pseudo-inertial frame
            if (frame.isPseudoInertial()) {

                // Compute STM
                final FieldMatrix<T> stm = getStm(field, orbit, dt);

                // Convert covariance in STM type (i.e., Equinoctial elements)
                final FieldStateCovariance<T> inStmType = changeTypeAndCreate(orbit, epoch, frame, orbitType, angleType,
                                                                              OrbitType.EQUINOCTIAL, PositionAngleType.MEAN,
                                                                              orbitalCovariance);

                // Shift covariance by applying the STM
                final FieldMatrix<T> shiftedCov = stm.multiply(inStmType.getMatrix().multiplyTransposed(stm));

                // Restore the initial covariance type
                return changeTypeAndCreate(shifted, shifted.getDate(), frame,
                                           OrbitType.EQUINOCTIAL, PositionAngleType.MEAN,
                                           orbitType, angleType, shiftedCov);
            }

            // State covariance expressed in a non pseudo-inertial frame
            else {

                // Convert state covariance to orbit pseudo-inertial frame
                final FieldStateCovariance<T> inOrbitFrame = changeCovarianceFrame(orbit, orbit.getFrame());

                // Shift the state covariance
                final FieldStateCovariance<T> shiftedCovariance = inOrbitFrame.shiftedBy(field, orbit, dt);

                // Restore the initial covariance frame
                return shiftedCovariance.changeCovarianceFrame(shifted, frame);
            }
        }

        // State covariance expressed in a commonly used local orbital frame (LOF)
        else {

            // Convert state covariance to orbit pseudo-inertial frame
            final FieldStateCovariance<T> inOrbitFrame = changeCovarianceFrame(orbit, orbit.getFrame());

            // Shift the state covariance
            final FieldStateCovariance<T> shiftedCovariance = inOrbitFrame.shiftedBy(field, orbit, dt);

            // Restore the initial covariance frame
            return shiftedCovariance.changeCovarianceFrame(shifted, lof);
        }

    }

    /** Get new state covariance instance.
     * @return new state covariance instance.
     */
    public StateCovariance toStateCovariance() {

        // Extract data
        final T[][] data      = orbitalCovariance.getData();
        final int   rowDim    = data.length;
        final int   columnDim = data.length;

        // Convert to non-field version
        final double[][] realData = new double[rowDim][columnDim];
        for (int i = 0; i < rowDim; i++) {
            for (int j = 0; j < columnDim; j++) {
                realData[i][j] = data[i][j].getReal();
            }
        }
        final BlockRealMatrix realMatrix = new BlockRealMatrix(realData);
        final AbsoluteDate    realDate   = epoch.toAbsoluteDate();

        // Return new state covariance according to current state covariance definition
        if (frame == null) {
            return new StateCovariance(realMatrix, realDate, lof);
        }
        else {
            return new StateCovariance(realMatrix, realDate, frame, orbitType, angleType);
        }
    }

    /**
     * Create a covariance matrix in another orbit type.
     * <p>
     * As this type change uses the jacobian matrix of the transformation, it introduces a linear approximation.
     * Hence, the input covariance matrix <b>will not exactly match</b> the new linearized case and the
     * distribution will not follow a generalized Gaussian distribution anymore.
     * <p>
     * This is based on equation (1) to (6) from "Vallado, D. A. (2004). Covariance transformations for satellite flight
     * dynamics operations."
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param date covariance epoch
     * @param covFrame covariance frame
     * @param inOrbitType initial orbit type of the state covariance matrix
     * @param inAngleType initial position angle type of the state covariance matrix
     * @param outOrbitType target orbit type of the state covariance matrix
     * @param outAngleType target position angle type of the state covariance matrix
     * @param inputCov input covariance
     *
     * @return the covariance expressed in the target orbit type with the target position angle
     */
    private FieldStateCovariance<T> changeTypeAndCreate(final FieldOrbit<T> orbit,
                                                        final FieldAbsoluteDate<T> date,
                                                        final Frame covFrame,
                                                        final OrbitType inOrbitType,
                                                        final PositionAngleType inAngleType,
                                                        final OrbitType outOrbitType,
                                                        final PositionAngleType outAngleType,
                                                        final FieldMatrix<T> inputCov) {

        // Notations:
        // I: Input orbit type
        // O: Output orbit type
        // C: Cartesian parameters

        // Extract field
        final Field<T> field = date.getField();

        // Compute dOutputdCartesian
        final T[][]         aOC               = MathArrays.buildArray(field, STATE_DIMENSION, STATE_DIMENSION);
        final FieldOrbit<T> orbitInOutputType = outOrbitType.convertType(orbit);
        orbitInOutputType.getJacobianWrtCartesian(outAngleType, aOC);
        final FieldMatrix<T> dOdC = new Array2DRowFieldMatrix<>(aOC, false);

        // Compute dCartesiandInput
        final T[][]         aCI              = MathArrays.buildArray(field, STATE_DIMENSION, STATE_DIMENSION);
        final FieldOrbit<T> orbitInInputType = inOrbitType.convertType(orbit);
        orbitInInputType.getJacobianWrtParameters(inAngleType, aCI);
        final FieldMatrix<T> dCdI = new Array2DRowFieldMatrix<>(aCI, false);

        // Compute dOutputdInput
        final FieldMatrix<T> dOdI = dOdC.multiply(dCdI);

        // Conversion of the state covariance matrix in target orbit type
        final FieldMatrix<T> outputCov = dOdI.multiply(inputCov.multiplyTransposed(dOdI));

        // Return the converted covariance
        return new FieldStateCovariance<>(outputCov, date, covFrame, outOrbitType, outAngleType);

    }

    /**
     * Create a covariance matrix from a {@link LOF local orbital frame} to another
     * {@link LOF local orbital frame}.
     * <p>
     * Changing the covariance frame is a linear process, this method does not introduce approximation.
     * <p>
     * The transformation is based on Equation (18) to (20) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado.
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param date covariance epoch
     * @param lofIn the local orbital frame in which the input covariance matrix is expressed
     * @param lofOut the target local orbital frame
     * @param inputCartesianCov input covariance {@code CARTESIAN})
     *
     * @return the covariance matrix expressed in the target commonly used local orbital frame in cartesian elements
     */
    private FieldStateCovariance<T> changeFrameAndCreate(final FieldOrbit<T> orbit,
                                                         final FieldAbsoluteDate<T> date,
                                                         final LOF lofIn,
                                                         final LOF lofOut,
                                                         final FieldMatrix<T> inputCartesianCov) {

        // Builds the matrix to perform covariance transformation
        final FieldMatrix<T> jacobianFromLofInToLofOut =
                getJacobian(date.getField(),
                            LOF.transformFromLOFInToLOFOut(lofIn, lofOut, date, orbit.getPVCoordinates()));

        // Get the Cartesian covariance matrix converted to frameOut
        final FieldMatrix<T> cartesianCovarianceOut =
                jacobianFromLofInToLofOut.multiply(inputCartesianCov.multiplyTransposed(jacobianFromLofInToLofOut));

        // Output converted covariance
        return new FieldStateCovariance<>(cartesianCovarianceOut, date, lofOut);

    }

    /**
     * Convert the covariance matrix from a {@link Frame frame} to a {@link LOF local orbital frame}.
     * <p>
     * Changing the covariance frame is a linear process, this method does not introduce approximation unless a change
     * in covariance orbit type is required.
     * <p>
     * The transformation is based on Equation (18) to (20) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado.
     * <p>
     * As the frame transformation must be performed with the covariance expressed in Cartesian elements, both the orbit
     * and position angle types of the input covariance must be provided.
     * <p>
     * <b>The output covariance matrix will provided in Cartesian orbit type and not converted back to
     * its original expression (if input different from Cartesian elements).</b>
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param date covariance epoch
     * @param frameIn the frame in which the input covariance matrix is expressed. In case the frame is <b>not</b>
     * pseudo-inertial, the input covariance matrix is expected to be expressed in <b>Cartesian elements</b>.
     * @param lofOut the target local orbital frame
     * @param inputCov input covariance
     * @param covOrbitType orbit type of the covariance matrix (used if frameIn is pseudo-inertial)
     * @param covAngleType position angle type of the covariance matrix (used if frameIn is pseudo-inertial) (not used
     * if covOrbitType equals {@code CARTESIAN})
     * @return the covariance matrix expressed in the target local orbital frame in Cartesian elements
     * @throws OrekitException if input frame is <b>not</b> pseudo-inertial <b>and</b> the input covariance is
     * <b>not</b> expressed in Cartesian elements.
     */
    private FieldStateCovariance<T> changeFrameAndCreate(final FieldOrbit<T> orbit,
                                                         final FieldAbsoluteDate<T> date,
                                                         final Frame frameIn,
                                                         final LOF lofOut,
                                                         final FieldMatrix<T> inputCov,
                                                         final OrbitType covOrbitType,
                                                         final PositionAngleType covAngleType) {

        // Input frame is inertial
        if (frameIn.isPseudoInertial()) {

            // Convert input matrix to Cartesian parameters in input frame
            final FieldMatrix<T> cartesianCovarianceIn =
                    changeTypeAndCreate(orbit, date, frameIn, covOrbitType, covAngleType,
                                        OrbitType.CARTESIAN, PositionAngleType.MEAN,
                                        inputCov).getMatrix();

            // Builds the matrix to perform covariance transformation
            final FieldMatrix<T> jacobianFromFrameInToLofOut =
                    getJacobian(date.getField(), lofOut.transformFromInertial(date, orbit.getPVCoordinates(frameIn)));

            // Get the Cartesian covariance matrix converted to frameOut
            final FieldMatrix<T> cartesianCovarianceOut =
                    jacobianFromFrameInToLofOut.multiply(
                            cartesianCovarianceIn.multiplyTransposed(jacobianFromFrameInToLofOut));

            // Return converted covariance matrix expressed in cartesian elements
            return new FieldStateCovariance<>(cartesianCovarianceOut, date, lofOut);

        }

        // Input frame is not inertial so the covariance matrix is expected to be in cartesian elements
        else {

            // Method checkInputConsistency already verify that input covariance is defined in CARTESIAN type
            final Frame orbitInertialFrame = orbit.getFrame();

            // Compute rotation matrix from frameIn to orbit inertial frame
            final FieldMatrix<T> cartesianCovarianceInOrbitFrame =
                    changeFrameAndCreate(orbit, date, frameIn, orbitInertialFrame, inputCov,
                                         OrbitType.CARTESIAN, PositionAngleType.MEAN).getMatrix();

            // Convert from orbit inertial frame to lofOut
            return changeFrameAndCreate(orbit, date, orbitInertialFrame, lofOut, cartesianCovarianceInOrbitFrame,
                                        OrbitType.CARTESIAN, PositionAngleType.MEAN);

        }

    }

    /**
     * Convert the covariance matrix from a {@link LOF  local orbital frame} to a {@link Frame frame}.
     * <p>
     * Changing the covariance frame is a linear process, this method does not introduce approximation.
     * <p>
     * The transformation is based on Equation (18) to (20) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado.
     * <p>
     * The <u>input</u> covariance matrix is necessarily provided in <b>Cartesian orbit type</b>.
     * <p>
     * The <u>output</u> covariance matrix will be expressed in <b>Cartesian elements</b>.
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param date covariance epoch
     * @param lofIn the local orbital frame in which the input covariance matrix is expressed
     * @param frameOut the target frame
     * @param inputCartesianCov input covariance ({@code CARTESIAN})
     *
     * @return the covariance matrix expressed in the target frame in cartesian elements
     */
    private FieldStateCovariance<T> changeFrameAndCreate(final FieldOrbit<T> orbit,
                                                         final FieldAbsoluteDate<T> date,
                                                         final LOF lofIn,
                                                         final Frame frameOut,
                                                         final FieldMatrix<T> inputCartesianCov) {

        // Output frame is pseudo-inertial
        if (frameOut.isPseudoInertial()) {

            // Builds the matrix to perform covariance transformation
            final FieldMatrix<T> jacobianFromLofInToFrameOut =
                    getJacobian(date.getField(),
                                lofIn.transformFromInertial(date, orbit.getPVCoordinates(frameOut)).getInverse());

            // Transform covariance
            final FieldMatrix<T> transformedCovariance =
                    jacobianFromLofInToFrameOut.multiply(
                            inputCartesianCov.multiplyTransposed(jacobianFromLofInToFrameOut));

            // Get the Cartesian covariance matrix converted to frameOut
            return new FieldStateCovariance<>(transformedCovariance, date, frameOut, OrbitType.CARTESIAN,
                                              DEFAULT_POSITION_ANGLE);

        }

        // Output frame is not pseudo-inertial
        else {

            // Builds the matrix to perform covariance transformation
            final FieldMatrix<T> jacobianFromLofInToOrbitFrame =
                    getJacobian(date.getField(),
                                lofIn.transformFromInertial(date, orbit.getPVCoordinates()).getInverse());

            // Get the Cartesian covariance matrix converted to orbit inertial frame
            final FieldMatrix<T> cartesianCovarianceInOrbitFrame = jacobianFromLofInToOrbitFrame.multiply(
                    inputCartesianCov.multiplyTransposed(jacobianFromLofInToOrbitFrame));

            // Get the Cartesian covariance matrix converted to frameOut
            return changeFrameAndCreate(orbit, date, orbit.getFrame(), frameOut, cartesianCovarianceInOrbitFrame,
                                        OrbitType.CARTESIAN, PositionAngleType.MEAN);
        }

    }

    /**
     * Get the covariance matrix in another frame.
     * <p>
     * The transformation is based on Equation (18) of "Covariance Transformations for Satellite Flight Dynamics
     * Operations" by David A. Vallado.
     * <p>
     * Changing the covariance frame is a linear process, this method does not introduce approximation unless a change
     * in covariance orbit type is required.
     * <p>
     * As the frame transformation must be performed with the covariance expressed in Cartesian elements, both the orbit
     * and position angle types of the input covariance must be provided.
     * <p>
     * In case the <u>input</u> frame is <b>not</b> pseudo-inertial, the <u>input</u> covariance matrix is necessarily
     * expressed in <b>Cartesian elements</b>.
     * <p>
     * In case the <u>output</u> frame is <b>not</b> pseudo-inertial, the <u>output</u> covariance matrix will be
     * expressed in <b>Cartesian elements</b>.
     *
     * @param orbit orbit to which the covariance matrix should correspond
     * @param date covariance epoch
     * @param frameIn the frame in which the input covariance matrix is expressed
     * @param frameOut the target frame
     * @param inputCov input covariance
     * @param covOrbitType orbit type of the covariance matrix (used if frameIn is pseudo-inertial)
     * @param covAngleType position angle type of the covariance matrix (used if frameIn is pseudo-inertial) (<b>not</b>
     * used if covOrbitType equals {@code CARTESIAN})
     * @return the covariance matrix expressed in the target frame
     *
     * @throws OrekitException if input frame is <b>not</b> pseudo-inertial <b>and</b> the input covariance is
     * <b>not</b> expressed in cartesian elements.
     */
    private FieldStateCovariance<T> changeFrameAndCreate(final FieldOrbit<T> orbit,
                                                         final FieldAbsoluteDate<T> date,
                                                         final Frame frameIn,
                                                         final Frame frameOut,
                                                         final FieldMatrix<T> inputCov,
                                                         final OrbitType covOrbitType,
                                                         final PositionAngleType covAngleType) {

        // Get the transform from the covariance frame to the output frame
        final FieldTransform<T> inToOut = frameIn.getTransformTo(frameOut, orbit.getDate());

        // Matrix to perform the covariance transformation
        final FieldMatrix<T> j = getJacobian(date.getField(), inToOut);

        // Input frame pseudo-inertial
        if (frameIn.isPseudoInertial()) {

            // Convert input matrix to Cartesian parameters in input frame
            final FieldMatrix<T> cartesianCovarianceIn =
                    changeTypeAndCreate(orbit, date, frameIn, covOrbitType, covAngleType,
                                        OrbitType.CARTESIAN, PositionAngleType.MEAN,
                                        inputCov).getMatrix();

            // Get the Cartesian covariance matrix converted to frameOut
            final FieldMatrix<T> cartesianCovarianceOut = j.multiply(cartesianCovarianceIn.multiplyTransposed(j));

            // Output frame is pseudo-inertial
            if (frameOut.isPseudoInertial()) {

                // Convert output Cartesian matrix to initial orbit type and position angle
                return changeTypeAndCreate(orbit, date, frameOut, OrbitType.CARTESIAN, PositionAngleType.MEAN,
                                           covOrbitType, covAngleType, cartesianCovarianceOut);

            }

            // Output frame is not pseudo-inertial
            else {

                // Output Cartesian matrix
                return new FieldStateCovariance<>(cartesianCovarianceOut, date, frameOut, OrbitType.CARTESIAN,
                                                  DEFAULT_POSITION_ANGLE);

            }
        }

        // Input frame is not pseudo-inertial
        else {

            // Method checkInputConsistency already verify that input covariance is defined in CARTESIAN type

            // Convert covariance matrix to frameOut
            final FieldMatrix<T> covInFrameOut = j.multiply(inputCov.multiplyTransposed(j));

            // Output the Cartesian covariance matrix converted to frameOut
            return new FieldStateCovariance<>(covInFrameOut, date, frameOut, OrbitType.CARTESIAN, DEFAULT_POSITION_ANGLE);

        }

    }

    /**
     * Builds the matrix to perform covariance frame transformation.
     *
     * @param field to which the elements belong
     * @param transform input transformation
     *
     * @return the matrix to perform the covariance frame transformation
     */
    private FieldMatrix<T> getJacobian(final Field<T> field, final FieldTransform<T> transform) {
        // Get the Jacobian of the transform
        final T[][] jacobian = MathArrays.buildArray(field, STATE_DIMENSION, STATE_DIMENSION);
        transform.getJacobian(CartesianDerivativesFilter.USE_PV, jacobian);
        // Return
        return new Array2DRowFieldMatrix<>(jacobian, false);

    }

    /**
     * Get the state transition matrix considering Keplerian contribution only.
     *
     * @param field to which the elements belong
     * @param initialOrbit orbit to which the initial covariance matrix should correspond
     * @param dt time difference between the two orbits
     *
     * @return the state transition matrix used to shift the covariance matrix
     */
    private FieldMatrix<T> getStm(final Field<T> field, final FieldOrbit<T> initialOrbit, final T dt) {

        // initialize the STM
        final FieldMatrix<T> stm = MatrixUtils.createFieldIdentityMatrix(field, STATE_DIMENSION);

        // State transition matrix using Keplerian contribution only
        final T mu           = initialOrbit.getMu();
        final T sma          = initialOrbit.getA();
        final T contribution = mu.divide(sma.pow(5)).sqrt().multiply(dt).multiply(-1.5);
        stm.setEntry(5, 0, contribution);

        // Return
        return stm;

    }

}
