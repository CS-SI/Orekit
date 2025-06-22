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

package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldShiftingPVCoordinatesProviderTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldOrbit<Binary64> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        final TimeStampedFieldPVCoordinates<Binary64> pvCoordinates = fieldOrbit.getPVCoordinates();
        final FieldShiftingPVCoordinatesProvider<Binary64> pvCoordinatesProvider = new FieldShiftingPVCoordinatesProvider<>(pvCoordinates,
                orbit.getFrame());
        final Frame frame = FramesFactory.getEME2000();
        final FieldAbsoluteDate<Binary64> shiftedDate = fieldOrbit.getDate().shiftedBy(1000);
        // WHEN
        final FieldVector3D<Binary64> position = pvCoordinatesProvider.getPosition(shiftedDate, frame);
        // THEN
        final FieldPVCoordinates<Binary64> shiftedPV = pvCoordinatesProvider.getPVCoordinates(shiftedDate, frame);
        final FieldVector3D<Binary64> expectedPosition = shiftedPV.getPosition();
        final double tolerance = 1e-9;
        assertEquals(expectedPosition.getX().getReal(), position.getX().getReal(), tolerance);
        assertEquals(expectedPosition.getY().getReal(), position.getY().getReal(), tolerance);
        assertEquals(expectedPosition.getZ().getReal(), position.getZ().getReal(), tolerance);
    }
}
