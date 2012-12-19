package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.TreeMap;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;

public class CoefficientFactoryTest {

    private static final double eps0  = 0d;
    private static final double eps11 = 1e-11;
    private static final double eps12 = 1e-12;

    @Test
    public void VnsCoefficientComputationTest() {
        final int order = 100;
        TreeMap<NSKey, Double> Vns = CoefficientsFactory.computeVnsCoefficient(order);

        // Odd terms are null
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < i + 1; j++) {
                if ((i - j) % 2 != 0) {
                    Assert.assertEquals(0d, Vns.get(new NSKey(i, j)), eps0);
                }
            }
        }

        // Check the first coefficients :
        Assert.assertEquals(1, Vns.get(new NSKey(0, 0)), eps0);
        Assert.assertEquals(0.5, Vns.get(new NSKey(1, 1)), eps0);
        Assert.assertEquals(-0.5, Vns.get(new NSKey(2, 0)), eps0);
        Assert.assertEquals(1 / 8d, Vns.get(new NSKey(2, 2)), eps0);
        Assert.assertEquals(-1 / 8d, Vns.get(new NSKey(3, 1)), eps0);
        Assert.assertEquals(1 / 48d, Vns.get(new NSKey(3, 3)), eps0);
        Assert.assertEquals(3 / 8d, Vns.get(new NSKey(4, 0)), eps0);
        Assert.assertEquals(-1 / 48d, Vns.get(new NSKey(4, 2)), eps0);
        Assert.assertEquals(1 / 384d, Vns.get(new NSKey(4, 4)), eps0);
        Assert.assertEquals(1 / 16d, Vns.get(new NSKey(5, 1)), eps0);
        Assert.assertEquals(-1 / 384d, Vns.get(new NSKey(5, 3)), eps0);
        Assert.assertEquals(1 / 3840d, Vns.get(new NSKey(5, 5)), eps0);
        Assert.assertEquals(Vns.lastKey().getN(), order - 1);
        Assert.assertEquals(Vns.lastKey().getS(), order - 1);
    }

    /**
     * Test the direct computation method : the getVmns is using the Vns computation to compute the
     * current element
     */
    @Test
    public void VmnsTestFromTwoMethods() throws OrekitException {
        Assert.assertEquals(getVmns2(0, 0, 0), CoefficientsFactory.getVmns(0, 0, 0), eps0);
        Assert.assertEquals(getVmns2(0, 1, 1), CoefficientsFactory.getVmns(0, 1, 1), eps0);
        Assert.assertEquals(getVmns2(0, 2, 2), CoefficientsFactory.getVmns(0, 2, 2), eps0);
        Assert.assertEquals(getVmns2(0, 3, 1), CoefficientsFactory.getVmns(0, 3, 1), eps0);
        Assert.assertEquals(getVmns2(0, 3, 3), CoefficientsFactory.getVmns(0, 3, 3), eps0);
        Assert.assertEquals(getVmns2(2, 2, 2), CoefficientsFactory.getVmns(2, 2, 2), eps0);

    }

    /** Error if m > n */
    @Test(expected = OrekitException.class)
    public void VmnsErrorCallingSequenceWrong_M_S_Test() throws OrekitException {
        // if m > n
        CoefficientsFactory.getVmns(3, 2, 1);
    }

    /**
     * Qns test based on two computation method. As methods are independent, if they give the same
     * results, we assume them to be consistent.
     */
    @Test
    public void testQNS() {
        Assert.assertEquals(1., CoefficientsFactory.getQnsPolynomialValue(0, 0, 0), 0.);
        // Method comparison :
        final int order = 10;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int g = 0; g < 1000; g++) {
            final double gamma = random.nextDouble();
            double[][] qns = CoefficientsFactory.computeQnsCoefficient(gamma, order);
            for (int n = 0; n < order; n++) {
                for (int s = 0; s <= n; s++) {
                    Assert.assertEquals(qns[n][s], CoefficientsFactory.getQnsPolynomialValue(gamma, n, s), Math.abs(eps11 * qns[n][s]));
                }
            }
        }
    }

    /** Gs and Hs computation test based on 2 independent methods.
     *  If they give same results, we assume them to be consistent.
     */
    @Test
    public void GsHsComputationTest() {
        final int s = 50;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int i = 0; i < 10; i++) {
            final double k = random.nextDouble();
            final double h = random.nextDouble();
            final double a = random.nextDouble();
            final double b = random.nextDouble();
            final double[][] GH = CoefficientsFactory.computeGsHs(k, h, a, b, s);
            for (int j = 1; j < s; j++) {
                final double[] GsHs = getGsHs(k, h, a, b, j);
                Assert.assertEquals(GsHs[0], GH[0][j], Math.abs(eps12 * GsHs[0]));
                Assert.assertEquals(GsHs[1], GH[1][j], Math.abs(eps12 * GsHs[1]));
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

    /** Compute directly G<sub>s</sub> and H<sub>s</sub> coefficients from equation 3.1-(4).
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param a 1st direction cosine
     * @param b 2nd direction cosine
     * @param s development order
     * @return Array of G<sub>s</sub> and H<sub>s</sub> values for s.<br>
     *         The 1st element contains the G<sub>s</sub> value.
     *         The 2nd element contains the H<sub>s</sub> value.
     */
    private static double[] getGsHs(final double k, final double h,
                                    final double a, final double b, final int s) {
        final Complex as   = new Complex(k, h).pow(s);
        final Complex bs   = new Complex(a, -b).pow(s);
        final Complex asbs = as.multiply(bs);
        return new double[] {asbs.getReal(), asbs.getImaginary()};
    }
}
