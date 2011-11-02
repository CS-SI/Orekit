package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.util.ArithmeticUtils;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * @author rdicosta
 */
public class CoefficientFactory {

    /** Internal storage of the polynomial values. Reused for further computation */
    private static TreeMap<NSKey, Double>         VNS            = new TreeMap<NSKey, Double>();

    private static int                            LAST_VNS_ORDER = 2;

    /** Map of the Qns derivatives, for each (n, s) couple {@link CoefficientFactory.NSKey} */
    private static Map<NSKey, PolynomialFunction> QNS_MAP        = new TreeMap<NSKey, PolynomialFunction>();

    static {
        // Initialization
        VNS.put(new NSKey(0, 0), 1.);
        VNS.put(new NSKey(1, 0), 0.);
        VNS.put(new NSKey(1, 1), 0.5);
    }

    /** Get the Q<sub>ns</sub> value from 2.8.1-(4) evaluated in &gamma;
     *  This method is using the Legendre polynomial to compute the Q<sub>ns</sub>'s one.
     *  This direct computation method allows to store the polynomials value in a static map.
     *  If the Q<sub>ns</sub> had been computed already, they just will be evaluated at &gamma;
     * @param gamma &gamma; angle for which Q<sub>ns</sub> is evaluated
     * @param n n value
     * @param s s value
     * 
     *  @return the polynomial value evaluated at &gamma;
     */
    public static double getQnsPolynomialValue(final double gamma,
                                               final int n,
                                               final int s) {
        PolynomialFunction derivative;
        if (QNS_MAP.containsKey(new NSKey(n, s))) {
            derivative = QNS_MAP.get(new NSKey(n, s));
        } else {
            PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(n);
            derivative = legendre;
            for (int i = 0; i < s; i++) {
                derivative = (PolynomialFunction) derivative.derivative();
            }
            QNS_MAP.put(new NSKey(n, s), derivative);
        }
        return derivative.value(gamma);
    }

    /** Compute the Q<sub>ns</sub> array evaluated at &gamma; from the recurrence formula 2.8.3-(2).
     *  As this method directly evaluate the polynomial at &gamma;, values aren't stored.
     *  For single use, this method is faster than the {@link #getQnsPolynomialValue(double, int, int)}
     * @param gamma &gamma; angle
     * @param order order N of computation
     * 
     *  @return Q<sub>ns</sub> array
     */
    public static double[][] computeQnsCoefficient(final double gamma, final int order) {

        // Initialization
        double[][] Qns = new double[order+1][];
        for (int i = 0; i < order+1; i++) {
            Qns[i] = new double[i + 1];
        }

        // first element
        Qns[0][0] = 1;

        for (int n = 1; n <= order; n++) {
            for (int s = 0; s <= n; s++) {
                if (n == s) {
                    Qns[n][s] = (2. * s - 1.) * Qns[s - 1][s - 1];
                } else if (n == (s + 1)) {
                    Qns[n][s] = (2. * s + 1.) * gamma * Qns[s][s];
                } else {
                    Qns[n][s] =  (2. * n - 1.) * gamma * Qns[n - 1][s] - (n + s - 1.) * Qns[n - 2][s];
                    Qns[n][s] /= (n - s);
                }
            }
        }

        return Qns;
    }

