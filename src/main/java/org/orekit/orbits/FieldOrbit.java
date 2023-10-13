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



import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.FieldDecompositionSolver;
import org.hipparchus.linear.FieldLUDecomposition;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeShiftable;
import org.orekit.time.FieldTimeStamped;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
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
 * @since 9.0
 * @param <T> type of the field elements
 */
public abstract class FieldOrbit<T extends CalculusFieldElement<T>>
    implements FieldPVCoordinatesProvider<T>, FieldTimeStamped<T>, FieldTimeShiftable<FieldOrbit<T>, T> {

    /** Frame in which are defined the orbital parameters. */
    private final Frame frame;

    /** Date of the orbital parameters. */
    private final FieldAbsoluteDate<T> date;

    /** Value of mu used to compute position and velocity (m³/s²). */
    private final T mu;

    /** Field-valued one.
     * @since 12.0
     * */
    private final T one;

    /** Field-valued zero.
     * @since 12.0
     * */
    private final T zero;

    /** Field used by this class.
     * @since 12.0
     */
    private final Field<T> field;

    /** Computed position.
     * @since 12.0
     */
    private transient FieldVector3D<T> position;

    /** Computed PVCoordinates. */
    private transient TimeStampedFieldPVCoordinates<T> pvCoordinates;

    /** Jacobian of the orbital parameters with mean angle with respect to the Cartesian coordinates. */
    private transient T[][] jacobianMeanWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with mean angle. */
    private transient T[][] jacobianWrtParametersMean;

    /** Jacobian of the orbital parameters with eccentric angle with respect to the Cartesian coordinates. */
    private transient T[][] jacobianEccentricWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with eccentric angle. */
    private transient T[][] jacobianWrtParametersEccentric;

    /** Jacobian of the orbital parameters with true angle with respect to the Cartesian coordinates. */
    private transient T[][] jacobianTrueWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with true angle. */
    private transient T[][] jacobianWrtParametersTrue;

    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    protected FieldOrbit(final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        ensurePseudoInertialFrame(frame);
        this.field = mu.getField();
        this.zero = this.field.getZero();
        this.one = this.field.getOne();
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
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param FieldPVCoordinates the position and velocity in the inertial frame
     * @param frame the frame in which the {@link TimeStampedPVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m^3/s^2)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    protected FieldOrbit(final TimeStampedFieldPVCoordinates<T> FieldPVCoordinates, final Frame frame, final T mu)
        throws IllegalArgumentException {
        ensurePseudoInertialFrame(frame);
        this.field = mu.getField();
        this.zero = this.field.getZero();
        this.one = this.field.getOne();
        this.date = FieldPVCoordinates.getDate();
        this.mu = mu;
        if (FieldPVCoordinates.getAcceleration().getNormSq().getReal() == 0.0) {
            // the acceleration was not provided,
            // compute it from Newtonian attraction
            final T r2 = FieldPVCoordinates.getPosition().getNormSq();
            final T r3 = r2.multiply(r2.sqrt());
            this.pvCoordinates = new TimeStampedFieldPVCoordinates<>(FieldPVCoordinates.getDate(),
                                                                     FieldPVCoordinates.getPosition(),
                                                                     FieldPVCoordinates.getVelocity(),
                                                                     new FieldVector3D<>(r3.reciprocal().multiply(mu.negate()), FieldPVCoordinates.getPosition()));
        } else {
            this.pvCoordinates = FieldPVCoordinates;
        }
        this.frame = frame;
    }

    /** Check if Cartesian coordinates include non-Keplerian acceleration.
     * @param pva Cartesian coordinates
     * @param mu central attraction coefficient
     * @param <T> type of the field elements
     * @return true if Cartesian coordinates include non-Keplerian acceleration
     */
    protected static <T extends CalculusFieldElement<T>> boolean hasNonKeplerianAcceleration(final FieldPVCoordinates<T> pva, final T mu) {

        final FieldVector3D<T> a = pva.getAcceleration();
        if (a == null) {
            return false;
        }

        final FieldVector3D<T> p = pva.getPosition();
        final T r2 = p.getNormSq();
        final T r  = r2.sqrt();
        final FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(mu.negate()), p);
        if (a.getNorm().getReal() > 1.0e-9 * keplerianAcceleration.getNorm().getReal()) {
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
        return getA().getReal() > 0.;
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

    /**Transforms the FieldOrbit instance into an Orbit instance.
     * @return Orbit instance with same properties
     */
    public abstract Orbit toOrbit();

    /** Get the semi-major axis.
     * <p>Note that the semi-major axis is considered negative for hyperbolic orbits.</p>
     * @return semi-major axis (m)
     */
    public abstract T getA();

    /** Get the semi-major axis derivative.
     * <p>Note that the semi-major axis is considered negative for hyperbolic orbits.</p>
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return semi-major axis  derivative (m/s)
     */
    public abstract T getADot();

    /** Get the first component of the equinoctial eccentricity vector.
     * @return first component of the equinoctial eccentricity vector
     */
    public abstract T getEquinoctialEx();

    /** Get the first component of the equinoctial eccentricity vector.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return first component of the equinoctial eccentricity vector
     */
    public abstract T getEquinoctialExDot();

    /** Get the second component of the equinoctial eccentricity vector.
     * @return second component of the equinoctial eccentricity vector
     */
    public abstract T getEquinoctialEy();

    /** Get the second component of the equinoctial eccentricity vector.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return second component of the equinoctial eccentricity vector
     */
    public abstract T getEquinoctialEyDot();

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector
     */
    public abstract T getHx();

    /** Get the first component of the inclination vector derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return first component of the inclination vector derivative
     */
    public abstract T getHxDot();

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector
     */
    public abstract T getHy();

    /** Get the second component of the inclination vector derivative.
     * @return second component of the inclination vector derivative
     */
    public abstract T getHyDot();

    /** Get the eccentric longitude argument.
     * @return E + ω + Ω eccentric longitude argument (rad)
     */
    public abstract T getLE();

    /** Get the eccentric longitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return d(E + ω + Ω)/dt eccentric longitude argument derivative (rad/s)
     */
    public abstract T getLEDot();

    /** Get the true longitude argument.
     * @return v + ω + Ω true longitude argument (rad)
     */
    public abstract T getLv();

    /** Get the true longitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return d(v + ω + Ω)/dt true longitude argument derivative (rad/s)
     */
    public abstract T getLvDot();

    /** Get the mean longitude argument.
     * @return M + ω + Ω mean longitude argument (rad)
     */
    public abstract T getLM();

    /** Get the mean longitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return d(M + ω + Ω)/dt mean longitude argument derivative (rad/s)
     */
    public abstract T getLMDot();

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity
     */
    public abstract T getE();

    /** Get the eccentricity derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return eccentricity derivative
     */
    public abstract T getEDot();

    /** Get the inclination.
     * <p>
     * If the orbit was created without derivatives, the value returned is null.
     * </p>
     * @return inclination (rad)
     */
    public abstract T getI();

    /** Get the inclination derivative.
     * @return inclination derivative (rad/s)
     */
    public abstract T getIDot();

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
    public abstract boolean hasDerivatives();

    /** Get the central attraction coefficient used for position and velocity conversions (m³/s²).
     * @return central attraction coefficient used for position and velocity conversions (m³/s²)
     */
    public T getMu() {
        return mu;
    }

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public T getKeplerianPeriod() {
        final T a = getA();
        return isElliptical() ?
               a.multiply(a.getPi().multiply(2.0)).multiply(a.divide(mu).sqrt()) :
               zero.add(Double.POSITIVE_INFINITY);
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public T getKeplerianMeanMotion() {
        final T absA = getA().abs();
        return absA.reciprocal().multiply(mu).sqrt().divide(absA);
    }

    /** Get the derivative of the mean anomaly with respect to the semi major axis.
     * @return derivative of the mean anomaly with respect to the semi major axis
     */
    public T getMeanAnomalyDotWrtA() {
        return getKeplerianMeanMotion().divide(getA()).multiply(-1.5);
    }

    /** Get the date of orbital parameters.
     * @return date of the orbital parameters
     */
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get the {@link TimeStampedPVCoordinates} in a specified frame.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return FieldPVCoordinates in the specified output frame
          * @see #getPVCoordinates()
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final Frame outputFrame) {
        if (pvCoordinates == null) {
            pvCoordinates = initPVCoordinates();
        }

        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == frame) {
            return pvCoordinates;
        }

        // Else, PV coordinates are transformed to output frame
        final FieldTransform<T> t = frame.getTransformTo(outputFrame, date);
        return t.transformPVCoordinates(pvCoordinates);
    }

    /** {@inheritDoc} */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> otherDate, final Frame otherFrame) {
        return shiftedBy(otherDate.durationFrom(getDate())).getPVCoordinates(otherFrame);
    }

    /** Get the position in a specified frame.
     * @param outputFrame frame in which the position coordinates shall be computed
     * @return position in the specified output frame
     * @see #getPosition()
     * @since 12.0
     */
    public FieldVector3D<T> getPosition(final Frame outputFrame) {
        if (position == null) {
            position = initPosition();
        }

        // If output frame requested is the same as definition frame,
        // Position vector is returned directly
        if (outputFrame == frame) {
            return position;
        }

        // Else, position vector is transformed to output frame
        final FieldStaticTransform<T> t = frame.getStaticTransformTo(outputFrame, date);
        return t.transformPosition(position);

    }

    /** Get the position in definition frame.
     * @return position in the definition frame
     * @see #getPVCoordinates()
     * @since 12.0
     */
    public FieldVector3D<T> getPosition() {
        if (position == null) {
            position = initPosition();
        }
        return position;
    }

    /** Get the {@link TimeStampedPVCoordinates} in definition frame.
     * @return FieldPVCoordinates in the definition frame
     * @see #getPVCoordinates(Frame)
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates() {
        if (pvCoordinates == null) {
            pvCoordinates = initPVCoordinates();
            position      = pvCoordinates.getPosition();
        }
        return pvCoordinates;
    }

    /** Getter for Field-valued one.
     * @return one
     * */
    protected T getOne() {
        return one;
    }

    /** Getter for Field-valued zero.
     * @return zero
     * */
    protected T getZero() {
        return zero;
    }

    /** Getter for Field.
     * @return CalculusField
     * */
    protected Field<T> getField() {
        return field;
    }

    /** Compute the position coordinates from the canonical parameters.
     * @return computed position coordinates
     * @since 12.0
     */
    protected abstract FieldVector3D<T> initPosition();

    /** Compute the position/velocity coordinates from the canonical parameters.
     * @return computed position/velocity coordinates
     */
    protected abstract TimeStampedFieldPVCoordinates<T> initPVCoordinates();

    /** Get a time-shifted orbit.
     * <p>
     * The orbit can be slightly shifted to close dates. This shift is based on
     * a simple Keplerian model. It is <em>not</em> intended as a replacement
     * for proper orbit and attitude propagation but should be sufficient for
     * small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new orbit, shifted with respect to the instance (which is immutable)
     */
    public abstract FieldOrbit<T> shiftedBy(T dt);

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
    public void getJacobianWrtCartesian(final PositionAngleType type, final T[][] jacobian) {

        final T[][] cachedJacobian;
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
    public void getJacobianWrtParameters(final PositionAngleType type, final T[][] jacobian) {

        final T[][] cachedJacobian;
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
    private T[][] createInverseJacobian(final PositionAngleType type) {

        // get the direct Jacobian
        final T[][] directJacobian = MathArrays.buildArray(getA().getField(), 6, 6);
        getJacobianWrtCartesian(type, directJacobian);

        // invert the direct Jacobian
        final FieldMatrix<T> matrix = MatrixUtils.createFieldMatrix(directJacobian);
        final FieldDecompositionSolver<T> solver = new FieldLUDecomposition<>(matrix).getSolver();
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
    protected abstract T[][] computeJacobianMeanWrtCartesian();

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
    protected abstract T[][] computeJacobianEccentricWrtCartesian();

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
    protected abstract T[][] computeJacobianTrueWrtCartesian();

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
    public abstract void addKeplerContribution(PositionAngleType type, T gm, T[] pDot);

        /** Fill a Jacobian half row with a single vector.
     * @param a coefficient of the vector
     * @param v vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */

    protected void fillHalfRow(final T a, final FieldVector3D<T> v, final T[] row, final int j) {
        row[j]     = a.multiply(v.getX());
        row[j + 1] = a.multiply(v.getY());
        row[j + 2] = a.multiply(v.getZ());
    }

    /** Fill a Jacobian half row with a linear combination of vectors.
     * @param a1 coefficient of the first vector
     * @param v1 first vector
     * @param a2 coefficient of the second vector
     * @param v2 second vector
     * @param row Jacobian matrix row
     * @param j index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected void fillHalfRow(final T a1, final FieldVector3D<T> v1, final T a2, final FieldVector3D<T> v2,
                                      final T[] row, final int j) {
        row[j]     = a1.linearCombination(a1, v1.getX(), a2, v2.getX());
        row[j + 1] = a1.linearCombination(a1, v1.getY(), a2, v2.getY());
        row[j + 2] = a1.linearCombination(a1, v1.getZ(), a2, v2.getZ());

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
    protected void fillHalfRow(final T a1, final FieldVector3D<T> v1,
                               final T a2, final FieldVector3D<T> v2,
                               final T a3, final FieldVector3D<T> v3,
                                      final T[] row, final int j) {
        row[j]     = a1.linearCombination(a1, v1.getX(), a2, v2.getX(), a3, v3.getX());
        row[j + 1] = a1.linearCombination(a1, v1.getY(), a2, v2.getY(), a3, v3.getY());
        row[j + 2] = a1.linearCombination(a1, v1.getZ(), a2, v2.getZ(), a3, v3.getZ());

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
    protected void fillHalfRow(final T a1, final FieldVector3D<T> v1, final T a2, final FieldVector3D<T> v2,
                                      final T a3, final FieldVector3D<T> v3, final T a4, final FieldVector3D<T> v4,
                                      final T[] row, final int j) {
        row[j]     = a1.linearCombination(a1, v1.getX(), a2, v2.getX(), a3, v3.getX(), a4, v4.getX());
        row[j + 1] = a1.linearCombination(a1, v1.getY(), a2, v2.getY(), a3, v3.getY(), a4, v4.getY());
        row[j + 2] = a1.linearCombination(a1, v1.getZ(), a2, v2.getZ(), a3, v3.getZ(), a4, v4.getZ());

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
    protected void fillHalfRow(final T a1, final FieldVector3D<T> v1, final T a2, final FieldVector3D<T> v2,
                                      final T a3, final FieldVector3D<T> v3, final T a4, final FieldVector3D<T> v4,
                                      final T a5, final FieldVector3D<T> v5,
                                      final T[] row, final int j) {
        row[j]     = a1.linearCombination(a1, v1.getX(), a2, v2.getX(), a3, v3.getX(), a4, v4.getX()).add(a5.multiply(v5.getX()));
        row[j + 1] = a1.linearCombination(a1, v1.getY(), a2, v2.getY(), a3, v3.getY(), a4, v4.getY()).add(a5.multiply(v5.getY()));
        row[j + 2] = a1.linearCombination(a1, v1.getZ(), a2, v2.getZ(), a3, v3.getZ(), a4, v4.getZ()).add(a5.multiply(v5.getZ()));

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
    protected void fillHalfRow(final T a1, final FieldVector3D<T> v1, final T a2, final FieldVector3D<T> v2,
                                      final T a3, final FieldVector3D<T> v3, final T a4, final FieldVector3D<T> v4,
                                      final T a5, final FieldVector3D<T> v5, final T a6, final FieldVector3D<T> v6,
                                      final T[] row, final int j) {
        row[j]     = a1.linearCombination(a1, v1.getX(), a2, v2.getX(), a3, v3.getX(), a4, v4.getX()).add(a1.linearCombination(a5, v5.getX(), a6, v6.getX()));
        row[j + 1] = a1.linearCombination(a1, v1.getY(), a2, v2.getY(), a3, v3.getY(), a4, v4.getY()).add(a1.linearCombination(a5, v5.getY(), a6, v6.getY()));
        row[j + 2] = a1.linearCombination(a1, v1.getZ(), a2, v2.getZ(), a3, v3.getZ(), a4, v4.getZ()).add(a1.linearCombination(a5, v5.getZ(), a6, v6.getZ()));

    }


}
