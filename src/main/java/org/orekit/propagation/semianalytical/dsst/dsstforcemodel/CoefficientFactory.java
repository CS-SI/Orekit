package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math.util.MathUtils;

public class CoefficientFactory {

    private static double[][]                      Vns               = new double[][] {};

    private static Map<QnsKey, PolynomialFunction> QnsDerivativesMap = new TreeMap<QnsKey, PolynomialFunction>();

    /** Get the Qns value from 2.8.1-(4) */
    public static double getQnsPolynomialValue(final int n,
                                               final int s,
                                               final double gamma) {
        PolynomialFunction derivative;
        if (QnsDerivativesMap.containsKey(new QnsKey(n, s))) {
            derivative = QnsDerivativesMap.get(new QnsKey(n, s));
        } else {
            PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(n);
            derivative = legendre;
            for (int i = 0; i < s; i++) {
                derivative = (PolynomialFunction) derivative.derivative();
            }
            QnsDerivativesMap.put(new QnsKey(n, s), derivative);
        }
        return derivative.value(gamma);
    }

    /**
     * Q<sub>ns</sub> array coefficient from 2.8.3-(2)
     */
    public static double[][] computeQnsCoefficient(final int order,
                                                   final double gamma) {
        // Initialization
        double[][] Qns = new double[order][];
        for (int i = 0; i < order; i++) {
            Qns[i] = new double[i + 1];
        }
        // first element
        Qns[0][0] = 1;

        for (int n = 1; n < order; n++) {
            for (int s = 0; s <= n; s++) {
                if (n == s) {
                    Qns[n][s] = (2 * s - 1) * Qns[s - 1][s - 1];
                } else if (n == s + 1) {
                    Qns[n][s] = (2 * s + 1) * gamma * Qns[s][s];
                } else if (n > s + 1) {
                    Qns[n][s] = ((2 * n - 1) * gamma * Qns[n - 1][s] - (n + s - 1) * Qns[n - 2][s]) / (n - s);
                }
            }
        }
        return Qns;
    }

    /**
     * Compute G<sub>s</sub> and H<sub>s</sub> polynomial from equation 3.1-(5)
     * 
     * @param ex
     *            x-component of the eccentricity vector (or k component in D.A Danielson notation)
     * @param ey
     *            y-component of the eccentricity vector (or h component in D.A Danielson notation)
     * @return Array of G<sub>s</sub> and H<sub>s</sub> polynomial. First column contains the
     *         G<sub>s</sub> values and the second column containt the H<sub>s</sub> values
     */
    public static double[][] computeGsHsCoefficient(final double ex,
                                                    final double ey,
                                                    final double alpha,
                                                    final double beta,
                                                    final int order) {
        // TODO this coefficient can also be computed by direct relation 3.1-(5) Check for
        // performances...
        // Initialization
        double[][] GsHs = new double[2][order];
        // First Gs coefficient
        GsHs[0][0] = 1;
        // First Hs coefficient
        GsHs[1][0] = 0;

        for (int s = 1; s < order; s++) {
            // Gs coefficient :
            GsHs[0][s] = (ey * alpha + ex * beta) * GsHs[0][s - 1] - (ex * alpha - ey * beta) * GsHs[1][s - 1];
            // Hs coefficient
            GsHs[1][s] = (ex * alpha - ey * beta) * GsHs[0][s - 1] + (ey * alpha + ex * beta) * GsHs[1][s - 1];
        }

        return GsHs;
    }
    
    /** Compute the V<sub>n, s</sub> coefficient from 2.8.2 - (1)(2) */
    public static double[][] computeVnsCoefficient(final int order) {
        if (order > Vns.length) {
            // Initialization :
            Vns = new double[order][];
            for (int i = 0; i < order; i++) {
                Vns[i] = new double[i + 1];
            }
            Vns[0] = new double[1];
            Vns[0][0] = 1;

            // Compute coefficient
            for (int n = 0; n < order; n++) {
                for (int s = 0; s < n + 1; s++) {
                    // s = n
                    if (n == s && (s + 1) < order) {
                        Vns[s + 1][s + 1] = Vns[s][s] / (2 * s + 2d);
                    }
                    // otherwise
                    if ((n + 2) < order && (n + 2 - s) % 2 == 0) {
                        Vns[n + 2][s] = (-n + s - 1) / (n + s + 2d) * Vns[n][s];
                    }
                }
            }
        }
        return Vns;
    }

    /** Initialize the V<sub>n, s</sub> <sup>m</sup> coefficient equation 2.8.2 - (1) */
    public static double getVmsn(final int m,
                                 final int n,
                                 final int s) {
        if (n > Vns.length) {
            // Update the Vns coefficient
            computeVnsCoefficient(n);
        }
        // If (n -s) is odd, the Vmsn coefficient is null
        double result = 0;
        if ((n - s) % 2 == 0) {
            result = MathUtils.factorial(n + s) / MathUtils.factorial(n - m) * Vns[n - 1][s - 1];
        }
        return result;
    }

    private static class QnsKey implements Comparable<QnsKey> {

        final int n;

        final int s;

        public QnsKey(final int n,
                      final int s) {
            this.n = n;
            this.s = s;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(QnsKey key) {
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

}
