/* Copyright 2002-2008 CS Communication & Systèmes
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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/**
 * This class handles circular orbital parameters.

 * <p>
 * The parameters used internally are the circular elements defined as follows:
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
 * The instance <code>CircularOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see     Orbit
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
    private static final long serialVersionUID = -5031200932453701026L;

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
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if the longitude argument type is not
     * one of {@link #MEAN_LONGITUDE_ARGUMENT}, @link #ECCENTRIC_LONGITUDE_ARGUMENT}
     * or  {@link #TRUE_LONGITUDE_ARGUMENT}
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
            this.alphaV = computeAlphaM(alpha);
            break;
        case ECCENTRIC_LONGITUDE_ARGUMENT :
            this.alphaV = computeAlphaE(alpha);
            break;
        case TRUE_LONGITUDE_ARGUMENT :
            this.alphaV = alpha;
            break;
        default :
            this.alphaV = Double.NaN;
            throw OrekitException.createIllegalArgumentException("angle type not supported, supported angles:" +
                                                                 " {0}, {1} and {2}",
                                                                 new Object[] {
                                                                     "MEAN_LONGITUDE_ARGUMENT",
                                                                     "ECCENTRIC_LONGITUDE_ARGUMENT",
                                                                     "TRUE_LONGITUDE_ARGUMENT"
                                                                 });
        }

    }

    /** Constructor from cartesian parameters.
     * @param pvCoordinates the {@link PVCoordinates} in inertial frame
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public CircularOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                              final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);

        // compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final double r  = pvP.getNorm();
        final double V2 = Vector3D.dotProduct(pvV, pvV);
        final double rV2OnMu = r * V2 / mu;
        a = r / (2 - rV2OnMu);

        // compute inclination
        final Vector3D momentum = Vector3D.crossProduct(pvP, pvV);
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node     = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        final double   n2       = Vector3D.dotProduct(node, node);
        // the following comparison with 0 IS REALLY numerically justified and stable
        raan = (n2 == 0) ? 0 : Math.atan2(node.getY(), node.getX());

        // 2D-coordinates in the canonical frame
        final double cosRaan = Math.cos(raan);
        final double sinRaan = Math.sin(raan);
        final double cosI    = Math.cos(i);
        final double sinI    = Math.sin(i);
        final Vector3D rVec  = new Vector3D(cosRaan, Math.sin(raan), 0);
        final Vector3D sVec  = new Vector3D(-cosI * sinRaan, cosI * cosRaan, sinI);
        final double x2      = Vector3D.dotProduct(pvP, rVec) / a;
        final double y2      = Vector3D.dotProduct(pvP, sVec) / a;

        // compute eccentricity vector
        final double eSE    = Vector3D.dotProduct(pvP, pvV) / Math.sqrt(mu * a);
        final double eCE    = rV2OnMu - 1;
        final double e2     = eCE * eCE + eSE * eSE;
        final double f      = eCE - e2;
        final double g      = Math.sqrt(1 - e2) * eSE;
        final double aOnR   = a / r;
        final double a2OnR2 = aOnR * aOnR;
        ex = a2OnR2 * (f * x2 + g * y2);
        ey = a2OnR2 * (f * y2 - g * x2);

        // compute longitude argument
        final double beta = 1 / (1 + Math.sqrt(1 - ex * ex - ey * ey));
        alphaV = computeAlphaE(Math.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE * beta * ey));
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public CircularOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a    = op.getA();
        i    = op.getI();
        raan = Math.atan2(op.getHy(), op.getHx());
        final double cosRaan = Math.cos(raan);
        final double sinRaan = Math.sin(raan);
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
        return ex * Math.cos(raan) - ey * Math.sin(raan);
    }

    /** Get the second component of the equinoctial eccentricity vector.
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        return ey * Math.cos(raan) + ex * Math.sin(raan);
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
        return  Math.cos(raan) * Math.tan(i / 2);
    }

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector.
     */
    public double getHy() {
        return  Math.sin(raan) * Math.tan(i / 2);
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
        final double epsilon   = Math.sqrt(1 - ex * ex - ey * ey);
        final double cosAlphaV = Math.cos(alphaV);
        final double sinAlphaV = Math.sin(alphaV);
        return alphaV + 2 * Math.atan((ey * cosAlphaV - ex * sinAlphaV) /
                                      (epsilon + 1 + ex * cosAlphaV + ey * sinAlphaV));
    }

    /** Computes the eccentric longitude argument.
     * @param alphaE = E + &omega; eccentric longitude argument (rad)
     * @return the true longitude argument.
     */
    private double computeAlphaE(final double alphaE) {
        final double epsilon   = Math.sqrt(1 - ex * ex - ey * ey);
        final double cosAlphaE = Math.cos(alphaE);
        final double sinAlphaE = Math.sin(alphaE);
        return alphaE + 2 * Math.atan((ex * sinAlphaE - ey * cosAlphaE) /
                                      (epsilon + 1 - ex * cosAlphaE - ey * sinAlphaE));
    }

    /** Get the mean longitude argument.
     * @return M + &omega; mean longitude argument (rad)
     */
    public double getAlphaM() {
        final double alphaE = getAlphaE();
        return alphaE - ex * Math.sin(alphaE) + ey * Math.cos(alphaE);
    }

    /** Computes the mean longitude argument.
     * @param alphaM = M + &omega;  mean longitude argument (rad)
     * @return the true longitude argument.
     */
    private double computeAlphaM(final double alphaM) {
        // Generalization of Kepler equation to equinoctial parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        double alphaE = alphaM;
        double shift = 0.0;
        double alphaEMalphaM = 0.0;
        double cosLE = Math.cos(alphaE);
        double sinLE = Math.sin(alphaE);
        int iter = 0;
        do {
            final double f2 = ex * sinLE - ey * cosLE;
            final double f1 = 1.0 - ex * cosLE - ey * sinLE;
            final double f0 = alphaEMalphaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            alphaEMalphaM -= shift;
            alphaE         = alphaM + alphaEMalphaM;
            cosLE          = Math.cos(alphaE);
            sinLE          = Math.sin(alphaE);

        } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

        return computeAlphaE(alphaE); // which set the alphaV parameter

    }

    /** Get the eccentricity.
     * @return eccentricity
     */
    public double getE() {
        return Math.sqrt(ex * ex + ey * ey);
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

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("circular parameters: ").append('{').
                                  append("a: ").append(a).
                                  append(", ex: ").append(ex).append(", ey: ").append(ey).
                                  append(", i: ").append(i).
                                  append(", raan: ").append(raan).
                                  append(", alphaV: ").append(Math.toDegrees(alphaV)).
                                  append(";}").toString();
    }

}
