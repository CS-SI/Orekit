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

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;

/**
 * Hansen coefficients K(t,n,s) for t=0 and n &lt; 0.
 * <p>
 *Implements Collins 4-242 or echivalently, Danielson 2.7.3-(6) for Hansen Coefficients and
 * Collins 4-245 or Danielson 3.1-(7) for derivatives. The recursions are transformed into
 * composition of linear transformations to obtain the associated polynomials
 * for coefficients and their derivatives - see Petre's paper
 *
 * @author Petre Bazavan
 * @author Lucian Barbulescu
 * @author Bryan Cazabonne
 * @param <T> type of the field elements
 */
public class FieldHansenZonalLinear <T extends CalculusFieldElement<T>> {

    /** The number of coefficients that will be computed with a set of roots. */
    private static final int SLICE = 10;

    /**
     * The first vector of polynomials associated to Hansen coefficients and
     * derivatives.
     */
    private final PolynomialFunction[][] mpvec;

    /** The second vector of polynomials associated only to derivatives. */
    private final PolynomialFunction[][] mpvecDeriv;

    /** The Hansen coefficients used as roots. */
    private final T[][] hansenRoot;

    /** The derivatives of the Hansen coefficients used as roots. */
    private final T[][] hansenDerivRoot;

    /** The s coefficient. */
    private final int s;

    /**
     * The offset used to identify the polynomial that corresponds to a negative
     * value of n in the internal array that starts at 0.
     */
    private final int offset;

    /** The number of slices needed to compute the coefficients. */
    private final int numSlices;

    /** 2<sup>s</sup>. */
    private final double twots;

    /** 2*s+1. */
    private final int twosp1;

    /** 2*s. */
    private final int twos;

    /** (2*s+1) / 2<sup>s</sup>. */
    private final double twosp1otwots;

    /**
     * Constructor.
     *
     * @param nMax the maximum (absolute) value of n coefficient
     * @param s s coefficient
     * @param field field used by default
     */
    public FieldHansenZonalLinear(final int nMax, final int s, final Field<T> field) {
        //Initialize fields
        final int Nmin = -nMax - 1;
        final int N0 = -(s + 2);
        this.offset = nMax + 1;
        this.s = s;
        this.twots = FastMath.pow(2., s);
        this.twos = 2 * s;
        this.twosp1 = this.twos + 1;
        this.twosp1otwots = (double) this.twosp1 / this.twots;

        // prepare structures for stored data
        final int size = nMax - s - 1;
        mpvec      = new PolynomialFunction[size][];
        mpvecDeriv = new PolynomialFunction[size][];

        this.numSlices  = FastMath.max((int) FastMath.ceil(((double) size) / SLICE), 1);
        hansenRoot      = MathArrays.buildArray(field, numSlices, 2);
        hansenDerivRoot = MathArrays.buildArray(field, numSlices, 2);

        // Prepare the data base of associated polynomials
        HansenUtilities.generateZonalPolynomials(N0, Nmin, offset, SLICE, s,
                                                 mpvec, mpvecDeriv);

    }

    /**
     * Compute the roots for the Hansen coefficients and their derivatives.
     *
     * @param chi 1 / sqrt(1 - e²)
     */
    public void computeInitValues(final T chi) {
        final Field<T> field = chi.getField();
        final T zero = field.getZero();
        // compute the values for n=s and n=s+1
        // See Danielson 2.7.3-(6a,b)
        hansenRoot[0][0] = zero;
        hansenRoot[0][1] = FastMath.pow(chi, this.twosp1).divide(this.twots);
        hansenDerivRoot[0][0] = zero;
        hansenDerivRoot[0][1] = FastMath.pow(chi, this.twos).multiply(this.twosp1otwots);

        final int st = -s - 1;
        for (int i = 1; i < numSlices; i++) {
            for (int j = 0; j < 2; j++) {
                // Get the required polynomials
                final PolynomialFunction[] mv = mpvec[st - (i * SLICE) - j + offset];
                final PolynomialFunction[] sv = mpvecDeriv[st - (i * SLICE) - j + offset];

                //Compute the root derivatives
                hansenDerivRoot[i][j] = mv[1].value(chi).multiply(hansenDerivRoot[i - 1][1]).
                                        add(mv[0].value(chi).multiply(hansenDerivRoot[i - 1][0])).
                                        add((sv[1].value(chi).multiply(hansenRoot[i - 1][1]).
                                        add(sv[0].value(chi).multiply(hansenRoot[i - 1][0]))
                                        ).divide(chi));
                hansenRoot[i][j] =     mv[1].value(chi).multiply(hansenRoot[i - 1][1]).
                                       add(mv[0].value(chi).multiply(hansenRoot[i - 1][0]));

            }

        }
    }

