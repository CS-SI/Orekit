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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.utils.Constants;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapiroModelTest {

    @Test
    void testComputeDelaySymmetry() {
        // GIVEN
        final ShapiroModel shapiroModel = new ShapiroModel(1.);
        final Vector3D position1 = new Vector3D(1., 2., 3.);
        final Vector3D position2 = new Vector3D(4., 5., 6.);
        // WHEN
        final double delay = shapiroModel.computeDelay(position1, position2);
        // THEN
        assertEquals(shapiroModel.computeDelay(position2, position1), delay);
    }

    @Test
    void testComputeEquivalentRange() {
        // GIVEN
        final ShapiroModel shapiroModel = new ShapiroModel(1.);
        final Vector3D position1 = new Vector3D(1., 2., 3.);
        final Vector3D position2 = new Vector3D(4., 5., 6.);
        // WHEN
        final double delay = shapiroModel.computeEquivalentRange(position1, position2);
        // THEN
        assertEquals(shapiroModel.computeDelay(position1, position2) * Constants.SPEED_OF_LIGHT, delay);
    }

    @Test
    void testComputeEquivalentRangeField() {
        // GIVEN
        final ShapiroModel shapiroModel = new ShapiroModel(1.);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldVector3D<Binary64> position1 = new FieldVector3D<>(field, new Vector3D(1., 2., 3.));
        final FieldVector3D<Binary64> position2 = new FieldVector3D<>(field, new Vector3D(4., 5., 6.));
        // WHEN
        final Binary64 distance = shapiroModel.computeEquivalentRange(position1, position2);
        // THEN
        assertEquals(shapiroModel.computeEquivalentRange(position1.toVector3D(), position2.toVector3D()), distance.getReal());
    }

    @Test
    void testComputeDelayField() {
        // GIVEN
        final ShapiroModel shapiroModel = new ShapiroModel(1.);
        final GradientField field = GradientField.getField(1);
        final FieldVector3D<Gradient> position1 = new FieldVector3D<>(new Gradient(1., 1.), field.getZero(), field.getZero());
        final FieldVector3D<Gradient> position2 = new FieldVector3D<>(field, new Vector3D(4., 5., 6.));
        // WHEN
        final Gradient delay = shapiroModel.computeDelay(position1, position2);
        // THEN
        assertEquals(shapiroModel.computeEquivalentRange(position1, position2).divide(Constants.SPEED_OF_LIGHT), delay);
    }
}
