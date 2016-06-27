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
package org.orekit.propagation.semianalytical.dsst.utilities.hansen;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;

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
 */
public class HansenZonalLinear {

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
    private double[][] hansenRoot;

    /** The derivatives of the Hansen coefficients used as roots. */
    private double[][] hansenDerivRoot;

    /** The minimum value for the order. */
    private int Nmin;


    /** The index of the initial condition, Petre's paper. */
    private int N0;

    /** The s coefficient. */
    private int s;

    /**
     * The offset used to identify the polynomial that corresponds to a negative
     * value of n in the internal array that starts at 0.
     */
    private int offset;

    /** The number of slices needed to compute the coefficients. */
    private int numSlices;

    /** 2<sup>s</sup>. */
    private double twots;

    /** 2*s+1. */
    private int twosp1;

    /** 2*s. */
    private int twos;

    /** (2*s+1) / 2<sup>s</sup>. */
    private double twosp1otwots;

    /**
     * Constructor.
     *
     * @param nMax the maximum (absolute) value of n coefficient
     * @param s s coefficient
     */
    public HansenZonalLinear(final int nMax, final int s) {

        //Initialize fields
        this.offset = nMax + 1;
        this.Nmin = -nMax - 1;
        N0 = -(s + 2);
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
        hansenRoot      = new double[numSlices][2];
        hansenDerivRoot = new double[numSlices][2];

        // Prepare the data base of associated polynomials
        generatePolynomials();

    }

    /**
     * Compute polynomial coefficient a.
     *
     *  <p>
     *  It is used to generate the coefficient for K₀<sup>-n, s</sup> when computing K₀<sup>-n-1, s</sup>
     *  and the coefficient for dK₀<sup>-n, s</sup> / de² when computing dK₀<sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(6) and Collins 4-242 and 4-245
     *  </p>
     *
     * @param mnm1 -n-1 value
     * @return the polynomial
     */
    private PolynomialFunction a(final int mnm1) {
        // from recurence Collins 4-242
        final double d1 = (mnm1 + 2) * (2 * mnm1 + 5);
        final double d2 = (mnm1 + 2 - s) * (mnm1 + 2 + s);
        return new PolynomialFunction(new double[] {
            0.0, 0.0, d1 / d2
        });
    }

    /**
     * Compute polynomial coefficient b.
     *
     *  <p>
     *  It is used to generate the coefficient for K₀<sup>-n+1, s</sup> when computing K₀<sup>-n-1, s</sup>
     *  and the coefficient for dK₀<sup>-n+1, s</sup> / de² when computing dK₀<sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(6) and Collins 4-242 and 4-245
     *  </p>
     *
     * @param mnm1 -n-1 value
     * @return the polynomial
     */
    private PolynomialFunction b(final int mnm1) {
        // from recurence Collins 4-242
        final double d1 = (mnm1 + 2) * (mnm1 + 3);
        final double d2 = (mnm1 + 2 - s) * (mnm1 + 2 + s);
        return new PolynomialFunction(new double[] {
            0.0, 0.0, -d1 / d2
        });
    }

