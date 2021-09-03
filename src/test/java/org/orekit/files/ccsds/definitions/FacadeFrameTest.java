/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.utils.IERSConventions;


public class FacadeFrameTest {

    @Test
    public void testMapCelestial() {
        for (CelestialBodyFrame cbf : CelestialBodyFrame.values()) {
            FrameFacade ff = FrameFacade.parse(cbf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assert.assertSame(cbf, ff.asCelestialBodyFrame());
            Assert.assertNull(ff.asOrbitRelativeFrame());
            Assert.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapLOF() {
        for (OrbitRelativeFrame orf : OrbitRelativeFrame.values()) {
            FrameFacade ff = FrameFacade.parse(orf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assert.assertNull(ff.asCelestialBodyFrame());
            Assert.assertSame(orf, ff.asOrbitRelativeFrame());
            Assert.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapSpacecraft() {
        for (SpacecraftBodyFrame.BaseEquipment be : SpacecraftBodyFrame.BaseEquipment.values()) {
            for (String label : Arrays.asList("1", "2", "A", "B")) {
                SpacecraftBodyFrame sbf = new SpacecraftBodyFrame(be, label);
                FrameFacade ff = FrameFacade.parse(sbf.toString(),
                                                   IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                   true, true, true);
            Assert.assertNull(ff.asCelestialBodyFrame());
            Assert.assertNull(ff.asOrbitRelativeFrame());
            Assert.assertEquals(be,    ff.asSpacecraftBodyFrame().getBaseEquipment());
            Assert.assertEquals(label, ff.asSpacecraftBodyFrame().getLabel());
            }
        }
    }

    @Test
    public void testUnknownFrame() {
        final String name = "unknown";
        FrameFacade ff = FrameFacade.parse(name,
                                           IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                           true, true, true);
        Assert.assertNull(ff.asFrame());
        Assert.assertNull(ff.asCelestialBodyFrame());
        Assert.assertNull(ff.asOrbitRelativeFrame());
        Assert.assertNull(ff.asSpacecraftBodyFrame());
        Assert.assertEquals(name, ff.getName());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
