package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.ModifiedNewcombOperators;

public class ModifiedNewcombOperatorTest {

    @Test
    public void multiplicationPolynomialsTest1() {
        List<PolynomialFunction> f1 = new ArrayList<PolynomialFunction>();
        List<PolynomialFunction> f2 = new ArrayList<PolynomialFunction>();

        // (1 + 2s + s^2) + (2 - 3s)n
        PolynomialFunction p1 = new PolynomialFunction(new double[] { 1, 2, 1 });
        PolynomialFunction p2 = new PolynomialFunction(new double[] { 2, -3 });
        f1.add(p1);
        f1.add(p2);

        // s + n
        PolynomialFunction q1 = new PolynomialFunction(new double[] { 0, 1 });
        PolynomialFunction q2 = new PolynomialFunction(new double[] { 1 });
        f2.add(q1);
        f2.add(q2);

        // Multiplication gives :
        List<PolynomialFunction> list = ModifiedNewcombOperators.NewcombPolynomialsGenerator.multiplyPolynomialList(f1, f2);

        // Constant term in s :
        checkPolynomial(list.get(0), "x + 2 x^2 + x^3");
        // Polynomial in 'n' :
        checkPolynomial(list.get(1), "1 + 4 x - 2 x^2");
        // Polynomial in 'n^2' :
        checkPolynomial(list.get(2), "2 - 3 x");
    }

    @Test
    public void multiplicationPolynomialsTest2() {
        List<PolynomialFunction> f1 = new ArrayList<PolynomialFunction>();
        List<PolynomialFunction> f2 = new ArrayList<PolynomialFunction>();

        // (1 + 3 s^2 + 8x^6) + (2 - 3s + 4s^2)n + (2s - 8s^4)n^3
        PolynomialFunction p1 = new PolynomialFunction(new double[] { 1, 0, 3, 0, 0, 0, 8 });
        PolynomialFunction p2 = new PolynomialFunction(new double[] { 2, -3, 4 });
        PolynomialFunction p3 = new PolynomialFunction(new double[] { 0 });
        PolynomialFunction p4 = new PolynomialFunction(new double[] { 0, 2, 0, 0, -8 });
        f1.add(p1);
        f1.add(p2);
        f1.add(p3);
        f1.add(p4);

        // (1 - s + 2s^2)n + (2 + s - 4s^2)n^3
        PolynomialFunction q1 = new PolynomialFunction(new double[] { 0 });
        PolynomialFunction q2 = new PolynomialFunction(new double[] { 1, -1, 2 });
        PolynomialFunction q3 = new PolynomialFunction(new double[] { 0 });
        PolynomialFunction q4 = new PolynomialFunction(new double[] { 2, 1, -4 });
        f2.add(q1);
        f2.add(q2);
        f2.add(q3);
        f2.add(q4);

        // Multiplication gives :
        List<PolynomialFunction> list = ModifiedNewcombOperators.NewcombPolynomialsGenerator.multiplyPolynomialList(f1, f2);

        // // No constant term in s :
        checkPolynomial(list.get(0), "0");
        // // Polynomial in 'n' :
        checkPolynomial(list.get(1), "1 - x + 5 x^2 - 3 x^3 + 6 x^4 + 8 x^6 - 8 x^7 + 16 x^8");
        // // Polynomial in 'n^2' :
        checkPolynomial(list.get(2), "2 - 5 x + 11 x^2 - 10 x^3 + 8 x^4");
        // // Polynomial in 'n^3' :
        checkPolynomial(list.get(3), "2 + x + 2 x^2 + 3 x^3 - 12 x^4 + 16 x^6 + 8 x^7 - 32 x^8");
        // // Polynomial in 'n^4' :
        checkPolynomial(list.get(4), "4 - 2 x - 5 x^2 + 20 x^3 - 24 x^4 + 8 x^5 - 16 x^6");
        // // No polynomial in 'n^5' :
        checkPolynomial(list.get(5), "0");
        // // Polynomial in 'n^6' :
        checkPolynomial(list.get(6), "4 x + 2 x^2 - 8 x^3 - 16 x^4 - 8 x^5 + 32 x^6");
    }

