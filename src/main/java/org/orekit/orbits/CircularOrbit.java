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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/**
 * This class handles circular orbital parameters.

 * <p>
 * The parameters used internally are the circular elements which can be
 * related to keplerian elements as follows:
 *   <ul>
 *     <li>a</li>
 *     <li>e<sub>x</sub> = e cos(&omega;)</li>
 *     <li>e<sub>y</sub> = e sin(&omega;)</li>
 *     <li>i</li>
 *     <li>&Omega;</li>
 *     <li>&alpha;<sub>v</sub> = v + &omega;</li>
 *   </ul>
 * where &Omega; stands for the Right Ascension of the Ascending Node and
 * &alpha;<sub>v</sub> stands for the true longitude argument
 * </p>
 * <p>
 * The conversion equations from and to keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is circular (but not equatorial), the circular
 * parameters are still unambiguously defined whereas some keplerian elements
 * (more precisely &omega; and &Omega;) become ambiguous. When orbit is equatorial,
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
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class CircularOrbit
    extends Orbit {

    /** Identifier for mean longitude argument. */
    public static final int MEAN_LONGITUDE_ARGUMENT = 0;

    /** Identifier for eccentric longitude argument. */
    public static final int ECCENTRIC_LONGITUDE_ARGUMENT = 1;

    /** Identifier for true longitude argument. */
    public static final int TRUE_LONGITUDE_ARGUMENT = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 5042463409964008691L;

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

    /** True longitude argument (rad). */
    private final double alphaV;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(&omega;), first component of circular eccentricity vector
     * @param ey e sin(&omega;), second component of circular eccentricity vector
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (&Omega;, rad)
     * @param alpha  an + &omega;, mean, eccentric or true longitude argument (rad)
     * @param type type of longitude argument, must be one of {@link #MEAN_LONGITUDE_ARGUMENT},
     * {@link #ECCENTRIC_LONGITUDE_ARGUMENT} or  {@link #TRUE_LONGITUDE_ARGUMENT}
     * @param frame the frame in which are defined the parameters
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if the longitude argument type is not
     * one of {@link #MEAN_LONGITUDE_ARGUMENT}, {@link #ECCENTRIC_LONGITUDE_ARGUMENT}
     * or {@link #TRUE_LONGITUDE_ARGUMENT} or if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     * @see #MEAN_LONGITUDE_ARGUMENT
     * @see #ECCENTRIC_LONGITUDE_ARGUMENT
     * @see #TRUE_LONGITUDE_ARGUMENT
     */
    public CircularOrbit(final double a, final double ex, final double ey,
                         final double i, final double raan,
                         final double alpha, final int type,
                         final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        this.a    =  a;
        this.ex   = ex;
        this.ey   = ey;
        this.i    = i;
        this.raan = raan;

        switch (type) {
        case MEAN_LONGITUDE_ARGUMENT :
            this.alphaV = eccentricToTrue(meanToEccentric(alpha));
            break;
        case ECCENTRIC_LONGITUDE_ARGUMENT :
            this.alphaV = eccentricToTrue(alpha);
            break;
        case TRUE_LONGITUDE_ARGUMENT :
            this.alphaV = alpha;
            break;
        default :
            this.alphaV = Double.NaN;
            throw OrekitException.createIllegalArgumentException(
                  OrekitMessages.ANGLE_TYPE_NOT_SUPPORTED,
                  "MEAN_LONGITUDE_ARGUMENT", "ECCENTRIC_LONGITUDE_ARGUMENT",
                  "TRUE_LONGITUDE_ARGUMENT");
        }

    }

    /** Constructor from cartesian parameters.
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CircularOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                              final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, date, mu);

        // compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final double r  = pvP.getNorm();
        final double V2 = pvV.getNormSq();
        final double rV2OnMu = r * V2 / mu;

        if (rV2OnMu > 2) {
            throw OrekitException.createIllegalArgumentException(
                  OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, getClass().getName());
        }

        a = r / (2 - rV2OnMu);

        // compute inclination
        final Vector3D momentum = pvCoordinates.getMomentum();
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node     = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        final double   n2       = Vector3D.dotProduct(node, node);
        // the following comparison with 0 IS REALLY numerically justified and stable
        raan = (n2 == 0) ? 0 : FastMath.atan2(node.getY(), node.getX());

        // 2D-coordinates in the canonical frame
        final double cosRaan = FastMath.cos(raan);
        final double sinRaan = FastMath.sin(raan);
        final double cosI    = FastMath.cos(i);
        final double sinI    = FastMath.sin(i);
        final double xP      = pvP.getX();
        final double yP      = pvP.getY();
        final double zP      = pvP.getZ();
        final double x2      = (xP * cosRaan + yP * sinRaan) / a;
        final double y2      = ((yP * cosRaan - xP * sinRaan) * cosI + zP * sinI) / a;

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

        // compute longitude argument
        final double beta = 1 / (1 + FastMath.sqrt(1 - ex * ex - ey * ey));
        alphaV = eccentricToTrue(FastMath.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE * beta * ey));
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public CircularOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a    = op.getA();
        i    = op.getI();
        raan = FastMath.atan2(op.getHy(), op.getHx());
        final double cosRaan = FastMath.cos(raan);
        final double sinRaan = FastMath.sin(raan);
        final double equiEx = op.getEquinoctialEx();
        final double equiEy = op.getEquinoctialEy();
        ex   = equiEx * cosRaan + equiEy * sinRaan;
        ey   = equiEy * cosRaan - equiEx * sinRaan;
        this.alphaV = op.getLv() - raan;
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public double getA() {
        return a;
    }

    /** Get the first component of the equinoctial eccentricity vector.
     * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
     */
    public double getEquinoctialEx() {
        return ex * FastMath.cos(raan) - ey * FastMath.sin(raan);
    }

    /** Get the second component of the equinoctial eccentricity vector.
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        return ey * FastMath.cos(raan) + ex * FastMath.sin(raan);
    }

    /** Get the first component of the circular eccentricity vector.
     * @return ex = e cos(&omega;), first component of the circular eccentricity vector
     */
    public double getCircularEx() {
        return ex;
    }

    /** Get the second component of the circular eccentricity vector.
     * @return ey = e sin(&omega;), second component of the circular eccentricity vector
     */
    public double getCircularEy() {
        return ey;
    }

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector.
     */
    public double getHx() {
        return  FastMath.cos(raan) * FastMath.tan(i / 2);
    }

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector.
     */
    public double getHy() {
        return  FastMath.sin(raan) * FastMath.tan(i / 2);
    }

    /** Get the true longitude argument.
     * @return v + &omega; true longitude argument (rad)
     */
    public double getAlphaV() {
        return alphaV;
    }

    /** Get the eccentric longitude argument.
     * @return E + &omega; eccentric longitude argument (rad)
     */
    public double getAlphaE() {
        final double epsilon   = FastMath.sqrt(1 - ex * ex - ey * ey);
        final double cosAlphaV = FastMath.cos(alphaV);
        final double sinAlphaV = FastMath.sin(alphaV);
        return alphaV + 2 * FastMath.atan((ey * cosAlphaV - ex * sinAlphaV) /
                                      (epsilon + 1 + ex * cosAlphaV + ey * sinAlphaV));
    }

    /** Computes the true longitude argument from the eccentric longitude argument.
     * @param alphaE = E + &omega; eccentric longitude argument (rad)
     * @return the true longitude argument.
     */
    private double eccentricToTrue(final double alphaE) {
        final double epsilon   = FastMath.sqrt(1 - ex * ex - ey * ey);
        final double cosAlphaE = FastMath.cos(alphaE);
        final double sinAlphaE = FastMath.sin(alphaE);
        return alphaE + 2 * FastMath.atan((ex * sinAlphaE - ey * cosAlphaE) /
                                      (epsilon + 1 - ex * cosAlphaE - ey * sinAlphaE));
    }

    /** Get the mean longitude argument.
     * @return M + &omega; mean longitude argument (rad)
     */
    public double getAlphaM() {
        final double alphaE = getAlphaE();
        return alphaE - ex * FastMath.sin(alphaE) + ey * FastMath.cos(alphaE);
    }

    /** Computes the eccentric longitude argument from the mean longitude argument.
     * @param alphaM = M + &omega;  mean longitude argument (rad)
     * @return the eccentric longitude argument.
     */
    private double meanToEccentric(final double alphaM) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        double alphaE        = alphaM;
        double shift         = 0.0;
        double alphaEMalphaM = 0.0;
        double cosAlphaE     = FastMath.cos(alphaE);
        double sinAlphaE     = FastMath.sin(alphaE);
        int    iter          = 0;
        do {
            final double f2 = ex * sinAlphaE - ey * cosAlphaE;
            final double f1 = 1.0 - ex * cosAlphaE - ey * sinAlphaE;
            final double f0 = alphaEMalphaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            alphaEMalphaM -= shift;
            alphaE         = alphaM + alphaEMalphaM;
            cosAlphaE      = FastMath.cos(alphaE);
            sinAlphaE      = FastMath.sin(alphaE);

        } while ((++iter < 50) && (FastMath.abs(shift) > 1.0e-12));

        return alphaE;

    }

    /** Get the eccentricity.
     * @return eccentricity
     */
    public double getE() {
        return FastMath.sqrt(ex * ex + ey * ey);
    }

    /** Get the inclination.
     * @return inclination (rad)
     */
    public double getI() {
        return i;
    }

    /** Get the right ascension of the ascending node.
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return raan;
    }

    /** Get the true latitude argument.
     * @return true latitude argument (rad)
     */
    public double getLv() {
        return alphaV + raan;
    }

    /** Get the eccentric latitude argument.
     * @return eccentric latitude argument.(rad)
     */
    public double getLE() {
        return getAlphaE() + raan;
    }

    /** Get the mean latitude argument.
     * @return mean latitude argument.(rad)
     */
    public double getLM() {
        return getAlphaM() + raan;
    }

    /** {@inheritDoc} */
    protected PVCoordinates initPVCoordinates() {

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
        final double cLe    = FastMath.cos(lE);
        final double sLe    = FastMath.sin(lE);
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
        return new PVCoordinates(position, velocity);

    }

    /** {@inheritDoc} */
    public CircularOrbit shiftedBy(final double dt) {
        return new CircularOrbit(a, ex, ey, i, raan,
                                 getAlphaM() + getKeplerianMeanMotion() * dt,
                                 MEAN_LONGITUDE_ARGUMENT, getFrame(),
                                 getDate().shiftedBy(dt), getMu());
    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("circular parameters: ").append('{').
                                  append("a: ").append(a).
                                  append(", ex: ").append(ex).append(", ey: ").append(ey).
                                  append(", i: ").append(FastMath.toDegrees(i)).
                                  append(", raan: ").append(FastMath.toDegrees(raan)).
                                  append(", alphaV: ").append(FastMath.toDegrees(alphaV)).
                                  append(";}").toString();
    }

}
