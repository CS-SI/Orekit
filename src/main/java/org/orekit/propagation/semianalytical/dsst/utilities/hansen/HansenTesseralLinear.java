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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;
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
 */
public class HansenTesseralLinear {

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

    /** The j coefficient. */
    private int j;

    /** The number of slices needed to compute the coefficients. */
    private int numSlices;

    /**
     * The offset used to identify the polynomial that corresponds to a negative.
     * value of n in the internal array that starts at 0
     */
    private int offset;

    /** The objects used to calculate initial data by means of Newcomb operators. */
    private HansenCoefficientsBySeries[] hansenInit;

    /**
     * Constructor.
     *
     * @param nMax the maximum (absolute) value of n parameter
     * @param s s parameter
     * @param j j parameter
     * @param n0 the minimum (absolute) value of n
     * @param maxHansen maximum power of e2 in Hansen expansion
     */
    public HansenTesseralLinear(final int nMax, final int s, final int j, final int n0, final int maxHansen) {
        //Initialize the fields
        this.offset = nMax + 1;
        this.Nmin = -nMax - 1;
        this.N0 = -n0 - 4;
        this.s = s;
        this.j = j;

        //Ensure that only the needed terms are computed
        final int maxRoots = FastMath.min(4, N0 - Nmin + 4);
        this.hansenInit = new HansenCoefficientsBySeries[maxRoots];
        for (int i = 0; i < maxRoots; i++) {
            this.hansenInit[i] = new HansenCoefficientsBySeries(N0 - i + 3, s, j, maxHansen);
        }

        // The first 4 values are computed with series. No linear combination is needed.
        final int size = N0 - Nmin;
        this.numSlices = (int) FastMath.max(FastMath.ceil(((double) size) / SLICE), 1);
        hansenRoot = new double[numSlices][4];
        hansenDerivRoot = new double[numSlices][4];
        if (size > 0) {
            mpvec = new PolynomialFunction[size][];
            mpvecDeriv = new PolynomialFunction[size][];

            // Prepare the database of the associated polynomials
            generatePolynomials();
        }
    }

