/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements;

import java.util.HashMap;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EstimatedEarthFrameProviderTest {

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetTransformGradient() {
        // GIVEN
        final EstimatedEarthFrameProvider provider = new EstimatedEarthFrameProvider(TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        initializeProvider(provider, date);
        final int freeParameters = 0;
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(freeParameters), date);
        // WHEN
        final FieldTransform<Gradient> transform = provider.getTransform(fieldDate);
        // THEN
        final FieldTransform<Gradient> transformGradient = provider.getTransform(fieldDate, freeParameters, new HashMap<>());
        assertEquals(transformGradient.getDate(), transform.getDate());
        assertEquals(transformGradient.getTranslation(), transform.getTranslation());
        assertEquals(0., Rotation.distance(transformGradient.getRotation().toRotation(), transform.getRotation().toRotation()), 1.e-13);
        assertEquals(transformGradient.getRotationRate(), transform.getRotationRate());
        assertEquals(transformGradient.getRotationAcceleration(), transform.getRotationAcceleration());
    }

    @Test
    void testGetStaticTransform() {
        // GIVEN
        final EstimatedEarthFrameProvider provider = new EstimatedEarthFrameProvider(TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        initializeProvider(provider, date);
        // WHEN
        final StaticTransform staticTransform = provider.getStaticTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(staticTransform.getDate(), transform.getDate());
        assertEquals(staticTransform.getTranslation(), transform.getTranslation());
        assertEquals(0., Rotation.distance(staticTransform.getRotation(), transform.getRotation()), 1.e-13);
    }

    @Test
    void testGetStaticTransformField() {
        // GIVEN
        final EstimatedEarthFrameProvider provider = new EstimatedEarthFrameProvider(TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        initializeProvider(provider, date);
        final int freeParameters = 1;
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(freeParameters), date)
                .shiftedBy(new Gradient(1., 1.));
        // WHEN
        final FieldStaticTransform<Gradient> staticTransform = provider.getStaticTransform(fieldDate);
        // THEN
        final FieldTransform<Gradient> transform = provider.getTransform(fieldDate);
        assertEquals(staticTransform.getDate(), transform.getDate());
        assertEquals(staticTransform.getTranslation(), transform.getTranslation());
        assertEquals(0., Rotation.distance(staticTransform.getRotation().toRotation(), transform.getRotation().toRotation()), 1.e-13);
    }

    private void initializeProvider(final EstimatedEarthFrameProvider provider, final AbsoluteDate date) {
        provider.getPolarDriftXDriver().setReferenceDate(date);
        provider.getPolarDriftYDriver().setReferenceDate(date);
        provider.getPolarOffsetXDriver().setReferenceDate(date);
        provider.getPolarOffsetYDriver().setReferenceDate(date);
        provider.getPrimeMeridianDriftDriver().setReferenceDate(date);
        provider.getPrimeMeridianOffsetDriver().setReferenceDate(date);
    }
}
