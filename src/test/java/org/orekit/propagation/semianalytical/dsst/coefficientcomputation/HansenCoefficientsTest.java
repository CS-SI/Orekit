package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.coefficients.HansenCoefficients;

/**
 * Test on Hansen coefficient
 */
public class HansenCoefficientsTest {


    final static double epsilonM15 = 1e-15;

    /** Test the series values for K<sub>0</sub><sup>-n-1,s</sup> */
    @Test
    public void testJ0NNegative() throws OrekitException {
        final double ecc = 0.1;
        final double chi = Math.pow(1 - ecc * ecc, -0.5);
        final double chi2 = chi * chi;
        final double chi3 = chi * chi2;

        HansenCoefficients hansen = new HansenCoefficients(ecc, 4);

        Assert.assertEquals(0, hansen.getHansenKernelValue(0, -1, 0), epsilonM15);
        Assert.assertEquals(0, hansen.getHansenKernelValue(0, -1, 1), epsilonM15);
        Assert.assertEquals(chi, hansen.getHansenKernelValue(0, -2, 0), epsilonM15);
        Assert.assertEquals(0, hansen.getHansenKernelValue(0, -2, 1), epsilonM15);
        Assert.assertEquals(chi3, hansen.getHansenKernelValue(0, -3, 0), epsilonM15);
        Assert.assertEquals(chi3 / 2., hansen.getHansenKernelValue(0, -3, 1), epsilonM15);
        Assert.assertEquals(0, hansen.getHansenKernelValue(0, -3, 2), epsilonM15);
        Assert.assertEquals(chi3 / 2. * (3 * chi2 - 1), hansen.getHansenKernelValue(0, -4, 0), epsilonM15);
        Assert.assertEquals(chi3 * chi2, hansen.getHansenKernelValue(0, -4, 1), epsilonM15);
    }


    @Test
    public void testJ0NPositive() throws OrekitException {
        final double ecc = 0.1;
        HansenCoefficients hansen = new HansenCoefficients(ecc, 4);

        Assert.assertEquals(1., hansen.getHansenKernelValue(0, 0, 0), epsilonM15);
        Assert.assertEquals(-1., hansen.getHansenKernelValue(0, 0, 1), epsilonM15);
        Assert.assertEquals(-3. / 2, hansen.getHansenKernelValue(0, 1, 1), epsilonM15);
    }

    /**
     * Result are from the Daniel J. Fonte thesis, implementing a 50x50 gravity field model in an
     * orbit determination system, page 121.
     * 
     * @throws OrekitException
     */
    @Test
    public void testJ1NHigh() throws OrekitException {
        final double ecc = 0.1;
        HansenCoefficients hansen = new HansenCoefficients(ecc, 4);
        Assert.assertEquals(18.505879, hansen.getHansenKernelValue(1, -31, -20), 0.4);
        Assert.assertEquals(56.7, hansen.getHansenKernelValue(1, -32, -20), 0.8);

    }
}
