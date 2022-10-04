/* Copyright 2002-2022 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.orbits;

import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.errors.OrekitMessages;

/**
 * Utility methods for converting between different Keplerian anomalies.
 * @author Luc Maisonobe
 * @author Andrew Goetz
 */
public final class KeplerianAnomalyUtility {

    /** First coefficient to compute elliptic Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute elliptic Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A = 3 * k2 * k2 / k1;
        B = k3 * k3 / (6 * k1);
    }

    private KeplerianAnomalyUtility() {
    }

    /**
     * Computes the elliptic true anomaly from the elliptic mean anomaly.
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param M elliptic mean anomaly (rad)
     * @return elliptic true anomaly (rad)
     */
    public static double ellipticMeanToTrue(final double e, final double M) {
        final double E = ellipticMeanToEccentric(e, M);
        final double v = ellipticEccentricToTrue(e, E);
        return v;
    }

    /**
     * Computes the elliptic mean anomaly from the elliptic true anomaly.
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param v elliptic true anomaly (rad)
     * @return elliptic mean anomaly (rad)
     */
    public static double ellipticTrueToMean(final double e, final double v) {
        final double E = ellipticTrueToEccentric(e, v);
        final double M = ellipticEccentricToMean(e, E);
        return M;
    }

    /**
     * Computes the elliptic true anomaly from the elliptic eccentric anomaly.
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param E elliptic eccentric anomaly (rad)
     * @return elliptic true anomaly (rad)
     */
    public static double ellipticEccentricToTrue(final double e, final double E) {
        final double beta = e / (1 + FastMath.sqrt((1 - e) * (1 + e)));
        final SinCos scE = FastMath.sinCos(E);
        return E + 2 * FastMath.atan(beta * scE.sin() / (1 - beta * scE.cos()));
    }

    /**
     * Computes the elliptic eccentric anomaly from the elliptic true anomaly.
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param v elliptic true anomaly (rad)
     * @return elliptic eccentric anomaly (rad)
     */
    public static double ellipticTrueToEccentric(final double e, final double v) {
        final double beta = e / (1 + FastMath.sqrt(1 - e * e));
        final SinCos scv = FastMath.sinCos(v);
        return v - 2 * FastMath.atan(beta * scv.sin() / (1 + beta * scv.cos()));
    }

    /**
     * Computes the elliptic eccentric anomaly from the elliptic mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is from Odell,
     * A.W., Gooding, R.H. "Procedures for solving Kepler's equation." Celestial
     * Mechanics 38, 307–334 (1986). https://doi.org/10.1007/BF01238923
     * </p>
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param M elliptic mean anomaly (rad)
     * @return elliptic eccentric anomaly (rad)
     */
    public static double ellipticMeanToEccentric(final double e, final double M) {
        // reduce M to [-PI PI) interval
        final double reducedM = MathUtils.normalizeAngle(M, 0.0);

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        double E;
        if (FastMath.abs(reducedM) < 1.0 / 6.0) {
            E = reducedM + e * (FastMath.cbrt(6 * reducedM) - reducedM);
        } else {
            if (reducedM < 0) {
                final double w = FastMath.PI + reducedM;
                E = reducedM + e * (A * w / (B - w) - FastMath.PI - reducedM);
            } else {
                final double w = FastMath.PI - reducedM;
                E = reducedM + e * (FastMath.PI - A * w / (B - w) - reducedM);
            }
        }

        final double e1 = 1 - e;
        final boolean noCancellationRisk = (e1 + E * E / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one
        // Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final double f;
            double fd;
            final SinCos sc = FastMath.sinCos(E);
            final double fdd = e * sc.sin();
            final double fddd = e * sc.cos();
            if (noCancellationRisk) {
                f = (E - fdd) - reducedM;
                fd = 1 - fddd;
            } else {
                f = eMeSinE(e, E) - reducedM;
                final double s = FastMath.sin(0.5 * E);
                fd = e1 + 2 * e * s * s;
            }
            final double dee = f * fd / (0.5 * f * fdd - fd * fd);

            // update eccentric anomaly, using expressions that limit underflow problems
            final double w = fd + 0.5 * dee * (fdd + dee * fddd / 3);
            fd += dee * (fdd + 0.5 * dee * fddd);
            E -= (f - dee * (fd - w)) / fd;

        }

        // expand the result back to original range
        E += M - reducedM;

        return E;
    }

