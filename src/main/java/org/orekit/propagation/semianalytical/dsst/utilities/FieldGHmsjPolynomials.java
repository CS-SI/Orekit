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

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;

/** Compute the G<sub>ms</sub><sup>j</sup> and the H<sub>ms</sub><sup>j</sup>
 *  polynomials in the equinoctial elements h, k and the direction cosines α and β
 *  and their partial derivatives with respect to k, h, α and β.
 *  <p>
 *  The expressions used are equations 2.7.5-(1)(2) from the Danielson paper.
 *  </p>
 *  @author Romain Di Costanzo
 *  @author Bryan Cazabonne (field translation)
 * @param <T> type of the field elements
 */
public class FieldGHmsjPolynomials <T extends CalculusFieldElement<T>> {
    /** C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient.
     * (k, h) are the (x, y) component of the eccentricity vector in equinoctial elements
     */
    private final FieldCjSjCoefficient<T> cjsjKH;

    /** C<sub>j</sub>(α, β), S<sub>j</sub>(α, β) coefficient.
     * (α, β) are the direction cosines
     */
    private final FieldCjSjCoefficient<T> cjsjAB;

    /** Is the orbit represented as a retrograde orbit.
     *  I = -1 if yes, +1 otherwise.
     */
    private int                   I;

    /** Zero for initialization. */
    private final T zero;

    /** Create a set of G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials.
     *  @param k X component of the eccentricity vector
     *  @param h Y component of the eccentricity vector
     *  @param alpha direction cosine α
     *  @param beta direction cosine β
     *  @param retroFactor -1 if the orbit is represented as retrograde, +1 otherwise
     *  @param field field element
     **/
    public FieldGHmsjPolynomials(final T k, final T h,
                            final T alpha, final T beta,
                            final int retroFactor,
                            final Field<T> field) {
        zero = field.getZero();
        this.cjsjKH = new FieldCjSjCoefficient<>(k, h, field);
        this.cjsjAB = new FieldCjSjCoefficient<>(alpha, beta, field);
        this.I = retroFactor;
    }

