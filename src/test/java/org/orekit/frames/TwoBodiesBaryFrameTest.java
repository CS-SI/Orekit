/* Copyright 2002-2023 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

/**Unit tests for {@link CR3BPRotatingTransformProvider}.
 * @author Vincent Mouraux
 */
public class TwoBodiesBaryFrameTest {

    @Test
    public void testTransformationOrientationForEarthMoon() {

        // Load Bodies
        final CelestialBody barycenter = CelestialBodyFactory.getEarthMoonBarycenter();
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        // Set frames
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame baryFrame = new TwoBodiesBaryFrame(earth,moon);

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute barycenter position and our frame origin in EME2000
        Vector3D truePosBary = barycenter.getPosition(date, eme2000);
        Vector3D posBary   = baryFrame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Moon are aligned as seen from Earth
        Assertions.assertEquals(truePosBary.getX(), posBary.getX(), 1.0e-8);
        Assertions.assertEquals(truePosBary.getY(), posBary.getY(), 1.0e-8);
        Assertions.assertEquals(truePosBary.getZ(), posBary.getZ(), 1.0e-8);
    }

    @Test
    public void testFieldTransformationOrientationForEarthMoon() {
        doTestFieldTransformationOrientationForEarthMoon(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldTransformationOrientationForEarthMoon(final Field<T> field) {

        // Load Bodies
        final CelestialBody barycenter = CelestialBodyFactory.getEarthMoonBarycenter();
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        // Set frames
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame baryFrame = new TwoBodiesBaryFrame(earth,moon);

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  TimeScalesFactory.getUTC());

        // Compute barycenter position and our frame origin in EME2000
        FieldVector3D<T> truePosBary = barycenter.getPosition(date, eme2000);
        FieldVector3D<T> posBary   = baryFrame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Moon are aligned as seen from Earth
        Assertions.assertEquals(truePosBary.getX().getReal(), posBary.getX().getReal(), 1.0e-8);
        Assertions.assertEquals(truePosBary.getY().getReal(), posBary.getY().getReal(), 1.0e-8);
        Assertions.assertEquals(truePosBary.getZ().getReal(), posBary.getZ().getReal(), 1.0e-8);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}