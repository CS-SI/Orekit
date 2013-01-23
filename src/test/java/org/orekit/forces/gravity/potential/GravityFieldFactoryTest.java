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


import java.io.File;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class GravityFieldFactoryTest {

    @Test
    public void testDefaultEGMMissingCoefficients() throws OrekitException {
        Utils.setDataRoot("potential/egm-format");
        // we explicitly DON'T call GravityFieldFactory.addPotentialCoefficientsReader
        // to make sure we use only the default readers
        try {
            GravityFieldFactory.getSphericalHarmonicsProvider(5, 3);
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
            GravityFieldFactory.getSphericalHarmonicsProvider(5, 3);
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
        SphericalHarmonicsProvider provider = GravityFieldFactory.getSphericalHarmonicsProvider(5, 3);
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
        SphericalHarmonicsProvider provider = GravityFieldFactory.getSphericalHarmonicsProvider(5, 3);
        Assert.assertEquals(5, provider.getMaxDegree());
        Assert.assertEquals(3, provider.getMaxOrder());
        Set<String> loaded = DataProvidersManager.getInstance().getLoadedDataNames();
        Assert.assertEquals(1, loaded.size());
        Assert.assertEquals("eigen_cg03c_coef", new File(loaded.iterator().next()).getName());
    }

}
