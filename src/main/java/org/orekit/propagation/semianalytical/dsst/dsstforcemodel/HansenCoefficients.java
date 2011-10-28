package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.MNSKey;

public class HansenCoefficients {

    private static TreeMap<MNSKey, Double> HANSEN_KERNEL             = new TreeMap<CoefficientFactory.MNSKey, Double>();

    private static TreeMap<MNSKey, Double> HANSEN_KERNEL_DERIVATIVES = new TreeMap<CoefficientFactory.MNSKey, Double>();

    private final double                   eccentricity;

    private final double                   EPSILON;

    public HansenCoefficients(final double ecc,
                              final double epsilon) {
        this.eccentricity = ecc;
        this.EPSILON = epsilon;
        initializeKernels();

    }

    private void initializeKernels() {
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(0, 0, 0), 0d);
        HANSEN_KERNEL.put(new MNSKey(0, 0, 0), 1d);
        HANSEN_KERNEL.put(new MNSKey(0, 0, 1), -1d);
        HANSEN_KERNEL.put(new MNSKey(0, 1, 0), 1 + eccentricity * eccentricity / 2d);
        HANSEN_KERNEL.put(new MNSKey(0, 1, 1), -3 / 2d);
    }

    /**
     * Get the K<sub>j</sub><sup>n,s</sup> coefficient value, for any value.
     * 
     * @param j
     *            j
     * @param n
     *            n
     * @param s
     *            s
     * @return
     * @throws OrekitException
     */
    public double getHansenKernelValue(final int j,
                                       final int n,
                                       final int s) throws OrekitException {
        double value;
        if (j == 0 && n >= 0) {
            // Compute the K0(n, s) coefficients, used for the third body description
            value = computeHansenKernelPositiveSubscriptNullJ(n, s);
        } else if (j == 0 && n < 0) {
            // Compute the K0(-n-1, s) coefficients used for zonal harmonics expanded in central
            // body potential expression
            value = computeHansenKernelNegativeSubscribtNullJ(-n - 1, s);
        } else {
            // Compute the general Kj(-n-1, s) coefficients used for tesseral harmonics expanded
            // in central body potential expression.
            value = computeHansenKernelNegativeSubscribtNonNullJ(j, -n - 1, s, EPSILON);
        }
        return value;
    }

    /**
     * Compute derivatives of the hansen kernel for any (j,n,s)
     * 
     * @param j
     *            j
     * @param n
     *            n
     * @param s
     *            s
     * @return
     * @throws OrekitException
     */
    public double getHansenKernelDerivative(final int j,
                                            final int n,
                                            final int s) throws OrekitException {
        double value = 0d;
        // Computation for negative subscript
        if (j == 0 && n < 0) {
            value = getDkdXNegativeSubscript(-n - 1, s);
        } else if (j == 0 && n >= 0) {
            value = getDkdXPositiveSubscriptJNull(n, s);
        } else if (j != 0 && n > 0d) {
            value = getDkdXPositiveSubscript(j, n, s, EPSILON);
        }
        return value;
    }

    /**
     * Compute K<sub>0</sub><sup>n,s</sup> with positive subscript from Equation 2.7.3 - (7)(8).
     * Those coefficients are used for third body description.
     * 
     * @param n
     *            n
     * @param s
     *            s
     * @return K<sub>0</sub><sup>n,s</sup>
     * @throws OrekitException
     */
    private double computeHansenKernelPositiveSubscriptNullJ(int n,
                                                             int s) throws OrekitException {

        final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
        final double khi2 = khi * khi;
        double result = 0d;
        double val = 0d;
        MNSKey key;
        if (n == (s - 1) && n >= 1) {
            key = new MNSKey(0, s - 2, s - 1);
            if (HANSEN_KERNEL.containsKey(key)) {
                val = HANSEN_KERNEL.get(key);
            } else {
                val = computeHansenKernelPositiveSubscriptNullJ(s - 2, s - 1);
                HANSEN_KERNEL.put(key, val);
            }
            result = -(2. * s - 1.) / s * val;

        } else if (n == s && n >= 1) {
            key = new MNSKey(0, s - 1, s);
            if (HANSEN_KERNEL.containsKey(key)) {
                val = HANSEN_KERNEL.get(key);
            } else {
                val = computeHansenKernelPositiveSubscriptNullJ(s - 1, s);
                HANSEN_KERNEL.put(key, val);
            }
            result = (2d * s + 1d) / (s + 1d) * val;

        } else if (n >= s + 1 && n >= 2) {
            key = new MNSKey(0, n - 2, s);
            if (HANSEN_KERNEL.containsKey(key) && (n >= 2)) {
                val = HANSEN_KERNEL.get(key);
            } else {
                val = computeHansenKernelPositiveSubscriptNullJ(n - 2, s);
                HANSEN_KERNEL.put(key, val);
            }
            key = new MNSKey(0, n - 1, s);
            if (HANSEN_KERNEL.containsKey(key) && (n >= 1)) {
                val = HANSEN_KERNEL.get(key);
            } else {
                val = computeHansenKernelPositiveSubscriptNullJ(n - 1, s);
                HANSEN_KERNEL.put(key, val);
            }

            double val1 = (2d * n + 1d) / (n + 1d);
            double val2 = -(n + s) * (n - s) / (n * (n + 1d) * khi2);
            double knM1 = HANSEN_KERNEL.get(new MNSKey(0, n - 1, s));
            double knM2 = HANSEN_KERNEL.get(new MNSKey(0, n - 2, s));
            result = val1 * knM1 + val2 * knM2;
            HANSEN_KERNEL.put(new MNSKey(0, n, s), result);
        }
        return result;
    }

    /**
     * Compute dK<sub>0</sub><sup>n,s</sup> / d&chi; with positive subscript from Equation 3.2 - (3)
     * 
     * @param n
     *            n must be positive
     * @param s
     *            s
     * @return dK<sub>0</sub><sup>n,s</sup> / d&chi;
     * @throws OrekitException
     */
    private double getDkdXPositiveSubscriptJNull(int n,
                                                 int s) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
        final double khi2 = khi * khi;

        double result = 0d;
        if ((n == s - 1) || (n != s)) {
            HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(0, n, s), 0d);
        } else {
            MNSKey nM1 = new MNSKey(0, n - 1, s);
            MNSKey nM2 = new MNSKey(0, n - 2, s);
            double dKnM1;
            double dKnM2;
            double knM2;
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(nM1)) {
                getDkdXNegativeSubscript(n - 1, s);
            }
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(nM2)) {
                getDkdXNegativeSubscript(n - 2, s);
            }

            dKnM1 = HANSEN_KERNEL_DERIVATIVES.get(nM1);
            dKnM2 = HANSEN_KERNEL_DERIVATIVES.get(nM2);
            knM2 = getHansenKernelValue(0, n - 2, s);

            final double val1 = (2d * n + 1d) / (n + 1d) * dKnM1;
            final double val2 = -(n + s) * (n - s) / (n * (n + 1d) * khi2) * dKnM2;
            final double val3 = 2d * (n + s) * (n - s) / (n * (n + 1d) * khi2 * khi) * knM2;
            result = val1 + val2 + val3;
            HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(0, n, s), result);
        }
        return result;
    }

    /**
     * Compute the Hansen derivated coefficient for the resonnant tesseral harmonics from equation
     * 3.3 - (5)
     * 
     * <pre>
     * dK<sub>j</sub><sup>n,s</sup> / de<sup>2</sup>
     * 
     * </pre>
     * 
     * @param ecc
     * @param j
     * @param n
     * @param s
     * @param convergenceCriteria
     * @return
     * @throws OrekitException
     */
    private double getDkdXPositiveSubscript(final int j,
                                            final int n,
                                            final int s,
                                            final double convergenceCriteria) throws OrekitException {
        // Initialization :
        final double Kjns = computeKernelOfHansenCoefficientFromNewcomb(j, n, s);
        final double coeff = FastMath.pow(1 - eccentricity * eccentricity, n + 1.5);
        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);
        final double KjnsTerm = -((n + 1.5) / (1 - eccentricity * eccentricity)) * Kjns;

        double tmp = EPSILON + 1;
        int i = 1;
        double result = 0d;

        // Iteration over the modified Newcomb Operator
        while (Math.abs(tmp) > EPSILON) {
            final double newcomb = ModifiedNewcombOperators.getValue(i + a, i + b, n, s);
            tmp = i * newcomb * FastMath.pow(eccentricity, 2 * (i - 1));
            result += tmp;
            i++;
        }
        return KjnsTerm + coeff * result;
    }

    /**
     * Compute dK<sub>0</sub><sup>-n-1,s</sup> / d&chi; with negative subscript from Equation 3.1 -
     * (7)
     * 
     * <pre>
     * dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
     * 
     * </pre>
     * 
     * @param n
     *            n must be negative and equal to the wanted returned order -n-1
     * @param s
     *            s
     * @return dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
     * @throws OrekitException
     */
    private double getDkdXNegativeSubscript(final int n,
                                            final int s) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
        final double khi2 = khi * khi;
        double value = 0d;
        MNSKey key = new MNSKey(0, -n - 1, s);

        if (n == FastMath.abs(s)) {
            HANSEN_KERNEL_DERIVATIVES.put(key, 0d);
        } else if (n == FastMath.abs(s) + 1) {
            value = (1 + 2 * s) * FastMath.pow(khi, 2 * s) / FastMath.pow(2, s);
            HANSEN_KERNEL_DERIVATIVES.put(key, value);
        } else {
            MNSKey mN = new MNSKey(0, -n, s);
            MNSKey mNp1 = new MNSKey(0, -n + 1, s);
            double kMns;
            double KMnP1s;
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(mN)) {
                getDkdXNegativeSubscript(n - 1, s);
            }
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(mNp1)) {
                getDkdXNegativeSubscript(n - 2, s);
            }

            kMns = HANSEN_KERNEL_DERIVATIVES.get(mN);
            KMnP1s = HANSEN_KERNEL_DERIVATIVES.get(mNp1);
            double KMnM1s = getHansenKernelValue(0, -n - 1, s);

            value = (n - 1d) * khi2 * ((3d - 2 * n) * kMns - (2d - n) * KMnP1s) / ((n - s - 1d) * (1d - n - s)) + 2d * KMnM1s / khi;

            HANSEN_KERNEL_DERIVATIVES.put(key, value);
        }
        return value;
    }

    /**
     * Compute the K<sub>0</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3 - (6). The given
     * formula used to compute the coefficient when n = s + 1 seems to be wrong. Instead we used the
     * following formula to handle this case :
     * 
     * <pre>
     * if n = s + 1 -> K<sub>0</sub><sup>-n-1,s</sup> = (e/2)<sup>s</sup> * (1-e<sup>2</sup>)<sup>(2n-1)/2</sup>
     * </pre>
     * 
     * This equation is issue from the paper "The computation of tables of Hansen coefficients" from
     * s. Hughes, published in Celestial Mechanics 29 (1981) 101-107
     * 
     * @param n
     *            must be positive. For a given 'n', the K<sub>0</sub><sup>-n-1,s</sup> will be
     *            returned
     * @param s
     *            s value
     * @return K<sub>0</sub><sup>-n-1,s</sup>
     * @throws OrekitException
     */
    private double computeHansenKernelNegativeSubscribtNullJ(final int n,
                                                             final int s) throws OrekitException {
        double result;
        // Positive s value only for formula application. -s value equal to the s value by
        // definition
        int ss = s;
        if (s < 0) {
            ss = -s;
        }

        MNSKey key1 = new MNSKey(0, -n - 1, ss);
        MNSKey key2 = new MNSKey(0, -n - 1, -ss);

        if (HANSEN_KERNEL.containsKey(key1)) {
            result = HANSEN_KERNEL.get(key1);
            HANSEN_KERNEL.put(key2, result);
        } else if (HANSEN_KERNEL.containsKey(key2)) {
            result = HANSEN_KERNEL.get(key2);
            HANSEN_KERNEL.put(key1, result);

        } else {
            final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
            final double khi2 = khi * khi;
            double value;
            double kMns;
            double KMnP1s;
            if (n == ss && n >= 0) {
                value = 0d;
            } else if (n == (ss + 1) && n >= 1) {
                // value = FastMath.pow(khi, 1 + 2 * ss) / FastMath.pow(2, ss);
                // Replaced formula. See method documentation for further informations
                // TODO check this as the basic expression of hansen coeff is different in danielson
                // and Hughes......

                value = FastMath.pow(eccentricity * 0.5, ss) * FastMath.pow((1 - eccentricity * eccentricity), -(2d * n - 1d) / 2d);
            } else {
                if (!HANSEN_KERNEL.containsKey(new MNSKey(0, -n, ss))) {
                    computeHansenKernelNegativeSubscribtNullJ(n - 1, ss);
                }

                if (!HANSEN_KERNEL.containsKey(new MNSKey(0, -n + 1, ss))) {
                    computeHansenKernelNegativeSubscribtNullJ(n - 2, ss);
                }
                kMns = HANSEN_KERNEL.get(new MNSKey(0, -n, ss));
                KMnP1s = HANSEN_KERNEL.get(new MNSKey(0, -n + 1, ss));

                value = (n - 1d) * khi2 * ((2d * n - 3) * kMns - (n - 2d) * KMnP1s) / ((n + ss - 1d) * (n - ss - 1d));
            }
            // Add K(n, s) and K(n, -s) as they are symmetric
            HANSEN_KERNEL.put(key1, value);
            HANSEN_KERNEL.put(key2, value);

            result = value;
        }
        return result;
    }

    /**
     * Calcul tenant compte des bornes sur s et b afin de lever les ambiguités sur l'expression de
     * récurrence. Ne marche pas pour j = 0 !
     * 
     * <pre>
     * K<sub>j</sub><sup>-n-1,s</sup>
     * </pre>
     * 
     * @param ecc
     * @param j
     *            J resonant term
     * @param m
     *            m resonant term
     * @param nMax
     *            Maximum order for computation
     * @param convergenceCriteria
     *            convergence criteria for the infinite convergence series defined by 2.7.3 - (10)
     * @return the map containing every computed values
     * @throws OrekitException
     */
    private double computeHansenKernelNegativeSubscribtNonNullJ(final int j,
                                                                final int s,
                                                                final int n,
                                                                final double convergenceCriteria) throws OrekitException {

        final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
        final double khi2 = khi * khi;
        double value;
        double kMn, kMnP1, kMnP3;
        HANSEN_KERNEL.put(new MNSKey(j, -n, s), computeKernelOfHansenCoefficientFromNewcomb(j, -n, s));
        HANSEN_KERNEL.put(new MNSKey(j, -n + 1, s), computeKernelOfHansenCoefficientFromNewcomb(j, -n + 1, s));
        HANSEN_KERNEL.put(new MNSKey(j, -n + 3, s), computeKernelOfHansenCoefficientFromNewcomb(j, -n + 3, s));

        kMn = HANSEN_KERNEL.get(new MNSKey(j, -n, s));
        kMnP1 = HANSEN_KERNEL.get(new MNSKey(j, -n + 1, s));
        kMnP3 = HANSEN_KERNEL.get(new MNSKey(j, -n + 3, s));
        final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
        final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
        final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
        final double factorMnP3 = j * j * (1 - n);
        value = commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3);
        HANSEN_KERNEL.put(new MNSKey(j, n, s), value);
        return value;
    }

    /**
     * Compute the Hansen coefficient K<sub>j</sub><sup>ns</sup> used for the resonant tesseral
     * harmonics from equation 2.7.3 - (10). The coefficient value is evaluated from the
     * {@link ModifiedNewcombOperators} elements.
     * 
     * @param ecc
     * @param j
     * @param n
     * @param s
     * @param convergenceCriteria
     * @return
     * @throws OrekitException
     *             if the Newcomb operator cannot be computed with the current indexes
     */
    public double computeKernelOfHansenCoefficientFromNewcomb(final int j,
                                                              final int n,
                                                              final int s) throws OrekitException {
        final double coeff = FastMath.pow(1 - eccentricity * eccentricity, n + 1.5);
        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);

        double tmp = EPSILON + 1;
        int i = 0;

        double result = 0d;
        while (Math.abs(tmp) > EPSILON) {
            final double newcomb = ModifiedNewcombOperators.getValue(i + a, i + b, n, s);
            tmp = newcomb * FastMath.pow(eccentricity, 2 * i);
            result += tmp;
            i++;
        }
        return coeff * result;
    }

}