    /** Compute recursively G<sub>s</sub> and H<sub>s</sub> polynomials from equation 3.1-(5)
     * 
     *  @param k x-component of the eccentricity vector
     *  @param h y-component of the eccentricity vector
     *  @param alpha 1st direction cosine
     *  @param beta 2nd direction cosine
     *  @param order development order
     *  @return Array of G<sub>s</sub> and H<sub>s</sub> polynomials for s from 0 to order.
     *          The 1st column contains the G<sub>s</sub> values.
     *          The 2nd column contains the H<sub>s</sub> values.
     *  @see getGsHsCoefficient
     */
    public static double[][] computeGsHsCoefficient(final double k,
                                                    final double h,
                                                    final double alpha,
                                                    final double beta,
                                                    final int order) {
        // Initialization
        double[][] GsHs = new double[2][order+1];
        // First Gs coefficient
        GsHs[0][0] = 1.;
        // First Hs coefficient
        GsHs[1][0] = 0.;

        // Constant terms
        final double hamkb = (h * alpha - k * beta);
        final double kaphb = (k * alpha + h * beta);

        for (int s = 1; s <= order; s++) {
            // Gs coefficient
            GsHs[0][s] = kaphb * GsHs[0][s - 1] - hamkb * GsHs[1][s - 1];
            // Hs coefficient
            GsHs[1][s] = hamkb * GsHs[0][s - 1] + kaphb * GsHs[1][s - 1];
        }

        return GsHs;
    }

    /** Compute directly G<sub>s</sub> and H<sub>s</sub> coefficients from equation 3.1-(4)
     * 
     *  @param k x-component of the eccentricity vector
     *  @param h y-component of the eccentricity vector
     *  @param alpha 1st direction cosine
     *  @param beta 2nd direction cosine
     *  @param s development order
     *  @return Array of G<sub>s</sub> and H<sub>s</sub> values for s.
     *          The 1st column contains the G<sub>s</sub> value.
     *          The 2nd column contains the H<sub>s</sub> value.
     */
    public static double[] getGsHsCoefficient(final double k,
                                              final double h,
                                              final double alpha,
                                              final double beta,
                                              final int s) {
        Complex a  = new Complex(k, h);
        Complex as = a.pow(s);
        Complex b  = new Complex(alpha, -beta);
        Complex bs = b.pow(s);
        Complex asbs = as.multiply(bs);
        return new double[] {asbs.getReal(), asbs.getImaginary()};
    }

    /** relation 3.1-(4) */
    public static double getGsCoefficient(final double k,
                                          final double h,
                                          final double alpha,
                                          final double beta,
                                          final int s) {
        Complex a = new Complex(k, h);
        Complex a2 = a.pow(s);
        Complex b = new Complex(alpha, -beta);
        Complex b2 = b.pow(s);
        return a2.multiply(b2).getReal();
    }

    /** relation 3.1-(4) */
    public static double getHsCoefficient(final double k,
                                          final double h,
                                          final double alpha,
                                          final double beta,
                                          final int s) {
        Complex a = new Complex(k, h);
        Complex a2 = a.pow(s);
        Complex b = new Complex(alpha, -beta);
        Complex b2 = b.pow(s);
        return a2.multiply(b2).getImaginary();
    }

    /**
     * Compute the V<sub>n, s</sub> coefficient from 2.8.2 - (1)(2).
     * 
     * @param order
     *            Order of the computation. Computation will be done from order 0 to order -1
     */
    public static TreeMap<NSKey, Double> computeVnsCoefficient(final int order) {

        if (order > LAST_VNS_ORDER) {
            // Compute coefficient
            // Need previous computation as recurrence relation is done at s + 1 and n + 2
            int min = (LAST_VNS_ORDER - 2 < 0) ? 0 : (LAST_VNS_ORDER - 2);
            for (int n = min; n < order; n++) {
                for (int s = 0; s < n + 1; s++) {
                    if ((n - s) % 2 != 0) {
                        VNS.put(new NSKey(n, s), 0.);
                    } else {
                        // s = n
                        if (n == s && (s + 1) < order) {
                            VNS.put(new NSKey(s + 1, s + 1), VNS.get(new NSKey(s, s)) / (2 * s + 2.));
                        }
                        // otherwise
                        if ((n + 2) < order) {
                            VNS.put(new NSKey(n + 2, s), VNS.get(new NSKey(n, s)) * (-n + s - 1.) / (n + s + 2.));
                        }
                    }
                }
            }
            LAST_VNS_ORDER = order;
        }
        return VNS;
    }

