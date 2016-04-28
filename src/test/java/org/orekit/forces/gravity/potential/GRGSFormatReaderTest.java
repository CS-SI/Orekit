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
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class GRGSFormatReaderTest {

    @Test
    public void testAdditionalColumn() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5-c1.txt", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        Assert.assertEquals(TideSystem.UNKNOWN, provider.getTideSystem());
        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);

        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 2;
        checkValue(harmonics.getUnnormalizedCnm(3, 0), date, 3, 0,
                   1997, 1, 1, 0.95857491635129E-06, 0.28175700027753E-11, maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5), date, 5, 5,
                   1997, 1, 1, 0.17481512311600E-06, 0.0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0), date, 4, 0,
                   1997, 1, 1, 0, 0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4), date, 4, 4,
                   1997, 1, 1, 0.30882755318300E-06, 0, maxUlps);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test
    public void testRegular05cNormalized() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        Assert.assertEquals(TideSystem.UNKNOWN, provider.getTideSystem());

        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);

        double offset     = date.durationFrom(refDate);
        double offsetYear = offset / Constants.JULIAN_YEAR;
        NormalizedSphericalHarmonics harmonics = provider.onDate(date);
        Assert.assertEquals(0.95857491635129E-06 + offsetYear * 0.28175700027753E-11,
                            harmonics.getNormalizedCnm(3, 0), 1.0e-15);
        Assert.assertEquals( 0.17481512311600E-06, harmonics.getNormalizedCnm(5, 5), 1.0e-15);
        Assert.assertEquals( 0.0,                harmonics.getNormalizedSnm(4, 0), 1.0e-15);
        Assert.assertEquals( 0.30882755318300E-06, harmonics.getNormalizedSnm(4, 4), 1.0e-15);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);
    }

    @Test
    public void testRegular05cUnnormalized() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);

        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 2;
        checkValue(harmonics.getUnnormalizedCnm(3, 0), date, 3, 0,
                   1997, 1, 1, 0.95857491635129E-06, 0.28175700027753E-11, maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5), date, 5, 5,
                   1997, 1, 1, 0.17481512311600E-06, 0.0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0), date, 4, 0,
                   1997, 1, 1, 0, 0, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4), date, 4, 4,
                   1997, 1, 1, 0.30882755318300E-06, 0, maxUlps);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test
    public void testReadLimits() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
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

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-1-grim5.dat", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-2-grim5.dat", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile3() throws OrekitException {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-3-grim5.dat", false));
        GravityFieldFactory.getUnnormalizedProvider(5, 5);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("potential:regular-data");
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
