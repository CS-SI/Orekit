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

import org.apache.commons.math3.analysis.UnivariateVectorFunction;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.errors.OrekitException;

/** Class used for {@link #integrate(UnivariateVectorFunction, double, double) integrating}
 *  a {@link org.apache.commons.math3.analysis.UnivariateVectorFunction function}
 *  for the orbital elements using the Gaussian quadrature rule.
 *  <p>
 *  This class has been adapted to the specific needs of the DSST from
 *  the org.apache.commons.math3.analysis.integration.gauss package.
 *  </p>
 *
 *  @author Pascal Parraud
 */
public class GaussQuadrature {

    /** Node points. */
    private final double[] nodePoints;

    /** Node weights. */
    private final double[] nodeWeights;

    /** Simple constructor.
     *  <p>
     *  Creates a Gauss integrator of the given order.
     *  </p>
     *
     *  @param numberOfPoints Order of the integration rule.
     */
    public GaussQuadrature(final int numberOfPoints) {

        if (numberOfPoints <= 0) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.NUMBER_OF_POINTS, numberOfPoints);
        }

        this.nodePoints  = new double[numberOfPoints];
        this.nodeWeights = new double[numberOfPoints];
        computePointsAndWeights(numberOfPoints, nodePoints, nodeWeights);

    }

    /** Integrates a given function on the given interval.
     *
     *  @param f Function to integrate.
     *  @param lowerBound Lower bound of the integration interval.
     *  @param upperBound Upper bound of the integration interval.
     *  @return the integral of the weighted function.
     */
    public double[] integrate(final UnivariateVectorFunction f,
                              final double lowerBound, final double upperBound) {

        final double[] adaptedPoints  = nodePoints.clone();
        final double[] adaptedWeights = nodeWeights.clone();
        transform(adaptedPoints, adaptedWeights, lowerBound, upperBound);
        return basicIntegrate(f, adaptedPoints, adaptedWeights);
    }

    /** Computes points and weights for the given quadrature order.
     *
     *  @param numberOfPoints quadrature order.
     *  @param points points for quadrature.
     *  @param weights weights for quadrature.
     */
    private void computePointsAndWeights(final int numberOfPoints,
                                         final double[] points,
                                         final double[] weights) {

        if (numberOfPoints == 1) {
            points[0]  = 0d;
            weights[0] = 2d;
            return;
        }

        // Get previous points and weights (recursive call).
        final int previousOrder = numberOfPoints - 1;
        final double[] previousPoints  = new double[previousOrder];
        final double[] previousWeights = new double[previousOrder];
        computePointsAndWeights(previousOrder, previousPoints, previousWeights);

        // Find i-th root of P[n+1] by bracketing.
        final int iMax = numberOfPoints / 2;
        for (int i = 0; i < iMax; i++) {
            // Lower-bound of the interval.
            double a = (i == 0) ? -1 : previousPoints[i - 1];
            // Upper-bound of the interval.
            double b = (iMax == 1) ? 1 : previousPoints[i];
            // P[j-1](a)
            double pma = 1;
            // P[j](a)
            double pa = a;
            // P[j-1](b)
            double pmb = 1;
            // P[j](b)
            double pb = b;
            for (int j = 1; j < numberOfPoints; j++) {
                final int two_j_p_1 = 2 * j + 1;
                final int j_p_1 = j + 1;
                // P[j+1](a)
                final double ppa = (two_j_p_1 * a * pa - j * pma) / j_p_1;
                // P[j+1](b)
                final double ppb = (two_j_p_1 * b * pb - j * pmb) / j_p_1;
                pma = pa;
                pa = ppa;
                pmb = pb;
                pb = ppb;
            }
            // Now pa = P[n+1](a), and pma = P[n](a) (same holds for b).
            // Middle of the interval.
            double c = 0.5 * (a + b);
            // P[j-1](c)
            double pmc = 1;
            // P[j](c)
            double pc = c;
            boolean done = false;
            while (!done) {
                done = b - a <= Math.ulp(c);
                pmc = 1;
                pc = c;
                for (int j = 1; j < numberOfPoints; j++) {
                    // P[j+1](c)
                    final double ppc = ((2 * j + 1) * c * pc - j * pmc) / (j + 1);
                    pmc = pc;
                    pc = ppc;
                }
                // Now pc = P[n+1](c) and pmc = P[n](c).
                if (!done) {
                    if (pa * pc <= 0) {
                        b = c;
                        pmb = pmc;
                        pb = pc;
                    } else {
                        a = c;
                        pma = pmc;
                        pa = pc;
                    }
                    c = 0.5 * (a + b);
                }
            }
            final double d = numberOfPoints * (pmc - c * pc);
            final double w = 2 * (1 - c * c) / (d * d);

            points[i] = c;
            weights[i] = w;

            final int idx = numberOfPoints - i - 1;
            points[idx] = -c;
            weights[idx] = w;
        }
        // If "numberOfPoints" is odd, 0 is a root.
        // Note: as written, the test for oddness will work for negative
        // integers too (although it is not necessary here), preventing
        // a FindBugs warning.
        if (numberOfPoints % 2 != 0) {
            double pmc = 1;
            for (int j = 1; j < numberOfPoints; j += 2) {
                pmc = -j * pmc / (j + 1);
            }
            final double d = numberOfPoints * pmc;
            final double w = 2 / (d * d);

            points[iMax] = 0d;
            weights[iMax] = w;
        }

    }

    /** Performs a change of variable so that the integration
     *  can be performed on an arbitrary interval {@code [a, b]}.
     *  <p>
     *  It is assumed that the natural interval is {@code [-1, 1]}.
     *  </p>
     *
     * @param points  Points to adapt to the new interval.
     * @param weights Weights to adapt to the new interval.
     * @param a Lower bound of the integration interval.
     * @param b Lower bound of the integration interval.
     */
    private void transform(final double[] points, final double[] weights,
                           final double a, final double b) {
        // Scaling
        final double scale = (b - a) / 2;
        final double shift = a + scale;
        for (int i = 0; i < points.length; i++) {
            points[i]   = points[i] * scale + shift;
            weights[i] *= scale;
        }
    }

    /** Returns an estimate of the integral of {@code f(x) * w(x)},
     *  where {@code w} is a weight function that depends on the actual
     *  flavor of the Gauss integration scheme.
     *
     * @param f Function to integrate.
     * @param points  Nodes.
     * @param weights Nodes weights.
     * @return the integral of the weighted function.
     */
    private double[] basicIntegrate(final UnivariateVectorFunction f,
                                    final double[] points,
                                    final double[] weights) {
        double x = points[0];
        double w = weights[0];
        double[] v = f.value(x);
        final double[] y = new double[v.length];
        for (int j = 0; j < v.length; j++) {
            y[j] = w * v[j];
        }
        final double[] t = y.clone();
        final double[] c = new double[v.length];
        final double[] s = t.clone();
        for (int i = 1; i < points.length; i++) {
            x = points[i];
            w = weights[i];
            v = f.value(x);
            for (int j = 0; j < v.length; j++) {
                y[j] = w * v[j] - c[j];
                t[j] =  s[j] + y[j];
                c[j] = (t[j] - s[j]) - y[j];
                s[j] = t[j];
            }
        }
        return s;
    }

}
