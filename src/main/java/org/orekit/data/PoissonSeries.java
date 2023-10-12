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
package org.orekit.data;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.MathUtils.SumAndResidual;

/**
 * Class representing a Poisson series for nutation or ephemeris computations.
 * <p>
 * A Poisson series is composed of a time polynomial part and a non-polynomial
 * part which consist in summation series. The {@link SeriesTerm series terms}
 * are harmonic functions (combination of sines and cosines) of polynomial
 * <em>arguments</em>. The polynomial arguments are combinations of luni-solar or
 * planetary {@link BodiesElements elements}.
 * </p>
 * @author Luc Maisonobe
 * @see PoissonSeriesParser
 * @see SeriesTerm
 * @see PolynomialNutation
 */
public class PoissonSeries {

    /** Polynomial part. */
    private final PolynomialNutation polynomial;

    /** Non-polynomial series. */
    private final Map<Long, SeriesTerm> series;

    /** Build a Poisson series from an IERS table file.
     * @param polynomial polynomial part (may be null)
     * @param series non-polynomial part
     */
    public PoissonSeries(final PolynomialNutation polynomial, final Map<Long, SeriesTerm> series) {
        this.polynomial = polynomial;
        this.series     = series;
    }

    /** Get the polynomial part of the series.
     * @return polynomial part of the series.
     */
    public PolynomialNutation getPolynomial() {
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
        double npHigh = 0;
        double npLow  = 0;
        for (final Map.Entry<Long, SeriesTerm> entry : series.entrySet()) {
            final double v       = entry.getValue().value(elements)[0];
            // Use 2Sum for high precision.
            final SumAndResidual sumAndResidual = MathUtils.twoSum(npHigh, v);
            npHigh = sumAndResidual.getSum();
            npLow += sumAndResidual.getResidual();
        }

        // add the polynomial and the non-polynomial parts
        return p + (npHigh + npLow);

    }

    /** Evaluate the value of the series.
     * @param elements bodies elements for nutation
     * @param <T> type of the field elements
     * @return value of the series
     */
    public <T extends CalculusFieldElement<T>> T value(final FieldBodiesElements<T> elements) {

        // polynomial part
        final T tc = elements.getTC();
        final T p  = polynomial.value(tc);

        // non-polynomial part
        T sum = tc.getField().getZero();
        for (final Map.Entry<Long, SeriesTerm> entry : series.entrySet()) {
            sum = sum.add(entry.getValue().value(elements)[0]);
        }

        // add the polynomial and the non-polynomial parts
        return p.add(sum);

    }

    /** This interface represents a fast evaluator for Poisson series.
     * @see PoissonSeries#compile(PoissonSeries...)
     * @since 6.1
     */
    public interface  CompiledSeries {

        /** Evaluate a set of Poisson series.
         * @param elements bodies elements for nutation
         * @return value of the series
         */
        double[] value(BodiesElements elements);

        /** Evaluate time derivative of a set of Poisson series.
         * @param elements bodies elements for nutation
         * @return time derivative of the series
         */
        double[] derivative(BodiesElements elements);

        /** Evaluate a set of Poisson series.
         * @param elements bodies elements for nutation
         * @param <S> the type of the field elements
         * @return value of the series
         */
        <S extends CalculusFieldElement<S>> S[] value(FieldBodiesElements<S> elements);

        /** Evaluate time derivative of a set of Poisson series.
         * @param elements bodies elements for nutation
         * @param <S> the type of the field elements
         * @return time derivative of the series
         */
        <S extends CalculusFieldElement<S>> S[] derivative(FieldBodiesElements<S> elements);

    }

