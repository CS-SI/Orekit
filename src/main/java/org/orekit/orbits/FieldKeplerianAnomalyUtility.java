/* Copyright 2002-2023 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitMessages;

/**
 * Utility methods for converting between different Keplerian anomalies.
 * @author Luc Maisonobe
 * @author Andrea Antolino
 * @author Andrew Goetz
 */
public class FieldKeplerianAnomalyUtility {

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

    /** Private constructor for utility class. */
    private FieldKeplerianAnomalyUtility() {
        // Nothing to do
    }

    /**
     * Computes the elliptic true anomaly from the elliptic mean anomaly.
     * @param <T> field type
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param M elliptic mean anomaly (rad)
     * @return elliptic true anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T ellipticMeanToTrue(final T e, final T M) {
        final T E = ellipticMeanToEccentric(e, M);
        final T v = ellipticEccentricToTrue(e, E);
        return v;
    }

    /**
     * Computes the elliptic mean anomaly from the elliptic true anomaly.
     * @param <T> field type
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param v elliptic true anomaly (rad)
     * @return elliptic mean anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T ellipticTrueToMean(final T e, final T v) {
        final T E = ellipticTrueToEccentric(e, v);
        final T M = ellipticEccentricToMean(e, E);
        return M;
    }

    /**
     * Computes the elliptic true anomaly from the elliptic eccentric anomaly.
     * @param <T> field type
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param E elliptic eccentric anomaly (rad)
     * @return elliptic true anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T ellipticEccentricToTrue(final T e, final T E) {
        final T beta = e.divide(e.multiply(e).negate().add(1).sqrt().add(1));
        final FieldSinCos<T> scE = FastMath.sinCos(E);
        return E.add(beta.multiply(scE.sin()).divide(beta.multiply(scE.cos()).subtract(1).negate()).atan().multiply(2));
    }

    /**
     * Computes the elliptic eccentric anomaly from the elliptic true anomaly.
     * @param <T> field type
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param v elliptic true anomaly (rad)
     * @return elliptic eccentric anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T ellipticTrueToEccentric(final T e, final T v) {
        final T beta = e.divide(e.multiply(e).negate().add(1).sqrt().add(1));
        final FieldSinCos<T> scv = FastMath.sinCos(v);
        return v.subtract((beta.multiply(scv.sin()).divide(beta.multiply(scv.cos()).add(1))).atan().multiply(2));
    }

    /**
     * Computes the elliptic eccentric anomaly from the elliptic mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is from Odell,
     * A.W., Gooding, R.H. "Procedures for solving Kepler's equation." Celestial
     * Mechanics 38, 307–334 (1986). https://doi.org/10.1007/BF01238923
     * </p>
     * @param <T> field type
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param M elliptic mean anomaly (rad)
     * @return elliptic eccentric anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T ellipticMeanToEccentric(final T e, final T M) {
        // reduce M to [-PI PI) interval
        final T reducedM = MathUtils.normalizeAngle(M, M.getField().getZero());

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        T E;
        if (reducedM.abs().getReal() < 1.0 / 6.0) {
            if (FastMath.abs(reducedM.getReal()) < Precision.SAFE_MIN) {
                // this is an Orekit change to the S12 starter, mainly used when T is some kind
                // of derivative structure. If reducedM is 0.0, the derivative of cbrt is
                // infinite which induces NaN appearing later in the computation. As in this
                // case E and M are almost equal, we initialize E with reducedM
                E = reducedM;
            } else {
                // this is the standard S12 starter
                E = reducedM.add(e.multiply((reducedM.multiply(6).cbrt()).subtract(reducedM)));
            }
        } else {
            final T pi = e.getPi();
            if (reducedM.getReal() < 0) {
                final T w = reducedM.add(pi);
                E = reducedM.add(e.multiply(w.multiply(A).divide(w.negate().add(B)).subtract(pi).subtract(reducedM)));
            } else {
                final T w = reducedM.negate().add(pi);
                E = reducedM
                        .add(e.multiply(w.multiply(A).divide(w.negate().add(B)).negate().subtract(reducedM).add(pi)));
            }
        }

        final T e1 = e.negate().add(1);
        final boolean noCancellationRisk = (e1.getReal() + E.getReal() * E.getReal() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one
        // Newton-Raphson step
        for (int j = 0; j < 2; ++j) {

            final T f;
            T fd;
            final FieldSinCos<T> scE = FastMath.sinCos(E);
            final T fdd = e.multiply(scE.sin());
            final T fddd = e.multiply(scE.cos());

            if (noCancellationRisk) {

                f = (E.subtract(fdd)).subtract(reducedM);
                fd = fddd.negate().add(1);
            } else {

                f = eMeSinE(e, E).subtract(reducedM);
                final T s = E.multiply(0.5).sin();
                fd = e1.add(e.multiply(s).multiply(s).multiply(2));
            }
            final T dee = f.multiply(fd).divide(f.multiply(fdd).multiply(0.5).subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow problems
            final T w = fd.add(dee.multiply(fdd.add(dee.multiply(fddd.divide(3)))).multiply(0.5));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(fddd).multiply(0.5))));
            E = E.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));

        }

        // expand the result back to original range
        E = E.add(M).subtract(reducedM);
        return E;
    }

    /**
     * Accurate computation of E - e sin(E).
     * <p>
     * This method is used when E is close to 0 and e close to 1, i.e. near the
     * perigee of almost parabolic orbits
     * </p>
     * @param <T> field type
     * @param e eccentricity
     * @param E eccentric anomaly (rad)
     * @return E - e sin(E)
     */
    private static <T extends CalculusFieldElement<T>> T eMeSinE(final T e, final T E) {
        T x = (e.negate().add(1)).multiply(E.sin());
        final T mE2 = E.negate().multiply(E);
        T term = E;
        double d = 0;
        // the inequality test below IS intentional and should NOT be replaced by a
        // check with a small tolerance
        for (T x0 = E.getField().getZero().add(Double.NaN); !Double.valueOf(x.getReal())
                .equals(Double.valueOf(x0.getReal()));) {
            d += 2;
            term = term.multiply(mE2.divide(d * (d + 1)));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /**
     * Computes the elliptic mean anomaly from the elliptic eccentric anomaly.
     * @param <T> field type
     * @param e eccentricity such that 0 &le; e &lt; 1
     * @param E elliptic eccentric anomaly (rad)
     * @return elliptic mean anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T ellipticEccentricToMean(final T e, final T E) {
        return E.subtract(e.multiply(E.sin()));
    }

    /**
     * Computes the hyperbolic true anomaly from the hyperbolic mean anomaly.
     * @param <T> field type
     * @param e eccentricity &gt; 1
     * @param M hyperbolic mean anomaly
     * @return hyperbolic true anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T hyperbolicMeanToTrue(final T e, final T M) {
        final T H = hyperbolicMeanToEccentric(e, M);
        final T v = hyperbolicEccentricToTrue(e, H);
        return v;
    }

    /**
     * Computes the hyperbolic mean anomaly from the hyperbolic true anomaly.
     * @param <T> field type
     * @param e eccentricity &gt; 1
     * @param v hyperbolic true anomaly (rad)
     * @return hyperbolic mean anomaly
     */
    public static <T extends CalculusFieldElement<T>> T hyperbolicTrueToMean(final T e, final T v) {
        final T H = hyperbolicTrueToEccentric(e, v);
        final T M = hyperbolicEccentricToMean(e, H);
        return M;
    }

    /**
     * Computes the hyperbolic true anomaly from the hyperbolic eccentric anomaly.
     * @param <T> field type
     * @param e eccentricity &gt; 1
     * @param H hyperbolic eccentric anomaly
     * @return hyperbolic true anomaly (rad)
     */
    public static <T extends CalculusFieldElement<T>> T hyperbolicEccentricToTrue(final T e, final T H) {
        final T s = e.add(1).divide(e.subtract(1)).sqrt();
        final T tanH = H.multiply(0.5).tanh();
        return s.multiply(tanH).atan().multiply(2);
    }

    /**
     * Computes the hyperbolic eccentric anomaly from the hyperbolic true anomaly.
     * @param <T> field type
     * @param e eccentricity &gt; 1
     * @param v hyperbolic true anomaly (rad)
     * @return hyperbolic eccentric anomaly
     */
    public static <T extends CalculusFieldElement<T>> T hyperbolicTrueToEccentric(final T e, final T v) {
        final FieldSinCos<T> scv = FastMath.sinCos(v);
        final T sinhH = e.multiply(e).subtract(1).sqrt().multiply(scv.sin()).divide(e.multiply(scv.cos()).add(1));
        return sinhH.asinh();
    }

    /**
     * Computes the hyperbolic eccentric anomaly from the hyperbolic mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is from
     * Gooding, R.H., Odell, A.W. "The hyperbolic Kepler equation (and the elliptic
     * equation revisited)." Celestial Mechanics 44, 267–282 (1988).
     * https://doi.org/10.1007/BF01235540
     * </p>
     * @param <T> field type
     * @param e eccentricity &gt; 1
     * @param M hyperbolic mean anomaly
     * @return hyperbolic eccentric anomaly
     */
    public static <T extends CalculusFieldElement<T>> T hyperbolicMeanToEccentric(final T e, final T M) {
        final Field<T> field = e.getField();
        final T zero = field.getZero();
        final T one = field.getOne();
        final T two = zero.add(2.0);
        final T three = zero.add(3.0);
        final T half = zero.add(0.5);
        final T onePointFive = zero.add(1.5);
        final T fourThirds = zero.add(4.0).divide(zero.add(3.0));

        // Solve L = S - g * asinh(S) for S = sinh(H).
        final T L = M.divide(e);
        final T g = e.reciprocal();
        final T g1 = one.subtract(g);

        // Starter based on Lagrange's theorem.
        T S = L;
        if (L.isZero()) {
            return M.getField().getZero();
        }
        final T cl = L.multiply(L).add(one).sqrt();
        final T al = L.asinh();
        final T w = g.multiply(g).multiply(al).divide(cl.multiply(cl).multiply(cl));
        S = one.subtract(g.divide(cl));
        S = L.add(g.multiply(al).divide(S.multiply(S).multiply(S)
                .add(w.multiply(L).multiply(onePointFive.subtract(fourThirds.multiply(g)))).cbrt()));

        // Two iterations (at most) of Halley-then-Newton process.
        for (int i = 0; i < 2; ++i) {
            final T s0 = S.multiply(S);
            final T s1 = s0.add(one);
            final T s2 = s1.sqrt();
            final T s3 = s1.multiply(s2);
            final T fdd = g.multiply(S).divide(s3);
            final T fddd = g.multiply(one.subtract(two.multiply(s0))).divide(s1.multiply(s3));

            T f;
            T fd;
            if (s0.divide(zero.add(6.0)).add(g1).getReal() >= 0.5) {
                f = S.subtract(g.multiply(S.asinh())).subtract(L);
                fd = one.subtract(g.divide(s2));
            } else {
                // Accurate computation of S - (1 - g1) * asinh(S)
                // when (g1, S) is close to (0, 0).
                final T t = S.divide(one.add(one.add(S.multiply(S)).sqrt()));
                final T tsq = t.multiply(t);
                T x = S.multiply(g1.add(g.multiply(tsq)));
                T term = two.multiply(g).multiply(t);
                T twoI1 = one;
                T x0;
                int j = 0;
                do {
                    if (++j == 1000000) {
                        // This isn't expected to happen, but it protects against an infinite loop.
                        throw new MathIllegalStateException(
                                OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY, j);
                    }
                    twoI1 = twoI1.add(2.0);
                    term = term.multiply(tsq);
                    x0 = x;
                    x = x.subtract(term.divide(twoI1));
                } while (x.getReal() != x0.getReal());
                f = x.subtract(L);
                fd = s0.divide(s2.add(one)).add(g1).divide(s2);
            }
            final T ds = f.multiply(fd).divide(half.multiply(f).multiply(fdd).subtract(fd.multiply(fd)));
            final T stemp = S.add(ds);
            if (S.getReal() == stemp.getReal()) {
                break;
            }
            f = f.add(ds.multiply(fd.add(half.multiply(ds.multiply(fdd.add(ds.divide(three).multiply(fddd)))))));
            fd = fd.add(ds.multiply(fdd.add(half.multiply(ds).multiply(fddd))));
            S = stemp.subtract(f.divide(fd));
        }

        final T H = S.asinh();
        return H;
    }

    /**
     * Computes the hyperbolic mean anomaly from the hyperbolic eccentric anomaly.
     * @param <T> field type
     * @param e eccentricity &gt; 1
     * @param H hyperbolic eccentric anomaly
     * @return hyperbolic mean anomaly
     */
    public static <T extends CalculusFieldElement<T>> T hyperbolicEccentricToMean(final T e, final T H) {
        return e.multiply(H.sinh()).subtract(H);
    }

}