    /**
     * Accurate computation of E - e sin(E).
     * <p>
     * This method is used when E is close to 0 and e close to 1, i.e. near the
     * perigee of almost parabolic orbits
     * </p>
     * @param e eccentricity
     * @param E eccentric anomaly (rad)
     * @return E - e sin(E)
     */
    private static double eMeSinE(final double e, final double E) {
        double x = (1 - e) * FastMath.sin(E);
        final double mE2 = -E * E;
        double term = E;
        double d = 0;
        // the inequality test below IS intentional and should NOT be replaced by a
        // check with a small tolerance
        for (double x0 = Double.NaN; !Double.valueOf(x).equals(Double.valueOf(x0));) {
            d += 2;
            term *= mE2 / (d * (d + 1));
            x0 = x;
            x = x - term;
        }
        return x;
    }

    /**
     * Computes the elliptic mean anomaly from the elliptic eccentric anomaly.
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param E elliptic eccentric anomaly (rad)
     * @return elliptic mean anomaly (rad)
     */
    public static double ellipticEccentricToMean(final double e, final double E) {
        return E - e * FastMath.sin(E);
    }

    /**
     * Computes the hyperbolic true anomaly from the hyperbolic mean anomaly.
     * @param e eccentricity &gt; 1
     * @param M hyperbolic mean anomaly
     * @return hyperbolic true anomaly (rad)
     */
    public static double hyperbolicMeanToTrue(final double e, final double M) {
        final double H = hyperbolicMeanToEccentric(e, M);
        final double v = hyperbolicEccentricToTrue(e, H);
        return v;
    }

    /**
     * Computes the hyperbolic mean anomaly from the hyperbolic true anomaly.
     * @param e eccentricity &gt; 1
     * @param v hyperbolic true anomaly (rad)
     * @return hyperbolic mean anomaly
     */
    public static double hyperbolicTrueToMean(final double e, final double v) {
        final double H = hyperbolicTrueToEccentric(e, v);
        final double M = hyperbolicEccentricToMean(e, H);
        return M;
    }

    /**
     * Computes the hyperbolic true anomaly from the hyperbolic eccentric anomaly.
     * @param e eccentricity &gt; 1
     * @param H hyperbolic eccentric anomaly
     * @return hyperbolic true anomaly (rad)
     */
    public static double hyperbolicEccentricToTrue(final double e, final double H) {
        return 2 * FastMath.atan(FastMath.sqrt((e + 1) / (e - 1)) * FastMath.tanh(0.5 * H));
    }

    /**
     * Computes the hyperbolic eccentric anomaly from the hyperbolic true anomaly.
     * @param e eccentricity &gt; 1
     * @param v hyperbolic true anomaly (rad)
     * @return hyperbolic eccentric anomaly
     */
    public static double hyperbolicTrueToEccentric(final double e, final double v) {
        final SinCos scv = FastMath.sinCos(v);
        final double sinhH = FastMath.sqrt(e * e - 1) * scv.sin() / (1 + e * scv.cos());
        return FastMath.asinh(sinhH);
    }

    /**
     * Computes the hyperbolic eccentric anomaly from the hyperbolic mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is Danby's
     * iterative method (3rd order) with Vallado's initial guess.
     * </p>
     * @param e eccentricity &gt; 1
     * @param M hyperbolic mean anomaly
     * @return hyperbolic eccentric anomaly
     */
    public static double hyperbolicMeanToEccentric(final double e, final double M) {
        // Resolution of hyperbolic Kepler equation for Keplerian parameters

        // Initial guess
        double H;
        if (e < 1.6) {
            if (-FastMath.PI < M && M < 0. || M > FastMath.PI) {
                H = M - e;
            } else {
                H = M + e;
            }
        } else {
            if (e < 3.6 && FastMath.abs(M) > FastMath.PI) {
                H = M - FastMath.copySign(e, M);
            } else {
                H = M / (e - 1.);
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final double f3 = e * FastMath.cosh(H);
            final double f2 = e * FastMath.sinh(H);
            final double f1 = f3 - 1.;
            final double f0 = f2 - H - M;
            final double f12 = 2. * f1;
            final double d = f0 / f12;
            final double fdf = f1 - d * f2;
            final double ds = f0 / fdf;

            final double shift = f0 / (fdf + ds * ds * f3 / 6.);

            H -= shift;

            if (FastMath.abs(shift) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY, iter);
    }

    /**
     * Computes the hyperbolic mean anomaly from the hyperbolic eccentric anomaly.
     * @param e eccentricity &gt; 1
     * @param H hyperbolic eccentric anomaly
     * @return hyperbolic mean anomaly
     */
    public static double hyperbolicEccentricToMean(final double e, final double H) {
        return e * FastMath.sinh(H) - H;
    }

}
