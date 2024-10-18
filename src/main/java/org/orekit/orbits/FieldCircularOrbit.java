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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


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
 * @since 9.0
 * @param <T> type of the field elements
 */

public class FieldCircularOrbit<T extends CalculusFieldElement<T>> extends FieldOrbit<T> implements PositionAngleBased {

    /** Semi-major axis (m). */
    private final T a;

    /** First component of the circular eccentricity vector. */
    private final T ex;

    /** Second component of the circular eccentricity vector. */
    private final T ey;

    /** Inclination (rad). */
    private final T i;

    /** Right Ascension of Ascending Node (rad). */
    private final T raan;

    /** Cached latitude argument (rad). */
    private final T cachedAlpha;

    /** Type of cached position angle (latitude argument). */
    private final PositionAngleType cachedPositionAngleType;

    /** Semi-major axis derivative (m/s). */
    private final T aDot;

    /** First component of the circular eccentricity vector derivative. */
    private final T exDot;

    /** Second component of the circular eccentricity vector derivative. */
    private final T eyDot;

    /** Inclination derivative (rad/s). */
    private final T iDot;

    /** Right Ascension of Ascending Node derivative (rad/s). */
    private final T raanDot;

    /** True latitude argument derivative (rad/s). */
    private final T cachedAlphaDot;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private FieldPVCoordinates<T> partialPV;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param alpha  an + ω, mean, eccentric or true latitude argument (rad)
     * @param type type of latitude argument
     * @param cachedPositionAngleType type of cached latitude argument
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     * @since 12.1
     */
    public FieldCircularOrbit(final T a, final T ex, final T ey, final T i, final T raan,
                              final T alpha, final PositionAngleType type,
                              final PositionAngleType cachedPositionAngleType,
                              final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(a, ex, ey, i, raan, alpha,
             null, null, null, null, null, null,
             type, cachedPositionAngleType, frame, date, mu);
    }

