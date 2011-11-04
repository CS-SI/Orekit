package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import java.util.TreeMap;

import org.apache.commons.math.random.MersenneTwister;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;

public class CoefficientFactoryTest {

    private static final double eps8  = 1e-8;
    private static final double eps11 = 1e-11;
    private static final double eps12 = 1e-12;

    @Test
    public void VnsCoefficientComputationTest() {
        final int order = 6;
        TreeMap<NSKey, Double> Vns = CoefficientFactory.computeVnsCoefficient(order);

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
        TreeMap<NSKey, Double> Vns30 = CoefficientFactory.computeVnsCoefficient(30);
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
        Assert.assertEquals(CoefficientFactory.getVmns2(0, 0, 0), CoefficientFactory.getVmns(0, 0, 0), eps8);
        Assert.assertEquals(CoefficientFactory.getVmns2(0, 1, 1), CoefficientFactory.getVmns(0, 1, 1), eps8);
        Assert.assertEquals(CoefficientFactory.getVmns2(0, 2, 2), CoefficientFactory.getVmns(0, 2, 2), eps8);
        Assert.assertEquals(CoefficientFactory.getVmns2(0, 3, 1), CoefficientFactory.getVmns(0, 3, 1), eps8);
        Assert.assertEquals(CoefficientFactory.getVmns2(0, 3, 3), CoefficientFactory.getVmns(0, 3, 3), eps8);
        Assert.assertEquals(CoefficientFactory.getVmns2(2, 2, 2), CoefficientFactory.getVmns(2, 2, 2), eps8);

    }

    /** Error if s > n */
    @Test(expected = OrekitException.class)
    public void VmnsErrorCallingSequenceWrong_N_S_Test() throws OrekitException {
        // if s > n
        CoefficientFactory.getVmns2(0, 0, 1);
    }

    /** Error if m > n */
    @Test(expected = OrekitException.class)
    public void VmnsErrorCallingSequenceWrong_M_S_Test() throws OrekitException {
        // if m > n
        CoefficientFactory.getVmns2(3, 2, 1);
    }

//    @Test
//    public void testVmnsCoefficientComputation() throws OrekitException {
//        final int N = 5;
//        final int M = 5;
//
//        @SuppressWarnings("deprecation")
//        Map<MNSKey, Double> map = CoefficientFactory.computeVmns2(M, N);
//        Iterator<Entry<MNSKey, Double>> it = map.entrySet().iterator();
//
//        while (it.hasNext()) {
//            Entry<MNSKey, Double> next = it.next();
//            System.out.println(next.getKey() + " " + next.getValue());
//        }
//        // With m = 0,
//        // final int order = 10;
//        // CoefficientFactory.getVmns(0, 2, 0);
//
//        Assert.assertEquals(map.get(new MNSKey(0, 0, 0)), CoefficientFactory.getVmns(0, 0, 0), eps8);
//        Assert.assertEquals(map.get(new MNSKey(0, 1, 1)), CoefficientFactory.getVmns(0, 1, 1), eps8);
//        Assert.assertEquals(map.get(new MNSKey(0, 2, 2)), CoefficientFactory.getVmns(0, 2, 2), eps8);
//        Assert.assertEquals(map.get(new MNSKey(0, 3, 1)), CoefficientFactory.getVmns(0, 3, 1), eps8);
//        Assert.assertEquals(map.get(new MNSKey(0, 3, 3)), CoefficientFactory.getVmns(0, 3, 3), eps8);
//    }

    /**
     * Qns test based on two computation method.
     * As methods are independent, if they give the same results, we assume them to be consistent.
     */
    @Test
    public void testQNS() {
        Assert.assertEquals(1., CoefficientFactory.getQnsPolynomialValue(0, 0, 0), 0.);
        // Method comparison :
        final int order = 10;
        final MersenneTwister random = new MersenneTwister(123456789);
        for (int g = 0; g < 1000; g++) {
            final double gamma = random.nextDouble();
            double[][] qns = CoefficientFactory.computeQnsCoefficient(gamma, order);
            for (int n = 0; n < order; n++) {
                for (int s = 0; s <= n; s++) {
                    Assert.assertEquals(qns[n][s], CoefficientFactory.getQnsPolynomialValue(gamma, n, s), Math.abs(eps11 * qns[n][s]));
                }
            }
        }
    }

    /**
     * Gs and Hs test based on 2 computation methods.
     * As methods are independent, if they give the same results, we assume them to be consistent.
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
            final double[][] GH = CoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, order);
            for (int j = 1; j < order; j++) {
                final double[] GsHs = CoefficientFactory.getGsHsCoefficient(k, h, alpha, beta, j);
                final double Gs = CoefficientFactory.getGsCoefficient(k, h, alpha, beta, j);
                final double Hs = CoefficientFactory.getHsCoefficient(k, h, alpha, beta, j);
                Assert.assertEquals(GsHs[0], Gs, 0.);
                Assert.assertEquals(GsHs[1], Hs, 0.);
                Assert.assertEquals(GsHs[0], GH[0][j], Math.abs(eps12 * Gs));
                Assert.assertEquals(GsHs[1], GH[1][j], Math.abs(eps12 * Hs));
            }
        }
    }
}
