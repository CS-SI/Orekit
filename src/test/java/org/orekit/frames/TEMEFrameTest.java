/* Copyright 2002-2010 CS Communication & Systèmes
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


import org.apache.commons.math.geometry.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class TEMEFrameTest {

    @Test
    public void testValladoTEMEofDate() throws OrekitException {

        // this reference test has been extracted from Vallado's book:
        // Fundamentals of Astrodynamics and Applications
        // David A. Vallado, Space Technology Library, 2007
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2000, 182),
                                           new TimeComponents(0.78495062 * Constants.JULIAN_DAY),
                                           TimeScalesFactory.getUTC());

        // TEME
        PVCoordinates pvTEME =
           new PVCoordinates(new Vector3D(-9060473.73569, 4658709.52502, 813686.73153),
                             new Vector3D(-2232.832783, -4110.453490, -3157.345433));

        // EME2000
        PVCoordinates pvEME2000 =
            new PVCoordinates(new Vector3D(-9059941.3786, 4659697.2000, 813958.8875),
                              new Vector3D(-2233.348094, -4110.136162, -3157.394074));
        
        Transform t = FramesFactory.getTEME().getTransformTo(FramesFactory.getEME2000(), t0);

        PVCoordinates delta = new PVCoordinates(t.transformPVCoordinates(pvTEME), pvEME2000);
      
        Assert.assertEquals(0.0, delta.getPosition().getNorm(), 1.2);
        Assert.assertEquals(0.0, delta.getVelocity().getNorm(), 4.0e-4);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
