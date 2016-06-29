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
    public static final PolynomialFunctionMatrix buildIdentityMatrix2() {
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
    public static final PolynomialFunctionMatrix buildZeroMatrix2() {
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
    public static final PolynomialFunctionMatrix buildIdentityMatrix4() {
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
    public static final PolynomialFunctionMatrix buildZeroMatrix4() {
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

}
