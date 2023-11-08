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
package org.orekit.orbits;

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class handles orbital parameters.

 * <p>
 * For user convenience, both the Cartesian and the equinoctial elements
 * are provided by this class, regardless of the canonical representation
 * implemented in the derived class (which may be classical Keplerian
 * elements for example).
 * </p>
 * <p>
 * The parameters are defined in a frame specified by the user. It is important
 * to make sure this frame is consistent: it probably is inertial and centered
 * on the central body. This information is used for example by some
 * force models.
 * </p>
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public abstract class Orbit
    implements TimeStamped, TimeShiftable<Orbit>, Serializable, PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 438733454597999578L;

    /** Frame in which are defined the orbital parameters. */
    private final Frame frame;

    /** Date of the orbital parameters. */
    private final AbsoluteDate date;

    /** Value of mu used to compute position and velocity (m³/s²). */
    private final double mu;

    /** Computed position.
     * @since 12.0
     */
    private transient Vector3D position;

    /** Computed PVCoordinates. */
    private transient TimeStampedPVCoordinates pvCoordinates;

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
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the position and velocity in the inertial frame
     * @param frame the frame in which the {@link TimeStampedPVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    protected Orbit(final TimeStampedPVCoordinates pvCoordinates, final Frame frame, final double mu)
        throws IllegalArgumentException {
        ensurePseudoInertialFrame(frame);
        this.date = pvCoordinates.getDate();
        this.mu = mu;
        if (pvCoordinates.getAcceleration().getNormSq() == 0) {
            // the acceleration was not provided,
            // compute it from Newtonian attraction
            final double r2 = pvCoordinates.getPosition().getNormSq();
            final double r3 = r2 * FastMath.sqrt(r2);
            this.pvCoordinates = new TimeStampedPVCoordinates(pvCoordinates.getDate(),
                                                              pvCoordinates.getPosition(),
                                                              pvCoordinates.getVelocity(),
                                                              new Vector3D(-mu / r3, pvCoordinates.getPosition()));
        } else {
            this.pvCoordinates = pvCoordinates;
        }
        this.frame = frame;
    }

    /** Check if Cartesian coordinates include non-Keplerian acceleration.
     * @param pva Cartesian coordinates
     * @param mu central attraction coefficient
     * @return true if Cartesian coordinates include non-Keplerian acceleration
     */
    protected static boolean hasNonKeplerianAcceleration(final PVCoordinates pva, final double mu) {

        final Vector3D p = pva.getPosition();
        final double r2 = p.getNormSq();
        final double r  = FastMath.sqrt(r2);
        final Vector3D keplerianAcceleration = new Vector3D(-mu / (r * r2), p);

        // Check if acceleration is null or relatively close to 0 compared to the keplerain acceleration
        final Vector3D a = pva.getAcceleration();
        if (a == null || a.getNorm() < 1e-9 * keplerianAcceleration.getNorm()) {
            return false;
        }

        final Vector3D nonKeplerianAcceleration = a.subtract(keplerianAcceleration);

        if ( nonKeplerianAcceleration.getNorm() > 1e-9 * keplerianAcceleration.getNorm()) {
            // we have a relevant acceleration, we can compute derivatives
            return true;
        } else {
            // the provided acceleration is either too small to be reliable (probably even 0), or NaN
            return false;
        }
    }

    /** Returns true if and only if the orbit is elliptical i.e. has a non-negative semi-major axis.
     * @return true if getA() is strictly greater than 0
     * @since 12.0
     */
    public boolean isElliptical() {
        return getA() > 0.;
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
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
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

    /** Get the semi-major axis derivative.
     * <p>Note that the semi-major axis is considered negative for hyperbolic orbits.</p>
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return semi-major axis  derivative (m/s)
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getADot();

    /** Get the first component of the equinoctial eccentricity vector derivative.
     * @return first component of the equinoctial eccentricity vector derivative
     */
    public abstract double getEquinoctialEx();

    /** Get the first component of the equinoctial eccentricity vector.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return first component of the equinoctial eccentricity vector
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getEquinoctialExDot();

    /** Get the second component of the equinoctial eccentricity vector derivative.
     * @return second component of the equinoctial eccentricity vector derivative
     */
    public abstract double getEquinoctialEy();

    /** Get the second component of the equinoctial eccentricity vector.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return second component of the equinoctial eccentricity vector
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getEquinoctialEyDot();

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector
     */
    public abstract double getHx();

    /** Get the first component of the inclination vector derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return first component of the inclination vector derivative
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getHxDot();

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector
     */
    public abstract double getHy();

    /** Get the second component of the inclination vector derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return second component of the inclination vector derivative
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getHyDot();

    /** Get the eccentric longitude argument.
     * @return E + ω + Ω eccentric longitude argument (rad)
     */
    public abstract double getLE();

    /** Get the eccentric longitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return d(E + ω + Ω)/dt eccentric longitude argument derivative (rad/s)
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getLEDot();

    /** Get the true longitude argument.
     * @return v + ω + Ω true longitude argument (rad)
     */
    public abstract double getLv();

    /** Get the true longitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return d(v + ω + Ω)/dt true longitude argument derivative (rad/s)
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getLvDot();

    /** Get the mean longitude argument.
     * @return M + ω + Ω mean longitude argument (rad)
     */
    public abstract double getLM();

    /** Get the mean longitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return d(M + ω + Ω)/dt mean longitude argument derivative (rad/s)
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getLMDot();

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity
     */
    public abstract double getE();

    /** Get the eccentricity derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return eccentricity derivative
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getEDot();

    /** Get the inclination.
     * @return inclination (rad)
     */
    public abstract double getI();

    /** Get the inclination derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return inclination derivative (rad/s)
     * @see #hasDerivatives()
     * @since 9.0
     */
    public abstract double getIDot();

    /** Check if orbit includes derivatives.
     * @return true if orbit includes derivatives
     * @see #getADot()
     * @see #getEquinoctialExDot()
     * @see #getEquinoctialEyDot()
     * @see #getHxDot()
     * @see #getHyDot()
     * @see #getLEDot()
     * @see #getLvDot()
     * @see #getLMDot()
     * @see #getEDot()
     * @see #getIDot()
     * @since 9.0
     */
    public boolean hasDerivatives() {
        return !Double.isNaN(getADot());
    }

    /** Get the central acceleration constant.
     * @return central acceleration constant
     */
    public double getMu() {
        return mu;
    }

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public double getKeplerianPeriod() {
        final double a = getA();
        return isElliptical() ? 2.0 * FastMath.PI * a * FastMath.sqrt(a / mu) : Double.POSITIVE_INFINITY;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public double getKeplerianMeanMotion() {
        final double absA = FastMath.abs(getA());
        return FastMath.sqrt(mu / absA) / absA;
    }

    /** Get the derivative of the mean anomaly with respect to the semi major axis.
     * @return derivative of the mean anomaly with respect to the semi major axis
     */
    public double getMeanAnomalyDotWrtA() {
        return -1.5 * getKeplerianMeanMotion() / getA();
    }

    /** Get the date of orbital parameters.
     * @return date of the orbital parameters
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the {@link TimeStampedPVCoordinates} in a specified frame.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return pvCoordinates in the specified output frame
          * @see #getPVCoordinates()
     */
    public TimeStampedPVCoordinates getPVCoordinates(final Frame outputFrame) {
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
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate otherDate, final Frame otherFrame) {
        return shiftedBy(otherDate.durationFrom(getDate())).getPVCoordinates(otherFrame);
    }

    /** Get the position in a specified frame.
     * @param outputFrame frame in which the position coordinates shall be computed
     * @return position in the specified output frame
     * @see #getPosition()
     * @since 12.0
     */
    public Vector3D getPosition(final Frame outputFrame) {
        if (position == null) {
            position = initPosition();
        }

        // If output frame requested is the same as definition frame,
        // Position vector is returned directly
        if (outputFrame == frame) {
            return position;
        }

        // Else, position vector is transformed to output frame
        final StaticTransform t = frame.getStaticTransformTo(outputFrame, date);
        return t.transformPosition(position);

    }

    /** Get the position in definition frame.
     * @return position in the definition frame
     * @see #getPVCoordinates()
     * @since 12.0
     */
    public Vector3D getPosition() {
        if (position == null) {
            position = initPosition();
        }
        return position;
    }

    /** Get the {@link TimeStampedPVCoordinates} in definition frame.
     * @return pvCoordinates in the definition frame
     * @see #getPVCoordinates(Frame)
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        if (pvCoordinates == null) {
            pvCoordinates = initPVCoordinates();
            position      = pvCoordinates.getPosition();
        }
        return pvCoordinates;
    }

    /** Compute the position coordinates from the canonical parameters.
     * @return computed position coordinates
     * @since 12.0
     */
    protected abstract Vector3D initPosition();

    /** Compute the position/velocity coordinates from the canonical parameters.
     * @return computed position/velocity coordinates
     */
    protected abstract TimeStampedPVCoordinates initPVCoordinates();

    /** Get a time-shifted orbit.
     * <p>
     * The orbit can be slightly shifted to close dates. The shifting model is a
     * Keplerian one if no derivatives are available in the orbit, or Keplerian
     * plus quadratic effect of the non-Keplerian acceleration if derivatives are
     * available. Shifting is <em>not</em> intended as a replacement for proper
     * orbit propagation but should be sufficient for small time shifts or coarse
     * accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new orbit, shifted with respect to the instance (which is immutable)
     */
    public abstract Orbit shiftedBy(double dt);

    /** Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row corresponds to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * @param type type of the position angle to use
     * @param jacobian placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     * is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobianWrtCartesian(final PositionAngleType type, final double[][] jacobian) {

        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
                case MEAN :
                    if (jacobianMeanWrtCartesian == null) {
                        // first call, we need to compute the Jacobian and cache it
                        jacobianMeanWrtCartesian = computeJacobianMeanWrtCartesian();
                    }
                    cachedJacobian = jacobianMeanWrtCartesian;
                    break;
                case ECCENTRIC :
                    if (jacobianEccentricWrtCartesian == null) {
                        // first call, we need to compute the Jacobian and cache it
                        jacobianEccentricWrtCartesian = computeJacobianEccentricWrtCartesian();
                    }
                    cachedJacobian = jacobianEccentricWrtCartesian;
                    break;
                case TRUE :
                    if (jacobianTrueWrtCartesian == null) {
                        // first call, we need to compute the Jacobian and cache it
                        jacobianTrueWrtCartesian = computeJacobianTrueWrtCartesian();
                    }
                    cachedJacobian = jacobianTrueWrtCartesian;
                    break;
                default :
                    throw new OrekitInternalError(null);
            }
        }

        // fill the user provided array
        for (int i = 0; i < cachedJacobian.length; ++i) {
            System.arraycopy(cachedJacobian[i], 0, jacobian[i], 0, cachedJacobian[i].length);
        }

    }

    /** Compute the Jacobian of the Cartesian parameters with respect to the orbital parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i of the orbit with
     * respect to orbital parameter j. This means each row corresponds to one Cartesian coordinate
     * x, y, z, xdot, ydot, zdot whereas columns 0 to 5 correspond to the orbital parameters.
     * </p>
     * @param type type of the position angle to use
     * @param jacobian placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     * is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobianWrtParameters(final PositionAngleType type, final double[][] jacobian) {

        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
                case MEAN :
                    if (jacobianWrtParametersMean == null) {
                        // first call, we need to compute the Jacobian and cache it
                        jacobianWrtParametersMean = createInverseJacobian(type);
                    }
                    cachedJacobian = jacobianWrtParametersMean;
                    break;
                case ECCENTRIC :
                    if (jacobianWrtParametersEccentric == null) {
                        // first call, we need to compute the Jacobian and cache it
                        jacobianWrtParametersEccentric = createInverseJacobian(type);
                    }
                    cachedJacobian = jacobianWrtParametersEccentric;
                    break;
                case TRUE :
                    if (jacobianWrtParametersTrue == null) {
                        // first call, we need to compute the Jacobian and cache it
                        jacobianWrtParametersTrue = createInverseJacobian(type);
                    }
                    cachedJacobian = jacobianWrtParametersTrue;
                    break;
                default :
                    throw new OrekitInternalError(null);
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
    private double[][] createInverseJacobian(final PositionAngleType type) {

        // get the direct Jacobian
        final double[][] directJacobian = new double[6][6];
        getJacobianWrtCartesian(type, directJacobian);

        // invert the direct Jacobian
        final RealMatrix matrix = MatrixUtils.createRealMatrix(directJacobian);
        final DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
        return solver.getInverse().getData();

    }

    /** Compute the Jacobian of the orbital parameters with mean angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to Cartesian coordinate j. This means each row correspond to one orbital parameter
     * whereas columns 0 to 5 correspond to the Cartesian coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * <p>
     * The array returned by this method will not be modified.
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
     * <p>
     * The array returned by this method will not be modified.
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
     * <p>
     * The array returned by this method will not be modified.
     * </p>
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianMeanWrtCartesian()
     * @see #computeJacobianEccentricWrtCartesian()
     */
    protected abstract double[][] computeJacobianTrueWrtCartesian();

    /** Add the contribution of the Keplerian motion to parameters derivatives
     * <p>
     * This method is used by integration-based propagators to evaluate the part of Keplerian
     * motion to evolution of the orbital state.
     * </p>
     * @param type type of the position angle in the state
     * @param gm attraction coefficient to use
     * @param pDot array containing orbital state derivatives to update (the Keplerian
     * part must be <em>added</em> to the array components, as the array may already
     * contain some non-zero elements corresponding to non-Keplerian parts)
     */
    public abstract void addKeplerContribution(PositionAngleType type, double gm, double[] pDot);

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
        row[j]     = MathArrays.linearCombination(a1, v1.getX(), a2, v2.getX());
        row[j + 1] = MathArrays.linearCombination(a1, v1.getY(), a2, v2.getY());
        row[j + 2] = MathArrays.linearCombination(a1, v1.getZ(), a2, v2.getZ());
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
        row[j]     = MathArrays.linearCombination(a1, v1.getX(), a2, v2.getX(), a3, v3.getX());
        row[j + 1] = MathArrays.linearCombination(a1, v1.getY(), a2, v2.getY(), a3, v3.getY());
        row[j + 2] = MathArrays.linearCombination(a1, v1.getZ(), a2, v2.getZ(), a3, v3.getZ());
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
        row[j]     = MathArrays.linearCombination(a1, v1.getX(), a2, v2.getX(), a3, v3.getX(), a4, v4.getX());
        row[j + 1] = MathArrays.linearCombination(a1, v1.getY(), a2, v2.getY(), a3, v3.getY(), a4, v4.getY());
        row[j + 2] = MathArrays.linearCombination(a1, v1.getZ(), a2, v2.getZ(), a3, v3.getZ(), a4, v4.getZ());
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
        final double[] a = new double[] {
            a1, a2, a3, a4, a5
        };
        row[j]     = MathArrays.linearCombination(a, new double[] {
            v1.getX(), v2.getX(), v3.getX(), v4.getX(), v5.getX()
        });
        row[j + 1] = MathArrays.linearCombination(a, new double[] {
            v1.getY(), v2.getY(), v3.getY(), v4.getY(), v5.getY()
        });
        row[j + 2] = MathArrays.linearCombination(a, new double[] {
            v1.getZ(), v2.getZ(), v3.getZ(), v4.getZ(), v5.getZ()
        });
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
        final double[] a = new double[] {
            a1, a2, a3, a4, a5, a6
        };
        row[j]     = MathArrays.linearCombination(a, new double[] {
            v1.getX(), v2.getX(), v3.getX(), v4.getX(), v5.getX(), v6.getX()
        });
        row[j + 1] = MathArrays.linearCombination(a, new double[] {
            v1.getY(), v2.getY(), v3.getY(), v4.getY(), v5.getY(), v6.getY()
        });
        row[j + 2] = MathArrays.linearCombination(a, new double[] {
            v1.getZ(), v2.getZ(), v3.getZ(), v4.getZ(), v5.getZ(), v6.getZ()
        });
    }

}
