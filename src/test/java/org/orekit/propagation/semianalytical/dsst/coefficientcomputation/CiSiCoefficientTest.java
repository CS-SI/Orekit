package org.orekit.propagation.semianalytical.dsst.coefficientcomputation;

import org.apache.commons.math3.complex.Complex;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.propagation.semianalytical.dsst.coefficients.CiSiCoefficient;

public class CiSiCoefficientTest {

    /** Test the CiSi series generation */
    @Test
    public void testGetCiSi() {
        final double epsilon = 1e-12;
        final double k = 0.1;
        final double h = 0.3;
        CiSiCoefficient cisi = new CiSiCoefficient(k, h);

        // Initialization test
        Assert.assertEquals(cisi.getCi(0), 1, epsilon);
        Assert.assertEquals(cisi.getSi(0), 0, epsilon);
        Assert.assertEquals(cisi.getCi(1), k, epsilon);
        Assert.assertEquals(cisi.getSi(1), h, epsilon);

        // j = 2 : theoritical result C2 = k2-h2 & S2 = 2hk
        double ci = k * k - h * h;
        double si = 2 * h * k;
        Assert.assertEquals(cisi.getCi(2), ci, epsilon);
        Assert.assertEquals(cisi.getSi(2), si, epsilon);

        // j = 3 : theoritical result C3 = Re(k + ih)^3 & S3 = Im(k + ih)^3
        Complex result = new Complex(k, h).pow(3);
        Assert.assertEquals(cisi.getCi(3), result.getReal(), epsilon);
        Assert.assertEquals(cisi.getSi(3), result.getImaginary(), epsilon);

        // j = 15 : theoritical result C15 = Re(k + ih)^15 & S15 = Im(k + ih)^15
        result = new Complex(k, h).pow(15);
        // New computation :
        cisi = new CiSiCoefficient(k, h);
        Assert.assertEquals(cisi.getCi(15), result.getReal(), epsilon);
        Assert.assertEquals(cisi.getSi(15), result.getImaginary(), epsilon);

    }

    @Test
    public void testDerivatives() {
        final double epsilon = 1e-12;
        final double k = 2d;
        final double h = 3d;
        CiSiCoefficient cisi = new CiSiCoefficient(k, h);
        // Derivative test at order 2 :

        // Derivative dC2/dh = -2h
        Assert.assertEquals(cisi.getDciDh(2), -2 * h, epsilon);
        // Derivative dC2/dk = 2k
        Assert.assertEquals(cisi.getDciDk(2), 2 * k, epsilon);
        // Derivative dS2/dh = 2k
        Assert.assertEquals(cisi.getDsiDh(2), 2 * k, epsilon);
        // Derivative dS2/dk = 2h
        Assert.assertEquals(cisi.getDsiDk(2), 2 * h, epsilon);

        // Derivative test at order 3 :

        // Derivative dC3/dh = -6kh
        Assert.assertEquals(cisi.getDciDh(3), -6 * k * h, epsilon);
        // Derivative dC3/dk = 3 * (k^2 - h^2)
        Assert.assertEquals(cisi.getDciDk(3), 3 * (k * k - h * h), epsilon);
        // Derivative dS3/dh = 3 * (k^2-h^2)
        Assert.assertEquals(cisi.getDsiDh(3), 3 * (k * k - h * h), epsilon);
        // Derivative dS3/dk = 6kh
        Assert.assertEquals(cisi.getDsiDk(3), 6 * k * h, epsilon);

    }
}
