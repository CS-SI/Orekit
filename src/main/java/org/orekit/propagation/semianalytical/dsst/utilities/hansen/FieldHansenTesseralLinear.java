/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst.utilities.hansen;

import java.lang.reflect.Array;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.semianalytical.dsst.utilities.NewcombOperators;

/**
 * Hansen coefficients K(t,n,s) for t!=0 and n &lt; 0.
 * <p>
 * Implements Collins 4-236 or Danielson 2.7.3-(9) for Hansen Coefficients and
 * Collins 4-240 for derivatives. The recursions are transformed into
 * composition of linear transformations to obtain the associated polynomials
 * for coefficients and their derivatives - see Petre's paper
 *
 * @author Petre Bazavan
 * @author Lucian Barbulescu
 * @author Bryan Cazabonne
 * @param <T> type of the field elements
 */
public class FieldHansenTesseralLinear <T extends CalculusFieldElement<T>> {

    /** The number of coefficients that will be computed with a set of roots. */
    private static final int SLICE = 10;

    /**
     * The first vector of polynomials associated to Hansen coefficients and
     * derivatives.
     */
    private PolynomialFunction[][] mpvec;

    /** The second vector of polynomials associated only to derivatives. */
    private PolynomialFunction[][] mpvecDeriv;

    /** The Hansen coefficients used as roots. */
    private final T[][] hansenRoot;

    /** The derivatives of the Hansen coefficients used as roots. */
    private final T[][] hansenDerivRoot;

    /** The minimum value for the order. */
    private final int Nmin;

    /** The index of the initial condition, Petre's paper. */
    private final int N0;

    /** The number of slices needed to compute the coefficients. */
    private final int numSlices;

    /**
     * The offset used to identify the polynomial that corresponds to a negative.
     * value of n in the internal array that starts at 0
     */
    private final int offset;

    /** The objects used to calculate initial data by means of Newcomb operators. */
    private final FieldHansenCoefficientsBySeries<T>[] hansenInit;

    /**
     * Constructor.
     *
     * @param nMax the maximum (absolute) value of n parameter
     * @param s s parameter
     * @param j j parameter
     * @param n0 the minimum (absolute) value of n
     * @param maxHansen maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
     * @param field field used by default
     */
    @SuppressWarnings("unchecked")
    public FieldHansenTesseralLinear(final int nMax, final int s, final int j, final int n0,
                                     final int maxHansen, final Field<T> field) {
        //Initialize the fields
        this.offset = nMax + 1;
        this.Nmin = -nMax - 1;
        this.N0 = -n0 - 4;

        final int maxRoots = FastMath.min(4, N0 - Nmin + 4);
        //Ensure that only the needed terms are computed
        this.hansenInit = (FieldHansenCoefficientsBySeries<T>[]) Array.newInstance(FieldHansenCoefficientsBySeries.class, maxRoots);
        for (int i = 0; i < maxRoots; i++) {
            this.hansenInit[i] = new FieldHansenCoefficientsBySeries<>(N0 - i + 3, s, j, maxHansen, field);
        }

        // The first 4 values are computed with series. No linear combination is needed.
        final int size = N0 - Nmin;
        this.numSlices = (int) FastMath.max(FastMath.ceil(((double) size) / SLICE), 1);
        hansenRoot = MathArrays.buildArray(field, numSlices, 4);
        hansenDerivRoot = MathArrays.buildArray(field, numSlices, 4);
        if (size > 0) {
            mpvec = new PolynomialFunction[size][];
            mpvecDeriv = new PolynomialFunction[size][];

            // Prepare the database of the associated polynomials
            HansenUtilities.generateTesseralPolynomials(N0, Nmin, offset, SLICE, j, s,
                                                        mpvec, mpvecDeriv);
        }

    }

