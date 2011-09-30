package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.util.FastMath;

/**
 * Compute the G<sub>ms</sub><sup>j</sup> and the H<sub>ms</sub><sup>j</sup> polynomials function in
 * the equinoctial elements h, k and the direction cosines &alpha; and &beta;<br>
 * and their partial derivatives to k, h, &alpha; and &beta; Those equation are issue from the
 * Danielson paper (Semi-Analytic Satellite Theory). The expression used here are equations at 2.7.5
 * - (1)(2).
 */
public class GHmsjPolynomials {

    /**
     * {C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient, where (k, h) are the (x, y) component
     * of the eccentricity vector in equinoctial elements
     */
    private final CiSiCoefficient cisiKH;

    /**
     * {C<sub>j</sub>(&alpha;, &beta;), S<sub>j</sub>(&alpha;, &beta;) coefficient, where (&alpha;,
     * &beta;) are the direction cosines
     */
    private final CiSiCoefficient cisiAB;

    /** Is the orbit represented as a retrograde orbit. I = -1 if yes, +1 otherwise */
    private int                   I;

    /**
     * Create a set of G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials
     * 
     * @param cisiHK
     *            {C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient
     * @param cisiAB
     *            {C<sub>j</sub>(&alpha;, &beta;), S<sub>j</sub>(&alpha;, &beta;) coefficient
     * @param isRetrograde
     *            Is the orbit represented as a retrograde orbit
     **/
    public GHmsjPolynomials(final CiSiCoefficient cisiHK,
                            final CiSiCoefficient cisiAB,
                            final boolean isRetrograde) {
        this.cisiKH = cisiHK;
        this.cisiAB = cisiAB;
        this.I = (isRetrograde ? -1 : 1);
    }

