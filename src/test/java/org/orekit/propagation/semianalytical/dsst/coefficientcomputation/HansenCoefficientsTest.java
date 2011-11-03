package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import org.apache.commons.math.util.ArithmeticUtils;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenCoefficients;

/**
 * Test on hansen coefficient
 */
public class HansenCoefficientsTest {

    final static double epsilonM3  = 1e-3;

    final static double epsilonM4  = 1e-4;

    final static double epsilonM15 = 1e-15;

    /** Test the series values for K<sub>0</sub><sup>-n-1,s</sup> */
    @Test
    public void testJ0NNegative() throws OrekitException {
        final double ecc = 0.1;
        final double chi = Math.pow(1 - ecc * ecc, -0.5);
        final double chi2 = chi * chi;
        final double chi3 = chi * chi2;

        HansenCoefficients hansen = new HansenCoefficients(ecc, 1e-15);

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
        HansenCoefficients hansen = new HansenCoefficients(ecc, epsilonM4);
        // System.out.println(hansen.getHansenKernelValue(0, 1, 0));
        System.out.println(hansen.getHansenKernelValue(0, 2, 0));
        System.out.println(hansen.getHansenKernelValue(0, 3, 0));
        System.out.println(hansen.getHansenKernelValue(0, 4, 0));

        System.out.println(hansen.getHansenKernelValue(0, 2, 2));

    }

    @Test
    public void testJ1NHigh() throws OrekitException {
        final double ecc = 0.1;
        HansenCoefficients hansen = new HansenCoefficients(ecc, epsilonM4);
        System.out.println(hansen.getHansenKernelValue(1, -31, -20));

    }

    @Test
    public void jEqual0_ValidationFromAnalyticFormula() throws OrekitException {
        final double ecc = 0.1;
        // Analytic formula from "The computation of tables of Hansen coefficients" from
        // s. Hughes, published in Celestial Mechanics 29 (1981) 101-107
        HansenCoefficients hansen = new HansenCoefficients(ecc, epsilonM3);

        System.out.println(analyticSeries(ecc, -4, -2));
        System.out.println(hansen.getHansenKernelValue(0, -4, -2));

    }

    public static double analyticSeries(final double ecc,
                                        final int n,
                                        final int s) {
        final double eO2 = Math.pow(ecc / 2, s);
        final double factor = eO2 * Math.pow(1 - ecc * ecc, -(2 * n - 1) / 2);
        int jMax = Math.abs(Math.round((n - 1 - s) / 2));
        double prod = 0d;
        double cnk1, cnk2;
        for (int j = 0; j < jMax; j++) {
            if (Math.abs(n) - 1 > 2 * j + s) {
                cnk1 = ArithmeticUtils.binomialCoefficient(Math.abs(n) - 1, 2 * j + s);
            } else {
                cnk1 = ArithmeticUtils.binomialCoefficient(2 * j + s, Math.abs(n) - 1);
            }

            if (2 * j + s > j) {
                cnk2 = ArithmeticUtils.binomialCoefficient(2 * j + s, j);
            } else {
                cnk2 = ArithmeticUtils.binomialCoefficient(j, 2 * j + s);
            }
            prod += Math.pow(2, -j) * cnk1 * cnk2 * Math.pow(ecc, 2 * j);
        }
        return factor * prod;

    }
}
