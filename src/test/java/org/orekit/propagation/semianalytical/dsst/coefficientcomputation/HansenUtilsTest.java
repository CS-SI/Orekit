/**
 * 
 */
package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils;

/**
 * @author rdicosta
 */
public class HansenUtilsTest {

    private final static double epsilon = 1e-12;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test method for
     * {@link org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils#computeKnsCoefficient(int, double)}
     * .
     * 
     * @throws OrekitException
     */
    @Test
    public void testComputeKnsCoefficient() throws OrekitException {
        final double ecc = 0.1;
        final int maxOrder = 10;
        final int convergenceOrder = 10;

        Map<NSKey, Double> map = HansenUtils.computeHansenKernelForZonalHarmonicsReccurence(maxOrder, ecc);

        // Test where n = s = 0 >= 0
        for (int i = 0; i < maxOrder; i++) {
            assertEquals(0d, HansenUtils.computeKernelOfHansenForZonalHarmonics(ecc, i, i, convergenceOrder), epsilon);
            assertEquals(0d, map.get(new NSKey(-i - 1, i)), epsilon);
        }

        // Test where n = s + 1 >=1
        for (int n = 1; n < maxOrder - 1; n++) {
            final int s = n - 1;
            assertEquals(map.get(new NSKey(-n - 1, s)), HansenUtils.computeKernelOfHansenForZonalHarmonics(ecc, n, s, convergenceOrder), epsilon);
        }

        // Test for other cases :
        assertEquals(map.get(new NSKey(-2 - 1, 0)), HansenUtils.computeKernelOfHansenForZonalHarmonics(ecc, 2, 0, 1), epsilon);

        System.out.println(HansenUtils.computeKernelOfHansenForZonalHarmonics(ecc, 5, 0, 2));

        Iterator<Entry<NSKey, Double>> ite = map.entrySet().iterator();
        while (ite.hasNext()) {
            Entry<NSKey, Double> next = ite.next();
            System.out.println(next.getKey() + " " + next.getValue());
        }
        // System.out.println(map.get(new NSKey(-5 , 0)));

        System.out.println(HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(0.1, 1, 30, -20, 2));

    }

    @Test
    public void testPascal() throws OrekitException {
        final double ecc = 0.1;

        Map<NSKey, Double> map = HansenUtils.computeHansenKernelForZonalHarmonicsReccurence(10, ecc);
        Iterator<Entry<NSKey, Double>> ite = map.entrySet().iterator();
        while (ite.hasNext()) {
            Entry<NSKey, Double> next = ite.next();
            // System.out.println(next.getKey() + " " + next.getValue());
        }

        // System.out.println(map.get(new NSKey(-1, 0)));
        // System.out.println(HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -1, 0,
        // 15));
        for (int n = 0; n < 10; n++) {
            for (int s = 0; s < n; s++) {
                NSKey key = new NSKey(-n - 1, s);
                final double mapp = map.get(key);
                final double coef = HansenUtils.computeKernelOfHansenCoefficient(ecc, 0, n, s, 3);
                if ((n != 3) && (n != s + 1) && (n != -s + 1)) {
                    System.out.println(key + "    " + mapp + "    " + coef + "    " + (mapp - coef));
                }
            }
        }
        // System.out.println(HansenUtils.computeKernelOfHansenCoefficient(ecc, 0, -1, 0, 5));

        // System.out.println(HansenUtils.computeKernelOfHansenForZonalHarmonics(ecc, -1, 0, 5));

    }

    /**
     * Test method for
     * {@link org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils#computeKnsDerivatives(double[][], double)}
     * .
     */
    @Test
    public void testComputeKnsDerivatives() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils#computeKernelOfHansenCoefficientFromNewcomb(int, double, int, int, int)}
     * .
     * 
     * @throws OrekitException
     */
    @Test
    public void testComputeKernelOfHansenCoefficient() throws OrekitException {
        double ex = 0.1;
        double ey = 0.1;
        double ecc = FastMath.sqrt(ex * ex + ey * ey);
        System.out.println(HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(ecc, 2, 0, 1, 4));
    }

    /**
     * Test method for
     * {@link org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils#computeDerivativedKernelOfHansenCoefficient(int, double, int, int, int)}
     * .
     */
    @Test
    public void testComputeDerivativedKernelOfHansenCoefficient() {
        fail("Not yet implemented");
    }

}
