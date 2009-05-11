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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class MEMEFrameTest extends TestCase {

    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           UTCScale.getInstance());

        Transform t = Frame.getGCRF().getTransformTo(Frame.getMEME(), t0);

        //GCRF iau76 w corr
        PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4038, 6378136.9252),
                              new Vector3D(-4743.220156, 790.536497, 5533.755728));

        //MOD iau76 w corr
        PVCoordinates pvMEME =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                              new Vector3D(-4746.263052, 786.014045, 5531.790562));

        checkPV(pvMEME, t.transformPVCoordinates(pvGCRFiau76), 4.4e-3, 1.6e-6);

    }

    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           UTCScale.getInstance());

        Transform t = Frame.getGCRF().getTransformTo(Frame.getMEME(), t0);

        //GCRF iau76 w corr
        PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(-40588150.3649, -11462167.0282, 27143.2028),
                              new Vector3D(834.787457, -2958.305691, -1.172994));

        //MOD iau76 w corr
        PVCoordinates pvMEME =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                              new Vector3D(837.708020, -2957.480117, -0.814253));

        checkPV(pvMEME, t.transformPVCoordinates(pvGCRFiau76), 2.5e-5, 6.9e-7);

    }

    public void setUp() {
        String root = getClass().getClassLoader().getResource("compressed-data").getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
    }

    private void checkPV(PVCoordinates reference,
                         PVCoordinates result,
                         double positionThreshold,
                         double velocityThreshold) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        assertEquals(0, dP.getNorm(), positionThreshold);
        assertEquals(0, dV.getNorm(), velocityThreshold);
    }

    public static Test suite() {
        return new TestSuite(MEMEFrameTest.class);
    }

}
