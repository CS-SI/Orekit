package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;

public class HansenUtils {

    /** Equation 2.7.3 - (6) computed only from the recurrence formula */
    public static Map<NSKey, Double> computeHansenKernelForZonalHarmonicsReccurence(final int degreeMax,
                                                                                    final double ecc) {
        TreeMap<NSKey, Double> map = new TreeMap<NSKey, Double>();
        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;
        NSKey key;
        double value;
        for (int n = 0; n < degreeMax; n++) {
            for (int s = 0; s < n + 1; s++) {
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
     * Equation 3.1 - (7) computed only from the recurrence formula
     * 
     * <pre>
     * dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
     * 
     * </pre>
     * 
     * @throws OrekitException
     */
    public static Map<NSKey, Double> computeDerivativeOfHansenKernelForZonalHarmonicsReccurence(final int degreeMax,
                                                                                                final double ecc,
                                                                                                final Map<NSKey, Double> Kns) throws OrekitException {
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
                    // TODO make it nicer !
                    double KMnM1s = Kns.get(new NSKey(-n - 1, s));/*
                                                                   * computeKernelOfHansenCoefficient(
                                                                   * ecc, 0, -n-1, s, 5);
                                                                   */

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
     * @param Kns
     *            Kernels of Hansen coefficients from equation 2.7.3 - (6)
     * @param B
     *            B coefficient defined by 2.1.6 - (1b)
     */
    public static double[][] computeKnsDerivativesOLD(final double[][] Kns,
                                                      final double B) {
        // Initialization :
        double[][] dKns = new double[Kns.length][];
        for (int i = 0; i < Kns.length; i++) {
            dKns[i] = new double[i + 1];
        }

        final double khi = 1 / B;
        final double khi2 = khi * khi;

        // Compute coefficients
        for (int n = 0; n < Kns.length; n++) {
            for (int s = 0; s < n; s++) {
                if (n == s + 1 && n >= 1) {
                    dKns[n][s] = (1 + 2 * s) * FastMath.pow(khi, 2 * s) / FastMath.pow(2, s);
                } else if (n > s + 1) {
                    dKns[n][s] = ((n - 1) * khi2 / ((n + s - 1) * (n - s + 1)))
                                    * ((2 * n - 3) * dKns[FastMath.abs(-n + 1)][s] - (n - 2) * dKns[FastMath.abs(-n + 2)][s]) + 2
                                    * Kns[n][s] / khi;
                }
            }
        }
        return dKns;
    }

    /**
     * Derivative of the Kernels of Hansen coefficients from equation 3.1-(7)
     * 
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
     * @throws OrekitException
     *             if the Newcomb operator cannot be computed with the current indexes
     */
    public static double computeKernelOfHansenCoefficientFromNewcomb(final double ecc,
                                                                     final int j,
                                                                     final int n,
                                                                     final int s,
                                                                     final int computationOrder) throws OrekitException {
        final double coeff = FastMath.pow(1 - ecc * ecc, n + 1.5);
        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);

        // TODO check a, b value from both expressions
        // New definition of a, b
        final int a2 = (FastMath.abs(j - s) + (j - s)) / 2;
        final int b2 = (FastMath.abs(j - s) - (j - s)) / 2;
        // System.out.println(a2 + " " + b2 + " " + ecc);

        double result = 0d;
        for (int i = 0; i < computationOrder; i++) {
             System.out.println("index " + i + " rho " + (i + a) + " sigma " + (i + b) + " n " +n + " s " + s );
            final double newcomb = ModifiedNewcombOperators.getValue(i + a, i + b, n, s);
            result += newcomb * FastMath.pow(ecc, 2 * i);;
//            System.out.println(i + " " + newcomb * FastMath.pow(ecc, 2 * i));
        }
        return coeff * result;
    }

    public static double computeKernelOfHansenCoefficient(final double ecc,
                                                          final int j,
                                                          final int n,
                                                          final int s,
                                                          final int computationOrder) throws OrekitException {
        final double kMn = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n, s, computationOrder);
        final double kMnP1 = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 1, s, computationOrder);
        final double kMnP3 = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, -n + 3, s, computationOrder);

        final double khi = 1 / FastMath.sqrt(1 - ecc * ecc);
        final double khi2 = khi * khi;

        final double commonFactor = khi2 / ((3 - n) * (1 - n + s) * (1 - n - s));
        final double factorMn = (3 - n) * (1 - n) * (3 - 2 * n);
        final double factorMnP1 = -(2 - n) * ((3 - n) * (1 - n) + 2 * j * s / khi);
        final double factorMnP3 = j * j * (1 - n);

        return commonFactor * (factorMn * kMn + factorMnP1 * kMnP1 + factorMnP3 * kMnP3);
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
     * @throws OrekitException
     */
    public static double computeDerivativedKernelOfHansenCoefficient(final double ecc,
                                                                     final int j,
                                                                     final int n,
                                                                     final int s,
                                                                     final int computationOrder) throws OrekitException {
        // Initialization :
        final double Kjns = computeKernelOfHansenCoefficientFromNewcomb(ecc, j, n, s, computationOrder);
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
