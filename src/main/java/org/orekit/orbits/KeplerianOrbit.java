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

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/**
 * This class handles traditional keplerian orbital parameters.

 * <p>
 * The parameters used internally are the classical keplerian elements:
 *   <pre>
 *     a
 *     e
 *     i
 *     &omega;
 *     &Omega;
 *     v
 *   </pre>
 * where &omega; stands for the Perigee Argument, &Omega; stands for the
 * Right Ascension of the Ascending Node and v stands for the true anomaly.
 * </p>
 * <p>
 * This class supports hyperbolic orbits, using the convention that semi major
 * axis is negative for such orbits (and of course eccentricity is greater than 1).
 * </p>
 * <p>
 * When orbit is either equatorial or circular, some keplerian elements
 * (more precisely &omega; and &Omega;) become ambiguous so this class should not
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
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class KeplerianOrbit extends Orbit {

    /** Identifier for mean anomaly.
     * @deprecated as of 5.1 replaced by {@link PositionAngle}
     */
    @Deprecated
    public static final int MEAN_ANOMALY = 0;

    /** Identifier for eccentric anomaly.
     * @deprecated as of 5.1 replaced by {@link PositionAngle}
     */
    @Deprecated
    public static final int ECCENTRIC_ANOMALY = 1;

    /** Identifier for true anomaly.
     * @deprecated as of 5.1 replaced by {@link PositionAngle}
     */
    @Deprecated
    public static final int TRUE_ANOMALY = 2;

    /** Eccentricity threshold for near circular orbits.
     *  if e < E_CIRC : the orbit is considered circular
     */
    public static final double E_CIRC = 1.e-10;

    /** Serializable UID. */
    private static final long serialVersionUID = 7593919633854535287L;

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
     * @param pa perigee argument (&omega;, rad)
     * @param raan right ascension of ascending node (&Omega;, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param type type of anomaly
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan,
                          final double anomaly, final PositionAngle type,
                          final Frame frame, final AbsoluteDate date, final double mu) {
        super(frame, date, mu);

        if (a * (1 - e) < 0) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a, e);
        }

        this.a    =    a;
        this.e    =    e;
        this.i    =    i;
        this.pa   =   pa;
        this.raan = raan;

        switch (type) {
        case MEAN :
            this.v = (a < 0) ?
                     hyperbolicEccentricToTrue(meanToHyperbolicEccentric(anomaly)) :
                     ellipticEccentricToTrue(meanToEllipticEccentric(anomaly));
            break;
        case ECCENTRIC :
            this.v = (a < 0) ?
                     hyperbolicEccentricToTrue(anomaly) :
                     ellipticEccentricToTrue(anomaly);
            break;
        case TRUE :
            this.v = anomaly;
            break;
        default : // this should never happen
            throw OrekitException.createInternalError(null);
        }

    }

    /** Creates a new instance.
     * @param a  semi-major axis (m), negative for hyperbolic orbits
     * @param e eccentricity
     * @param i inclination (rad)
     * @param pa perigee argument (&omega;, rad)
     * @param raan right ascension of ascending node (&Omega;, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
     * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if the longitude argument type is not
     * one of {@link #MEAN_ANOMALY}, {@link #ECCENTRIC_ANOMALY}
     * or {@link #TRUE_ANOMALY} or if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     * @see #MEAN_ANOMALY
     * @see #ECCENTRIC_ANOMALY
     * @see #TRUE_ANOMALY
     * @deprecated as of 5.1 replaced by {@link #KeplerianOrbit(double, double, double,
     * double, double, double, PositionAngle, Frame, AbsoluteDate, double)
     */
    @Deprecated
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan,
                          final double anomaly, final int type,
                          final Frame frame, final AbsoluteDate date, final double mu)
    throws IllegalArgumentException {
        super(frame, date, mu);

        if (a * (1 - e) < 0) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, a, e);
        }

        this.a    =    a;
        this.e    =    e;
        this.i    =    i;
        this.pa   =   pa;
        this.raan = raan;

        switch (type) {
        case MEAN_ANOMALY :
            this.v = (a < 0) ?
                     hyperbolicEccentricToTrue(meanToHyperbolicEccentric(anomaly)) :
                     ellipticEccentricToTrue(meanToEllipticEccentric(anomaly));
            break;
        case ECCENTRIC_ANOMALY :
            this.v = (a < 0) ?
                     hyperbolicEccentricToTrue(anomaly) :
                     ellipticEccentricToTrue(anomaly);
            break;
        case TRUE_ANOMALY :
            this.v = anomaly;
            break;
        default :
            this.v = Double.NaN;
            throw OrekitException.createIllegalArgumentException(
                  OrekitMessages.ANGLE_TYPE_NOT_SUPPORTED,
                  "MEAN_ANOMALY", "ECCENTRIC_ANOMALY", "TRUE_ANOMALY");

        }
    }

    /** Constructor from cartesian parameters.
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public KeplerianOrbit(final PVCoordinates pvCoordinates,
                          final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, date, mu);

        // compute inclination
        final Vector3D momentum = pvCoordinates.getMomentum();
        final double m2 = Vector3D.dotProduct(momentum, momentum);
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        final double n2 = Vector3D.dotProduct(node, node);
        // the following comparison with 0 IS REALLY numerically justified and stable
        raan = (n2 == 0) ? 0 : FastMath.atan2(node.getY(), node.getX());

        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final Vector3D pvP     = pvCoordinates.getPosition();
        final Vector3D pvV     = pvCoordinates.getVelocity();
        final double   r       = pvP.getNorm();
        final double   V2      = pvV.getNormSq();
        final double   rV2OnMu = r * V2 / mu;

        // compute semi-major axis (will be negative for hyperbolic orbits)
        a = r / (2 - rV2OnMu);

        // compute eccentricity (will be larger than 1 for hyperbolic orbits)
        final double muA = mu * a;
        e = FastMath.sqrt(1 - m2 / muA);

        // compute true anomaly
        if (a > 0) {
            if (e < E_CIRC) {
                // circular orbit
                v = 0;
            } else {
                // elliptic orbit
                final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(muA);
                final double eCE = rV2OnMu - 1;
                v = ellipticEccentricToTrue(FastMath.atan2(eSE, eCE));
            }
        } else {
            // hyperbolic orbit
            final double eSH = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(-muA);
            final double eCH = rV2OnMu - 1;
            v = hyperbolicEccentricToTrue(FastMath.log((eCH + eSH) / (eCH - eSH)) / 2);
        }

        // compute perigee argument
        final double px = Vector3D.dotProduct(pvP, node);
        final double py = Vector3D.dotProduct(pvP, Vector3D.crossProduct(momentum, node)) / FastMath.sqrt(m2);
        pa = FastMath.atan2(py, px) - v;

    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public KeplerianOrbit(final Orbit op) {
        this(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public double getA() {
        return a;
    }

    /** Get the eccentricity.
     * @return eccentricity
     */
    public double getE() {
        return e;
    }

    /** Get the inclination.
     * @return inclination (rad)
     */
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
    public double getAnomaly(PositionAngle type) {
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
            final double t = FastMath.sqrt((e - 1) / (e + 1)) * FastMath.tan(v / 2);
            return FastMath.log((1 + t) / (1 - t));
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
            double f;
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
     * a naive initialization and classical Halley method for iterations.
     * </p>
     * @param M mean anomaly (rad)
     * @return v the true anomaly
     */
    private double meanToHyperbolicEccentric(final double M) {

        // resolution of hyperbolic Kepler equation for keplerian parameters
        double H     = -M;
        double shift = 0.0;
        double HpM   = 0.0;
        int    iter  = 0;
        do {
            final double f2 = e * FastMath.sinh(H);
            final double f1 = e * FastMath.cosh(H) - 1;
            final double f0 = f2 - HpM;

            final double f12 = 2 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            HpM -= shift;
            H    = HpM - M;

        } while ((++iter < 50) && (FastMath.abs(shift) > 1.0e-12));

        return H;

    }

    /** Get the first component of the eccentricity vector.
     * @return first component of the eccentricity vector
     */
    public double getEquinoctialEx() {
        return  e * FastMath.cos(pa + raan);
    }

    /** Get the second component of the eccentricity vector.
     * @return second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        return  e * FastMath.sin(pa + raan);
    }

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector.
     */
    public double getHx() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return  FastMath.cos(raan) * FastMath.tan(i / 2);
    }

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector.
     */
    public double getHy() {
        // Check for equatorial retrograde orbit
        if (FastMath.abs(i - FastMath.PI) < 1.0e-10) {
            return Double.NaN;
        }
        return  FastMath.sin(raan) * FastMath.tan(i / 2);
    }

    /** Get the true latitude argument.
     * @return true latitude argument (rad)
     */
    public double getLv() {
        return pa + raan + v;
    }

    /** Get the eccentric latitude argument.
     * @return eccentric latitude argument.(rad)
     */
    public double getLE() {
        return pa + raan + getEccentricAnomaly();
    }

    /** Get the mean latitude argument.
     * @return mean latitude argument.(rad)
     */
    public double getLM() {
        return pa + raan + getMeanAnomaly();
    }

    /** {@inheritDoc} */
    protected PVCoordinates initPVCoordinates() {

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
     * @param q unit vector in the orbital plane in quadrature with q
     * @return computed position/velocity coordinates
     */
    private PVCoordinates initPVCoordinatesElliptical(final Vector3D p, final Vector3D q) {

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

        return new PVCoordinates(new Vector3D(x, p, y, q), new Vector3D(xDot, p, yDot, q));

    }

    /** Initialize the position/velocity coordinates, hyperbolic case.
     * @param p unit vector in the orbital plane pointing towards perigee
     * @param q unit vector in the orbital plane in quadrature with q
     * @return computed position/velocity coordinates
     */
    private PVCoordinates initPVCoordinatesHyperbolic(final Vector3D p, final Vector3D q) {

        // hyperbolic eccentric anomaly
        final double h      = getEccentricAnomaly();
        final double cH     = FastMath.cosh(h);
        final double sH     = FastMath.sinh(h);
        final double sE2m1  = FastMath.sqrt((e - 1) * (e + 1));

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cH - e);
        final double y      = -a * sE2m1 * sH;
        final double factor = FastMath.sqrt(getMu() / -a) / (e * cH - 1);
        final double xDot   = -factor * sH;
        final double yDot   =  factor * sE2m1 * cH;

        return new PVCoordinates(new Vector3D(x, p, y, q), new Vector3D(xDot, p, yDot, q));

    }

    /** {@inheritDoc} */
    public KeplerianOrbit shiftedBy(final double dt) {
        return new KeplerianOrbit(a, e, i, pa, raan,
                                  getMeanAnomaly() + getKeplerianMeanMotion() * dt,
                                  PositionAngle.MEAN, getFrame(), getDate().shiftedBy(dt), getMu());
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
                                  append("; lv: ").append(FastMath.toDegrees(v)).
                                  append(";}").toString();
    }

}
