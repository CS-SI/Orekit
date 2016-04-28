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
 * A quadratic matrix of
 * {@link org.hipparchus.analysis.polynomials.PolynomialFunction}.
 *
 * @author Petre Bazavan
 * @author Lucian Barbulescu
 */
public class PolynomialFunctionMatrix {

    /** The order of the matrix. */
    private int order;
    /** The elements of the matrix. */
    private PolynomialFunction elements[][];

    /**
     * Create a matrix.
     *
     * <p>
     * All elements are null
     *
     * @param order
     *            the order of the matrix
     */
    PolynomialFunctionMatrix(final int order) {
        this.order = order;
        this.elements = new PolynomialFunction[order][order];
    }

    /**
     * Set an element of the matrix.
     *
     * @param line
     *            the line
     * @param column
     *            the column
     * @param value
     *            the value
     */
    public void setElem(final int line, final int column, final PolynomialFunction value) {
        elements[line][column] = value;
    }

    /**
     * Get the value of an element.
     *
     * @param line
     *            the line
     * @param column
     *            the column
     * @return the value
     */
    public PolynomialFunction getElem(final int line, final int column) {
        return elements[line][column];
    }

    /**
     * Multiply the argument matrix with the current matrix.
     *
     * @param matrix
     *            the argument matrix
     * @return the result of the multiplication
     */
    public PolynomialFunctionMatrix multiply(final PolynomialFunctionMatrix matrix) {
        final PolynomialFunctionMatrix result = new PolynomialFunctionMatrix(order);
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < order; j++) {
                PolynomialFunction cc = HansenUtilities.ZERO;
                for (int k = 0; k < order; k++) {
                    cc = cc.add(matrix.getElem(i, k).multiply(elements[k][j]));
                }
                result.setElem(i, j, cc);
            }
        }
        return result;
    }

    /**
     * Set values for all elements.
     *
     * @param polynomials
     *            the values that will be used for the matrix
     */
    public void setMatrix(final PolynomialFunction[][] polynomials) {
        elements = polynomials.clone();
    }

    /**
     * Set the value of a line of the matrix.
     *
     * @param line
     *            the line number
     * @param polynomials
     *            the values to set
     */
    public void setMatrixLine(final int line, final PolynomialFunction[] polynomials) {
        elements[line] = polynomials;
    }

    /**
     * Get a line of the matrix.
     *
     * @param line
     *            the line number
     * @return the line of the matrix as a vector
     */
    public PolynomialFunction[] getMatrixLine(final int line) {
        return elements[line].clone();
    }

    /**
     * Add the argument matrix with the current matrix.
     *
     * @param matrix
     *            the argument matrix
     * @return the result of the addition
     */
    public PolynomialFunctionMatrix add(final PolynomialFunctionMatrix matrix) {
        final PolynomialFunctionMatrix c = new PolynomialFunctionMatrix(order);
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < order; j++) {
                c.setElem(i, j, elements[i][j].add(matrix.getElem(i, j)));
            }
        }
        return c;
    }
}
