/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.data;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.MathArrays;

/**
 * Class representing a Poisson series for nutation or ephemeris computations.
 * <p>
 * A Poisson series is composed of a time polynomial part and a non-polynomial
 * part which consist in summation series. The {@link SeriesTerm series terms}
 * are harmonic functions (combination of sines and cosines) of polynomial
 * <em>arguments</em>. The polynomial arguments are combinations of luni-solar or
 * planetary {@link BodiesElements elements}.
 * </p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @see PoissonSeriesParser
 * @see SeriesTerm
 * @see PolynomialNutation
 */
public class PoissonSeries<T extends RealFieldElement<T>> {

    /** Polynomial part. */
    private final PolynomialNutation<T> polynomial;

    /** Non-polynomial series. */
    private final Map<Long, SeriesTerm<T>> series;

    /** Build a Poisson series from an IERS table file.
     * @param polynomial polynomial part (may be null)
     * @param series non-polynomial part
     */
    public PoissonSeries(final PolynomialNutation<T> polynomial, final Map<Long, SeriesTerm<T>> series) {
        this.polynomial = polynomial;
        this.series     = series;
    }

    /** Get the polynomial part of the series.
     * @return polynomial part of the series.
     */
    public PolynomialNutation<T> getPolynomial() {
        return polynomial;
    }

    /** Get the number of different terms in the non-polynomial part.
     * @return number of different terms in the non-polynomial part
     */
    public int getNonPolynomialSize() {
        return series.size();
    }

    /** Evaluate the value of the series.
     * @param elements bodies elements for nutation
     * @return value of the series
     */
    public double value(final BodiesElements elements) {

        // polynomial part
        final double p = polynomial.value(elements.getTC());

        // non-polynomial part
        // compute sum accurately, using Møller-Knuth TwoSum algorithm without branching
        // the following statements must NOT be simplified, they rely on floating point
        // arithmetic properties (rounding and representable numbers)
        double npHigh = 0;
        double npLow  = 0;
        for (final SeriesTerm<T> term : series.values()) {
            final double v       = term.value(elements)[0];
            final double sum     = npHigh + v;
            final double sPrime  = sum - v;
            final double tPrime  = sum - sPrime;
            final double deltaS  = npHigh  - sPrime;
            final double deltaT  = v - tPrime;
            npLow  += deltaS   + deltaT;
            npHigh  = sum;
        }

        // add the polynomial and the non-polynomial parts
        return p + (npHigh + npLow);

    }

    /** Evaluate the value of the series.
     * @param elements bodies elements for nutation
     * @return value of the series
     */
    public T value(final FieldBodiesElements<T> elements) {

        // polynomial part
        final T tc = elements.getTC();
        final T p  = polynomial.value(tc);

        // non-polynomial part
        T sum = tc.getField().getZero();
        for (final SeriesTerm<T> term : series.values()) {
            sum = sum.add(term.value(elements)[0]);
        }

        // add the polynomial and the non-polynomial parts
        return p.add(sum);

    }

    /** This interface represents a fast evaluator for Poisson series.
     * @see PoissonSeries#compile(PoissonSeries...)
     * @param <S> the type of the field elements
     * @since 6.1
     */
    public interface  CompiledSeries<S extends RealFieldElement<S>> {

        /** Evaluate a set of Poisson series.
         * @param elements bodies elements for nutation
         * @return value of the series
         */
        double[] value(BodiesElements elements);

        /** Evaluate a set of Poisson series.
         * @param elements bodies elements for nutation
         * @return value of the series
         */
        S[] value(FieldBodiesElements<S> elements);

    }

