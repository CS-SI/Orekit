/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.events;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;

class FieldElevationDetectorTest {

    @Test
    void testWithRefraction() {
        // GIVEN
        final TopocentricFrame frame = new TopocentricFrame(new OneAxisEllipsoid(1, 0, FramesFactory.getGCRF()),
                new GeodeticPoint(0., 0., 0), "");
        final FieldElevationDetector<Binary64> fieldElevationDetector = new FieldElevationDetector<>(Binary64Field.getInstance(), frame);
        final AtmosphericRefractionModel model = Mockito.mock(AtmosphericRefractionModel.class);
        // WHEN
        final FieldElevationDetector<Binary64> detector = fieldElevationDetector.withRefraction(model);
        // THEN
        Assertions.assertEquals(model, detector.getRefractionModel());
    }
}
