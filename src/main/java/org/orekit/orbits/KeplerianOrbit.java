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

import java.io.Serializable;
import java.util.Collection;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


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
 *
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
 */
public class KeplerianOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 20141228L;

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
    private final double a;

    /** Eccentricity. */
    private final double e;

    /** Inclination (rad). */
    private final double i;

    /** Perigee Argument (rad). */
    private final double pa;

    /** Right Ascension of Ascending Node (rad). */
    private final double raan;

    /** True anomaly (rad). */
    private final double v;

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
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan,
                          final double anomaly, final PositionAngle type,
                          final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);

        if (a * (1 - e) < 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a, e);
        }

        this.a    =    a;
        this.e    =    e;
        this.i    =    i;
        this.pa   =   pa;
        this.raan = raan;

        final double tmpV;
        switch (type) {
            case MEAN :
                tmpV = (a < 0) ? hyperbolicEccentricToTrue(meanToHyperbolicEccentric(anomaly, e)) :
                    ellipticEccentricToTrue(meanToEllipticEccentric(anomaly));
                break;
            case ECCENTRIC :
                tmpV = (a < 0) ? hyperbolicEccentricToTrue(anomaly) :
                    ellipticEccentricToTrue(anomaly);
                break;
            case TRUE :
                tmpV = anomaly;
                break;
            default : // this should never happen
                throw new OrekitInternalError(null);
        }

        // check true anomaly range
        if (1 + e * FastMath.cos(tmpV) <= 0) {
            final double vMax = FastMath.acos(-1 / e);
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE,
                                                                 tmpV, e, -vMax, vMax);
        }
        this.v = tmpV;

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public KeplerianOrbit(final TimeStampedPVCoordinates pvCoordinates,
                          final Frame frame, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);

        // compute inclination
        final Vector3D momentum = pvCoordinates.getMomentum();
        final double m2 = momentum.getNormSq();
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        raan = Vector3D.crossProduct(Vector3D.PLUS_K, momentum).getAlpha();

        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final Vector3D pvP     = pvCoordinates.getPosition();
        final Vector3D pvV     = pvCoordinates.getVelocity();
        final double   r       = pvP.getNorm();
        final double   V2      = pvV.getNormSq();
        final double   rV2OnMu = r * V2 / mu;

        // compute semi-major axis (will be negative for hyperbolic orbits)
        a = r / (2 - rV2OnMu);
        final double muA = mu * a;

        // compute true anomaly
        if (a > 0) {
            // elliptic or circular orbit
            final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(muA);
            final double eCE = rV2OnMu - 1;
            e = FastMath.sqrt(eSE * eSE + eCE * eCE);
            v = ellipticEccentricToTrue(FastMath.atan2(eSE, eCE));
        } else {
            // hyperbolic orbit
            final double eSH = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(-muA);
            final double eCH = rV2OnMu - 1;
            e = FastMath.sqrt(1 - m2 / muA);
            v = hyperbolicEccentricToTrue(FastMath.log((eCH + eSH) / (eCH - eSH)) / 2);
        }

        // compute perigee argument
        final Vector3D node = new Vector3D(raan, 0.0);
        final double px = Vector3D.dotProduct(pvP, node);
        final double py = Vector3D.dotProduct(pvP, Vector3D.crossProduct(momentum, node)) / FastMath.sqrt(m2);
        pa = FastMath.atan2(py, px) - v;

    }

    /** Constructor from cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(double)} and {@link #getPVCoordinates(AbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public KeplerianOrbit(final PVCoordinates pvCoordinates,
                          final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedPVCoordinates(date, pvCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public KeplerianOrbit(final Orbit op) {
        this(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    public double getA() {
        return a;
    }

    /** {@inheritDoc} */
    public double getE() {
        return e;
    }

    /** {@inheritDoc} */
    public double getI() {
        return i;
    }

    /** Get the perigee argument.
     * @return perigee argument (rad)
     */
    public double getPerigeeArgument() {
        return pa;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the anomaly.
     * @param type type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getMeanAnomaly() :
                                              ((type == PositionAngle.ECCENTRIC) ? getEccentricAnomaly() :
                                                                                   getTrueAnomaly());
    }

    /** Get the true anomaly.
     * @return true anomaly (rad)
     */
    public double getTrueAnomaly() {
        return v;
    }

    /** Get the eccentric anomaly.
     * @return eccentric anomaly (rad)
     */
    public double getEccentricAnomaly() {
        if (a < 0) {
            // hyperbolic case
            final double sinhH = FastMath.sqrt(e * e - 1) * FastMath.sin(v) /
                                 (1 + e * FastMath.cos(v));
            return FastMath.asinh(sinhH);
        }

        // elliptic case
        final double beta = e / (1 + FastMath.sqrt((1 - e) * (1 + e)));
        return v - 2 * FastMath.atan(beta * FastMath.sin(v) / (1 + beta * FastMath.cos(v)));

    }

    /** Computes the true anomaly from the elliptic eccentric anomaly.
     * @param E eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private double ellipticEccentricToTrue(final double E) {
        final double beta = e / (1 + FastMath.sqrt((1 - e) * (1 + e)));
        return E + 2 * FastMath.atan(beta * FastMath.sin(E) / (1 - beta * FastMath.cos(E)));
    }

    /** Computes the true anomaly from the hyperbolic eccentric anomaly.
     * @param H hyperbolic eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private double hyperbolicEccentricToTrue(final double H) {
        return 2 * FastMath.atan(FastMath.sqrt((e + 1) / (e - 1)) * FastMath.tanh(H / 2));
    }

    /** Get the mean anomaly.
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {

        if (a < 0) {
            // hyperbolic case
            final double H = getEccentricAnomaly();
            return e * FastMath.sinh(H) - H;
        }

        // elliptic case
        final double E = getEccentricAnomaly();
        return E - e * FastMath.sin(E);

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
    private double meanToEllipticEccentric(final double M) {

        // reduce M to [-PI PI) interval
        final double reducedM = MathUtils.normalizeAngle(M, 0.0);

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        double E;
        if (FastMath.abs(reducedM) < 1.0 / 6.0) {
            E = reducedM + e * (FastMath.cbrt(6 * reducedM) - reducedM);
        } else {
            if (reducedM < 0) {
                final double w = FastMath.PI + reducedM;
                E = reducedM + e * (A * w / (B - w) - FastMath.PI - reducedM);
            } else {
                final double w = FastMath.PI - reducedM;
                E = reducedM + e * (FastMath.PI - A * w / (B - w) - reducedM);
            }
        }

        final double e1 = 1 - e;
        final boolean noCancellationRisk = (e1 + E * E / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final double f;
            double fd;
            final double fdd  = e * FastMath.sin(E);
            final double fddd = e * FastMath.cos(E);
            if (noCancellationRisk) {
                f  = (E - fdd) - reducedM;
                fd = 1 - fddd;
            } else {
                f  = eMeSinE(E) - reducedM;
                final double s = FastMath.sin(0.5 * E);
                fd = e1 + 2 * e * s * s;
            }
            final double dee = f * fd / (0.5 * f * fdd - fd * fd);

            // update eccentric anomaly, using expressions that limit underflow problems
            final double w = fd + 0.5 * dee * (fdd + dee * fddd / 3);
            fd += dee * (fdd + 0.5 * dee * fddd);
            E  -= (f - dee * (fd - w)) / fd;

        }

        // expand the result back to original range
        E += M - reducedM;

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
    private double eMeSinE(final double E) {
        double x = (1 - e) * FastMath.sin(E);
        final double mE2 = -E * E;
        double term = E;
        double d    = 0;
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (double x0 = Double.NaN; x != x0;) {
            d += 2;
            term *= mE2 / (d * (d + 1));
            x0 = x;
            x = x - term;
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
    private double meanToHyperbolicEccentric(final double M, final double ecc) {

        // Resolution of hyperbolic Kepler equation for keplerian parameters

        // Initial guess
        double H;
        if (ecc < 1.6) {
            if ((-FastMath.PI < M && M < 0.) || M > FastMath.PI) {
                H = M - ecc;
            } else {
                H = M + ecc;
            }
        } else {
            if (ecc < 3.6 && FastMath.abs(M) > FastMath.PI) {
                H = M - FastMath.copySign(ecc, M);
            } else {
                H = M / (ecc - 1.);
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final double f3  = ecc * FastMath.cosh(H);
            final double f2  = ecc * FastMath.sinh(H);
            final double f1  = f3 - 1.;
            final double f0  = f2 - H - M;
            final double f12 = 2. * f1;
            final double d   = f0 / f12;
            final double fdf = f1 - d * f2;
            final double ds  = f0 / fdf;

            final double shift = f0 / (fdf + ds * ds * f3 / 6.);

            H -= shift;

            if (FastMath.abs(shift) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY,
                                            iter);
    }

    /** {@inheritDoc} */
    public double getEquinoctialEx() {
        return  e * FastMath.cos(pa + raan);
    }

    /** {@inheritDoc} */
    public double getEquinoctialEy() {
        return  e * FastMath.sin(pa + raan);
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
    public double getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return  FastMath.sin(raan) * FastMath.tan(i / 2);
    }

    /** {@inheritDoc} */
    public double getLv() {
        return pa + raan + v;
    }

    /** {@inheritDoc} */
    public double getLE() {
        return pa + raan + getEccentricAnomaly();
    }

    /** {@inheritDoc} */
    public double getLM() {
        return pa + raan + getMeanAnomaly();
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates initPVCoordinates() {

        // preliminary variables
        final double cosRaan = FastMath.cos(raan);
        final double sinRaan = FastMath.sin(raan);
        final double cosPa   = FastMath.cos(pa);
        final double sinPa   = FastMath.sin(pa);
        final double cosI    = FastMath.cos(i);
        final double sinI    = FastMath.sin(i);

        final double crcp    = cosRaan * cosPa;
        final double crsp    = cosRaan * sinPa;
        final double srcp    = sinRaan * cosPa;
        final double srsp    = sinRaan * sinPa;

        // reference axes defining the orbital plane
        final Vector3D p = new Vector3D( crcp - cosI * srsp,  srcp + cosI * crsp, sinI * sinPa);
        final Vector3D q = new Vector3D(-crsp - cosI * srcp, -srsp + cosI * crcp, sinI * cosPa);

        return (a > 0) ? initPVCoordinatesElliptical(p, q) : initPVCoordinatesHyperbolic(p, q);

    }

    /** Initialize the position/velocity coordinates, elliptic case.
     * @param p unit vector in the orbital plane pointing towards perigee
     * @param q unit vector in the orbital plane in quadrature with p
     * @return computed position/velocity coordinates
     */
    private TimeStampedPVCoordinates initPVCoordinatesElliptical(final Vector3D p, final Vector3D q) {

        // elliptic eccentric anomaly
        final double uME2   = (1 - e) * (1 + e);
        final double s1Me2  = FastMath.sqrt(uME2);
        final double E      = getEccentricAnomaly();
        final double cosE   = FastMath.cos(E);
        final double sinE   = FastMath.sin(E);

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cosE - e);
        final double y      = a * sinE * s1Me2;
        final double factor = FastMath.sqrt(getMu() / a) / (1 - e * cosE);
        final double xDot   = -sinE * factor;
        final double yDot   =  cosE * s1Me2 * factor;


        final Vector3D position = new Vector3D(x, p, y, q);
        final double r2 = x * x + y * y;
        final Vector3D velocity = new Vector3D(xDot, p, yDot, q);
        final Vector3D acceleration = new Vector3D(-getMu() / (r2 * FastMath.sqrt(r2)), position);
        return new TimeStampedPVCoordinates(getDate(), position, velocity, acceleration);

    }

    /** Initialize the position/velocity coordinates, hyperbolic case.
     * @param p unit vector in the orbital plane pointing towards perigee
     * @param q unit vector in the orbital plane in quadrature with p
     * @return computed position/velocity coordinates
     */
    private TimeStampedPVCoordinates initPVCoordinatesHyperbolic(final Vector3D p, final Vector3D q) {

        // compute position and velocity factors
        final double sinV      = FastMath.sin(v);
        final double cosV      = FastMath.cos(v);
        final double f         = a * (1 - e * e);
        final double posFactor = f / (1 + e * cosV);
        final double velFactor = FastMath.sqrt(getMu() / f);

        final Vector3D position     = new Vector3D( posFactor * cosV, p, posFactor * sinV, q);
        final Vector3D velocity     = new Vector3D(-velFactor * sinV, p, velFactor * (e + cosV), q);
        final Vector3D acceleration = new Vector3D(-getMu() / (posFactor * posFactor * posFactor), position);
        return new TimeStampedPVCoordinates(getDate(), position, velocity, acceleration);

    }

    /** {@inheritDoc} */
    public KeplerianOrbit shiftedBy(final double dt) {
        return new KeplerianOrbit(a, e, i, pa, raan,
                                  getMeanAnomaly() + getKeplerianMeanMotion() * dt,
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
    public KeplerianOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousPA   = Double.NaN;
        double previousRAAN = Double.NaN;
        double previousM    = Double.NaN;
        for (final Orbit orbit : sample) {
            final KeplerianOrbit kep = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
            final double continuousPA;
            final double continuousRAAN;
            final double continuousM;
            if (previousDate == null) {
                continuousPA   = kep.getPerigeeArgument();
                continuousRAAN = kep.getRightAscensionOfAscendingNode();
                continuousM    = kep.getMeanAnomaly();
            } else {
                final double dt      = kep.getDate().durationFrom(previousDate);
                final double keplerM = previousM + kep.getKeplerianMeanMotion() * dt;
                continuousPA   = MathUtils.normalizeAngle(kep.getPerigeeArgument(), previousPA);
                continuousRAAN = MathUtils.normalizeAngle(kep.getRightAscensionOfAscendingNode(), previousRAAN);
                continuousM    = MathUtils.normalizeAngle(kep.getMeanAnomaly(), keplerM);
            }
            previousDate = kep.getDate();
            previousPA   = continuousPA;
            previousRAAN = continuousRAAN;
            previousM    = continuousM;
            interpolator.addSamplePoint(kep.getDate().durationFrom(date),
                                        new double[] {
                                            kep.getA(),
                                            kep.getE(),
                                            kep.getI(),
                                            continuousPA,
                                            continuousRAAN,
                                            continuousM
                                        });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new KeplerianOrbit(interpolated[0], interpolated[1], interpolated[2],
                                  interpolated[3], interpolated[4], interpolated[5],
                                  PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianMeanWrtCartesian() {
        if (a > 0) {
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
    private double[][] computeJacobianMeanWrtCartesianElliptical() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        final PVCoordinates pvc = getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();
        final Vector3D momentum = pvc.getMomentum();
        final double v2         = velocity.getNormSq();
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double r3         = r * r2;

        final double px         = position.getX();
        final double py         = position.getY();
        final double pz         = position.getZ();
        final double vx         = velocity.getX();
        final double vy         = velocity.getY();
        final double vz         = velocity.getZ();
        final double mx         = momentum.getX();
        final double my         = momentum.getY();
        final double mz         = momentum.getZ();

        final double mu         = getMu();
        final double sqrtMuA    = FastMath.sqrt(a * mu);
        final double sqrtAoMu   = FastMath.sqrt(a / mu);
        final double a2         = a * a;
        final double twoA       = 2 * a;
        final double rOnA       = r / a;

        final double oMe2       = 1 - e * e;
        final double epsilon    = FastMath.sqrt(oMe2);
        final double sqrtRec    = 1 / epsilon;

        final double cosI       = FastMath.cos(i);
        final double sinI       = FastMath.sin(i);
        final double cosPA      = FastMath.cos(pa);
        final double sinPA      = FastMath.sin(pa);

        final double pv         = Vector3D.dotProduct(position, velocity);
        final double cosE       = (a - r) / (a * e);
        final double sinE       = pv / (e * sqrtMuA);

        // da
        final Vector3D vectorAR = new Vector3D(2 * a2 / r3, position);
        final Vector3D vectorARDot = velocity.scalarMultiply(2 * a2 / mu);
        fillHalfRow(1, vectorAR,    jacobian[0], 0);
        fillHalfRow(1, vectorARDot, jacobian[0], 3);

        // de
        final double factorER3 = pv / twoA;
        final Vector3D vectorER   = new Vector3D(cosE * v2 / (r * mu), position,
                                                 sinE / sqrtMuA, velocity,
                                                 -factorER3 * sinE / sqrtMuA, vectorAR);
        final Vector3D vectorERDot = new Vector3D(sinE / sqrtMuA, position,
                                                  cosE * 2 * r / mu, velocity,
                                                  -factorER3 * sinE / sqrtMuA, vectorARDot);
        fillHalfRow(1, vectorER,    jacobian[1], 0);
        fillHalfRow(1, vectorERDot, jacobian[1], 3);

        // dE / dr (Eccentric anomaly)
        final double coefE = cosE / (e * sqrtMuA);
        final Vector3D  vectorEAnR =
            new Vector3D(-sinE * v2 / (e * r * mu), position, coefE, velocity,
                         -factorER3 * coefE, vectorAR);

        // dE / drDot
        final Vector3D  vectorEAnRDot =
            new Vector3D(-sinE * 2 * r / (e * mu), velocity, coefE, position,
                         -factorER3 * coefE, vectorARDot);

        // precomputing some more factors
        final double s1 = -sinE * pz / r - cosE * vz * sqrtAoMu;
        final double s2 = -cosE * pz / r3;
        final double s3 = -sinE * vz / (2 * sqrtMuA);
        final double t1 = sqrtRec * (cosE * pz / r - sinE * vz * sqrtAoMu);
        final double t2 = sqrtRec * (-sinE * pz / r3);
        final double t3 = sqrtRec * (cosE - e) * vz / (2 * sqrtMuA);
        final double t4 = sqrtRec * (e * sinI * cosPA * sqrtRec - vz * sqrtAoMu);
        final Vector3D s = new Vector3D(cosE / r, Vector3D.PLUS_K,
                                        s1,       vectorEAnR,
                                        s2,       position,
                                        s3,       vectorAR);
        final Vector3D sDot = new Vector3D(-sinE * sqrtAoMu, Vector3D.PLUS_K,
                                           s1,               vectorEAnRDot,
                                           s3,               vectorARDot);
        final Vector3D t =
            new Vector3D(sqrtRec * sinE / r, Vector3D.PLUS_K).add(new Vector3D(t1, vectorEAnR,
                                                                               t2, position,
                                                                               t3, vectorAR,
                                                                               t4, vectorER));
        final Vector3D tDot = new Vector3D(sqrtRec * (cosE - e) * sqrtAoMu, Vector3D.PLUS_K,
                                           t1,                              vectorEAnRDot,
                                           t3,                              vectorARDot,
                                           t4,                              vectorERDot);

        // di
        final double factorI1 = -sinI * sqrtRec / sqrtMuA;
        final double i1 =  factorI1;
        final double i2 = -factorI1 * mz / twoA;
        final double i3 =  factorI1 * mz * e / oMe2;
        final double i4 = cosI * sinPA;
        final double i5 = cosI * cosPA;
        fillHalfRow(i1, new Vector3D(vy, -vx, 0), i2, vectorAR, i3, vectorER, i4, s, i5, t,
                    jacobian[2], 0);
        fillHalfRow(i1, new Vector3D(-py, px, 0), i2, vectorARDot, i3, vectorERDot, i4, sDot, i5, tDot,
                    jacobian[2], 3);

        // dpa
        fillHalfRow(cosPA / sinI, s,    -sinPA / sinI, t,    jacobian[3], 0);
        fillHalfRow(cosPA / sinI, sDot, -sinPA / sinI, tDot, jacobian[3], 3);

        // dRaan
        final double factorRaanR = 1 / (mu * a * oMe2 * sinI * sinI);
        fillHalfRow(-factorRaanR * my, new Vector3D(  0, vz, -vy),
                     factorRaanR * mx, new Vector3D(-vz,  0,  vx),
                     jacobian[4], 0);
        fillHalfRow(-factorRaanR * my, new Vector3D( 0, -pz,  py),
                     factorRaanR * mx, new Vector3D(pz,   0, -px),
                     jacobian[4], 3);

        // dM
        fillHalfRow(rOnA, vectorEAnR,    -sinE, vectorER,    jacobian[5], 0);
        fillHalfRow(rOnA, vectorEAnRDot, -sinE, vectorERDot, jacobian[5], 3);

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
    private double[][] computeJacobianMeanWrtCartesianHyperbolic() {

        final double[][] jacobian = new double[6][6];

        // compute various intermediate parameters
        final PVCoordinates pvc = getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();
        final Vector3D momentum = pvc.getMomentum();
        final double r2         = position.getNormSq();
        final double r          = FastMath.sqrt(r2);
        final double r3         = r * r2;

        final double x          = position.getX();
        final double y          = position.getY();
        final double z          = position.getZ();
        final double vx         = velocity.getX();
        final double vy         = velocity.getY();
        final double vz         = velocity.getZ();
        final double mx         = momentum.getX();
        final double my         = momentum.getY();
        final double mz         = momentum.getZ();

        final double mu         = getMu();
        final double absA       = -a;
        final double sqrtMuA    = FastMath.sqrt(absA * mu);
        final double a2         = a * a;
        final double rOa        = r / absA;

        final double cosI       = FastMath.cos(i);
        final double sinI       = FastMath.sin(i);

        final double pv         = Vector3D.dotProduct(position, velocity);

        // da
        final Vector3D vectorAR = new Vector3D(-2 * a2 / r3, position);
        final Vector3D vectorARDot = velocity.scalarMultiply(-2 * a2 / mu);
        fillHalfRow(-1, vectorAR,    jacobian[0], 0);
        fillHalfRow(-1, vectorARDot, jacobian[0], 3);

        // differentials of the momentum
        final double m      = momentum.getNorm();
        final double oOm    = 1 / m;
        final Vector3D dcXP = new Vector3D(  0,  vz, -vy);
        final Vector3D dcYP = new Vector3D(-vz,   0,  vx);
        final Vector3D dcZP = new Vector3D( vy, -vx,   0);
        final Vector3D dcXV = new Vector3D(  0,  -z,   y);
        final Vector3D dcYV = new Vector3D(  z,   0,  -x);
        final Vector3D dcZV = new Vector3D( -y,   x,   0);
        final Vector3D dCP  = new Vector3D(mx * oOm, dcXP, my * oOm, dcYP, mz * oOm, dcZP);
        final Vector3D dCV  = new Vector3D(mx * oOm, dcXV, my * oOm, dcYV, mz * oOm, dcZV);

        // dp
        final double mOMu   = m / mu;
        final Vector3D dpP  = new Vector3D(2 * mOMu, dCP);
        final Vector3D dpV  = new Vector3D(2 * mOMu, dCV);

        // de
        final double p      = m * mOMu;
        final double moO2ae = 1 / (2 * absA * e);
        final double m2OaMu = -p / absA;
        fillHalfRow(moO2ae, dpP, m2OaMu * moO2ae, vectorAR,    jacobian[1], 0);
        fillHalfRow(moO2ae, dpV, m2OaMu * moO2ae, vectorARDot, jacobian[1], 3);

        // di
        final double cI1 = 1 / (m * sinI);
        final double cI2 = cosI * cI1;
        fillHalfRow(cI2, dCP, -cI1, dcZP, jacobian[2], 0);
        fillHalfRow(cI2, dCV, -cI1, dcZV, jacobian[2], 3);

        // dPA
        final double cP1     =  y * oOm;
        final double cP2     = -x * oOm;
        final double cP3     = -(mx * cP1 + my * cP2);
        final double cP4     = cP3 * oOm;
        final double cP5     = -1 / (r2 * sinI * sinI);
        final double cP6     = z  * cP5;
        final double cP7     = cP3 * cP5;
        final Vector3D dacP  = new Vector3D(cP1, dcXP, cP2, dcYP, cP4, dCP, oOm, new Vector3D(-my, mx, 0));
        final Vector3D dacV  = new Vector3D(cP1, dcXV, cP2, dcYV, cP4, dCV);
        final Vector3D dpoP  = new Vector3D(cP6, dacP, cP7, Vector3D.PLUS_K);
        final Vector3D dpoV  = new Vector3D(cP6, dacV);

        final double re2     = r2 * e * e;
        final double recOre2 = (p - r) / re2;
        final double resOre2 = (pv * mOMu) / re2;
        final Vector3D dreP  = new Vector3D(mOMu, velocity, pv / mu, dCP);
        final Vector3D dreV  = new Vector3D(mOMu, position, pv / mu, dCV);
        final Vector3D davP  = new Vector3D(-resOre2, dpP, recOre2, dreP, resOre2 / r, position);
        final Vector3D davV  = new Vector3D(-resOre2, dpV, recOre2, dreV);
        fillHalfRow(1, dpoP, -1, davP, jacobian[3], 0);
        fillHalfRow(1, dpoV, -1, davV, jacobian[3], 3);

        // dRAAN
        final double cO0 = cI1 * cI1;
        final double cO1 =  mx * cO0;
        final double cO2 = -my * cO0;
        fillHalfRow(cO1, dcYP, cO2, dcXP, jacobian[4], 0);
        fillHalfRow(cO1, dcYV, cO2, dcXV, jacobian[4], 3);

        // dM
        final double s2a    = pv / (2 * absA);
        final double oObux  = 1 / FastMath.sqrt(m * m + mu * absA);
        final double scasbu = pv * oObux;
        final Vector3D dauP = new Vector3D(1 / sqrtMuA, velocity, -s2a / sqrtMuA, vectorAR);
        final Vector3D dauV = new Vector3D(1 / sqrtMuA, position, -s2a / sqrtMuA, vectorARDot);
        final Vector3D dbuP = new Vector3D(oObux * mu / 2, vectorAR,    m * oObux, dCP);
        final Vector3D dbuV = new Vector3D(oObux * mu / 2, vectorARDot, m * oObux, dCV);
        final Vector3D dcuP = new Vector3D(oObux, velocity, -scasbu * oObux, dbuP);
        final Vector3D dcuV = new Vector3D(oObux, position, -scasbu * oObux, dbuV);
        fillHalfRow(1, dauP, -e / (1 + rOa), dcuP, jacobian[5], 0);
        fillHalfRow(1, dauV, -e / (1 + rOa), dcuV, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianEccentricWrtCartesian() {
        if (a > 0) {
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
    private double[][] computeJacobianEccentricWrtCartesianElliptical() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesianElliptical();

        // Differentiating the Kepler equation M = E - e sin E leads to:
        // dM = (1 - e cos E) dE - sin E de
        // which is inverted and rewritten as:
        // dE = a/r dM + sin E a/r de
        final double eccentricAnomaly = getEccentricAnomaly();
        final double cosE             = FastMath.cos(eccentricAnomaly);
        final double sinE             = FastMath.sin(eccentricAnomaly);
        final double aOr              = 1 / (1 - e * cosE);

        // update anomaly row
        final double[] eRow           = jacobian[1];
        final double[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aOr * (anomalyRow[j] + sinE * eRow[j]);
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
    private double[][] computeJacobianEccentricWrtCartesianHyperbolic() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = computeJacobianMeanWrtCartesianHyperbolic();

        // Differentiating the Kepler equation M = e sinh H - H leads to:
        // dM = (e cosh H - 1) dH + sinh H de
        // which is inverted and rewritten as:
        // dH = 1 / (e cosh H - 1) dM - sinh H / (e cosh H - 1) de
        final double H      = getEccentricAnomaly();
        final double coshH  = FastMath.cosh(H);
        final double sinhH  = FastMath.sinh(H);
        final double absaOr = 1 / (e * coshH - 1);

        // update anomaly row
        final double[] eRow       = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = absaOr * (anomalyRow[j] - sinhH * eRow[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianTrueWrtCartesian() {
        if (a > 0) {
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
    private double[][] computeJacobianTrueWrtCartesianElliptical() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesianElliptical();

        // Differentiating the eccentric anomaly equation sin E = sqrt(1-e^2) sin v / (1 + e cos v)
        // and using cos E = (e + cos v) / (1 + e cos v) to get rid of cos E leads to:
        // dE = [sqrt (1 - e^2) / (1 + e cos v)] dv - [sin E / (1 - e^2)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dE + [sin E / sqrt (1 - e^2)] a/r de
        final double e2               = e * e;
        final double oMe2             = 1 - e2;
        final double epsilon          = FastMath.sqrt(oMe2);
        final double eccentricAnomaly = getEccentricAnomaly();
        final double cosE             = FastMath.cos(eccentricAnomaly);
        final double sinE             = FastMath.sin(eccentricAnomaly);
        final double aOr              = 1 / (1 - e * cosE);
        final double aFactor          = epsilon * aOr;
        final double eFactor          = sinE * aOr / epsilon;

        // update anomaly row
        final double[] eRow           = jacobian[1];
        final double[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor * anomalyRow[j] + eFactor * eRow[j];
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
    private double[][] computeJacobianTrueWrtCartesianHyperbolic() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = computeJacobianEccentricWrtCartesianHyperbolic();

        // Differentiating the eccentric anomaly equation sinh H = sqrt(e^2-1) sin v / (1 + e cos v)
        // and using cosh H = (e + cos v) / (1 + e cos v) to get rid of cosh H leads to:
        // dH = [sqrt (e^2 - 1) / (1 + e cos v)] dv + [sinh H / (e^2 - 1)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dH - [sinh H / sqrt (e^2 - 1)] a/r de
        final double e2       = e * e;
        final double e2Mo     = e2 - 1;
        final double epsilon  = FastMath.sqrt(e2Mo);
        final double H        = getEccentricAnomaly();
        final double coshH    = FastMath.cosh(H);
        final double sinhH    = FastMath.sinh(H);
        final double aOr      = 1 / (e * coshH - 1);
        final double aFactor  = epsilon * aOr;
        final double eFactor  = sinhH * aOr / epsilon;

        // update anomaly row
        final double[] eRow           = jacobian[1];
        final double[] anomalyRow     = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor * anomalyRow[j] - eFactor * eRow[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final double gm,
                                      final double[] pDot) {
        final double oMe2;
        final double ksi;
        final double absA = FastMath.abs(a);
        final double n    = FastMath.sqrt(gm / absA) / absA;
        switch (type) {
            case MEAN :
                pDot[5] += n;
                break;
            case ECCENTRIC :
                oMe2 = FastMath.abs(1 - e * e);
                ksi  = 1 + e * FastMath.cos(v);
                pDot[5] += n * ksi / oMe2;
                break;
            case TRUE :
                oMe2 = FastMath.abs(1 - e * e);
                ksi  = 1 + e * FastMath.cos(v);
                pDot[5] += n * ksi * ksi / (oMe2 * FastMath.sqrt(oMe2));
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
                                  append("a: ").append(a).
                                  append("; e: ").append(e).
                                  append("; i: ").append(FastMath.toDegrees(i)).
                                  append("; pa: ").append(FastMath.toDegrees(pa)).
                                  append("; raan: ").append(FastMath.toDegrees(raan)).
                                  append("; v: ").append(FastMath.toDegrees(v)).
                                  append(";}").toString();
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140617L;

        /** Double values. */
        private double[] d;

        /** Frame in which are defined the orbital parameters. */
        private final Frame frame;

        /** Simple constructor.
         * @param orbit instance to serialize
         */
        private DTO(final KeplerianOrbit orbit) {

            final TimeStampedPVCoordinates pv = orbit.getPVCoordinates();

            // decompose date
            final double epoch  = FastMath.floor(pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH));
            final double offset = pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH.shiftedBy(epoch));

            this.d = new double[] {
                epoch, offset, orbit.getMu(),
                orbit.a, orbit.e, orbit.i,
                orbit.pa, orbit.raan, orbit.v
            };

            this.frame = orbit.getFrame();

        }

        /** Replace the deserialized data transfer object with a {@link KeplerianOrbit}.
         * @return replacement {@link KeplerianOrbit}
         */
        private Object readResolve() {
            return new KeplerianOrbit(d[3], d[4], d[5], d[6], d[7], d[8], PositionAngle.TRUE,
                                      frame, AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                      d[2]);
        }

    }

}