    /**
     * Initialize the V<sub>n, s</sub> <sup>m</sup> coefficient from the V<sub>n, s</sub>
     * <sup>m</sup> expression as function of the V<sub>n, s</sub> coefficients. See text in 2.8.2
     * 
     * @param m
     * @param n
     * @param s
     * 
     * @throws OrekitException if s > n or m > s
     */
    public static double getVmns(final int m,
                                 final int n,
                                 final int s) throws OrekitException {
        if (s > n) {
            throw new OrekitException(OrekitMessages.DSST_VMSN_COEFFICIENT_ERROR_NS, s, n);
        }
        if (m > n) {
            throw new OrekitException(OrekitMessages.DSST_VMSN_COEFFICIENT_ERROR_MS, m, s);
        }

        if ((n + 1) > LAST_VNS_ORDER) {
            // Update the Vns coefficient
            computeVnsCoefficient(n + 1);
        }

        // If (n -s) is odd, the Vmsn coefficient is null
        double result = 0;
        if ((n - s) % 2 == 0) {
            result = ArithmeticUtils.factorial(n + s) * VNS.get(new NSKey(n, s)) / ArithmeticUtils.factorial(n - m);
        }
        return result;
    }

    /**
     * Direct computation for the Vmns coefficient from equation 2.7.1 - (6)
     * 
     * @throws OrekitException
     */
    public static double getVmns2(final int m,
                                  final int n,
                                  final int s) throws OrekitException {
        if (s > n) {
            throw new OrekitException(OrekitMessages.DSST_VMSN_COEFFICIENT_ERROR_NS, s, n);
        }
        if (m > n) {
            throw new OrekitException(OrekitMessages.DSST_VMSN_COEFFICIENT_ERROR_MS, m, s);
        }
        double vmsn = 0d;
        if ((n - s) % 2 == 0) {
            final double num = FastMath.pow(-1, (n - s) / 2d) * ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);
            final double den = FastMath.pow(2, n) * ArithmeticUtils.factorial(n - m) * ArithmeticUtils.factorial((n + s) / 2)
                            * ArithmeticUtils.factorial((n - s) / 2);
            vmsn = num / den;
        }
        return vmsn;
    }

    /**
     * Implementation of the V<sub>n, s</sub> <sup>m</sup> from equations in paragraph 2.7.2.
     * 
     * @param mMax
     * @param sMax
     * @return Map<MNSKey, Double>
     */
    @Deprecated
    public static Map<MNSKey, Double> computeVmns2(final int mMax,
                                                   final int sMax) {
        // V[m][s][n]
        Map<MNSKey, Double> Vmsn = new TreeMap<CoefficientFactory.MNSKey, Double>();
        double num;
        double den;
        MNSKey key;
        double value;

        // Initialization :
        Vmsn.put(new MNSKey(0, 0, 0), 1d);

        // Case where m = 0
        for (int s = 0; s < sMax; s++) {
            key = new MNSKey(0, s + 1, s + 1);
            value = ((2 * s + 1) * Vmsn.get(new MNSKey(0, s, s)) / (s + 1));
            Vmsn.put(key, value);

        }

        // Case where n = s, m != 0 :
        for (int m = 0; m < mMax; m++) {
            for (int s = 0; s <= m + 1; s++) {
                if (s <= m + 1) {
                    key = new MNSKey(m + 1, s, s);
                    System.out.println(key);
                    value = (s - m) * Vmsn.get(new MNSKey(m, s, s));
                    Vmsn.put(key, value);
                }
            }
        }

        for (int m = 1; m < mMax; m++) {
            int nMin = FastMath.max(2, m);
            for (int n = nMin; n < m + 1; n++) {
                for (int s = 0; s < n + 1; s++) {
                    if ((n - s) % 2 != 0) {
                        key = new MNSKey(m, s, n);
                        Vmsn.put(new MNSKey(m, s, n), 0d);
                    } else {
                        // (n - s) even
                        num = -(n + s + 1) * (n - s + 1) * Vmsn.get(new MNSKey(m, s, s));
                        den = (n - m + 2) * (n - m + 1);
                        key = new MNSKey(m, s, n + 2);
                        value = num / den;
                        Vmsn.put(key, value);
                    }
                }
            }
        }
        return Vmsn;
    }

    /**
     * Implementation of the V<sub>n, s</sub> <sup>m</sup> from equations in paragraph 2.7.2.
     * 
     * @param mRange
     * @param nRange
     * @param sRange
     * @return Map<MNSKey, Double>
     */
    @Deprecated
    public static Map<MNSKey, Double> computeVmns(final int mMax,
                                                  final int sMax) {
        // V[m][s][n]
        Map<MNSKey, Double> Vmsn = new TreeMap<CoefficientFactory.MNSKey, Double>();
        double num;
        double den;
        MNSKey key;
        double value;

        // Initialization :
        Vmsn.put(new MNSKey(0, 0, 0), 1d);

        // Case where n = s :
        for (int m = 0; m < mMax; m++) {
            for (int s = 0; s < sMax; s++) {
                key = new MNSKey(0, s + 1, s + 1);
                value = ((2 * s + 1) * Vmsn.get(new MNSKey(0, s, s)) / (s + 1));
                Vmsn.put(key, value);
                System.out.println(key + " " + value);
                // Case when m > 0 and n = s
                key = new MNSKey(m + 1, s, s);
                value = (s - m) * Vmsn.get(new MNSKey(m, s, s));
                System.out.println(key + " " + value);

                Vmsn.put(key, value);
                // Case for non negative m and s and increasing n
                int nMin = FastMath.max(2, m);
                nMin = FastMath.max(nMin, FastMath.abs(s));
                for (int n = nMin; n < mMax; n++) {
                    // Null if (n - s) is odd
                    if ((n - s) % 2 != 0) {
                        key = new MNSKey(m, s, n);
                        Vmsn.put(new MNSKey(m, s, n), 0d);
                        System.out.println(key + " " + value);

                    } else {
                        // (n - s) even
                        num = -(n + s + 1) * (n - s + 1) * Vmsn.get(new MNSKey(m, s, s));
                        den = (n - m + 2) * (n - m + 1);
                        key = new MNSKey(m, s, n + 2);
                        value = num / den;
                        Vmsn.put(key, value);
                        System.out.println(key + " " + value);

                    }
                }
            }
        }
        return Vmsn;
    }

    /**
     * Key formed by two integer values
     */
    public static class NSKey implements Comparable<NSKey> {

        /** n value */
        final int n;

        /** s value */
        final int s;

        /** Default constructor */
        public NSKey(final int n,
                     final int s) {
            this.n = n;
            this.s = s;
        }

        /** Get n */
        public int getN() {
            return n;
        }

        /** Get s */
        public int getS() {
            return s;
        }

        @Override
        public String toString() {
            return new String("[" + n + ", " + s + "]");
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(NSKey key) {
            int result = 1;
            if (n == key.n) {
                if (s < key.s) {
                    result = -1;
                } else if (s == key.s) {
                    result = 0;
                }
            } else if (n < key.n) {
                result = -1;
            }
            return result;
        }
    }

    /**
     * MNS couple's key
     */
    public static class MNSKey implements Comparable<MNSKey> {

        /** m value */
        final int m;

        /** s value */
        final int s;

        /** n value */
        final int n;

        /** Default constructor */
        public MNSKey(final int m,
                      final int n,
                      final int s) {
            this.m = m;
            this.s = s;
            this.n = n;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(MNSKey key) {
            int result = 1;
            if (m == key.m) {
                if (n == key.n) {
                    if (s < key.s) {
                        result = -1;
                    } else if (s == key.s) {
                        result = 0;
                    } else {
                        result = 1;
                    }
                } else if (n < key.n) {
                    result = -1;
                } else {
                    result = 1;
                }
            } else if (m < key.m) {
                result = -1;
            }
            return result;
        }

        @Override
        public String toString() {
            return new String("[" + m + ", " + n + ", " + s + "]");
        }
    }

}