    /** Get the G<sub>ms</sub><sup>j</sup> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return the G<sub>ms</sub><sup>j</sup>
     */
    public T getGmsj(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T gms = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            gms = cjsjKH.getCj(sMj).multiply(cjsjAB.getCj(mMis)).
                  subtract(cjsjKH.getSj(sMj).multiply(cjsjAB.getSj(mMis)).multiply(sgn(s - j)).multiply(I));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            gms = cjsjKH.getCj(sMj).multiply(cjsjAB.getCj(sMim)).
                  add(cjsjKH.getSj(sMj).multiply(cjsjAB.getSj(sMim)).multiply(sgn(s - j)).multiply(sgn(s - m)));
        }
        return gms;
    }

    /** Get the H<sub>ms</sub><sup>j</sup> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return the H<sub>ms</sub><sup>j</sup>
     */
    public T getHmsj(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T hms = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            hms = cjsjKH.getCj(sMj).multiply(cjsjAB.getSj(mMis)).multiply(I).
                            add(cjsjKH.getSj(sMj).multiply(cjsjAB.getCj(mMis)).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            hms = cjsjKH.getCj(sMj).multiply(cjsjAB.getSj(sMim)).multiply(-sgn(s - m)).
                  add(cjsjKH.getSj(sMj).multiply(cjsjAB.getCj(sMim)).multiply(sgn(s - j)));
        }
        return hms;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public T getdGmsdk(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T dGmsdk = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdk = cjsjKH.getDcjDk(sMj).multiply(cjsjAB.getCj(mMis)).
                     subtract(cjsjKH.getDsjDk(sMj).multiply(cjsjAB.getSj(mMis)).multiply(I).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdk = cjsjKH.getDcjDk(sMj).multiply(cjsjAB.getCj(sMim)).
                     add(cjsjKH.getDsjDk(sMj).multiply(cjsjAB.getSj(sMim)).multiply(sgn(s - m)).multiply(sgn(s - j)));
        }
        return dGmsdk;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public T getdGmsdh(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T dGmsdh = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdh = cjsjKH.getDcjDh(sMj).multiply(cjsjAB.getCj(mMis)).
                     subtract(cjsjKH.getDsjDh(sMj).multiply(cjsjAB.getSj(mMis)).multiply(I).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdh = cjsjKH.getDcjDh(sMj).multiply(cjsjAB.getCj(sMim)).
                     add(cjsjKH.getDsjDh(sMj).multiply(cjsjAB.getSj(sMim)).multiply(sgn(s - m)).multiply(sgn(s - j)));
        }
        return dGmsdh;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>α</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>α</sub>
     */
    public T getdGmsdAlpha(final int m, final int s, final int j) {
        final int sMj  = FastMath.abs(s - j);
        T dGmsdAl = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdAl = cjsjKH.getCj(sMj).multiply(cjsjAB.getDcjDk(mMis)).
                      subtract(cjsjKH.getSj(sMj).multiply(cjsjAB.getDsjDk(mMis)).multiply(I).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdAl = cjsjKH.getCj(sMj).multiply(cjsjAB.getDcjDk(sMim)).
                      add(cjsjKH.getSj(sMj).multiply(cjsjAB.getDsjDk(sMim)).multiply(sgn(s - j)).multiply(sgn(s - m)));
        }
        return dGmsdAl;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>β</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>β</sub>
     */
    public T getdGmsdBeta(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T dGmsdBe = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dGmsdBe = cjsjKH.getCj(sMj).multiply(cjsjAB.getDcjDh(mMis)).
                      subtract(cjsjKH.getSj(sMj).multiply(cjsjAB.getDsjDh(mMis)).multiply(I).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dGmsdBe = cjsjKH.getCj(sMj).multiply(cjsjAB.getDcjDh(sMim)).
                      add(cjsjKH.getSj(sMj).multiply(cjsjAB.getDsjDh(sMim)).multiply(sgn(s - j)).multiply(sgn(s - m)));
        }
        return dGmsdBe;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public T getdHmsdk(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T dHmsdk = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdk = cjsjKH.getDcjDk(sMj).multiply(cjsjAB.getSj(mMis)).multiply(I).
                     add(cjsjKH.getDsjDk(sMj).multiply(cjsjAB.getCj(mMis)).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdk = cjsjKH.getDcjDk(sMj).multiply(cjsjAB.getSj(sMim)).multiply(-sgn(s - m)).
                     add(cjsjKH.getDsjDk(sMj).multiply(cjsjAB.getCj(sMim)).multiply(sgn(s - j)));
        }
        return dHmsdk;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public T getdHmsdh(final int m,  final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T dHmsdh = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdh = cjsjKH.getDcjDh(sMj).multiply(cjsjAB.getSj(mMis)).multiply(I).
                     add(cjsjKH.getDsjDh(sMj).multiply(cjsjAB.getCj(mMis)).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdh = cjsjKH.getDcjDh(sMj).multiply(cjsjAB.getSj(sMim)).multiply(-sgn(s - m)).
                     add(cjsjKH.getDsjDh(sMj).multiply(cjsjAB.getCj(sMim)).multiply(sgn(s - j)));
        }
        return dHmsdh;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>α</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>α</sub>
     */
    public T getdHmsdAlpha(final int m, final int s, final int j) {
        final int sMj  = FastMath.abs(s - j);
        T dHmsdAl = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdAl = cjsjKH.getCj(sMj).multiply(cjsjAB.getDsjDk(mMis)).multiply(I).
                      add(cjsjKH.getSj(sMj).multiply(cjsjAB.getDcjDk(mMis)).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdAl = cjsjKH.getCj(sMj).multiply(cjsjAB.getDsjDk(sMim)).multiply(-sgn(s - m)).
                      add(cjsjKH.getSj(sMj).multiply(cjsjAB.getDcjDk(sMim)).multiply(sgn(s - j)));
        }
        return dHmsdAl;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>β</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>β</sub>
     */
    public T getdHmsdBeta(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        T dHmsdBe = zero;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdBe = cjsjKH.getCj(sMj).multiply(cjsjAB.getDsjDh(mMis)).multiply(I).
                      add(cjsjKH.getSj(sMj).multiply(cjsjAB.getDcjDh(mMis)).multiply(sgn(s - j)));
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdBe = cjsjKH.getCj(sMj).multiply(cjsjAB.getDsjDh(sMim)).multiply(-sgn(s - m)).
                      add(cjsjKH.getSj(sMj).multiply(cjsjAB.getDcjDh(sMim)).multiply(sgn(s - j)));
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
