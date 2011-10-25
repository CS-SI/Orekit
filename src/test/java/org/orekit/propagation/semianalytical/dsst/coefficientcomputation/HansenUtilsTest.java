/**
 * 
 */
package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math.util.ArithmeticUtils;
import org.apache.commons.math.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.MNSKey;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenCoefficients;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils;

/**
 * @author rdicosta
 */
public class HansenUtilsTest {

    private final static double epsilon = 1e-3;

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

        Map<NSKey, Double> map = HansenUtils.computeHansenKernelForZonalHarmonicsReccurence(ecc, maxOrder);

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

        System.out.println(HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(0.1, 1, 30, -20, epsilon));

    }

    @Test
    public void testPascal() throws OrekitException {
        final double ecc = 0.1;

        final int nMax = 15;
        double start = System.currentTimeMillis();

        Map<MNSKey, Double> map = HansenUtils.computeKernelOfHansenCoefficientNEW(ecc, 0, 0, nMax, epsilon);
        //
        for (int s = -nMax; s < nMax; s++) {
            int nMin = FastMath.max(2, Math.abs(s));
            for (int n = nMin; n < nMax; n++) {
                MNSKey key = new MNSKey(0, -n - 1, s);
                final double mapp = map.get(key);
                final double mapp2 = HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -n - 1, s, epsilon);
                final double coef = HansenUtils.computeKernelOfHansenCoefficient(ecc, 0, n, s, epsilon);
                if ((n != 3) && (n != s + 1) && (n != -s + 1)) {
                    System.out.println((-n - 1) + "  " + s + "    " + mapp + "    " + mapp2 + "  " + coef + "  " + ((mapp - mapp2) / mapp)
                                    * 100 + "  " + ((mapp - coef) / mapp) * 100 + "  " + ((mapp2 - coef) / mapp2) * 100);
                }
            }
        }
        double end = System.currentTimeMillis();

        System.out.println((end - start) / 1000d);
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
    public void testComputeKernelOfHansenCoefficientRecurrenceCall() throws OrekitException {
        double ecc = 0.1;
        final int nMax = 30;

        final HansenCoefficients hansen = new HansenCoefficients(ecc);
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 4, 0) + " " + hansen.getHansenKernelValue(0, 0, -5, 0));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 3, 0) + " " + hansen.getHansenKernelValue(0, 0, -4, 0));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 2, 0) + " " + hansen.getHansenKernelValue(0, 0, -3, 0));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 1, 0) + " " + hansen.getHansenKernelValue(0, 0, -2, 0));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 0, 0) + " " + hansen.getHansenKernelValue(0, 0, -1, 0));
        //
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 4, 1) + " " + hansen.getHansenKernelValue(0, 0, -5, 1));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 3, 1) + " " + hansen.getHansenKernelValue(0, 0, -4, 1));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 2, 1) + " " + hansen.getHansenKernelValue(0, 0, -3, 1));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 1, 1) + " " + hansen.getHansenKernelValue(0, 0, -2, 1));

        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 3, 1) + " " + hansen.getHansenKernelValue(0, 0, -4, 1));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 2, 1) + " " + hansen.getHansenKernelValue(0, 0, -3, 1));
        System.out.println(HansenUtils.computeHansenKernelReccurenceCALL(ecc, 1, 1) + " " + hansen.getHansenKernelValue(0, 0, -2, 1));
        
        // System.out.println(HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(ecc, 0, -5, 3,
        // epsilon));

        // Map<MNSKey, Double> map = HansenUtils.computeKernelOfHansenCoefficientNEW(ecc, 0, 0,
        // nMax, epsilon);

        for (int s = -nMax; s < nMax; s++) {
            int nMin = FastMath.max(2, Math.abs(s));
            for (int n = nMin; n < nMax; n++) {
                double res = HansenUtils.computeHansenKernelReccurenceCALL(ecc, n, s);
                // double ref = map.get(new MNSKey(0, -n - 1, s));
                // System.out.println(res + "   " + ref + "   " + (res - ref));

                System.out.println((-n - 1) + "  " + s + "  " + res);

            }
        }
    }

    @Test
    public void testHansenCoefficientSymetry() throws OrekitException {
        double ecc = 0.1;

        // Map<MNSKey, Double> map = HansenUtils.computeKernelOfHansenCoefficientNEW(ecc, 2, 0, 15,
        // epsilon);
        double value = HansenUtils.computeKernelOfHansenCoefficientRecurssif(ecc, 2, 0, 15, 5, epsilon);
        System.out.println(value);
        // -n-1 check
        // n = 1
        final double ref_n1_s0 = FastMath.pow(1 - ecc * ecc, -0.5);
        ArithmeticUtils.binomialCoefficient(2, 0);
        // System.out.println(map.get(new MNSKey(0, -2, 0)) + "  " + ref_n1_s0);

        // for (int s = -nMax; s < nMax; s++) {
        // int nMin = FastMath.max(2, Math.abs(s));
        // for (int n = nMin; n < nMax; n++) {
        // MNSKey keyPlus = new MNSKey(0, -n - 1, s);
        // MNSKey keyMinus = new MNSKey(0, -n - 1, -s);
        //
        // System.out.println(map.get(keyPlus) + " " + map.get(keyMinus) + "  " +
        // Math.abs(map.get(keyPlus) - map.get(keyMinus))) ;
        // }
        // }

    }

    /**
     * Test method for
     * {@link org.orekit.propagation.semianalytical.dsst.dsstforcemodel.HansenUtils#computeDerivativedKernelOfHansenCoefficient(int, double, int, int, int)}
     * .
     * 
     * @throws OrekitException
     */
    @Test
    public void testComputeDerivativedKernelOfHansenCoefficient() throws OrekitException {
        double ecc = 0.1;
        int nMax = 10;
        Map<MNSKey, Double> map = new TreeMap<MNSKey, Double>();
        for (int s = -nMax; s < nMax; s++) {
            int nMin = FastMath.max(2, Math.abs(s));
            for (int n = nMin; n < nMax; n++) {
                map = HansenUtils.computeDerivative(ecc, n, s, epsilon);
                // System.out.println(map.get(new MNSKey(0, -n - 1, s)));
            }
        }
        System.out.println();
    }

}
