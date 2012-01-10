package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.math.BigInteger;
import java.util.ArrayList;

import org.apache.commons.math.util.ArithmeticUtils;

/**
 * This class has been created for DSST purpose. The DSST needs large factorial values (for high
 * central body potential) and the {@link ArithmeticUtils#factorial(int)} method only admit integer
 * inferior to 20, due to the {@link Long} java upper value. This method is based on
 * {@link BigInteger} numbers which can have infinite precision, and so factorial can be computed
 * with large integer input. <br>
 * Data computed are stored in a static map, filled when needed. The 12th first terms are
 * pre-computed.
 * 
 * @author rdicosta
 */
public class DSSTFactorial {

    // Create cache
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

    /** Factorial method, using {@link BigInteger} cached in the ArrayList */
    public static synchronized BigInteger fact(int x) {
        if (x < 0)
            throw new IllegalArgumentException("x must be non-negative.");
        for (int size = TABLE.size(); size <= x; size++) {
            BigInteger lastfact = (BigInteger) TABLE.get(size - 1);
            BigInteger nextfact = lastfact.multiply(BigInteger.valueOf(size));
            TABLE.add(nextfact);
        }
        return (BigInteger) TABLE.get(x);
    }

}
