/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.utils;

import java.io.Serializable;

/** Container for Love numbers.
 * @author luc Luc Maisonobe
 * @since 6.1
 */
public class LoveNumbers implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131014L;

    /** Real part of the nominal Love numbers. */
    private final double[][] real;

    /** Imaginary part of the nominal Love numbers. */
    private final double[][] imaginary;

    /** Time-dependent part of the Love numbers. */
    private final double[][] plus;

    /** Simple constructor.
     * @param real real part of the nominal Love numbers
     * @param imaginary imaginary part of the nominal Love numbers
     * @param plus time-dependent part of the Love numbers
     */
    public LoveNumbers(final double[][] real, final double[][] imaginary, final double[][] plus) {
        this.real      = copyIrregular(real);
        this.imaginary = copyIrregular(imaginary);
        this.plus      = copyIrregular(plus);
    }

    /** Copy irregular-shape array.
     * @param source source array
     * @return copied array
     */
    private double[][] copyIrregular(final double[][] source) {
        final double[][] copy = new double[source.length][];
        for (int i = 0; i < source.length; ++i) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    /** Get the size of the arrays.
     * @return size of the arrays (i.e. max degree for Love numbers + 1)
     */
    public int getSize() {
        return real.length;
    }

    /** Get the real part of a nominal Love numbers.
     * @param n degree of the Love number (must be less than {@link #getSize()})
     * @param m order of the Love number (must be less than {@code n})
     * @return real part of k<sub>n,m</sub>
     */
    public final double getReal(final int n, final int m) {
        return real[n][m];
    }

    /** Get the imaginary part of a nominal Love numbers.
     * @param n degree of the Love number (must be less than {@link #getSize()})
     * @param m order of the Love number (must be less than {@code n})
     * @return imaginary part of k<sub>n,m</sub>
     */
    public final double getImaginary(final int n, final int m) {
        return imaginary[n][m];
    }

    /** Get the real part of a nominal Love numbers.
     * @param n degree of the Love number (must be less than {@link #getSize()})
     * @param m order of the Love number (must be less than {@code n})
     * @return k<sub>n,m</sub><sup>+</sup>
     */
    public final double getPlus(final int n, final int m) {
        return plus[n][m];
    }

}

