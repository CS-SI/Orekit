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
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class ICGEMFormatReaderTest {

    @Test
    public void testReadLimits() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        SphericalHarmonicsProvider provider = GravityFieldFactory.getSphericalHarmonicsProvider(3, 2);
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

    @Test
    public void testRegular05c() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        SphericalHarmonicsProvider provider = GravityFieldFactory.getSphericalHarmonicsProvider(5, 5);
        AbsoluteDate refDate = new AbsoluteDate("2004-10-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2013-01-08T10:46:53", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);
        int maxUlps = 2;
        checkValue(provider.getUnnormalizedCnm(date.durationFrom(provider.getReferenceDate()), 3, 0),
                   date, 3, 0, 2004, 10, 1, 0.957212879862e-06, 0.490000000000e-11, 0, 0, 0, 0,
                   maxUlps);
        checkValue(provider.getUnnormalizedCnm(date.durationFrom(provider.getReferenceDate()), 5, 5),
                   date, 5, 5, 2004, 10, 1, 0.174804558032e-06, 0, 0, 0, 0, 0,
                   maxUlps);
        checkValue(provider.getUnnormalizedSnm(date.durationFrom(provider.getReferenceDate()), 4, 0),
                   date, 4, 0, 2004, 10, 1, 0, 0, 0, 0, 0, 0,
                   maxUlps);
        checkValue(provider.getUnnormalizedSnm(date.durationFrom(provider.getReferenceDate()), 4, 4),
                   date, 4, 4, 2004, 10, 1, 0.308816581016e-06, 0, 0, 0, 0, 0,
                   maxUlps);
        Assert.assertEquals(0.3986004415E+15, provider.getMu(), 1.0e-20);
        Assert.assertEquals(0.6378136460E+07, provider.getAe(), 1.0e-20);
    }

    @Test
    public void testEigen06() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", false));
        SphericalHarmonicsProvider provider = GravityFieldFactory.getSphericalHarmonicsProvider(5, 5);
        AbsoluteDate refDate = new AbsoluteDate("2005-01-01T12:00:00", TimeScalesFactory.getTT());
        Assert.assertEquals(refDate, provider.getReferenceDate());
        AbsoluteDate date = new AbsoluteDate("2013-01-08T10:46:53", TimeScalesFactory.getTT());
        Assert.assertEquals(date.durationFrom(refDate), provider.getOffset(date), Precision.SAFE_MIN);
        int maxUlps = 2;
        checkValue(provider.getUnnormalizedCnm(date.durationFrom(provider.getReferenceDate()), 3, 0),
                   date, 3, 0, 2005, 1, 1, 9.57211326674e-07, -8.37191630994e-12,
                   -1.76087178236e-11, 9.47617140143e-11, 1.06252954726e-11, -9.12524501214e-12,
                   maxUlps);
        checkValue(provider.getUnnormalizedCnm(date.durationFrom(provider.getReferenceDate()), 5, 5),
                   date, 5, 5, 2005, 1, 1, 1.74807033099e-07, -1.33498578664e-12,
                   -2.76043013690e-12, -8.28591865697e-12, 1.57898939101e-12, 2.90931436419e-12,
                   maxUlps);
        checkValue(provider.getUnnormalizedSnm(date.durationFrom(provider.getReferenceDate()), 4, 0),
                   date, 4, 0, 2005, 1, 1, 0, 0, 0, 0, 0, 0,
                   maxUlps);
        checkValue(provider.getUnnormalizedSnm(date.durationFrom(provider.getReferenceDate()), 4, 4),
                   date, 4, 4, 2005, 1, 1, 3.08820169866e-07, 4.35447782358e-13,
                   -1.21823769110e-11, 3.89722186321e-11, 7.28153817742e-12, -7.64506592459e-12,
                   maxUlps);
        Assert.assertEquals(0.3986004415E+15, provider.getMu(), 1.0e-20);
        Assert.assertEquals(0.6378136460E+07, provider.getAe(), 1.0e-20);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("corrupted-1-g007_eigen_coef", false));
        GravityFieldFactory.getSphericalHarmonicsProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("corrupted-2-g007_eigen_coef", false));
        GravityFieldFactory.getSphericalHarmonicsProvider(5, 5);
    }

    private void checkValue(final double value,
                            final AbsoluteDate date, final int n, final int m,
                            final int refYear, final int refMonth, final int refDay,
                            final double constant, final double trend,
                            final double cosYear, final double sinYear,
                            final double cosHalfYear, final double sinHalfYear,
                            final int maxUlps) {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        AbsoluteDate refDate = new AbsoluteDate(refYear, refMonth, refDay, 12, 0, 0, TimeScalesFactory.getTT());
        double dtYear = date.durationFrom(refDate) / Constants.JULIAN_YEAR;
        double normalized = factor * (constant +
                                      trend       * dtYear +
                                      cosYear     * FastMath.cos(MathUtils.TWO_PI * dtYear) +
                                      sinYear     * FastMath.sin(MathUtils.TWO_PI * dtYear) +
                                      cosHalfYear * FastMath.cos(MathUtils.TWO_PI * dtYear * 2) +
                                      sinHalfYear * FastMath.sin(MathUtils.TWO_PI * dtYear * 2));
        double epsilon = maxUlps * FastMath.ulp(normalized);
        Assert.assertEquals(normalized, value, epsilon);
    }

}
