/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class GRGSFormatReaderTest {

    @Test
    public void testAdditionalColumn() {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5-c1.txt", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        Assertions.assertEquals(TideSystem.UNKNOWN, provider.getTideSystem());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());

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
        Assertions.assertEquals(0.3986004415E+15 , provider.getMu(),  0);
        Assertions.assertEquals(0.6378136460E+07 , provider.getAe(),  0);

    }

    @Test
    public void testRegular05cNormalized() {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        Assertions.assertEquals(TideSystem.UNKNOWN, provider.getTideSystem());

        AbsoluteDate refDate = new AbsoluteDate("1997-01-01T12:00:00", TimeScalesFactory.getTT());
        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());

        double offset     = date.durationFrom(refDate);
        double offsetYear = offset / Constants.JULIAN_YEAR;
        NormalizedSphericalHarmonics harmonics = provider.onDate(date);
        Assertions.assertEquals(0.95857491635129E-06 + offsetYear * 0.28175700027753E-11,
                            harmonics.getNormalizedCnm(3, 0), 1.0e-15);
        Assertions.assertEquals( 0.17481512311600E-06, harmonics.getNormalizedCnm(5, 5), 1.0e-15);
        Assertions.assertEquals( 0.0,                harmonics.getNormalizedSnm(4, 0), 1.0e-15);
        Assertions.assertEquals( 0.30882755318300E-06, harmonics.getNormalizedSnm(4, 4), 1.0e-15);
        Assertions.assertEquals(0.3986004415E+15 , provider.getMu(),  0);
        Assertions.assertEquals(0.6378136460E+07 , provider.getAe(),  0);
    }

    @Test
    public void testRegular05cUnnormalized() {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        AbsoluteDate date = new AbsoluteDate("2011-05-01T01:02:03", TimeScalesFactory.getTT());

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
        Assertions.assertEquals(0.3986004415E+15 , provider.getMu(),  0);
        Assertions.assertEquals(0.6378136460E+07 , provider.getAe(),  0);

    }

    @Test
    public void testReadLimits() {
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(3, 2);
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(new AbsoluteDate(1997, 1, 1, 12, 0, 0.0, TimeScalesFactory.getTT()));
        try {
            harmonics.getUnnormalizedCnm(3, 3);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assertions.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        try {
            harmonics.getUnnormalizedCnm(4, 2);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assertions.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        harmonics.getUnnormalizedCnm(3, 2);
        Assertions.assertEquals(3, provider.getMaxDegree());
        Assertions.assertEquals(2, provider.getMaxOrder());
    }

    @Test
    public void testCorruptedFile1() {
        Assertions.assertThrows(OrekitException.class, () -> {
            GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-1-grim5.dat", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    public void testCorruptedFile2() {
        Assertions.assertThrows(OrekitException.class, () -> {
            GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-2-grim5.dat", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    public void testCorruptedFile3() {
        Assertions.assertThrows(OrekitException.class, () -> {
            GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-3-grim5.dat", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    public void testCorruptedFile4() {
        Assertions.assertThrows(OrekitException.class, () -> {
            GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("corrupted-4-grim5.dat", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("potential:regular-data");
    }

    private void checkValue(final double value,
                            final AbsoluteDate date, final int n, final int m,
                            final int refYear, final int refMonth, final int refDay,
                            final double constant, final double trend,
                            final int maxUlps) {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        AbsoluteDate refDate = new AbsoluteDate(refYear, refMonth, refDay, 12, 0, 0, TimeScalesFactory.getTT());
        double dtYear = date.durationFrom(refDate) / Constants.JULIAN_YEAR;
        double normalized = factor * (constant + trend * dtYear);
        double epsilon = maxUlps * FastMath.ulp(normalized);
        Assertions.assertEquals(normalized, value, epsilon);
    }

}
