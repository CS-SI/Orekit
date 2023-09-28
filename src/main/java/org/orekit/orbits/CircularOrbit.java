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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * This class handles circular orbital parameters.

 * <p>
 * The parameters used internally are the circular elements which can be
 * related to Keplerian elements as follows:
 *   <ul>
 *     <li>a</li>
 *     <li>e<sub>x</sub> = e cos(ω)</li>
 *     <li>e<sub>y</sub> = e sin(ω)</li>
 *     <li>i</li>
 *     <li>Ω</li>
 *     <li>α<sub>v</sub> = v + ω</li>
 *   </ul>
 * where Ω stands for the Right Ascension of the Ascending Node and
 * α<sub>v</sub> stands for the true latitude argument
 *
 * <p>
 * The conversion equations from and to Keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is circular (but not equatorial), the circular
 * parameters are still unambiguously defined whereas some Keplerian elements
 * (more precisely ω and Ω) become ambiguous. When orbit is equatorial,
 * neither the Keplerian nor the circular parameters can be defined unambiguously.
 * {@link EquinoctialOrbit equinoctial orbits} is the recommended way to represent
 * orbits.
 * </p>
 * <p>
 * The instance <code>CircularOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CartesianOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class CircularOrbit extends Orbit implements PositionAngleBased {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170414L;

    /** Semi-major axis (m). */
    private final double a;

    /** First component of the circular eccentricity vector. */
    private final double ex;

    /** Second component of the circular eccentricity vector. */
    private final double ey;

    /** Inclination (rad). */
    private final double i;

    /** Right Ascension of Ascending Node (rad). */
    private final double raan;

    /** True latitude argument (rad). */
    private final double alphaV;

    /** Semi-major axis derivative (m/s). */
    private final double aDot;

    /** First component of the circular eccentricity vector derivative. */
    private final double exDot;

    /** Second component of the circular eccentricity vector derivative. */
    private final double eyDot;

    /** Inclination derivative (rad/s). */
    private final double iDot;

    /** Right Ascension of Ascending Node derivative (rad/s). */
    private final double raanDot;

    /** True latitude argument derivative (rad/s). */
    private final double alphaVDot;

    /** Indicator for {@link PVCoordinates} serialization. */
    private final boolean serializePV;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private transient PVCoordinates partialPV;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alpha  an + ω, mean, eccentric or true latitude argument (rad)
     * @param type type of latitude argument
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final double a, final double ex, final double ey,
                         final double i, final double raan, final double alpha,
                         final PositionAngleType type,
                         final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(a, ex, ey, i, raan, alpha,
             Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
             type, frame, date, mu);
    }

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alpha  an + ω, mean, eccentric or true latitude argument (rad)
     * @param aDot  semi-major axis derivative (m/s)
     * @param exDot d(e cos(ω))/dt, first component of circular eccentricity vector derivative
     * @param eyDot d(e sin(ω))/dt, second component of circular eccentricity vector derivative
     * @param iDot inclination  derivative(rad/s)
     * @param raanDot right ascension of ascending node derivative (rad/s)
     * @param alphaDot  d(an + ω), mean, eccentric or true latitude argument derivative (rad/s)
     * @param type type of latitude argument
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final double a, final double ex, final double ey,
                         final double i, final double raan, final double alpha,
                         final double aDot, final double exDot, final double eyDot,
                         final double iDot, final double raanDot, final double alphaDot,
                         final PositionAngleType type,
                         final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex * ex + ey * ey >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.a       =  a;
        this.aDot    =  aDot;
        this.ex      = ex;
        this.exDot   = exDot;
        this.ey      = ey;
        this.eyDot   = eyDot;
        this.i       = i;
        this.iDot    = iDot;
        this.raan    = raan;
        this.raanDot = raanDot;

        if (hasDerivatives()) {
            final UnivariateDerivative1 exUD    = new UnivariateDerivative1(ex,    exDot);
            final UnivariateDerivative1 eyUD    = new UnivariateDerivative1(ey,    eyDot);
            final UnivariateDerivative1 alphaUD = new UnivariateDerivative1(alpha, alphaDot);
            final UnivariateDerivative1 alphavUD;
            switch (type) {
                case MEAN :
                    alphavUD = FieldCircularOrbit.eccentricToTrue(FieldCircularOrbit.meanToEccentric(alphaUD, exUD, eyUD), exUD, eyUD);
                    break;
                case ECCENTRIC :
                    alphavUD = FieldCircularOrbit.eccentricToTrue(alphaUD, exUD, eyUD);
                    break;
                case TRUE :
                    alphavUD = alphaUD;
                    break;
                default :
                    throw new OrekitInternalError(null);
            }
            this.alphaV    = alphavUD.getValue();
            this.alphaVDot = alphavUD.getDerivative(1);
        } else {
            switch (type) {
                case MEAN :
                    this.alphaV = eccentricToTrue(meanToEccentric(alpha, ex, ey), ex, ey);
                    break;
                case ECCENTRIC :
                    this.alphaV = eccentricToTrue(alpha, ex, ey);
                    break;
                case TRUE :
                    this.alphaV = alpha;
                    break;
                default :
                    throw new OrekitInternalError(null);
            }
            this.alphaVDot = Double.NaN;
        }

        serializePV = false;
        partialPV   = null;

    }

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alphaV  v + ω, true latitude argument (rad)
     * @param aDot  semi-major axis derivative (m/s)
     * @param exDot d(e cos(ω))/dt, first component of circular eccentricity vector derivative
     * @param eyDot d(e sin(ω))/dt, second component of circular eccentricity vector derivative
     * @param iDot inclination  derivative(rad/s)
     * @param raanDot right ascension of ascending node derivative (rad/s)
     * @param alphaVDot  d(v + ω), true latitude argument derivative (rad/s)
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    private CircularOrbit(final double a, final double ex, final double ey,
                          final double i, final double raan, final double alphaV,
                          final double aDot, final double exDot, final double eyDot,
                          final double iDot, final double raanDot, final double alphaVDot,
                          final TimeStampedPVCoordinates pvCoordinates, final Frame frame,
                          final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);
        this.a           =  a;
        this.aDot        =  aDot;
        this.ex          = ex;
        this.exDot       = exDot;
        this.ey          = ey;
        this.eyDot       = eyDot;
        this.i           = i;
        this.iDot        = iDot;
        this.raan        = raan;
        this.raanDot     = raanDot;
        this.alphaV      = alphaV;
        this.alphaVDot   = alphaVDot;
        this.serializePV = true;
        this.partialPV   = null;
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final TimeStampedPVCoordinates pvCoordinates, final Frame frame, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);

        // compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final Vector3D pvA = pvCoordinates.getAcceleration();
        final double r2 = pvP.getNormSq();
        final double r  = FastMath.sqrt(r2);
        final double V2 = pvV.getNormSq();
        final double rV2OnMu = r * V2 / mu;
        a = r / (2 - rV2OnMu);

        if (!isElliptical()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        // compute inclination
        final Vector3D momentum = pvCoordinates.getMomentum();
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node  = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        raan = FastMath.atan2(node.getY(), node.getX());

        // 2D-coordinates in the canonical frame
        final SinCos scRaan = FastMath.sinCos(raan);
        final SinCos scI    = FastMath.sinCos(i);
        final double xP     = pvP.getX();
        final double yP     = pvP.getY();
        final double zP     = pvP.getZ();
        final double x2     = (xP * scRaan.cos() + yP * scRaan.sin()) / a;
        final double y2     = ((yP * scRaan.cos() - xP * scRaan.sin()) * scI.cos() + zP * scI.sin()) / a;

        // compute eccentricity vector
        final double eSE    = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(mu * a);
        final double eCE    = rV2OnMu - 1;
        final double e2     = eCE * eCE + eSE * eSE;
        final double f      = eCE - e2;
        final double g      = FastMath.sqrt(1 - e2) * eSE;
        final double aOnR   = a / r;
        final double a2OnR2 = aOnR * aOnR;
        ex = a2OnR2 * (f * x2 + g * y2);
        ey = a2OnR2 * (f * y2 - g * x2);

        // compute latitude argument
        final double beta = 1 / (1 + FastMath.sqrt(1 - ex * ex - ey * ey));
        alphaV = eccentricToTrue(FastMath.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE * beta * ey), ex, ey);

        partialPV   = pvCoordinates;

        if (hasNonKeplerianAcceleration(pvCoordinates, mu)) {
            // we have a relevant acceleration, we can compute derivatives

            final double[][] jacobian = new double[6][6];
            getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

            final Vector3D keplerianAcceleration    = new Vector3D(-mu / (r * r2), pvP);
            final Vector3D nonKeplerianAcceleration = pvA.subtract(keplerianAcceleration);
            final double   aX                       = nonKeplerianAcceleration.getX();
            final double   aY                       = nonKeplerianAcceleration.getY();
            final double   aZ                       = nonKeplerianAcceleration.getZ();
            aDot    = jacobian[0][3] * aX + jacobian[0][4] * aY + jacobian[0][5] * aZ;
            exDot   = jacobian[1][3] * aX + jacobian[1][4] * aY + jacobian[1][5] * aZ;
            eyDot   = jacobian[2][3] * aX + jacobian[2][4] * aY + jacobian[2][5] * aZ;
            iDot    = jacobian[3][3] * aX + jacobian[3][4] * aY + jacobian[3][5] * aZ;
            raanDot = jacobian[4][3] * aX + jacobian[4][4] * aY + jacobian[4][5] * aZ;

            // in order to compute true anomaly derivative, we must compute
            // mean anomaly derivative including Keplerian motion and convert to true anomaly
            final double alphaMDot = getKeplerianMeanMotion() +
                                     jacobian[5][3] * aX + jacobian[5][4] * aY + jacobian[5][5] * aZ;
            final UnivariateDerivative1 exUD     = new UnivariateDerivative1(ex, exDot);
            final UnivariateDerivative1 eyUD     = new UnivariateDerivative1(ey, eyDot);
            final UnivariateDerivative1 alphaMUD = new UnivariateDerivative1(getAlphaM(), alphaMDot);
            final UnivariateDerivative1 alphavUD = FieldCircularOrbit.eccentricToTrue(FieldCircularOrbit.meanToEccentric(alphaMUD, exUD, eyUD), exUD, eyUD);
            alphaVDot = alphavUD.getDerivative(1);

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot      = Double.NaN;
            exDot     = Double.NaN;
            eyDot     = Double.NaN;
            iDot      = Double.NaN;
            raanDot   = Double.NaN;
            alphaVDot = Double.NaN;
        }

        serializePV = true;

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                         final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedPVCoordinates(date, pvCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public CircularOrbit(final Orbit op) {

        super(op.getFrame(), op.getDate(), op.getMu());

        a    = op.getA();
        i    = op.getI();
        final double hx = op.getHx();
        final double hy = op.getHy();
        final double h2 = hx * hx + hy * hy;
        final double h  = FastMath.sqrt(h2);
        raan = FastMath.atan2(hy, hx);
        final SinCos scRaan  = FastMath.sinCos(raan);
        final double cosRaan = h == 0 ? scRaan.cos() : hx / h;
        final double sinRaan = h == 0 ? scRaan.sin() : hy / h;
        final double equiEx = op.getEquinoctialEx();
        final double equiEy = op.getEquinoctialEy();
        ex     = equiEx * cosRaan + equiEy * sinRaan;
        ey     = equiEy * cosRaan - equiEx * sinRaan;
        alphaV = op.getLv() - raan;

        if (op.hasDerivatives()) {
            aDot    = op.getADot();
            final double hxDot = op.getHxDot();
            final double hyDot = op.getHyDot();
            iDot    = 2 * (cosRaan * hxDot + sinRaan * hyDot) / (1 + h2);
            raanDot = (hx * hyDot - hy * hxDot) / h2;
            final double equiExDot = op.getEquinoctialExDot();
            final double equiEyDot = op.getEquinoctialEyDot();
            exDot   = (equiExDot + equiEy * raanDot) * cosRaan +
                      (equiEyDot - equiEx * raanDot) * sinRaan;
            eyDot   = (equiEyDot - equiEx * raanDot) * cosRaan -
                      (equiExDot + equiEy * raanDot) * sinRaan;
            alphaVDot = op.getLvDot() - raanDot;
        } else {
            aDot      = Double.NaN;
            exDot     = Double.NaN;
            eyDot     = Double.NaN;
            iDot      = Double.NaN;
            raanDot   = Double.NaN;
            alphaVDot = Double.NaN;
        }

        serializePV = false;
        partialPV   = null;

    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.CIRCULAR;
    }

    /** {@inheritDoc} */
    public double getA() {
        return a;
    }

    /** {@inheritDoc} */
    public double getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        final SinCos sc = FastMath.sinCos(raan);
        return ex * sc.cos() - ey * sc.sin();
    }

    /** {@inheritDoc} */
    public double getEquinoctialExDot() {
        final SinCos sc = FastMath.sinCos(raan);
        return (exDot - ey * raanDot) * sc.cos() - (eyDot + ex * raanDot) * sc.sin();
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        final SinCos sc = FastMath.sinCos(raan);
        return ey * sc.cos() + ex * sc.sin();
    }

    /** {@inheritDoc} */
    public double getEquinoctialEyDot() {
        final SinCos sc = FastMath.sinCos(raan);
        return (eyDot + ex * raanDot) * sc.cos() + (exDot - ey * raanDot) * sc.sin();
    }

    /** Get the first component of the circular eccentricity vector.
     * @return ex = e cos(ω), first component of the circular eccentricity vector
     */
    public double getCircularEx() {
        return ex;
    }

    /** Get the first component of the circular eccentricity vector derivative.
     * @return ex = e cos(ω), first component of the circular eccentricity vector derivative
     * @since 9.0
     */
    public double getCircularExDot() {
        return exDot;
    }

    /** Get the second component of the circular eccentricity vector.
     * @return ey = e sin(ω), second component of the circular eccentricity vector
     */
    public double getCircularEy() {
        return ey;
    }

    /** Get the second component of the circular eccentricity vector derivative.
     * @return ey = e sin(ω), second component of the circular eccentricity vector derivative
     */
    public double getCircularEyDot() {
        return eyDot;
    }

    /** {@inheritDoc} */
    public double getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return  FastMath.cos(raan) * FastMath.tan(i / 2);
    }

    /** {@inheritDoc} */
    public double getHxDot() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        final SinCos sc  = FastMath.sinCos(raan);
        final double tan = FastMath.tan(0.5 * i);
        return 0.5 * sc.cos() * (1 + tan * tan) * iDot - sc.sin() * tan * raanDot;
    }

    /** {@inheritDoc} */
    public double getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return  FastMath.sin(raan) * FastMath.tan(i / 2);
    }

    /** {@inheritDoc} */
    public double getHyDot() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        final SinCos sc  = FastMath.sinCos(raan);
        final double tan = FastMath.tan(0.5 * i);
        return 0.5 * sc.sin() * (1 + tan * tan) * iDot + sc.cos() * tan * raanDot;
    }

    /** Get the true latitude argument.
     * @return v + ω true latitude argument (rad)
     */
    public double getAlphaV() {
        return alphaV;
    }

    /** Get the true latitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return v + ω true latitude argument derivative (rad/s)
     * @since 9.0
     */
    public double getAlphaVDot() {
        return alphaVDot;
    }

    /** Get the eccentric latitude argument.
     * @return E + ω eccentric latitude argument (rad)
     */
    public double getAlphaE() {
        return trueToEccentric(alphaV, ex, ey);
    }

    /** Get the eccentric latitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return d(E + ω)/dt eccentric latitude argument derivative (rad/s)
     * @since 9.0
     */
    public double getAlphaEDot() {
        final UnivariateDerivative1 alphaVUD = new UnivariateDerivative1(alphaV, alphaVDot);
        final UnivariateDerivative1 exUD     = new UnivariateDerivative1(ex,     exDot);
        final UnivariateDerivative1 eyUD     = new UnivariateDerivative1(ey,     eyDot);
        final UnivariateDerivative1 alphaEUD = FieldCircularOrbit.trueToEccentric(alphaVUD, exUD, eyUD);
        return alphaEUD.getDerivative(1);
    }

    /** Get the mean latitude argument.
     * @return M + ω mean latitude argument (rad)
     */
    public double getAlphaM() {
        return eccentricToMean(trueToEccentric(alphaV, ex, ey), ex, ey);
    }

    /** Get the mean latitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return d(M + ω)/dt mean latitude argument derivative (rad/s)
     * @since 9.0
     */
    public double getAlphaMDot() {
        final UnivariateDerivative1 alphaVUD = new UnivariateDerivative1(alphaV, alphaVDot);
        final UnivariateDerivative1 exUD     = new UnivariateDerivative1(ex,     exDot);
        final UnivariateDerivative1 eyUD     = new UnivariateDerivative1(ey,     eyDot);
        final UnivariateDerivative1 alphaMUD = FieldCircularOrbit.eccentricToMean(FieldCircularOrbit.trueToEccentric(alphaVUD, exUD, eyUD), exUD, eyUD);
        return alphaMUD.getDerivative(1);
    }

    /** Get the latitude argument.
     * @param type type of the angle
     * @return latitude argument (rad)
     */
    public double getAlpha(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getAlphaM() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getAlphaE() :
                                                                                   getAlphaV());
    }

    /** Get the latitude argument derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @param type type of the angle
     * @return latitude argument derivative (rad/s)
     * @since 9.0
     */
    public double getAlphaDot(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getAlphaMDot() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getAlphaEDot() :
                                                                                   getAlphaVDot());
    }

    /** Computes the true latitude argument from the eccentric latitude argument.
     * @param alphaE = E + ω eccentric latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @return the true latitude argument.
     */
    public static double eccentricToTrue(final double alphaE, final double ex, final double ey) {
        final double epsilon   = FastMath.sqrt(1 - ex * ex - ey * ey);
        final SinCos scAlphaE  = FastMath.sinCos(alphaE);
        return alphaE + 2 * FastMath.atan((ex * scAlphaE.sin() - ey * scAlphaE.cos()) /
                                      (epsilon + 1 - ex * scAlphaE.cos() - ey * scAlphaE.sin()));
    }

    /** Computes the eccentric latitude argument from the true latitude argument.
     * @param alphaV = V + ω true latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @return the eccentric latitude argument.
     */
    public static double trueToEccentric(final double alphaV, final double ex, final double ey) {
        final double epsilon   = FastMath.sqrt(1 - ex * ex - ey * ey);
        final SinCos scAlphaV  = FastMath.sinCos(alphaV);
        return alphaV + 2 * FastMath.atan((ey * scAlphaV.cos() - ex * scAlphaV.sin()) /
                                      (epsilon + 1 + ex * scAlphaV.cos() + ey * scAlphaV.sin()));
    }

    /** Computes the eccentric latitude argument from the mean latitude argument.
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @return the eccentric latitude argument.
     */
    public static double meanToEccentric(final double alphaM, final double ex, final double ey) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        double alphaE         = alphaM;
        double shift          = 0.0;
        double alphaEMalphaM  = 0.0;
        SinCos scAlphaE       = FastMath.sinCos(alphaE);
        int    iter           = 0;
        do {
            final double f2 = ex * scAlphaE.sin() - ey * scAlphaE.cos();
            final double f1 = 1.0 - ex * scAlphaE.cos() - ey * scAlphaE.sin();
            final double f0 = alphaEMalphaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            alphaEMalphaM -= shift;
            alphaE         = alphaM + alphaEMalphaM;
            scAlphaE       = FastMath.sinCos(alphaE);

        } while (++iter < 50 && FastMath.abs(shift) > 1.0e-12);

        return alphaE;

    }

    /** Computes the mean latitude argument from the eccentric latitude argument.
     * @param alphaE = E + ω  mean latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @return the mean latitude argument.
     */
    public static double eccentricToMean(final double alphaE, final double ex, final double ey) {
        final SinCos scAlphaE = FastMath.sinCos(alphaE);
        return alphaE + (ey * scAlphaE.cos() - ex * scAlphaE.sin());
    }

    /** {@inheritDoc} */
    public double getE() {
        return FastMath.sqrt(ex * ex + ey * ey);
    }

    /** {@inheritDoc} */
    public double getEDot() {
        return (ex * exDot + ey * eyDot) / getE();
    }

    /** {@inheritDoc} */
    public double getI() {
        return i;
    }

    /** {@inheritDoc} */
    public double getIDot() {
        return iDot;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the right ascension of the ascending node derivative.
     * <p>
     * If the orbit was created without derivatives, the value returned is {@link Double#NaN}.
     * </p>
     * @return right ascension of the ascending node derivative (rad/s)
     * @since 9.0
     */
    public double getRightAscensionOfAscendingNodeDot() {
        return raanDot;
    }

    /** {@inheritDoc} */
    public double getLv() {
        return alphaV + raan;
    }

    /** {@inheritDoc} */
    public double getLvDot() {
        return alphaVDot + raanDot;
    }

    /** {@inheritDoc} */
    public double getLE() {
        return getAlphaE() + raan;
    }

    /** {@inheritDoc} */
    public double getLEDot() {
        return getAlphaEDot() + raanDot;
    }

    /** {@inheritDoc} */
    public double getLM() {
        return getAlphaM() + raan;
    }

    /** {@inheritDoc} */
    public double getLMDot() {
        return getAlphaMDot() + raanDot;
    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

        // get equinoctial parameters
        final double equEx = getEquinoctialEx();
        final double equEy = getEquinoctialEy();
        final double hx = getHx();
        final double hy = getHy();
        final double lE = getLE();

        // inclination-related intermediate parameters
        final double hx2   = hx * hx;
        final double hy2   = hy * hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy =  2 * hx * hy * factH;
        final double uz = -2 * hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz =  2 * hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = equEx * equEy;
        final double ex2  = equEx * equEx;
        final double ey2  = equEy * equEy;
        final double e2   = ex2 + ey2;
        final double eta  = 1 + FastMath.sqrt(1 - e2);
        final double beta = 1. / eta;

        // eccentric latitude argument
        final SinCos scLe   = FastMath.sinCos(lE);
        final double cLe    = scLe.cos();
        final double sLe    = scLe.sin();
        final double exCeyS = equEx * cLe + equEy * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - equEx);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - equEy);

        final double factor = FastMath.sqrt(getMu() / a) / (1 - exCeyS);
        final double xdot   = factor * (-sLe + beta * equEy * exCeyS);
        final double ydot   = factor * ( cLe - beta * equEx * exCeyS);

        final Vector3D position =
                        new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final Vector3D velocity =
                        new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot * uz + ydot * vz);

        partialPV = new PVCoordinates(position, velocity);

    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * <p>
     * This method should be called only when {@link #hasDerivatives()} returns true.
     * </p>
     * @return non-Keplerian part of the acceleration
     */
    private Vector3D nonKeplerianAcceleration() {

        final double[][] dCdP = new double[6][6];
        getJacobianWrtParameters(PositionAngleType.MEAN, dCdP);

        final double nonKeplerianMeanMotion = getAlphaMDot() - getKeplerianMeanMotion();
        final double nonKeplerianAx = dCdP[3][0] * aDot    + dCdP[3][1] * exDot   + dCdP[3][2] * eyDot   +
                                      dCdP[3][3] * iDot    + dCdP[3][4] * raanDot + dCdP[3][5] * nonKeplerianMeanMotion;
        final double nonKeplerianAy = dCdP[4][0] * aDot    + dCdP[4][1] * exDot   + dCdP[4][2] * eyDot   +
                                      dCdP[4][3] * iDot    + dCdP[4][4] * raanDot + dCdP[4][5] * nonKeplerianMeanMotion;
        final double nonKeplerianAz = dCdP[5][0] * aDot    + dCdP[5][1] * exDot   + dCdP[5][2] * eyDot   +
                                      dCdP[5][3] * iDot    + dCdP[5][4] * raanDot + dCdP[5][5] * nonKeplerianMeanMotion;

        return new Vector3D(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);

    }

    /** {@inheritDoc} */
    protected Vector3D initPosition() {

        // get equinoctial parameters
        final double equEx = getEquinoctialEx();
        final double equEy = getEquinoctialEy();
        final double hx = getHx();
        final double hy = getHy();
        final double lE = getLE();

        // inclination-related intermediate parameters
        final double hx2   = hx * hx;
        final double hy2   = hy * hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy =  2 * hx * hy * factH;
        final double uz = -2 * hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz =  2 * hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = equEx * equEy;
        final double ex2  = equEx * equEx;
        final double ey2  = equEy * equEy;
        final double e2   = ex2 + ey2;
        final double eta  = 1 + FastMath.sqrt(1 - e2);
        final double beta = 1. / eta;

        // eccentric latitude argument
        final SinCos scLe   = FastMath.sinCos(lE);
        final double cLe    = scLe.cos();
        final double sLe    = scLe.sin();

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - equEx);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - equEy);

        return new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);

    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final double r2 = partialPV.getPosition().getNormSq();
        final Vector3D keplerianAcceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), partialPV.getPosition());
        final Vector3D acceleration = hasDerivatives() ?
                                      keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                      keplerianAcceleration;

        return new TimeStampedPVCoordinates(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    public CircularOrbit shiftedBy(final double dt) {

        // use Keplerian-only motion
        final CircularOrbit keplerianShifted = new CircularOrbit(a, ex, ey, i, raan,
                                                                 getAlphaM() + getKeplerianMeanMotion() * dt,
                                                                 PositionAngleType.MEAN, getFrame(),
                                                                 getDate().shiftedBy(dt), getMu());

        if (hasDerivatives()) {

            // extract non-Keplerian acceleration from first time derivatives
            final Vector3D nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final Vector3D fixedP   = new Vector3D(1, keplerianShifted.partialPV.getPosition(),
                                                   0.5 * dt * dt, nonKeplerianAcceleration);
            final double   fixedR2 = fixedP.getNormSq();
            final double   fixedR  = FastMath.sqrt(fixedR2);
            final Vector3D fixedV  = new Vector3D(1, keplerianShifted.partialPV.getVelocity(),
                                                  dt, nonKeplerianAcceleration);
            final Vector3D fixedA  = new Vector3D(-getMu() / (fixedR2 * fixedR), keplerianShifted.partialPV.getPosition(),
                                                  1, nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new CircularOrbit(new TimeStampedPVCoordinates(keplerianShifted.getDate(),
                                                                  fixedP, fixedV, fixedA),
                                     keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianMeanWrtCartesian() {


        final double[][] jacobian = new double[6][6];

        computePVWithoutA();
        final Vector3D position = partialPV.getPosition();
        final Vector3D velocity = partialPV.getVelocity();
        final double x          = position.getX();
        final double y          = position.getY();
        final double z          = position.getZ();
        final double vx         = velocity.getX();
        final double vy         = velocity.getY();
        final double vz         = velocity.getZ();
        final double pv         = Vector3D.dotProduct(position, velocity);
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double v2         = velocity.getNormSq();

        final double mu         = getMu();
        final double oOsqrtMuA  = 1 / FastMath.sqrt(mu * a);
        final double rOa        = r / a;
        final double aOr        = a / r;
        final double aOr2       = a / r2;
        final double a2         = a * a;

        final double ex2        = ex * ex;
        final double ey2        = ey * ey;
        final double e2         = ex2 + ey2;
        final double epsilon    = FastMath.sqrt(1 - e2);
        final double beta       = 1 / (1 + epsilon);

        final double eCosE      = 1 - rOa;
        final double eSinE      = pv * oOsqrtMuA;

        final SinCos scI    = FastMath.sinCos(i);
        final SinCos scRaan = FastMath.sinCos(raan);
        final double cosI       = scI.cos();
        final double sinI       = scI.sin();
        final double cosRaan    = scRaan.cos();
        final double sinRaan    = scRaan.sin();

        // da
        fillHalfRow(2 * aOr * aOr2, position, jacobian[0], 0);
        fillHalfRow(2 * a2 / mu, velocity, jacobian[0], 3);

        // differentials of the normalized momentum
        final Vector3D danP = new Vector3D(v2, position, -pv, velocity);
        final Vector3D danV = new Vector3D(r2, velocity, -pv, position);
        final double recip  = 1 / partialPV.getMomentum().getNorm();
        final double recip2 = recip * recip;
        final Vector3D dwXP = new Vector3D(recip, new Vector3D(  0,  vz, -vy), -recip2 * sinRaan * sinI, danP);
        final Vector3D dwYP = new Vector3D(recip, new Vector3D(-vz,   0,  vx),  recip2 * cosRaan * sinI, danP);
        final Vector3D dwZP = new Vector3D(recip, new Vector3D( vy, -vx,   0), -recip2 * cosI,           danP);
        final Vector3D dwXV = new Vector3D(recip, new Vector3D(  0,  -z,   y), -recip2 * sinRaan * sinI, danV);
        final Vector3D dwYV = new Vector3D(recip, new Vector3D(  z,   0,  -x),  recip2 * cosRaan * sinI, danV);
        final Vector3D dwZV = new Vector3D(recip, new Vector3D( -y,   x,   0), -recip2 * cosI,           danV);

        // di
        fillHalfRow(sinRaan * cosI, dwXP, -cosRaan * cosI, dwYP, -sinI, dwZP, jacobian[3], 0);
        fillHalfRow(sinRaan * cosI, dwXV, -cosRaan * cosI, dwYV, -sinI, dwZV, jacobian[3], 3);

        // dRaan
        fillHalfRow(sinRaan / sinI, dwYP, cosRaan / sinI, dwXP, jacobian[4], 0);
        fillHalfRow(sinRaan / sinI, dwYV, cosRaan / sinI, dwXV, jacobian[4], 3);

        // orbital frame: (p, q, w) p along ascending node, w along momentum
        // the coordinates of the spacecraft in this frame are: (u, v, 0)
        final double u     =  x * cosRaan + y * sinRaan;
        final double cv    = -x * sinRaan + y * cosRaan;
        final double v     = cv * cosI + z * sinI;

        // du
        final Vector3D duP = new Vector3D(cv * cosRaan / sinI, dwXP,
                                          cv * sinRaan / sinI, dwYP,
                                          1, new Vector3D(cosRaan, sinRaan, 0));
        final Vector3D duV = new Vector3D(cv * cosRaan / sinI, dwXV,
                                          cv * sinRaan / sinI, dwYV);

        // dv
        final Vector3D dvP = new Vector3D(-u * cosRaan * cosI / sinI + sinRaan * z, dwXP,
                                          -u * sinRaan * cosI / sinI - cosRaan * z, dwYP,
                                          cv, dwZP,
                                          1, new Vector3D(-sinRaan * cosI, cosRaan * cosI, sinI));
        final Vector3D dvV = new Vector3D(-u * cosRaan * cosI / sinI + sinRaan * z, dwXV,
                                          -u * sinRaan * cosI / sinI - cosRaan * z, dwYV,
                                          cv, dwZV);

        final Vector3D dc1P = new Vector3D(aOr2 * (2 * eSinE * eSinE + 1 - eCosE) / r2, position,
                                            -2 * aOr2 * eSinE * oOsqrtMuA, velocity);
        final Vector3D dc1V = new Vector3D(-2 * aOr2 * eSinE * oOsqrtMuA, position,
                                            2 / mu, velocity);
        final Vector3D dc2P = new Vector3D(aOr2 * eSinE * (eSinE * eSinE - (1 - e2)) / (r2 * epsilon), position,
                                            aOr2 * (1 - e2 - eSinE * eSinE) * oOsqrtMuA / epsilon, velocity);
        final Vector3D dc2V = new Vector3D(aOr2 * (1 - e2 - eSinE * eSinE) * oOsqrtMuA / epsilon, position,
                                            eSinE / (mu * epsilon), velocity);

        final double cof1   = aOr2 * (eCosE - e2);
        final double cof2   = aOr2 * epsilon * eSinE;
        final Vector3D dexP = new Vector3D(u, dc1P,  v, dc2P, cof1, duP,  cof2, dvP);
        final Vector3D dexV = new Vector3D(u, dc1V,  v, dc2V, cof1, duV,  cof2, dvV);
        final Vector3D deyP = new Vector3D(v, dc1P, -u, dc2P, cof1, dvP, -cof2, duP);
        final Vector3D deyV = new Vector3D(v, dc1V, -u, dc2V, cof1, dvV, -cof2, duV);
        fillHalfRow(1, dexP, jacobian[1], 0);
        fillHalfRow(1, dexV, jacobian[1], 3);
        fillHalfRow(1, deyP, jacobian[2], 0);
        fillHalfRow(1, deyV, jacobian[2], 3);

        final double cle = u / a + ex - eSinE * beta * ey;
        final double sle = v / a + ey + eSinE * beta * ex;
        final double m1  = beta * eCosE;
        final double m2  = 1 - m1 * eCosE;
        final double m3  = (u * ey - v * ex) + eSinE * beta * (u * ex + v * ey);
        final double m4  = -sle + cle * eSinE * beta;
        final double m5  = cle + sle * eSinE * beta;
        fillHalfRow((2 * m3 / r + aOr * eSinE + m1 * eSinE * (1 + m1 - (1 + aOr) * m2) / epsilon) / r2, position,
                    (m1 * m2 / epsilon - 1) * oOsqrtMuA, velocity,
                    m4, dexP, m5, deyP, -sle / a, duP, cle / a, dvP,
                    jacobian[5], 0);
        fillHalfRow((m1 * m2 / epsilon - 1) * oOsqrtMuA, position,
                    (2 * m3 + eSinE * a + m1 * eSinE * r * (eCosE * beta * 2 - aOr * m2) / epsilon) / mu, velocity,
                    m4, dexV, m5, deyV, -sle / a, duV, cle / a, dvV,
                    jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation aM = aE - ex sin aE + ey cos aE leads to:
        // daM = (1 - ex cos aE - ey sin aE) daE - sin aE dex + cos aE dey
        // which is inverted and rewritten as:
        // daE = a/r daM + sin aE a/r dex - cos aE a/r dey
        final double alphaE = getAlphaE();
        final SinCos scAe   = FastMath.sinCos(alphaE);
        final double cosAe  = scAe.cos();
        final double sinAe  = scAe.sin();
        final double aOr    = 1 / (1 - ex * cosAe - ey * sinAe);

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL  = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = aOr * (rowL[j] + sinAe * rowEx[j] - cosAe * rowEy[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesian();

        // Differentiating the eccentric latitude equation
        // tan((aV - aE)/2) = [ex sin aE - ey cos aE] / [sqrt(1-ex^2-ey^2) + 1 - ex cos aE - ey sin aE]
        // leads to
        // cT (daV - daE) = cE daE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin aE - ey cos aE)^2] / 2
        // d  = 1 + sqrt(1-ex^2-ey^2) - ex cos aE - ey sin aE
        // cE = (ex cos aE + ey sin aE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX =  sin aE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin aE - ey cos aE) / sqrt(1-ex^2-ey^2)
        // cY = -cos aE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin aE - ey cos aE) / sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true latitude
        // daV = (cT + cE) / cT daE + cX / cT deX + cY / cT deX
        final double alphaE    = getAlphaE();
        final SinCos scAe      = FastMath.sinCos(alphaE);
        final double cosAe     = scAe.cos();
        final double sinAe     = scAe.sin();
        final double eSinE     = ex * sinAe - ey * cosAe;
        final double ecosE     = ex * cosAe + ey * sinAe;
        final double e2        = ex * ex + ey * ey;
        final double epsilon   = FastMath.sqrt(1 - e2);
        final double onePeps   = 1 + epsilon;
        final double d         = onePeps - ecosE;
        final double cT        = (d * d + eSinE * eSinE) / 2;
        final double cE        = ecosE * onePeps - e2;
        final double cX        = ex * eSinE / epsilon - ey + sinAe * onePeps;
        final double cY        = ey * eSinE / epsilon + ex - cosAe * onePeps;
        final double factorLe  = (cT + cE) / cT;
        final double factorEx  = cX / cT;
        final double factorEy  = cY / cT;

        // update latitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowA = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowA[j] = factorLe * rowA[j] + factorEx * rowEx[j] + factorEy * rowEy[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngleType type, final double gm,
                                      final double[] pDot) {
        final double oMe2;
        final double ksi;
        final double n  = FastMath.sqrt(gm / a) / a;
        final SinCos sc = FastMath.sinCos(alphaV);
        switch (type) {
            case MEAN :
                pDot[5] += n;
                break;
            case ECCENTRIC :
                oMe2  = 1 - ex * ex - ey * ey;
                ksi   = 1 + ex * sc.cos() + ey * sc.sin();
                pDot[5] += n * ksi / oMe2;
                break;
            case TRUE :
                oMe2  = 1 - ex * ex - ey * ey;
                ksi   = 1 + ex * sc.cos() + ey * sc.sin();
                pDot[5] += n * ksi * ksi / (oMe2 * FastMath.sqrt(oMe2));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuilder().append("circular parameters: ").append('{').
                                  append("a: ").append(a).
                                  append(", ex: ").append(ex).append(", ey: ").append(ey).
                                  append(", i: ").append(FastMath.toDegrees(i)).
                                  append(", raan: ").append(FastMath.toDegrees(raan)).
                                  append(", alphaV: ").append(FastMath.toDegrees(alphaV)).
                                  append(";}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getCachedPositionAngleType() {
        return PositionAngleType.TRUE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasRates() {
        return hasDerivatives();
    }

    /** {@inheritDoc} */
    @Override
    public CircularOrbit removeRates() {
        final PositionAngleType positionAngleType = getCachedPositionAngleType();
        return new CircularOrbit(getA(), getCircularEx(), getCircularEy(), getI(), getRightAscensionOfAscendingNode(),
                getAlpha(positionAngleType), positionAngleType, getFrame(), getDate(), getMu());
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170414L;

        /** Double values. */
        private double[] d;

        /** Frame in which are defined the orbital parameters. */
        private final Frame frame;

        /** Simple constructor.
         * @param orbit instance to serialize
         */
        private DTO(final CircularOrbit orbit) {

            final AbsoluteDate date = orbit.getDate();

            // decompose date
            final AbsoluteDate j2000Epoch =
                    DataContext.getDefault().getTimeScales().getJ2000Epoch();
            final double epoch  = FastMath.floor(date.durationFrom(j2000Epoch));
            final double offset = date.durationFrom(j2000Epoch.shiftedBy(epoch));

            if (orbit.serializePV) {
                final TimeStampedPVCoordinates pv = orbit.getPVCoordinates();
                if (orbit.hasDerivatives()) {
                    this.d = new double[] {
                        // date + mu + orbit + derivatives + Cartesian : 24 parameters
                        epoch, offset, orbit.getMu(),
                        orbit.a, orbit.ex, orbit.ey,
                        orbit.i, orbit.raan, orbit.alphaV,
                        orbit.aDot, orbit.exDot, orbit.eyDot,
                        orbit.iDot, orbit.raanDot, orbit.alphaVDot,
                        pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                        pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                        pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ(),
                    };
                } else {
                    this.d = new double[] {
                        // date + mu + orbit + Cartesian : 18 parameters
                        epoch, offset, orbit.getMu(),
                        orbit.a, orbit.ex, orbit.ey,
                        orbit.i, orbit.raan, orbit.alphaV,
                        pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                        pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                        pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ(),
                    };
                }
            } else {
                if (orbit.hasDerivatives()) {
                    // date + mu + orbit + derivatives: 15 parameters
                    this.d = new double[] {
                        epoch, offset, orbit.getMu(),
                        orbit.a, orbit.ex, orbit.ey,
                        orbit.i, orbit.raan, orbit.alphaV,
                        orbit.aDot, orbit.exDot, orbit.eyDot,
                        orbit.iDot, orbit.raanDot, orbit.alphaVDot
                    };
                } else {
                    // date + mu + orbit: 9 parameters
                    this.d = new double[] {
                        epoch, offset, orbit.getMu(),
                        orbit.a, orbit.ex, orbit.ey,
                        orbit.i, orbit.raan, orbit.alphaV
                    };
                }
            }

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link CircularOrbit}.
         * @return replacement {@link CircularOrbit}
         */
        private Object readResolve() {
            final AbsoluteDate j2000Epoch =
                    DataContext.getDefault().getTimeScales().getJ2000Epoch();
            switch (d.length) {
                case 24 : // date + mu + orbit + derivatives + Cartesian
                    return new CircularOrbit(d[ 3], d[ 4], d[ 5], d[ 6], d[ 7], d[ 8],
                                             d[ 9], d[10], d[11], d[12], d[13], d[14],
                                             new TimeStampedPVCoordinates(j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                                                          new Vector3D(d[15], d[16], d[17]),
                                                                          new Vector3D(d[18], d[19], d[20]),
                                                                          new Vector3D(d[21], d[22], d[23])),
                                             frame,
                                             d[2]);
                case 18 : // date + mu + orbit + Cartesian
                    return new CircularOrbit(d[3], d[4], d[5], d[6], d[7], d[8],
                                             Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                                             new TimeStampedPVCoordinates(j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                                                          new Vector3D(d[ 9], d[10], d[11]),
                                                                          new Vector3D(d[12], d[13], d[14]),
                                                                          new Vector3D(d[15], d[16], d[17])),
                                             frame,
                                             d[2]);
                case 15 : // date + mu + orbit + derivatives
                    return new CircularOrbit(d[ 3], d[ 4], d[ 5], d[ 6], d[ 7], d[ 8],
                                             d[ 9], d[10], d[11], d[12], d[13], d[14],
                                             PositionAngleType.TRUE,
                                             frame, j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                             d[2]);
                default : // date + mu + orbit
                    return new CircularOrbit(d[3], d[4], d[5], d[6], d[7], d[8], PositionAngleType.TRUE,
                                             frame, j2000Epoch.shiftedBy(d[0]).shiftedBy(d[1]),
                                             d[2]);

            }
        }

    }

}
