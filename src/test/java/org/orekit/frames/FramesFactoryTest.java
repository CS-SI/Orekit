/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.frames;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;

public class FramesFactoryTest {

    @Test
    public void testTreeRoot() throws OrekitException {
        Assert.assertNull(FramesFactory.getFrame(Predefined.GCRF).getParent());
    }

    @Test
    public void testTreeICRF() throws OrekitException {
        Frame icrf = FramesFactory.getFrame(Predefined.ICRF);
        Assert.assertEquals(CelestialBodyFactory.EARTH_MOON + "/inertial", icrf.getParent().getName());
        Assert.assertEquals(Predefined.EME2000.getName(), icrf.getParent().getParent().getName());
    }

    @Test
    public void testTree() throws OrekitException {
        Predefined[][] reference = new Predefined[][] {
            { Predefined.EME2000,                         Predefined.GCRF },
            { Predefined.ITRF_2008_WITHOUT_TIDAL_EFFECTS, Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_2008_WITH_TIDAL_EFFECTS,    Predefined.ITRF_2005_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS, Predefined.TIRF_2000_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_2005_WITH_TIDAL_EFFECTS,    Predefined.TIRF_2000_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS, Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_2000_WITH_TIDAL_EFFECTS,    Predefined.ITRF_2005_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_97_WITHOUT_TIDAL_EFFECTS,   Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_97_WITH_TIDAL_EFFECTS,      Predefined.ITRF_2000_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_93_WITHOUT_TIDAL_EFFECTS,   Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_93_WITH_TIDAL_EFFECTS,      Predefined.ITRF_2000_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_EQUINOX,                    Predefined.GTOD_WITH_EOP_CORRECTIONS },
            { Predefined.TIRF_2000_WITHOUT_TIDAL_EFFECTS, Predefined.CIRF_2000 },
            { Predefined.TIRF_2000_WITH_TIDAL_EFFECTS,    Predefined.CIRF_2000 },
            { Predefined.CIRF_2000,                       Predefined.GCRF },
            { Predefined.VEIS_1950,                       Predefined.GTOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_WITHOUT_EOP_CORRECTIONS,    Predefined.TOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_WITH_EOP_CORRECTIONS,       Predefined.TOD_WITH_EOP_CORRECTIONS },
            { Predefined.TOD_WITHOUT_EOP_CORRECTIONS,     Predefined.MOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.TOD_WITH_EOP_CORRECTIONS,        Predefined.MOD_WITH_EOP_CORRECTIONS },
            { Predefined.MOD_WITHOUT_EOP_CORRECTIONS,     Predefined.EME2000 },
            { Predefined.MOD_WITH_EOP_CORRECTIONS,        Predefined.GCRF },
            { Predefined.TEME,                            Predefined.TOD_WITHOUT_EOP_CORRECTIONS }
        };
        for (final Predefined[] pair : reference) {
            Frame child  = FramesFactory.getFrame(pair[0]);
            Frame parent = FramesFactory.getFrame(pair[1]);
            Assert.assertEquals("wrong parent for " + child.getName(),
                                parent.getName(), child.getParent().getName());
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
