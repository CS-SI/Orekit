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
package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.math.BigInteger;
import java.util.ArrayList;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.errors.OrekitException;

/**
 * This class has been created for DSST purpose. The DSST needs large factorial values (for high
 * central body potential) and the {@link org.apache.commons.math3.util.ArithmeticUtils#factorial(int)}
 * method only admit integer inferior to 20, due to the {@link Long} java upper value. This method is
 * based on {@link BigInteger} numbers which can have infinite precision, and so factorial can be computed
 * with large integer input. <br>
 * Data computed are stored in a static map, filled when needed. The 12th first terms are
 * pre-computed.
 *
 * @author rdicosta
 */
public class DSSTFactorial {

    /** Cache. */
    private static ArrayList<BigInteger> TABLE = new ArrayList<BigInteger>();

    static {
        // Initialize the first elements
        TABLE.add(BigInteger.valueOf(1)); // 0!
        TABLE.add(BigInteger.valueOf(1)); // 1!
        TABLE.add(BigInteger.valueOf(2)); // 2!
        TABLE.add(BigInteger.valueOf(6)); // 3!
        TABLE.add(BigInteger.valueOf(24)); // 4!
        TABLE.add(BigInteger.valueOf(120)); // 5!
        TABLE.add(BigInteger.valueOf(720)); // 6!
        TABLE.add(BigInteger.valueOf(5040)); // 7!
        TABLE.add(BigInteger.valueOf(40320)); // 8!
        TABLE.add(BigInteger.valueOf(362880)); // 9!
        TABLE.add(BigInteger.valueOf(3628800)); // 10!
        TABLE.add(BigInteger.valueOf(39916800)); // 11!
        TABLE.add(BigInteger.valueOf(479001600)); // 12!
    }

    /** Private constructor, as class is a utility.
     */
    private DSSTFactorial() {
    }

    /** Factorial method, using {@link BigInteger} cached in the ArrayList.
     * @param n integer for which we want factorial
     * @return n!
     */
    public static synchronized BigInteger fact(final int n) {
        if (n < 0) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.FACTORIAL_NEGATIVE_PARAMETER,
                                                                 n);
        }
        for (int size = TABLE.size(); size <= n; size++) {
            final BigInteger lastfact = (BigInteger) TABLE.get(size - 1);
            final BigInteger nextfact = lastfact.multiply(BigInteger.valueOf(size));
            TABLE.add(nextfact);
        }
        return TABLE.get(n);
    }

}
