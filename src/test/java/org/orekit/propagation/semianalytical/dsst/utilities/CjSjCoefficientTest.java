package org.orekit.propagation.semianalytical.dsst.utilities;

import org.apache.commons.math3.complex.Complex;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;

public class CjSjCoefficientTest {

    @Test
    public void testGetCjSj() {
        final double epsilon = 1e-15;
        final double k = 0.1;
        final double h = 0.3;
        CjSjCoefficient cjsj = new CjSjCoefficient(k, h);

        // Initialization test
        Assert.assertEquals(cjsj.getCj(0), 1, epsilon);
        Assert.assertEquals(cjsj.getSj(0), 0, epsilon);
        Assert.assertEquals(cjsj.getCj(1), k, epsilon);
        Assert.assertEquals(cjsj.getSj(1), h, epsilon);

        // j = 2 : theoritical result C2 = k2-h2 & S2 = 2hk
        double cj = k * k - h * h;
        double sj = 2 * h * k;
        Assert.assertEquals(cjsj.getCj(2), cj, epsilon);
        Assert.assertEquals(cjsj.getSj(2), sj, epsilon);

        // j = 3 : theoritical result C3 = Re(k + ih)^3 & S3 = Im(k + ih)^3
        Complex result = new Complex(k, h).pow(3);
        Assert.assertEquals(cjsj.getCj(3), result.getReal(), epsilon);
        Assert.assertEquals(cjsj.getSj(3), result.getImaginary(), epsilon);

        // j = 15 : theoritical result C15 = Re(k + ih)^15 & S15 = Im(k + ih)^15
        result = new Complex(k, h).pow(15);
        // New computation :
        cjsj = new CjSjCoefficient(k, h);
        Assert.assertEquals(cjsj.getCj(15), result.getReal(), epsilon);
        Assert.assertEquals(cjsj.getSj(15), result.getImaginary(), epsilon);

    }

    @Test
    public void testDerivatives() {
        final double epsilon = 1e-15;
        final double k = 2d;
        final double h = 3d;
        CjSjCoefficient cjsj = new CjSjCoefficient(k, h);
        // Derivative test at order 2 :

        // Derivative dC2/dh = -2h
        Assert.assertEquals(cjsj.getDcjDh(2), -2 * h, epsilon);
        // Derivative dC2/dk = 2k
        Assert.assertEquals(cjsj.getDcjDk(2), 2 * k, epsilon);
        // Derivative dS2/dh = 2k
        Assert.assertEquals(cjsj.getDsjDh(2), 2 * k, epsilon);
        // Derivative dS2/dk = 2h
        Assert.assertEquals(cjsj.getDsjDk(2), 2 * h, epsilon);

        // Derivative test at order 3 :

        // Derivative dC3/dh = -6kh
        Assert.assertEquals(cjsj.getDcjDh(3), -6 * k * h, epsilon);
        // Derivative dC3/dk = 3 * (k^2 - h^2)
        Assert.assertEquals(cjsj.getDcjDk(3), 3 * (k * k - h * h), epsilon);
        // Derivative dS3/dh = 3 * (k^2-h^2)
        Assert.assertEquals(cjsj.getDsjDh(3), 3 * (k * k - h * h), epsilon);
        // Derivative dS3/dk = 6kh
        Assert.assertEquals(cjsj.getDsjDk(3), 6 * k * h, epsilon);

    }
}