    /**
     * Get the K₀<sup>-n-1,s</sup> coefficient value.
     *
     * <p> The s value is given in the class constructor
     *
     * @param mnm1 (-n-1) coefficient
     * @param chi The value of χ
     * @return K₀<sup>-n-1,s</sup>
     */
    public T getValue(final int mnm1, final T chi) {
        //Compute n
        final int n = -mnm1 - 1;

        //Compute the potential slice
        int sliceNo = (n - s) / SLICE;
        if (sliceNo < numSlices) {
            //Compute the index within the slice
            final int indexInSlice = (n - s) % SLICE;

            //Check if a root must be returned
            if (indexInSlice <= 1) {
                return hansenRoot[sliceNo][indexInSlice];
            }
        } else {
            //the value was a potential root for a slice, but that slice was not required
            //Decrease the slice number
            sliceNo--;
        }

        // Danielson 2.7.3-(6c)/Collins 4-242 and Petre's paper
        final PolynomialFunction[] v = mpvec[mnm1 + offset];
        T ret = v[1].value(chi).multiply(hansenRoot[sliceNo][1]);
        if (hansenRoot[sliceNo][0].getReal() != 0) {
            ret = ret.add(v[0].value(chi).multiply(hansenRoot[sliceNo][0]));
        }
        return  ret;
    }

    /**
     * Get the dK₀<sup>-n-1,s</sup> / d&Chi; coefficient derivative.
     *
     * <p> The s value is given in the class constructor.
     *
     * @param mnm1 (-n-1) coefficient
     * @param chi The value of χ
     * @return dK₀<sup>-n-1,s</sup> / d&Chi;
     */
    public T getDerivative(final int mnm1, final T chi) {
        //Compute n
        final int n = -mnm1 - 1;

        //Compute the potential slice
        int sliceNo = (n - s) / SLICE;
        if (sliceNo < numSlices) {
            //Compute the index within the slice
            final int indexInSlice = (n - s) % SLICE;

            //Check if a root must be returned
            if (indexInSlice <= 1) {
                return hansenDerivRoot[sliceNo][indexInSlice];
            }
        } else {
            //the value was a potential root for a slice, but that slice was not required
            //Decrease the slice number
            sliceNo--;
        }

        // Danielson 3.1-(7c) and Petre's paper
        final PolynomialFunction[] v = mpvec[mnm1 + offset];
        T ret = v[1].value(chi).multiply(hansenDerivRoot[sliceNo][1]);
        if (hansenDerivRoot[sliceNo][0].getReal() != 0) {
            ret = ret.add(v[0].value(chi).multiply(hansenDerivRoot[sliceNo][0]));
        }

        // Danielson 2.7.3-(6b)
        final PolynomialFunction[] v1 = mpvecDeriv[mnm1 + offset];
        T hret = v1[1].value(chi).multiply(hansenRoot[sliceNo][1]);
        if (hansenRoot[sliceNo][0].getReal() != 0) {
            hret = hret.add(v1[0].value(chi).multiply(hansenRoot[sliceNo][0]));
        }
        ret = ret.add(hret.divide(chi));

        return ret;

    }

}
