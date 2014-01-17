/* Copyright 2002-2014 CS Systèmes d'Information
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

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * Hansen coefficients K(t,n,s) for t=0 and n > 0.
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
    /**
     * The first vector of polynomials associated to Hansen coefficients and
     * derivatives.
     */
    private PolynomialFunction[][] mpvec;

    /** The second vector of polynomials associated only to derivatives. */
    private PolynomialFunction[][] mpvecDeriv;

    /** The maximum order of n indexes. */
    private int Nmax;

    /** The index of the initial condition, Petre's paper. */
    private int N0;

    /** The s index. */
    private int s;

    /** The value of K<sub>0</sub><sup>s,s</sup> computed with Collins (4-255). */
    private double hansenS;

    /** The value of K<sub>0</sub><sup>s+1,s</sup> computed with Collins (4-256). */
    private double hansenSp1;
    /** Coefficient used to compute K<sub>0</sub><sup>s+1,s</sup>.
     *
     * <p>
     *  hansenSp1Coef1 = ( (2*s+1)!! / (s+2)! )
     * </p>
     *  see Collins (4-256)
     */
    private double hansenSp1Coef1;
    /** Coefficient used to compute K<sub>0</sub><sup>s+1,s</sup>.
     *
     * <p>
     *  hansenSp1Coef2 = ( 2*s+3 )
     * </p>
     *  see Collins (4-256)
     */
    private double hansenSp1Coef2;

    /** The value of dK<sub>0</sub><sup>s+1,s</sup> / d&Chi; computed with Collins (4-259). */
    private double hansenDerivSp1;
    /** Coefficient used to compute dK<sub>0</sub><sup>s+1,s</sup> / d&Chi;.
     *
     * <p>
     *  hansenDerivSp1Coef1 = ( 2 * (2*s+1)!! / (s+2)! )
     * </p>
     *  see Collins (4-256)
     */
    private double hansenDerivSp1Coef1;

    /**
     * Constructor.
     *
     * @param Nmax the maximum value of n
     * @param s the value of s
     */
    public HansenThirdBodyLinear(final int Nmax, final int s) {

        this.Nmax = Nmax;
        N0 = s;
        this.s = s;

        // initialization of structures for stored data
        mpvec = new PolynomialFunction[this.Nmax + 1][];
        mpvecDeriv = new PolynomialFunction[this.Nmax + 1][];

        // Prepare the database of the associated polynomials
        generatePolynomials();
        computeInitialValues();
    }

    /**
     * Compute polynomial coefficient a.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>0</sub><sup>n-1, s</sup> when computing K<sub>0</sub><sup>n, s</sup>
     *  and the coefficient for dK<sub>0</sub><sup>n-1, s</sup> / d&Chi; when computing dK<sub>0</sub><sup>n, s</sup> / d&Chi;
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
     *  It is used to generate the coefficient for K<sub>0</sub><sup>n-2, s</sup> when computing K<sub>0</sub><sup>n, s</sup>
     *  and the coefficient for dK<sub>0</sub><sup>n-2, s</sup> / d&Chi; when computing dK<sub>0</sub><sup>n, s</sup> / d&Chi;
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
     *  It is used to generate the coefficient for K<sub>0</sub><sup>n-2, s</sup> when computing dK<sub>0</sub><sup>n, s</sup> / d&Chi;
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
        for (int i = N0 + 2; i <= Nmax; i++) {
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
            if (i > N0 + 2) {
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
        }
    }

    /**
     * Compute the initial values (see Collins, 4-255, 4-256 and 4.259)
     * <p>
     * K<sub>0</sub><sup>s, s</sup> = (-1)<sup>s</sup> * ( (2*s+1)!! / (s+1)! )
     * </p>
     * <p>
     * K<sub>0</sub><sup>s+1, s</sup> = (-1)<sup>s</sup> * ( (2*s+1)!! / (s+2)!
     * ) * (2*s+3 - &chi;<sup>-2</sup>)
     * </p>
     * <p>
     * dK<sub>0</sub><sup>s+1, s</sup> / d&chi; = = (-1)<sup>s</sup> * 2 * (
     * (2*s+1)!! / (s+2)! ) * &chi;<sup>-3</sup>
     * </p>
     */
    private void computeInitialValues() {
        // the value for K<sub>0</sub><sup>s, s</s> does not depend of &chi;
        hansenS = (s % 2 == 0) ? 1.0 : -1.0;
        for (int i = s; i >= 1; i--) {
            hansenS *= (2.0 * i + 1.0) / (i + 1.0);
        }

        // the value for K<sub>0</sub><sup>s+1, s</s> is hansenSplusoneCoef1 *
        // (hansenSplusoneCoef2 - &chi;<sup>-2</sup>)
        hansenSp1Coef1 = hansenS / (s + 2.);
        hansenSp1Coef2 = 2. * s + 3.;

        // the value for dK<sub>0</sub><sup>s+1, s</s> is hansenDerivSplusone *
        // &chi;<sup>-3</sup>
        hansenDerivSp1Coef1 = hansenSp1Coef1 * 2.;
    }

    /**
     * Compute the value of the Hansen coefficient K<sub>0</sub><sup>n, s</sup>.
     *
     * @param n n value
     * @param chitm1 &chi;<sup>-1</sup>
     * @return the coefficient K<sub>0</sub><sup>n, s</sup>
     */
    public double getValue(final int n, final double chitm1) {
        // Danielson 2.7.3-(7a,b)
        if (n == s) {
            return hansenS;
        }

        //Compute K<sub>0</sub><sup>s+1, s</sup>
        hansenSp1 = hansenSp1Coef1 * (hansenSp1Coef2 - chitm1 * chitm1);
        if (n == s + 1) {
            return hansenSp1;
        }
        // Computes the coefficient by linear transformation
        // Danielson 2.7.3-(7c)/Collins 4-254 and Petre's paper
        final PolynomialFunction[] vv = mpvec[n];
        return vv[0].value(chitm1) * hansenS + vv[1].value(chitm1) * hansenSp1;

    }

    /**
     * Compute the value of the Hansen coefficient dK<sub>0</sub><sup>n, s</sup> / d&Chi;.
     *
     * @param n n value
     * @param chitm1 &chi;<sup>-1</sup>
     * @return the coefficient dK<sub>0</sub><sup>n, s</sup> / d&Chi;
     */
    public double getDerivative(final int n, final double chitm1) {
        if (n == s) {
            return 0;
        }
        //Compute dK<sub>0</sub><sup>s+1, s</sup> / d&Chi;
        hansenDerivSp1 = hansenDerivSp1Coef1 * chitm1 * chitm1 * chitm1;
        if (n == s + 1) {
            return hansenDerivSp1;
        }
        //Compute K<sub>0</sub><sup>s+1, s</sup>
        hansenSp1 = hansenSp1Coef1 * (hansenSp1Coef2 - chitm1 * chitm1);

        // Computes the coefficient by linear transformation
        // Danielson 2.7.3-(7c)/Collins 4-254 and Petre's paper
        final PolynomialFunction[] v  = mpvec[n];
        final PolynomialFunction[] vv = mpvecDeriv[n];
        return v[1].value(chitm1) * hansenDerivSp1 +
               vv[0].value(chitm1) * hansenS + vv[1].value(chitm1) * hansenSp1;

    }

}
