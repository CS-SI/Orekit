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


import java.io.File;
import java.util.Set;

import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;

public class GravityFieldFactoryTest {

    @Test
    public void testDefaultEGMMissingCoefficients() throws OrekitException {
        Utils.setDataRoot("potential/egm-format");
        // we explicitly DON'T call GravityFieldFactory.addPotentialCoefficientsReader
        // to make sure we use only the default readers
        try {
            GravityFieldFactory.getUnnormalizedProvider(5, 3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("egm96_to5.ascii.gz", new File((String) oe.getParts()[3]).getName());
        }
    }

    @Test
    public void testDefaultGRGSMissingCoefficients() throws OrekitException {
        Utils.setDataRoot("potential/grgs-format");
        // we explicitly DON'T call GravityFieldFactory.addPotentialCoefficientsReader
        // to make sure we use only the default readers
        try {
            GravityFieldFactory.getUnnormalizedProvider(5, 3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("grim5_C1.dat", new File((String) oe.getParts()[3]).getName());
        }
    }

    @Test
    public void testDefaultIncludesICGEM() throws OrekitException {
        Utils.setDataRoot("potential/icgem-format");
        // we explicitly DON'T call GravityFieldFactory.addPotentialCoefficientsReader
        // to make sure we use only the default readers
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 3);
        Assert.assertEquals(5, provider.getMaxDegree());
        Assert.assertEquals(3, provider.getMaxOrder());
        Set<String> loaded = DataProvidersManager.getInstance().getLoadedDataNames();
        Assert.assertEquals(1, loaded.size());
        Assert.assertEquals("g007_eigen_05c_coef", new File(loaded.iterator().next()).getName());
    }

    @Test
    public void testDefaultIncludesSHM() throws OrekitException {
        Utils.setDataRoot("potential/shm-format");
        // we explicitly DON'T call GravityFieldFactory.addPotentialCoefficientsReader
        // to make sure we use only the default readers
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 3);
        Assert.assertEquals(5, provider.getMaxDegree());
        Assert.assertEquals(3, provider.getMaxOrder());
        Set<String> loaded = DataProvidersManager.getInstance().getLoadedDataNames();
        Assert.assertEquals(1, loaded.size());
        Assert.assertEquals("eigen_cg03c_coef", new File(loaded.iterator().next()).getName());
    }

    @Test
    public void testNormalizationFirstElements() throws OrekitException {
        int max = 50;
        double[][] factors = GravityFieldFactory.getUnnormalizationFactors(max, max);
        Assert.assertEquals(max + 1, factors.length);
        for (int i = 0; i <= max; ++i) {
            Assert.assertEquals(i + 1, factors[i].length);
            for (int j = 0; j <= i; ++j) {
                double ref = FastMath.sqrt((2 * i + 1) *
                                           CombinatoricsUtils.factorialDouble(i - j) /
                                           CombinatoricsUtils.factorialDouble(i + j));
                if (j > 0) {
                    ref *= FastMath.sqrt(2);
                }
                Assert.assertEquals(ref, factors[i][j], 8.0e-15);
            }
        }
    }

    @Test
    public void testNormalizationSquareField() throws OrekitException {
        int max = 89;
        double[][] factors = GravityFieldFactory.getUnnormalizationFactors(max, max);
        Assert.assertEquals(max + 1, factors.length);
        for (int i = 0; i <= max; ++i) {
            Assert.assertEquals(i + 1, factors[i].length);
            for (int j = 0; j <= i; ++j) {
                Assert.assertTrue(factors[i][j] > Precision.SAFE_MIN);
            }
        }
    }

    @Test
    public void testNormalizationLowOrder() throws OrekitException {
        int maxDegree = 393;
        int maxOrder  = 63;
        double[][] factors = GravityFieldFactory.getUnnormalizationFactors(maxDegree, maxOrder);
        Assert.assertEquals(maxDegree + 1, factors.length);
        for (int i = 0; i <= maxDegree; ++i) {
            Assert.assertEquals(FastMath.min(i, maxOrder) + 1, factors[i].length);
            for (int j = 0; j <= FastMath.min(i, maxOrder); ++j) {
                Assert.assertTrue(factors[i][j] > Precision.SAFE_MIN);
            }
        }
    }

    @Test(expected=OrekitException.class)
    public void testNormalizationUnderflowSquareField() throws OrekitException {
        GravityFieldFactory.getUnnormalizationFactors(90, 90);
    }

    @Test(expected=OrekitException.class)
    public void testNormalizationUnderflowLowOrder1() throws OrekitException {
        GravityFieldFactory.getUnnormalizationFactors(394, 63);
    }

