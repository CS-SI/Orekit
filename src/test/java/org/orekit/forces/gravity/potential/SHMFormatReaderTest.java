/* Copyright 2002-2016 CS Systèmes d'Information
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


import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class SHMFormatReaderTest {

    @Test
    public void testReadLimits() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_cg03c_coef", false));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(3, 2);
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(provider.getReferenceDate());
        try {
            harmonics.getUnnormalizedCnm(3, 3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        try {
            harmonics.getUnnormalizedCnm(4, 2);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        harmonics.getUnnormalizedCnm(3, 2);
        Assert.assertEquals(3, provider.getMaxDegree());
        Assert.assertEquals(2, provider.getMaxOrder());
    }

    @Test
    public void testRegular03cNormalized() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_cg03c_coef", false));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        Assert.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());

        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);

        NormalizedSphericalHarmonics harmonics = provider.onDate(date);
        double offset     = date.durationFrom(refDate);
        double offsetYear = offset / Constants.JULIAN_YEAR;
        Assert.assertEquals(0.957201462136e-06 + offsetYear * 0.490000000000e-11,
                            harmonics.getNormalizedCnm(3, 0), 1.0e-15);
        Assert.assertEquals( 0.174786174485e-06, harmonics.getNormalizedCnm(5, 5), 1.0e-15);
        Assert.assertEquals( 0.0,                harmonics.getNormalizedSnm(4, 0), 1.0e-15);
        Assert.assertEquals( 0.308834784975e-06, harmonics.getNormalizedSnm(4, 4), 1.0e-15);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test
    public void testRegular03cUnnormalized() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_cg03c_coef", false));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        Assert.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());

        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);

        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 2;
        checkValue(harmonics.getUnnormalizedCnm(3, 0), date, 3, 0,
                   1997, 1, 1, 0.957201462136e-06, 0.490000000000e-11, maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5), date, 5, 5,
                   1997, 1, 1, 0.174786174485e-06, 0.0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0), date, 4, 0,
                   1997, 1, 1, 0, 0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4), date, 4, 4,
                   1997, 1, 1, 0.308834784975e-06, 0, maxUlps);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test
    public void testReadCompressed01c() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("compressed-eigen-cg01c_coef", false));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        Assert.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());

        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);

        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 2;
        checkValue(harmonics.getUnnormalizedCnm(3, 0), date, 3, 0,
                   1997, 1, 1, 0.957187536534E-06, 0.490000000000E-11, maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5), date, 5, 5,
                   1997, 1, 1, 0.174787189024E-06, 0.0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0), date, 4, 0,
                   1997, 1, 1, 0, 0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4), date, 4, 4,
                   1997, 1, 1, 0.308834848269E-06, 0, maxUlps);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("corrupted-1-eigen_coef", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("corrupted-2-eigen_coef", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile3() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("corrupted-3-eigen_coef", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Test
    public void testZeroTide() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("dummy_unknown_tide_shm", false));
        Assert.assertEquals(TideSystem.UNKNOWN,
                            GravityFieldFactory.getUnnormalizedProvider(5, 5).getTideSystem());
    }

    private void checkValue(final double value,
                            final AbsoluteDate date, final int n, final int m,
                            final int refYear, final int refMonth, final int refDay,
                            final double constant, final double trend,
                            final int maxUlps)
        throws OrekitException {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        AbsoluteDate refDate = new AbsoluteDate(refYear, refMonth, refDay, 12, 0, 0, TimeScalesFactory.getTT());
        double dtYear = date.durationFrom(refDate) / Constants.JULIAN_YEAR;
        double normalized = factor * (constant + trend * dtYear);
        double epsilon = maxUlps * FastMath.ulp(normalized);
        Assert.assertEquals(normalized, value, epsilon);
    }

}
