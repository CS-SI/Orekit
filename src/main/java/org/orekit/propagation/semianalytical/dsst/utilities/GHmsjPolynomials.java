/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

/** Compute the G<sub>ms</sub><sup>j</sup> and the H<sub>ms</sub><sup>j</sup>
 *  polynomials in the equinoctial elements h, k and the direction cosines &alpha; and &beta;
 *  and their partial derivatives with respect to k, h, &alpha; and &beta;.
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

    /** C<sub>j</sub>(&alpha;, &beta;), S<sub>j</sub>(&alpha;, &beta;) coefficient.
     * (&alpha;, &beta;) are the direction cosines
     */
    private final CjSjCoefficient cjsjAB;

    /** Is the orbit represented as a retrograde orbit.
     *  I = -1 if yes, +1 otherwise.
     */
    private int                   I;

    /** Create a set of G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials.
     *  @param k X component of the eccentricity vector
     *  @param h Y component of the eccentricity vector
     *  @param alpha direction cosine &alpha;
     *  @param beta direction cosine &beta;
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

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub>
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

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub>
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
        double dHmsdk = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            dHmsdk = I * cjsjKH.getDcjDh(sMj) * cjsjAB.getSj(mMis) +
                    sgn(s - j) * cjsjKH.getDsjDh(sMj) * cjsjAB.getCj(mMis);
        } else {
            final int sMim = FastMath.abs(s - I * m);
            dHmsdk = -sgn(s - m) * cjsjKH.getDcjDh(sMj) * cjsjAB.getSj(sMim) +
                    sgn(s - j) * cjsjKH.getDsjDh(sMj) * cjsjAB.getCj(sMim);
        }
        return dHmsdk;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub>
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

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub>
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

    /** Compute the S<sub>j</sub>(k, h) and the C<sub>j</sub>(k, h) series
     *  and their partial derivatives with respect to k and h.
     *  <p>
     *  Those series are given in Danielson paper by expression 2.5.3-(5):
     *  <pre>C<sub>j</sub>(k, h) + i S<sub>j</sub>(k, h) = (k+ih)<sup>j</sup> </pre>
     *  </p>
     *  The C<sub>j</sub>(k, h) and the S<sub>j</sub>(k, h) elements are store as an
     *  {@link ArrayList} of {@link Complex} number, the C<sub>j</sub>(k, h) being
     *  represented by the real and the S<sub>j</sub>(k, h) by the imaginary part.
     */
    private static class CjSjCoefficient {

        /** Last computed order j. */
        private int jLast;

        /** Complex base (k + ih) of the C<sub>j</sub>, S<sub>j</sub> series. */
        private final Complex kih;

        /** List of computed elements. */
        private final List<Complex> cjsj;

        /** C<sub>j</sub>(k, h) and S<sub>j</sub>(k, h) constructor.
         * @param k k value
         * @param h h value
         */
        public CjSjCoefficient(final double k, final double h) {
            kih  = new Complex(k, h);
            cjsj = new ArrayList<Complex>();
            cjsj.add(new Complex(1, 0));
            cjsj.add(kih);
            jLast = 1;
        }

        /** Get the C<sub>j</sub> coefficient.
         * @param j order
         * @return C<sub>j</sub>
         */
        public double getCj(final int j) {
            if (j > jLast) {
                // Update to order j
                updateCjSj(j);
            }
            return cjsj.get(j).getReal();
        }

        /** Get the S<sub>j</sub> coefficient.
         * @param j order
         * @return S<sub>j</sub>
         */
        public double getSj(final int j) {
            if (j > jLast) {
                // Update to order j
                updateCjSj(j);
            }
            return cjsj.get(j).getImaginary();
        }

        /** Get the dC<sub>j</sub> / dk coefficient.
         * @param j order
         * @return dC<sub>j</sub> / d<sub>k</sub>
         */
        public double getDcjDk(final int j) {
            return j == 0 ? 0 : j * getCj(j - 1);
        }

        /** Get the dS<sub>j</sub> / dk coefficient.
         * @param j order
         * @return dS<sub>j</sub> / d<sub>k</sub>
         */
        public double getDsjDk(final int j) {
            return j == 0 ? 0 : j * getSj(j - 1);
        }

        /** Get the dC<sub>j</sub> / dh coefficient.
         * @param j order
         * @return dC<sub>i</sub> / d<sub>k</sub>
         */
        public double getDcjDh(final int j) {
            return j == 0 ? 0 : -j * getSj(j - 1);
        }

        /** Get the dS<sub>j</sub> / dh coefficient.
         * @param j order
         * @return dS<sub>j</sub> / d<sub>h</sub>
         */
        public double getDsjDh(final int j) {
            return j == 0 ? 0 : j * getCj(j - 1);
        }

        /** Update the cjsj up to order j.
         * @param j order
         */
        private void updateCjSj(final int j) {
            Complex last = cjsj.get(cjsj.size() - 1);
            for (int i = jLast; i < j; i++) {
                final Complex next = last.multiply(kih);
                cjsj.add(next);
                last = next;
            }
            jLast = j;
        }

    }

}
