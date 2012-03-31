/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

/**
 * Resonant couple used in the expression of the Tesseral resonant term for the
 * {@link DSSTCentralBody}.
 *
 * @author Romain Di Costanzo
 */
public class ResonantCouple implements Comparable<ResonantCouple> {

    /** n value. */
    private final int n;

    /** m value. */
    private final int m;

    /** Simple constructor.
     * @param n n-value
     * @param m m-value
     */
    public ResonantCouple(final int n, final int m) {
        this.m = m;
        this.n = n;
    }

    /** Get the m-value.
     * @return m-value
     */
    public int getM() {
        return m;
    }

    /** Get the n-value.
     * @return n-value
     */
    public int getN() {
        return n;
    }

    /**
     * {@inheritDoc}
     * Compare a resonant couple to another one. Comparison is done on the order.
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
}
