/* Copyright 2002-2022 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.utils.IERSConventions;

import java.util.Arrays;


public class FacadeFrameTest {

    @Test
    public void testMapCelestial() {
        for (CelestialBodyFrame cbf : CelestialBodyFrame.values()) {
            FrameFacade ff = FrameFacade.parse(cbf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assertions.assertSame(cbf, ff.asCelestialBodyFrame());
            Assertions.assertNull(ff.asOrbitRelativeFrame());
            Assertions.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapLOF() {
        for (OrbitRelativeFrame orf : OrbitRelativeFrame.values()) {
            FrameFacade ff = FrameFacade.parse(orf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assertions.assertNull(ff.asCelestialBodyFrame());
            Assertions.assertSame(orf, ff.asOrbitRelativeFrame());
            Assertions.assertNull(ff.asSpacecraftBodyFrame());
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
            Assertions.assertNull(ff.asCelestialBodyFrame());
            Assertions.assertNull(ff.asOrbitRelativeFrame());
            Assertions.assertEquals(be,    ff.asSpacecraftBodyFrame().getBaseEquipment());
            Assertions.assertEquals(label, ff.asSpacecraftBodyFrame().getLabel());
            }
        }
    }

    @Test
    public void testUnknownFrame() {
        final String name = "unknown";
        FrameFacade ff = FrameFacade.parse(name,
                                           IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                           true, true, true);
        Assertions.assertNull(ff.asFrame());
        Assertions.assertNull(ff.asCelestialBodyFrame());
        Assertions.assertNull(ff.asOrbitRelativeFrame());
        Assertions.assertNull(ff.asSpacecraftBodyFrame());
        Assertions.assertEquals(name, ff.getName());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
