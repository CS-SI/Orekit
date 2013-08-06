/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.data;

import java.io.Serializable;

/**
 * Polynomial nutation function.
 *
 * @author Luc Maisonobe
 * @see PoissonSeries
 */
public class PolynomialNutation implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130728L;

    /** Coefficients of the polynomial part. */
    private double[] coefficients;

    /** Build a polynomial from its coefficients.
     * @param coefficients polynomial coefficients in increasing degree
     */
    public PolynomialNutation(final double ... coefficients) {
        this.coefficients = coefficients.clone();
    }

    /** Evaluate the value of the polynomial.
     * @param elements bodies elements for nutation
     * @return value of the polynomial
     */
    public double value(final BodiesElements elements) {

        final double tc = elements.getTC();

        double p = 0;
        for (int i = coefficients.length - 1; i >= 0; --i) {
            p = p * tc + coefficients[i];
        }

        return p;

    }

}
