package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory;

public class CoefficientFactoryTest {

    private static final double epsilon = 1e-8;

    @Test
    public void testVnsCoefficientComputation() {
        final int order = 10;
        double[][] Vns = CoefficientFactory.computeVnsCoefficient(order);

        for (int i = 0; i < order; i++) {
            for (int j = 0; j < i + 1; j++) {
                // Odd terms are null
                if ((i - j) % 2 != 0) {
                    Assert.assertEquals(0d, Vns[i][j], epsilon);
                }
            }
        }

        // Check the first coefficient :
        Assert.assertEquals(1, Vns[0][0], epsilon);
        Assert.assertEquals(0.5, Vns[1][1], epsilon);
        Assert.assertEquals(-0.5, Vns[2][0], epsilon);
        Assert.assertEquals(1 / 8d, Vns[2][2], epsilon);
        Assert.assertEquals(-1 / 8d, Vns[3][1], epsilon);
        Assert.assertEquals(1 / 48d, Vns[3][3], epsilon);
        Assert.assertEquals(3 / 8d, Vns[4][0], epsilon);
        Assert.assertEquals(-1 / 48d, Vns[4][2], epsilon);
        Assert.assertEquals(1 / 384d, Vns[4][4], epsilon);
        Assert.assertEquals(1 / 16d, Vns[5][1], epsilon);
        Assert.assertEquals(-1 / 384d, Vns[5][3], epsilon);
        Assert.assertEquals(1 / 3840d, Vns[5][5], epsilon);
    }

    /**
     * Qns test based on two computation method. As method are independent and give the same result,
     * we assume the result to be consistent.
     */
    @Test
    public void QnsGenerationTest() {
        Assert.assertEquals(1, CoefficientFactory.getQnsPolynomialValue(0, 0, 0), 1e-15);
        // Method comparison :
        final int order = 10;
        double gamma;
        for (int g = 0; g < 1000; g++) {
            gamma = Math.random();
            double[][] qns = CoefficientFactory.computeQnsCoefficient(order, gamma);
            for (int n = 0; n < order; n++) {
                for (int s = 0; s <= n; s++) {
                    Assert.assertEquals(qns[n][s], CoefficientFactory.getQnsPolynomialValue(n, s, gamma), epsilon);
                }
            }
        }
    }

    /**
     * Gs and Hs test based on two computation method. As method are independent and give the same
     * result, we assume the result to be consistent.
     */
    @Test
    public void GsHsComputationTest() {
        final int order = 50;
        for (int j = 0; j < order; j++) {
            final double k = Math.random();
            final double h = Math.random();
            final double alpha = Math.random();
            final double beta = Math.random();
            double[][] GsHs = CoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, order);
            Assert.assertEquals(GsHs[0][j], CoefficientFactory.getGsCoefficient(k, h, alpha, beta, j), epsilon);
            Assert.assertEquals(GsHs[1][j], CoefficientFactory.getHsCoefficient(k, h, alpha, beta, j), epsilon);
        }
    }
}
