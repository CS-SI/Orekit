/* Copyright 2002-2025 CS GROUP
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
 * Hansen coefficients K(t,n,s) for t=0 and n &gt; 0.
 * <p>
 * Implements Collins 4-254 or Danielson 2.7.3-(7) for Hansen Coefficients and
 * Danielson 3.2-(3) for derivatives. The recursions are transformed into
 * composition of linear transformations to obtain the associated polynomials
 * for coefficients and their derivatives - see Petre's paper
 *
 * @author Petre Bazavan
 * @author Lucian Barbulescu
 * @author Bryan Cazabonne
 * @param <T> type of the field elements
 */
public class FieldHansenThirdBodyLinear <T extends CalculusFieldElement<T>> {

    /** The number of coefficients that will be computed with a set of roots. */
    private  static final int SLICE = 10;

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

    /** The number of slices needed to compute the coefficients. */
    private final int numSlices;

    /** The s index. */
    private final int s;

    /** (-1)<sup>s</sup> * (2*s + 1)!! / (s+1)!  */
    private double twosp1dfosp1f;

    /** (-1)<sup>s</sup> * (2*s + 1)!! / (s+2)!  */
    private final double twosp1dfosp2f;

    /** (-1)<sup>s</sup> * 2 * (2*s + 1)!! / (s+2)!  */
    private final double two2sp1dfosp2f;

    /** (2*s + 3). */
    private final double twosp3;

    /**
     * Constructor.
     *
     * @param nMax the maximum value of n
     * @param s the value of s
     * @param field field used by default
     */
    public FieldHansenThirdBodyLinear(final int nMax, final int s, final Field<T> field) {
        // initialise fields
        this.s = s;

        //Compute the fields that will be used to determine the initial values for the coefficients
        this.twosp1dfosp1f = (s % 2 == 0) ? 1.0 : -1.0;
        for (int i = s; i >= 1; i--) {
            this.twosp1dfosp1f *= (2.0 * i + 1.0) / (i + 1.0);
        }

        this.twosp1dfosp2f = this.twosp1dfosp1f / (s + 2.);
        this.twosp3 = 2 * s + 3;
        this.two2sp1dfosp2f = 2 * this.twosp1dfosp2f;

        // initialization of structures for stored data
        mpvec = new PolynomialFunction[nMax + 1][];
        mpvecDeriv = new PolynomialFunction[nMax + 1][];

        this.numSlices  = FastMath.max(1, (nMax - s + SLICE - 2) / SLICE);

        hansenRoot      = MathArrays.buildArray(field, numSlices, 2);
        hansenDerivRoot = MathArrays.buildArray(field, numSlices, 2);

        // Prepare the database of the associated polynomials
        HansenUtilities.generateThirdBodyPolynomials(s, nMax, SLICE, s,
                                                     mpvec, mpvecDeriv);

    }

    /**
     * Compute the initial values (see Collins, 4-255, 4-256 and 4.259)
     * <p>
     * K₀<sup>s, s</sup> = (-1)<sup>s</sup> * ( (2*s+1)!! / (s+1)! )
     * </p>
     * <p>
     * K₀<sup>s+1, s</sup> = (-1)<sup>s</sup> * ( (2*s+1)!! / (s+2)!
     * ) * (2*s+3 - χ<sup>-2</sup>)
     * </p>
     * <p>
     * dK₀<sup>s+1, s</sup> / dχ = = (-1)<sup>s</sup> * 2 * (
     * (2*s+1)!! / (s+2)! ) * χ<sup>-3</sup>
     * </p>
     * @param chitm1 sqrt(1 - e²)
     * @param chitm2 sqrt(1 - e²)²
     * @param chitm3 sqrt(1 - e²)³
     */
    public void computeInitValues(final T chitm1, final T chitm2, final T chitm3) {
        final Field<T> field = chitm2.getField();
        final T zero = field.getZero();
        this.hansenRoot[0][0] = zero.newInstance(twosp1dfosp1f);
        this.hansenRoot[0][1] = (chitm2.negate().add(this.twosp3)).multiply(this.twosp1dfosp2f);
        this.hansenDerivRoot[0][0] = zero;
        this.hansenDerivRoot[0][1] = chitm3.multiply(two2sp1dfosp2f);

        for (int i = 1; i < numSlices; i++) {
            for (int j = 0; j < 2; j++) {
                // Get the required polynomials
                final PolynomialFunction[] mv = mpvec[s + (i * SLICE) + j];
                final PolynomialFunction[] sv = mpvecDeriv[s + (i * SLICE) + j];

                //Compute the root derivatives
                hansenDerivRoot[i][j] = mv[1].value(chitm1).multiply(hansenDerivRoot[i - 1][1]).
                                    add(mv[0].value(chitm1).multiply(hansenDerivRoot[i - 1][0])).
                                    add(sv[1].value(chitm1).multiply(hansenRoot[i - 1][1])).
                                    add(sv[0].value(chitm1).multiply(hansenRoot[i - 1][0]));

                //Compute the root Hansen coefficients
                hansenRoot[i][j] =  mv[1].value(chitm1).multiply(hansenRoot[i - 1][1]).
                                add(mv[0].value(chitm1).multiply(hansenRoot[i - 1][0]));
            }
        }
    }

    /**
     * Compute the value of the Hansen coefficient K₀<sup>n, s</sup>.
     *
     * @param n n value
     * @param chitm1 χ<sup>-1</sup>
     * @return the coefficient K₀<sup>n, s</sup>
     */
    public T getValue(final int n, final T chitm1) {
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
        final PolynomialFunction[] v = mpvec[n];
        T ret = v[1].value(chitm1).multiply(hansenRoot[sliceNo][1]);
        if (hansenRoot[sliceNo][0].getReal() != 0) {
            ret = ret.add(v[0].value(chitm1).multiply(hansenRoot[sliceNo][0]));
        }

        return ret;

    }

    /**
     * Compute the value of the Hansen coefficient dK₀<sup>n, s</sup> / d&Chi;.
     *
     * @param n n value
     * @param chitm1 χ<sup>-1</sup>
     * @return the coefficient dK₀<sup>n, s</sup> / d&Chi;
     */
    public T getDerivative(final int n, final T chitm1) {
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

        final PolynomialFunction[] v = mpvec[n];
        T ret = v[1].value(chitm1).multiply(hansenDerivRoot[sliceNo][1]);
        if (hansenDerivRoot[sliceNo][0].getReal() != 0) {
            ret = ret.add(v[0].value(chitm1).multiply(hansenDerivRoot[sliceNo][0]));
        }

        // Danielson 2.7.3-(7c)/Collins 4-254 and Petre's paper
        final PolynomialFunction[] v1 = mpvecDeriv[n];
        ret = ret.add(v1[1].value(chitm1).multiply(hansenRoot[sliceNo][1]));
        if (hansenRoot[sliceNo][0].getReal() != 0) {
            ret = ret.add(v1[0].value(chitm1).multiply(hansenRoot[sliceNo][0]));
        }
        return ret;

    }

}
