/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.orbits;

import java.util.Collection;

import org.hipparchus.RealFieldElement;

import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
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
 * related to keplerian elements as follows:
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
 * </p>
 * <p>
 * The conversion equations from and to keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is circular (but not equatorial), the circular
 * parameters are still unambiguously defined whereas some keplerian elements
 * (more precisely ω and Ω) become ambiguous. When orbit is equatorial,
 * neither the keplerian nor the circular parameters can be defined unambiguously.
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

public  class FieldCircularOrbit<T extends RealFieldElement<T>>
    extends FieldOrbit<T> {

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
                              final Frame frame, final FieldAbsoluteDate<T> date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (ex.getReal() * ex.getReal() + ey.getReal() * ey.getReal() >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }
        this.a    =  a;
        this.ex   = ex;
        this.ey   = ey;
        this.i    = i;
        this.raan = raan;

        one = a.getField().getOne();
        zero = a.getField().getZero();

        switch (type) {
            case MEAN :
                this.alphaV = eccentricToTrue(meanToEccentric(alpha));
                break;
            case ECCENTRIC :
                this.alphaV = eccentricToTrue(alpha);
                break;
            case TRUE :
                this.alphaV = alpha;
                break;
            default :
                throw new OrekitInternalError(null);
        }


    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param PVCoordinates the {@link FieldPVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCircularOrbit(final TimeStampedFieldPVCoordinates<T> PVCoordinates,
                              final Frame frame, final double mu)
        throws IllegalArgumentException {
        super(PVCoordinates, frame, mu);

        // compute semi-major axis
        final FieldVector3D<T> pvP = PVCoordinates.getPosition();
        final FieldVector3D<T> pvV = PVCoordinates.getVelocity();
        final T r  = pvP.getNorm();
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
        final FieldVector3D<T> momentum = PVCoordinates.getMomentum();
        i = FieldVector3D.angle(momentum, new FieldVector3D<T>(zero, zero, one));

        // compute right ascension of ascending node
        final FieldVector3D<T> node  = FieldVector3D.crossProduct(new FieldVector3D<T>(zero, zero, one), momentum);
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
        final T beta = (ex.multiply(ex).negate().subtract(ey.multiply(ey)).add(1)).sqrt().add(1).reciprocal();
        alphaV = eccentricToTrue (y2.add(ey).add(eSE.multiply(beta).multiply(ex)).atan2(x2.add(ex).subtract(eSE.multiply(beta).multiply(ey))));

    }

    /** Constructor from cartesian parameters.
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
                              final FieldAbsoluteDate<T> date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedFieldPVCoordinates<T>(date, PVCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldCircularOrbit(final FieldOrbit<T> op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a    = op.getA();
        i    = op.getI();
        raan = op.getHy().atan2(op.getHx());
        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        final T equiEx = op.getEquinoctialEx();
        final T equiEy = op.getEquinoctialEy();
        ex   = equiEx.multiply(cosRaan).add(equiEy.multiply(sinRaan));
        ey   = equiEy.multiply(cosRaan).subtract(equiEx.multiply(sinRaan));
        this.alphaV = op.getLv().subtract(raan);
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
    public T getEquinoctialEx() {
        return ex.multiply(raan.cos()).subtract(ey.multiply(raan.sin()));
    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        return ey.multiply(raan.cos()).add(ex.multiply(raan.sin()));
    }

    /** Get the first component of the circular eccentricity vector.
     * @return ex = e cos(ω), first component of the circular eccentricity vector
     */
    public T getCircularEx() {
        return ex;
    }

    /** Get the second component of the circular eccentricity vector.
     * @return ey = e sin(ω), second component of the circular eccentricity vector
     */
    public T getCircularEy() {
        return ey;
    }

    /** {@inheritDoc} */
    public T getHx() {
        return  raan.cos().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getHy() {
        return raan.sin().multiply(i.divide(2).tan());
    }

    /** Get the true latitude argument.
     * @return v + ω true latitude argument (rad)
     */
    public T getAlphaV() {
        return alphaV;
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

    /** Get the eccentric latitude argument.
     * @return E + ω eccentric latitude argument (rad)
     */
    public T getAlphaE() {
        final T epsilon   = (ex.multiply(ex).negate().add(1).subtract(ey.multiply(ey))).sqrt();
        final T cosAlphaV = alphaV.cos();
        final T sinAlphaV = alphaV.sin();
        return alphaV.add(ey.multiply(cosAlphaV).subtract(ex.multiply(sinAlphaV)).divide
                                      (epsilon.add(1).add(ex.multiply(cosAlphaV).add(ey.multiply(sinAlphaV)))).atan().multiply(2));

    }

    /** Computes the true latitude argument from the eccentric latitude argument.
     * @param alphaE = E + ω eccentric latitude argument (rad)
     * @return the true latitude argument.
     */
    private T eccentricToTrue(final T alphaE) {
        final T epsilon   = (ex.multiply(ex).negate().add(1).subtract(ey.multiply(ey))).sqrt();
        final T cosAlphaE = alphaE.cos();
        final T sinAlphaE = alphaE.sin();
        return alphaE.add(ex.multiply(sinAlphaE).subtract(ey.multiply(cosAlphaE)).divide(
                                      epsilon.add(1).subtract(ex.multiply(cosAlphaE)).subtract(
                                      ey.multiply(sinAlphaE))).atan().multiply(2));
    }

    /** Get the mean latitude argument.
     * @return M + ω mean latitude argument (rad)
     */
    public T getAlphaM() {
        final T alphaE = getAlphaE();
        return alphaE.subtract(ex.multiply(alphaE.sin())).add(ey.multiply(alphaE.cos()));
    }

    /** Computes the eccentric latitude argument from the mean latitude argument.
     * @param alphaM = M + ω  mean latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    private T meanToEccentric(final T alphaM) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)

        T alphaE        = alphaM;
        T shift         = zero;
        T alphaEMalphaM = zero;
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

    /** {@inheritDoc} */
    public T getE() {
        return ex.multiply(ex).add(ey.multiply(ey)).sqrt();
    }

    /** {@inheritDoc} */
    public T getI() {
        return i;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public T getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** {@inheritDoc} */
    public T getLv() {
        return alphaV.add(raan);
    }

    /** {@inheritDoc} */
    public T getLE() {
        return getAlphaE().add(raan);
    }

    /** {@inheritDoc} */
    public T getLM() {
        return getAlphaM().add(raan);
    }

    /** {@inheritDoc} */
    protected TimeStampedFieldPVCoordinates<T> initFieldPVCoordinates() {

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

        final FieldVector3D<T> position =
            new FieldVector3D<T>(x.multiply(ux).add(y.multiply(vx)), x.multiply(uy).add(y.multiply(vy)), x.multiply(uz).add(y.multiply(vz)));
        final T r2         = position.getNormSq();
        final FieldVector3D<T> velocity =
            new FieldVector3D<T>(xdot.multiply(ux).add(ydot.multiply(vx)), xdot.multiply(uy).add(ydot.multiply(vy)), xdot.multiply(uz).add(ydot.multiply(vz)));
        final FieldVector3D<T> acceleration = new FieldVector3D<T>(zero.add(-getMu()).divide(r2.multiply(r2.sqrt())), position);
        return new TimeStampedFieldPVCoordinates<T>(getDate(), position, velocity, acceleration);

    }

    /** {@inheritDoc} */
    public FieldCircularOrbit<T> shiftedBy(final T dt) {
        return new FieldCircularOrbit<T>(a, ex, ey, i, raan,
                                 getAlphaM().add(getKeplerianMeanMotion().multiply(dt)),
                                 PositionAngle.MEAN, getFrame(),
                                 getDate().shiftedBy(dt), getMu());
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
    public FieldCircularOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Collection<FieldOrbit<T>> sample) {

        // set up an interpolator
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

        // add sample points
        FieldAbsoluteDate<T> previousDate = null;
        T previousRAAN   = zero.add(Double.NaN);
        T previousAlphaM = zero.add(Double.NaN);
        for (final FieldOrbit<T> orbit : sample) {
            final FieldCircularOrbit<T> circ = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(orbit);
            final T continuousRAAN;
            final T continuousAlphaM;
            if (previousDate == null) {
                continuousRAAN   = circ.getRightAscensionOfAscendingNode();
                continuousAlphaM = circ.getAlphaM();
            } else {
                final T dt       = circ.getDate().durationFrom(previousDate);
                final T keplerAM = previousAlphaM .add(circ.getKeplerianMeanMotion().multiply(dt));
                continuousRAAN   = normalizeAngle(circ.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousAlphaM = normalizeAngle(circ.getAlphaM(), keplerAM);
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
            interpolator.addSamplePoint(circ.getDate().durationFrom(date),
                                        toAdd);
        }

        // interpolate
        final T[] interpolated = interpolator.value(zero);

        // build a new interpolated instance
        return new FieldCircularOrbit<T>(interpolated[0], interpolated[1], interpolated[2],
                                 interpolated[3], interpolated[4], interpolated[5],
                                 PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianMeanWrtCartesian() {

        final T[][] jacobian = MathArrays.buildArray(one.getField(), 6, 6);

        // compute various intermediate parameters
        final FieldPVCoordinates<T> pvc = getPVCoordinates();
        final FieldVector3D<T> position = pvc.getPosition();
        final FieldVector3D<T> velocity = pvc.getVelocity();

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

        final double mu         = getMu();
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
        fillHalfRow(a2.multiply(2.0 / mu), velocity, jacobian[0], 3);

        // differentials of the normalized momentum
        final FieldVector3D<T> danP = new FieldVector3D<T>(v2, position, pv.negate(), velocity);
        final FieldVector3D<T> danV = new FieldVector3D<T>(r2, velocity, pv.negate(), position);
        final T recip  = pvc.getMomentum().getNorm().reciprocal();
        final T recip2 = recip.multiply(recip);
        final FieldVector3D<T> dwXP = new FieldVector3D<T>(recip, new FieldVector3D<T>( zero,               vz, vy.negate()),  recip2.negate().multiply(sinRaan).multiply(sinI), danP);
        final FieldVector3D<T> dwYP = new FieldVector3D<T>(recip, new FieldVector3D<T>( vz.negate(),  zero,              vx),  recip2.multiply(cosRaan).multiply(sinI), danP);
        final FieldVector3D<T> dwZP = new FieldVector3D<T>(recip, new FieldVector3D<T>( vy,    vx.negate(),            zero),  recip2.negate().multiply(cosI),           danP);
        final FieldVector3D<T> dwXV = new FieldVector3D<T>(recip, new FieldVector3D<T>( zero,   z.negate(),               y),  recip2.negate().multiply(sinRaan).multiply(sinI), danV);
        final FieldVector3D<T> dwYV = new FieldVector3D<T>(recip, new FieldVector3D<T>(    z,         zero,      x.negate()),  recip2.multiply(cosRaan).multiply(sinI), danV);
        final FieldVector3D<T> dwZV = new FieldVector3D<T>(recip, new FieldVector3D<T>( y.negate(),       x,            zero), recip2.negate().multiply(cosI),           danV);

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
        final FieldVector3D<T> duP = new FieldVector3D<T>(cv.multiply(cosRaan).divide(sinI), dwXP,
                                          cv.multiply(sinRaan).divide(sinI), dwYP,
                                          one, new FieldVector3D<T>(cosRaan, sinRaan, zero));
        final FieldVector3D<T> duV = new FieldVector3D<T>(cv.multiply(cosRaan).divide(sinI), dwXV,
                                          cv.multiply(sinRaan).divide(sinI), dwYV);

        // dv
        final FieldVector3D<T> dvP = new FieldVector3D<T>(u.negate().multiply(cosRaan).multiply(cosI).divide(sinI).add(sinRaan.multiply(z)), dwXP,
                                          u.negate().multiply(sinRaan).multiply(cosI).divide(sinI).subtract(cosRaan.multiply(z)), dwYP,
                                          cv, dwZP,
                                          one, new FieldVector3D<T>(sinRaan.negate().multiply(cosI), cosRaan.multiply(cosI), sinI));
        final FieldVector3D<T> dvV = new FieldVector3D<T>(u.negate().multiply(cosRaan).multiply(cosI).divide(sinI).add(sinRaan.multiply(z)), dwXV,
                                          u.negate().multiply(sinRaan).multiply(cosI).divide(sinI).subtract(cosRaan.multiply(z)), dwYV,
                                          cv, dwZV);

        final FieldVector3D<T> dc1P = new FieldVector3D<T>(aOr2.multiply(eSinE.multiply(eSinE).multiply(2).add(1).subtract(eCosE)).divide(r2), position,
                                            aOr2.multiply(-2).multiply(eSinE).multiply(oOsqrtMuA), velocity);
        final FieldVector3D<T> dc1V = new FieldVector3D<T>(aOr2.multiply(-2).multiply(eSinE).multiply(oOsqrtMuA), position,
                                            zero.add(2).divide(mu), velocity);
        final FieldVector3D<T> dc2P = new FieldVector3D<T>(aOr2.multiply(eSinE).multiply(eSinE.multiply(eSinE).subtract(e2.negate().add(1))).divide(r2.multiply(epsilon)), position,
                                            aOr2.multiply(e2.negate().add(1).subtract(eSinE.multiply(eSinE))).multiply(oOsqrtMuA).divide(epsilon), velocity);
        final FieldVector3D<T> dc2V = new FieldVector3D<T>(aOr2.multiply(e2.negate().add(1).subtract(eSinE.multiply(eSinE))).multiply(oOsqrtMuA).divide(epsilon), position,
                                            eSinE.divide(epsilon.multiply(mu)), velocity);

        final T cof1   = aOr2.multiply(eCosE.subtract(e2));
        final T cof2   = aOr2.multiply(epsilon).multiply(eSinE);
        final FieldVector3D<T> dexP = new FieldVector3D<T>(u, dc1P,  v, dc2P, cof1, duP,  cof2, dvP);
        final FieldVector3D<T> dexV = new FieldVector3D<T>(u, dc1V,  v, dc2V, cof1, duV,  cof2, dvV);
        final FieldVector3D<T> deyP = new FieldVector3D<T>(v, dc1P, u.negate(), dc2P, cof1, dvP, cof2.negate(), duP);
        final FieldVector3D<T> deyV = new FieldVector3D<T>(v, dc1V, u.negate(), dc2V, cof1, dvV, cof2.negate(), duV);
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
    public void addKeplerContribution(final PositionAngle type, final double gm,
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
                                  append("a: ").append(a).
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
     *   <li>normalize an angle between 0 and 2&pi;:<br/>
     *       {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     *   <li>normalize an angle between -&pi; and +&pi;<br/>
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
     * @since 1.2
     */
    public static <T extends RealFieldElement<T>> T normalizeAngle(final T a, final T center) {
        return a.subtract(2 * FastMath.PI * FastMath.floor((a.getReal() + FastMath.PI - center.getReal()) / (2 * FastMath.PI)));
    }

    @Override
    public Orbit toOrbit() {
        // public CircularOrbit(final double a, final double ex, final double ey,
//        public CircularOrbit(final double a, final double ex, final double ey,
//                             final double i, final double raan,
//                             final double alpha, final PositionAngle type,
//                             final Frame frame, final AbsoluteDate date, final double mu)
        return new CircularOrbit(a.getReal(), ex.getReal(), ey.getReal(), i.getReal(), raan.getReal(),
                                 alphaV.getReal(), PositionAngle.TRUE, getFrame(),
                                 getDate().toAbsoluteDate(), getMu());
    }


}
