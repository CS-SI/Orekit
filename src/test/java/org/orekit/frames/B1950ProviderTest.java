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


public class B1950ProviderTest {

    @Test
    public void testISS() throws OrekitException {

        // data retrieved from http://obsat.ca/Celements.htm
        Frame b1950 = FramesFactory.getB1950();
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2005, 45),
                                           new TimeComponents(12, 30, 0.0),
                                           TimeScalesFactory.getUTC());

        Transform j2000ToM50 = FramesFactory.getEME2000().getTransformTo(b1950, t0);
        Transform j2000ToTDR = FramesFactory.getEME2000().getTransformTo(FramesFactory.getGTOD(false), t0);

        PVCoordinates pvM50 =
            new PVCoordinates(new Vector3D(6219460.30, 633395.77, -2517564.66),
                              new Vector3D(-2637.503224, 4893.031800, -5313.617822));
        PVCoordinates pvEME2000 =
                new PVCoordinates(new Vector3D(6224145.01, 702971.25, -2487339.70),
                                  new Vector3D(-2666.209536, 4863.377216, -5326.500407));
        PVCoordinates pvTDR =
                new PVCoordinates(new Vector3D(5170403.10, 3537832.81, -2484286.46),
                                  new Vector3D(-4373.891732, 2671.766349, -5327.596635));

        checkPV(pvM50, j2000ToM50.transformPVCoordinates(pvEME2000), 2.5e-5, 6.9e-7);
        checkPV(pvTDR, j2000ToTDR.transformPVCoordinates(pvEME2000), 2.3e-2, 4.3e-5);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference,
                         PVCoordinates result,
                         double positionThreshold,
                         double velocityThreshold) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
