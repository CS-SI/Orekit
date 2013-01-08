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
     * I = -1 if yes, +1 otherwise.
     */
    private int                   I;

    /** Create a set of G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials.
     *  @param k X component of the eccentricity vector
     *  @param h Y component of the eccentricity vector
     *  @param alpha direction cosine &alpha;
     *  @param beta direction cosine &beta;
     *  @param isRetrograde Is the orbit is represented as a retrograde orbit I = -1, +1 otherwise
     **/
    public GHmsjPolynomials(final double k, final double h,
                            final double alpha, final double beta,
                            final int isRetrograde) {
        this.cjsjKH = new CjSjCoefficient(k, h);
        this.cjsjAB = new CjSjCoefficient(alpha, beta);
        this.I = isRetrograde;
    }

    /** Get the G<sub>ms</sub><sup>j</sup> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return the G<sub>ms</sub><sup>j</sup>
     */
    public double getGmsj(final int m, final int s, final int j) {
        final int abssMj = FastMath.abs(s - j);
        double gms = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            gms = cjsjKH.getCj(abssMj) * cjsjAB.getCj(mMis) -
                  I * FastMath.signum(s - j) * cjsjKH.getSj(abssMj) * cjsjAB.getSj(mMis);
        } else if (FastMath.abs(s) >= m) {
            final int sMim = FastMath.abs(s - I * m);
            gms = cjsjKH.getCj(abssMj) * cjsjAB.getCj(sMim) +
                  FastMath.signum(s - j) * FastMath.signum(s - m) * cjsjKH.getSj(abssMj) * cjsjAB.getSj(sMim);
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
            hms = I * cjsjKH.getCj(sMj) * cjsjAB.getSj(mMis) + FastMath.signum(s - j) *
                  cjsjKH.getSj(sMj) * cjsjAB.getCj(mMis);
        } else if (FastMath.abs(s) >= m) {
            final int sMim = FastMath.abs(s - I * m);
            hms = -FastMath.signum(s - m) * cjsjKH.getCj(sMj) * cjsjAB.getSj(sMim) +
                   FastMath.signum(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getCj(sMim);
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
        final int mMis = m - I * s;
        final int sMim = FastMath.abs(s - I * m);

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cjsjAB.getCj(mMis) * cjsjKH.getDcjDk(sMj) -
                   I * FastMath.signum(s - j) * cjsjAB.getSj(mMis) * cjsjKH.getDsjDk(sMj);
        } else if (FastMath.abs(s) >= m) {
            dGms = cjsjAB.getCj(sMim) * cjsjKH.getDcjDk(sMj) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cjsjAB.getSj(sMim) * cjsjKH.getDsjDk(sMj);
        }
        return dGms;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public double getdGmsdh(final int m, final int s, final int j) {
        final int mMis = m - I * s;
        final int sMim = FastMath.abs(s - I * m);
        final int sMj = FastMath.abs(s - j);

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cjsjAB.getCj(mMis) * cjsjKH.getDcjDh(sMj) -
                   I * FastMath.signum(s - j) * cjsjAB.getSj(mMis) * cjsjKH.getDsjDh(sMj);
        } else if (FastMath.abs(s) >= m) {
            dGms = cjsjAB.getCj(sMim) * cjsjKH.getDcjDh(sMj) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cjsjAB.getSj(sMim) * cjsjKH.getDsjDh(sMj);
        }
        return dGms;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub>
     */
    public double getdGmsdAlpha(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int sMim = FastMath.abs(s - I * m);

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cjsjKH.getCj(sMj) * cjsjAB.getDcjDk(mMis) -
                   I * FastMath.signum(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDk(mMis);
        } else if (FastMath.abs(s) >= m) {
            dGms = cjsjKH.getCj(sMj) * cjsjAB.getDcjDk(sMim) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDk(sMim);
        }
        return dGms;
    }

    /** Get the dG<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub>
     */
    public double getdGmsdBeta(final int m, final int s, final int j) {
        final int sMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int sMim = FastMath.abs(s - I * m);

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cjsjKH.getCj(sMj) * cjsjAB.getDcjDh(mMis) -
                   I * FastMath.signum(s - j) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDh(mMis);
        } else if (FastMath.abs(s) > m) {
            dGms = cjsjKH.getCj(sMj) * cjsjAB.getDcjDh(sMim) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cjsjKH.getSj(sMj) * cjsjAB.getDsjDh(sMim);
        }
        return dGms;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public double getdHmsdk(final int m, final int s, final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cjsjAB.getSj(mMis) * cjsjKH.getDcjDk(abssMj) +
                   FastMath.signum(s - j) * cjsjAB.getCj(mMis) * cjsjKH.getDsjDk(abssMj);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cjsjAB.getSj(abssMim) * cjsjKH.getDcjDk(abssMj) +
                    FastMath.signum(s - j) * cjsjAB.getCj(abssMim) * cjsjKH.getDsjDk(abssMj);
        }
        return dHms;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public double getdHmsdh(final int m,  final int s, final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cjsjAB.getSj(mMis) * cjsjKH.getDcjDh(abssMj) +
                   FastMath.signum(s - j) * cjsjAB.getCj(mMis) * cjsjKH.getDsjDh(abssMj);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cjsjAB.getSj(abssMim) * cjsjKH.getDcjDh(abssMj) +
                    FastMath.signum(s - j) * cjsjAB.getCj(abssMim) * cjsjKH.getDsjDh(abssMj);
        }
        return dHms;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub>
     */
    public double getdHmsdAlpha(final int m, final int s, final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cjsjKH.getCj(abssMj) * cjsjAB.getDsjDk(mMis) +
                   FastMath.signum(s - j) * cjsjKH.getSj(abssMj) * cjsjAB.getDcjDk(mMis);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cjsjKH.getCj(abssMj) * cjsjAB.getDsjDk(abssMim) +
                    FastMath.signum(s - j) * cjsjKH.getSj(abssMj) * cjsjAB.getDcjDk(abssMim);
        }
        return dHms;
    }

    /** Get the dH<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub> coefficient.
     * @param m m subscript
     * @param s s subscript
     * @param j order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub>
     */
    public double getdHmsdBeta(final int m, final int s, final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cjsjKH.getCj(abssMj) * cjsjAB.getDsjDh(mMis) +
                   FastMath.signum(s - j) * cjsjKH.getSj(abssMj) * cjsjAB.getDcjDh(mMis);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cjsjKH.getCj(abssMj) * cjsjAB.getDsjDh(abssMim) +
                    FastMath.signum(s - j) * cjsjKH.getSj(abssMj) * cjsjAB.getDcjDh(abssMim);
        }
        return dHms;
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
