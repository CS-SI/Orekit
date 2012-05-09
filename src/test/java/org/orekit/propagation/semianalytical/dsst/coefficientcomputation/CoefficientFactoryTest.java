package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import java.util.TreeMap;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.NSKey;

public class CoefficientFactoryTest {

    private static final double eps8  = 1e-8;
    private static final double eps11 = 1e-11;
    private static final double eps12 = 1e-12;

    @Test
    public void VnsCoefficientComputationTest() {
        final int order = 6;
        TreeMap<NSKey, Double> Vns = DSSTCoefficientFactory.computeVnsCoefficient(order);

        // Odd terms are null
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < i + 1; j++) {
                if ((i - j) % 2 != 0) {
                    Assert.assertEquals(0d, Vns.get(new NSKey(i, j)), eps8);
                }
            }
        }

        // Check the first coefficient :
        Assert.assertEquals(1, Vns.get(new NSKey(0, 0)), eps8);
        Assert.assertEquals(0.5, Vns.get(new NSKey(1, 1)), eps8);
        Assert.assertEquals(-0.5, Vns.get(new NSKey(2, 0)), eps8);
        Assert.assertEquals(1 / 8d, Vns.get(new NSKey(2, 2)), eps8);
        Assert.assertEquals(-1 / 8d, Vns.get(new NSKey(3, 1)), eps8);
        Assert.assertEquals(1 / 48d, Vns.get(new NSKey(3, 3)), eps8);
        Assert.assertEquals(3 / 8d, Vns.get(new NSKey(4, 0)), eps8);
        Assert.assertEquals(-1 / 48d, Vns.get(new NSKey(4, 2)), eps8);
        Assert.assertEquals(1 / 384d, Vns.get(new NSKey(4, 4)), eps8);
        Assert.assertEquals(1 / 16d, Vns.get(new NSKey(5, 1)), eps8);
        Assert.assertEquals(-1 / 384d, Vns.get(new NSKey(5, 3)), eps8);
        Assert.assertEquals(1 / 3840d, Vns.get(new NSKey(5, 5)), eps8);
        Assert.assertEquals(Vns.lastKey().getN(), 5);
        Assert.assertEquals(Vns.lastKey().getS(), 5);

        /** Compute Vns coefficient by using the cache process to store those coefficient */
        TreeMap<NSKey, Double> Vns30 = DSSTCoefficientFactory.computeVnsCoefficient(30);
        NSKey lastKey30 = Vns30.lastKey();
        Assert.assertEquals(lastKey30.getN(), 29);
        Assert.assertEquals(lastKey30.getS(), 29);
    }

    /**
     * Test the direct computation method : the getVmns is using the Vns computation to compute the
     * current element
     */
    @Test
    public void VmnsTestFromTwoMethods() throws OrekitException {
        Assert.assertEquals(getVmns2(0, 0, 0), DSSTCoefficientFactory.getVmns(0, 0, 0), eps8);
        Assert.assertEquals(getVmns2(0, 1, 1), DSSTCoefficientFactory.getVmns(0, 1, 1), eps8);
        Assert.assertEquals(getVmns2(0, 2, 2), DSSTCoefficientFactory.getVmns(0, 2, 2), eps8);
        Assert.assertEquals(getVmns2(0, 3, 1), DSSTCoefficientFactory.getVmns(0, 3, 1), eps8);
        Assert.assertEquals(getVmns2(0, 3, 3), DSSTCoefficientFactory.getVmns(0, 3, 3), eps8);
        Assert.assertEquals(getVmns2(2, 2, 2), DSSTCoefficientFactory.getVmns(2, 2, 2), eps8);

    }

    /** Error if m > n */
    @Test(expected = OrekitException.class)
    public void VmnsErrorCallingSequenceWrong_M_S_Test() throws OrekitException {
        // if m > n
        DSSTCoefficientFactory.getVmns(3, 2, 1);
    }

    /**
     * Qns test based on two computation method. As methods are independent, if they give the same
     * results, we assume them to be consistent.
     */
    @Test
    public void testQNS() {
        Assert.assertEquals(1., DSSTCoefficientFactory.getQnsPolynomialValue(0, 0, 0), 0.);
        // Method comparison :
        final int order = 10;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int g = 0; g < 1000; g++) {
            final double gamma = random.nextDouble();
            double[][] qns = DSSTCoefficientFactory.computeQnsCoefficient(gamma, order);
            for (int n = 0; n < order; n++) {
                for (int s = 0; s <= n; s++) {
                    Assert.assertEquals(qns[n][s], DSSTCoefficientFactory.getQnsPolynomialValue(gamma, n, s), Math.abs(eps11 * qns[n][s]));
                }
            }
        }
    }

    /**
     * Gs and Hs test based on 2 computation methods. As methods are independent, if they give the
     * same results, we assume them to be consistent.
     */
    @Test
    public void GsHsComputationTest() {
        final int order = 50;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double alpha = random.nextDouble();
            final double beta = random.nextDouble();
            final double[][] GH = DSSTCoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, order);
            for (int j = 1; j < order; j++) {
                final double[] GsHs = DSSTCoefficientFactory.getGsHsCoefficient(k, h, alpha, beta, j);
                final double Gs = DSSTCoefficientFactory.getGsCoefficient(k, h, alpha, beta, j);
                final double Hs = DSSTCoefficientFactory.getHsCoefficient(k, h, alpha, beta, j);
                Assert.assertEquals(GsHs[0], Gs, 0.);
                Assert.assertEquals(GsHs[1], Hs, 0.);
                Assert.assertEquals(GsHs[0], GH[0][j], Math.abs(eps12 * Gs));
                Assert.assertEquals(GsHs[1], GH[1][j], Math.abs(eps12 * Hs));
            }
        }
    }

    /**
     * Direct computation for the Vmns coefficient from equation 2.7.1 - (6)
     * 
     * @throws OrekitException
     */
    public static double getVmns2(final int m,
                                  final int n,
                                  final int s) throws OrekitException {
        double vmsn = 0d;
        if ((n - s) % 2 == 0) {
            final double num = FastMath.pow(-1, (n - s) / 2d) * ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);
            final double den = FastMath.pow(2, n) * ArithmeticUtils.factorial(n - m) * ArithmeticUtils.factorial((n + s) / 2)
                            * ArithmeticUtils.factorial((n - s) / 2);
            vmsn = num / den;
        }
        return vmsn;
    }
}
