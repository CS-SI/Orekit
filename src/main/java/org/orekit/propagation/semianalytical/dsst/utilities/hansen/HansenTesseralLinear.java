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
import org.apache.commons.math3.util.FastMath;
import org.orekit.propagation.semianalytical.dsst.utilities.NewcombOperators;

/**
 * Hansen coefficients K(t,n,s) for t!=0 and n < 0.
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
    /**
     * The first vector of polynomials associated to Hansen coefficients and
     * derivatives.
     */
    private PolynomialFunction[][] mpvec;

    /** The second vector of polynomials associated only to derivatives. */
    private PolynomialFunction[][] mpvecDeriv;

    /** The minimum value for the order. */
    private int Nmin;

    /** The index of the initial condition, Petre's paper. */
    private int N0;

    /** The s coefficient. */
    private int s;

    /** The j coefficient. */
    private int j;

    /**
     * The offset used to identify the polynomial that corresponds to a negative.
     * value of n in the internal array that starts at 0
     */
    private int offset;

    /** The objects used to calculate initial data by means of newcomb operators. */
    private HansenCoefficientsBySeries[] hansenInit;

    /** Initial values of Hansen coefficients. */
    private double[] hansenValue;
    /** Initial values of Hansen coefficients derivatives. */
    private double[] hansenDeriv;

    /**
     * Constructor.
     *
     * @param nMax the maximum (absolute) value of n parameter
     * @param s s parameter
     * @param j j parameter
     * @param n0 the minimum (absolute) value of n
     */
    public HansenTesseralLinear(final int nMax, final int s, final int j, final int n0) {
        //Initialize the fields
        this.offset = nMax + 1;
        this.Nmin = -nMax - 1;
        this.N0 = -n0 - 4;
        this.s = s;
        this.j = j;

        // Create the objects used to generate the initial values
        this.hansenInit = new HansenCoefficientsBySeries[4];
        for (int i = 0; i < 4; i++) {
            this.hansenInit[i] = new HansenCoefficientsBySeries(N0 + i, s, j);
        }

        this.hansenValue = new double[4];
        this.hansenDeriv = new double[4];

        // The first 4 values are computed with series. No linear combination is needed.
        final int size = N0 - Nmin;
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
     *  and the coefficient for dK<sub>j</sub><sup>-n, s</sup> / de<sup>2</sup> when computing dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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
     *  and the coefficient for dK<sub>j</sub><sup>-n+1, s</sup> / de<sup>2</sup> when computing dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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
     *  and the coefficient for dK<sub>j</sub><sup>-n+3, s</sup> / de<sup>2</sup> when computing dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n-1, s</sup> / d&chi; when computing dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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
     *  It is used to generate the coefficient for K<sub>j</sub><sup>-n+1, s</sup> / d&chi; when computing dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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


        // The matrix that contains the coefficients at each step
        final PolynomialFunctionMatrix a = new PolynomialFunctionMatrix(4);

        // Initialization of the matrices for linear transformations
        // The final configuration of these matrices are obtained by composition
        // of linear transformations

        // The matrix of polynomials associated to Hansen coefficients, Petre's
        // paper
        PolynomialFunctionMatrix A = HansenUtilities.buildIdentityMatrix4();

        // The matrix of polynomials associated to derivatives, Petre's paper
        final PolynomialFunctionMatrix B = HansenUtilities.buildZeroMatrix4();
        PolynomialFunctionMatrix D = HansenUtilities.buildZeroMatrix4();

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
        }
    }

    /**
     * Compute the values for the first four coefficients and their derivatives by means of series.
     *
     * @param e2 e<sup>2</sup>
     * @param chi &Chi;
     * @param chi2 &Chi;<sup>2</sup>
     * @param precision the precision that will be used in the series
     */
    public void computeInitValues(final double e2, final double chi, final double chi2, final int precision) {
        for (int i = 0; i < 4; i++) {
            this.hansenValue[i] = this.hansenInit[i].getValue(e2, chi, chi2, precision);
            this.hansenDeriv[i] = this.hansenInit[i].getDerivativeValue(e2, chi, chi2,
                    precision, this.hansenValue[i]);
        }
    }

    /**
     * Compute the value of the Hansen coefficient K<sub>j</sub><sup>-n-1, s</sup>.
     *
     * @param mnm1 -n-1
     * @param chi &chi;
     * @return the coefficient K<sub>j</sub><sup>-n-1, s</sup>
     */
    public double getValue(final int mnm1, final double chi) {

        // Check if the required coefficient is one of the initialization values
        final int i = mnm1 - N0;
        if (i >= 0 && i < 4) {
            return hansenValue[i];
        }

        // Computes the coefficient by linear transformation
        // Danielson 2.7.3-(9) or Collins 4-236 and Petre's paper
        final PolynomialFunction[] vv = mpvec[mnm1 + offset];
        final double v0 = vv[0].value(chi);
        final double v1 = vv[1].value(chi);
        final double v2 = vv[2].value(chi);
        final double v3 = vv[3].value(chi);
        return v0 * hansenValue[3] + v1 * hansenValue[2] + v2 * hansenValue[1] + v3 * hansenValue[0];

    }

    /**
     * Compute the value of the derivative dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>.
     *
     * @param mnm1 -n-1
     * @param chi &chi;
     * @return the derivative dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup>
     */
    public double getDerivative(final int mnm1, final double chi) {

        // Check if the required derivative is one of the initialization values
        final int i = mnm1 - N0;
        if (i >= 0 && i < 4) {
            return hansenDeriv[i];
        }

        // Compute the derivative by linear transformation
        // Collins 4-240 and Petre's paper
        PolynomialFunction[] vv = mpvec[mnm1 + offset];
        final double dv0 = vv[0].value(chi);
        final double dv1 = vv[1].value(chi);
        final double dv2 = vv[2].value(chi);
        final double dv3 = vv[3].value(chi);
        double ret = dv0 * hansenDeriv[3] + dv1 * hansenDeriv[2] + dv2 * hansenDeriv[1] + dv3 * hansenDeriv[0];

        vv = mpvecDeriv[mnm1 + offset];
        final double v0 = vv[0].value(chi);
        final double v1 = vv[1].value(chi);
        final double v2 = vv[2].value(chi);
        final double v3 = vv[3].value(chi);
        ret += v0 * hansenValue[3] + v1 * hansenValue[2] + v2 * hansenValue[1] + v3 * hansenValue[0];

        return ret;

    }

    /**
     * Compute a Hansen coefficient with series.
     *
     */
    private class HansenCoefficientsBySeries {

        /** -n-1 coefficient. */
        private final int mnm1;
        /** s coefficient. */
        private final int s;
        /** j coefficient. */
        private final int j;

        /**
         * Class constructor.
         *
         * @param mnm1 -n-1 value
         * @param s s value
         * @param j j value
         */
        public HansenCoefficientsBySeries(final int mnm1, final int s, final int j) {
            this.mnm1 = mnm1;
            this.s = s;
            this.j = j;
        }

        /**
         * Compute the value of K<sub>j</sub><sup>-n-1, s</sup> with series.
         *
         * @param e2 e<sup>2</sup>
         * @param chi &Chi;
         * @param chi2 &Chi;<sup>2</sup>
         * @param maxNewcomb Max power of e<sup>2</sup> in series expansion
         * @return the value of K<sub>j</sub><sup>-n-1, s</sup>
         */
        public double getValue(final double e2, final double chi, final double chi2, final int maxNewcomb) {
            // Initialization
            final int a = FastMath.max(j - s, 0);
            final int b = FastMath.max(s - j, 0);
            // Expansion until maxNewcomb, the maximum power in e^2 for the
            // Kernel value
            double sum = NewcombOperators.getValue(maxNewcomb + a, maxNewcomb + b, mnm1, s);
            for (int alpha = maxNewcomb - 1; alpha >= 0; alpha--) {
                sum *= e2;
                sum += NewcombOperators.getValue(alpha + a, alpha + b, mnm1, s);
            }
            // Kernel value from equation 2.7.3-(10)
            final double value = FastMath.pow(chi2, -mnm1 - 1) * sum / chi;

            return value;
        }

        /**
         *  Compute the value of dK<sub>j</sub><sup>-n-1, s</sup> / de<sup>2</sup> with series.
         *
         * @param e2 e<sup>2</sup>
         * @param chi &Chi;
         * @param chi2 &Chi;<sup>2</sup>
         * @param maxNewcomb Max power of e<sup>2</sup> in series expansion
         * @param Kjmnm1s the value of K<sub>j</sub><sup>-n-1, s</sup>
         * @return derivative
         */
        public double getDerivativeValue(final double e2, final double chi, final double chi2,
                                         final int maxNewcomb, final double Kjmnm1s) {
            // Initialization
            final int a = FastMath.max(j - s, 0);
            final int b = FastMath.max(s - j, 0);
            // Expansion until maxNewcomb-1, the maximum power in e^2 for the
            // Kernel derivative
            double sum = maxNewcomb * NewcombOperators.getValue(maxNewcomb + a, maxNewcomb + b, mnm1, s);
            for (int alpha = maxNewcomb - 1; alpha >= 1; alpha--) {
                sum *= e2;
                sum += alpha * NewcombOperators.getValue(alpha + a, alpha + b, mnm1, s);
            }
            // Kernel derivative from equation 3.3-(5)
            final double coef = -(mnm1 + 1.5);
            final double derivative = coef * chi2 * Kjmnm1s + FastMath.pow(chi2, -mnm1 - 1) * sum / chi;

            return derivative;
        }
    }
}