    /** Join several nutation series, for fast simultaneous evaluation.
     * @param poissonSeries Poisson series to join
     * @return a single function that evaluates all series together
     * @since 6.1
     */
    @SafeVarargs
    public static CompiledSeries compile(final PoissonSeries... poissonSeries) {

        // store all polynomials
        final PolynomialNutation[] polynomials = new PolynomialNutation[poissonSeries.length];
        for (int i = 0; i < polynomials.length; ++i) {
            polynomials[i] = poissonSeries[i].polynomial;
        }

        // gather all series terms
        final Map<Long, SeriesTerm> joinedMap = new HashMap<Long, SeriesTerm>();
        for (final PoissonSeries ps : poissonSeries) {
            for (Map.Entry<Long, SeriesTerm> entry : ps.series.entrySet()) {
                final long key = entry.getKey();
                if (!joinedMap.containsKey(key)) {

                    // retrieve all Delaunay and planetary multipliers from the key
                    final int[] m = NutationCodec.decode(key);

                    // prepare a new term, ready to handle the required dimension
                    final SeriesTerm term =
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
            for (final Map.Entry<Long, SeriesTerm> entry : poissonSeries[i].series.entrySet()) {
                final SeriesTerm singleTerm = entry.getValue();
                final SeriesTerm joinedTerm = joinedMap.get(entry.getKey());
                for (int degree = 0; degree <= singleTerm.getDegree(0); ++degree) {
                    joinedTerm.add(i, degree,
                                   singleTerm.getSinCoeff(0, degree),
                                   singleTerm.getCosCoeff(0, degree));
                }
            }
        }

        // use a single array for faster access
        final SeriesTerm[] joinedTerms = new SeriesTerm[joinedMap.size()];
        int index = 0;
        for (final Map.Entry<Long, SeriesTerm> entry : joinedMap.entrySet()) {
            joinedTerms[index++] = entry.getValue();
        }

        return new CompiledSeries() {

            /** {@inheritDoc} */
            @Override
            public double[] value(final BodiesElements elements) {

                // non-polynomial part
                final double[] npHigh = new double[polynomials.length];
                final double[] npLow  = new double[polynomials.length];
                for (final SeriesTerm term : joinedTerms) {
                    final double[] termValue = term.value(elements);
                    for (int i = 0; i < termValue.length; ++i) {
                        // Use 2Sum for high precision.
                        final SumAndResidual sumAndResidual = MathUtils.twoSum(npHigh[i], termValue[i]);
                        npHigh[i] = sumAndResidual.getSum();
                        npLow[i] += sumAndResidual.getResidual();
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
            public double[] derivative(final BodiesElements elements) {

                // non-polynomial part
                final double[] v = new double[polynomials.length];
                for (final SeriesTerm term : joinedTerms) {
                    final double[] termDerivative = term.derivative(elements);
                    for (int i = 0; i < termDerivative.length; ++i) {
                        v[i] += termDerivative[i];
                    }
                }

                // add polynomial part
                for (int i = 0; i < v.length; ++i) {
                    v[i] += polynomials[i].derivative(elements.getTC());
                }
                return v;

            }

            /** {@inheritDoc} */
            @Override
            public <S extends CalculusFieldElement<S>> S[] value(final FieldBodiesElements<S> elements) {

               // non-polynomial part
                final S[] v = MathArrays.buildArray(elements.getTC().getField(), polynomials.length);
                for (final SeriesTerm term : joinedTerms) {
                    final S[] termValue = term.value(elements);
                    for (int i = 0; i < termValue.length; ++i) {
                        v[i] = v[i].add(termValue[i]);
                    }
                }

                // add polynomial part
                final S tc = elements.getTC();
                for (int i = 0; i < v.length; ++i) {
                    v[i] = v[i].add(polynomials[i].value(tc));
                }
                return v;

            }

            /** {@inheritDoc} */
            @Override
            public <S extends CalculusFieldElement<S>> S[] derivative(final FieldBodiesElements<S> elements) {

               // non-polynomial part
                final S[] v = MathArrays.buildArray(elements.getTC().getField(), polynomials.length);
                for (final SeriesTerm term : joinedTerms) {
                    final S[] termDerivative = term.derivative(elements);
                    for (int i = 0; i < termDerivative.length; ++i) {
                        v[i] = v[i].add(termDerivative[i]);
                    }
                }

                // add polynomial part
                final S tc = elements.getTC();
                for (int i = 0; i < v.length; ++i) {
                    v[i] = v[i].add(polynomials[i].derivative(tc));
                }
                return v;

            }

        };

    }

}
