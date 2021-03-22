package org.orekit.utils;

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Tuple;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.utils.TwoSum.FieldSumAndResidual;
import org.orekit.utils.TwoSum.SumAndResidual;

/**
 * Unit tests for {@link TwoSum}.
 */
public final class TwoSumTest {

    /**
     * Tests {@link TwoSum#twoSum(double, double)}.
     */
    @Test
    public void testTwoSum() {
        //      sum = 0.30000000000000004
        // residual = -2.7755575615628914E-17
        final double a1 = 0.1;
        final double b1 = 0.2;
        final SumAndResidual result1 = TwoSum.twoSum(a1, b1);
        Assert.assertEquals(a1 + b1, result1.getSum(), 0.);
        Assert.assertEquals(a1 + b1, result1.getSum() + result1.getResidual(), 0.);

        //      sum = -1580.3205849419005
        // residual = -1.1368683772161603E-13
        final double a2 = -615.7212034581913;
        final double b2 = -964.5993814837093;
        final SumAndResidual result2 = TwoSum.twoSum(a2, b2);
        Assert.assertEquals(a2 + b2, result2.getSum(), 0.);
        Assert.assertEquals(a2 + b2, result2.getSum() + result2.getResidual(), 0.);

        //      sum = 251.8625825973395
        // residual = 1.4210854715202004E-14
        final double a3 = 60.348375484313706;
        final double b3 = 191.5142071130258;
        final SumAndResidual result3 = TwoSum.twoSum(a3, b3);
        Assert.assertEquals(a3 + b3, result3.getSum(), 0.);
        Assert.assertEquals(a3 + b3, result3.getSum() + result3.getResidual(), 0.);

        //      sum = 622.8319023175123
        // residual = -4.3315557304163255E-14
        final double a4 = 622.8314146170453;
        final double b4 = 0.0004877004669900762;
        final SumAndResidual result4 = TwoSum.twoSum(a4, b4);
        Assert.assertEquals(a4 + b4, result4.getSum(), 0.);
        Assert.assertEquals(a4 + b4, result4.getSum() + result4.getResidual(), 0.);
    }

    /**
     * Tests {@link TwoSum#twoSum(RealFieldElement, RealFieldElement)}.
     */
    @Test
    public void testTwoSumField() {
        final Tuple a = new Tuple(0.1, -615.7212034581913, 60.348375484313706, 622.8314146170453);
        final Tuple b = new Tuple(0.2, -964.5993814837093, 191.5142071130258, 0.0004877004669900762);
        final FieldSumAndResidual<Tuple> result = TwoSum.twoSum(a, b);
        for (int i = 0; i < a.getDimension(); ++i) {
            Assert.assertEquals(a.getComponent(i) + b.getComponent(i), result.getSum().getComponent(i), 0.);
            Assert.assertEquals(a.getComponent(i) + b.getComponent(i),
                    result.getSum().getComponent(i) + result.getResidual().getComponent(i), 0.);
        }
    }

}
