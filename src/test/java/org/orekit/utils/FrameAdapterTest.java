/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class FrameAdapterTest {

    @Test
    public void testDouble() {

        final Frame                         eme2000        = FramesFactory.getEME2000();
        final CelestialBody                 moon           = CelestialBodyFactory.getMoon();
        final Frame                         moonFrame      = moon.getBodyOrientedFrame();
        final ExtendedPVCoordinatesProvider moonPVProvider = new FrameAdapter(moonFrame);

        final AbsoluteDate t0 = new AbsoluteDate("2000-01-22T13:30:00", TimeScalesFactory.getUTC());
        double maxP = 0;
        double maxV = 0;
        double maxA = 0;
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            final AbsoluteDate t = t0.shiftedBy(dt);
            final TimeStampedPVCoordinates pvRef     = moon.getPVCoordinates(t, eme2000);
            final TimeStampedPVCoordinates pvAdapted = moonPVProvider.getPVCoordinates(t, eme2000);
            maxP = FastMath.max(maxP, Vector3D.distance(pvRef.getPosition(),     pvAdapted.getPosition()));
            maxV = FastMath.max(maxV, Vector3D.distance(pvRef.getVelocity(),     pvAdapted.getVelocity()));
            maxA = FastMath.max(maxA, Vector3D.distance(pvRef.getAcceleration(), pvAdapted.getAcceleration()));
        }
        Assertions.assertEquals(0.0, maxP, 7.6e-7);
        Assertions.assertEquals(0.0, maxV, 2.9e-12);
        Assertions.assertEquals(0.0, maxA, 1.1e-17);

    }

    @Test
    public void testField() {
        doTestField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestField(final Field<T> field) {

        final Frame                         eme2000        = FramesFactory.getEME2000();
        final CelestialBody                 moon           = CelestialBodyFactory.getMoon();
        final Frame                         moonFrame      = moon.getBodyOrientedFrame();
        final ExtendedPVCoordinatesProvider moonPVProvider = new FrameAdapter(moonFrame);

        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field,
                                                                new AbsoluteDate("2000-01-22T13:30:00",
                                                                                 TimeScalesFactory.getUTC()));
        T maxP = field.getZero();
        T maxV = field.getZero();
        T maxA = field.getZero();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            final FieldAbsoluteDate<T> t = t0.shiftedBy(dt);
            final TimeStampedFieldPVCoordinates<T> pvRef = moon.getPVCoordinates(t, eme2000);
            final TimeStampedFieldPVCoordinates<T> pvAdapted = moonPVProvider.getPVCoordinates(t, eme2000);
            maxP = FastMath.max(maxP, FieldVector3D.distance(pvRef.getPosition(),     pvAdapted.getPosition()));
            maxV = FastMath.max(maxV, FieldVector3D.distance(pvRef.getVelocity(),     pvAdapted.getVelocity()));
            maxA = FastMath.max(maxA, FieldVector3D.distance(pvRef.getAcceleration(), pvAdapted.getAcceleration()));
        }
        Assertions.assertEquals(0.0, maxP.getReal(), 7.6e-7);
        Assertions.assertEquals(0.0, maxV.getReal(), 2.9e-12);
        Assertions.assertEquals(0.0, maxA.getReal(), 1.1e-17);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
