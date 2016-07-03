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
 * Hansen coefficients K(t,n,s) for t=0 and n &gt; 0.
 * <p>
 * Implements Collins 4-254 or Danielson 2.7.3-(7) for Hansen Coefficients and
 * Danielson 3.2-(3) for derivatives. The recursions are transformed into
 * composition of linear transformations to obtain the associated polynomials
 * for coefficients and their derivatives - see Petre's paper
 *
 * @author Petre Bazavan
 * @author Lucian Barbulescu
 */
public class HansenThirdBodyLinear {

    /** The number of coefficients that will be computed with a set of roots. */
    private  static final int SLICE = 10;

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

    /** The number of slices needed to compute the coefficients. */
    private int numSlices;

    /** The maximum order of n indexes. */
    private int nMax;

    /** The index of the initial condition, Petre's paper. */
    private int N0;

    /** The s index. */
    private int s;

    /** (-1)<sup>s</sup> * (2*s + 1)!! / (s+1)!  */
    private double twosp1dfosp1f;

    /** (-1)<sup>s</sup> * (2*s + 1)!! / (s+2)!  */
    private double twosp1dfosp2f;

    /** (-1)<sup>s</sup> * 2 * (2*s + 1)!! / (s+2)!  */
    private double two2sp1dfosp2f;

    /** (2*s + 3). */
    private double twosp3;

    /**
     * Constructor.
     *
     * @param nMax the maximum value of n
     * @param s the value of s
     */
    public HansenThirdBodyLinear(final int nMax, final int s) {

        // initialise fields
        this.nMax = nMax;
        N0 = s;
        this.s = s;

        // initialization of structures for stored data
        mpvec = new PolynomialFunction[this.nMax + 1][];
        mpvecDeriv = new PolynomialFunction[this.nMax + 1][];

        //Compute the fields that will be used to determine the initial values for the coefficients
        this.twosp1dfosp1f = (s % 2 == 0) ? 1.0 : -1.0;
        for (int i = s; i >= 1; i--) {
            this.twosp1dfosp1f *= (2.0 * i + 1.0) / (i + 1.0);
        }

        this.twosp1dfosp2f = this.twosp1dfosp1f / (s + 2.);
        this.twosp3 = 2 * s + 3;
        this.two2sp1dfosp2f = 2 * this.twosp1dfosp2f;

        // initialization of structures for stored data
        mpvec = new PolynomialFunction[this.nMax + 1][];
        mpvecDeriv = new PolynomialFunction[this.nMax + 1][];

        this.numSlices = (int) FastMath.ceil(((double) nMax - s - 1) / SLICE);
        hansenRoot = new double[numSlices][2];
        hansenDerivRoot = new double[numSlices][2];

        // Prepare the database of the associated polynomials
        generatePolynomials();

    }

    /**
     * Compute polynomial coefficient a.
     *
     *  <p>
     *  It is used to generate the coefficient for K₀<sup>n-1, s</sup> when computing K₀<sup>n, s</sup>
     *  and the coefficient for dK₀<sup>n-1, s</sup> / d&Chi; when computing dK₀<sup>n, s</sup> / d&Chi;
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(7c) and Collins 4-254 and 4-257
     *  </p>
     *
     * @param n n value
     * @return the polynomial
     */
    private PolynomialFunction a(final int n) {
        // from recurrence Danielson 2.7.3-(7c), Collins 4-254
        final double r1 = 2 * n + 1;
        final double r2 = n + 1;

        return new PolynomialFunction(new double[] {
            r1 / r2
        });
    }

    /**
     * Compute polynomial coefficient b.
     *
     *  <p>
     *  It is used to generate the coefficient for K₀<sup>n-2, s</sup> when computing K₀<sup>n, s</sup>
     *  and the coefficient for dK₀<sup>n-2, s</sup> / d&Chi; when computing dK₀<sup>n, s</sup> / d&Chi;
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(7c) and Collins 4-254 and 4-257
     *  </p>
     *
     * @param n n value
     * @return the polynomial
     */
    private PolynomialFunction b(final int n) {
        // from recurrence Danielson 2.7.3-(7c), Collins 4-254
        final double r1 = (n + s) * (n - s);
        final double r2 = n * (n + 1);

        return new PolynomialFunction(new double[] {
            0.0, 0.0, -r1 / r2
        });
    }