    /**
     * Get the G<sub>ms</sub><sup>j</sup> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return the G<sub>ms</sub><sup>j</sup>
     */
    public double getGmsj(final int m,
                          final int s,
                          final int j) {
        final int sMj = FastMath.abs(s - j);
        double gms = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            gms = cisiKH.getCi(sMj) * cisiAB.getCi(mMis) - I * FastMath.signum(s - j) * cisiKH.getSi(sMj)
                            * cisiAB.getSi(mMis);
        } else if (FastMath.abs(s) >= m) {
            final int sMim = FastMath.abs(s - I * m);
            gms = cisiKH.getCi(sMj) * cisiAB.getCi(sMim) + FastMath.signum(s - j) * FastMath.signum(s - m)
                            * cisiKH.getSi(sMj) * cisiAB.getSi(sMim);
        }
        return gms;
    }

    /**
     * Get the H<sub>ms</sub><sup>j</sup> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return the H<sub>ms</sub><sup>j</sup>
     */
    public double getHmsj(final int m,
                          final int s,
                          final int j) {

        final int sMj = FastMath.abs(s - j);
        double hms = 0d;
        if (FastMath.abs(s) <= m) {
            final int mMis = m - I * s;
            hms = I * cisiKH.getCi(sMj) * cisiAB.getSi(mMis) + FastMath.signum(s - j) * cisiKH.getSi(sMj)
                            * cisiAB.getCi(mMis);
        } else if (FastMath.abs(s) >= m) {
            final int sMim = FastMath.abs(s - I * m);
            hms = -FastMath.signum(s - m) * cisiKH.getCi(sMj) * cisiAB.getSi(sMim) + FastMath.signum(s - j)
                            * cisiKH.getSi(sMj) * cisiAB.getCi(sMim);
        }
        return hms;
    }

    /**
     * Get the dG<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public double getdGmsdk(final int m,
                            final int s,
                            final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int sMim = s - I * m;

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cisiAB.getCi(mMis) * cisiKH.getDciDk(s - j) - I * FastMath.signum(s - j) * cisiAB.getSi(mMis)
                            * cisiKH.getDsiDk(abssMj);
        } else if (FastMath.abs(s) >= m) {
            dGms = cisiAB.getCi(sMim) * cisiKH.getDciDk(s - j) + FastMath.signum(s - j) * FastMath.signum(s - m)
                            * cisiAB.getSi(sMim) * cisiKH.getDsiDk(abssMj);
        }
        return dGms;
    }

    /**
     * Get the dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public double getdGmsdh(final int m,
                            final int s,
                            final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int sMim = s - I * m;

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cisiAB.getCi(mMis) * cisiKH.getDciDh(s - j) - I * FastMath.signum(s - j) * cisiAB.getSi(mMis)
                            * cisiKH.getDsiDh(abssMj);
        } else if (FastMath.abs(s) >= m) {
            dGms = cisiAB.getCi(sMim) * cisiKH.getDciDh(s - j) + FastMath.signum(s - j) * FastMath.signum(s - m)
                            * cisiAB.getSi(sMim) * cisiKH.getDsiDh(abssMj);
        }
        return dGms;
    }

    /**
     * Get the dG<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub>
     */
    public double getdGmsdAlpha(final int m,
                                final int s,
                                final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int sMim = s - I * m;

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cisiKH.getCi(s - j) * cisiAB.getDciDk(mMis) - I * FastMath.signum(s - j) * cisiKH.getSi(abssMj)
                            * cisiAB.getDsiDk(mMis);
        } else if (FastMath.abs(s) >= m) {
            dGms = cisiKH.getCi(s - j) * cisiAB.getDciDk(sMim) + FastMath.signum(s - j) * FastMath.signum(s - m)
                            * cisiKH.getSi(abssMj) * cisiAB.getDsiDk(sMim);
        }
        return dGms;
    }

    /**
     * Get the dG<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dG<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub>
     */
    public double getdGmsdBeta(final int m,
                               final int s,
                               final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int sMim = s - I * m;

        double dGms = 0d;
        if (FastMath.abs(s) <= m) {
            dGms = cisiKH.getCi(s - j) * cisiAB.getDciDh(mMis) - I * FastMath.signum(s - j) * cisiKH.getSi(abssMj)
                            * cisiAB.getDsiDh(mMis);
        } else if (FastMath.abs(s) > m) {
            dGms = cisiKH.getCi(s - j) * cisiAB.getDciDh(sMim) + FastMath.signum(s - j) * FastMath.signum(s - m)
                            * cisiKH.getSi(abssMj) * cisiAB.getDsiDh(sMim);
        }
        return dGms;
    }

    /**
     * Get the dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>k</sub>
     */
    public double getdHmsdk(final int m,
                            final int s,
                            final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cisiAB.getSi(mMis) * cisiKH.getDciDk(abssMj) + FastMath.signum(s - j) * cisiAB.getCi(mMis)
                            * cisiKH.getDsiDk(abssMj);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiAB.getSi(abssMim) * cisiKH.getDciDk(abssMj) + FastMath.signum(s - j)
                            * cisiAB.getCi(abssMim) * cisiKH.getDsiDk(abssMj);
        }
        return dHms;
    }

    /**
     * Get the dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>h</sub>
     */
    public double getdHmsdh(final int m,
                            final int s,
                            final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cisiAB.getSi(mMis) * cisiKH.getDciDh(abssMj) + FastMath.signum(s - j) * cisiAB.getCi(mMis)
                            * cisiKH.getDsiDh(abssMj);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiAB.getSi(abssMim) * cisiKH.getDciDh(abssMj) + FastMath.signum(s - j)
                            * cisiAB.getCi(abssMim) * cisiKH.getDsiDh(abssMj);
        }
        return dHms;
    }

    /**
     * Get the dH<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>&alpha;</sub>
     */
    public double getdHmsdAlpha(final int m,
                                final int s,
                                final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cisiKH.getCi(abssMj) * cisiAB.getDsiDk(mMis) + FastMath.signum(s - j) * cisiKH.getSi(abssMj)
                            * cisiAB.getDciDk(mMis);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiKH.getCi(abssMj) * cisiAB.getDsiDk(abssMim) + FastMath.signum(s - j)
                            * cisiKH.getSi(abssMj) * cisiAB.getDciDk(abssMim);
        }
        return dHms;
    }

    /**
     * Get the dH<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub> coefficient
     * 
     * @param m
     *            m subscript
     * @param s
     *            s subscript
     * @param j
     *            order
     * @return dH<sub>ms</sub><sup>j</sup> / d<sub>&beta;</sub>
     */
    public double getdHmsdBeta(final int m,
                               final int s,
                               final int j) {
        final int abssMj = FastMath.abs(s - j);
        final int mMis = m - I * s;
        final int abssMim = FastMath.abs(s - I * m);

        double dHms = 0d;
        if (FastMath.abs(s) <= m) {
            dHms = I * cisiKH.getCi(abssMj) * cisiAB.getDsiDh(mMis) + FastMath.signum(s - j) * cisiKH.getSi(abssMj)
                            * cisiAB.getDciDh(mMis);
        } else if (FastMath.abs(s) > m) {
            dHms = -FastMath.signum(s - m) * cisiKH.getCi(abssMj) * cisiAB.getDsiDh(abssMim) + FastMath.signum(s - j)
                            * cisiKH.getSi(abssMj) * cisiAB.getDciDh(abssMim);
        }
        return dHms;
    }
}