    @Test(expected=OrekitException.class)
    public void testNormalizationUnderflowLowOrde2() throws OrekitException {
        GravityFieldFactory.getUnnormalizationFactors(393, 64);
    }

    @Test
    public void testUnnormalizer() throws OrekitException {
        Utils.setDataRoot("potential/icgem-format");
        final double shift = 1.23456e8;
        UnnormalizedSphericalHarmonicsProvider ref =
                GravityFieldFactory.getUnnormalizedProvider(5, 5);
        UnnormalizedSphericalHarmonics refHarmonics =
                ref.onDate(ref.getReferenceDate().shiftedBy(shift));
        NormalizedSphericalHarmonicsProvider normalized =
                GravityFieldFactory.getNormalizedProvider(5, 5);
        UnnormalizedSphericalHarmonicsProvider unnormalized =
                GravityFieldFactory.getUnnormalizedProvider(normalized);
        UnnormalizedSphericalHarmonics unnormalizedHarmonics =
                unnormalized.onDate(unnormalized.getReferenceDate().shiftedBy(shift));
        Assert.assertEquals(ref.getMaxDegree(), unnormalized.getMaxDegree());
        Assert.assertEquals(ref.getMaxOrder(), unnormalized.getMaxOrder());
        Assert.assertEquals(ref.getReferenceDate(), unnormalized.getReferenceDate());
        Assert.assertEquals(ref.getAe(), unnormalized.getAe(), FastMath.ulp(ref.getAe()));
        Assert.assertEquals(ref.getMu(), unnormalized.getMu(), FastMath.ulp(ref.getMu()));
        Assert.assertEquals(ref.getOffset(AbsoluteDate.GPS_EPOCH),
                            unnormalized.getOffset(AbsoluteDate.GPS_EPOCH),
                            FastMath.ulp(ref.getOffset(AbsoluteDate.GPS_EPOCH)));
        for (int i = 0; i <= 5; ++i) {
            for (int j = 0; j <= i; ++j) {
                double cRef  = refHarmonics.getUnnormalizedCnm(i, j);
                double cTest = unnormalizedHarmonics.getUnnormalizedCnm(i, j);
                Assert.assertEquals(cRef, cTest, FastMath.ulp(cRef));
                double sRef  = refHarmonics.getUnnormalizedSnm(i, j);
                double sTest = unnormalizedHarmonics.getUnnormalizedSnm(i, j);
                Assert.assertEquals(sRef, sTest, FastMath.ulp(sRef));
            }
        }
    }

    @Test
    public void testNormalizer() throws OrekitException {
        Utils.setDataRoot("potential/icgem-format");
        final double shift = 1.23456e8;
        NormalizedSphericalHarmonicsProvider ref =
                GravityFieldFactory.getNormalizedProvider(5, 5);
        NormalizedSphericalHarmonics refHarmonics =
                ref.onDate(ref.getReferenceDate().shiftedBy(shift));
        UnnormalizedSphericalHarmonicsProvider unnormalized =
                GravityFieldFactory.getUnnormalizedProvider(5, 5);
        NormalizedSphericalHarmonicsProvider normalized =
                GravityFieldFactory.getNormalizedProvider(unnormalized);
        NormalizedSphericalHarmonics normalizedHarmonics =
                normalized.onDate(normalized.getReferenceDate().shiftedBy(shift));
        Assert.assertEquals(ref.getMaxDegree(), normalized.getMaxDegree());
        Assert.assertEquals(ref.getMaxOrder(), normalized.getMaxOrder());
        Assert.assertEquals(ref.getReferenceDate(), normalized.getReferenceDate());
        Assert.assertEquals(ref.getAe(), normalized.getAe(), FastMath.ulp(ref.getAe()));
        Assert.assertEquals(ref.getMu(), normalized.getMu(), FastMath.ulp(ref.getMu()));
        Assert.assertEquals(ref.getOffset(AbsoluteDate.GPS_EPOCH),
                            normalized.getOffset(AbsoluteDate.GPS_EPOCH),
                            FastMath.ulp(ref.getOffset(AbsoluteDate.GPS_EPOCH)));
        for (int i = 0; i <= 5; ++i) {
            for (int j = 0; j <= i; ++j) {
                double cRef  = refHarmonics.getNormalizedCnm(i, j);
                double cTest = normalizedHarmonics.getNormalizedCnm(i, j);
                Assert.assertEquals(cRef, cTest, FastMath.ulp(cRef));
                double sRef  = refHarmonics.getNormalizedSnm(i, j);
                double sTest = normalizedHarmonics.getNormalizedSnm(i, j);
                Assert.assertEquals(sRef, sTest, FastMath.ulp(sRef));
            }
        }
    }

}
