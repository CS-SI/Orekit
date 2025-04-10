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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

class RelativeDistanceDetectorTest {

    @Test
    void testCreate() {
        // GIVEN
        final double distanceThreshold = 1.;
        final RelativeDistanceDetector distanceDetector = new RelativeDistanceDetector(
                Mockito.mock(PVCoordinatesProvider.class), distanceThreshold);
        final EventHandler expectedHandler = new StopOnIncreasing();
        // WHEN
        final RelativeDistanceDetector detector = distanceDetector.create(distanceDetector.getDetectionSettings(), expectedHandler);
        // THEN
        Assertions.assertEquals(expectedHandler, detector.getHandler());
    }

    @Test
    void testGetDistanceThreshold() {
        // GIVEN
        final double expectedDistanceThreshold = 1.;
        final RelativeDistanceDetector distanceDetector = new RelativeDistanceDetector(
                Mockito.mock(PVCoordinatesProvider.class), expectedDistanceThreshold);
        // WHEN
        final double actualDistanceThreshold = distanceDetector.getDistanceThreshold();
        // THEN
        Assertions.assertEquals(expectedDistanceThreshold, actualDistanceThreshold);
    }

    @Test
    void testG() {
        // GIVEN
        final double zeroDistanceThreshold = 0.;
        final CartesianOrbit initialOrbit = createOrbit();
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final RelativeDistanceDetector distanceDetector = new RelativeDistanceDetector(propagator,
                zeroDistanceThreshold);
        // WHEN
        final double g = distanceDetector.g(new SpacecraftState(initialOrbit));
        // THEN
        Assertions.assertEquals(0., g, 1e-8);
    }

    private CartesianOrbit createOrbit() {
        final Vector3D position = new Vector3D(1e7, 2e3, 1e4);
        final Vector3D velocity = new Vector3D(0., 6e3, -1e2);
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                position, velocity);
        return new CartesianOrbit(pvCoordinates, FramesFactory.getGCRF(), Constants.EGM96_EARTH_MU);
    }

}
