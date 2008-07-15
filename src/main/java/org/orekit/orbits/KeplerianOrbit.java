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
 * This class handles keplerian orbital parameters.

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
 * The instance <code>KeplerianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see     Orbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class KeplerianOrbit extends Orbit {

    /** Identifier for mean anomaly. */
    public static final int MEAN_ANOMALY = 0;

    /** Identifier for eccentric anomaly. */
    public static final int ECCENTRIC_ANOMALY = 1;

    /** Identifier for true anomaly. */
    public static final int TRUE_ANOMALY = 2;

    /** Eccentricity threshold for near circular orbits.
     *  if e < E_CIRC : the orbit is considered circular
     */
    public static final double E_CIRC = 1.e-10;

    /** Serializable UID. */
    private static final long serialVersionUID = -8628129146897296527L;

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
     * @param a  semi-major axis (m)
     * @param e eccentricity
     * @param i inclination (rad)
     * @param pa perigee argument (&omega;, rad)
     * @param raan right ascension of ascending node (&Omega;, rad)
     * @param anomaly mean, eccentric or true anomaly (rad)
     * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
     * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
     * @param frame the frame in which the parameters are defined
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if the longitude argument type is not
     * one of {@link #MEAN_ANOMALY}, @link {@link #ECCENTRIC_ANOMALY}}
     * or  {@link #TRUE_ANOMALY}
     * @see #MEAN_ANOMALY
     * @see #ECCENTRIC_ANOMALY
     * @see #TRUE_ANOMALY
     */
    public KeplerianOrbit(final double a, final double e, final double i,
                          final double pa, final double raan,
                          final double anomaly, final int type,
                          final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        this.a    =    a;
        this.e    =    e;
        this.i    =    i;
        this.pa   =   pa;
        this.raan = raan;

        switch (type) {
        case MEAN_ANOMALY :
            this.v = computeMeanAnomaly(anomaly);
            break;
        case ECCENTRIC_ANOMALY :
            this.v = computeEccentricAnomaly(anomaly);
            break;
        case TRUE_ANOMALY :
            this.v = anomaly;
            break;
        default :
            this.v = Double.NaN;
            throw OrekitException.createIllegalArgumentException("angle type not supported, supported angles:" +
                                                                 " {0}, {1} and {2}",
                                                                 new Object[] {
                                                                     "MEAN_ANOMALY",
                                                                     "ECCENTRIC_ANOMALY",
                                                                     "TRUE_ANOMALY"
                                                                 });

        }
    }

    /** Constructor from cartesian parameters.
     * @param pvCoordinates the PVCoordinates of the satellite
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public KeplerianOrbit(final PVCoordinates pvCoordinates,
                          final Frame frame, final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);

        // compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double V2 = Vector3D.dotProduct(pvV, pvV);
        final double rV2OnMu = r * V2 / mu;
        a = r / (2 - rV2OnMu);

        // compute eccentricity
        final double muA = mu * a;
        final double eSE = Vector3D.dotProduct(pvP, pvV) / Math.sqrt(muA);
        final double eCE = rV2OnMu - 1;
        e = Math.sqrt(eSE * eSE + eCE * eCE);

        // compute inclination
        final Vector3D momentum =
            Vector3D.crossProduct(pvP, pvV);
        final double m2 = Vector3D.dotProduct(momentum, momentum);
        i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        final double n2 = Vector3D.dotProduct(node, node);
        // the following comparison with 0 IS REALLY numerically justified and stable
        raan = (n2 == 0) ? 0 : Math.atan2(node.getY(), node.getX());

        // compute true anomaly
        if (e < E_CIRC) {
            v = 0;
        } else {
            final double E = Math.atan2(eSE, eCE);
            final double k = 1 / (1 + Math.sqrt(m2 / muA));
            v = E + 2 * Math.atan(k * eSE / (1 - k * eCE));
        }

        // compute perigee argument
        final double cosRaan = Math.cos(raan);
        final double sinRaan = Math.sin(raan);
        final double px = cosRaan * pvP.getX() +
                          sinRaan * pvP.getY();
        final double py = Math.cos(i) * (cosRaan * pvP.getY() - sinRaan * pvP.getX()) +
                          Math.sin(i) * pvP.getZ();
        pa = Math.atan2(py, px) - v;
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public KeplerianOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a    = op.getA();
        e    = op.getE();
        i    = op.getI();
        raan = Math.atan2(op.getHy(), op.getHx());
        pa   = Math.atan2(op.getEquinoctialEy(), op.getEquinoctialEx()) - raan;
        v    = op.getLv() - (pa + raan);
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
        final double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
        return v - 2 * Math.atan(beta * Math.sin(v) / (1 + beta * Math.cos(v)));
    }

    /** Computes the eccentric anomaly.
     * @param E eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private double computeEccentricAnomaly (final double E) {
        final double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
        return E + 2 * Math.atan(beta * Math.sin(E) / (1 - beta * Math.cos(E)));
    }

    /** Get the mean anomaly.
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {
        final double E = getEccentricAnomaly();
        return E - e * Math.sin(E);
    }

    /** Computes the mean anomaly.
     * @param M mean anomaly (rad)
     * @return v the true anomaly
     */
    private double computeMeanAnomaly (final double M) {

        // resolution of Kepler equation for keplerian parameters
        double E = M;
        double shift = 0.0;
        double EmM   = 0.0;
        int iter = 0;
        do {
            final double f2 = e * Math.sin(E);
            final double f1 = 1.0 - e * Math.cos(E);
            final double f0 = EmM - f2;

            final double f12 = 2 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            EmM -= shift;
            E    = M + EmM;

        } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

        return computeEccentricAnomaly(E);

    }

    /** Get the first component of the eccentricity vector.
     * @return first component of the eccentricity vector
     */
    public double getEquinoctialEx() {
        return  e * Math.cos(pa + raan);
    }

    /** Get the second component of the eccentricity vector.
     * @return second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        return  e * Math.sin(pa + raan);
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

    /**  Returns a string representation of this keplerian parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("keplerian parameters: ").append('{').
                                  append("a: ").append(a).
                                  append("; e: ").append(e).
                                  append("; i: ").append(Math.toDegrees(i)).
                                  append("; pa: ").append(Math.toDegrees(pa)).
                                  append("; raan: ").append(Math.toDegrees(raan)).
                                  append("; lv: ").append(Math.toDegrees(v)).
                                  append(";}").toString();
    }

}