    /**
     * Compute the values for the first four coefficients and their derivatives by means of series.
     *
     * @param e2 e²
     * @param chi &Chi;
     * @param chi2 &Chi;²
     */
    public void computeInitValues(final T e2, final T chi, final T chi2) {
        // compute the values for n, n+1, n+2 and n+3 by series
        // See Danielson 2.7.3-(10)
        //Ensure that only the needed terms are computed
        final int maxRoots = FastMath.min(4, N0 - Nmin + 4);
        for (int i = 0; i < maxRoots; i++) {
            final FieldGradient<T> hansenKernel = hansenInit[i].getValueGradient(e2, chi, chi2);
            this.hansenRoot[0][i] = hansenKernel.getValue();
            this.hansenDerivRoot[0][i] = hansenKernel.getPartialDerivative(0);
        }

        for (int i = 1; i < numSlices; i++) {
            for (int k = 0; k < 4; k++) {
                final PolynomialFunction[] mv = mpvec[N0 - (i * SLICE) - k + 3 + offset];
                final PolynomialFunction[] sv = mpvecDeriv[N0 - (i * SLICE) - k + 3 + offset];

                hansenDerivRoot[i][k] = mv[3].value(chi).multiply(hansenDerivRoot[i - 1][3]).
                                        add(mv[2].value(chi).multiply(hansenDerivRoot[i - 1][2])).
                                        add(mv[1].value(chi).multiply(hansenDerivRoot[i - 1][1])).
                                        add(mv[0].value(chi).multiply(hansenDerivRoot[i - 1][0])).
                                        add(sv[3].value(chi).multiply(hansenRoot[i - 1][3])).
                                        add(sv[2].value(chi).multiply(hansenRoot[i - 1][2])).
                                        add(sv[1].value(chi).multiply(hansenRoot[i - 1][1])).
                                        add(sv[0].value(chi).multiply(hansenRoot[i - 1][0]));

                hansenRoot[i][k] =  mv[3].value(chi).multiply(hansenRoot[i - 1][3]).
                                    add(mv[2].value(chi).multiply(hansenRoot[i - 1][2])).
                                    add(mv[1].value(chi).multiply(hansenRoot[i - 1][1])).
                                    add(mv[0].value(chi).multiply(hansenRoot[i - 1][0]));
            }
        }
    }

    /**
     * Compute the value of the Hansen coefficient K<sub>j</sub><sup>-n-1, s</sup>.
     *
     * @param mnm1 -n-1
     * @param chi χ
     * @return the coefficient K<sub>j</sub><sup>-n-1, s</sup>
     */
    public T getValue(final int mnm1, final T chi) {
        //Compute n
        final int n = -mnm1 - 1;

        //Compute the potential slice
        int sliceNo = (n + N0 + 4) / SLICE;
        if (sliceNo < numSlices) {
            //Compute the index within the slice
            final int indexInSlice = (n + N0 + 4) % SLICE;

            //Check if a root must be returned
            if (indexInSlice <= 3) {
                return hansenRoot[sliceNo][indexInSlice];
            }
        } else {
            //the value was a potential root for a slice, but that slice was not required
            //Decrease the slice number
            sliceNo--;
        }

        // Computes the coefficient by linear transformation
        // Danielson 2.7.3-(9) or Collins 4-236 and Petre's paper
        final PolynomialFunction[] v = mpvec[mnm1 + offset];
        return v[3].value(chi).multiply(hansenRoot[sliceNo][3]).
               add(v[2].value(chi).multiply(hansenRoot[sliceNo][2])).
               add(v[1].value(chi).multiply(hansenRoot[sliceNo][1])).
               add(v[0].value(chi).multiply(hansenRoot[sliceNo][0]));

    }

    /**
     * Compute the value of the derivative dK<sub>j</sub><sup>-n-1, s</sup> / de².
     *
     * @param mnm1 -n-1
     * @param chi χ
     * @return the derivative dK<sub>j</sub><sup>-n-1, s</sup> / de²
     */
    public T getDerivative(final int mnm1, final T chi) {

        //Compute n
        final int n = -mnm1 - 1;

        //Compute the potential slice
        int sliceNo = (n + N0 + 4) / SLICE;
        if (sliceNo < numSlices) {
            //Compute the index within the slice
            final int indexInSlice = (n + N0 + 4) % SLICE;

            //Check if a root must be returned
            if (indexInSlice <= 3) {
                return hansenDerivRoot[sliceNo][indexInSlice];
            }
        } else {
            //the value was a potential root for a slice, but that slice was not required
            //Decrease the slice number
            sliceNo--;
        }

        // Computes the coefficient by linear transformation
        // Danielson 2.7.3-(9) or Collins 4-236 and Petre's paper
        final PolynomialFunction[] v = mpvec[mnm1 + this.offset];
        final PolynomialFunction[] vv = mpvecDeriv[mnm1 + this.offset];

        return v[3].value(chi).multiply(hansenDerivRoot[sliceNo][3]).
               add(v[2].value(chi).multiply(hansenDerivRoot[sliceNo][2])).
               add(v[1].value(chi).multiply(hansenDerivRoot[sliceNo][1])).
               add(v[0].value(chi).multiply(hansenDerivRoot[sliceNo][0])).
               add(vv[3].value(chi).multiply(hansenRoot[sliceNo][3])).
               add(vv[2].value(chi).multiply(hansenRoot[sliceNo][2])).
               add( vv[1].value(chi).multiply(hansenRoot[sliceNo][1])).
               add(vv[0].value(chi).multiply(hansenRoot[sliceNo][0]));

    }

