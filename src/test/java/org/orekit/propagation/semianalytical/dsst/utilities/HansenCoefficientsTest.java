package org.orekit.propagation.semianalytical.dsst.utilities;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;

/**
 * Test on Hansen coefficient
 */
public class HansenCoefficientsTest {

    final static double epsilonM15 = 1e-15;

//    @Test
//    @Ignore
//    public void test() throws OrekitException{
//        double ecc = 1e-6;
//        double delta = 0d;
//        
//        for (int i = 0; i < 10; i++){
//            ecc += delta;
//            HansenCoefficients hansen = new HansenCoefficients(ecc);
//            System.out.println("ECC " + ecc);
//            for (int m = 0; m < 10; m++){
//                for (int s = 0; s < 10; s++){
//                    for (int n = 0; n < 10; n++){
//                        System.out.println(hansen.getHansenKernelValue(m, n, s));
//                    }
//                }
//            }
//            System.out.println();
//            
//            
//            
//            delta = 1e-6;
//        }
//    }

    /**
     * Result are from the Daniel J. Fonte thesis, page 121,
     * for a 50x50 gravity field model in an orbit determination system.
     * 
     * @throws OrekitException
     */
    @Test
    public void testJ1NHigh() throws OrekitException {
        final double ecc = 0.1;
        HansenCoefficients hansen = new HansenCoefficients(ecc, 4);
        Assert.assertEquals(18.505879, hansen.getValue(1, -31, -20), 0.4);
        Assert.assertEquals(56.7, hansen.getValue(1, -32, -20), 0.8);

    }
}
