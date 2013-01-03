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
package org.orekit.propagation.semianalytical.dsst.utilities;


/** Resonant couple.
 *  <p>
 *  Resonant couples are used in the expression of the tesseral
 *  resonant terms for the central body gravitational perturbation.
 *  </p>
 *
 *  @author Romain Di Costanzo
 */
public class ResonantCouple implements Comparable<ResonantCouple> {

    /** Degree n. */
    private final int n;

    /** Order m. */
    private final int m;

    /** Simple constructor.
     * @param n degree
     * @param m order
     */
    public ResonantCouple(final int n, final int m) {
        this.n = n;
        this.m = m;
    }

    /** Get the order.
     *  @return order m
     */
    public int getM() {
        return m;
    }

    /** Get the degree.
     *  @return degree n
     */
    public int getN() {
        return n;
    }

    /** Compares a resonant couple to another one.
     *  <p>
     *  Comparison is done on the order first.
     *  </p>
     *
     *  @param  couple the object to be compared.
     *  @return a negative integer, zero, or a positive integer as this couple
     *          is less than, equal to, or greater than the specified couple.
     */
    public int compareTo(final ResonantCouple couple) {
        int result = 1;
        if (n == couple.n) {
            if (m < couple.m) {
                result = -1;
            } else if (m == couple.m) {
                result = 0;
            }
        } else if (n < couple.n) {
            result = -1;
        }
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(final Object couple) {

        if (couple == this) {
            // first fast check
            return true;
        }

        if ((couple != null) && (couple instanceof ResonantCouple)) {
            return (n == ((ResonantCouple) couple).n) &&
                   (m == ((ResonantCouple) couple).m);
        }

        return false;

    }

    /** {@inheritDoc} */
    public int hashCode() {
        return 0xd3dc54ce ^ (n << 8) ^ m;
    }

}
