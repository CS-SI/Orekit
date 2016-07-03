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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;


/** Container class for common parameters used by all DSST forces.
 *  <p>
 *  Most of them are defined in Danielson paper at § 2.1.
 *  </p>
 *  @author Pascal Parraud
 */
public class AuxiliaryElements {

    /** Orbit date. */
    private final AbsoluteDate date;

    /** Orbit frame. */
    private final Frame frame;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Eccentricity. */
    private final double ecc;

    /** Keplerian mean motion. */
    private final double n;

    /** Keplerian period. */
    private final double period;

    /** Semi-major axis. */
    private final double sma;

    /** x component of eccentricity vector. */
    private final double k;

    /** y component of eccentricity vector. */
    private final double h;

    /** x component of inclination vector. */
    private final double q;

    /** y component of inclination vector. */
    private final double p;

    /** Mean longitude. */
    private final double lm;

    /** True longitude. */
    private final double lv;

    /** Eccentric longitude. */
    private final double le;

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private final int    I;

    /** A = sqrt(μ * a). */
    private final double A;

    /** B = sqrt(1 - h² - k²). */
    private final double B;

    /** C = 1 + p² + q². */
    private final double C;

    /** Equinoctial frame f vector. */
    private final Vector3D f;

    /** Equinoctial frame g vector. */
    private final Vector3D g;

    /** Equinoctial frame w vector. */
    private final Vector3D w;

    /** Direction cosine α. */
    private final double alpha;

    /** Direction cosine β. */
    private final double beta;

    /** Direction cosine γ. */
    private final double gamma;

    /** Simple constructor.
     * @param orbit related mean orbit for auxiliary elements
     * @param retrogradeFactor retrograde factor I [Eq. 2.1.2-(2)]
     */
    public AuxiliaryElements(final Orbit orbit, final int retrogradeFactor) {
        // Date of the orbit
        date = orbit.getDate();

        // Orbit definition frame
        frame = orbit.getFrame();

        // Central body attraction coefficient
        mu = orbit.getMu();

        // Eccentricity
        ecc = orbit.getE();

        // Keplerian mean motion
        n = orbit.getKeplerianMeanMotion();

        // Keplerian period
        period = orbit.getKeplerianPeriod();

        // Equinoctial elements [Eq. 2.1.2-(1)]
        sma = orbit.getA();
        k   = orbit.getEquinoctialEx();
        h   = orbit.getEquinoctialEy();
        q   = orbit.getHx();
        p   = orbit.getHy();
        lm  = MathUtils.normalizeAngle(orbit.getLM(), FastMath.PI);
        lv  = MathUtils.normalizeAngle(orbit.getLv(), FastMath.PI);
        le  = MathUtils.normalizeAngle(orbit.getLE(), FastMath.PI);

        // Retrograde factor [Eq. 2.1.2-(2)]
        I = retrogradeFactor;

        final double k2 = k * k;
        final double h2 = h * h;
        final double q2 = q * q;
        final double p2 = p * p;

        // A, B, C parameters [Eq. 2.1.6-(1)]
        A = FastMath.sqrt(mu * sma);
        B = FastMath.sqrt(1 - k2 - h2);
        C = 1 + q2 + p2;

        // Equinoctial reference frame [Eq. 2.1.4-(1)]
        final double ooC = 1. / C;
        final double px2 = 2. * p;
        final double qx2 = 2. * q;
        final double pq2 = px2 * q;
        f = new Vector3D(ooC, new Vector3D(1. - p2 + q2, pq2, -px2 * I));
        g = new Vector3D(ooC, new Vector3D(pq2 * I, (1. + p2 - q2) * I, qx2));
        w = new Vector3D(ooC, new Vector3D(px2, -qx2, (1. - p2 - q2) * I));

        // Direction cosines for central body [Eq. 2.1.9-(1)]
        alpha = f.getZ();
        beta  = g.getZ();
        gamma = w.getZ();
    }

    /** Get the date of the orbit.
     * @return the date
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the definition frame of the orbit.
     * @return the definition frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the central body attraction coefficient.
     * @return μ
     */
    public double getMu() {
        return mu;
    }

    /** Get the eccentricity.
     * @return ecc
     */
    public double getEcc() {
        return ecc;
    }

    /** Get the Keplerian mean motion.
     * @return n
     */
    public double getMeanMotion() {
        return n;
    }

    /** Get the Keplerian period.
     * @return period
     */
    public double getKeplerianPeriod() {
        return period;
    }

    /** Get the semi-major axis.
     * @return the semi-major axis a
     */
    public double getSma() {
        return sma;
    }

    /** Get the x component of eccentricity vector.
     * <p>
     * This element called k in DSST corresponds to ex
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return k
     */
    public double getK() {
        return k;
    }

    /** Get the y component of eccentricity vector.
     * <p>
     * This element called h in DSST corresponds to ey
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return h
     */
    public double getH() {
        return h;
    }

    /** Get the x component of inclination vector.
     * <p>
     * This element called q in DSST corresponds to hx
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return q
     */
    public double getQ() {
        return q;
    }

    /** Get the y component of inclination vector.
     * <p>
     * This element called p in DSST corresponds to hy
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return p
     */
    public double getP() {
        return p;
    }

    /** Get the mean longitude.
     * @return lm
     */
    public double getLM() {
        return lm;
    }

    /** Get the true longitude.
     * @return lv
     */
    public double getLv() {
        return lv;
    }

    /** Get the eccentric longitude.
     * @return lf
     */
    public double getLf() {
        return le;
    }

    /** Get the retrograde factor.
     * @return the retrograde factor I
     */
    public int getRetrogradeFactor() {
        return I;
    }

    /** Get A = sqrt(μ * a).
     * @return A
     */
    public double getA() {
        return A;
    }

    /** Get B = sqrt(1 - e²).
     * @return B
     */
    public double getB() {
        return B;
    }

    /** Get C = 1 + p² + q².
     * @return C
     */
    public double getC() {
        return C;
    }

    /** Get equinoctial frame vector f.
     * @return f vector
     */
    public Vector3D getVectorF() {
        return f;
    }

    /** Get equinoctial frame vector g.
     * @return g vector
     */
    public Vector3D getVectorG() {
        return g;
    }

    /** Get equinoctial frame vector w.
     * @return w vector
     */
    public Vector3D getVectorW() {
        return w;
    }

    /** Get direction cosine α for central body.
     * @return α
     */
    public double getAlpha() {
        return alpha;
    }

    /** Get direction cosine β for central body.
     * @return β
     */
    public double getBeta() {
        return beta;
    }

    /** Get direction cosine γ for central body.
     * @return γ
     */
    public double getGamma() {
        return gamma;
    }

}
