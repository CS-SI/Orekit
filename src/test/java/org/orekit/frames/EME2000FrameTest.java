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

import static org.junit.Assert.assertEquals;

import org.apache.commons.math.geometry.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class EME2000FrameTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), t0);

        PVCoordinates pvGcrfIau2000A =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4038, 6378136.9252),
                              new Vector3D(-4743.220156, 790.536497, 5533.755728));
        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9758, 6378136.3118),
                              new Vector3D(-4743.219766, 790.536344, 5533.756084));
        checkPV(pvEME2000EqA, t.transformPVCoordinates(pvGcrfIau2000A), 1.1e-4, 2.6e-7);

        PVCoordinates pvGcrfIau2000B =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4012, 6378136.9277),
                              new Vector3D(-4743.220156, 790.536495, 5533.755729));
        PVCoordinates pvEME2000EqB =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9733, 6378136.3142),
                              new Vector3D(-4743.219766, 790.536342, 5533.756085));
        checkPV(pvEME2000EqB, t.transformPVCoordinates(pvGcrfIau2000B), 7.4e-5, 2.6e-7);

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

        Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), t0);

        PVCoordinates pvGCRFiau2000A =
            new PVCoordinates(new Vector3D(-40588150.3617, -11462167.0397, 27143.1974),
                              new Vector3D(834.787458, -2958.305691, -1.172993));
        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(-40588149.5482, -11462169.9118, 27146.8462),
                              new Vector3D(834.787667, -2958.305632, -1.172963));
        checkPV(pvEME2000EqA, t.transformPVCoordinates(pvGCRFiau2000A), 5.8e-5, 6.4e-7);

        PVCoordinates pvGCRFiau2000B =
            new PVCoordinates(new Vector3D(-40588150.3617,-11462167.0397, 27143.2125),
                              new Vector3D(834.787458,-2958.305691,-1.172999));
        PVCoordinates pvEME2000EqB =
            new PVCoordinates(new Vector3D(-40588149.5481, -11462169.9118, 27146.8613),
                              new Vector3D(834.787667, -2958.305632, -1.172968));
        checkPV(pvEME2000EqB, t.transformPVCoordinates(pvGCRFiau2000B), 1.1e-4, 5.6e-7);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference,
                         PVCoordinates result, double positionThreshold,
                         double velocityThreshold) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        assertEquals(0, dP.getNorm(), positionThreshold);
        assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