    /**
     * Generate the polynomials needed in the linear transformation.
     *
     * <p>
     * See Petre's paper
     * </p>
     */
    private void generatePolynomials() {

        int sliceCounter = 0;
        int index;

        // Initialisation of matrix for linear transformmations
        // The final configuration of these matrix are obtained by composition
        // of linear transformations
        PolynomialFunctionMatrix A = HansenUtilities.buildIdentityMatrix2();
        PolynomialFunctionMatrix D = HansenUtilities.buildZeroMatrix2();
        PolynomialFunctionMatrix E = HansenUtilities.buildIdentityMatrix2();

        // generation of polynomials associated to Hansen coefficients and to
        // their derivatives
        final PolynomialFunctionMatrix a = HansenUtilities.buildZeroMatrix2();
        a.setElem(0, 1, HansenUtilities.ONE);

        //The B matrix is constant.
        final PolynomialFunctionMatrix B = HansenUtilities.buildZeroMatrix2();
        // from Collins 4-245 and Petre's paper
        B.setElem(1, 1, new PolynomialFunction(new double[] {
            2.0
        }));

        for (int i = N0 - 1; i > Nmin - 1; i--) {
            index = i + offset;
            // Matrix of the current linear transformation
            // Petre's paper
            a.setMatrixLine(1, new PolynomialFunction[] {
                b(i), a(i)
            });
            // composition of linear transformations
            // see Petre's paper
            A = A.multiply(a);
            // store the polynomials for Hansen coefficients
            mpvec[index] = A.getMatrixLine(1);

            D = D.multiply(a);
            E = E.multiply(a);
            D = D.add(E.multiply(B));

            // store the polynomials for Hansen coefficients from the expressions
            // of derivatives
            mpvecDeriv[index] = D.getMatrixLine(1);

            if (++sliceCounter % SLICE == 0) {
                // Re-Initialisation of matrix for linear transformmations
                // The final configuration of these matrix are obtained by composition
                // of linear transformations
                A = HansenUtilities.buildIdentityMatrix2();
                D = HansenUtilities.buildZeroMatrix2();
                E = HansenUtilities.buildIdentityMatrix2();
            }

        }
    }

    /**
     * Compute the roots for the Hansen coefficients and their derivatives.
     *
     * @param chi 1 / sqrt(1 - e²)
     */
    public void computeInitValues(final double chi) {
        // compute the values for n=s and n=s+1
        // See Danielson 2.7.3-(6a,b)
        hansenRoot[0][0] = 0;
        hansenRoot[0][1] = FastMath.pow(chi, this.twosp1) / this.twots;
        hansenDerivRoot[0][0] = 0;
        hansenDerivRoot[0][1] = this.twosp1otwots * FastMath.pow(chi, this.twos);

        final int st = -s - 1;
        for (int i = 1; i < numSlices; i++) {
            for (int j = 0; j < 2; j++) {
                // Get the required polynomials
                final PolynomialFunction[] mv = mpvec[st - (i * SLICE) - j + offset];
                final PolynomialFunction[] sv = mpvecDeriv[st - (i * SLICE) - j + offset];

                //Compute the root derivatives
                hansenDerivRoot[i][j] = mv[1].value(chi) * hansenDerivRoot[i - 1][1] +
                                        mv[0].value(chi) * hansenDerivRoot[i - 1][0] +
                                        (sv[1].value(chi) * hansenRoot[i - 1][1] +
                                         sv[0].value(chi) * hansenRoot[i - 1][0]
                                        ) / chi;
                hansenRoot[i][j] =     mv[1].value(chi) * hansenRoot[i - 1][1] +
                                       mv[0].value(chi) * hansenRoot[i - 1][0];

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
    public double getValue(final int mnm1, final double chi) {

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
        double ret = v[1].value(chi) * hansenRoot[sliceNo][1];
        if (hansenRoot[sliceNo][0] != 0) {
            ret += v[0].value(chi) * hansenRoot[sliceNo][0];
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
    public double getDerivative(final int mnm1, final double chi) {

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
        double ret = v[1].value(chi) * hansenDerivRoot[sliceNo][1];
        if (hansenDerivRoot[sliceNo][0] != 0) {
            ret += v[0].value(chi) * hansenDerivRoot[sliceNo][0];
        }

        // Danielson 2.7.3-(6b)
        final PolynomialFunction[] v1 = mpvecDeriv[mnm1 + offset];
        double hret = v1[1].value(chi) * hansenRoot[sliceNo][1];
        if (hansenRoot[sliceNo][0] != 0) {
            hret += v1[0].value(chi) * hansenRoot[sliceNo][0];
        }
        ret += hret / chi;

        return ret;

    }

}