    /**
     * Compute a Hansen coefficient with series.
     * <p>
     * This class implements the computation of the Hansen kernels
     * through a power series in e² and that is using
     * modified Newcomb operators. The reference formulae can be found
     * in Danielson 2.7.3-10 and 3.3-5
     * </p>
     */
    private static class FieldHansenCoefficientsBySeries <T extends CalculusFieldElement<T>> {

        /** -n-1 coefficient. */
        private final int mnm1;

        /** s coefficient. */
        private final int s;

        /** j coefficient. */
        private final int j;

        /** Max power in e² for the Newcomb's series expansion. */
        private final int maxNewcomb;

        /** Polynomial representing the serie. */
        private final PolynomialFunction polynomial;

        /**
         * Class constructor.
         *
         * @param mnm1 -n-1 value
         * @param s s value
         * @param j j value
         * @param maxHansen max power of e² in series expansion
         * @param field field for the function parameters and value
         */
        FieldHansenCoefficientsBySeries(final int mnm1, final int s,
                                          final int j, final int maxHansen, final Field<T> field) {
            this.mnm1 = mnm1;
            this.s = s;
            this.j = j;
            this.maxNewcomb = maxHansen;
            this.polynomial = generatePolynomial();
        }

        /** Computes the value of Hansen kernel and its derivative at e².
         * <p>
         * The formulae applied are described in Danielson 2.7.3-10 and
         * 3.3-5
         * </p>
         * @param e2 e²
         * @param chi &Chi;
         * @param chi2 &Chi;²
         * @return the value of the Hansen coefficient and its derivative for e²
         */
        private FieldGradient<T> getValueGradient(final T e2, final T chi, final T chi2) {

            final T zero = e2.getField().getZero();
            //Estimation of the serie expansion at e2
            final FieldGradient<T> serie = polynomial.value(FieldGradient.variable(1, 0, e2));

            final T value      =  FastMath.pow(chi2, -mnm1 - 1).multiply(serie.getValue()).divide(chi);
            final T coef       = zero.subtract(mnm1 + 1.5);
            final T derivative = coef.multiply(chi2).multiply(value).
                            add(FastMath.pow(chi2, -mnm1 - 1).multiply(serie.getPartialDerivative(0)).divide(chi));
            return new FieldGradient<T>(value, derivative);
        }

        /** Generate the serie expansion in e².
         * <p>
         * Generate the series expansion in e² used in the formulation
         * of the Hansen kernel (see Danielson 2.7.3-10):
         * &Sigma; Y<sup>ns</sup><sub>α+a,α+b</sub>
         * *e<sup>2α</sup>
         * </p>
         * @return polynomial representing the power serie expansion
         */
        private PolynomialFunction generatePolynomial() {
            // Initialization
            final int aHT = FastMath.max(j - s, 0);
            final int bHT = FastMath.max(s - j, 0);

            final double[] coefficients = new double[maxNewcomb + 1];

            //Loop for getting the Newcomb operators
            for (int alphaHT = 0; alphaHT <= maxNewcomb; alphaHT++) {
                coefficients[alphaHT] =
                        NewcombOperators.getValue(alphaHT + aHT, alphaHT + bHT, mnm1, s);
            }

            //Creation of the polynomial
            return new PolynomialFunction(coefficients);
        }
    }

}