    /**
     * Compute polynomial coefficient d.
     *
     *  <p>
     *  It is used to generate the coefficient for K₀<sup>n-2, s</sup> when computing dK₀<sup>n, s</sup> / d&Chi;
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(7c) and Collins 4-254 and 4-257
     *  </p>
     *
     * @param n n value
     * @return the polynomial
     */
    private PolynomialFunction d(final int n) {
        // from Danielson 3.2-(3b)
        final double r1 = 2 * (n + s) * (n - s);
        final double r2 = n * (n + 1);

        return new PolynomialFunction(new double[] {
            0.0, 0.0, 0.0, r1 / r2
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

        // Initialization of the matrices for linear transformations
        // The final configuration of these matrices are obtained by composition
        // of linear transformations

        // the matrix A for the polynomials associated
        // to Hansen coefficients, Petre's pater
        PolynomialFunctionMatrix A = HansenUtilities.buildIdentityMatrix2();

        // the matrix D for the polynomials associated
        // to derivatives, Petre's paper
        final PolynomialFunctionMatrix B = HansenUtilities.buildZeroMatrix2();
        PolynomialFunctionMatrix D = HansenUtilities.buildZeroMatrix2();
        PolynomialFunctionMatrix E = HansenUtilities.buildIdentityMatrix2();

        // The matrix that contains the coefficients at each step
        final PolynomialFunctionMatrix a = HansenUtilities.buildZeroMatrix2();
        a.setElem(0, 1, HansenUtilities.ONE);

        // The generation process
        for (int i = N0 + 2; i <= nMax; i++) {
            // Collins 4-254 or Danielson 2.7.3-(7)
            // Petre's paper
            // The matrix of the current linear transformation is actualized
            a.setMatrixLine(1, new PolynomialFunction[] {
                b(i), a(i)
            });

            // composition of the linear transformations to calculate
            // the polynomials associated to Hansen coefficients
            A = A.multiply(a);
            // store the polynomials associated to Hansen coefficients
            this.mpvec[i] = A.getMatrixLine(1);
            // composition of the linear transformations to calculate
            // the polynomials associated to derivatives
            // Danielson 3.2-(3b) and Petre's paper
            D = D.multiply(a);
            if (sliceCounter % SLICE != 0) {
                a.setMatrixLine(1, new PolynomialFunction[] {
                    b(i - 1), a(i - 1)
                });
                E = E.multiply(a);
            }

            B.setElem(1, 0, d(i));
            // F = E.prod(B);
            D = D.add(E.multiply(B));
            // store the polynomials associated to the derivatives
            this.mpvecDeriv[i] = D.getMatrixLine(1);

            if (++sliceCounter % SLICE == 0) {
                // Re-Initialization of the matrices for linear transformations
                // The final configuration of these matrices are obtained by composition
                // of linear transformations
                A = HansenUtilities.buildIdentityMatrix2();
                D = HansenUtilities.buildZeroMatrix2();
                E = HansenUtilities.buildIdentityMatrix2();
            }
        }
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
    public void computeInitValues(final double chitm1, final double chitm2, final double chitm3) {
        this.hansenRoot[0][0] = this.twosp1dfosp1f;
        this.hansenRoot[0][1] = this.twosp1dfosp2f * (this.twosp3 - chitm2);
        this.hansenDerivRoot[0][0] = 0;
        this.hansenDerivRoot[0][1] = this.two2sp1dfosp2f * chitm3;

        for (int i = 1; i < numSlices; i++) {
            for (int j = 0; j < 2; j++) {
                // Get the required polynomials
                final PolynomialFunction[] mv = mpvec[s + (i * SLICE) + j];
                final PolynomialFunction[] sv = mpvecDeriv[s + (i * SLICE) + j];

                //Compute the root derivatives
                hansenDerivRoot[i][j] = mv[1].value(chitm1) * hansenDerivRoot[i - 1][1] +
                                        mv[0].value(chitm1) * hansenDerivRoot[i - 1][0] +
                                        sv[1].value(chitm1) * hansenRoot[i - 1][1] +
                                        sv[0].value(chitm1) * hansenRoot[i - 1][0];

                //Compute the root Hansen coefficients
                hansenRoot[i][j] =  mv[1].value(chitm1) * hansenRoot[i - 1][1] +
                                    mv[0].value(chitm1) * hansenRoot[i - 1][0];
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
    public double getValue(final int n, final double chitm1) {

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
        double ret = v[1].value(chitm1) * hansenRoot[sliceNo][1];
        if (hansenRoot[sliceNo][0] != 0) {
            ret += v[0].value(chitm1) * hansenRoot[sliceNo][0];
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
    public double getDerivative(final int n, final double chitm1) {
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
        double ret = v[1].value(chitm1) * hansenDerivRoot[sliceNo][1];
        if (hansenDerivRoot[sliceNo][0] != 0) {
            ret += v[0].value(chitm1) * hansenDerivRoot[sliceNo][0];
        }

        // Danielson 2.7.3-(7c)/Collins 4-254 and Petre's paper
        final PolynomialFunction[] v1 = mpvecDeriv[n];
        ret += v1[1].value(chitm1) * hansenRoot[sliceNo][1];
        if (hansenRoot[sliceNo][0] != 0) {
            ret += v1[0].value(chitm1) * hansenRoot[sliceNo][0];
        }
        return ret;

    }

}
