/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.orbits;

import java.io.Serializable;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.QRDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * This class handles orbital parameters without date.

 * <p>
 * The aim of this class is to separate the orbital parameters from the date
 * for cases where dates are managed elsewhere. This occurs for example during
 * numerical integration and interpolation because date is the free parameter
 * whereas the orbital parameters are bound to either differential or
 * interpolation equations.</p>

 * <p>
 * For user convenience, both the Cartesian and the equinoctial elements
 * are provided by this class, regardless of the canonical representation
 * implemented in the derived class (which may be classical keplerian
 * elements for example).
 * </p>
 * <p>
 * The parameters are defined in a frame specified by the user. It is important
 * to make sure this frame is consistent: it probably is inertial and centered
 * on the central body. This information is used for example by some
 * force models.
 * </p>
 * <p>
 * The object <code>OrbitalParameters</code> is guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public abstract class Orbit implements TimeStamped, Serializable, PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 438733454597999578L;

    /** Frame in which are defined the orbital parameters. */
    private final Frame frame;

    /** Date of the orbital parameters. */
    private final AbsoluteDate date;

    /** Value of mu used to compute position and velocity (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Computed PVCoordinates. */
    private PVCoordinates pvCoordinates;

    /** Jacobian of the orbital parameters with mean angle with respect to the Cartesian coordinates. */
    private transient double[][] jacobianMeanWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with mean angle. */
    private transient double[][] jacobianWrtParametersMean;

    /** Jacobian of the orbital parameters with eccentric angle with respect to the Cartesian coordinates. */
    private transient double[][] jacobianEccentricWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with eccentric angle. */
    private transient double[][] jacobianWrtParametersEccentric;

    /** Jacobian of the orbital parameters with true angle with respect to the Cartesian coordinates. */
    private transient double[][] jacobianTrueWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with true angle. */
    private transient double[][] jacobianWrtParametersTrue;

    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    protected Orbit(final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        ensurePseudoInertialFrame(frame);
        this.date                      = date;
        this.mu                        = mu;
        this.pvCoordinates             = null;
        this.frame                     = frame;
        jacobianMeanWrtCartesian       = null;
        jacobianWrtParametersMean      = null;
        jacobianEccentricWrtCartesian  = null;
        jacobianWrtParametersEccentric = null;
        jacobianTrueWrtCartesian       = null;
        jacobianWrtParametersTrue      = null;
    }

    /** Set the orbit from Cartesian parameters.
     * @param pvCoordinates the position and velocity in the inertial frame
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    protected Orbit(final PVCoordinates pvCoordinates, final Frame frame,
                    final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        ensurePseudoInertialFrame(frame);
        this.date = date;
        this.mu = mu;
        this.pvCoordinates = pvCoordinates;
        this.frame = frame;
    }

    /** Get the orbit type.
     * @return orbit type
     */
    public abstract OrbitType getType();

    /** Ensure the defining frame is a pseudo-inertial frame.
     * @param frame frame to check
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    private static void ensurePseudoInertialFrame(final Frame frame)
        throws IllegalArgumentException {
        if (!frame.isPseudoInertial()) {
            throw OrekitException.createIllegalArgumentException(
                OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS,
                frame.getName());
        }
    }

    /** Get the frame in which the orbital parameters are defined.
     * @return frame in which the orbital parameters are defined
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the semi-major axis.
     * <p>Note that the semi-major axis is considered negative for hyperbolic orbits.</p>
     * @return semi-major axis (m)
     */
    public abstract double getA();

    /** Get the first component of the equinoctial eccentricity vector.
     * @return first component of the equinoctial eccentricity vector
     */
    public abstract double getEquinoctialEx();

    /** Get the second component of the equinoctial eccentricity vector.
     * @return second component of the equinoctial eccentricity vector
     */
    public abstract double getEquinoctialEy();

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector
     */
    public abstract double getHx();

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector
     */
    public abstract double getHy();

    /** Get the eccentric latitude argument.
     * @return eccentric latitude argument (rad)
     */
    public abstract double getLE();

    /** Get the true latitude argument.
     * @return true latitude argument (rad)
     */
    public abstract double getLv();

    /** Get the mean latitude argument.
     * @return mean latitude argument (rad)
     */
    public abstract double getLM();

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity
     */
    public abstract double getE();

    /** Get the inclination.
     * @return inclination (rad)
     */
    public abstract double getI();

    /** Get the central acceleration constant.
     * @return central acceleration constant
     */
    public double getMu() {
        return mu;
    }

    /** Get the keplerian period.
     * <p>The keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public double getKeplerianPeriod() {
        final double a = getA();
        return (a < 0) ? Double.POSITIVE_INFINITY : 2.0 * FastMath.PI * a * FastMath.sqrt(a / mu);
    }

    /** Get the keplerian mean motion.
     * <p>The keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian mean motion in radians per second
     */
    public double getKeplerianMeanMotion() {
        final double absA = FastMath.abs(getA());
        return FastMath.sqrt(mu / absA) / absA;
    }

    /** Get the date of orbital parameters.
     * @return date of the orbital parameters
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the {@link PVCoordinates} in a specified frame.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return pvCoordinates in the specified output frame
     * @exception OrekitException if transformation between frames cannot be computed
     * @see #getPVCoordinates()
     */
    public PVCoordinates getPVCoordinates(final Frame outputFrame)
        throws OrekitException {
        if (pvCoordinates == null) {
            pvCoordinates = initPVCoordinates();
        }

        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == frame) {
            return pvCoordinates;
        }

        // Else, PV coordinates are transformed to output frame
        final Transform t = frame.getTransformTo(outputFrame, date);
        return t.transformPVCoordinates(pvCoordinates);
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate otherDate, final Frame otherFrame)
        throws OrekitException {
        return shiftedBy(otherDate.durationFrom(getDate())).getPVCoordinates(otherFrame);
    }


    /** Get the {@link PVCoordinates} in definition frame.
     * @return pvCoordinates in the definition frame
     * @see #getPVCoordinates(Frame)
     */
    public PVCoordinates getPVCoordinates() {
        if (pvCoordinates == null) {
            pvCoordinates = initPVCoordinates();
        }
        return pvCoordinates;
    }

    /** Compute the position/velocity coordinates from the canonical parameters.
     * @return computed position/velocity coordinates
     */
    protected abstract PVCoordinates initPVCoordinates();

    /** Get a time-shifted orbit.
     * <p>
     * The orbit can be slightly shifted to close dates. This shift is based on
     * a simple keplerian model. It is <em>not</em> intended as a replacement
     * for proper orbit and attitude propagation but should be sufficient for
     * small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new orbit, shifted with respect to the instance (which is immutable)
     * @see org.orekit.time.AbsoluteDate#shiftedBy(double)
     * @see org.orekit.utils.PVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.Attitude#shiftedBy(double)
     * @see org.orekit.propagation.SpacecraftState#shiftedBy(double)
     */
    public abstract Orbit shiftedBy(final double dt);

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row correspond to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * @param type type of the position angle to use
     * @param jacobian placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     * is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobianWrtCartesian(final PositionAngle type, final double[][] jacobian) {

        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
            case MEAN :
                if (jacobianMeanWrtCartesian == null) {
                    // first call, we need to compute the jacobian and cache it
                    jacobianMeanWrtCartesian = computeJacobianMeanWrtCartesian();
                }
                cachedJacobian = jacobianMeanWrtCartesian;
                break;
            case ECCENTRIC :
                if (jacobianEccentricWrtCartesian == null) {
                    // first call, we need to compute the jacobian and cache it
                    jacobianEccentricWrtCartesian = computeJacobianEccentricWrtCartesian();
                }
                cachedJacobian = jacobianEccentricWrtCartesian;
                break;
            case TRUE :
                if (jacobianTrueWrtCartesian == null) {
                    // first call, we need to compute the jacobian and cache it
                    jacobianTrueWrtCartesian = computeJacobianTrueWrtCartesian();
                }
                cachedJacobian = jacobianTrueWrtCartesian;
                break;
            default :
                throw OrekitException.createInternalError(null);
            }
        }

        // fill the user provided array
        for (int i = 0; i < cachedJacobian.length; ++i) {
            System.arraycopy(cachedJacobian[i], 0, jacobian[i], 0, cachedJacobian[i].length);
        }

    }

    /** Compute the Jacobian of the Cartesian parameters with respect to the orbital parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row correspond to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * @param type type of the position angle to use
     * @param jacobian placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     * is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobianWrtParameters(final PositionAngle type, final double[][] jacobian) {

        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
            case MEAN :
                if (jacobianWrtParametersMean == null) {
                    // first call, we need to compute the jacobian and cache it
                    jacobianWrtParametersMean = createInverseJacobian(type);
                }
                cachedJacobian = jacobianWrtParametersMean;
                break;
            case ECCENTRIC :
                if (jacobianWrtParametersEccentric == null) {
                    // first call, we need to compute the jacobian and cache it
                    jacobianWrtParametersEccentric = createInverseJacobian(type);
                }
                cachedJacobian = jacobianWrtParametersEccentric;
                break;
            case TRUE :
                if (jacobianWrtParametersTrue == null) {
                    // first call, we need to compute the jacobian and cache it
                    jacobianWrtParametersTrue = createInverseJacobian(type);
                }
                cachedJacobian = jacobianWrtParametersTrue;
                break;
            default :
                throw OrekitException.createInternalError(null);
            }
        }

        // fill the user-provided array
        for (int i = 0; i < cachedJacobian.length; ++i) {
            System.arraycopy(cachedJacobian[i], 0, jacobian[i], 0, cachedJacobian[i].length);
        }

    }

    /** Create an inverse Jacobian.
     * @param type type of the position angle to use
     * @return inverse Jacobian
     */
    private double[][] createInverseJacobian(final PositionAngle type) {

        // get the direct Jacobian
        final double[][] directJacobian = new double[6][6];
        getJacobianWrtCartesian(type, directJacobian);

        // invert the direct Jacobian
        final RealMatrix matrix = MatrixUtils.createRealMatrix(directJacobian);
        final DecompositionSolver solver = new QRDecompositionImpl(matrix).getSolver();
        return solver.getInverse().getData();

    }

    /** Compute the Jacobian of the orbital parameters with mean angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row correspond to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianEccentricWrtCartesian()
     * @see #computeJacobianTrueWrtCartesian()
     */
    protected abstract double[][] computeJacobianMeanWrtCartesian();

    /** Compute the Jacobian of the orbital parameters with eccentric angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row correspond to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianMeanWrtCartesian()
     * @see #computeJacobianTrueWrtCartesian()
     */
    protected abstract double[][] computeJacobianEccentricWrtCartesian();

    /** Compute the Jacobian of the orbital parameters with true angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row correspond to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianMeanWrtCartesian()
     * @see #computeJacobianEccentricWrtCartesian()
     */
    protected abstract double[][] computeJacobianTrueWrtCartesian();

    /** Add the contribution of the Keplerian motion to parameters derivatives
     * <p>
     * This method is used by numerical propagators to evaluate the part of Keplerrian
     * motion to evolution of the orbital state.
     * </p>
     * @param type type of the position angle in the state
     * @param gm attraction coefficient to use
     * @param pDot array containing orbital state derivatives to update (the Keplerian
     * part must be <em>added</em> to the array components, as the array may already
     * contain some non-zero elements corresponding to non-Keplerian parts)
     */
    public abstract void addKeplerContribution(final PositionAngle type, final double gm, double[] pDot);

        /** Fill a Jacobian half row with a single vector.
     * @param a coefficient of the vector
     * @param v vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a, final Vector3D v, final double[] row, final int j) {
        row[j]     = a * v.getX();
        row[j + 1] = a * v.getY();
        row[j + 2] = a * v.getZ();
    }

    /** Fill a Jacobian half row with a linear combination of vectors.
     * @param a1 coefficient of the first vector
     * @param v1 first vector
     * @param a2 coefficient of the second vector
     * @param v2 second vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double[] row, final int j) {
        row[j]     = a1 * v1.getX() + a2 * v2.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ();
    }

    /** Fill a Jacobian half row with a linear combination of vectors.
     * @param a1 coefficient of the first vector
     * @param v1 first vector
     * @param a2 coefficient of the second vector
     * @param v2 second vector
     * @param a3 coefficient of the third vector
     * @param v3 third vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3,
                                      final double[] row, final int j) {
        row[j]     = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ();
    }

    /** Fill a Jacobian half row with a linear combination of vectors.
     * @param a1 coefficient of the first vector
     * @param v1 first vector
     * @param a2 coefficient of the second vector
     * @param v2 second vector
     * @param a3 coefficient of the third vector
     * @param v3 third vector
     * @param a4 coefficient of the fourth vector
     * @param v4 fourth vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3, final double a4, final Vector3D v4,
                                      final double[] row, final int j) {
        row[j]     = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX() + a4 * v4.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY() + a4 * v4.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ() + a4 * v4.getZ();
    }

    /** Fill a Jacobian half row with a linear combination of vectors.
     * @param a1 coefficient of the first vector
     * @param v1 first vector
     * @param a2 coefficient of the second vector
     * @param v2 second vector
     * @param a3 coefficient of the third vector
     * @param v3 third vector
     * @param a4 coefficient of the fourth vector
     * @param v4 fourth vector
     * @param a5 coefficient of the fifth vector
     * @param v5 fifth vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3, final double a4, final Vector3D v4,
                                      final double a5, final Vector3D v5,
                                      final double[] row, final int j) {
        row[j]     = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX() + a4 * v4.getX() + a5 * v5.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY() + a4 * v4.getY() + a5 * v5.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ() + a4 * v4.getZ() + a5 * v5.getZ();
    }

    /** Fill a Jacobian half row with a linear combination of vectors.
     * @param a1 coefficient of the first vector
     * @param v1 first vector
     * @param a2 coefficient of the second vector
     * @param v2 second vector
     * @param a3 coefficient of the third vector
     * @param v3 third vector
     * @param a4 coefficient of the fourth vector
     * @param v4 fourth vector
     * @param a5 coefficient of the fifth vector
     * @param v5 fifth vector
     * @param a6 coefficient of the sixth vector
     * @param v6 sixth vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3, final double a4, final Vector3D v4,
                                      final double a5, final Vector3D v5, final double a6, final Vector3D v6,
                                      final double[] row, final int j) {
        row[j]     = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX() + a4 * v4.getX() + a5 * v5.getX() + a6 * v6.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY() + a4 * v4.getY() + a5 * v5.getY() + a6 * v6.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ() + a4 * v4.getZ() + a5 * v5.getZ() + a6 * v6.getZ();
    }

}
