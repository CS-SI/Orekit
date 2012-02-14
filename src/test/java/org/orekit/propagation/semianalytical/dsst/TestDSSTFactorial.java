package org.orekit.propagation.semianalytical.dsst;

import junit.framework.Assert;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.junit.Test;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTFactorial;

/**
 * Test the factorial method created for DSST purpose
 * 
 * @author rdicosta
 */
public class TestDSSTFactorial {

    @Test
    public void testFactorial() {
        // Test the 20 first elements with CAM comparison
        for (int i = 0; i <= 20; i++) {
            Assert.assertEquals(ArithmeticUtils.factorial(i), DSSTFactorial.fact(i).longValue());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException() {
        DSSTFactorial.fact(-1);
    }

}
