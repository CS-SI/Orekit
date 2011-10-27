package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.MNSKey;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;

/**
 * @author rdicosta
 */
public class HansenUtils {

    private static TreeMap<MNSKey, Double> HANSEN_KERNEL             = new TreeMap<CoefficientFactory.MNSKey, Double>();

    private static TreeMap<MNSKey, Double> HANSEN_KERNEL_DERIVATIVES = new TreeMap<CoefficientFactory.MNSKey, Double>();

    private static double                  eccentricity              = 0d;

    static {
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(0, 0, 0), 0d);
    }

    /**
     * Equation 2.7.3 - (6) computed only from the recurrence formula
     * 
     * <pre>
     * K<sub>0</sub><sup>-n-1,s</sup>
     * </pre>
     * 
     * @param ecc
     * @param nMax
     * @return
     */
    @Deprecated
    public static Map<NSKey, Double> computeHansenKernelForZonalHarmonicsReccurence(final double ecc,
                                                                                    final int nMax) {
        TreeMap<NSKey, Double> map = new TreeMap<NSKey, Double>();
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        NSKey key;
        double value;

        for (int s = -nMax; s < nMax; s++) {
            final int currentN = FastMath.max(2, FastMath.abs(s));
            for (int n = currentN; n < nMax; n++) {
                key = new NSKey(-n - 1, s);
                if (n == s && n >= 0) {
                    value = 0d;
                } else if (n == (s + 1) && n >= 1) {
                    value = FastMath.pow(khi, 1 + 2 * s) / FastMath.pow(2, s);
                } else {
                    double kMns = map.get(new NSKey(-n, s));
                    double KMnP1s = map.get(new NSKey(-n + 1, s));
                    value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s) / ((n + s - 1) * (n - s - 1));
                }
                map.put(key, value);
            }

        }
        return map;
    }

    /**
     * J = 0, m = 0
     * 
     * @param ecc
     * @param nMax
     * @return
     * @throws OrekitException
     */
    public static Map<MNSKey, Double> computeHansenKernelForZonalHarmonics(final double ecc,
                                                                           final int nMax,
                                                                           final double convergenceCriteria) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        MNSKey key;
        double value;

        for (int s = -nMax; s < nMax; s++) {
            final int currentN = FastMath.max(2, FastMath.abs(s));
            for (int n = currentN; n < nMax; n++) {
                // j = 0
                key = new MNSKey(0, -n - 1, s);
                if (n == FastMath.abs(s) && n >= 0) {
                    value = 0d;
                } else if (n == (FastMath.abs(s) + 1) && n >= 1) {
                    value = FastMath.pow(khi, 1 + 2 * s) / FastMath.pow(2, s);
                } else {
                    double kMns = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n, s, convergenceCriteria);
                    double KMnP1s = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n + 1, s, convergenceCriteria);
                    value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s) / ((n + s - 1) * (n - s - 1));
                }
                HANSEN_KERNEL.put(key, value);
            }

        }
        return HANSEN_KERNEL;
    }

    public static double computeHansenKernelReccurenceCALL(final double ecc,
                                                           final int n,
                                                           final int s) throws OrekitException {
        double result;
        // Positive s value only for formula application. -s value equal to the s value by
        // definition
        int ss = s;
        if (s < 0) {
            ss = -s;
        }

        MNSKey keyCheck1 = new MNSKey(0, -n - 1, ss);
        MNSKey keyCheck2 = new MNSKey(0, -n - 1, -ss);

        if (HANSEN_KERNEL.containsKey(keyCheck1)) {
            result = HANSEN_KERNEL.get(new MNSKey(0, -n - 1, ss));
            HANSEN_KERNEL.put(keyCheck2, result);
        } else if (HANSEN_KERNEL.containsKey(keyCheck2)) {
            result = HANSEN_KERNEL.get(new MNSKey(0, -n - 1, -ss));
            HANSEN_KERNEL.put(keyCheck1, result);

        } else {
            final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
            final double khi2 = khi * khi;
            double value;
            double kMns;
            double KMnP1s;

            MNSKey key1 = new MNSKey(0, -n - 1, ss);
            MNSKey key2 = new MNSKey(0, -n - 1, -ss);

            if (n == ss && n >= 0) {
                value = 0d;
            } else if (n == (ss + 1) && n >= 1) {
                // value = FastMath.pow(khi, 1 + 2 * ss) / FastMath.pow(2, ss);
                value = FastMath.pow(ecc * 0.5, ss) * FastMath.pow((1 - ecc * ecc), -(2 * n - 1) / 2d);
            } else {
                if (!HANSEN_KERNEL.containsKey(new MNSKey(0, -n, ss))) {
                    computeHansenKernelReccurenceCALL(ecc, n - 1, ss);
                }

                if (!HANSEN_KERNEL.containsKey(new MNSKey(0, -n + 1, ss))) {
                    computeHansenKernelReccurenceCALL(ecc, n - 2, ss);
                }
                kMns = HANSEN_KERNEL.get(new MNSKey(0, -n, ss));
                KMnP1s = HANSEN_KERNEL.get(new MNSKey(0, -n + 1, ss));

                value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s) / ((n + ss - 1) * (n - ss - 1));
            }
            // Add K(-n-1, s) and K(-n-1, -s) as they are symetric
            HANSEN_KERNEL.put(key1, value);
            HANSEN_KERNEL.put(key2, value);

            result = value;
        }
        return result;
    }

    private static void initializeHansen(double ecc) {
        if (Double.compare(ecc, eccentricity) != 0 || HANSEN_KERNEL.size() == 0) {
            HANSEN_KERNEL.clear();
            final double xM20 = Math.pow((1 - ecc * ecc), -0.5);
            final double xM21 = 0d;
            final double xM30 = Math.pow((1 - ecc * ecc), -1.5);
            final double xM31 = ecc / 2d * Math.pow((1 - ecc * ecc), -1.5);
            // n = 0, m = 0
            HANSEN_KERNEL.put(new MNSKey(0, -1, 0), xM20);
            // n = 1, m = 0
            HANSEN_KERNEL.put(new MNSKey(0, -2, 0), xM20);
            // n = 1, m = 1
            HANSEN_KERNEL.put(new MNSKey(0, -2, 1), xM21);
            // n = 2, m = 0
            // HANSEN_KERNEL.put(new MNSKey(0, -3, 0), xM30);
            // // n = 2, m = 1
            // HANSEN_KERNEL.put(new MNSKey(0, -3, 1), xM31);
            eccentricity = ecc;
        }
    }

    public static Map<MNSKey, Double> computeDerivative(final double ecc,
                                                        final int n,
                                                        final int s,
                                                        final double convergenceCriteria) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        double value;
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
                computeDerivative(ecc, n - 1, s, convergenceCriteria);
            }
            if (!HANSEN_KERNEL_DERIVATIVES.containsKey(mNp1)) {
                computeDerivative(ecc, n - 2, s, convergenceCriteria);
            }

            kMns = HANSEN_KERNEL_DERIVATIVES.get(mN);
            KMnP1s = HANSEN_KERNEL_DERIVATIVES.get(mNp1);
            double KMnM1s = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n - 1, s, convergenceCriteria);

            value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s + 2 * KMnM1s / khi) / ((n + s - 1) * (n - s + 1));

            HANSEN_KERNEL_DERIVATIVES.put(key, value);
        }
        return HANSEN_KERNEL_DERIVATIVES;
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
    @Deprecated
    public static Map<NSKey, Double> computeDerivativeOfHansenKernelForZonalHarmonicsReccurence(final int degreeMax,
                                                                                                final double ecc,
                                                                                                final double convergenceCriteria) throws OrekitException {
        TreeMap<NSKey, Double> map = new TreeMap<NSKey, Double>();
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        NSKey key;
        double value;
        for (int n = 0; n < degreeMax; n++) {
            for (int s = 0; s < n + 1; s++) {
                key = new NSKey(-n - 1, s);
                if (n == s) {
                    value = 0d;
                } else if (n == (s + 1)) {
                    value = (1 + 2 * s) * FastMath.pow(khi, 2 * s) / FastMath.pow(2, s);
                } else {
                    double kMns = map.get(new NSKey(-n, s));
                    double KMnP1s = map.get(new NSKey(-n + 1, s));
                    double KMnM1s = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n - 1, s, convergenceCriteria);

                    value = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s + 2 * KMnM1s / khi) / ((n + s - 1) * (n - s + 1));
                }
                map.put(key, value);
            }

        }
        return map;
    }

    /**
     * Kernels of Hansen coefficients from equation 2.7.3 - (6).
     * 
     * @param n
     *            n value
     * @param s
     *            s value
     * @param ecc
     *            eccentricity
     * @param convergenceOrder
     * @return
     * @throws OrekitException
     */
    public static double computeKernelOfHansenForZonalHarmonics(final double ecc,
                                                                final int n,
                                                                final int s,
                                                                final int convergenceOrder) throws OrekitException {
        double result;
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;

        if (n == s && n >= 0) {
            result = 0d;
        } else if (n == (s + 1) && n >= 1) {
            result = FastMath.pow(khi, 1 + 2 * s) / FastMath.pow(2, s);
        } else {
            final double kMns = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n, s, convergenceOrder);
            final double KMnP1s = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n + 1, s, convergenceOrder);
            result = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s) / ((n + s - 1) * (n - s - 1));
        }
        return result;
    }

    /**
     * Derivative of the Kernels of Hansen coefficients from equation 3.1-(7)
     * 
     * @param n
     * @param s
     * @param ecc
     * @param computationOrder
     * @return
     * @throws OrekitException
     */
    public static double computeKnsDerivatives(final int n,
                                               final int s,
                                               final double ecc,
                                               final int computationOrder) throws OrekitException {

        double result;
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;

        if (n == s) {
            result = 0d;
        } else if (n == (s + 1)) {
            result = (1 + 2 * s) * FastMath.pow(khi, 2 * s) / FastMath.pow(2, s);
        } else {
            final double kMns = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n, s, computationOrder);
            final double KMnP1s = computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n + 1, s, computationOrder);
            result = (n - 1) * khi2 * ((2 * n - 3) * kMns - (n - 2) * KMnP1s) / ((n + s - 1) * (n - s - 1));
        }
        return result;
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
    public static double computeKernelOfHansenCoefficientFromNewcomb(final double ecc,
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

    /**
     * Equation 2.7.3 - (9) computed from the general recurrence formula. Deprecated because
     * indetermination for (n == 3) || (n == s + 1) || (n == -s + 1)
     * 
     * <pre>
     * K<sub>j</sub><sup>-n-1,s</sup>
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
    @Deprecated
    public static double computeKernelOfHansenCoefficient(final double ecc,
                                                          final int j,
                                                          final int n,
                                                          final int s,
                                                          final double convergenceCriteria) throws OrekitException {
        final double kMn = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n, s, convergenceCriteria);
        final double kMnP1 = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 1, s, convergenceCriteria);
        final double kMnP3 = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 3, s, convergenceCriteria);

        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;

        if ((n == 3) || (n == s + 1) || (n == -s + 1)) {
            // System.out.println("error equation 2.7.3 - 9 undefined for : " + n + "  " + s );
            return 0d;
        }
        final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
        final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
        final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
        final double factorMnP3 = j * j * (1 - n);

        return commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3);
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
    public static Map<MNSKey, Double> computeKernelOfHansenCoefficientNEW(final double ecc,
                                                                          final int j,
                                                                          final int m,
                                                                          final int nMax,
                                                                          final double convergenceCriteria) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;

        // For non resonant term, use the special formulation defined by equation 2.7.3 - (6)
        if (j == 0) {
            computeHansenKernelForZonalHarmonics(ecc, nMax, convergenceCriteria);
        } else {

            for (int s = -nMax; s < nMax; s++) {
                int currentN = FastMath.max(2, m);
                currentN = FastMath.max(currentN, FastMath.abs(s));
                for (int n = currentN; n < nMax; n++) {
                    MNSKey key = new MNSKey(j, -n - 1, s);
                    if (!HANSEN_KERNEL.containsKey(key)) {
                        final double kMn = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n, s, convergenceCriteria);
                        final double kMnP1 = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 1, s, convergenceCriteria);
                        final double kMnP3 = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 3, s, convergenceCriteria);
                        final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
                        final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
                        final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
                        final double factorMnP3 = j * j * (1 - n);
                        // System.out.println(key);
                        HANSEN_KERNEL.put(key, commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3));
                    }
                }
            }
        }
        return HANSEN_KERNEL;
    }

    public static double computeKernelOfHansenCoefficientRecurssif(final double ecc,
                                                                   final int j,
                                                                   final int n,
                                                                   final int s,
                                                                   final double convergenceCriteria) throws OrekitException {
        // initializeDerivatives(ecc, j, s);
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        double value;
        double kMn, kMnP1, kMnP3;
        HANSEN_KERNEL.put(new MNSKey(j, -1, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -1, s, convergenceCriteria));
        HANSEN_KERNEL.put(new MNSKey(j, -2, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -2, s, convergenceCriteria));
        HANSEN_KERNEL.put(new MNSKey(j, -3, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -3, s, convergenceCriteria));
        HANSEN_KERNEL.put(new MNSKey(j, -4, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -4, s, convergenceCriteria));

        // For non resonant term, use the special formulation defined by equation 2.7.3 - (6)

        MNSKey key = new MNSKey(j, -n - 1, s);
        if (!HANSEN_KERNEL.containsKey(key)) {

            if (!HANSEN_KERNEL.containsKey(new MNSKey(j, -n, s))) {
                computeKernelOfHansenCoefficientRecurssif(ecc, j, n - 1, s, convergenceCriteria);
            }
            if (!HANSEN_KERNEL.containsKey(new MNSKey(j, -n + 1, s))) {
                computeKernelOfHansenCoefficientRecurssif(ecc, j, n - 2, s, convergenceCriteria);
            }
            if (!HANSEN_KERNEL.containsKey(new MNSKey(j, -n + 3, s))) {
                computeKernelOfHansenCoefficientRecurssif(ecc, j, n - 4, s, convergenceCriteria);
            }

            kMn = HANSEN_KERNEL.get(new MNSKey(j, -n, s));
            kMnP1 = HANSEN_KERNEL.get(new MNSKey(j, -n + 1, s));
            kMnP3 = HANSEN_KERNEL.get(new MNSKey(j, -n + 3, s));
            final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
            final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
            final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
            final double factorMnP3 = j * j * (1 - n);
            // System.out.println(key);
            value = commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3);
            if (Double.isInfinite(value)) {
                System.out.println(key);
            }
            HANSEN_KERNEL.put(key, value);

        } else {
            value = HANSEN_KERNEL.get(new MNSKey(j, n, s));
        }
        return value;
    }

    public static double computeKernel(final double ecc,
                                       final int j,
                                       final int n,
                                       final int s,
                                       final double convergenceCriteria) throws OrekitException {
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        double value;
        double kMn, kMnP1, kMnP3;
        HANSEN_KERNEL.put(new MNSKey(j, -n, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n, s, convergenceCriteria));
        HANSEN_KERNEL.put(new MNSKey(j, -n + 1, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 1, s, convergenceCriteria));
        HANSEN_KERNEL.put(new MNSKey(j, -n + 3, s), computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 3, s, convergenceCriteria));

        kMn = HANSEN_KERNEL.get(new MNSKey(j, -n, s));
        kMnP1 = HANSEN_KERNEL.get(new MNSKey(j, -n + 1, s));
        kMnP3 = HANSEN_KERNEL.get(new MNSKey(j, -n + 3, s));
        final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
        final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
        final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
        final double factorMnP3 = j * j * (1 - n);
        // System.out.println(key);
        value = commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3);
        return value;
    }

    private static void initializeDerivatives(double ecc,
                                              int s,
                                              int j) throws OrekitException {
        final double kMn = computeHansenKernelReccurenceCALL(ecc, 2, s);
        final double kMnP1 = computeHansenKernelReccurenceCALL(ecc, 3, s);
        final double kMnP3 = computeHansenKernelReccurenceCALL(ecc, 5, s);
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(j, 2, s), kMn);
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(j, 3, s), kMnP1);
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(j, 5, s), kMnP3);

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
    public static double computeDerivativedKernelOfHansenCoefficient(final double ecc,
                                                                     final int j,
                                                                     final int n,
                                                                     final int s,
                                                                     final double convergenceCriteria) throws OrekitException {
        // Initialization :
        final double Kjns = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, n, s, convergenceCriteria);
        final double coeff = FastMath.pow(1 - ecc * ecc, n + 1.5);
        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);
        final double KjnsTerm = -((n + 1.5) / (1 - ecc * ecc)) * Kjns;

        // Iteration over the modified Newcomb Operator
        double result = 0d;
        for (int i = 1; i < n; i++) {
            result += i * ModifiedNewcombOperators.getValue(i + a, i + b, n, s) * FastMath.pow(ecc, 2 * (i - 1));
        }
        return KjnsTerm + coeff * result;
    }

}
