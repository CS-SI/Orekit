/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.coefficients;

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
    private final CjSjCoefficient cisiKH;

    /** C<sub>j</sub>(&alpha;, &beta;), S<sub>j</sub>(&alpha;, &beta;) coefficient.
     * (&alpha;, &beta;) are the direction cosines
     */
    private final CjSjCoefficient cisiAB;

    /** Is the orbit represented as a retrograde orbit.
     * I = -1 if yes, +1 otherwise.
     */
    private int                   I;

    /** Create a set of G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials.
     * @param cisiHK {C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient
     * @param cisiAB {C<sub>j</sub>(&alpha;, &beta;), S<sub>j</sub>(&alpha;, &beta;) coefficient
     * @param isRetrograde Is the orbit is represented as a retrograde orbit I = -1, +1 otherwise
     **/
    public GHmsjPolynomials(final CjSjCoefficient cisiHK, final CjSjCoefficient cisiAB,
                            final int isRetrograde) {
        this.cisiKH = cisiHK;
        this.cisiAB = cisiAB;
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
            gms = cisiKH.getCj(abssMj) * cisiAB.getCj(mMis) -
                  I * FastMath.signum(s - j) * cisiKH.getSj(abssMj) * cisiAB.getSj(mMis);
        } else if (FastMath.abs(s) >= m) {
            final int sMim = FastMath.abs(s - I * m);
            gms = cisiKH.getCj(abssMj) * cisiAB.getCj(sMim) +
                  FastMath.signum(s - j) * FastMath.signum(s - m) * cisiKH.getSj(abssMj) * cisiAB.getSj(sMim);
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
            hms = I * cisiKH.getCj(sMj) * cisiAB.getSj(mMis) + FastMath.signum(s - j) *
                  cisiKH.getSj(sMj) * cisiAB.getCj(mMis);
        } else if (FastMath.abs(s) >= m) {
            final int sMim = FastMath.abs(s - I * m);
            hms = -FastMath.signum(s - m) * cisiKH.getCj(sMj) * cisiAB.getSj(sMim) +
                   FastMath.signum(s - j) * cisiKH.getSj(sMj) * cisiAB.getCj(sMim);
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
            dGms = cisiAB.getCj(mMis) * cisiKH.getDcjDk(sMj) -
                   I * FastMath.signum(s - j) * cisiAB.getSj(mMis) * cisiKH.getDsjDk(sMj);
        } else if (FastMath.abs(s) >= m) {
            dGms = cisiAB.getCj(sMim) * cisiKH.getDcjDk(sMj) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cisiAB.getSj(sMim) * cisiKH.getDsjDk(sMj);
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
            dGms = cisiAB.getCj(mMis) * cisiKH.getDcjDh(sMj) -
                   I * FastMath.signum(s - j) * cisiAB.getSj(mMis) * cisiKH.getDsjDh(sMj);
        } else if (FastMath.abs(s) >= m) {
            dGms = cisiAB.getCj(sMim) * cisiKH.getDcjDh(sMj) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cisiAB.getSj(sMim) * cisiKH.getDsjDh(sMj);
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
            dGms = cisiKH.getCj(sMj) * cisiAB.getDcjDk(mMis) -
                   I * FastMath.signum(s - j) * cisiKH.getSj(sMj) * cisiAB.getDsjDk(mMis);
        } else if (FastMath.abs(s) >= m) {
            dGms = cisiKH.getCj(sMj) * cisiAB.getDcjDk(sMim) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cisiKH.getSj(sMj) * cisiAB.getDsjDk(sMim);
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
            dGms = cisiKH.getCj(sMj) * cisiAB.getDcjDh(mMis) -
                   I * FastMath.signum(s - j) * cisiKH.getSj(sMj) * cisiAB.getDsjDh(mMis);
        } else if (FastMath.abs(s) > m) {
            dGms = cisiKH.getCj(sMj) * cisiAB.getDcjDh(sMim) +
                   FastMath.signum(s - j) * FastMath.signum(s - m) * cisiKH.getSj(sMj) * cisiAB.getDsjDh(sMim);
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
            dHms = I * cisiAB.getSj(mMis) * cisiKH.getDcjDk(abssMj) +
                   FastMath.signum(s - j) * cisiAB.getCj(mMis) * cisiKH.getDsjDk(abssMj);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiAB.getSj(abssMim) * cisiKH.getDcjDk(abssMj) +
                    FastMath.signum(s - j) * cisiAB.getCj(abssMim) * cisiKH.getDsjDk(abssMj);
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
            dHms = I * cisiAB.getSj(mMis) * cisiKH.getDcjDh(abssMj) +
                   FastMath.signum(s - j) * cisiAB.getCj(mMis) * cisiKH.getDsjDh(abssMj);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiAB.getSj(abssMim) * cisiKH.getDcjDh(abssMj) +
                    FastMath.signum(s - j) * cisiAB.getCj(abssMim) * cisiKH.getDsjDh(abssMj);
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
            dHms = I * cisiKH.getCj(abssMj) * cisiAB.getDsjDk(mMis) +
                   FastMath.signum(s - j) * cisiKH.getSj(abssMj) * cisiAB.getDcjDk(mMis);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiKH.getCj(abssMj) * cisiAB.getDsjDk(abssMim) +
                    FastMath.signum(s - j) * cisiKH.getSj(abssMj) * cisiAB.getDcjDk(abssMim);
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
            dHms = I * cisiKH.getCj(abssMj) * cisiAB.getDsjDh(mMis) +
                   FastMath.signum(s - j) * cisiKH.getSj(abssMj) * cisiAB.getDcjDh(mMis);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiKH.getCj(abssMj) * cisiAB.getDsjDh(abssMim) +
                    FastMath.signum(s - j) * cisiKH.getSj(abssMj) * cisiAB.getDcjDh(abssMim);
        }
        return dHms;
    }

}
