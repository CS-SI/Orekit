/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class VEISFrameTest {

    @Test
    public void testRefLEO() throws OrekitException {

        AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                              new TimeComponents(07, 51, 28.386009),
                                              TimeScalesFactory.getUTC());

        Transform t0 = FramesFactory.getEME2000().getTransformTo(FramesFactory.getVeis1950(), date0);

        // J2000
        PVCoordinates pvJ2000 =
            new PVCoordinates(new Vector3D(5102509.6000, 6123011.5200, 6378136.3000),
                              new Vector3D(-4743.219600, 790.536600, 5533.756190));

        // Veis (mslib90)
        PVCoordinates pvVEIS =
            new PVCoordinates(new Vector3D(5168161.598034, 6065377.671130, 6380344.532758),
                              new Vector3D(-4736.246465, 843.352600, 5531.931275));

        PVCoordinates delta0 = new PVCoordinates(t0.transformPVCoordinates(pvJ2000), pvVEIS);
        Assert.assertEquals(0.0, delta0.getPosition().getNorm(), 9.0e-4);
        Assert.assertEquals(0.0, delta0.getVelocity().getNorm(), 3.0e-5);

    }

    @Test
    public void testRefGEO() throws OrekitException{

        // this reference test has been extracted from the following paper:
        AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                              TimeComponents.H00,
                                              TimeScalesFactory.getUTC());

        Transform t0 = FramesFactory.getEME2000().getTransformTo(FramesFactory.getVeis1950(), date0);

        //J2000
        PVCoordinates pvJ2000 =
            new PVCoordinates(new Vector3D(-40588150.3620, -11462167.0280, 27147.6490),
                              new Vector3D(834.787457, -2958.305691, -1.173016));

        // VEIS (mslib90)
        PVCoordinates pvVEIS =
            new PVCoordinates(new Vector3D(-40713785.134055, -11007613.451052, 10293.258344),
                              new Vector3D(801.657321, -2967.454926, -0.928881));

        PVCoordinates delta0 = new PVCoordinates(t0.transformPVCoordinates(pvJ2000), pvVEIS);
        Assert.assertEquals(0.0, delta0.getPosition().getNorm(), 2.5e-3);
        Assert.assertEquals(0.0, delta0.getVelocity().getNorm(), 1.1e-4);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
