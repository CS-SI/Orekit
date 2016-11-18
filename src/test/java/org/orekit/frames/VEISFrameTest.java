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
package org.orekit.frames;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
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

        // the following result were obtained using this code in mslib Fortran library
        // model         =  pm_lieske_wahr
        // jul1950%jour  =  19819                ! 2004-04-06
        // jul1950%sec   =  28288.386009_pm_reel ! 07:51:28.386009
        // delta_tu1     =  0.0_pm_reel          ! 0.0 dtu1 as here we don't use EOP corrections
        // delta_tai     =  32.0_pm_reel         ! TAI - UTC
        // pos_J2000(1)  = 5102509.6000_pm_reel
        // pos_J2000(2)  = 6123011.5200_pm_reel
        // pos_J2000(3)  = 6378136.3000_pm_reel
        // vit_J2000(1)  = -4743.219600_pm_reel
        // vit_J2000(2)  =   790.536600_pm_reel
        // vit_J2000(3)  =  5533.756190_pm_reel
        // call mr_J2000_veis (model, jul1950, delta_tu1, delta_tai, pos_J2000, pos_veis, code_retour, &
        //                     vit_J2000, vit_veis, jacob )
        PVCoordinates pvVEIS =
            new PVCoordinates(new Vector3D(5168161.5980523797, 6065377.6711138152, 6380344.5327578690),
                              new Vector3D(-4736.2464648667, 843.3525998501, 5531.9312750395));
        PVCoordinates delta0 = new PVCoordinates(t0.transformPVCoordinates(pvJ2000), pvVEIS);
        Assert.assertEquals(0.0, delta0.getPosition().getNorm(), 7.0e-4);
        Assert.assertEquals(0.0, delta0.getVelocity().getNorm(), 8.0e-5);

    }

    @Test
    public void testRefGEO() throws OrekitException{

        AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                              TimeComponents.H00,
                                              TimeScalesFactory.getUTC());

        Transform t0 = FramesFactory.getEME2000().getTransformTo(FramesFactory.getVeis1950(), date0);

        //J2000
        PVCoordinates pvJ2000 =
            new PVCoordinates(new Vector3D(-40588150.3620, -11462167.0280, 27147.6490),
                              new Vector3D(834.787457, -2958.305691, -1.173016));

        // the following result were obtained using this code in mslib Fortran library
        // model         =  pm_lieske_wahr
        // jul1950%jour  =  19875                ! 2004-06-01
        // jul1950%sec   =  0.0_pm_reel          ! 00:00:00.000
        // delta_tu1     =  0.0_pm_reel          ! 0.0 dtu1 as here we don't use EOP corrections
        // delta_tai     =  32.0_pm_reel
        // pos_J2000(1)  = -40588150.3620_pm_reel
        // pos_J2000(2)  = -11462167.0280_pm_reel
        // pos_J2000(3)  =     27147.6490_pm_reel
        // vit_J2000(1)  =       834.787457_pm_reel
        // vit_J2000(2)  =     -2958.305691_pm_reel
        // vit_J2000(3)  =        -1.173016_pm_reel
        PVCoordinates pvVEIS =
            new PVCoordinates(new Vector3D(-40713785.1340916604, -11007613.4509160239, 10293.2583441036),
                              new Vector3D(801.6573208750, -2967.4549256851, -0.9288811067));

        PVCoordinates delta0 = new PVCoordinates(t0.transformPVCoordinates(pvJ2000), pvVEIS);
        Assert.assertEquals(0.0, delta0.getPosition().getNorm(), 4.0e-4);
        Assert.assertEquals(0.0, delta0.getVelocity().getNorm(), 4.0e-4);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
