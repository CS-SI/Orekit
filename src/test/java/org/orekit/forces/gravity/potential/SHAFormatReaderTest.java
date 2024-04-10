/* Contributed in the public domain.
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
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;

public class SHAFormatReaderTest {

    @Test
    public void testReadNormalized() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHAFormatReader("sha.grgm1200b_sigma_truncated_5x5", true));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        NormalizedSphericalHarmonics harmonics = provider.onDate(AbsoluteDate.FUTURE_INFINITY);
        Assertions.assertEquals(TideSystem.UNKNOWN, provider.getTideSystem());
        Assertions.assertEquals(-3.1973502105869101E-06, harmonics.getNormalizedCnm(3, 0), 1.0e-15);
        Assertions.assertEquals( 3.1105527966439498E-06, harmonics.getNormalizedCnm(5, 5), 1.0e-15);
        Assertions.assertEquals( 0.0, harmonics.getNormalizedSnm(4, 0), 1.0e-15);
        Assertions.assertEquals( 3.9263792903879803E-06, harmonics.getNormalizedSnm(4, 4), 1.0e-15);
        Assertions.assertEquals(2.7542657233402899E-06, harmonics.getNormalizedCnm(5, 4), 1.0e-15);
    }

    @Test
    public void testReadUnnormalized() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHAFormatReader("sha.grgm1200b_sigma_truncated_5x5", true));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(AbsoluteDate.FUTURE_INFINITY);
        Assertions.assertEquals(TideSystem.UNKNOWN, provider.getTideSystem());
        int maxUlps = 1;
        checkValue(harmonics.getUnnormalizedCnm(3, 0), 3, 0, -3.1973502105869101E-06, maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5), 5, 5, 3.1105527966439498E-06, maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0), 4, 0, 0.0,                maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4), 4, 4, 3.9263792903879803E-06, maxUlps);

        double a = (2.7542657233402899E-06);
        double b = 9*8*7*6*5*4*3*2;
        double c = 2*11/b;
        double result = a*FastMath.sqrt(c);

        Assertions.assertEquals(result, harmonics.getUnnormalizedCnm(5, 4), 1.0e-20);

        a = -6.0069538669876603E-06;
        b = 8*7*6*5*4*3*2;
        c=2*9/b;
        result = a*FastMath.sqrt(c);
        Assertions.assertEquals(result, harmonics.getUnnormalizedCnm(4, 4), 1.0e-20);

        Assertions.assertEquals(2.0321922328195912e-4, -harmonics.getUnnormalizedCnm(2, 0), 1.0e-20);
    }

    @Test
    public void testCorruptedFile1() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("potential");
            GravityFieldFactory.addPotentialCoefficientsReader(new SHAFormatReader("corrupted-1_sha.grgm1200b_sigma_truncated_5x5", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    public void testCorruptedFile2() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("potential");
            GravityFieldFactory.addPotentialCoefficientsReader(new SHAFormatReader("corrupted-2_sha.grgm1200b_sigma_truncated_5x5", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    @Test
    public void testCorruptedFile3() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("potential");
            GravityFieldFactory.addPotentialCoefficientsReader(new SHAFormatReader("corrupted-3_sha.grgm1200b_sigma_truncated_5x5", false));
            GravityFieldFactory.getUnnormalizedProvider(5, 5);
        });
    }

    private void checkValue(final double value, final int n, final int m,
                            final double constant, final int maxUlps)
        {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        double normalized = factor * constant;
        double epsilon = maxUlps * FastMath.ulp(normalized);
        Assertions.assertEquals(normalized, value, epsilon);
    }
}
