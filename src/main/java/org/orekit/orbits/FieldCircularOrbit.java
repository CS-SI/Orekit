/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
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
 */

public  class FieldCircularOrbit<T extends RealFieldElement<T>>
    extends FieldOrbit<T> {

    /** Factory for first time derivatives. */
    private static final Map<Field<? extends RealFieldElement<?>>, FDSFactory<? extends RealFieldElement<?>>> FACTORIES =
                    new HashMap<>();

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

    /** True latitude argument (rad). */
    private final T alphaV;

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
    private final T alphaVDot;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private FieldPVCoordinates<T> partialPV;

    /** one. */
    private final T one;

    /** zero. */
    private final T zero;

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
    public FieldCircularOrbit(final T a, final T ex, final T ey,
                              final T i, final T raan,
                              final T alpha, final PositionAngle type,
                              final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(a, ex, ey, i, raan, alpha,
             null, null, null, null, null, null,
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
    public FieldCircularOrbit(final T a, final T ex, final T ey,
                              final T i, final T raan, final T alpha,
                              final T aDot, final T exDot, final T eyDot,
                              final T iDot, final T raanDot, final T alphaDot,
                              final PositionAngle type,
                              final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex.getReal() * ex.getReal() + ey.getReal() * ey.getReal() >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
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

        one = a.getField().getOne();
        zero = a.getField().getZero();

        if (hasDerivatives()) {
            @SuppressWarnings("unchecked")
            final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
            final FieldDerivativeStructure<T> exDS    = factory.build(ex,    exDot);
            final FieldDerivativeStructure<T> eyDS    = factory.build(ey,    eyDot);
            final FieldDerivativeStructure<T> alphaDS = factory.build(alpha, alphaDot);
            final FieldDerivativeStructure<T> alphavDS;
            switch (type) {
                case MEAN :
                    alphavDS = eccentricToTrue(meanToEccentric(alphaDS, exDS, eyDS), exDS, eyDS);
                    break;
                case ECCENTRIC :
                    alphavDS = eccentricToTrue(alphaDS, exDS, eyDS);
                    break;
                case TRUE :
                    alphavDS = alphaDS;
                    break;
                default :
                    throw new OrekitInternalError(null);
            }
            this.alphaV    = alphavDS.getValue();
            this.alphaVDot = alphavDS.getPartialDerivative(1);
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
            this.alphaVDot = null;
        }

        this.partialPV = null;

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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

        // compute semi-major axis
        final FieldVector3D<T> pvP = pvCoordinates.getPosition();
        final FieldVector3D<T> pvV = pvCoordinates.getVelocity();
        final FieldVector3D<T> pvA = pvCoordinates.getAcceleration();
        final T r2 = pvP.getNormSq();
        final T r  = r2.sqrt();
        final T V2 = pvV.getNormSq();
        final T rV2OnMu = r.multiply(V2).divide(mu);

        zero = r.getField().getZero();
        one = r.getField().getOne();

        if (rV2OnMu.getReal() > 2) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        a = r.divide(rV2OnMu.negate().add(2));

        // compute inclination
        final FieldVector3D<T> momentum = pvCoordinates.getMomentum();
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(r.getField());
        i = FieldVector3D.angle(momentum, plusK);

        // compute right ascension of ascending node
        final FieldVector3D<T> node  = FieldVector3D.crossProduct(plusK, momentum);
        raan = node.getY().atan2(node.getX());

        // 2D-coordinates in the canonical frame
        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        final T cosI    = i.cos();
        final T sinI    = i.sin();
        final T xP      = pvP.getX();
        final T yP      = pvP.getY();
        final T zP      = pvP.getZ();
        final T x2      = (xP.multiply(cosRaan).add(yP .multiply(sinRaan))).divide(a);
        final T y2      = (yP.multiply(cosRaan).subtract(xP.multiply(sinRaan))).multiply(cosI).add(zP.multiply(sinI)).divide(a);

        // compute eccentricity vector
        final T eSE    = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(mu).sqrt());
        final T eCE    = rV2OnMu.subtract(1);
        final T e2     = eCE.multiply(eCE).add(eSE.multiply(eSE));
        final T f      = eCE.subtract(e2);
        final T g      = eSE.multiply(e2.negate().add(1).sqrt());
        final T aOnR   = a.divide(r);
        final T a2OnR2 = aOnR.multiply(aOnR);
        ex = a2OnR2.multiply(f.multiply(x2).add(g.multiply(y2)));
        ey = a2OnR2.multiply(f.multiply(y2).subtract(g.multiply(x2)));

        // compute latitude argument
        final T beta = (ex.multiply(ex).add(ey.multiply(ey)).negate().add(1)).sqrt().add(1).reciprocal();
        alphaV = eccentricToTrue(y2.add(ey).add(eSE.multiply(beta).multiply(ex)).atan2(x2.add(ex).subtract(eSE.multiply(beta).multiply(ey))),
                                 ex, ey);

        partialPV = pvCoordinates;

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

        if (hasNonKeplerianAcceleration(pvCoordinates, mu)) {
            // we have a relevant acceleration, we can compute derivatives

            final T[][] jacobian = MathArrays.buildArray(a.getField(), 6, 6);
            getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

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
            @SuppressWarnings("unchecked")
            final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
            final FieldDerivativeStructure<T> exDS     = factory.build(ex, exDot);
            final FieldDerivativeStructure<T> eyDS     = factory.build(ey, eyDot);
            final FieldDerivativeStructure<T> alphaMDS = factory.build(getAlphaM(), alphaMDot);
            final FieldDerivativeStructure<T> alphavDS = eccentricToTrue(meanToEccentric(alphaMDS, exDS, eyDS), exDS, eyDS);
            alphaVDot = alphavDS.getPartialDerivative(1);

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot      = null;
            exDot     = null;
            eyDot     = null;
            iDot      = null;
            raanDot   = null;
            alphaVDot = null;
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
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
        final T h2 = hx.multiply(hx).add(hy.multiply(hy));
        final T h  = h2.sqrt();
        raan = hy.atan2(hx);
        final T cosRaan = h.getReal() == 0 ? raan.cos() : hx.divide(h);
        final T sinRaan = h.getReal() == 0 ? raan.sin() : hy.divide(h);
        final T equiEx = op.getEquinoctialEx();
        final T equiEy = op.getEquinoctialEy();
        ex   = equiEx.multiply(cosRaan).add(equiEy.multiply(sinRaan));
        ey   = equiEy.multiply(cosRaan).subtract(equiEx.multiply(sinRaan));
        this.alphaV = op.getLv().subtract(raan);

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

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
            alphaVDot = op.getLvDot().subtract(raanDot);
        } else {
            aDot      = null;
            exDot     = null;
            eyDot     = null;
            iDot      = null;
            raanDot   = null;
            alphaVDot = null;
        }

        partialPV = null;

        one = a.getField().getOne();
        zero = a.getField().getZero();

    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.CIRCULAR;
    }

    /** {@inheritDoc} */
    public T getA() {
        return a;
    }

    /** {@inheritDoc} */
    public T getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    public T getEquinoctialEx() {
        return ex.multiply(raan.cos()).subtract(ey.multiply(raan.sin()));
    }

    /** {@inheritDoc} */
    public T getEquinoctialExDot() {

        if (!hasDerivatives()) {
            return null;
        }

        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        return exDot.subtract(ey.multiply(raanDot)).multiply(cosRaan).
               subtract(eyDot.add(ex.multiply(raanDot)).multiply(sinRaan));

    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        return ey.multiply(raan.cos()).add(ex.multiply(raan.sin()));
    }

    /** {@inheritDoc} */
    public T getEquinoctialEyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        return eyDot.add(ex.multiply(raanDot)).multiply(cosRaan).
               add(exDot.subtract(ey.multiply(raanDot)).multiply(sinRaan));

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
    public T getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return zero.add(Double.NaN);
        }
        return  raan.cos().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getHxDot() {

        if (!hasDerivatives()) {
            return null;
        }

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return zero.add(Double.NaN);
        }

        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        final T tan     = i.multiply(0.5).tan();
        return cosRaan.multiply(0.5).multiply(tan.multiply(tan).add(1)).multiply(iDot).
               subtract(sinRaan.multiply(tan).multiply(raanDot));

    }

    /** {@inheritDoc} */
    public T getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return zero.add(Double.NaN);
        }
        return raan.sin().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getHyDot() {

        if (!hasDerivatives()) {
            return null;
        }

        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return zero.add(Double.NaN);
        }

        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        final T tan     = i.multiply(0.5).tan();
        return sinRaan.multiply(0.5).multiply(tan.multiply(tan).add(1)).multiply(iDot).
               add(cosRaan.multiply(tan).multiply(raanDot));

    }

    /** Get the true latitude argument.
     * @return v + ω true latitude argument (rad)
     */
    public T getAlphaV() {
        return alphaV;
    }

    /** Get the true latitude argument derivative.
     * @return d(v + ω)/dt true latitude argument derivative (rad/s)
     */
    public T getAlphaVDot() {
        return alphaVDot;
    }

    /** Get the eccentric latitude argument.
     * @return E + ω eccentric latitude argument (rad)
     */
    public T getAlphaE() {
        return trueToEccentric(alphaV, ex, ey);
    }

    /** Get the eccentric latitude argument derivative.
     * @return d(E + ω)/dt eccentric latitude argument derivative (rad/s)
     */
    public T getAlphaEDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> alphaVDS = factory.build(alphaV, alphaVDot);
        final FieldDerivativeStructure<T> exDS     = factory.build(ex, exDot);
        final FieldDerivativeStructure<T> eyDS     = factory.build(ey, eyDot);
        final FieldDerivativeStructure<T> alphaEDS = trueToEccentric(alphaVDS, exDS, eyDS);
        return alphaEDS.getPartialDerivative(1);

    }

    /** Get the mean latitude argument.
     * @return M + ω mean latitude argument (rad)
     */
    public T getAlphaM() {
        return eccentricToMean(trueToEccentric(alphaV, ex, ey), ex, ey);
    }

    /** Get the mean latitude argument derivative.
     * @return d(M + ω)/dt mean latitude argument derivative (rad/s)
     */
    public T getAlphaMDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> alphaVDS = factory.build(alphaV, alphaVDot);
        final FieldDerivativeStructure<T> exDS     = factory.build(ex, exDot);
        final FieldDerivativeStructure<T> eyDS     = factory.build(ey, eyDot);
        final FieldDerivativeStructure<T> alphaMDS = eccentricToMean(trueToEccentric(alphaVDS, exDS, eyDS), exDS, eyDS);
        return alphaMDS.getPartialDerivative(1);

    }

    /** Get the latitude argument.
     * @param type type of the angle
     * @return latitude argument (rad)
     */
    public T getAlpha(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getAlphaM() :
                                              ((type == PositionAngle.ECCENTRIC) ? getAlphaE() :
                                                                                   getAlphaV());
    }

    /** Get the latitude argument derivative.
     * @param type type of the angle
     * @return latitude argument derivative (rad/s)
     */
    public T getAlphaDot(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getAlphaMDot() :
                                              ((type == PositionAngle.ECCENTRIC) ? getAlphaEDot() :
                                                                                   getAlphaVDot());
    }

    /** Computes the true latitude argument from the eccentric latitude argument.
     * @param alphaE = E + ω eccentric latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param <T> Type of the field elements
     * @return the true latitude argument.
     */
    public static <T extends RealFieldElement<T>> T eccentricToTrue(final T alphaE, final T ex, final T ey) {
        final T epsilon   = ex.multiply(ex).add(ey.multiply(ey)).negate().add(1).sqrt();
        final T cosAlphaE = alphaE.cos();
        final T sinAlphaE = alphaE.sin();
        return alphaE.add(ex.multiply(sinAlphaE).subtract(ey.multiply(cosAlphaE)).divide(
                                      epsilon.add(1).subtract(ex.multiply(cosAlphaE)).subtract(
                                      ey.multiply(sinAlphaE))).atan().multiply(2));
    }

    /** Computes the eccentric latitude argument from the true latitude argument.
     * @param alphaV = v + ω true latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param <T> Type of the field elements
     * @return the eccentric latitude argument.
     */
    public static <T extends RealFieldElement<T>> T trueToEccentric(final T alphaV, final T ex, final T ey) {
        final T epsilon   = ex.multiply(ex).add(ey.multiply(ey)).negate().add(1).sqrt();
        final T cosAlphaV = alphaV.cos();
        final T sinAlphaV = alphaV.sin();
        return alphaV.add(ey.multiply(cosAlphaV).subtract(ex.multiply(sinAlphaV)).divide
                                      (epsilon.add(1).add(ex.multiply(cosAlphaV).add(ey.multiply(sinAlphaV)))).atan().multiply(2));
    }

    /** Computes the eccentric latitude argument from the mean latitude argument.
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param <T> Type of the field elements
     * @return the eccentric latitude argument.
     */
    public static <T extends RealFieldElement<T>> T meanToEccentric(final T alphaM, final T ex, final T ey) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)

        T alphaE        = alphaM;
        T shift         = alphaM.getField().getZero();
        T alphaEMalphaM = alphaM.getField().getZero();
        T cosAlphaE     = alphaE.cos();
        T sinAlphaE     = alphaE.sin();
        int    iter     = 0;
        do {
            final T f2 = ex.multiply(sinAlphaE).subtract(ey.multiply(cosAlphaE));
            final T f1 = ex.negate().multiply(cosAlphaE).subtract(ey.multiply(sinAlphaE)).add(1);
            final T f0 = alphaEMalphaM.subtract(f2);

            final T f12 = f1.multiply(2);
            shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            alphaEMalphaM  = alphaEMalphaM.subtract(shift);
            alphaE         = alphaM.add(alphaEMalphaM);
            cosAlphaE      = alphaE.cos();
            sinAlphaE      = alphaE.sin();
        } while ((++iter < 50) && (FastMath.abs(shift.getReal()) > 1.0e-12));
        return alphaE;

    }

    /** Computes the mean latitude argument from the eccentric latitude argument.
     * @param alphaE = E + ω  eccentric latitude argument (rad)
     * @param ex e cos(ω), first component of circular eccentricity vector
     * @param ey e sin(ω), second component of circular eccentricity vector
     * @param <T> Type of the field elements
     * @return the mean latitude argument.
     */
    public static <T extends RealFieldElement<T>> T eccentricToMean(final T alphaE, final T ex, final T ey) {
        return alphaE.subtract(ex.multiply(alphaE.sin()).subtract(ey.multiply(alphaE.cos())));
    }

    /** {@inheritDoc} */
    public T getE() {
        return ex.multiply(ex).add(ey.multiply(ey)).sqrt();
    }

    /** {@inheritDoc} */
    public T getEDot() {

        if (!hasDerivatives()) {
            return null;
        }

        return ex.multiply(exDot).add(ey.multiply(eyDot)).divide(getE());

    }

    /** {@inheritDoc} */
    public T getI() {
        return i;
    }

    /** {@inheritDoc} */
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
    public T getLv() {
        return alphaV.add(raan);
    }

    /** {@inheritDoc} */
    public T getLvDot() {
        return hasDerivatives() ? alphaVDot.add(raanDot) : null;
    }

    /** {@inheritDoc} */
    public T getLE() {
        return getAlphaE().add(raan);
    }

    /** {@inheritDoc} */
    public T getLEDot() {
        return hasDerivatives() ? getAlphaEDot().add(raanDot) : null;
    }

    /** {@inheritDoc} */
    public T getLM() {
        return getAlphaM().add(raan);
    }

    /** {@inheritDoc} */
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
        final T ex2  = equEx.multiply(equEx);
        final T ey2  = equEy.multiply(equEy);
        final T e2   = ex2.add(ey2);
        final T eta  = e2.negate().add(1).sqrt().add(1);
        final T beta = eta.reciprocal();

        // eccentric latitude argument
        final T cLe    = lE.cos();
        final T sLe    = lE.sin();
        final T exCeyS = equEx.multiply(cLe).add(equEy.multiply(sLe));
        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(beta.negate().multiply(ey2).add(1).multiply(cLe).add(beta.multiply(exey).multiply(sLe)).subtract(equEx));
        final T y      = a.multiply(beta.negate().multiply(ex2).add(1).multiply(sLe).add(beta.multiply(exey).multiply(cLe)).subtract(equEy));

        final T factor = one.add(getMu()).divide(a).sqrt().divide(exCeyS.negate().add(1));
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

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * <p>
     * This method should be called only when {@link #hasDerivatives()} returns true.
     * </p>
     * @return non-Keplerian part of the acceleration
     */
    private FieldVector3D<T> nonKeplerianAcceleration() {

        final T[][] dCdP = MathArrays.buildArray(a.getField(), 6, 6);
        getJacobianWrtParameters(PositionAngle.MEAN, dCdP);

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
    public FieldCircularOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getDate().getField().getZero().add(dt));
    }

    /** {@inheritDoc} */
    public FieldCircularOrbit<T> shiftedBy(final T dt) {

        // use Keplerian-only motion
        final FieldCircularOrbit<T> keplerianShifted = new FieldCircularOrbit<>(a, ex, ey, i, raan,
                                                                                getAlphaM().add(getKeplerianMeanMotion().multiply(dt)),
                                                                                PositionAngle.MEAN, getFrame(),
                                                                                getDate().shiftedBy(dt), getMu());

        if (hasDerivatives()) {

            // extract non-Keplerian acceleration from first time derivatives
            final FieldVector3D<T> nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(one, keplerianShifted.partialPV.getPosition(),
                                                                  dt.multiply(dt).multiply(0.5), nonKeplerianAcceleration);
            final T   fixedR2 = fixedP.getNormSq();
            final T   fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(one, keplerianShifted.partialPV.getVelocity(),
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR2.multiply(fixedR).reciprocal().multiply(getMu().negate()),
                                                                 keplerianShifted.partialPV.getPosition(),
                                                                 one, nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new FieldCircularOrbit<>(new TimeStampedFieldPVCoordinates<>(keplerianShifted.getDate(),
                                                                                fixedP, fixedV, fixedA),
                                            keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on circular elements, without derivatives (which means the interpolation
     * falls back to Lagrange interpolation only).
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * <p>
     * If orbit interpolation on large samples is needed, using the {@link
     * org.orekit.propagation.analytical.Ephemeris} class is a better way than using this
     * low-level interpolation. The Ephemeris class automatically handles selection of
     * a neighboring sub-sample with a predefined number of point from a large global sample
     * in a thread-safe way.
     * </p>
     */
    public FieldCircularOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Stream<FieldOrbit<T>> sample) {

        // first pass to check if derivatives are available throughout the sample
        final List<FieldOrbit<T>> list = sample.collect(Collectors.toList());
        boolean useDerivatives = true;
        for (final FieldOrbit<T> orbit : list) {
            useDerivatives = useDerivatives && orbit.hasDerivatives();
        }

        // set up an interpolator
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();

        // second pass to feed interpolator
        FieldAbsoluteDate<T> previousDate   = null;
        T                    previousRAAN   = zero.add(Double.NaN);
        T                    previousAlphaM = zero.add(Double.NaN);
        for (final FieldOrbit<T> orbit : list) {
            final FieldCircularOrbit<T> circ = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(orbit);
            final T continuousRAAN;
            final T continuousAlphaM;
            if (previousDate == null) {
                continuousRAAN   = circ.getRightAscensionOfAscendingNode();
                continuousAlphaM = circ.getAlphaM();
            } else {
                final T dt       = circ.getDate().durationFrom(previousDate);
                final T keplerAM = previousAlphaM .add(circ.getKeplerianMeanMotion().multiply(dt));
                continuousRAAN   = MathUtils.normalizeAngle(circ.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousAlphaM = MathUtils.normalizeAngle(circ.getAlphaM(), keplerAM);
            }
            previousDate   = circ.getDate();
            previousRAAN   = continuousRAAN;
            previousAlphaM = continuousAlphaM;
            final T[] toAdd = MathArrays.buildArray(one.getField(), 6);
            toAdd[0] = circ.getA();
            toAdd[1] = circ.getCircularEx();
            toAdd[2] = circ.getCircularEy();
            toAdd[3] = circ.getI();
            toAdd[4] = continuousRAAN;
            toAdd[5] = continuousAlphaM;
            if (useDerivatives) {
                final T[] toAddDot = MathArrays.buildArray(one.getField(), 6);
                toAddDot[0] = circ.getADot();
                toAddDot[1] = circ.getCircularExDot();
                toAddDot[2] = circ.getCircularEyDot();
                toAddDot[3] = circ.getIDot();
                toAddDot[4] = circ.getRightAscensionOfAscendingNodeDot();
                toAddDot[5] = circ.getAlphaMDot();
                interpolator.addSamplePoint(circ.getDate().durationFrom(date),
                                            toAdd, toAddDot);
            } else {
                interpolator.addSamplePoint(circ.getDate().durationFrom(date),
                                            toAdd);
            }
        }

        // interpolate
        final T[][] interpolated = interpolator.derivatives(zero, 1);

        // build a new interpolated instance
        return new FieldCircularOrbit<>(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                        interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                        interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                        interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                        PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianMeanWrtCartesian() {

        final T[][] jacobian = MathArrays.buildArray(one.getField(), 6, 6);

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
        final T oOsqrtMuA  = one.divide(a.multiply(mu).sqrt());
        final T rOa        = r.divide(a);
        final T aOr        = a.divide(r);
        final T aOr2       = a.divide(r2);
        final T a2         = a.multiply(a);

        final T ex2        = ex.multiply(ex);
        final T ey2        = ey.multiply(ey);
        final T e2         = ex2.add(ey2);
        final T epsilon    = e2.negate().add(1.0).sqrt();
        final T beta       = epsilon.add(1).reciprocal();

        final T eCosE      = rOa.negate().add(1);
        final T eSinE      = pv.multiply(oOsqrtMuA);

        final T cosI       = i.cos();
        final T sinI       = i.sin();
        final T cosRaan    = raan.cos();
        final T sinRaan    = raan.sin();

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
                                                          new FieldVector3D<>(zero, vz, vy.negate()),
                                                          recip2N.multiply(sinRaan).multiply(sinI),
                                                          danP);
        final FieldVector3D<T> dwYP = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(vz.negate(), zero, vx),
                                                          recip2.multiply(cosRaan).multiply(sinI),
                                                          danP);
        final FieldVector3D<T> dwZP = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(vy, vx.negate(), zero),
                                                          recip2N.multiply(cosI),
                                                          danP);
        final FieldVector3D<T> dwXV = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(zero, z.negate(), y),
                                                          recip2N.multiply(sinRaan).multiply(sinI),
                                                          danV);
        final FieldVector3D<T> dwYV = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(z, zero, x.negate()),
                                                          recip2.multiply(cosRaan).multiply(sinI),
                                                          danV);
        final FieldVector3D<T> dwZV = new FieldVector3D<>(recip,
                                                          new FieldVector3D<>(y.negate(), x, zero),
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
                                                         one, new FieldVector3D<>(cosRaan, sinRaan, zero));
        final FieldVector3D<T> duV = new FieldVector3D<>(cv.multiply(cosRaan).divide(sinI), dwXV,
                                                         cv.multiply(sinRaan).divide(sinI), dwYV);

        // dv
        final FieldVector3D<T> dvP = new FieldVector3D<>(u.negate().multiply(cosRaan).multiply(cosI).divide(sinI).add(sinRaan.multiply(z)), dwXP,
                                                         u.negate().multiply(sinRaan).multiply(cosI).divide(sinI).subtract(cosRaan.multiply(z)), dwYP,
                                                         cv, dwZP,
                                                         one, new FieldVector3D<>(sinRaan.negate().multiply(cosI), cosRaan.multiply(cosI), sinI));
        final FieldVector3D<T> dvV = new FieldVector3D<>(u.negate().multiply(cosRaan).multiply(cosI).divide(sinI).add(sinRaan.multiply(z)), dwXV,
                                                         u.negate().multiply(sinRaan).multiply(cosI).divide(sinI).subtract(cosRaan.multiply(z)), dwYV,
                                                         cv, dwZV);

        final FieldVector3D<T> dc1P = new FieldVector3D<>(aOr2.multiply(eSinE.multiply(eSinE).multiply(2).add(1).subtract(eCosE)).divide(r2), position,
                                                          aOr2.multiply(-2).multiply(eSinE).multiply(oOsqrtMuA), velocity);
        final FieldVector3D<T> dc1V = new FieldVector3D<>(aOr2.multiply(-2).multiply(eSinE).multiply(oOsqrtMuA), position,
                                                          zero.add(2).divide(mu), velocity);
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
        fillHalfRow(one, dexP, jacobian[1], 0);
        fillHalfRow(one, dexV, jacobian[1], 3);
        fillHalfRow(one, deyP, jacobian[2], 0);
        fillHalfRow(one, deyV, jacobian[2], 3);

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
    protected T[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation aM = aE - ex sin aE + ey cos aE leads to:
        // daM = (1 - ex cos aE - ey sin aE) dE - sin aE dex + cos aE dey
        // which is inverted and rewritten as:
        // daE = a/r daM + sin aE a/r dex - cos aE a/r dey
        final T alphaE = getAlphaE();
        final T cosAe  = alphaE.cos();
        final T sinAe  = alphaE.sin();
        final T aOr    = one.divide(one.subtract(ex.multiply(cosAe)).subtract(ey.multiply(sinAe)));

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
        final T alphaE    = getAlphaE();
        final T cosAe     = alphaE.cos();
        final T sinAe     = alphaE.sin();
        final T eSinE     = ex.multiply(sinAe).subtract(ey.multiply(cosAe));
        final T ecosE     = ex.multiply(cosAe).add(ey.multiply(sinAe));
        final T e2        = ex.multiply(ex).add(ey.multiply(ey));
        final T epsilon   = (one.subtract(e2)).sqrt();
        final T onePeps   = one.add(epsilon);
        final T d         = onePeps.subtract(ecosE);
        final T cT        = (d.multiply(d).add(eSinE.multiply(eSinE))).divide(2);
        final T cE        = ecosE.multiply(onePeps).subtract(e2);
        final T cX        = ex.multiply(eSinE).divide(epsilon).subtract(ey).add(sinAe.multiply(onePeps));
        final T cY        = ey.multiply(eSinE).divide(epsilon).add(ex).subtract(cosAe.multiply(onePeps));
        final T factorLe  = (cT.add(cE)).divide(cT);
        final T factorEx  = cX.divide(cT);
        final T factorEy  = cY.divide(cT);

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
    public void addKeplerContribution(final PositionAngle type, final T gm,
                                      final T[] pDot) {
        final T oMe2;
        final T ksi;
        final T n = a.reciprocal().multiply(gm).sqrt().divide(a);
        switch (type) {
            case MEAN :
                pDot[5] = pDot[5].add(n);
                break;
            case ECCENTRIC :
                oMe2  = one.subtract(ex.multiply(ex)).subtract(ey.multiply(ey));
                ksi   = one.add(ex.multiply(alphaV.cos())).add(ey.multiply(alphaV.sin()));
                pDot[5] = pDot[5].add(n.multiply(ksi).divide(oMe2));
                break;
            case TRUE :
                oMe2  = one.subtract(ex.multiply(ex)).subtract(ey.multiply(ey));
                ksi   = one.add(ex.multiply(alphaV.cos())).add(ey.multiply(alphaV.sin()));
                pDot[5] = pDot[5].add(n.multiply(ksi).multiply(ksi).divide(oMe2.multiply(oMe2.sqrt())));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("circular parameters: ").append('{').
                                  append("a: ").append(a.getReal()).
                                  append(", ex: ").append(ex.getReal()).append(", ey: ").append(ey.getReal()).
                                  append(", i: ").append(FastMath.toDegrees(i.getReal())).
                                  append(", raan: ").append(FastMath.toDegrees(raan.getReal())).
                                  append(", alphaV: ").append(FastMath.toDegrees(alphaV.getReal())).
                                  append(";}").toString();
    }



    /**
     * Normalize an angle in a 2&pi; wide interval around a center value.
     * <p>This method has three main uses:</p>
     * <ul>
     *   <li>normalize an angle between 0 and 2&pi;:<br>
     *       {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     *   <li>normalize an angle between -&pi; and +&pi;<br>
     *       {@code a = MathUtils.normalizeAngle(a, 0.0);}</li>
     *   <li>compute the angle between two defining angular positions:<br>
     *       {@code angle = MathUtils.normalizeAngle(end, start) - start;}</li>
     * </ul>
     * <p>Note that due to numerical accuracy and since &pi; cannot be represented
     * exactly, the result interval is <em>closed</em>, it cannot be half-closed
     * as would be more satisfactory in a purely mathematical view.</p>
     * @param a angle to normalize
     * @param center center of the desired 2&pi; interval for the result
     * @param <T> the type of the field elements
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     * @deprecated replaced by {@link MathUtils#normalizeAngle(RealFieldElement, RealFieldElement)}
     */
    @Deprecated
    public static <T extends RealFieldElement<T>> T normalizeAngle(final T a, final T center) {
        return a.subtract(2 * FastMath.PI * FastMath.floor((a.getReal() + FastMath.PI - center.getReal()) / (2 * FastMath.PI)));
    }

    @Override
    public CircularOrbit toOrbit() {
        if (hasDerivatives()) {
            return new CircularOrbit(a.getReal(), ex.getReal(), ey.getReal(),
                                     i.getReal(), raan.getReal(), alphaV.getReal(),
                                     aDot.getReal(), exDot.getReal(), eyDot.getReal(),
                                     iDot.getReal(), raanDot.getReal(), alphaVDot.getReal(),
                                     PositionAngle.TRUE, getFrame(),
                                     getDate().toAbsoluteDate(), getMu().getReal());
        } else {
            return new CircularOrbit(a.getReal(), ex.getReal(), ey.getReal(),
                                     i.getReal(), raan.getReal(), alphaV.getReal(),
                                     PositionAngle.TRUE, getFrame(),
                                     getDate().toAbsoluteDate(), getMu().getReal());
        }
    }


}
