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
 * This class handles equinoctial orbital parameters.

 * <p>
 * The parameters used internally are the equinoctial elements defined as follows:
 *   <pre>
 *     a
 *     ex = e cos(&omega; + &Omega;)
 *     ey = e sin(&omega; + &Omega;)
 *     hx = tan(i/2) cos(&Omega;)
 *     hy = tan(i/2) sin(&Omega;)
 *     lv = v + &omega; + &Omega;
 *   </pre>
 * where &omega; stands for the Perigee Argument and &Omega; stands for the
 * Right Ascension of the Ascending Node.
 * </p>
 * <p>
 * The instance <code>EquinoctialOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see     Orbit
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class EquinoctialOrbit extends Orbit {

    /** Identifier for mean latitude argument. */
    public static final int MEAN_LATITUDE_ARGUMENT = 0;

    /** Identifier for eccentric latitude argument. */
    public static final int ECCENTRIC_LATITUDE_ARGUMENT = 1;

    /** Identifier for true latitude argument. */
    public static final int TRUE_LATITUDE_ARGUMENT = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = -1779638201767656602L;

    /** Semi-major axis (m). */
    private final double a;

    /** First component of the eccentricity vector. */
    private final double ex;

    /** Second component of the eccentricity vector. */
    private final double ey;

    /** First component of the inclination vector. */
    private final double hx;

    /** Second component of the inclination vector. */
    private final double hy;

    /** True latitude argument (rad). */
    private final double lv;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(&omega; + &Omega;), first component of eccentricity vector
     * @param ey e sin(&omega; + &Omega;), second component of eccentricity vector
     * @param hx tan(i/2) cos(&Omega;), first component of inclination vector
     * @param hy tan(i/2) sin(&Omega;), second component of inclination vector
     * @param l  an + &omega; + &Omega;, mean, eccentric or true latitude argument (rad)
     * @param type type of latitude argument, must be one of {@link #MEAN_LATITUDE_ARGUMENT},
     * {@link #ECCENTRIC_LATITUDE_ARGUMENT} or  {@link #TRUE_LATITUDE_ARGUMENT}
     * @param frame the frame in which the parameters are defined
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if the longitude argument type is not
     * one of {@link #MEAN_LATITUDE_ARGUMENT}, @link #ECCENTRIC_LATITUDE_ARGUMENT}
     * or  {@link #TRUE_LATITUDE_ARGUMENT}
     * @see #MEAN_LATITUDE_ARGUMENT
     * @see #ECCENTRIC_LATITUDE_ARGUMENT
     * @see #TRUE_LATITUDE_ARGUMENT
     */
    public EquinoctialOrbit(final double a, final double ex, final double ey,
                            final double hx, final double hy,
                            final double l, final int type,
                            final Frame frame, final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        this.a  =  a;
        this.ex = ex;
        this.ey = ey;
        this.hx = hx;
        this.hy = hy;

        switch (type) {
        case MEAN_LATITUDE_ARGUMENT :
            this.lv = computeLM(l);
            break;
        case ECCENTRIC_LATITUDE_ARGUMENT :
            this.lv = computeLE(l);
            break;
        case TRUE_LATITUDE_ARGUMENT :
            this.lv = l;
            break;
        default :
            this.lv = Double.NaN;
            throw OrekitException.createIllegalArgumentException("angle type not supported, supported angles:" +
                                                                 " {0}, {1} and {2}",
                                                                 new Object[] {
                                                                     "MEAN_LATITUDE_ARGUMENT",
                                                                     "ECCENTRIC_LATITUDE_ARGUMENT",
                                                                     "TRUE_LATITUDE_ARGUMENT"
                                                                 });
        }

    }

    /** Constructor from cartesian parameters.
     * @param pvCoordinates the position end velocity
     * @param frame the frame in which are defined the {@link PVCoordinates}
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public EquinoctialOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                                 final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);

        //  compute semi-major axis
        final Vector3D pvP = pvCoordinates.getPosition();
        final Vector3D pvV = pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double V2 = Vector3D.dotProduct(pvV, pvV);
        final double rV2OnMu = r * V2 / mu;
        a = r / (2 - rV2OnMu);

        // compute inclination vector
        final Vector3D w = Vector3D.crossProduct(pvP, pvV).normalize();
        final double d = 1.0 / (1 + w.getZ());
        hx = -d * w.getY();
        hy =  d * w.getX();

        // compute true latitude argument
        final Vector3D p = pvP;
        final double cLv = (p.getX() - d * p.getZ() * w.getX()) / r;
        final double sLv = (p.getY() - d * p.getZ() * w.getY()) / r;
        lv = Math.atan2(sLv, cLv);

        // compute eccentricity vector
        final double eSE = Vector3D.dotProduct(p, pvV) / Math.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e2  = eCE * eCE + eSE * eSE;
        final double f   = eCE - e2;
        final double g   = Math.sqrt(1 - e2) * eSE;
        ex = a * (f * cLv + g * sLv) / r;
        ey = a * (f * sLv - g * cLv) / r;
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public EquinoctialOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        a  = op.getA();
        ex = op.getEquinoctialEx();
        ey = op.getEquinoctialEy();
        hx = op.getHx();
        hy = op.getHy();
        lv = op.getLv();
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public double getA() {
        return a;
    }

    /** Get the first component of the eccentricity vector.
     * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
     */
    public double getEquinoctialEx() {
        return ex;
    }

    /** Get the second component of the eccentricity vector.
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        return ey;
    }

    /** Get the first component of the inclination vector.
     * @return tan(i/2) cos(&Omega;), first component of the inclination vector
     */
    public double getHx() {
        return hx;
    }

    /** Get the second component of the inclination vector.
     * @return tan(i/2) sin(&Omega;), second component of the inclination vector
     */
    public double getHy() {
        return hy;
    }

    /** Get the true latitude argument.
     * @return v + &omega; + &Omega; true latitude argument (rad)
     */
    public double getLv() {
        return lv;
    }

    /** Get the eccentric latitude argument.
     * @return E + &omega; + &Omega; eccentric latitude argument (rad)
     */
    public double getLE() {
        final double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
        final double cosLv   = Math.cos(lv);
        final double sinLv   = Math.sin(lv);
        final double num     = ey * cosLv - ex * sinLv;
        final double den     = epsilon + 1 + ex * cosLv + ey * sinLv;
        return lv + 2 * Math.atan(num / den);
    }

    /** Computes the eccentric latitude argument.
     * @param lE = E + &omega; + &Omega; eccentric latitude argument (rad)
     * @return the true latitude argument
     */
    private double computeLE(final double lE) {
        final double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
        final double cosLE   = Math.cos(lE);
        final double sinLE   = Math.sin(lE);
        final double num = ex * sinLE - ey * cosLE;
        final double den = epsilon + 1 - ex * cosLE - ey * sinLE;
        return lE + 2 * Math.atan(num / den);
    }

    /** Get the mean latitude argument.
     * @return M + &omega; + &Omega; mean latitude argument (rad)
     */
    public double getLM() {
        final double lE = getLE();
        return lE - ex * Math.sin(lE) + ey * Math.cos(lE);
    }

    /** Computes the mean latitude argument.
     * @param lM = M + &omega; + &Omega; mean latitude argument (rad)
     * @return the true latitude argument
     */
    private double computeLM(final double lM) {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        double lE = lM;
        double shift = 0.0;
        double lEmlM = 0.0;
        double cosLE = Math.cos(lE);
        double sinLE = Math.sin(lE);
        int iter = 0;
        do {
            final double f2 = ex * sinLE - ey * cosLE;
            final double f1 = 1.0 - ex * cosLE - ey * sinLE;
            final double f0 = lEmlM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            lEmlM -= shift;
            lE     = lM + lEmlM;
            cosLE  = Math.cos(lE);
            sinLE  = Math.sin(lE);

        } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

        return computeLE(lE); // which set the lv parameter

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
        return 2 * Math.atan(Math.sqrt(hx * hx + hy * hy));
    }

    /**  Returns a string representation of this equinoctial parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("equinoctial parameters: ").append('{').
                                  append("a: ").append(a).
                                  append("; ex: ").append(ex).append("; ey: ").append(ey).
                                  append("; hx: ").append(hx).append("; hy: ").append(hy).
                                  append("; lv: ").append(Math.toDegrees(lv)).
                                  append(";}").toString();
    }

}
