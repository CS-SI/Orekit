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

/**
 * Hansen coefficients K(t,n,s) for t=0 and n < 0.
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
    /**
     * The first vector of polynomials associated to Hansen coefficients and
     * derivatives.
     */
    private PolynomialFunction[] mpvec;

    /** The second vector of polynomials associated only to derivatives. */
    private PolynomialFunction[] mpvecDeriv;

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

    /** 2<sup>s</sup>. */
    private double twots;

    /** 2<sup>s-1</sup>. */
    private double twotsm1;

    /** 2*s+1. */
    private int twosp1;

    /** 2*s-2. */
    private int twosm2;

    /** 2*s. */
    private int twos;

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
        this.twotsm1 = FastMath.pow(2., s - 1);
        this.twos = 2 * s;
        this.twosp1 = this.twos + 1;
        this.twosm2 = this.twos - 2;

        // prepare structures for stored data
        final int size = nMax - s - 1;
        mpvec = new PolynomialFunction[size];
        mpvecDeriv = new PolynomialFunction[size];

        // Prepare the data base of associated polynomials
        generatePolynomials();
    }

    /**
     * Compute polynomial coefficient a.
     *
     *  <p>
     *  It is used to generate the coefficient for K<sub>0</sub><sup>-n, s</sup> when computing K<sub>0</sub><sup>-n-1, s</sup>
     *  and the coefficient for dK<sub>0</sub><sup>-n, s</sup> / de<sup>2</sup> when computing dK<sub>0</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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
     *  It is used to generate the coefficient for K<sub>0</sub><sup>-n+1, s</sup> when computing K<sub>0</sub><sup>-n-1, s</sup>
     *  and the coefficient for dK<sub>0</sub><sup>-n+1, s</sup> / de<sup>2</sup> when computing dK<sub>0</sub><sup>-n-1, s</sup> / de<sup>2</sup>
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

        // Initialisation of matrix for linear transformmations
        // The final configuration of these matrix are obtained by composition
        // of linear transformations
        PolynomialFunctionMatrix A = HansenUtilities.buildIdentityMatrix2();
        final PolynomialFunctionMatrix B = HansenUtilities.buildZeroMatrix2();
        PolynomialFunctionMatrix D = HansenUtilities.buildZeroMatrix2();
        PolynomialFunctionMatrix E = HansenUtilities.buildIdentityMatrix2();

        // from Collins 4-245 and Petre's paper
        B.setElem(1, 1, new PolynomialFunction(new double[] {
            0, 0, 1
        }));

        // generation of polynomials associated to Hansen coefficients and to
        // their derivatives
        final PolynomialFunctionMatrix a = HansenUtilities.buildZeroMatrix2();
        a.setElem(0, 1, HansenUtilities.ONE);

        int index;
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
            mpvec[index] = A.getElem(1, 1);

            D = D.multiply(a);
            E = E.multiply(a);
            D = D.add(E.multiply(B));

            // store the polynomials for Hansen coefficients from the expressions
            // of derivatives
            mpvecDeriv[index] = D.getElem(1, 1);
        }
    }

    /**
     * Get the K<sub>0</sub><sup>-n-1,s</sup> coefficient value. <br />
     * The s value is given in the class constructor <br />
     *
     * @param mnm1 (-n-1) coefficient
     * @param chi The value of &chi;
     * @return K<sub>0</sub><sup>-n-1,s</sup>
     */
    public double getValue(final int mnm1, final double chi) {
        // Danielson 2.7.3-(6a,b)
        if (mnm1 == N0 + 1) {
            return 0;
        }

        final double han1 = FastMath.pow(chi, twosp1) / twots;
        if (mnm1 == N0) {
            return han1;
        }

        // Danielson 2.7.3-(6c)/Collins 4-242 and Petre's paper
        return mpvec[mnm1 + offset].value(chi) * han1;
    }

    /**
     * Get the dK<sub>0</sub><sup>-n-1,s</sup> / d&Chi; coefficient derivative. <br />
     * The s value is given in the class constructor <br />
     *
     * @param mnm1 (-n-1) coefficient
     * @param chi The value of &chi;
     * @return dK<sub>0</sub><sup>-n-1,s</sup> / d&Chi;
     */
    public double getDerivative(final int mnm1, final double chi) {
        // Danielson 3.1-(7a)
        if (mnm1 == N0 + 1) {
            return 0;
        }

        final double hanDeriv1 = this.twosp1 * FastMath.pow(chi, twos) / twots;
        if (mnm1 == N0) {
            return hanDeriv1;
        }

        // Danielson 3.1-(7c) and Petre's paper
        double ret = mpvec[mnm1 + offset].value(chi) * hanDeriv1;
        // Danielson 2.7.3-(6b)
        final double han1 = FastMath.pow(chi, twosm2) / twotsm1;
        ret += mpvecDeriv[mnm1 + offset].value(chi) * han1;
        return ret;
    }
}
