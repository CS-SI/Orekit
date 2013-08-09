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
package org.orekit.frames;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class FramesFactoryTest {

    @Test
    public void testTreeRoot() throws OrekitException {
        Assert.assertNull(FramesFactory.getFrame(Predefined.GCRF).getParent());
    }

    @Test
    public void testTreeICRF() throws OrekitException {
        Frame icrf = FramesFactory.getFrame(Predefined.ICRF);
        Transform t = icrf.getTransformTo(FramesFactory.getGCRF(),
                                          new AbsoluteDate(1969, 6, 25, TimeScalesFactory.getTT()));
        Assert.assertEquals(0.0, t.getRotation().getAngle(), 1.0e-15);
        Assert.assertEquals(CelestialBodyFactory.EARTH_MOON + "/inertial", icrf.getParent().getName());
        Assert.assertEquals(Predefined.GCRF.getName(), icrf.getParent().getParent().getName());
    }

    @Test
    public void testTree() throws OrekitException {
        Predefined[][] reference = new Predefined[][] {
            { Predefined.EME2000,                                     Predefined.GCRF },
            { Predefined.ITRF_2008_WITHOUT_TIDAL_EFFECTS,             Predefined.TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_2008_WITH_TIDAL_EFFECTS,                Predefined.TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS,             Predefined.ITRF_2008_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_2005_WITH_TIDAL_EFFECTS,                Predefined.ITRF_2008_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS,             Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_2000_WITH_TIDAL_EFFECTS,                Predefined.ITRF_2005_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_97_WITHOUT_TIDAL_EFFECTS,               Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_97_WITH_TIDAL_EFFECTS,                  Predefined.ITRF_2000_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_93_WITHOUT_TIDAL_EFFECTS,               Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS },
            { Predefined.ITRF_93_WITH_TIDAL_EFFECTS,                  Predefined.ITRF_2000_WITH_TIDAL_EFFECTS },
            { Predefined.ITRF_EQUINOX_CONVENTIONS_1996,               Predefined.GTOD_CONVENTIONS_1996 },
            { Predefined.ITRF_EQUINOX_CONV_2003,                      Predefined.GTOD_CONVENTIONS_2003 },
            { Predefined.ITRF_EQUINOX_CONV_2010,                      Predefined.GTOD_CONVENTIONS_2010 },
            { Predefined.TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS, Predefined.CIRF_CONVENTIONS_2010 },
            { Predefined.TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS,    Predefined.CIRF_CONVENTIONS_2010 },
            { Predefined.CIRF_CONVENTIONS_2010,                       Predefined.GCRF },
            { Predefined.VEIS_1950,                                   Predefined.GTOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_WITHOUT_EOP_CORRECTIONS,                Predefined.TOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_CONVENTIONS_1996,                       Predefined.TOD_CONVENTIONS_1996 },
            { Predefined.GTOD_CONVENTIONS_2003,                       Predefined.TOD_CONVENTIONS_2003 },
            { Predefined.GTOD_CONVENTIONS_2010,                       Predefined.TOD_CONVENTIONS_2010 },
            { Predefined.TOD_WITHOUT_EOP_CORRECTIONS,                 Predefined.MOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.TOD_CONVENTIONS_1996,                        Predefined.MOD_CONVENTIONS_1996 },
            { Predefined.TOD_CONVENTIONS_2003,                        Predefined.MOD_CONVENTIONS_2003 },
            { Predefined.TOD_CONVENTIONS_2010,                        Predefined.MOD_CONVENTIONS_2010 },
            { Predefined.MOD_WITHOUT_EOP_CORRECTIONS,                 Predefined.EME2000 },
            { Predefined.MOD_CONVENTIONS_1996,                        Predefined.GCRF },
            { Predefined.MOD_CONVENTIONS_2003,                        Predefined.EME2000 },
            { Predefined.MOD_CONVENTIONS_2010,                        Predefined.GCRF },
            { Predefined.TEME,                                        Predefined.TOD_WITHOUT_EOP_CORRECTIONS }
        };
        for (final Predefined[] pair : reference) {
            Frame child  = FramesFactory.getFrame(pair[0]);
            Frame parent = FramesFactory.getFrame(pair[1]);
            Assert.assertEquals("wrong parent for " + child.getName(),
                                parent.getName(), child.getParent().getName());
        }
    }

    @Test
    public void testSerialization()
            throws OrekitException, IOException, ClassNotFoundException {
        for (Predefined predefined : Predefined.values()) {

            Frame original = FramesFactory.getFrame(predefined);
            switch (predefined) {
            case ICRF :
                Assert.assertEquals(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER + "/inertial", original.getName());
                break;
            case ITRF_EQUINOX :
                Assert.assertEquals(Predefined.ITRF_EQUINOX_CONVENTIONS_1996.getName(), original.getName());
                break;
            case TIRF_2000_CONV_2003_WITH_TIDAL_EFFECTS :
                Assert.assertEquals(Predefined.TIRF_CONVENTIONS_2003_WITH_TIDAL_EFFECTS.getName(), original.getName());
                break;
            case TIRF_2000_CONV_2003_WITHOUT_TIDAL_EFFECTS :
                Assert.assertEquals(Predefined.TIRF_CONVENTIONS_2003_WITHOUT_TIDAL_EFFECTS.getName(), original.getName());
                break;
            case TIRF_2000_CONV_2010_WITH_TIDAL_EFFECTS :
                Assert.assertEquals(Predefined.TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS.getName(), original.getName());
                break;
            case TIRF_2000_CONV_2010_WITHOUT_TIDAL_EFFECTS :
                Assert.assertEquals(Predefined.TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS.getName(), original.getName());
                break;
            case CIRF_2000_CONV_2003 :
                Assert.assertEquals(Predefined.CIRF_CONVENTIONS_2003.getName(), original.getName());
                break;
            case CIRF_2000_CONV_2010 :
                Assert.assertEquals(Predefined.CIRF_CONVENTIONS_2010.getName(), original.getName());
                break;
            case GTOD_WITH_EOP_CORRECTIONS :
                Assert.assertEquals(Predefined.GTOD_CONVENTIONS_1996.getName(), original.getName());
                break;
            case TOD_WITH_EOP_CORRECTIONS :
                Assert.assertEquals(Predefined.TOD_CONVENTIONS_1996.getName(), original.getName());
                break;
            case MOD_WITH_EOP_CORRECTIONS :
                Assert.assertEquals(Predefined.MOD_CONVENTIONS_1996.getName(), original.getName());
                break;
            default :
                Assert.assertEquals(predefined.getName(), original.getName());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream    oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
             if (predefined == Predefined.GCRF) {
                Assert.assertTrue(bos.size() >  50);
                Assert.assertTrue(bos.size() < 100);
            } else if (predefined == Predefined.ICRF) {
                Assert.assertTrue(bos.size() > 430);
                Assert.assertTrue(bos.size() < 480);
            } else {
                Assert.assertTrue(bos.size() > 100);
                Assert.assertTrue(bos.size() < 160);
            }

            ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream     ois = new ObjectInputStream(bis);
            Frame deserialized  = (Frame) ois.readObject();
            Assert.assertTrue(original == deserialized);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