    /**
     * @throws OrekitException
     */
    @Test
    public void GenerationOfNewcombOperator_Rho_Sup_Sigma_Test() throws OrekitException {
        // (0, 0) : 1
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(0, 0).get(0), "1");
        // (1, 0) : s - n / 2
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(1, 0).get(0), "x");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(1, 0).get(1), "-0.5");
        // (2, 0) : (5/8 s + 1/2 s^2) + (-3/8 - 1/2s)n + 1/8 n^2
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(2, 0).get(0), "0.625 x + 0.5 x^2");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(2, 0).get(1), "-0.375 - 0.5 x");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(2, 0).get(2), "0.125");
        // (1, 1) : (3/2 + 1/2 s - 1/2 s^2) + n + 1/8 n^2
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(1, 1).get(0), "1.5 + 0.5 x - 0.5 x^2");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(1, 1).get(1), "1");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(1, 1).get(2), "0.125");
        // (3, 0) : (13/24 s + 5/8 s^2 + 1/6 s^3) + (-17/48 -11/16 s - 1/4 s^2)n + (3/16 + 1/8 s)n^2
        PolynomialFunction f0 = new PolynomialFunction(new double[] { 0, 13. / 24., 5. / 8., 1 / 6. });
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(3, 0).get(0), f0);
        PolynomialFunction f1 = new PolynomialFunction(new double[] { -17. / 48, -11. / 16, -1. / 4 });
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(3, 0).get(1), f1);
        PolynomialFunction f2 = new PolynomialFunction(new double[] { 3. / 16, 1. / 8 });
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(3, 0).get(2), f2);
    }

    @Test
    public void evaluationOfNewcombOperatorTests() throws OrekitException {
        // (1, 0) : s - n / 2. With s = 4, n = 2 -> Newcomb = 18,375
        Assert.assertEquals(3, ModifiedNewcombOperators.getValue(1, 0, 2, 4), 1e-15);
        // (2, 0) : (5/8 s + 1/2 s^2) + (-3/8 - 1/2s)n + 1/8 n^2. With s = 7, n = 3 -> Newcomb = 18.375
        Assert.assertEquals(18.375, ModifiedNewcombOperators.getValue(2, 0, 3, 7), 1e-15);
        // (3, 0) : With n = 3, s = 7 -> Newcomb = 48.3
        Assert.assertEquals(48.333333333333, ModifiedNewcombOperators.getValue(3, 0, 3, 7), 1e-12);
    }

    @Test
    public void GenerationOfNewcombOperator_Rho_Inf_Sigma_Test() {
        // (0, 1) : -s - n / 2
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(0, 1).get(0), "-x");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(0, 1).get(1), "-0.5");
        // (0, 2) : (5/8 s + 1/2 s^2) + (-3/8 - 1/2s)n + 1/8 n^2
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(0, 2).get(0), "-0.625 x + 0.5 x^2");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(0, 2).get(1), "-0.375 + 0.5 x");
        checkPolynomial(ModifiedNewcombOperators.getPolynomialList(0, 2).get(2), "0.125");
        ModifiedNewcombOperators.getPolynomialList(0, 10);

    }

    /** */
    @Test
    public void sumOfPolynomialsTest() {
        List<PolynomialFunction> f1 = new ArrayList<PolynomialFunction>();
        List<PolynomialFunction> f2 = new ArrayList<PolynomialFunction>();

        // (1 + 2s + s^2) + (2 - 3s)n
        PolynomialFunction p1 = new PolynomialFunction(new double[] { 1, 2, 1 });
        PolynomialFunction p2 = new PolynomialFunction(new double[] { 2, -3 });
        f1.add(p1);
        f1.add(p2);

        // s + n
        PolynomialFunction q1 = new PolynomialFunction(new double[] { 0, 1 });
        PolynomialFunction q2 = new PolynomialFunction(new double[] { 1 });
        f2.add(q1);
        f2.add(q2);

        // Sum gives :
        List<PolynomialFunction> list = ModifiedNewcombOperators.NewcombPolynomialsGenerator.sumPolynomialList(f1, f2);

        // Constant term in s :
        checkPolynomial(list.get(0), "1 + 3 x + x^2");
        // Polynomial in 'n' :
        checkPolynomial(list.get(1), "3 - 3 x");

    }

    public void checkPolynomial(PolynomialFunction p,
                                String reference) {
        Assert.assertEquals(reference, p.toString());
    }

    public void checkPolynomial(PolynomialFunction p,
                                PolynomialFunction reference) {
        if (p.degree() != reference.degree()) {
            Assert.assertTrue(false);
        }
        for (int i = 0; i < p.degree() + 1; i++) {
            Assert.assertEquals(reference.getCoefficients()[i], p.getCoefficients()[i], 1e-15);
        }
    }
}
