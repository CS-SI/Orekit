/* Copyright 2002-2024 Luc Maisonobe
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
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.*;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class ExtendedPositionProviderAdapterTest {

    @Test
    public void testDouble() {

        final Frame         eme2000   = FramesFactory.getEME2000();
        final CelestialBody moon      = CelestialBodyFactory.getMoon();
        final Frame         moonFrame = new ExtendedPositionProviderAdapter(eme2000, moon, "moon-frame");

        final AbsoluteDate t0 = new AbsoluteDate("2000-01-22T13:30:00", TimeScalesFactory.getUTC());
        double maxP = 0;
        double maxV = 0;
        double maxA = 0;
        double maxR = 0;
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            final AbsoluteDate t = t0.shiftedBy(dt);
            final TimeStampedPVCoordinates pv = moon.getPVCoordinates(t, moonFrame);
            maxP = FastMath.max(maxP, pv.getPosition().getNorm());
            maxV = FastMath.max(maxV, pv.getVelocity().getNorm());
            maxA = FastMath.max(maxA, pv.getAcceleration().getNorm());
            maxR = FastMath.max(maxR, moonFrame.getTransformTo(eme2000, t).getRotation().getAngle());
        }
        Assertions.assertEquals(0.0, maxP, 5.0e-7);
        Assertions.assertEquals(0.0, maxV, 1.1e-12);
        Assertions.assertEquals(0.0, maxA, 2.8e-18);
        Assertions.assertEquals(0.0, maxR, 1.0e-30);

    }

    @Test
    public void testField() {
        doTestField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestField(final Field<T> field) {

        final Frame         eme2000   = FramesFactory.getEME2000();
        final CelestialBody moon      = CelestialBodyFactory.getMoon();
        final Frame         moonFrame = new ExtendedPositionProviderAdapter(eme2000, moon, "moon-frame");

        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field,
                                                                new AbsoluteDate("2000-01-22T13:30:00",
                                                                                 TimeScalesFactory.getUTC()));
        T maxP = field.getZero();
        T maxV = field.getZero();
        T maxA = field.getZero();
        T maxR = field.getZero();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            final FieldAbsoluteDate<T> t = t0.shiftedBy(dt);
            final TimeStampedFieldPVCoordinates<T> pv = moon.getPVCoordinates(t, moonFrame);
            maxP = FastMath.max(maxP, pv.getPosition().getNorm());
            maxV = FastMath.max(maxV, pv.getVelocity().getNorm());
            maxA = FastMath.max(maxA, pv.getAcceleration().getNorm());
            maxR = FastMath.max(maxR, moonFrame.getTransformTo(eme2000, t).getRotation().getAngle());
        }
        Assertions.assertEquals(0.0, maxP.getReal(), 5.0e-7);
        Assertions.assertEquals(0.0, maxV.getReal(), 1.1e-12);
        Assertions.assertEquals(0.0, maxA.getReal(), 2.8e-18);
        Assertions.assertEquals(0.0, maxR.getReal(), 1.0e-30);

    }

    @Test
    void testGetStaticTransform() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final Frame inputFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final ExtendedPositionProviderAdapter adapter = new ExtendedPositionProviderAdapter(inputFrame, provider, "");
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final StaticTransform staticTransform = adapter.getStaticTransformTo(frame, date);
        // THEN
        final Transform transform = adapter.getTransformTo(frame, date);
        Assertions.assertEquals(transform.getTranslation(), staticTransform.getTranslation());
        Assertions.assertEquals(0., Rotation.distance(transform.getRotation(), staticTransform.getRotation()));
    }

    @Test
    void testGetStaticTransformField() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final Frame inputFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final ExtendedPositionProviderAdapter adapter = new ExtendedPositionProviderAdapter(inputFrame, provider, "");
        final Frame frame = FramesFactory.getEME2000();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = new FieldAbsoluteDate<>(field, date);
        // WHEN
        final FieldStaticTransform<Binary64> staticTransform = adapter.getStaticTransformTo(frame, fieldAbsoluteDate);
        // THEN
        final FieldTransform<Binary64> transform = adapter.getTransformTo(frame, fieldAbsoluteDate);
        Assertions.assertEquals(transform.getTranslation(), staticTransform.getTranslation());
        Assertions.assertEquals(0., Rotation.distance(transform.getRotation().toRotation(),
                staticTransform.getRotation().toRotation()));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private static class TestProvider implements ExtendedPositionProvider {

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame) {
            return new FieldVector3D<>(date.getField(), Vector3D.MINUS_I);
        }
    }

}