    /** Creates a new instance without derivatives and with cached position angle same as value inputted.
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
    public FieldCircularOrbit(final T a, final T ex, final T ey, final T i, final T raan,
                              final T alpha, final PositionAngleType type,
                              final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
            throws IllegalArgumentException {
        this(a, ex, ey, i, raan, alpha, type, type, frame, date, mu);
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
    public FieldCircularOrbit(final T a, final T ex, final T ey,
                              final T i, final T raan, final T alpha,
                              final T aDot, final T exDot, final T eyDot,
                              final T iDot, final T raanDot, final T alphaDot, final PositionAngleType type,
                              final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
            throws IllegalArgumentException {
        this(a, ex, ey, i, raan, alpha, aDot, exDot, eyDot, iDot, raanDot, alphaDot, type, type, frame, date, mu);
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
     * @param cachedPositionAngleType type of cached latitude argument
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     * @since 12.1
     */
    public FieldCircularOrbit(final T a, final T ex, final T ey,
                              final T i, final T raan, final T alpha,
                              final T aDot, final T exDot, final T eyDot,
                              final T iDot, final T raanDot, final T alphaDot,
                              final PositionAngleType type, final PositionAngleType cachedPositionAngleType,
                              final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex.getReal() * ex.getReal() + ey.getReal() * ey.getReal() >= 1.0) {
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
        this.cachedPositionAngleType = cachedPositionAngleType;

        if (hasDerivatives()) {
            final FieldUnivariateDerivative1<T> alphaUD = initializeCachedAlpha(alpha, alphaDot, type);
            this.cachedAlpha = alphaUD.getValue();
            this.cachedAlphaDot = alphaUD.getFirstDerivative();
        } else {
            this.cachedAlpha = initializeCachedAlpha(alpha, type);
            this.cachedAlphaDot = null;
        }

        partialPV   = null;

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the {@link FieldPVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCircularOrbit(final TimeStampedFieldPVCoordinates<T> pvCoordinates,
                              final Frame frame, final T mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);
        this.cachedPositionAngleType = PositionAngleType.TRUE;

        // compute semi-major axis
        final FieldVector3D<T> pvP = pvCoordinates.getPosition();
        final FieldVector3D<T> pvV = pvCoordinates.getVelocity();
        final FieldVector3D<T> pvA = pvCoordinates.getAcceleration();
        final T r2 = pvP.getNormSq();
        final T r  = r2.sqrt();
        final T V2 = pvV.getNormSq();
        final T rV2OnMu = r.multiply(V2).divide(mu);

        a = r.divide(rV2OnMu.negate().add(2));

        if (!isElliptical()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        // compute inclination
        final FieldVector3D<T> momentum = pvCoordinates.getMomentum();
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(r.getField());
        i = FieldVector3D.angle(momentum, plusK);

        // compute right ascension of ascending node
        final FieldVector3D<T> node  = FieldVector3D.crossProduct(plusK, momentum);
        raan = node.getY().atan2(node.getX());

        // 2D-coordinates in the canonical frame
        final FieldSinCos<T> scRaan = FastMath.sinCos(raan);
        final FieldSinCos<T> scI    = FastMath.sinCos(i);
        final T xP      = pvP.getX();
        final T yP      = pvP.getY();
        final T zP      = pvP.getZ();
        final T x2      = (xP.multiply(scRaan.cos()).add(yP .multiply(scRaan.sin()))).divide(a);
        final T y2      = (yP.multiply(scRaan.cos()).subtract(xP.multiply(scRaan.sin()))).multiply(scI.cos()).add(zP.multiply(scI.sin())).divide(a);

        // compute eccentricity vector
        final T eSE    = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(mu).sqrt());
        final T eCE    = rV2OnMu.subtract(1);
        final T e2     = eCE.multiply(eCE).add(eSE.multiply(eSE));
        final T f      = eCE.subtract(e2);
        final T g      = eSE.multiply(e2.negate().add(1).sqrt());
        final T aOnR   = a.divide(r);
        final T a2OnR2 = aOnR.square();
        ex = a2OnR2.multiply(f.multiply(x2).add(g.multiply(y2)));
        ey = a2OnR2.multiply(f.multiply(y2).subtract(g.multiply(x2)));

        // compute latitude argument
        final T beta = (ex.multiply(ex).add(ey.multiply(ey)).negate().add(1)).sqrt().add(1).reciprocal();
        cachedAlpha = FieldCircularLatitudeArgumentUtility.eccentricToTrue(ex, ey, y2.add(ey).add(eSE.multiply(beta).multiply(ex)).atan2(x2.add(ex).subtract(eSE.multiply(beta).multiply(ey)))
        );

        partialPV = pvCoordinates;

        if (hasNonKeplerianAcceleration(pvCoordinates, mu)) {
            // we have a relevant acceleration, we can compute derivatives

            final T[][] jacobian = MathArrays.buildArray(a.getField(), 6, 6);
            getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

            final FieldVector3D<T> keplerianAcceleration    = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(mu.negate()), pvP);
            final FieldVector3D<T> nonKeplerianAcceleration = pvA.subtract(keplerianAcceleration);
            final T   aX                       = nonKeplerianAcceleration.getX();
            final T   aY                       = nonKeplerianAcceleration.getY();
            final T   aZ                       = nonKeplerianAcceleration.getZ();
            aDot    = jacobian[0][3].multiply(aX).add(jacobian[0][4].multiply(aY)).add(jacobian[0][5].multiply(aZ));
            exDot   = jacobian[1][3].multiply(aX).add(jacobian[1][4].multiply(aY)).add(jacobian[1][5].multiply(aZ));
            eyDot   = jacobian[2][3].multiply(aX).add(jacobian[2][4].multiply(aY)).add(jacobian[2][5].multiply(aZ));
            iDot    = jacobian[3][3].multiply(aX).add(jacobian[3][4].multiply(aY)).add(jacobian[3][5].multiply(aZ));
            raanDot = jacobian[4][3].multiply(aX).add(jacobian[4][4].multiply(aY)).add(jacobian[4][5].multiply(aZ));

            // in order to compute true anomaly derivative, we must compute
            // mean anomaly derivative including Keplerian motion and convert to true anomaly
            final T alphaMDot = getKeplerianMeanMotion().
                                add(jacobian[5][3].multiply(aX)).add(jacobian[5][4].multiply(aY)).add(jacobian[5][5].multiply(aZ));
            final FieldUnivariateDerivative1<T> exUD     = new FieldUnivariateDerivative1<>(ex, exDot);
            final FieldUnivariateDerivative1<T> eyUD     = new FieldUnivariateDerivative1<>(ey, eyDot);
            final FieldUnivariateDerivative1<T> alphaMUD = new FieldUnivariateDerivative1<>(getAlphaM(), alphaMDot);
            final FieldUnivariateDerivative1<T> alphavUD = FieldCircularLatitudeArgumentUtility.meanToTrue(exUD, eyUD, alphaMUD);
            cachedAlphaDot = alphavUD.getFirstDerivative();

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot      = null;
            exDot     = null;
            eyDot     = null;
            iDot      = null;
            raanDot   = null;
            cachedAlphaDot = null;
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(CalculusFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param PVCoordinates the {@link FieldPVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCircularOrbit(final FieldPVCoordinates<T> PVCoordinates, final Frame frame,
                              final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(new TimeStampedFieldPVCoordinates<>(date, PVCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldCircularOrbit(final FieldOrbit<T> op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a    = op.getA();
        i    = op.getI();
        final T hx = op.getHx();
        final T hy = op.getHy();
        final T h2 = hx.square().add(hy.square());
        final T h  = h2.sqrt();
        raan = hy.atan2(hx);
        final FieldSinCos<T> scRaan = FastMath.sinCos(raan);
        final T cosRaan = h.getReal() == 0 ? scRaan.cos() : hx.divide(h);
        final T sinRaan = h.getReal() == 0 ? scRaan.sin() : hy.divide(h);
        final T equiEx = op.getEquinoctialEx();
        final T equiEy = op.getEquinoctialEy();
        ex   = equiEx.multiply(cosRaan).add(equiEy.multiply(sinRaan));
        ey   = equiEy.multiply(cosRaan).subtract(equiEx.multiply(sinRaan));
        cachedPositionAngleType = PositionAngleType.TRUE;
        cachedAlpha = op.getLv().subtract(raan);

        if (op.hasDerivatives()) {
            aDot      = op.getADot();
            final T      hxDot = op.getHxDot();
            final T      hyDot = op.getHyDot();
            iDot      = cosRaan.multiply(hxDot).add(sinRaan.multiply(hyDot)).multiply(2).divide(h2.add(1));
            raanDot   = hx.multiply(hyDot).subtract(hy.multiply(hxDot)).divide(h2);
            final T equiExDot = op.getEquinoctialExDot();
            final T equiEyDot = op.getEquinoctialEyDot();
            exDot     = equiExDot.add(equiEy.multiply(raanDot)).multiply(cosRaan).
                        add(equiEyDot.subtract(equiEx.multiply(raanDot)).multiply(sinRaan));
            eyDot     = equiEyDot.subtract(equiEx.multiply(raanDot)).multiply(cosRaan).
                        subtract(equiExDot.add(equiEy.multiply(raanDot)).multiply(sinRaan));
            cachedAlphaDot = op.getLvDot().subtract(raanDot);
        } else {
            aDot      = null;
            exDot     = null;
            eyDot     = null;
            iDot      = null;
            raanDot   = null;
            cachedAlphaDot = null;
        }

        partialPV = null;

    }

    /** Constructor from Field and CircularOrbit.
     * <p>Build a FieldCircularOrbit from non-Field CircularOrbit.</p>
     * @param field CalculusField to base object on
     * @param op non-field orbit with only "constant" terms
     * @since 12.0
     */
    public FieldCircularOrbit(final Field<T> field, final CircularOrbit op) {
        super(op.getFrame(), new FieldAbsoluteDate<>(field, op.getDate()), field.getZero().newInstance(op.getMu()));

        a    = getZero().newInstance(op.getA());
        i    = getZero().newInstance(op.getI());
        raan = getZero().newInstance(op.getRightAscensionOfAscendingNode());
        ex   = getZero().newInstance(op.getCircularEx());
        ey   = getZero().newInstance(op.getCircularEy());
        cachedPositionAngleType = op.getCachedPositionAngleType();
        cachedAlpha = getZero().newInstance(op.getAlpha(cachedPositionAngleType));

        if (op.hasDerivatives()) {
            aDot      = getZero().newInstance(op.getADot());
            iDot      = getZero().newInstance(op.getIDot());
            raanDot   = getZero().newInstance(op.getRightAscensionOfAscendingNodeDot());
            exDot     = getZero().newInstance(op.getCircularExDot());
            eyDot     = getZero().newInstance(op.getCircularEyDot());
            cachedAlphaDot = getZero().newInstance(op.getAlphaDot(cachedPositionAngleType));
        } else {
            aDot      = null;
            exDot     = null;
            eyDot     = null;
            iDot      = null;
            raanDot   = null;
            cachedAlphaDot = null;
        }

        partialPV = null;

    }

    /** Constructor from Field and Orbit.
     * <p>Build a FieldCircularOrbit from any non-Field Orbit.</p>
     * @param field CalculusField to base object on
     * @param op non-field orbit with only "constant" terms
     * @since 12.0
     */
    public FieldCircularOrbit(final Field<T> field, final Orbit op) {
        this(field, (CircularOrbit) OrbitType.CIRCULAR.convertType(op));
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.CIRCULAR;
    }

    /** {@inheritDoc} */
    @Override
    public T getA() {
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public T getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialEx() {
        final FieldSinCos<T> sc = FastMath.sinCos(raan);
        return ex.multiply(sc.cos()).subtract(ey.multiply(sc.sin()));
    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialExDot() {

        if (!hasDerivatives()) {
            return null;
        }

        final FieldSinCos<T> sc = FastMath.sinCos(raan);
        return exDot.subtract(ey.multiply(raanDot)).multiply(sc.cos()).
               subtract(eyDot.add(ex.multiply(raanDot)).multiply(sc.sin()));

    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialEy() {
        final FieldSinCos<T> sc = FastMath.sinCos(raan);
        return ey.multiply(sc.cos()).add(ex.multiply(sc.sin()));
    }

    /** {@inheritDoc} */
    @Override
    public T getEquinoctialEyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        final FieldSinCos<T> sc = FastMath.sinCos(raan);
        return eyDot.add(ex.multiply(raanDot)).multiply(sc.cos()).
               add(exDot.subtract(ey.multiply(raanDot)).multiply(sc.sin()));

    }

    /** Get the first component of the circular eccentricity vector.
     * @return ex = e cos(ω), first component of the circular eccentricity vector
     */
    public T getCircularEx() {
        return ex;
    }

    /** Get the first component of the circular eccentricity vector derivative.
     * @return d(ex)/dt = d(e cos(ω))/dt, first component of the circular eccentricity vector derivative
     */
    public T getCircularExDot() {
        return exDot;
    }

    /** Get the second component of the circular eccentricity vector.
     * @return ey = e sin(ω), second component of the circular eccentricity vector
     */
    public T getCircularEy() {
        return ey;
    }

    /** Get the second component of the circular eccentricity vector derivative.
     * @return d(ey)/dt = d(e sin(ω))/dt, second component of the circular eccentricity vector derivative
     */
    public T getCircularEyDot() {
        return eyDot;
    }

    /** {@inheritDoc} */
    @Override
    public T getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }
        return raan.cos().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    @Override
    public T getHxDot() {

        if (!hasDerivatives()) {
            return null;
        }

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }

        final FieldSinCos<T> sc = FastMath.sinCos(raan);
        final T tan             = i.multiply(0.5).tan();
        return sc.cos().multiply(0.5).multiply(tan.multiply(tan).add(1)).multiply(iDot).
               subtract(sc.sin().multiply(tan).multiply(raanDot));

    }

    /** {@inheritDoc} */
    @Override
    public T getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }
        return raan.sin().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    @Override
    public T getHyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.subtract(i.getPi()).getReal()) < 1.0e-10) {
            return getZero().add(Double.NaN);
        }

        final FieldSinCos<T> sc = FastMath.sinCos(raan);
        final T tan     = i.multiply(0.5).tan();
        return sc.sin().multiply(0.5).multiply(tan.multiply(tan).add(1)).multiply(iDot).
               add(sc.cos().multiply(tan).multiply(raanDot));

    }

    /** Get the true latitude argument.
     * @return v + ω true latitude argument (rad)
     */
    public T getAlphaV() {
        switch (cachedPositionAngleType) {
            case TRUE:
                return cachedAlpha;

            case ECCENTRIC:
                return FieldCircularLatitudeArgumentUtility.eccentricToTrue(ex, ey, cachedAlpha);

            case MEAN:
                return FieldCircularLatitudeArgumentUtility.meanToTrue(ex, ey, cachedAlpha);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the true latitude argument derivative.
     * @return d(v + ω)/dt true latitude argument derivative (rad/s)
     */
    public T getAlphaVDot() {

        if (!hasDerivatives()) {
            return null;
        }
        switch (cachedPositionAngleType) {
            case ECCENTRIC:
                final FieldUnivariateDerivative1<T> alphaEUD = new FieldUnivariateDerivative1<>(cachedAlpha, cachedAlphaDot);
                final FieldUnivariateDerivative1<T> exUD     = new FieldUnivariateDerivative1<>(ex,     exDot);
                final FieldUnivariateDerivative1<T> eyUD     = new FieldUnivariateDerivative1<>(ey,     eyDot);
                final FieldUnivariateDerivative1<T> alphaVUD = FieldCircularLatitudeArgumentUtility.eccentricToTrue(exUD, eyUD,
                        alphaEUD);
                return alphaVUD.getFirstDerivative();

            case TRUE:
                return cachedAlphaDot;

            case MEAN:
                final FieldUnivariateDerivative1<T> alphaMUD = new FieldUnivariateDerivative1<>(cachedAlpha, cachedAlphaDot);
                final FieldUnivariateDerivative1<T> exUD2    = new FieldUnivariateDerivative1<>(ex,     exDot);
                final FieldUnivariateDerivative1<T> eyUD2    = new FieldUnivariateDerivative1<>(ey,     eyDot);
                final FieldUnivariateDerivative1<T> alphaVUD2 = FieldCircularLatitudeArgumentUtility.meanToTrue(exUD2,
                        eyUD2, alphaMUD);
                return alphaVUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the eccentric latitude argument.
     * @return E + ω eccentric latitude argument (rad)
     */
    public T getAlphaE() {
        switch (cachedPositionAngleType) {
            case TRUE:
                return FieldCircularLatitudeArgumentUtility.trueToEccentric(ex, ey, cachedAlpha);

            case ECCENTRIC:
                return cachedAlpha;

            case MEAN:
                return FieldCircularLatitudeArgumentUtility.meanToEccentric(ex, ey, cachedAlpha);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the eccentric latitude argument derivative.
     * @return d(E + ω)/dt eccentric latitude argument derivative (rad/s)
     */
    public T getAlphaEDot() {

        if (!hasDerivatives()) {
            return null;
        }
        switch (cachedPositionAngleType) {
            case TRUE:
                final FieldUnivariateDerivative1<T> alphaVUD = new FieldUnivariateDerivative1<>(cachedAlpha, cachedAlphaDot);
                final FieldUnivariateDerivative1<T> exUD = new FieldUnivariateDerivative1<>(ex, exDot);
                final FieldUnivariateDerivative1<T> eyUD = new FieldUnivariateDerivative1<>(ey, eyDot);
                final FieldUnivariateDerivative1<T> alphaEUD = FieldCircularLatitudeArgumentUtility.trueToEccentric(exUD, eyUD,
                        alphaVUD);
                return alphaEUD.getFirstDerivative();

            case ECCENTRIC:
                return cachedAlphaDot;

            case MEAN:
                final FieldUnivariateDerivative1<T> alphaMUD = new FieldUnivariateDerivative1<>(cachedAlpha, cachedAlphaDot);
                final FieldUnivariateDerivative1<T> exUD2 = new FieldUnivariateDerivative1<>(ex, exDot);
                final FieldUnivariateDerivative1<T> eyUD2 = new FieldUnivariateDerivative1<>(ey, eyDot);
                final FieldUnivariateDerivative1<T> alphaVUD2 = FieldCircularLatitudeArgumentUtility.meanToEccentric(exUD2,
                        eyUD2, alphaMUD);
                return alphaVUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }

    }

    /** Get the mean latitude argument.
     * @return M + ω mean latitude argument (rad)
     */
    public T getAlphaM() {
        switch (cachedPositionAngleType) {
            case TRUE:
                return FieldCircularLatitudeArgumentUtility.trueToMean(ex, ey, cachedAlpha);

            case MEAN:
                return cachedAlpha;

            case ECCENTRIC:
                return FieldCircularLatitudeArgumentUtility.eccentricToMean(ex, ey, cachedAlpha);

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the mean latitude argument derivative.
     * @return d(M + ω)/dt mean latitude argument derivative (rad/s)
     */
    public T getAlphaMDot() {

        if (!hasDerivatives()) {
            return null;
        }
        switch (cachedPositionAngleType) {
            case TRUE:
                final FieldUnivariateDerivative1<T> alphaVUD = new FieldUnivariateDerivative1<>(cachedAlpha, cachedAlphaDot);
                final FieldUnivariateDerivative1<T> exUD = new FieldUnivariateDerivative1<>(ex, exDot);
                final FieldUnivariateDerivative1<T> eyUD = new FieldUnivariateDerivative1<>(ey, eyDot);
                final FieldUnivariateDerivative1<T> alphaMUD = FieldCircularLatitudeArgumentUtility.trueToMean(exUD, eyUD,
                        alphaVUD);
                return alphaMUD.getFirstDerivative();

            case MEAN:
                return cachedAlphaDot;

            case ECCENTRIC:
                final FieldUnivariateDerivative1<T> alphaEUD = new FieldUnivariateDerivative1<>(cachedAlpha, cachedAlphaDot);
                final FieldUnivariateDerivative1<T> exUD2 = new FieldUnivariateDerivative1<>(ex, exDot);
                final FieldUnivariateDerivative1<T> eyUD2 = new FieldUnivariateDerivative1<>(ey, eyDot);
                final FieldUnivariateDerivative1<T> alphaMUD2 = FieldCircularLatitudeArgumentUtility.eccentricToMean(exUD2,
                        eyUD2, alphaEUD);
                return alphaMUD2.getFirstDerivative();

            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the latitude argument.
     * @param type type of the angle
     * @return latitude argument (rad)
     */
    public T getAlpha(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getAlphaM() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getAlphaE() :
                                                                                   getAlphaV());
    }

    /** Get the latitude argument derivative.
     * @param type type of the angle
     * @return latitude argument derivative (rad/s)
     */
    public T getAlphaDot(final PositionAngleType type) {
        return (type == PositionAngleType.MEAN) ? getAlphaMDot() :
                                              ((type == PositionAngleType.ECCENTRIC) ? getAlphaEDot() :
                                                                                   getAlphaVDot());
    }

    /** {@inheritDoc} */
    @Override
    public T getE() {
        return ex.multiply(ex).add(ey.multiply(ey)).sqrt();
    }

    /** {@inheritDoc} */
    @Override
    public T getEDot() {

        if (!hasDerivatives()) {
            return null;
        }

        return ex.multiply(exDot).add(ey.multiply(eyDot)).divide(getE());

    }

    /** {@inheritDoc} */
    @Override
    public T getI() {
        return i;
    }

    /** {@inheritDoc} */
    @Override
    public T getIDot() {
        return iDot;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public T getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the right ascension of the ascending node derivative.
     * @return right ascension of the ascending node derivative (rad/s)
     */
    public T getRightAscensionOfAscendingNodeDot() {
        return raanDot;
    }

    /** {@inheritDoc} */
    @Override
    public T getLv() {
        return getAlphaV().add(raan);
    }

    /** {@inheritDoc} */
    @Override
    public T getLvDot() {
        return hasDerivatives() ? getAlphaVDot().add(raanDot) : null;
    }

    /** {@inheritDoc} */
    @Override
    public T getLE() {
        return getAlphaE().add(raan);
    }

    /** {@inheritDoc} */
    @Override
    public T getLEDot() {
        return hasDerivatives() ? getAlphaEDot().add(raanDot) : null;
    }

    /** {@inheritDoc} */
    @Override
    public T getLM() {
        return getAlphaM().add(raan);
    }

    /** {@inheritDoc} */
    @Override
    public T getLMDot() {
        return hasDerivatives() ? getAlphaMDot().add(raanDot) : null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDerivatives() {
        return aDot != null;
    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

        // get equinoctial parameters
        final T equEx = getEquinoctialEx();
        final T equEy = getEquinoctialEy();
        final T hx = getHx();
        final T hy = getHy();
        final T lE = getLE();
        // inclination-related intermediate parameters
        final T hx2   = hx.multiply(hx);
        final T hy2   = hy.multiply(hy);
        final T factH = (hx2.add(1).add(hy2)).reciprocal();

        // reference axes defining the orbital plane
        final T ux = (hx2.add(1).subtract(hy2)).multiply(factH);
        final T uy =  hx.multiply(2).multiply(hy).multiply(factH);
        final T uz = hy.multiply(-2).multiply(factH);

        final T vx = uy;
        final T vy = (hy2.subtract(hx2).add(1)).multiply(factH);
        final T vz =  hx.multiply(factH).multiply(2);

        // eccentricity-related intermediate parameters
        final T exey = equEx.multiply(equEy);
        final T ex2  = equEx.square();
        final T ey2  = equEy.square();
        final T e2   = ex2.add(ey2);
        final T eta  = e2.negate().add(1).sqrt().add(1);
        final T beta = eta.reciprocal();

        // eccentric latitude argument
        final FieldSinCos<T> scLe = FastMath.sinCos(lE);
        final T cLe    = scLe.cos();
        final T sLe    = scLe.sin();
        final T exCeyS = equEx.multiply(cLe).add(equEy.multiply(sLe));
        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(beta.negate().multiply(ey2).add(1).multiply(cLe).add(beta.multiply(exey).multiply(sLe)).subtract(equEx));
        final T y      = a.multiply(beta.negate().multiply(ex2).add(1).multiply(sLe).add(beta.multiply(exey).multiply(cLe)).subtract(equEy));

        final T factor = getOne().add(getMu()).divide(a).sqrt().divide(exCeyS.negate().add(1));
        final T xdot   = factor.multiply( beta.multiply(equEy).multiply(exCeyS).subtract(sLe ));
        final T ydot   = factor.multiply( cLe.subtract(beta.multiply(equEx).multiply(exCeyS)));

        final FieldVector3D<T> position = new FieldVector3D<>(x.multiply(ux).add(y.multiply(vx)),
                                                              x.multiply(uy).add(y.multiply(vy)),
                                                              x.multiply(uz).add(y.multiply(vz)));
        final FieldVector3D<T> velocity = new FieldVector3D<>(xdot.multiply(ux).add(ydot.multiply(vx)),
                                                              xdot.multiply(uy).add(ydot.multiply(vy)),
                                                              xdot.multiply(uz).add(ydot.multiply(vz)));

        partialPV = new FieldPVCoordinates<>(position, velocity);

    }


    /** Initialize cached alpha with rate.
     * @param alpha input alpha
     * @param alphaDot rate of input alpha
     * @param inputType position angle type passed as input
     * @return alpha to cache with rate
     * @since 12.1
     */
    private FieldUnivariateDerivative1<T> initializeCachedAlpha(final T alpha, final T alphaDot,
                                                                final PositionAngleType inputType) {
        if (cachedPositionAngleType == inputType) {
            return new FieldUnivariateDerivative1<>(alpha, alphaDot);

        } else {
            final FieldUnivariateDerivative1<T> exUD = new FieldUnivariateDerivative1<>(ex, exDot);
            final FieldUnivariateDerivative1<T> eyUD = new FieldUnivariateDerivative1<>(ey, eyDot);
            final FieldUnivariateDerivative1<T> alphaUD = new FieldUnivariateDerivative1<>(alpha, alphaDot);

            switch (cachedPositionAngleType) {

                case ECCENTRIC:
                    if (inputType == PositionAngleType.MEAN) {
                        return FieldCircularLatitudeArgumentUtility.meanToEccentric(exUD, eyUD, alphaUD);
                    } else {
                        return FieldCircularLatitudeArgumentUtility.trueToEccentric(exUD, eyUD, alphaUD);
                    }

                case TRUE:
                    if (inputType == PositionAngleType.MEAN) {
                        return FieldCircularLatitudeArgumentUtility.meanToTrue(exUD, eyUD, alphaUD);
                    } else {
                        return FieldCircularLatitudeArgumentUtility.eccentricToTrue(exUD, eyUD, alphaUD);
                    }

                case MEAN:
                    if (inputType == PositionAngleType.TRUE) {
                        return FieldCircularLatitudeArgumentUtility.trueToMean(exUD, eyUD, alphaUD);
                    } else {
                        return FieldCircularLatitudeArgumentUtility.eccentricToMean(exUD, eyUD, alphaUD);
                    }

                default:
                    throw new OrekitInternalError(null);

            }

        }

    }

    /** Initialize cached alpha.
     * @param alpha input alpha
     * @param positionAngleType position angle type passed as input
     * @return alpha to cache
     * @since 12.1
     */
    private T initializeCachedAlpha(final T alpha, final PositionAngleType positionAngleType) {
        return FieldCircularLatitudeArgumentUtility.convertAlpha(positionAngleType, alpha, ex, ey, cachedPositionAngleType);
    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * <p>
     * This method should be called only when {@link #hasDerivatives()} returns true.
     * </p>
     * @return non-Keplerian part of the acceleration
     */
    private FieldVector3D<T> nonKeplerianAcceleration() {

        final T[][] dCdP = MathArrays.buildArray(a.getField(), 6, 6);
        getJacobianWrtParameters(PositionAngleType.MEAN, dCdP);

        final T nonKeplerianMeanMotion = getAlphaMDot().subtract(getKeplerianMeanMotion());
        final T nonKeplerianAx =     dCdP[3][0].multiply(aDot).
                                 add(dCdP[3][1].multiply(exDot)).
                                 add(dCdP[3][2].multiply(eyDot)).
                                 add(dCdP[3][3].multiply(iDot)).
                                 add(dCdP[3][4].multiply(raanDot)).
                                 add(dCdP[3][5].multiply(nonKeplerianMeanMotion));
        final T nonKeplerianAy =     dCdP[4][0].multiply(aDot).
                                 add(dCdP[4][1].multiply(exDot)).
                                 add(dCdP[4][2].multiply(eyDot)).
                                 add(dCdP[4][3].multiply(iDot)).
                                 add(dCdP[4][4].multiply(raanDot)).
                                 add(dCdP[4][5].multiply(nonKeplerianMeanMotion));
        final T nonKeplerianAz =     dCdP[5][0].multiply(aDot).
                                 add(dCdP[5][1].multiply(exDot)).
                                 add(dCdP[5][2].multiply(eyDot)).
                                 add(dCdP[5][3].multiply(iDot)).
                                 add(dCdP[5][4].multiply(raanDot)).
                                 add(dCdP[5][5].multiply(nonKeplerianMeanMotion));

        return new FieldVector3D<>(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);

    }

    /** {@inheritDoc} */
    @Override
    protected FieldVector3D<T> initPosition() {
        // get equinoctial parameters
        final T equEx = getEquinoctialEx();
        final T equEy = getEquinoctialEy();
        final T hx = getHx();
        final T hy = getHy();
        final T lE = getLE();
        // inclination-related intermediate parameters
        final T hx2   = hx.multiply(hx);
        final T hy2   = hy.multiply(hy);
        final T factH = (hx2.add(1).add(hy2)).reciprocal();

        // reference axes defining the orbital plane
        final T ux = (hx2.add(1).subtract(hy2)).multiply(factH);
        final T uy =  hx.multiply(2).multiply(hy).multiply(factH);
        final T uz = hy.multiply(-2).multiply(factH);

        final T vx = uy;
        final T vy = (hy2.subtract(hx2).add(1)).multiply(factH);
        final T vz =  hx.multiply(factH).multiply(2);

        // eccentricity-related intermediate parameters
        final T exey = equEx.multiply(equEy);
        final T ex2  = equEx.square();
        final T ey2  = equEy.square();
        final T e2   = ex2.add(ey2);
        final T eta  = e2.negate().add(1).sqrt().add(1);
        final T beta = eta.reciprocal();

        // eccentric latitude argument
        final FieldSinCos<T> scLe = FastMath.sinCos(lE);
        final T cLe    = scLe.cos();
        final T sLe    = scLe.sin();

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(beta.negate().multiply(ey2).add(1).multiply(cLe).add(beta.multiply(exey).multiply(sLe)).subtract(equEx));
        final T y      = a.multiply(beta.negate().multiply(ex2).add(1).multiply(sLe).add(beta.multiply(exey).multiply(cLe)).subtract(equEy));

        return new FieldVector3D<>(x.multiply(ux).add(y.multiply(vx)),
                                   x.multiply(uy).add(y.multiply(vy)),
                                   x.multiply(uz).add(y.multiply(vz)));

    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedFieldPVCoordinates<T> initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final T r2 = partialPV.getPosition().getNormSq();
        final FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r2.multiply(r2.sqrt()).reciprocal().multiply(getMu().negate()),
                                                                           partialPV.getPosition());
        final FieldVector3D<T> acceleration = hasDerivatives() ?
                                              keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                              keplerianAcceleration;

        return new TimeStampedFieldPVCoordinates<>(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    @Override
    public FieldCircularOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getZero().newInstance(dt));
    }

    /** {@inheritDoc} */
    @Override
    public FieldCircularOrbit<T> shiftedBy(final T dt) {

        // use Keplerian-only motion
        final FieldCircularOrbit<T> keplerianShifted = new FieldCircularOrbit<>(a, ex, ey, i, raan,
                                                                                getAlphaM().add(getKeplerianMeanMotion().multiply(dt)),
                                                                                PositionAngleType.MEAN, cachedPositionAngleType, getFrame(),
                                                                                getDate().shiftedBy(dt), getMu());

        if (hasDerivatives()) {

            // extract non-Keplerian acceleration from first time derivatives
            final FieldVector3D<T> nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(getOne(), keplerianShifted.partialPV.getPosition(),
                                                                  dt.square().multiply(0.5), nonKeplerianAcceleration);
            final T   fixedR2 = fixedP.getNormSq();
            final T   fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(getOne(), keplerianShifted.partialPV.getVelocity(),
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR2.multiply(fixedR).reciprocal().multiply(getMu().negate()),
                                                                 keplerianShifted.partialPV.getPosition(),
                                                                 getOne(), nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new FieldCircularOrbit<>(new TimeStampedFieldPVCoordinates<>(keplerianShifted.getDate(),
                                                                                fixedP, fixedV, fixedA),
                                            keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc} */
    @Override
    protected T[][] computeJacobianMeanWrtCartesian() {

        final T[][] jacobian = MathArrays.buildArray(getOne().getField(), 6, 6);

        // compute various intermediate parameters
        computePVWithoutA();
        final FieldVector3D<T> position = partialPV.getPosition();
        final FieldVector3D<T> velocity = partialPV.getVelocity();

        final T x          = position.getX();
        final T y          = position.getY();
        final T z          = position.getZ();
        final T vx         = velocity.getX();
        final T vy         = velocity.getY();
        final T vz         = velocity.getZ();
        final T pv         = FieldVector3D.dotProduct(position, velocity);
        final T r2         = position.getNormSq();
        final T r          = r2.sqrt();
        final T v2         = velocity.getNormSq();

        final T mu         = getMu();
        final T oOsqrtMuA  = getOne().divide(a.multiply(mu).sqrt());
        final T rOa        = r.divide(a);
        final T aOr        = a.divide(r);
        final T aOr2       = a.divide(r2);
        final T a2         = a.square();

        final T ex2        = ex.square();
        final T ey2        = ey.square();
        final T e2         = ex2.add(ey2);
        final T epsilon    = e2.negate().add(1.0).sqrt();
        final T beta       = epsilon.add(1).reciprocal();

        final T eCosE      = rOa.negate().add(1);
        final T eSinE      = pv.multiply(oOsqrtMuA);

        final FieldSinCos<T> scI    = FastMath.sinCos(i);
        final FieldSinCos<T> scRaan = FastMath.sinCos(raan);
        final T cosI       = scI.cos();
        final T sinI       = scI.sin();
        final T cosRaan    = scRaan.cos();
        final T sinRaan    = scRaan.sin();

        // da
        fillHalfRow(aOr.multiply(2.0).multiply(aOr2), position, jacobian[0], 0);
        fillHalfRow(a2.multiply(mu.divide(2.).reciprocal()), velocity, jacobian[0], 3);

        // differentials of the normalized momentum
        final FieldVector3D<T> danP = new FieldVector3D<>(v2, position, pv.negate(), velocity);
        final FieldVector3D<T> danV = new FieldVector3D<>(r2, velocity, pv.negate(), position);
        final T recip   = partialPV.getMomentum().getNorm().reciprocal();
        final T recip2  = recip.multiply(recip);
        final T recip2N = recip2.negate();
        final FieldVector3D<T> dwXP = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(getZero(), vz, vy.negate()),
                                                          recip2N.multiply(sinRaan).multiply(sinI),
                                                          danP);
        final FieldVector3D<T> dwYP = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(vz.negate(), getZero(), vx),
                                                          recip2.multiply(cosRaan).multiply(sinI),
                                                          danP);
        final FieldVector3D<T> dwZP = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(vy, vx.negate(), getZero()),
                                                          recip2N.multiply(cosI),
                                                          danP);
        final FieldVector3D<T> dwXV = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(getZero(), z.negate(), y),
                                                          recip2N.multiply(sinRaan).multiply(sinI),
                                                          danV);
        final FieldVector3D<T> dwYV = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(z, getZero(), x.negate()),
                                                          recip2.multiply(cosRaan).multiply(sinI),
                                                          danV);
        final FieldVector3D<T> dwZV = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(y.negate(), x, getZero()),
                                                          recip2N.multiply(cosI),
                                                          danV);

        // di
        fillHalfRow(sinRaan.multiply(cosI), dwXP, cosRaan.negate().multiply(cosI), dwYP, sinI.negate(), dwZP, jacobian[3], 0);
        fillHalfRow(sinRaan.multiply(cosI), dwXV, cosRaan.negate().multiply(cosI), dwYV, sinI.negate(), dwZV, jacobian[3], 3);

        // dRaan
        fillHalfRow(sinRaan.divide(sinI), dwYP, cosRaan.divide(sinI), dwXP, jacobian[4], 0);
        fillHalfRow(sinRaan.divide(sinI), dwYV, cosRaan.divide(sinI), dwXV, jacobian[4], 3);

        // orbital frame: (p, q, w) p along ascending node, w along momentum
        // the coordinates of the spacecraft in this frame are: (u, v, 0)
        final T u     =  x.multiply(cosRaan).add(y.multiply(sinRaan));
        final T cv    =  x.negate().multiply(sinRaan).add(y.multiply(cosRaan));
        final T v     = cv.multiply(cosI).add(z.multiply(sinI));

        // du
        final FieldVector3D<T> duP = new FieldVector3D<>(cv.multiply(cosRaan).divide(sinI), dwXP,
                                                         cv.multiply(sinRaan).divide(sinI), dwYP,
                                                         getOne(), new FieldVector3D<>(cosRaan, sinRaan, getZero()));
        final FieldVector3D<T> duV = new FieldVector3D<>(cv.multiply(cosRaan).divide(sinI), dwXV,
                                                         cv.multiply(sinRaan).divide(sinI), dwYV);

        // dv
        final FieldVector3D<T> dvP = new FieldVector3D<>(u.negate().multiply(cosRaan).multiply(cosI).divide(sinI).add(sinRaan.multiply(z)), dwXP,
                                                         u.negate().multiply(sinRaan).multiply(cosI).divide(sinI).subtract(cosRaan.multiply(z)), dwYP,
                                                         cv, dwZP,
                                                         getOne(), new FieldVector3D<>(sinRaan.negate().multiply(cosI), cosRaan.multiply(cosI), sinI));
        final FieldVector3D<T> dvV = new FieldVector3D<>(u.negate().multiply(cosRaan).multiply(cosI).divide(sinI).add(sinRaan.multiply(z)), dwXV,
                                                         u.negate().multiply(sinRaan).multiply(cosI).divide(sinI).subtract(cosRaan.multiply(z)), dwYV,
                                                         cv, dwZV);

        final FieldVector3D<T> dc1P = new FieldVector3D<>(aOr2.multiply(eSinE.multiply(eSinE).multiply(2).add(1).subtract(eCosE)).divide(r2), position,
                                                          aOr2.multiply(-2).multiply(eSinE).multiply(oOsqrtMuA), velocity);
        final FieldVector3D<T> dc1V = new FieldVector3D<>(aOr2.multiply(-2).multiply(eSinE).multiply(oOsqrtMuA), position,
                                                          getZero().newInstance(2).divide(mu), velocity);
        final FieldVector3D<T> dc2P = new FieldVector3D<>(aOr2.multiply(eSinE).multiply(eSinE.multiply(eSinE).subtract(e2.negate().add(1))).divide(r2.multiply(epsilon)), position,
                                                          aOr2.multiply(e2.negate().add(1).subtract(eSinE.multiply(eSinE))).multiply(oOsqrtMuA).divide(epsilon), velocity);
        final FieldVector3D<T> dc2V = new FieldVector3D<>(aOr2.multiply(e2.negate().add(1).subtract(eSinE.multiply(eSinE))).multiply(oOsqrtMuA).divide(epsilon), position,
                                                          eSinE.divide(epsilon.multiply(mu)), velocity);

        final T cof1   = aOr2.multiply(eCosE.subtract(e2));
        final T cof2   = aOr2.multiply(epsilon).multiply(eSinE);
        final FieldVector3D<T> dexP = new FieldVector3D<>(u, dc1P,  v, dc2P, cof1, duP,  cof2, dvP);
        final FieldVector3D<T> dexV = new FieldVector3D<>(u, dc1V,  v, dc2V, cof1, duV,  cof2, dvV);
        final FieldVector3D<T> deyP = new FieldVector3D<>(v, dc1P, u.negate(), dc2P, cof1, dvP, cof2.negate(), duP);
        final FieldVector3D<T> deyV = new FieldVector3D<>(v, dc1V, u.negate(), dc2V, cof1, dvV, cof2.negate(), duV);
        fillHalfRow(getOne(), dexP, jacobian[1], 0);
        fillHalfRow(getOne(), dexV, jacobian[1], 3);
        fillHalfRow(getOne(), deyP, jacobian[2], 0);
        fillHalfRow(getOne(), deyV, jacobian[2], 3);

        final T cle = u.divide(a).add(ex).subtract(eSinE.multiply(beta).multiply(ey));
        final T sle = v.divide(a).add(ey).add(eSinE.multiply(beta).multiply(ex));
        final T m1  = beta.multiply(eCosE);
        final T m2  = m1.multiply(eCosE).negate().add(1);
        final T m3  = (u.multiply(ey).subtract(v.multiply(ex))).add(eSinE.multiply(beta).multiply(u.multiply(ex).add(v.multiply(ey))));
        final T m4  = sle.negate().add(cle.multiply(eSinE).multiply(beta));
        final T m5  = cle.add(sle.multiply(eSinE).multiply(beta));
        final T kk = m3.multiply(2).divide(r).add(aOr.multiply(eSinE)).add(m1.multiply(eSinE).multiply(m1.add(1).subtract(aOr.add(1).multiply(m2))).divide(epsilon)).divide(r2);
        final T jj = (m1.multiply(m2).divide(epsilon).subtract(1)).multiply(oOsqrtMuA);
        fillHalfRow(kk, position,
                    jj, velocity,
                    m4, dexP,
                    m5, deyP,
                    sle.negate().divide(a), duP,
                    cle.divide(a), dvP,
                    jacobian[5], 0);
        final T ll = (m1.multiply(m2).divide(epsilon ).subtract(1)).multiply(oOsqrtMuA);
        final T mm = m3.multiply(2).add(eSinE.multiply(a)).add(m1.multiply(eSinE).multiply(r).multiply(eCosE.multiply(beta).multiply(2).subtract(aOr.multiply(m2))).divide(epsilon)).divide(mu);

        fillHalfRow(ll, position,
                    mm, velocity,
                    m4, dexV,
                    m5, deyV,
                    sle.negate().divide(a), duV,
                    cle.divide(a), dvV,
                    jacobian[5], 3);
        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected T[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation aM = aE - ex sin aE + ey cos aE leads to:
        // daM = (1 - ex cos aE - ey sin aE) dE - sin aE dex + cos aE dey
        // which is inverted and rewritten as:
        // daE = a/r daM + sin aE a/r dex - cos aE a/r dey
        final T alphaE            = getAlphaE();
        final FieldSinCos<T> scAe = FastMath.sinCos(alphaE);
        final T cosAe             = scAe.cos();
        final T sinAe             = scAe.sin();
        final T aOr               = getOne().divide(getOne().subtract(ex.multiply(cosAe)).subtract(ey.multiply(sinAe)));

        // update longitude row
        final T[] rowEx = jacobian[1];
        final T[] rowEy = jacobian[2];
        final T[] rowL  = jacobian[5];
        for (int j = 0; j < 6; ++j) {
         // rowL[j] = aOr * (      rowL[j] +   sinAe *        rowEx[j]   -         cosAe *        rowEy[j]);
            rowL[j] = aOr.multiply(rowL[j].add(sinAe.multiply(rowEx[j])).subtract( cosAe.multiply(rowEy[j])));
        }
        jacobian[5] = rowL;
        return jacobian;

    }
    /** {@inheritDoc} */
    @Override
    protected T[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final T[][] jacobian = computeJacobianEccentricWrtCartesian();
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
        final T alphaE            = getAlphaE();
        final FieldSinCos<T> scAe = FastMath.sinCos(alphaE);
        final T cosAe             = scAe.cos();
        final T sinAe             = scAe.sin();
        final T eSinE             = ex.multiply(sinAe).subtract(ey.multiply(cosAe));
        final T ecosE             = ex.multiply(cosAe).add(ey.multiply(sinAe));
        final T e2                = ex.multiply(ex).add(ey.multiply(ey));
        final T epsilon           = (getOne().subtract(e2)).sqrt();
        final T onePeps           = getOne().add(epsilon);
        final T d                 = onePeps.subtract(ecosE);
        final T cT                = (d.multiply(d).add(eSinE.multiply(eSinE))).divide(2);
        final T cE                = ecosE.multiply(onePeps).subtract(e2);
        final T cX                = ex.multiply(eSinE).divide(epsilon).subtract(ey).add(sinAe.multiply(onePeps));
        final T cY                = ey.multiply(eSinE).divide(epsilon).add(ex).subtract(cosAe.multiply(onePeps));
        final T factorLe          = (cT.add(cE)).divide(cT);
        final T factorEx          = cX.divide(cT);
        final T factorEy          = cY.divide(cT);

        // update latitude row
        final T[] rowEx = jacobian[1];
        final T[] rowEy = jacobian[2];
        final T[] rowA = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowA[j] = factorLe.multiply(rowA[j]).add(factorEx.multiply(rowEx[j])).add(factorEy.multiply(rowEy[j]));
        }
        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    public void addKeplerContribution(final PositionAngleType type, final T gm, final T[] pDot) {
        pDot[5] = pDot[5].add(computeKeplerianAlphaDot(type, a, ex, ey, gm, cachedAlpha, cachedPositionAngleType));
    }

    /**
     * Compute rate of argument of latitude.
     * @param type position angle type of rate
     * @param a semi major axis
     * @param ex ex
     * @param ey ey
     * @param mu mu
     * @param alpha argument of latitude
     * @param cachedType position angle type of passed alpha
     * @param <T> field type
     * @return first-order time derivative for alpha
     * @since 12.2
     */
    private static <T extends CalculusFieldElement<T>> T computeKeplerianAlphaDot(final PositionAngleType type, final T a,
                                                                                  final T ex, final T ey, final T mu,
                                                                                  final T alpha, final PositionAngleType cachedType) {
        final T n = a.reciprocal().multiply(mu).sqrt().divide(a);
        if (type == PositionAngleType.MEAN) {
            return n;
        }
        final FieldSinCos<T> sc;
        final T ksi;
        if (type == PositionAngleType.ECCENTRIC) {
            sc = FastMath.sinCos(FieldCircularLatitudeArgumentUtility.convertAlpha(cachedType, alpha, ex, ey, type));
            ksi  = ((ex.multiply(sc.cos())).add(ey.multiply(sc.sin()))).negate().add(1).reciprocal();
            return n.multiply(ksi);
        } else {  // TRUE
            sc = FastMath.sinCos(FieldCircularLatitudeArgumentUtility.convertAlpha(cachedType, alpha, ex, ey, type));
            final T one = n.getField().getOne();
            final T oMe2  = one.subtract(ex.square()).subtract(ey.square());
            ksi   = one.add(ex.multiply(sc.cos())).add(ey.multiply(sc.sin()));
            return n.multiply(ksi.square()).divide(oMe2.multiply(oMe2.sqrt()));
        }
    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuilder().append("circular parameters: ").append('{').
                                  append("a: ").append(a.getReal()).
                                  append(", ex: ").append(ex.getReal()).append(", ey: ").append(ey.getReal()).
                                  append(", i: ").append(FastMath.toDegrees(i.getReal())).
                                  append(", raan: ").append(FastMath.toDegrees(raan.getReal())).
                                  append(", alphaV: ").append(FastMath.toDegrees(getAlphaV().getReal())).
                                  append(";}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getCachedPositionAngleType() {
        return cachedPositionAngleType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasRates() {
        return hasDerivatives();
    }

    /** {@inheritDoc} */
    @Override
    public FieldCircularOrbit<T> removeRates() {
        return new FieldCircularOrbit<>(getA(), getCircularEx(), getCircularEy(),
                getI(), getRightAscensionOfAscendingNode(), cachedAlpha,
                cachedPositionAngleType, getFrame(), getDate(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public CircularOrbit toOrbit() {
        final double cachedPositionAngle = cachedAlpha.getReal();
        if (hasDerivatives()) {
            return new CircularOrbit(a.getReal(), ex.getReal(), ey.getReal(),
                                     i.getReal(), raan.getReal(), cachedPositionAngle,
                                     aDot.getReal(), exDot.getReal(), eyDot.getReal(),
                                     iDot.getReal(), raanDot.getReal(), cachedAlphaDot.getReal(),
                                     cachedPositionAngleType, getFrame(),
                                     getDate().toAbsoluteDate(), getMu().getReal());
        } else {
            return new CircularOrbit(a.getReal(), ex.getReal(), ey.getReal(),
                                     i.getReal(), raan.getReal(), cachedPositionAngle,
                                     cachedPositionAngleType, getFrame(),
                                     getDate().toAbsoluteDate(), getMu().getReal());
        }
    }


}
