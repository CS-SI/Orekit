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
 * method only admit integer less than 20, due to the {@link Long} java upper value. This method is
 * based on {@link BigInteger} numbers which can have infinite precision, and so factorial can be computed
 * with large integer input. <br>
 * Data computed are stored in a static map, filled when needed. The 20th first terms are
 * pre-computed.
 *
 * @author rdicosta
 */
public class DSSTFactorial {

    /** Cache. */
    private static ArrayList<BigInteger> TABLE = new ArrayList<BigInteger>(30);

    static {
        // Initialize the first elements
        TABLE.add(BigInteger.valueOf(1L)); // 0!
        TABLE.add(BigInteger.valueOf(1L)); // 1!
        TABLE.add(BigInteger.valueOf(2L)); // 2!
        TABLE.add(BigInteger.valueOf(6L)); // 3!
        TABLE.add(BigInteger.valueOf(24L)); // 4!
        TABLE.add(BigInteger.valueOf(120L)); // 5!
        TABLE.add(BigInteger.valueOf(720L)); // 6!
        TABLE.add(BigInteger.valueOf(5040L)); // 7!
        TABLE.add(BigInteger.valueOf(40320L)); // 8!
        TABLE.add(BigInteger.valueOf(362880L)); // 9!
        TABLE.add(BigInteger.valueOf(3628800L)); // 10!
        TABLE.add(BigInteger.valueOf(39916800L)); // 11!
        TABLE.add(BigInteger.valueOf(479001600L)); // 12!
        TABLE.add(BigInteger.valueOf(6227020800L)); // 13!
        TABLE.add(BigInteger.valueOf(87178291200L)); // 14!
        TABLE.add(BigInteger.valueOf(1307674368000L)); // 15!
        TABLE.add(BigInteger.valueOf(20922789888000L)); // 16!
        TABLE.add(BigInteger.valueOf(355687428096000L)); // 17!
        TABLE.add(BigInteger.valueOf(6402373705728000L)); // 18!
        TABLE.add(BigInteger.valueOf(121645100408832000L)); // 19!
        TABLE.add(BigInteger.valueOf(2432902008176640000L)); // 20!
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
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.FACTORIAL_NEGATIVE_PARAMETER, n);
        }
        for (int size = TABLE.size(); size <= n; size++) {
            final BigInteger lastfact = TABLE.get(size - 1);
            final BigInteger nextfact = lastfact.multiply(BigInteger.valueOf(size));
            TABLE.add(nextfact);
        }
        return TABLE.get(n);
    }

}
