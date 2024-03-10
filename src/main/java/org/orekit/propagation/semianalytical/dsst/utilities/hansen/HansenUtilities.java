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

import org.hipparchus.analysis.polynomials.PolynomialFunction;

/**
 * Utilities class.
 *
 * @author Lucian Barbulescu
 */
public class HansenUtilities {

    /** 1 represented as a polynomial. */
    public static final PolynomialFunction ONE = new PolynomialFunction(new double[] {
        1
    });

    /** 0 represented as a polynomial. */
    public static final PolynomialFunction ZERO = new PolynomialFunction(new double[] {
        0
    });

    /** Private constructor as class is a utility.
     */
    private HansenUtilities() {
    }

    /**
     * Build the identity matrix of order 2.
     *
     * <pre>
     *       / 1   0 \
     *  I₂ = |       |
     *       \ 0   1 /
     * </pre>
     *
     * @return the identity matrix of order 2
     */
    public static PolynomialFunctionMatrix buildIdentityMatrix2() {
        final PolynomialFunctionMatrix matrix = new PolynomialFunctionMatrix(2);
        matrix.setMatrix(new PolynomialFunction[][] {
            {
                ONE,  ZERO
            },
            {
                ZERO, ONE
            }
        });
        return matrix;
    }

    /**
     * Build the empty matrix of order 2.
     *
     * <pre>
     *       / 0   0 \
     *  E₂ = |       |
     *       \ 0   0 /
     * </pre>
     *
     * @return the identity matrix of order 2
     */
    public static PolynomialFunctionMatrix buildZeroMatrix2() {
        final PolynomialFunctionMatrix matrix = new PolynomialFunctionMatrix(2);
        matrix.setMatrix(new PolynomialFunction[][] {
            {
                ZERO, ZERO
            },
            {
                ZERO, ZERO
            }
        });
        return matrix;
    }


    /**
     * Build the identity matrix of order 4.
     *
     * <pre>
     *       / 1  0  0  0 \
     *       |            |
     *       | 0  1  0  0 |
     *  I₄ = |            |
     *       | 0  0  1  0 |
     *       |            |
     *       \ 0  0  0  1 /
     * </pre>
     *
     * @return the identity matrix of order 4
     */
    public static PolynomialFunctionMatrix buildIdentityMatrix4() {
        final PolynomialFunctionMatrix matrix = new PolynomialFunctionMatrix(4);
        matrix.setMatrix(new PolynomialFunction[][] {
            {
                ONE,  ZERO, ZERO, ZERO
            },
            {
                ZERO, ONE,  ZERO, ZERO
            },
            {
                ZERO, ZERO, ONE,  ZERO
            },
            {
                ZERO, ZERO, ZERO, ONE
            }
        });
        return matrix;
    }

