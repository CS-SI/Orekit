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
import org.hipparchus.exception.MathIllegalArgumentException;
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
 * This class handles traditional keplerian orbital parameters.

 * <p>
 * The parameters used internally are the classical keplerian elements:
 *   <pre>
 *     a
 *     e
 *     i
 *     ω
 *     Ω
 *     v
 *   </pre>
 * where ω stands for the Perigee Argument, Ω stands for the
 * Right Ascension of the Ascending Node and v stands for the true anomaly.
 * </p>
 * <p>
 * This class supports hyperbolic orbits, using the convention that semi major
 * axis is negative for such orbits (and of course eccentricity is greater than 1).
 * </p>
 * <p>
 * When orbit is either equatorial or circular, some keplerian elements
 * (more precisely ω and Ω) become ambiguous so this class should not
 * be used for such orbits. For this reason, {@link EquinoctialOrbit equinoctial
 * orbits} is the recommended way to represent orbits.
 * </p>

 * <p>
 * The instance <code>KeplerianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see     Orbit
 * @see    CircularOrbit
 * @see    CartesianOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Andrea Antolino
 */


public class FieldKeplerianOrbit<T extends RealFieldElement<T>> extends FieldOrbit<T> {

    /** First coefficient to compute Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A  = 3 * k2 * k2 / k1;
        B  = k3 * k3 / (6 * k1);
    }
    /** Semi-major axis (m). */
    private final T a;

    /** Eccentricity. */
    private final T e;

    /** Inclination (rad). */
    private final T i;

    /** Perigee Argument (rad). */
    private final T pa;

    /** Right Ascension of Ascending Node (rad). */
    private final T raan;

    /** True anomaly (rad). */
    private final T v;

    /** Element identité. */

    private final T one;

    /**Element zero. */

    private final T zero;

    /** Third Canonical Vector. */

    private final FieldVector3D<T> PLUS_K;

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity
     * @param i inclination (rad)
     * @param pa perigee argument (ω, rad)
     * @param raan right ascension of ascending node (Ω, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param type type of anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame} or a and e don't match for hyperbolic orbits,
     * or v is out of range for hyperbolic orbits
     */
    public FieldKeplerianOrbit(final T a, final T e, final T i,
                          final T pa, final T raan,
                          final T anomaly, final PositionAngle type,
                          final Frame frame, final FieldAbsoluteDate<T> date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        if (a.multiply(e.negate().add(1)).getReal() < 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a, e);
        }

        this.a    =    a;
        this.e    =    e;
        this.i    =    i;
        this.pa   =   pa;
        this.raan = raan;

        /**Element identité.*/

        this.one = a.getField().getOne();

        /**Element zero.*/
        this.zero = a.getField().getZero();

        /**Third canonical vector. */
        this.PLUS_K = new FieldVector3D<T>(zero, zero, one);
        final T tmpV;

        switch (type) {
            case MEAN :

                tmpV = (a.getReal() < 0) ? hyperbolicEccentricToTrue(meanToHyperbolicEccentric(anomaly, e)) :
                    ellipticEccentricToTrue(meanToEllipticEccentric(anomaly));

                break;
            case ECCENTRIC :
                tmpV = (a.getReal() < 0) ? hyperbolicEccentricToTrue(anomaly) :
                    ellipticEccentricToTrue(anomaly);

                break;
            case TRUE :
                tmpV = anomaly;
                break;
            default : // this should never happen
                throw new OrekitInternalError(null);
        }

