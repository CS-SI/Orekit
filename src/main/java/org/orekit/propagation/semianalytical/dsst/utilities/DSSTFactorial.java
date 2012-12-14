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
package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.ArrayList;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.errors.OrekitException;

/**
 * This class has been created for DSST purpose. The DSST needs large factorial values (for high
 * central body potentiad and the {@link org.apache.commons.math3.util.ArithmeticUtils#factorial(int)}
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
    private static ArrayList<Double> TABLE = new ArrayList<Double>(30);

    static {
        // Initialize the first elements
        TABLE.add(1d); // 0!
        TABLE.add(1d); // 1!
        TABLE.add(2d); // 2!
        TABLE.add(6d); // 3!
        TABLE.add(24d); // 4!
        TABLE.add(120d); // 5!
        TABLE.add(720d); // 6!
        TABLE.add(5040d); // 7!
        TABLE.add(40320d); // 8!
        TABLE.add(362880d); // 9!
        TABLE.add(3628800d); // 10!
        TABLE.add(39916800d); // 11!
        TABLE.add(479001600d); // 12!
        TABLE.add(6227020800d); // 13!
        TABLE.add(87178291200d); // 14!
        TABLE.add(1307674368000d); // 15!
        TABLE.add(20922789888000d); // 16!
        TABLE.add(355687428096000d); // 17!
        TABLE.add(6402373705728000d); // 18!
        TABLE.add(121645100408832000d); // 19!
        TABLE.add(2432902008176640000d); // 20!
    }

    /** Private constructor, as class is a utility.
     */
    private DSSTFactorial() {
    }

    /** Factorial method, using {@link Double} cached in the ArrayList.
     *  @param n integer for which we want factorial
     *  @return n!
     */
    public static synchronized double fact(final int n) {
        if (n < 0) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.FACTORIAL_NEGATIVE_PARAMETER, n);
        }
        for (int size = TABLE.size(); size <= n; size++) {
            TABLE.add(TABLE.get(size - 1) * size);
        }
        return TABLE.get(n);
    }

}