    /**
     * Compute polynomial coefficient a.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n, s</sup> when computing K<sub>j</sub><sup>-n-1, s</sup>
     *  and the coefficient for dK<sub>j</sub><sup>-n, s</sup> / de² when computing dK<sub>j</sub><sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(9) and Collins 4-236 and 4-240
     *  </p>
     *
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private PolynomialFunction a(final int mnm1) {
        // Collins 4-236, Danielson 2.7.3-(9)
        final double r1 = (mnm1 + 2.) * (2. * mnm1 + 5.);
        final double r2 = (2. + mnm1 + s) * (2. + mnm1 - s);
        return new PolynomialFunction(new double[] {
            0.0, 0.0, r1 / r2
        });
    }

    /**
     * Compute polynomial coefficient b.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n+1, s</sup> when computing K<sub>j</sub><sup>-n-1, s</sup>
     *  and the coefficient for dK<sub>j</sub><sup>-n+1, s</sup> / de² when computing dK<sub>j</sub><sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(9) and Collins 4-236 and 4-240
     *  </p>
     *
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private PolynomialFunction b(final int mnm1) {
        // Collins 4-236, Danielson 2.7.3-(9)
        final double r2 = (2. + mnm1 + s) * (2. + mnm1 - s);
        final double d1 = (mnm1 + 3.) * 2. * j * s / (r2 * (mnm1 + 4.));
        final double d2 = (mnm1 + 3.) * (mnm1 + 2.) / r2;
        return new PolynomialFunction(new double[] {
            0.0, -d1, -d2
        });
    }

    /**
     * Compute polynomial coefficient c.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n+3, s</sup> when computing K<sub>j</sub><sup>-n-1, s</sup>
     *  and the coefficient for dK<sub>j</sub><sup>-n+3, s</sup> / de² when computing dK<sub>j</sub><sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(9) and Collins 4-236 and 4-240
     *  </p>
     *
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private PolynomialFunction c(final int mnm1) {
        // Collins 4-236, Danielson 2.7.3-(9)
        final double r1 = j * j * (mnm1 + 2.);
        final double r2 = (mnm1 + 4.) * (2. + mnm1 + s) * (2. + mnm1 - s);

        return new PolynomialFunction(new double[] {
            0.0, 0.0, r1 / r2
        });
    }

    /**
     * Compute polynomial coefficient d.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n-1, s</sup> / dχ when computing dK<sub>j</sub><sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(9) and Collins 4-236 and 4-240
     *  </p>
     *
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private PolynomialFunction d(final int mnm1) {
        // Collins 4-236, Danielson 2.7.3-(9)
        return new PolynomialFunction(new double[] {
            0.0, 0.0, 1.0
        });
    }

    /**
     * Compute polynomial coefficient f.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n+1, s</sup> / dχ when computing dK<sub>j</sub><sup>-n-1, s</sup> / de²
     *  </p>
     *
     *  <p>
     *  See Danielson 2.7.3-(9) and Collins 4-236 and 4-240
     *  </p>
     *
     * @param n index
     * @return the polynomial
     */
    private PolynomialFunction f(final int n) {
        // Collins 4-236, Danielson 2.7.3-(9)
        final double r1 = (n + 3.0) * j * s;
        final double r2 = (n + 4.0) * (2.0 + n + s) * (2.0 + n - s);
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


        // Initialization of the matrices for linear transformations
        // The final configuration of these matrices are obtained by composition
        // of linear transformations

        // The matrix of polynomials associated to Hansen coefficients, Petre's
        // paper
        PolynomialFunctionMatrix A = HansenUtilities.buildIdentityMatrix4();

        // The matrix of polynomials associated to derivatives, Petre's paper
        final PolynomialFunctionMatrix B = HansenUtilities.buildZeroMatrix4();
        PolynomialFunctionMatrix D = HansenUtilities.buildZeroMatrix4();
        final PolynomialFunctionMatrix a = HansenUtilities.buildZeroMatrix4();

        // The matrix of the current linear transformation
        a.setMatrixLine(0, new PolynomialFunction[] {
            HansenUtilities.ZERO, HansenUtilities.ONE, HansenUtilities.ZERO, HansenUtilities.ZERO
        });
        a.setMatrixLine(1, new PolynomialFunction[] {
            HansenUtilities.ZERO, HansenUtilities.ZERO, HansenUtilities.ONE, HansenUtilities.ZERO
        });
        a.setMatrixLine(2, new PolynomialFunction[] {
            HansenUtilities.ZERO, HansenUtilities.ZERO, HansenUtilities.ZERO, HansenUtilities.ONE
        });
        // The generation process
        int index;
        int sliceCounter = 0;
        for (int i = N0 - 1; i > Nmin - 1; i--) {
            index = i + this.offset;
            // The matrix of the current linear transformation is updated
            // Petre's paper
            a.setMatrixLine(3, new PolynomialFunction[] {
                    c(i), HansenUtilities.ZERO, b(i), a(i)
            });

            // composition of the linear transformations to calculate
            // the polynomials associated to Hansen coefficients
            // Petre's paper
            A = A.multiply(a);
            // store the polynomials for Hansen coefficients
            mpvec[index] = A.getMatrixLine(3);
            // composition of the linear transformations to calculate
            // the polynomials associated to derivatives
            // Petre's paper
            D = D.multiply(a);

            //Update the B matrix
            B.setMatrixLine(3, new PolynomialFunction[] {
                HansenUtilities.ZERO, f(i),
                HansenUtilities.ZERO, d(i)
            });
            D = D.add(A.multiply(B));

            // store the polynomials for Hansen coefficients from the
            // expressions of derivatives
            mpvecDeriv[index] = D.getMatrixLine(3);

            if (++sliceCounter % SLICE == 0) {
                // Re-Initialisation of matrix for linear transformmations
                // The final configuration of these matrix are obtained by composition
                // of linear transformations
                A = HansenUtilities.buildIdentityMatrix4();
                D = HansenUtilities.buildZeroMatrix4();
            }
        }
    }

