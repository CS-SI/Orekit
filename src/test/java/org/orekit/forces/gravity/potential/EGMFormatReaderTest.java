/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.forces.gravity.potential;


import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

public class EGMFormatReaderTest {

    @Test
    public void testReadNormalized() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", true));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        Assert.assertEquals( 0.957254173792E-06, provider.getNormalizedCnm(Double.NaN, 3, 0), 1.0e-15);
        Assert.assertEquals( 0.174971983203E-06, provider.getNormalizedCnm(Double.NaN, 5, 5), 1.0e-15);
        Assert.assertEquals( 0.0,                provider.getNormalizedSnm(Double.NaN, 4, 0), 1.0e-15);
        Assert.assertEquals( 0.308853169333E-06, provider.getNormalizedSnm(Double.NaN, 4, 4), 1.0e-15);
        Assert.assertEquals(-0.295301647654E-06, provider.getNormalizedCnm(Double.NaN, 5, 4), 1.0e-15);

        Assert.assertNull(provider.getReferenceDate());
        Assert.assertEquals(0, provider.getOffset(AbsoluteDate.J2000_EPOCH), Precision.SAFE_MIN);
        Assert.assertEquals(0, provider.getOffset(AbsoluteDate.MODIFIED_JULIAN_EPOCH), Precision.SAFE_MIN);
    }

    @Test
    public void testReadUnnormalized() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        int maxUlps = 1;
        checkValue(provider.getUnnormalizedCnm(Double.NaN, 3, 0), 3, 0, 0.957254173792E-06, maxUlps);
        checkValue(provider.getUnnormalizedCnm(Double.NaN, 5, 5), 5, 5, 0.174971983203E-06, maxUlps);
        checkValue(provider.getUnnormalizedSnm(Double.NaN, 4, 0), 4, 0, 0.0,                maxUlps);
        checkValue(provider.getUnnormalizedSnm(Double.NaN, 4, 4), 4, 4, 0.308853169333E-06, maxUlps);

        double a = (-0.295301647654E-06);
        double b = 9*8*7*6*5*4*3*2;
        double c = 2*11/b;
        double result = a*FastMath.sqrt(c);

        Assert.assertEquals(result, provider.getUnnormalizedCnm(Double.NaN, 5, 4), 1.0e-20);

        a = -0.188560802735E-06;
        b = 8*7*6*5*4*3*2;
        c=2*9/b;
        result = a*FastMath.sqrt(c);
        Assert.assertEquals(result, provider.getUnnormalizedCnm(Double.NaN, 4, 4), 1.0e-20);

        Assert.assertEquals(1.0826266835531513e-3, -provider.getUnnormalizedCnm(Double.NaN, 2, 0), 1.0e-20);

        Assert.assertNull(provider.getReferenceDate());
        Assert.assertEquals(0, provider.getOffset(AbsoluteDate.J2000_EPOCH), Precision.SAFE_MIN);
        Assert.assertEquals(0, provider.getOffset(AbsoluteDate.MODIFIED_JULIAN_EPOCH), Precision.SAFE_MIN);
    }

    @Test
    public void testReadLimits() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(3, 2);
        try {
            provider.getUnnormalizedCnm(0.0, 3, 3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        try {
            provider.getUnnormalizedCnm(0.0, 4, 2);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        provider.getUnnormalizedCnm(0.0, 3, 2);
        Assert.assertEquals(3, provider.getMaxDegree());
        Assert.assertEquals(2, provider.getMaxOrder());
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("corrupted-1-egm96_to5", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("corrupted-2-egm96_to5", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    private void checkValue(final double value, final int n, final int m,
                            final double constant, final int maxUlps)
        throws OrekitException {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        double normalized = factor * constant;
        double epsilon = maxUlps * FastMath.ulp(normalized);
        Assert.assertEquals(normalized, value, epsilon);
    }

}
