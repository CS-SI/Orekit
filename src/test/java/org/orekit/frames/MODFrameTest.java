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


public class MODFrameTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(true), t0);
        //GCRF iau76 w corr
        PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4007, 6378136.9282),
                              new Vector3D(-4743.220157, 790.536497, 5533.755727));
        //MOD iau76 w corr
        PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                              new Vector3D(-4746.263052, 786.014045, 5531.790562));

        checkPV(pvMODiau76Wcorr, tt.transformPVCoordinates(pvGCRFiau76), 2.6e-5, 7.2e-7);

        Transform tf = FramesFactory.getEME2000().getTransformTo(FramesFactory.getMOD(false), t0);
        //J2000 iau76
        PVCoordinates pvJ2000iau76 =
            new PVCoordinates(new Vector3D(5102509.6000, 6123011.5200, 6378136.3000),
                              new Vector3D(-4743.219600, 790.536600, 5533.756190));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                              new Vector3D(-4746.262495, 786.014149, 5531.791025));
        checkPV(pvMODiau76, tf.transformPVCoordinates(pvJ2000iau76), 4.3e-5, 2.7e-7);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(true), t0);
        //GCRF iau76 w corr
        PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(-40588150.3649, -11462167.0282, 27143.2028),
                              new Vector3D(834.787457, -2958.305691, -1.172994));
        //MOD iau76 w corr
        PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                              new Vector3D(837.708020, -2957.480117, -0.814253));
        checkPV(pvMODiau76Wcorr, tt.transformPVCoordinates(pvGCRFiau76), 2.5e-5, 6.9e-7);

        Transform tf = FramesFactory.getEME2000().getTransformTo(FramesFactory.getMOD(false), t0);
        //J2000 iau76
        PVCoordinates pvJ2000iau76 =
            new PVCoordinates(new Vector3D(-40588150.3620, -11462167.0280, 27147.6490),
                              new Vector3D(834.787457, -2958.305691, -1.173016));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                              new Vector3D(837.708020, -2957.480118, -0.814275));
        checkPV(pvMODiau76, tf.transformPVCoordinates(pvJ2000iau76), 3.3e-5, 6.9e-7);

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
