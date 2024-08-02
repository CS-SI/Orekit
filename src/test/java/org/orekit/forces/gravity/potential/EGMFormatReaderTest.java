/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class EGMFormatReaderTest {

    @Test
    void testReadNormalized() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", true));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        NormalizedSphericalHarmonics harmonics = provider.onDate(AbsoluteDate.FUTURE_INFINITY);
        assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        assertEquals( 0.957254173792E-06, harmonics.getNormalizedCnm(3, 0), 1.0e-15);
        assertEquals( 0.174971983203E-06, harmonics.getNormalizedCnm(5, 5), 1.0e-15);
        assertEquals( 0.0,                harmonics.getNormalizedSnm(4, 0), 1.0e-15);
        assertEquals( 0.308853169333E-06, harmonics.getNormalizedSnm(4, 4), 1.0e-15);
        assertEquals(-0.295301647654E-06, harmonics.getNormalizedCnm(5, 4), 1.0e-15);
    }

    @Test
    void testReadUnnormalized() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(AbsoluteDate.FUTURE_INFINITY);
        assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        int maxUlps = 1;
        checkValue(harmonics.getUnnormalizedCnm(3, 0), 3, 0, 0.957254173792E-06, maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5), 5, 5, 0.174971983203E-06, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0), 4, 0, 0.0,                maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4), 4, 4, 0.308853169333E-06, maxUlps);

        double a = (-0.295301647654E-06);
        double b = 9*8*7*6*5*4*3*2;
        double c = 2*11/b;
        double result = a*FastMath.sqrt(c);

        assertEquals(result, harmonics.getUnnormalizedCnm(5, 4), 1.0e-20);

        a = -0.188560802735E-06;
        b = 8*7*6*5*4*3*2;
        c=2*9/b;
        result = a*FastMath.sqrt(c);
        assertEquals(result, harmonics.getUnnormalizedCnm(4, 4), 1.0e-20);

        assertEquals(1.0826266835531513e-3, -harmonics.getUnnormalizedCnm(2, 0), 1.0e-20);

    }

    @Test
    void testReadLimits() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(3, 2);
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(null);
        try {
            harmonics.getUnnormalizedCnm(3, 3);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        try {
            harmonics.getUnnormalizedCnm(4, 2);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        harmonics.getUnnormalizedCnm(3, 2);
        assertEquals(3, provider.getMaxDegree());
        assertEquals(2, provider.getMaxOrder());
    }

    @Test
    void testCorruptedFile1() {
        assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("potential");
            GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("corrupted-1-egm96_to5", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    void testCorruptedFile2() {
        assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("potential");
            GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("corrupted-2-egm96_to5", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    void testCorruptedFile3() {
        assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("potential");
            GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("corrupted-3-egm96_to5", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    void testZeroTidePattern1() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("dummy_egm2008", true));
        assertEquals(TideSystem.ZERO_TIDE,
                            GravityFieldFactory.getUnnormalizedProvider(5, 5).getTideSystem());
    }

    @Test
    void testZeroTidePattern2() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("dummy_zerotide", true));
        assertEquals(TideSystem.ZERO_TIDE,
                            GravityFieldFactory.getUnnormalizedProvider(5, 5).getTideSystem());
    }

    @Test
    void testWgs84CoefficientOverride()
        {
        final double epsilon = Precision.EPSILON;

        Utils.setDataRoot("potential");
        EGMFormatReader egm96Reader = new EGMFormatReader("egm96_to5.ascii", true);
        GravityFieldFactory.addPotentialCoefficientsReader(egm96Reader);
        GravityFieldFactory.getNormalizedProvider(5, 5);
        assertEquals(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, egm96Reader.getAe(), epsilon);
        assertEquals(Constants.EGM96_EARTH_MU, egm96Reader.getMu(), epsilon);

        Utils.setDataRoot("potential");
        EGMFormatReader wgs84Egm96Reader = new EGMFormatReader("egm96_to5.ascii", true, true);
        GravityFieldFactory.addPotentialCoefficientsReader(wgs84Egm96Reader);
        GravityFieldFactory.getNormalizedProvider(5, 5);
        assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, wgs84Egm96Reader.getAe(), epsilon);
        assertEquals(Constants.WGS84_EARTH_MU, wgs84Egm96Reader.getMu(), epsilon);

    }

    private void checkValue(final double value, final int n, final int m,
                            final double constant, final int maxUlps)
        {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        double normalized = factor * constant;
        double epsilon = maxUlps * FastMath.ulp(normalized);
        assertEquals(normalized, value, epsilon);
    }

}