    /** Join several nutation series, for fast simultaneous evaluation.
     * @param poissonSeries Poisson series to join
     * @return a single function that evaluates all series together
     * @param <S> the type of the field elements
     * @since 6.1
     */
    @SafeVarargs
    public static <S extends RealFieldElement<S>> CompiledSeries<S> compile(final PoissonSeries<S> ... poissonSeries) {

        // store all polynomials
        @SuppressWarnings("unchecked")
        final PolynomialNutation<S>[] polynomials =
                (PolynomialNutation<S>[]) Array.newInstance(PolynomialNutation.class, poissonSeries.length);
        for (int i = 0; i < polynomials.length; ++i) {
            polynomials[i] = poissonSeries[i].polynomial;
        }

        // gather all series terms
        final Map<Long, SeriesTerm<S>> joinedMap = new HashMap<Long, SeriesTerm<S>>();
        for (final PoissonSeries<S> ps : poissonSeries) {
            for (long key : ps.series.keySet()) {
                if (!joinedMap.containsKey(key)) {

                    // retrieve all Delaunay and planetary multipliers from the key
                    final int[] m = NutationCodec.decode(key);

                    // prepare a new term, ready to handle the required dimension
                    final SeriesTerm<S> term =
                            SeriesTerm.buildTerm(m[0],
                                                 m[1], m[2], m[3], m[4], m[5],
                                                 m[6], m[7], m[8], m[9], m[10], m[11], m[12], m[13], m[14]);
                    term.add(poissonSeries.length - 1, -1, Double.NaN, Double.NaN);

                    // store it
                    joinedMap.put(key, term);

                }
            }
        }

        // join series by sharing terms, in order to speed up evaluation
        // which is dominated by the computation of sine/cosine in each term
        for (int i = 0; i < poissonSeries.length; ++i) {
            for (final Map.Entry<Long, SeriesTerm<S>> entry : poissonSeries[i].series.entrySet()) {
                final SeriesTerm<S> singleTerm = entry.getValue();
                final SeriesTerm<S> joinedTerm = joinedMap.get(entry.getKey());
                for (int degree = 0; degree <= singleTerm.getDegree(0); ++degree) {
                    joinedTerm.add(i, degree,
                                   singleTerm.getSinCoeff(0, degree),
                                   singleTerm.getCosCoeff(0, degree));
                }
            }
        }

        // use a single array for faster access
        @SuppressWarnings("unchecked")
        final SeriesTerm<S>[] joinedTerms =
                joinedMap.values().toArray((SeriesTerm<S>[]) Array.newInstance(SeriesTerm.class, joinedMap.size()));

        return new CompiledSeries<S>() {

            /** {@inheritDoc} */
            @Override
            public double[] value(final BodiesElements elements) {

                // non-polynomial part
                // compute sum accurately, using Møller-Knuth TwoSum algorithm without branching
                // the following statements must NOT be simplified, they rely on floating point
                // arithmetic properties (rounding and representable numbers)
                final double[] npHigh = new double[polynomials.length];
                final double[] npLow  = new double[polynomials.length];
                for (final SeriesTerm<S> term : joinedTerms) {
                    final double[] termValue = term.value(elements);
                    for (int i = 0; i < termValue.length; ++i) {
                        final double v       = termValue[i];
                        final double sum     = npHigh[i] + v;
                        final double sPrime  = sum - v;
                        final double tPrime  = sum - sPrime;
                        final double deltaS  = npHigh[i]  - sPrime;
                        final double deltaT  = v - tPrime;
                        npLow[i]  += deltaS   + deltaT;
                        npHigh[i]  = sum;
                    }
                }

                // add residual and polynomial part
                for (int i = 0; i < npHigh.length; ++i) {
                    npHigh[i] += npLow[i] + polynomials[i].value(elements.getTC());
                }
                return npHigh;

            }

            /** {@inheritDoc} */
            @Override
            public S[] value(final FieldBodiesElements<S> elements) {

               // non-polynomial part
                final S[] v = MathArrays.buildArray(elements.getTC().getField(), polynomials.length);
                for (final SeriesTerm<S> term : joinedTerms) {
                    final S[] termValue = term.value(elements);
                    for (int i = 0; i < termValue.length; ++i) {
                        v[i] = v[i].add(termValue[i]);
                    }
                }

                // add residual and polynomial part
                final S tc = elements.getTC();
                for (int i = 0; i < v.length; ++i) {
                    v[i] = v[i].add(polynomials[i].value(tc));
                }
                return v;

            }

        };

    }

}