        // check true anomaly range
        if (e.multiply(tmpV.cos()).add(1).getReal() <= 0) {
            final T vMax = e.reciprocal().negate().acos();
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE,
                                                                 tmpV, e, vMax.negate(), vMax);
        }
        this.v = tmpV;

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param FieldPVCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldKeplerianOrbit (final TimeStampedFieldPVCoordinates<T> FieldPVCoordinates,
                          final Frame frame, final double mu)
        throws IllegalArgumentException {

        super(FieldPVCoordinates, frame, mu);
        /**Element identité*/

        this.one = FieldPVCoordinates.getPosition().getX().getField().getOne();

        /**Element zero*/
        this.zero = this.one.getField().getZero();

        /**Third canonical vector */
        this.PLUS_K = new FieldVector3D<T>(zero, zero, one);

        // compute inclination
        final FieldVector3D<T> momentum = FieldPVCoordinates.getMomentum();
        final T m2 = momentum.getNormSq();

        i = FieldVector3D.angle(momentum, PLUS_K);
        // compute right ascension of ascending node
        raan = FieldVector3D.crossProduct(PLUS_K, momentum).getAlpha();
        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final FieldVector3D<T> pvP     = FieldPVCoordinates.getPosition();
        final FieldVector3D<T> pvV     = FieldPVCoordinates.getVelocity();

        final T   r       = pvP.getNorm();
        final T   V2      = pvV.getNormSq();
        final T   rV2OnMu = r.multiply(V2).divide(mu);

        // compute semi-major axis (will be negative for hyperbolic orbits)
        a = r.divide(rV2OnMu.negate().add(2.0));
        final T muA = a.multiply(mu);

        // compute true anomaly
        if (a.getReal() > 0) {
            // elliptic or circular orbit
            final T eSE = FieldVector3D.dotProduct(pvP, pvV).divide(muA.sqrt());
            final T eCE = rV2OnMu.subtract(1);
            e = (eSE.multiply(eSE).add(eCE.multiply(eCE))).sqrt();
            v = ellipticEccentricToTrue(eSE.atan2(eCE)); //(atan2(eSE, eCE));
        } else {
            // hyperbolic orbit
            final T eSH = FieldVector3D.dotProduct(pvP, pvV).divide(muA.negate().sqrt());
            final T eCH = rV2OnMu.subtract(1);
            e = (m2.negate().divide(muA).add(1)).sqrt();
            v = hyperbolicEccentricToTrue((eCH.add(eSH)).divide(eCH.subtract(eSH)).log().divide(2));
        }

        // compute perigee argument
        final FieldVector3D<T> node = new FieldVector3D<T>(raan, zero);
        final T px = FieldVector3D.dotProduct(pvP, node);
        final T py = FieldVector3D.dotProduct(pvP, FieldVector3D.crossProduct(momentum, node)).divide(m2.sqrt());
        pa = py.atan2(px).subtract(v);
    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code FieldPVCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param FieldPVCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldKeplerianOrbit(final FieldPVCoordinates<T> FieldPVCoordinates,
                          final Frame frame, final FieldAbsoluteDate<T> date, final double mu)
        throws IllegalArgumentException {

        this(new TimeStampedFieldPVCoordinates<T>(date, FieldPVCoordinates), frame, mu);

    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldKeplerianOrbit(final FieldOrbit<T> op) {
        this(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    public T getA() {
        return a;
    }

    /** {@inheritDoc} */
    public T getE() {

        return e;
    }

    /** {@inheritDoc} */
    public T getI() {
        return i;
    }

    /** Get the perigee argument.
     * @return perigee argument (rad)
     */
    public T getPerigeeArgument() {
        return pa;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public T getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the anomaly.
     * @param type type of the angle
     * @return anomaly (rad)
     */
    public T getAnomaly(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getMeanAnomaly() :
                                              ((type == PositionAngle.ECCENTRIC) ? getEccentricAnomaly() :
                                                                                   getTrueAnomaly());
    }

    /** Get the true anomaly.
     * @return true anomaly (rad)
     */
    public T getTrueAnomaly() {
        return v;
    }

    /** Get the eccentric anomaly.
     * @return eccentric anomaly (rad)
     */
    public T getEccentricAnomaly() {
        if (a.getReal() < 0) {
            // hyperbolic case
            final T sinhH = (e.multiply(e).subtract(1)).sqrt().multiply(v.sin().divide(e.multiply(v.cos()).add(1)));
            return sinhH.asinh();
        }
        // elliptic case
        final T beta = e.divide((e.subtract(1).negate().multiply(e.add(1))).sqrt().add(1));
        return v.subtract((beta.multiply(v.sin()).divide(beta.multiply(v.cos()).add(1))).atan().multiply(2));

    }

    /** Computes the true anomaly from the elliptic eccentric anomaly.
     * @param E eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private T ellipticEccentricToTrue(final T E) {

        final T beta = e.divide(e.negate().add(1).multiply(e.add(1)).sqrt().add(1));
        return E.add(beta.multiply(E.sin()).divide(beta.multiply(E.cos()).negate().add(1)).atan().multiply(2));
    }

    /** Computes the true anomaly from the hyperbolic eccentric anomaly.
     * @param H hyperbolic eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private T hyperbolicEccentricToTrue(final T H) {
//      return 2 * FastMath.atan(FastMath.sqrt((e + 1) / (e - 1)) * FastMath.tanh(H / 2));
        return ((e.add(1).divide(e.subtract(1))).sqrt().multiply(H.divide(2.0).tanh())).atan().multiply(2);
    }

    /** Get the mean anomaly.
     * @return mean anomaly (rad)
     */
    public T getMeanAnomaly() {

        if (a.getReal() < 0) {
            // hyperbolic case
            final T H = getEccentricAnomaly();
            return e.multiply(H.sinh()).subtract(H);
        }

        // elliptic case
        final T E = getEccentricAnomaly();
        return E.subtract(e.multiply(E.sin()));

    }

    /** Computes the elliptic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving Kepler equation has been published
     * in: "Procedures for  solving Kepler's Equation", A. W. Odell and
     * R. H. Gooding, Celestial Mechanics 38 (1986) 307-334
     * </p>
     * @param M mean anomaly (rad)
     * @return v the true anomaly
     */
    private T meanToEllipticEccentric(final T M) {
//SUBSTITUTED THE NORMALIZE ANGLE FUNCTION
        // reduce M to [-PI PI) interval
        final T reducedM = normalizeAngle(M, this.zero);

//
        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        T E;
        if (reducedM.abs().getReal() < 1.0 / 6.0) {
            E = reducedM.add(e.multiply( (reducedM.multiply(6).cbrt()).subtract(reducedM)));
        } else {
            if (reducedM.getReal() < 0) {
                final T w = reducedM.add(FastMath.PI);
                E = reducedM.add(e.multiply(w.multiply(A).divide(w.negate().add(B)).subtract(FastMath.PI).subtract(reducedM)));
            } else {
                final T w = reducedM.negate().add(FastMath.PI);
                E = reducedM.add(e.multiply(w.multiply(A).divide(w.negate().add(B)).negate().subtract(reducedM).add(FastMath.PI)));
            }
        }

        final T e1 = e.negate().add(1);
        final boolean noCancellationRisk = (e1.getReal() + E.getReal() * E.getReal() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {

            final T f;
            T fd;
            final T fdd  = e.multiply(E.sin());
            final T fddd = e.multiply(E.cos());

            if (noCancellationRisk) {

                f  = (E.subtract(fdd)).subtract(reducedM);
                fd = fddd.negate().add(1);
            } else {


                f  = eMeSinE(E).subtract(reducedM);
                final T s = E.multiply(0.5).sin();
                fd = e1.add(e.multiply(s).multiply(s).multiply(2));
            }
            final T dee = f.multiply(fd).divide(f.multiply(fdd).multiply(0.5).subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow problems
            final T w = fd.add(dee.multiply(fdd.add(dee.multiply(fddd.divide(3)))).multiply(0.5));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(fddd).multiply(0.5))));
            E = E.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));

        }

        // expand the result back to original range
        E = E.add(M).subtract(reducedM);
        return E;
    }

    /** Accurate computation of E - e sin(E).
     * <p>
     * This method is used when E is close to 0 and e close to 1,
     * i.e. near the perigee of almost parabolic orbits
     * </p>
     * @param E eccentric anomaly
     * @return E - e sin(E)
     */
    private T eMeSinE(final T E) {

        T x = (e.negate().add(1)).multiply(E.sin());
        final T mE2 = E.negate().multiply(E);
        T term = E;
        double d    = 0;
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (T x0 = zero.add(Double.NaN); x.getReal() != x0.getReal();) {
            d += 2;
            term = term.multiply(mE2.divide(d * (d + 1)));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /** Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is
     * Danby's iterative method (3rd order) with Vallado's initial guess.
     * </p>
     * @param M mean anomaly (rad)
     * @param ecc eccentricity
     * @return H the hyperbolic eccentric anomaly
     */
    private T meanToHyperbolicEccentric(final T M, final T ecc) {

        // Resolution of hyperbolic Kepler equation for keplerian parameters

        // Initial guess
        T H;
        if (ecc.getReal() < 1.6) {
            if ((-FastMath.PI < M.getReal() && M.getReal() < 0.) || M.getReal() > FastMath.PI) {
                H = M.subtract(ecc);
            } else {
                H = M.add(ecc);
            }
        } else {
            if (ecc.getReal() < 3.6 && M.abs().getReal() > FastMath.PI) {
                H = M.subtract(ecc.copySign(M));
            } else {
                H = M.divide(ecc.subtract(1));
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final T f3  = ecc.multiply(H.cosh());
            final T f2  = ecc.multiply(H.sinh());
            final T f1  = f3.subtract(1);
            final T f0  = f2.subtract(H).subtract(M);
            final T f12 = f1.multiply(2);
            final T d   = f0.divide(f12);
            final T fdf = f1.subtract(d.multiply(f2));
            final T ds  = f0.divide(fdf);

            final T shift = f0.divide(fdf.add(ds.multiply(ds.multiply(f3.divide(6)))));

            H = H.subtract(shift);

            if ((shift.abs().getReal()) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalArgumentException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY,
                                       iter);
    }

    /** {@inheritDoc} */
    public T getEquinoctialEx() {
        return  e.multiply(pa.add(raan).cos());
    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        return  e.multiply((pa.add(raan)).sin());
    }

    /** {@inheritDoc} */
    public T getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return this.zero.add(Double.NaN);
        }
        return  raan.cos().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i.getReal() - FastMath.PI) < 1.0e-10) {
            return this.zero.add(Double.NaN);
        }
        return  raan.sin().multiply(i.divide(2).tan());
    }

    /** {@inheritDoc} */
    public T getLv() {
        return pa.add(raan).add(v);
    }

    /** {@inheritDoc} */
    public T getLE() {
        return pa.add(raan).add(getEccentricAnomaly());
    }

    /** {@inheritDoc} */
    public T getLM() {
        return pa.add(raan).add(getMeanAnomaly());
    }

    /** {@inheritDoc} */
    protected TimeStampedFieldPVCoordinates<T> initFieldPVCoordinates() {

        // preliminary variables
        final T cosRaan = raan.cos();
        final T sinRaan = raan.sin();
        final T cosPa   = pa.cos();
        final T sinPa   = pa.sin();
        final T cosI    = i.cos();
        final T sinI    = i.sin();
        final T crcp    = cosRaan.multiply(cosPa);
        final T crsp    = cosRaan.multiply(sinPa);
        final T srcp    = sinRaan.multiply(cosPa);
        final T srsp    = sinRaan.multiply(sinPa);

        // reference axes defining the orbital plane
        final FieldVector3D<T> p = new FieldVector3D<T>( crcp.subtract(cosI.multiply(srsp)),  srcp.add(cosI.multiply(crsp)), sinI.multiply(sinPa));
        final FieldVector3D<T> q = new FieldVector3D<T>( crsp.add(cosI.multiply(srcp)).negate(), cosI.multiply(crcp).subtract(srsp), sinI.multiply(cosPa));
        return (a.getReal() > 0) ? initFieldPVCoordinatesElliptical(p, q) : initFieldPVCoordinatesHyperbolic(p, q);

    }

    /** Initialize the position/velocity coordinates, elliptic case.
     * @param p unit vector in the orbital plane pointing towards perigee
     * @param q unit vector in the orbital plane in quadrature with p
     * @return computed position/velocity coordinates
     */
    private TimeStampedFieldPVCoordinates<T> initFieldPVCoordinatesElliptical(final FieldVector3D<T> p, final FieldVector3D<T> q) {

        // elliptic eccentric anomaly
        final T uME2   = e.negate().add(1).multiply(e.add(1));
        final T s1Me2  = uME2.sqrt();
        final T E      = getEccentricAnomaly();
        final T cosE   = E.cos();
        final T sinE   = E.sin();

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(cosE.subtract(e));
        final T y      = a.multiply(sinE).multiply(s1Me2);
        final T factor = (a.reciprocal().multiply(getMu())).sqrt().divide(e.negate().multiply(cosE).add(1));
        final T xDot   = sinE.negate().multiply(factor);
        final T yDot   = cosE.multiply(s1Me2).multiply(factor);

        final FieldVector3D<T> position = new FieldVector3D<T>(x, p, y, q);
        final T r2 = x.multiply(x).add(y.multiply(y));

        final FieldVector3D<T> velocity = new FieldVector3D<T>(xDot, p, yDot, q);

        final FieldVector3D<T> acceleration = new FieldVector3D<T>(r2.multiply(r2.sqrt()).reciprocal().multiply(-getMu()), position);

        return new TimeStampedFieldPVCoordinates<T>(getDate(), position, velocity, acceleration);

    }

    /** Initialize the position/velocity coordinates, hyperbolic case.
     * @param p unit vector in the orbital plane pointing towards perigee
     * @param q unit vector in the orbital plane in quadrature with p
     * @return computed position/velocity coordinates
     */
    private TimeStampedFieldPVCoordinates<T> initFieldPVCoordinatesHyperbolic(final FieldVector3D<T> p, final FieldVector3D<T> q) {

        // compute position and velocity factors
        final T sinV      = v.sin();
        final T cosV      = v.cos();
        final T f         = a.multiply(e.multiply(e).negate().add(1));
        final T posFactor = f.divide(e.multiply(cosV).add(1));
        final T velFactor = f.reciprocal().multiply(getMu()).sqrt();

        final FieldVector3D<T> position     = new FieldVector3D<T>( posFactor.multiply(cosV), p, posFactor.multiply(sinV), q);
        final FieldVector3D<T> velocity     = new FieldVector3D<T>( velFactor.multiply(sinV).negate(), p, velFactor.multiply(e.add(cosV)), q);
        final FieldVector3D<T> acceleration = new FieldVector3D<T>(posFactor.multiply(posFactor).multiply(posFactor).reciprocal().multiply(-getMu()), position);
        return new TimeStampedFieldPVCoordinates<T>(getDate(), position, velocity, acceleration);

    }

    /** {@inheritDoc} */
    public FieldKeplerianOrbit<T> shiftedBy(final T dt) {
        return new FieldKeplerianOrbit<T>(a, e, i, pa, raan,
                                  getKeplerianMeanMotion().multiply(dt).add(getMeanAnomaly()),
                                  PositionAngle.MEAN, getFrame(), getDate().shiftedBy(dt), getMu());
    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Keplerian elements, without derivatives (which means the interpolation
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
    public FieldKeplerianOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Collection<FieldOrbit<T>> sample) {

        // set up an interpolator
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

        // add sample points
        FieldAbsoluteDate<T> previousDate = null;
        T previousPA   = this.zero.add(Double.NaN);
        T previousRAAN = this.zero.add(Double.NaN);
        T previousM    = this.zero.add(Double.NaN);
        for (final FieldOrbit<T> orbit : sample) {
            final FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit <T>(orbit);
       //   final FieldKeplerianOrbit<T> kep = (FieldKeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
            final T continuousPA;
            final T continuousRAAN;
            final T continuousM;
            if (previousDate == null) {
                continuousPA   = kep.getPerigeeArgument();
                continuousRAAN = kep.getRightAscensionOfAscendingNode();
                continuousM    = kep.getMeanAnomaly();
            } else {
                final T dt      = kep.getDate().durationFrom(previousDate);
                final T keplerM = previousM.add(kep.getKeplerianMeanMotion().multiply(dt));
                continuousPA   = normalizeAngle(kep.getPerigeeArgument(), previousPA);
                continuousRAAN = normalizeAngle(kep.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousM    = normalizeAngle(kep.getMeanAnomaly(), keplerM);
            }
            previousDate = kep.getDate();
            previousPA   = continuousPA;
            previousRAAN = continuousRAAN;
            previousM    = continuousM;
            final T[] useless = MathArrays.buildArray(getA().getField(), 6);
            useless[0] = kep.getA();
            useless[1] = kep.getE();
            useless[2] = kep.getI();
            useless[3] = continuousPA;
            useless[4] = continuousRAAN;
            useless[5] = continuousM;
            //@SuppressWarnings("unchecked")
            interpolator.addSamplePoint(this.zero.add(kep.getDate().durationFrom(date)),
                                        useless);
        }

        // interpolate
        //ADDED RISK CASTING TO T() (it should work i think)
        final T[] interpolated = (T[]) interpolator.value(this.zero);

        // build a new interpolated instance
        return new FieldKeplerianOrbit<T>(interpolated[0], interpolated[1], interpolated[2],
                                  interpolated[3], interpolated[4], interpolated[5],
                                  PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianMeanWrtCartesian() {
        if (a.getReal() > 0) {
            return computeJacobianMeanWrtCartesianElliptical();
        } else {
            return computeJacobianMeanWrtCartesianHyperbolic();
        }
    }

    /** Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private T[][] computeJacobianMeanWrtCartesianElliptical() {

        final T[][] jacobian = MathArrays.buildArray(getA().getField(), 6, 6);

        // compute various intermediate parameters
        final FieldPVCoordinates<T> pvc = getPVCoordinates();
        final FieldVector3D<T> position = pvc.getPosition();
        final FieldVector3D<T> velocity = pvc.getVelocity();
        final FieldVector3D<T> momentum = pvc.getMomentum();
        final T v2         = velocity.getNormSq();
        final T r2         = position.getNormSq();
        final T r          = r2.sqrt();
        final T r3         = r.multiply(r2);

        final T px         = position.getX();
        final T py         = position.getY();
        final T pz         = position.getZ();
        final T vx         = velocity.getX();
        final T vy         = velocity.getY();
        final T vz         = velocity.getZ();
        final T mx         = momentum.getX();
        final T my         = momentum.getY();
        final T mz         = momentum.getZ();

        final double mu         = getMu();
        final T sqrtMuA    = a.multiply(mu).sqrt();
        final T sqrtAoMu   = a.divide(mu).sqrt();
        final T a2         = a.multiply(a);
        final T twoA       = a.multiply(2);
        final T rOnA       = r.divide(a);

        final T oMe2       = e.multiply(e).negate().add(1);
        final T epsilon    = oMe2.sqrt();
        final T sqrtRec    = epsilon.reciprocal();

        final T cosI       = i.cos();
        final T sinI       = i.sin();
        final T cosPA      = pa.cos();
        final T sinPA      = pa.sin();

        final T pv         = FieldVector3D.dotProduct(position, velocity);
        final T cosE       = a.subtract(r).divide(a.multiply(e));
        final T sinE       = pv.divide(e.multiply(sqrtMuA));

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<T>(a2.multiply(2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = velocity.scalarMultiply(a2.multiply(2 / mu));
        fillHalfRow(this.one, vectorAR,    jacobian[0], 0);
        fillHalfRow(this.one, vectorARDot, jacobian[0], 3);

        // de
        final T factorER3 = pv.divide(twoA);
        final FieldVector3D<T> vectorER   = new FieldVector3D<T>(cosE.multiply(v2).divide(r.multiply(mu)), position,
                                                 sinE.divide(sqrtMuA), velocity,
                                                 factorER3.negate().multiply(sinE).divide(sqrtMuA), vectorAR);
        final FieldVector3D<T> vectorERDot = new FieldVector3D<T>(sinE.divide(sqrtMuA), position,
                                                  cosE.multiply(2 / mu).multiply(r), velocity,
                                                  factorER3.negate().multiply(sinE).divide(sqrtMuA), vectorARDot);
        fillHalfRow(this.one, vectorER,    jacobian[1], 0);
        fillHalfRow(this.one, vectorERDot, jacobian[1], 3);

        // dE / dr (Eccentric anomaly)
        final T coefE = cosE.divide(e.multiply(sqrtMuA));
        final FieldVector3D<T>  vectorEAnR =
            new FieldVector3D<T>(sinE.negate().multiply(v2).divide(e.multiply(r).multiply(mu)), position, coefE, velocity,
                         factorER3.negate().multiply(coefE), vectorAR);

        // dE / drDot
        final FieldVector3D<T>  vectorEAnRDot =
            new FieldVector3D<T>(sinE.multiply(-2).multiply(r).divide(e.multiply(mu)), velocity, coefE, position,
                         factorER3.negate().multiply(coefE), vectorARDot);

        // precomputing some more factors
        final T s1 = sinE.negate().multiply(pz).divide(r).subtract(cosE.multiply(vz).multiply(sqrtAoMu));
        final T s2 = cosE.negate().multiply(pz).divide(r3);
        final T s3 = sinE.multiply(vz).divide(sqrtMuA.multiply(-2));
        final T t1 = sqrtRec.multiply(cosE.multiply(pz).divide(r).subtract(sinE.multiply(vz).multiply(sqrtAoMu)));
        final T t2 = sqrtRec.multiply(sinE.negate().multiply(pz).divide(r3));
        final T t3 = sqrtRec.multiply(cosE.subtract(e)).multiply(vz).divide(sqrtMuA.multiply(2));
        final T t4 = sqrtRec.multiply(e.multiply(sinI).multiply(cosPA).multiply(sqrtRec).subtract(vz.multiply(sqrtAoMu)));
        final FieldVector3D<T> s = new FieldVector3D<T>(cosE.divide(r), this.PLUS_K,
                                        s1,       vectorEAnR,
                                        s2,       position,
                                        s3,       vectorAR);
        final FieldVector3D<T> sDot = new FieldVector3D<T>( sinE.negate().multiply(sqrtAoMu), this.PLUS_K,
                                           s1,               vectorEAnRDot,
                                           s3,               vectorARDot);
        final FieldVector3D<T> t =
            new FieldVector3D<T>(sqrtRec.multiply(sinE).divide(r), this.PLUS_K).add(new FieldVector3D<T>(t1, vectorEAnR,
                                                                               t2, position,
                                                                               t3, vectorAR,
                                                                               t4, vectorER));
        final FieldVector3D<T> tDot = new FieldVector3D<T>(sqrtRec.multiply(cosE.subtract(e)).multiply(sqrtAoMu), this.PLUS_K,
                                           t1,                              vectorEAnRDot,
                                           t3,                              vectorARDot,
                                           t4,                              vectorERDot);

        // di
        final T factorI1 = sinI.negate().multiply(sqrtRec).divide(sqrtMuA);
        final T i1 =  factorI1;
        final T i2 =  factorI1.negate().multiply(mz).divide(twoA);
        final T i3 =  factorI1.multiply(mz).multiply(e).divide(oMe2);
        final T i4 = cosI.multiply(sinPA);
        final T i5 = cosI.multiply(cosPA);
        fillHalfRow(i1, new FieldVector3D<T>(vy, vx.negate(), this.zero), i2, vectorAR, i3, vectorER, i4, s, i5, t,
                    jacobian[2], 0);
        fillHalfRow(i1, new FieldVector3D<T>(py.negate(), px, this.zero), i2, vectorARDot, i3, vectorERDot, i4, sDot, i5, tDot,
                    jacobian[2], 3);

        // dpa
        fillHalfRow(cosPA.divide(sinI), s,    sinPA.negate().divide(sinI), t,    jacobian[3], 0);
        fillHalfRow(cosPA.divide(sinI), sDot, sinPA.negate().divide(sinI), tDot, jacobian[3], 3);

        // dRaan
        final T factorRaanR = (a.multiply(mu).multiply(oMe2).multiply(sinI).multiply(sinI)).reciprocal();
        fillHalfRow( factorRaanR.negate().multiply(my), new FieldVector3D<T>(zero, vz, vy.negate()),
                     factorRaanR.multiply(mx), new FieldVector3D<T>(vz.negate(), zero,  vx),
                     jacobian[4], 0);
        fillHalfRow(factorRaanR.negate().multiply(my), new FieldVector3D<T>(zero, pz.negate(),  py),
                     factorRaanR.multiply(mx), new FieldVector3D<T>(pz, zero, px.negate()),
                     jacobian[4], 3);

        // dM
        fillHalfRow(rOnA, vectorEAnR,    sinE.negate(), vectorER,    jacobian[5], 0);
        fillHalfRow(rOnA, vectorEAnRDot, sinE.negate(), vectorERDot, jacobian[5], 3);

        return jacobian;

    }

    /** Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private T[][] computeJacobianMeanWrtCartesianHyperbolic() {

        final T[][] jacobian = MathArrays.buildArray(getA().getField(), 6, 6);

        // compute various intermediate parameters
        final FieldPVCoordinates<T> pvc = getPVCoordinates();
        final FieldVector3D<T> position = pvc.getPosition();
        final FieldVector3D<T> velocity = pvc.getVelocity();
        final FieldVector3D<T> momentum = pvc.getMomentum();
        final T r2         = position.getNormSq();
        final T r          = r2.sqrt();
        final T r3         = r.multiply(r2);

        final T x          = position.getX();
        final T y          = position.getY();
        final T z          = position.getZ();
        final T vx         = velocity.getX();
        final T vy         = velocity.getY();
        final T vz         = velocity.getZ();
        final T mx         = momentum.getX();
        final T my         = momentum.getY();
        final T mz         = momentum.getZ();

        final double mu         = getMu();
        final T absA       = a.negate();
        final T sqrtMuA    = absA.multiply(mu).sqrt();
        final T a2         = a.multiply(a);
        final T rOa        = r.divide(absA);

        final T cosI       = i.cos();
        final T sinI       = i.sin();

        final T pv         = FieldVector3D.dotProduct(position, velocity);

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<T>(a2.multiply(-2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = velocity.scalarMultiply(a2.multiply(-2 / mu));
        fillHalfRow(this.one.negate(), vectorAR,    jacobian[0], 0);
        fillHalfRow(this.one.negate(), vectorARDot, jacobian[0], 3);

        // differentials of the momentum
        final T m      = momentum.getNorm();
        final T oOm    = m.reciprocal();
        final FieldVector3D<T> dcXP = new FieldVector3D<T>(this.zero, vz, vy.negate());
        final FieldVector3D<T> dcYP = new FieldVector3D<T>(vz.negate(),   this.zero,  vx);
        final FieldVector3D<T> dcZP = new FieldVector3D<T>( vy, vx.negate(),   this.zero);
        final FieldVector3D<T> dcXV = new FieldVector3D<T>(  this.zero,  z.negate(),   y);
        final FieldVector3D<T> dcYV = new FieldVector3D<T>(  z,   this.zero,  x.negate());
        final FieldVector3D<T> dcZV = new FieldVector3D<T>( y.negate(),   x,   this.zero);
        final FieldVector3D<T> dCP  = new FieldVector3D<T>(mx.multiply(oOm), dcXP, my.multiply(oOm), dcYP, mz.multiply(oOm), dcZP);
        final FieldVector3D<T> dCV  = new FieldVector3D<T>(mx.multiply(oOm), dcXV, my.multiply(oOm), dcYV, mz.multiply(oOm), dcZV);

        // dp
        final T mOMu   = m.divide(mu);
        final FieldVector3D<T> dpP  = new FieldVector3D<T>(mOMu.multiply(2), dCP);
        final FieldVector3D<T> dpV  = new FieldVector3D<T>(mOMu.multiply(2), dCV);

        // de
        final T p      = m.multiply(mOMu);
        final T moO2ae = absA.multiply(2).multiply(e).reciprocal();
        final T m2OaMu = p.negate().divide(absA);
        fillHalfRow(moO2ae, dpP, m2OaMu.multiply(moO2ae), vectorAR,    jacobian[1], 0);
        fillHalfRow(moO2ae, dpV, m2OaMu.multiply(moO2ae), vectorARDot, jacobian[1], 3);

        // di
        final T cI1 = m.multiply(sinI).reciprocal();
        final T cI2 = cosI.multiply(cI1);
        fillHalfRow(cI2, dCP, cI1.negate(), dcZP, jacobian[2], 0);
        fillHalfRow(cI2, dCV, cI1.negate(), dcZV, jacobian[2], 3);


        // dPA
        final T cP1     =  y.multiply(oOm);
        final T cP2     =  x.negate().multiply(oOm);
        final T cP3     =  mx.multiply(cP1).add(my.multiply(cP2)).negate();
        final T cP4     =  cP3.multiply(oOm);
        final T cP5     =  r2.multiply(sinI).multiply(sinI).negate().reciprocal();
        final T cP6     = z.multiply(cP5);
        final T cP7     = cP3.multiply(cP5);
        final FieldVector3D<T> dacP  = new FieldVector3D<T>(cP1, dcXP, cP2, dcYP, cP4, dCP, oOm, new FieldVector3D<T>(my.negate(), mx, this.zero));
        final FieldVector3D<T> dacV  = new FieldVector3D<T>(cP1, dcXV, cP2, dcYV, cP4, dCV);
        final FieldVector3D<T> dpoP  = new FieldVector3D<T>(cP6, dacP, cP7, this.PLUS_K);
        final FieldVector3D<T> dpoV  = new FieldVector3D<T>(cP6, dacV);

        final T re2     = r2.multiply(e).multiply(e);
        final T recOre2 = p.subtract(r).divide(re2);
        final T resOre2 = pv.multiply(mOMu).divide(re2);
        final FieldVector3D<T> dreP  = new FieldVector3D<T>(mOMu, velocity, pv.divide(mu), dCP);
        final FieldVector3D<T> dreV  = new FieldVector3D<T>(mOMu, position, pv.divide(mu), dCV);
        final FieldVector3D<T> davP  = new FieldVector3D<T>(resOre2.negate(), dpP, recOre2, dreP, resOre2.divide(r), position);
        final FieldVector3D<T> davV  = new FieldVector3D<T>(resOre2.negate(), dpV, recOre2, dreV);
        fillHalfRow(this.one, dpoP, this.one.negate(), davP, jacobian[3], 0);
        fillHalfRow(this.one, dpoV, this.one.negate(), davV, jacobian[3], 3);

        // dRAAN
        final T cO0 = cI1.multiply(cI1);
        final T cO1 =  mx.multiply(cO0);
        final T cO2 =  my.negate().multiply(cO0);
        fillHalfRow(cO1, dcYP, cO2, dcXP, jacobian[4], 0);
        fillHalfRow(cO1, dcYV, cO2, dcXV, jacobian[4], 3);

        // dM
        final T s2a    = pv.divide(absA.multiply(2));
        final T oObux  = m.multiply(m).add(absA.multiply(mu)).sqrt().reciprocal();
        final T scasbu = pv.multiply(oObux);
        final FieldVector3D<T> dauP = new FieldVector3D<T>(sqrtMuA.reciprocal(), velocity, s2a.negate().divide(sqrtMuA), vectorAR);
        final FieldVector3D<T> dauV = new FieldVector3D<T>(sqrtMuA.reciprocal(), position, s2a.negate().divide(sqrtMuA), vectorARDot);
        final FieldVector3D<T> dbuP = new FieldVector3D<T>(oObux.multiply(mu / 2), vectorAR,    m.multiply(oObux), dCP);
        final FieldVector3D<T> dbuV = new FieldVector3D<T>(oObux.multiply(mu / 2), vectorARDot, m.multiply(oObux), dCV);
        final FieldVector3D<T> dcuP = new FieldVector3D<T>(oObux, velocity, scasbu.negate().multiply(oObux), dbuP);
        final FieldVector3D<T> dcuV = new FieldVector3D<T>(oObux, position, scasbu.negate().multiply(oObux), dbuV);
        fillHalfRow(this.one, dauP, e.negate().divide(rOa.add(1)), dcuP, jacobian[5], 0);
        fillHalfRow(this.one, dauV, e.negate().divide(rOa.add(1)), dcuV, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianEccentricWrtCartesian() {
        if (a.getReal() > 0) {
            return computeJacobianEccentricWrtCartesianElliptical();
        } else {
            return computeJacobianEccentricWrtCartesianHyperbolic();
        }
    }

    /** Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private T[][] computeJacobianEccentricWrtCartesianElliptical() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesianElliptical();

        // Differentiating the Kepler equation M = E - e sin E leads to:
        // dM = (1 - e cos E) dE - sin E de
        // which is inverted and rewritten as:
        // dE = a/r dM + sin E a/r de
        final T eccentricAnomaly = getEccentricAnomaly();
        final T cosE             = eccentricAnomaly.cos();
        final T sinE             = eccentricAnomaly.sin();
        final T aOr              = e.negate().multiply(cosE).add(1).reciprocal();

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aOr.multiply(anomalyRow[j].add(sinE.multiply(eRow[j])));
        }

        return jacobian;

    }

    /** Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private T[][] computeJacobianEccentricWrtCartesianHyperbolic() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesianHyperbolic();

        // Differentiating the Kepler equation M = e sinh H - H leads to:
        // dM = (e cosh H - 1) dH + sinh H de
        // which is inverted and rewritten as:
        // dH = 1 / (e cosh H - 1) dM - sinh H / (e cosh H - 1) de
        final T H      = getEccentricAnomaly();
        final T coshH  = H.cosh();
        final T sinhH  = H.sinh();
        final T absaOr = e.multiply(coshH).subtract(1).reciprocal();
        // update anomaly row
        final T[] eRow       = jacobian[1];
        final T[] anomalyRow = jacobian[5];

        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = absaOr.multiply(anomalyRow[j].subtract(sinhH.multiply(eRow[j])));

        }

        return jacobian;

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianTrueWrtCartesian() {
        if (a.getReal() > 0) {
            return computeJacobianTrueWrtCartesianElliptical();
        } else {
            return computeJacobianTrueWrtCartesianHyperbolic();
        }
    }

    /** Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private T[][] computeJacobianTrueWrtCartesianElliptical() {

        // start by computing the Jacobian with eccentric angle
        final T[][] jacobian = computeJacobianEccentricWrtCartesianElliptical();
        // Differentiating the eccentric anomaly equation sin E = sqrt(1-e^2) sin v / (1 + e cos v)
        // and using cos E = (e + cos v) / (1 + e cos v) to get rid of cos E leads to:
        // dE = [sqrt (1 - e^2) / (1 + e cos v)] dv - [sin E / (1 - e^2)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dE + [sin E / sqrt (1 - e^2)] a/r de
        final T e2               = e.multiply(e);
        final T oMe2             = e2.negate().add(1);
        final T epsilon          = oMe2.sqrt();
        final T eccentricAnomaly = getEccentricAnomaly();
        final T cosE             = eccentricAnomaly.cos();
        final T sinE             = eccentricAnomaly.sin();
        final T aOr              = e.multiply(cosE).negate().add(1).reciprocal();
        final T aFactor          = epsilon.multiply(aOr);
        final T eFactor          = sinE.multiply(aOr).divide(epsilon);

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor.multiply(anomalyRow[j]).add(eFactor.multiply(eRow[j]));
        }
        return jacobian;

    }

    /** Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with
     * respect to cartesian coordinate j (x for j=0, y for j=1, z for j=2, xDot for j=3,
     * yDot for j=4, zDot for j=5).
     * </p>
     * @return 6x6 Jacobian matrix
     */
    private T[][] computeJacobianTrueWrtCartesianHyperbolic() {

        // start by computing the Jacobian with eccentric angle
        final T[][] jacobian = computeJacobianEccentricWrtCartesianHyperbolic();

        // Differentiating the eccentric anomaly equation sinh H = sqrt(e^2-1) sin v / (1 + e cos v)
        // and using cosh H = (e + cos v) / (1 + e cos v) to get rid of cosh H leads to:
        // dH = [sqrt (e^2 - 1) / (1 + e cos v)] dv + [sinh H / (e^2 - 1)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dH - [sinh H / sqrt (e^2 - 1)] a/r de
        final T e2       = e.multiply(e);
        final T e2Mo     = e2.subtract(1);
        final T epsilon  = e2Mo.sqrt();
        final T H        = getEccentricAnomaly();
        final T coshH    = H.cosh();
        final T sinhH    = H.sinh();
        final T aOr      = e.multiply(coshH).subtract(1).reciprocal();
        final T aFactor  = epsilon.multiply(aOr);
        final T eFactor  = sinhH .multiply(aOr).divide(epsilon);

        // update anomaly row
        final T[] eRow           = jacobian[1];
        final T[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor.multiply(anomalyRow[j]).subtract(eFactor.multiply(eRow[j]));
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final double gm,
                                      final T[] pDot) {
        final T oMe2;
        final T ksi;
        final T absA = a.abs();
        final T n    = absA.reciprocal().multiply(gm).sqrt().divide(absA);
        switch (type) {
            case MEAN :
                pDot[5] = pDot[5].add(n);
                break;
            case ECCENTRIC :
                oMe2 = e.multiply(e).negate().add(1).abs();
                ksi  = e.multiply(v.cos()).add(1);
                pDot[5] = pDot[5].add( n.multiply(ksi).divide(oMe2));
                break;
            case TRUE :
                oMe2 = e.multiply(e).negate().add(1).abs();
                ksi  = e.multiply(v.cos()).add(1);
                pDot[5] = pDot[5].add(n.multiply(ksi).multiply(ksi).divide(oMe2.multiply(oMe2.sqrt())));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this keplerian parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("keplerian parameters: ").append('{').
                                  append("a: ").append(a.getReal()).
                                  append("; e: ").append(e.getReal()).
                                  append("; i: ").append(FastMath.toDegrees(i.getReal())).
                                  append("; pa: ").append(FastMath.toDegrees(pa.getReal())).
                                  append("; raan: ").append(FastMath.toDegrees(raan.getReal())).
                                  append("; v: ").append(FastMath.toDegrees(v.getReal())).
                                  append(";}").toString();
    }
//
//    /** Replace the instance with a data transfer object for serialization.
//     * @return data transfer object that will be serialized
//     */
//    private Object writeReplace() {
//        return new DTO(this);
//    }
//    /** Internal class used only for serialization. */
//    private class DTO implements Serializable {
//
//        /** Serializable UID. */
//        private static final long serialVersionUID = 20140617L;
//
//        /** Double values. */
//        private T[] d;
//
//        /** Frame in which are defined the orbital parameters. */
//        private final Frame frame;
//
//        /** Simple constructor.
//         * @param orbit instance to serialize
//         */
//        private DTO(final FieldKeplerianOrbit<T> orbit) {
//
//            final TimeStampedFieldPVCoordinates<T> pv = orbit.getFieldPVCoordinates();
//            final FieldAbsoluteDate<T> FAD = new FieldAbsoluteDate<T>(orbit.a.getField());
//            // decompose date
//            final double epoch  = FastMath.floor(pv.getDate().durationFrom(FAD.J2000_EPOCH(orbit.a.getField())).getReal());
//            final T offset = pv.getDate().durationFrom(FAD.J2000_EPOCH(orbit.a.getField()).shiftedBy(epoch));
//
//            this.d = MathArrays.buildArray(getA().getField(), 9);
//            this.d[0] = a.getField().getZero().add(epoch);
//            this.d[1] = a.getField().getZero().add(offset);
//            this.d[2] = a.getField().getZero().add(orbit.getMu());
//            this.d[3] = orbit.a;
//            this.d[4] = orbit.e;
//            this.d[5] = orbit.i;
//            this.d[6] = orbit.pa;
//            this.d[7] = orbit.raan;
//            this.d[8] = orbit.v;
//
//            this.frame = orbit.getFrame();
//
//        }
//
//        /** Replace the deserialized data transfer object with a {@link FieldKeplerianOrbit}.
//         * @return replacement {@link FieldKeplerianOrbit}
//         */
//        /**
//        private Object readResolve() {
//            return new FieldKeplerianOrbit<T>(d[3], d[4], d[5], d[6], d[7], d[8], PositionAngle.TRUE,
//                                      frame, FieldAbsoluteDate<T>.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
//                                      d[2]);
//        }
//        */
//    }

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
    public KeplerianOrbit toOrbit() {
        return new KeplerianOrbit(a.getReal(), e.getReal(), i.getReal(),
                                         pa.getReal(), raan.getReal(),
                                         v.getReal(), PositionAngle.TRUE,
                                         getFrame(), getDate().toAbsoluteDate(), getMu());
    }


}
