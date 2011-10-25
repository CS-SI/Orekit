package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.MNSKey;

public class HansenCoefficients {

    private static TreeMap<MNSKey, Double> HANSEN_KERNEL             = new TreeMap<CoefficientFactory.MNSKey, Double>();

    private static TreeMap<MNSKey, Double> HANSEN_KERNEL_DERIVATIVES = new TreeMap<CoefficientFactory.MNSKey, Double>();

    private final double                   eccentricity;

    private final static double            EPSILON                   = 1e-3;

    public HansenCoefficients(final double ecc) {
        this.eccentricity = ecc;
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(0, 0, 0), 0d);
    }

    public double getHansenKernelValue(final int m,
                                       final int j,
                                       final int n,
                                       final int s) throws OrekitException {
        double value;
        if (j == 0 && n >= 0) {
            // Compute the K0ns coefficients, used for the third body description
            value = 0d;
            // TODO
        } else if (j == 0 && n < 0) {
            value = computeHansenKernelNegativeSubscribt(-n - 1, s);
        } else {
            computeKernelOfHansenCoefficient(j, m, n, EPSILON);
            value = HANSEN_KERNEL.get(new MNSKey(j, n, s));
        }
        return value;
    }

    /**
     * Equation 3.1 - (7) computed only from the recurrence formula
     * 
     * <pre>
     * dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
     * 
     * </pre>
     * 
     * @param degreeMax
     * @param ecc
     * @param convergenceCriteria
     * @return
     * @throws OrekitException
     */
    public double getDkdX(final int n,
                          final int s) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
        final double khi2 = khi * khi;
        double value = 0d;
        MNSKey key = new MNSKey(0, -n - 1, s);

        if (FastMath.abs(n) == FastMath.abs(s)) {
            HANSEN_KERNEL_DERIVATIVES.put(key, 0d);
        } else if (FastMath.abs(n) == FastMath.abs(s) + 1) {
            value = (1 + 2 * s) * FastMath.pow(khi, 2 * s) / FastMath.pow(2, s);
            HANSEN_KERNEL_DERIVATIVES.put(key, value);
        } else {
            MNSKey mN = new MNSKey(0, -n, s);
            MNSKey mNp1 = new MNSKey(0, -n + 1, s);
            double kMns;
            double KMnP1s;
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(mN)) {
                getDkdX(n - 1, s);
            }
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(mNp1)) {
                getDkdX(n - 2, s);
            }

            kMns = HANSEN_KERNEL_DERIVATIVES.get(mN);
            KMnP1s = HANSEN_KERNEL_DERIVATIVES.get(mNp1);
            double KMnM1s = computeHansenKernelNegativeSubscribt(-n - 1, s);

            value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s + 2 * KMnM1s / khi) / ((n + s - 1) * (n - s + 1));

            HANSEN_KERNEL_DERIVATIVES.put(key, value);
        }
        return value;
    }

    /**
     * Equation 2.7.3 - (6) computed only from the recurrence formula
     * 
     * <pre>
     * K<sub>0</sub><sup>-n-1,s</sup>
     * </pre>
     * 
     * @param n
     *            must be negative
     * @param s
     *            s value
     * @return K<sub>0</sub><sup>-n-1,s</sup>
     * @throws OrekitException
     */
    private double computeHansenKernelNegativeSubscribt(final int n,
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
            result = HANSEN_KERNEL.get(new MNSKey(0, n, ss));
            HANSEN_KERNEL.put(key2, result);
        } else if (HANSEN_KERNEL.containsKey(key2)) {
            result = HANSEN_KERNEL.get(new MNSKey(0, n, -ss));
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
                value = FastMath.pow(eccentricity * 0.5, ss) * FastMath.pow((1 - eccentricity * eccentricity), -(2 * n - 1) / 2d);
            } else {
                if (!HANSEN_KERNEL.containsKey(new MNSKey(0, -n, ss))) {
                    computeHansenKernelNegativeSubscribt(n - 1, ss);
                }

                if (!HANSEN_KERNEL.containsKey(new MNSKey(0, -n + 1, ss))) {
                    computeHansenKernelNegativeSubscribt(n - 2, ss);
                }
                kMns = HANSEN_KERNEL.get(new MNSKey(0, -n, ss));
                KMnP1s = HANSEN_KERNEL.get(new MNSKey(0, -n + 1, ss));

                value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s) / ((n + ss - 1) * (n - ss - 1));
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
    private Map<MNSKey, Double> computeKernelOfHansenCoefficient(final int j,
                                                                 final int m,
                                                                 final int nMax,
                                                                 final double convergenceCriteria) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - eccentricity * eccentricity);
        final double khi2 = khi * khi;

        for (int s = -nMax; s < nMax; s++) {
            int currentN = FastMath.max(2, m);
            currentN = FastMath.max(currentN, FastMath.abs(s));
            for (int n = currentN; n < nMax; n++) {
                MNSKey key = new MNSKey(j, -n - 1, s);
                if (!HANSEN_KERNEL.containsKey(key)) {
                    final double kMn = computeKernelOfHansenCoefficientFromNewcomb(eccentricity, j, -n, s, convergenceCriteria);
                    final double kMnP1 = computeKernelOfHansenCoefficientFromNewcomb(eccentricity, j, -n + 1, s, convergenceCriteria);
                    final double kMnP3 = computeKernelOfHansenCoefficientFromNewcomb(eccentricity, j, -n + 3, s, convergenceCriteria);
                    final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
                    final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
                    final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
                    final double factorMnP3 = j * j * (1 - n);
                    // System.out.println(key);
                    HANSEN_KERNEL.put(key, commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3));
                }
            }
        }
        return HANSEN_KERNEL;
    }

    /**
     * Compute the Hansen coefficient K<sub>j</sub><sup>ns</sup> for the resonnant tesseral
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
    private double computeKernelOfHansenCoefficientFromNewcomb(final double ecc,
                                                               final int j,
                                                               final int n,
                                                               final int s,
                                                               final double convergenceCriteria) throws OrekitException {
        final double coeff = FastMath.pow(1 - ecc * ecc, n + 1.5);
        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);

        double tmp = convergenceCriteria + 1;
        int i = 0;

        double result = 0d;
        while (Math.abs(tmp) > convergenceCriteria) {
            final double newcomb = ModifiedNewcombOperators.getValue(i + a, i + b, n, s);
            tmp = newcomb * FastMath.pow(ecc, 2 * i);
            result += tmp;
            i++;
        }
        return coeff * result;
    }

}
