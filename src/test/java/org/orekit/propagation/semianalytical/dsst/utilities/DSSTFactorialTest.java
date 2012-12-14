package org.orekit.propagation.semianalytical.dsst.utilities;

import junit.framework.Assert;

import org.junit.Test;
import org.orekit.propagation.semianalytical.dsst.utilities.DSSTFactorial;

/**
 * Test the factorial method created for DSST purpose
 * 
 * @author rdicosta
 */
public class DSSTFactorialTest {

    @Test
    public void testFactorial() {
        // Test the 100 first elements
        double fact = 1d;
        for (int i = 1; i <= 100; i++) {
            fact *= i;
            Assert.assertEquals(fact, DSSTFactorial.fact(i));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException() {
        DSSTFactorial.fact(-1);
    }

}
