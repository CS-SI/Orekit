/* Copyright 2002-2025 CS GROUP
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
package org.orekit.frames;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class TEMEProviderTest {

    @Test
    public void testValladoTEMEofDate() {

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

        // reference position in EME2000
        // note that Valado's book gives
        //        PVCoordinates pvEME2000Ref =
        //            new PVCoordinates(new Vector3D(-9059941.3786, 4659697.2000, 813958.8875),
        //                              new Vector3D(-2233.348094, -4110.136162, -3157.394074));
        // the values we use here are slightly different, they were computed using
        // Vallado's C++ companion code to the book, using the teme_j2k function with
        // all 106 nutation terms and the 2 corrections elements of the equation of the equinoxes
        PVCoordinates pvEME2000Ref =
            new PVCoordinates(new Vector3D(-9059941.5224999374914, 4659697.1225837596648, 813957.72947647583351),
                              new Vector3D(-2233.3476939179299769, -4110.1362849403413335, -3157.3941963060194738));

        Transform t = FramesFactory.getTEME().getTransformTo(FramesFactory.getEME2000(), t0);

        PVCoordinates pvEME2000Computed = t.transformPVCoordinates(pvTEME);
        PVCoordinates delta = new PVCoordinates(pvEME2000Computed, pvEME2000Ref);
        Assertions.assertEquals(0.0, delta.getPosition().getNorm(), 0.025);
        Assertions.assertEquals(0.0, delta.getVelocity().getNorm(), 1.0e-4);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
