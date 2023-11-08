/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.time.FieldAbsoluteDate;

/** Container class for common parameters used by all DSST forces.
 *  <p>
 *  Most of them are defined in Danielson paper at § 2.1.
 *  </p>
 *  @author Bryan Cazabonne
 * @param <T> type of the field elements
 */
public class FieldAuxiliaryElements<T extends CalculusFieldElement<T>> {

    /** Orbit. */
    private final FieldOrbit<T> orbit;

    /** Orbit date. */
    private final FieldAbsoluteDate<T> date;

    /** Orbit frame. */
    private final Frame frame;

    /** Eccentricity. */
    private final T ecc;

    /** Keplerian mean motion. */
    private final T n;

    /** Keplerian period. */
    private final T period;

    /** Semi-major axis. */
    private final T sma;

    /** x component of eccentricity vector. */
    private final T k;

    /** y component of eccentricity vector. */
    private final T h;

    /** x component of inclination vector. */
    private final T q;

    /** y component of inclination vector. */
    private final T p;

    /** Mean longitude. */
    private final T lm;

    /** True longitude. */
    private final T lv;

    /** Eccentric longitude. */
    private final T le;

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

    /** B = sqrt(1 - h² - k²). */
    private final T B;

    /** C = 1 + p² + q². */
    private final T C;

    /** Equinoctial frame f vector. */
    private final FieldVector3D<T> f;

    /** Equinoctial frame g vector. */
    private final FieldVector3D<T> g;

    /** Equinoctial frame w vector. */
    private final FieldVector3D<T> w;

    /** Direction cosine α. */
    private final T alpha;

    /** Direction cosine β. */
    private final T beta;

    /** Direction cosine γ. */
    private final T gamma;

    /** Simple constructor.
     * @param orbit related mean orbit for auxiliary elements
     * @param retrogradeFactor retrograde factor I [Eq. 2.1.2-(2)]
     */
    public FieldAuxiliaryElements(final FieldOrbit<T> orbit, final int retrogradeFactor) {

        final T pi = orbit.getDate().getField().getZero().getPi();

        // Orbit
        this.orbit = orbit;

        // Date of the orbit
        date = orbit.getDate();

        // Orbit definition frame
        frame = orbit.getFrame();

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
        lm  = MathUtils.normalizeAngle(orbit.getLM(), pi);
        lv  = MathUtils.normalizeAngle(orbit.getLv(), pi);
        le  = MathUtils.normalizeAngle(orbit.getLE(), pi);

        // Retrograde factor [Eq. 2.1.2-(2)]
        I = retrogradeFactor;

        final T k2 = k.multiply(k);
        final T h2 = h.multiply(h);
        final T q2 = q.multiply(q);
        final T p2 = p.multiply(p);

        // A, B, C parameters [Eq. 2.1.6-(1)]
        B = FastMath.sqrt(k2.add(h2).negate().add(1.));
        C = q2.add(p2).add(1);

        // Equinoctial reference frame [Eq. 2.1.4-(1)]
        final T ooC = C.reciprocal();
        final T px2 = p.multiply(2.);
        final T qx2 = q.multiply(2.);
        final T pq2 = px2.multiply(q);
        f = new FieldVector3D<>(ooC, new FieldVector3D<>(p2.negate().add(1.).add(q2), pq2, px2.multiply(I).negate()));
        g = new FieldVector3D<>(ooC, new FieldVector3D<>(pq2.multiply(I), (p2.add(1.).subtract(q2)).multiply(I), qx2));
        w = new FieldVector3D<>(ooC, new FieldVector3D<>(px2, qx2.negate(), (p2.add(q2).negate().add(1.)).multiply(I)));

        // Direction cosines for central body [Eq. 2.1.9-(1)]
        alpha = (T) f.getZ();
        beta  = (T) g.getZ();
        gamma = (T) w.getZ();
    }

    /** Get the orbit.
     * @return the orbit
     */
    public FieldOrbit<T> getOrbit() {
        return orbit;
    }

    /** Get the date of the orbit.
     * @return the date
     */
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get the definition frame of the orbit.
     * @return the definition frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the eccentricity.
     * @return ecc
     */
    public T getEcc() {
        return ecc;
    }

    /** Get the Keplerian mean motion.
     * @return n
     */
    public T getMeanMotion() {
        return n;
    }

    /** Get the Keplerian period.
     * @return period
     */
    public T getKeplerianPeriod() {
        return period;
    }

    /** Get the semi-major axis.
     * @return the semi-major axis a
     */
    public T getSma() {
        return sma;
    }

    /** Get the x component of eccentricity vector.
     * <p>
     * This element called k in DSST corresponds to ex
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return k
     */
    public T getK() {
        return k;
    }

    /** Get the y component of eccentricity vector.
     * <p>
     * This element called h in DSST corresponds to ey
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return h
     */
    public T getH() {
        return h;
    }

    /** Get the x component of inclination vector.
     * <p>
     * This element called q in DSST corresponds to hx
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return q
     */
    public T getQ() {
        return q;
    }

    /** Get the y component of inclination vector.
     * <p>
     * This element called p in DSST corresponds to hy
     * for the {@link org.orekit.orbits.EquinoctialOrbit}
     * </p>
     * @return p
     */
    public T getP() {
        return p;
    }

    /** Get the mean longitude.
     * @return lm
     */
    public T getLM() {
        return lm;
    }

    /** Get the true longitude.
     * @return lv
     */
    public T getLv() {
        return lv;
    }

    /** Get the eccentric longitude.
     * @return le
     */
    public T getLe() {
        return le;
    }

    /** Get the retrograde factor.
     * @return the retrograde factor I
     */
    public int getRetrogradeFactor() {
        return I;
    }

    /** Get B = sqrt(1 - e²).
     * @return B
     */
    public T getB() {
        return B;
    }

    /** Get C = 1 + p² + q².
     * @return C
     */
    public T getC() {
        return C;
    }

    /** Get equinoctial frame vector f.
     * @return f vector
     */
    public FieldVector3D<T> getVectorF() {
        return f;
    }

    /** Get equinoctial frame vector g.
     * @return g vector
     */
    public FieldVector3D<T> getVectorG() {
        return g;
    }

    /** Get equinoctial frame vector w.
     * @return w vector
     */
    public FieldVector3D<T> getVectorW() {
        return w;
    }

    /** Get direction cosine α for central body.
     * @return α
     */
    public T getAlpha() {
        return alpha;
    }

    /** Get direction cosine β for central body.
     * @return β
     */
    public T getBeta() {
        return beta;
    }

    /** Get direction cosine γ for central body.
     * @return γ
     */
    public T getGamma() {
        return gamma;
    }

    /** Transforms the FieldAuxiliaryElements instance into an AuxiliaryElements instance.
     * @return the AuxiliaryElements instance
     * @since 11.3.3
     */
    public AuxiliaryElements toAuxiliaryElements() {
        return new AuxiliaryElements(orbit.toOrbit(), getRetrogradeFactor());
    }
}
