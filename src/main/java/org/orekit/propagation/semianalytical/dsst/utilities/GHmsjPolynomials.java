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

import org.hipparchus.util.FastMath;

/** Compute the G<sub>ms</sub><sup>j</sup> and the H<sub>ms</sub><sup>j</sup>
 *  polynomials in the equinoctial elements h, k and the direction cosines α and β
 *  and their partial derivatives with respect to k, h, α and β.
 *  <p>
 *  The expressions used are equations 2.7.5-(1)(2) from the Danielson paper.
 *  </p>
 *  @author Romain Di Costanzo
 */
public class GHmsjPolynomials {

    /** C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient.
     * (k, h) are the (x, y) component of the eccentricity vector in equinoctial elements
     */
    private final CjSjCoefficient cjsjKH;

    /** C<sub>j</sub>(α, β), S<sub>j</sub>(α, β) coefficient.
     * (α, β) are the direction cosines
     */
    private final CjSjCoefficient cjsjAB;

    /** Is the orbit represented as a retrograde orbit.
     *  I = -1 if yes, +1 otherwise.
     */
    private int                   I;

    /** Create a set of G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials.
     *  @param k X component of the eccentricity vector
     *  @param h Y component of the eccentricity vector
     *  @param alpha direction cosine α
     *  @param beta direction cosine β
     *  @param retroFactor -1 if the orbit is represented as retrograde, +1 otherwise
     **/
    public GHmsjPolynomials(final double k, final double h,
                            final double alpha, final double beta,
                            final int retroFactor) {
        this.cjsjKH = new CjSjCoefficient(k, h);
        this.cjsjAB = new CjSjCoefficient(alpha, beta);
        this.I = retroFactor;
    }

    /** Get the G<sub>ms</sub><sup>j</sup> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return the G<sub>ms</sub><sup>j</sup>
     */
    public double getGmsj(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double gms = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            gms = cjsjKH.getCj(sMj) * cjsjAB.getCj(mMis) -
                  I * sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getSj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            gms = cjsjKH.getCj(sMj) * cjsjAB.getCj(sMim) +
                  sgn(s - j) * sgn(s - m) * cjsjKH.getSj(sMj) * cjsjAB.getSj(sMim);
        }
        return gms;
    }

    /** Get the H<sub>ms</sub><sup>j</sup> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return the H<sub>ms</sub><sup>j</sup>
     */
    public double getHmsj(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double hms = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            hms = I * cjsjKH.getCj(sMj) * cjsjAB.getSj(mMis) + sgn(s - j) *
                  cjsjKH.getSj(sMj) * cjsjAB.getCj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            hms = -sgn(s - m) * cjsjKH.getCj(sMj) * cjsjAB.getSj(sMim) +
                   sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getCj(sMim);
        }
        return hms;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public double getdGmsdk(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double dGmsdk = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdk = cjsjKH.getDcjDk(sMj) * cjsjAB.getCj(mMis) -
                   I * sgn(s - j) * cjsjKH.getDsjDk(sMj) * cjsjAB.getSj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdk = cjsjKH.getDcjDk(sMj) * cjsjAB.getCj(sMim) +
                    sgn(s - j) * sgn(s - m) * cjsjKH.getDsjDk(sMj) * cjsjAB.getSj(sMim);
        }
        return dGmsdk;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public double getdGmsdh(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double dGmsdh = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdh = cjsjKH.getDcjDh(sMj) * cjsjAB.getCj(mMis) -
                    I * sgn(s - j) * cjsjKH.getDsjDh(sMj) * cjsjAB.getSj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdh = cjsjKH.getDcjDh(sMj) * cjsjAB.getCj(sMim) +
                    sgn(s - j) * sgn(s - m) * cjsjKH.getDsjDh(sMj) * cjsjAB.getSj(sMim);
        }
        return dGmsdh;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>α</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>α</sub>
     */
    public double getdGmsdAlpha(final int m, final int s, final int j) {
        final int sMj  = FastMath.abs(s - j);
        double dGmsdAl = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdAl = cjsjKH.getCj(sMj) * cjsjAB.getDcjDk(mMis) -
                   I * sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDk(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdAl = cjsjKH.getCj(sMj) * cjsjAB.getDcjDk(sMim) +
                    sgn(s - j) * sgn(s - m) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDk(sMim);
        }
        return dGmsdAl;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>β</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>β</sub>
     */
    public double getdGmsdBeta(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double dGmsdBe = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdBe = cjsjKH.getCj(sMj) * cjsjAB.getDcjDh(mMis) -
                    I * sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDh(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdBe = cjsjKH.getCj(sMj) * cjsjAB.getDcjDh(sMim) +
                    sgn(s - j) * sgn(s - m) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDh(sMim);
        }
        return dGmsdBe;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public double getdHmsdk(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double dHmsdk = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdk = I * cjsjKH.getDcjDk(sMj) * cjsjAB.getSj(mMis) +
                    sgn(s - j) * cjsjKH.getDsjDk(sMj) * cjsjAB.getCj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdk = -sgn(s - m) * cjsjKH.getDcjDk(sMj) * cjsjAB.getSj(sMim) +
                    sgn(s - j) * cjsjKH.getDsjDk(sMj) * cjsjAB.getCj(sMim);
        }
        return dHmsdk;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public double getdHmsdh(final int m,  final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double dHmsdh = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdh = I * cjsjKH.getDcjDh(sMj) * cjsjAB.getSj(mMis) +
                    sgn(s - j) * cjsjKH.getDsjDh(sMj) * cjsjAB.getCj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdh = -sgn(s - m) * cjsjKH.getDcjDh(sMj) * cjsjAB.getSj(sMim) +
                    sgn(s - j) * cjsjKH.getDsjDh(sMj) * cjsjAB.getCj(sMim);
        }
        return dHmsdh;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>α</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>α</sub>
     */
    public double getdHmsdAlpha(final int m, final int s, final int j) {
        final int sMj  = FastMath.abs(s - j);
        double dHmsdAl = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdAl = I * cjsjKH.getCj(sMj) * cjsjAB.getDsjDk(mMis) +
                    sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDcjDk(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdAl = -sgn(s - m) * cjsjKH.getCj(sMj) * cjsjAB.getDsjDk(sMim) +
                    sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDcjDk(sMim);
        }
        return dHmsdAl;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>β</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>β</sub>
     */
    public double getdHmsdBeta(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        double dHmsdBe = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdBe = I * cjsjKH.getCj(sMj) * cjsjAB.getDsjDh(mMis) +
                   sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDcjDh(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdBe = -sgn(s - m) * cjsjKH.getCj(sMj) * cjsjAB.getDsjDh(sMim) +
                    sgn(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDcjDh(sMim);
        }
        return dHmsdBe;
    }

    /** Get the sign of an integer.
     *  @param i number on which evaluation is done
     *  @return -1 or +1 depending on sign of i
     */
    private int sgn(final int i) {
        return (i < 0) ? -1 : 1;
    }
}