    /**
     * Build the empty matrix of order 4.
     *
     * <pre>
     *       / 0  0  0  0 \
     *       |            |
     *       | 0  0  0  0 |
     *  E₄ = |            |
     *       | 0  0  0  0 |
     *       |            |
     *       \ 0  0  0  0 /
     * </pre>
     *
     * @return the identity matrix of order 4
     */
    public static PolynomialFunctionMatrix buildZeroMatrix4() {
        final PolynomialFunctionMatrix matrix = new PolynomialFunctionMatrix(4);
        matrix.setMatrix(new PolynomialFunction[][] {
            {
                ZERO, ZERO, ZERO, ZERO
            },
            {
                ZERO, ZERO, ZERO, ZERO
            },
            {
                ZERO, ZERO, ZERO, ZERO
            },
            {
                ZERO, ZERO, ZERO, ZERO
            }
        } );
        return matrix;
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
     * @param s the s coefficient
     * @param mnm1 -n-1 value
     * @return the polynomial
     */
    private static PolynomialFunction aZonal(final int s, final int mnm1) {
        // from recurrence Collins 4-242
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
     * @param s the s coefficient
     * @param mnm1 -n-1 value
     * @return the polynomial
     */
    private static PolynomialFunction bZonal(final int s, final int mnm1) {
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
     * @param n0         the index of the initial condition, Petre's paper
     * @param nMin       rhe minimum value for the order
     * @param offset     offset used to identify the polynomial that corresponds
     *                   to a negative value of n in the internal array that
     *                   starts at 0
     * @param slice      number of coefficients that will be computed with a set
     *                   of roots
     * @param s          the s coefficient
     * @param mpvec      array to store the first vector of polynomials
     *                   associated to Hansen coefficients and derivatives.
     * @param mpvecDeriv array to store the second vector of polynomials
     *                   associated only to derivatives.
     * <p>
     * See Petre's paper
     * </p>
     */
    public static void generateZonalPolynomials(final int n0, final int nMin,
                                                final int offset, final int slice,
                                                final int s,
                                                final PolynomialFunction[][] mpvec,
                                                final PolynomialFunction[][] mpvecDeriv) {

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

        for (int i = n0 - 1; i > nMin - 1; i--) {
            index = i + offset;
            // Matrix of the current linear transformation
            // Petre's paper
            a.setMatrixLine(1, new PolynomialFunction[] {
                bZonal(s, i), aZonal(s, i)
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

            if (++sliceCounter % slice == 0) {
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
     * @param s the s coefficient
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private static PolynomialFunction aTesseral(final int s, final int mnm1) {
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
     * @param j the j coefficient
     * @param s the s coefficient
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private static PolynomialFunction bTesseral(final int j, final int s, final int mnm1) {
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
     * @param j the j coefficient
     * @param s the s coefficient
     * @param mnm1 -n-1
     * @return the polynomial
     */
    private static PolynomialFunction cTesseral(final int j, final int s, final int mnm1) {
        // Collins 4-236, Danielson 2.7.3-(9)
        final double r1 = j * j * (mnm1 + 2.);
        final double r2 = (mnm1 + 4.) * (2. + mnm1 + s) * (2. + mnm1 - s);

        return new PolynomialFunction(new double[] {
            0.0, 0.0, r1 / r2
        });
    }

    /**
     * Compute polynomial coefficient d.
     * <p>
     * It is used to generate the coefficient for K<sub>j</sub><sup>-n-1,
     * s</sup> / dχ when computing dK<sub>j</sub><sup>-n-1, s</sup> / de²
     * </p>
     * <p>
     * See Danielson 2.7.3-(9) and Collins 4-236 and 4-240
     * </p>
     *
     * @return the polynomial
     */
    private static PolynomialFunction dTesseral() {
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
     * @param j the j coefficient
     * @param s the s coefficient
     * @param n index
     * @return the polynomial
     */
    private static PolynomialFunction fTesseral(final int j, final int s, final int n) {
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
     * @param n0         the index of the initial condition, Petre's paper
     * @param nMin       rhe minimum value for the order
     * @param offset     offset used to identify the polynomial that corresponds
     *                   to a negative value of n in the internal array that
     *                   starts at 0
     * @param slice      number of coefficients that will be computed with a set
     *                   of roots
     * @param j          the j coefficient
     * @param s          the s coefficient
     * @param mpvec      array to store the first vector of polynomials
     *                   associated to Hansen coefficients and derivatives.
     * @param mpvecDeriv array to store the second vector of polynomials
     *                   associated only to derivatives.
     */
    public static void generateTesseralPolynomials(final int n0, final int nMin,
                                                   final int offset, final int slice,
                                                   final int j, final int s,
                                                   final PolynomialFunction[][] mpvec,
                                                   final PolynomialFunction[][] mpvecDeriv) {


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
        for (int i = n0 - 1; i > nMin - 1; i--) {
            index = i + offset;
            // The matrix of the current linear transformation is updated
            // Petre's paper
            a.setMatrixLine(3, new PolynomialFunction[] {
                cTesseral(j, s, i), HansenUtilities.ZERO, bTesseral(j, s, i), aTesseral(s, i)
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
                HansenUtilities.ZERO, fTesseral(j, s, i),
                HansenUtilities.ZERO, dTesseral()
            });
            D = D.add(A.multiply(B));

            // store the polynomials for Hansen coefficients from the
            // expressions of derivatives
            mpvecDeriv[index] = D.getMatrixLine(3);

            if (++sliceCounter % slice == 0) {
                // Re-Initialisation of matrix for linear transformmations
                // The final configuration of these matrix are obtained by composition
                // of linear transformations
                A = HansenUtilities.buildIdentityMatrix4();
                D = HansenUtilities.buildZeroMatrix4();
            }
        }
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
    private static PolynomialFunction aThirdBody(final int n) {
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
     * @param s          the s coefficient
     * @param n n value
     * @return the polynomial
     */
    private static PolynomialFunction bThirdBody(final int s, final int n) {
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
     * @param s the s coefficient
     * @param n n value
     * @return the polynomial
     */
    private static PolynomialFunction dThirdBody(final int s, final int n) {
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
     * @param n0 the index of the initial condition, Petre's paper
     * @param nMax the maximum order of n indexes
     * @param slice number of coefficients that will be computed with a set of roots
     * @param s the s coefficient
     * @param mpvec      array to store the first vector of polynomials
     *                   associated to Hansen coefficients and derivatives.
     * @param mpvecDeriv array to store the second vector of polynomials
     *                   associated only to derivatives.
     * <p>
     * See Petre's paper
     * </p>
     */
    public static void generateThirdBodyPolynomials(final int n0, final int nMax,
                                                    final int slice,
                                                    final int s,
                                                    final PolynomialFunction[][] mpvec,
                                                    final PolynomialFunction[][] mpvecDeriv) {

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
        for (int i = n0 + 2; i <= nMax; i++) {
            // Collins 4-254 or Danielson 2.7.3-(7)
            // Petre's paper
            // The matrix of the current linear transformation is actualized
            a.setMatrixLine(1, new PolynomialFunction[] {
                bThirdBody(s, i), aThirdBody(i)
            });

            // composition of the linear transformations to calculate
            // the polynomials associated to Hansen coefficients
            A = A.multiply(a);
            // store the polynomials associated to Hansen coefficients
            mpvec[i] = A.getMatrixLine(1);
            // composition of the linear transformations to calculate
            // the polynomials associated to derivatives
            // Danielson 3.2-(3b) and Petre's paper
            D = D.multiply(a);
            if (sliceCounter % slice != 0) {
                a.setMatrixLine(1, new PolynomialFunction[] {
                    bThirdBody(s, i - 1), aThirdBody(i - 1)
                });
                E = E.multiply(a);
            }

            B.setElem(1, 0, dThirdBody(s, i));
            // F = E.prod(B);
            D = D.add(E.multiply(B));
            // store the polynomials associated to the derivatives
            mpvecDeriv[i] = D.getMatrixLine(1);

            if (++sliceCounter % slice == 0) {
                // Re-Initialization of the matrices for linear transformations
                // The final configuration of these matrices are obtained by composition
                // of linear transformations
                A = HansenUtilities.buildIdentityMatrix2();
                D = HansenUtilities.buildZeroMatrix2();
                E = HansenUtilities.buildIdentityMatrix2();
            }
        }
    }

}