    /**
     * Compute the values for the first four coefficients and their derivatives by means of series.
     *
     * @param e2 e²
     * @param chi &Chi;
     * @param chi2 &Chi;²
     */
    public void computeInitValues(final double e2, final double chi, final double chi2) {
        // compute the values for n, n+1, n+2 and n+3 by series
        // See Danielson 2.7.3-(10)
        //Ensure that only the needed terms are computed
        final int maxRoots = FastMath.min(4, N0 - Nmin + 4);
        for (int i = 0; i < maxRoots; i++) {
            final DerivativeStructure hansenKernel = hansenInit[i].getValue(e2, chi, chi2);
            this.hansenRoot[0][i] = hansenKernel.getValue();
            this.hansenDerivRoot[0][i] = hansenKernel.getPartialDerivative(1);
        }

        for (int i = 1; i < numSlices; i++) {
            for (int k = 0; k < 4; k++) {
                final PolynomialFunction[] mv = mpvec[N0 - (i * SLICE) - k + 3 + offset];
                final PolynomialFunction[] sv = mpvecDeriv[N0 - (i * SLICE) - k + 3 + offset];

                hansenDerivRoot[i][k] = mv[3].value(chi) * hansenDerivRoot[i - 1][3] +
                                        mv[2].value(chi) * hansenDerivRoot[i - 1][2] +
                                        mv[1].value(chi) * hansenDerivRoot[i - 1][1] +
                                        mv[0].value(chi) * hansenDerivRoot[i - 1][0] +
                                        sv[3].value(chi) * hansenRoot[i - 1][3] +
                                        sv[2].value(chi) * hansenRoot[i - 1][2] +
                                        sv[1].value(chi) * hansenRoot[i - 1][1] +
                                        sv[0].value(chi) * hansenRoot[i - 1][0];

                hansenRoot[i][k] =  mv[3].value(chi) * hansenRoot[i - 1][3] +
                                    mv[2].value(chi) * hansenRoot[i - 1][2] +
                                    mv[1].value(chi) * hansenRoot[i - 1][1] +
                                    mv[0].value(chi) * hansenRoot[i - 1][0];
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
    public double getValue(final int mnm1, final double chi) {
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
        return v[3].value(chi) * hansenRoot[sliceNo][3] +
               v[2].value(chi) * hansenRoot[sliceNo][2] +
               v[1].value(chi) * hansenRoot[sliceNo][1] +
               v[0].value(chi) * hansenRoot[sliceNo][0];

    }

    /**
     * Compute the value of the derivative dK<sub>j</sub><sup>-n-1, s</sup> / de².
     *
     * @param mnm1 -n-1
     * @param chi χ
     * @return the derivative dK<sub>j</sub><sup>-n-1, s</sup> / de²
     */
    public double getDerivative(final int mnm1, final double chi) {

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

        return v[3].value(chi)  * hansenDerivRoot[sliceNo][3] +
               v[2].value(chi)  * hansenDerivRoot[sliceNo][2] +
               v[1].value(chi)  * hansenDerivRoot[sliceNo][1] +
               v[0].value(chi)  * hansenDerivRoot[sliceNo][0] +
               vv[3].value(chi) * hansenRoot[sliceNo][3] +
               vv[2].value(chi) * hansenRoot[sliceNo][2] +
               vv[1].value(chi) * hansenRoot[sliceNo][1] +
               vv[0].value(chi) * hansenRoot[sliceNo][0];

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
    private static class HansenCoefficientsBySeries {

        /** -n-1 coefficient. */
        private final int mnm1;

        /** s coefficient. */
        private final int s;

        /** j coefficient. */
        private final int j;

        /** Max power in e² for the Newcomb's series expansion. */
        private final int maxNewcomb;

        /** Polynomial representing the serie. */
        private PolynomialFunction polynomial;

        /**
         * Class constructor.
         *
         * @param mnm1 -n-1 value
         * @param s s value
         * @param j j value
         * @param maxHansen max power of e² in series expansion
         */
        HansenCoefficientsBySeries(final int mnm1, final int s,
                                          final int j, final int maxHansen) {
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
        public DerivativeStructure getValue(final double e2, final double chi, final double chi2) {

            //Estimation of the serie expansion at e2
            final DerivativeStructure serie = polynomial.value(
                    new DerivativeStructure(1, 1, 0, e2));

            final double value      =  FastMath.pow(chi2, -mnm1 - 1) * serie.getValue() / chi;
            final double coef       = -(mnm1 + 1.5);
            final double derivative = coef * chi2 * value +
                                      FastMath.pow(chi2, -mnm1 - 1) * serie.getPartialDerivative(1) / chi;
            return new DerivativeStructure(1, 1, value, derivative);
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
