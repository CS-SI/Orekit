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

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;

public class AstronomicalAmplitudeReaderTest {

    @Test
    public void testHfFES2004()
        throws OrekitException {
        AstronomicalAmplitudeReader reader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
        Map<Integer, Double> astronomicalAmplitudesMap = reader.getAstronomicalAmplitudesMap();

        Assert.assertEquals(81, astronomicalAmplitudesMap.size());

        // check all main waves
        Assert.assertEquals( 0.02793, astronomicalAmplitudesMap.get( 55565), 1.0e-10);
        Assert.assertEquals(-0.00492, astronomicalAmplitudesMap.get( 56554), 1.0e-10);
        Assert.assertEquals(-0.03100, astronomicalAmplitudesMap.get( 57555), 1.0e-10);
        Assert.assertEquals(-0.03518, astronomicalAmplitudesMap.get( 65455), 1.0e-10);
        Assert.assertEquals(-0.06663, astronomicalAmplitudesMap.get( 75555), 1.0e-10);
        Assert.assertEquals(-0.01276, astronomicalAmplitudesMap.get( 85455), 1.0e-10);
        Assert.assertEquals(-0.00204, astronomicalAmplitudesMap.get( 93555), 1.0e-10);
        Assert.assertEquals(-0.05020, astronomicalAmplitudesMap.get(135655), 1.0e-10);
        Assert.assertEquals(-0.26221, astronomicalAmplitudesMap.get(145555), 1.0e-10);
        Assert.assertEquals(-0.12203, astronomicalAmplitudesMap.get(163555), 1.0e-10);
        Assert.assertEquals( 0.36878, astronomicalAmplitudesMap.get(165555), 1.0e-10);
        Assert.assertEquals( 0.01601, astronomicalAmplitudesMap.get(235755), 1.0e-10);
        Assert.assertEquals( 0.12099, astronomicalAmplitudesMap.get(245655), 1.0e-10);
        Assert.assertEquals( 0.63192, astronomicalAmplitudesMap.get(255555), 1.0e-10);
        Assert.assertEquals( 0.29400, astronomicalAmplitudesMap.get(273555), 1.0e-10);
        Assert.assertEquals( 0.07996, astronomicalAmplitudesMap.get(275555), 1.0e-10);

        // check a few secondary waves
        Assert.assertEquals( 0.00231, astronomicalAmplitudesMap.get( 65445), 1.0e-10);
        Assert.assertEquals( 0.00229, astronomicalAmplitudesMap.get( 65465), 1.0e-10);
        Assert.assertEquals(-0.00375, astronomicalAmplitudesMap.get( 65555), 1.0e-10);
        Assert.assertEquals( 0.00414, astronomicalAmplitudesMap.get(155665), 1.0e-10);
        Assert.assertEquals( 0.00359, astronomicalAmplitudesMap.get(265555), 1.0e-10);
        Assert.assertEquals( 0.00447, astronomicalAmplitudesMap.get(265655), 1.0e-10);
        Assert.assertEquals( 0.00197, astronomicalAmplitudesMap.get(265665), 1.0e-10);
        Assert.assertEquals( 0.00195, astronomicalAmplitudesMap.get(285465), 1.0e-10);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("tides");
    }

}
