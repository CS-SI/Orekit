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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class ShiftingPVCoordinatesProviderTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final TimeStampedPVCoordinates pvCoordinates = orbit.getPVCoordinates();
        final ShiftingPVCoordinatesProvider pvCoordinatesProvider = new ShiftingPVCoordinatesProvider(pvCoordinates,
                orbit.getFrame());
        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate shiftedDate = orbit.getDate().shiftedBy(1000);
        // WHEN
        final Vector3D position = pvCoordinatesProvider.getPosition(shiftedDate, frame);
        // THEN
        final PVCoordinates shiftedPV = pvCoordinatesProvider.getPVCoordinates(shiftedDate, frame);
        assertArrayEquals(shiftedPV.getPosition().toArray(), position.toArray(), 1e-9);
    }
}
