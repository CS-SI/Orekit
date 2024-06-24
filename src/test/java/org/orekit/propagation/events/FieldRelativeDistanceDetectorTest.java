/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.complex.Complex;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldStopOnDecreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

class FieldRelativeDistanceDetectorTest {

    @Test
    void testGetDistanceThreshold() {
        // GIVEN
        final Complex expectedDistanceThreshold = Complex.ONE;
        final FieldRelativeDistanceDetector<Complex> distanceDetector = new FieldRelativeDistanceDetector<>(
                mockProvider(), expectedDistanceThreshold);
        // WHEN
        final Complex actualDistanceThreshold = distanceDetector.getDistanceThreshold();
        // THEN
        Assertions.assertEquals(expectedDistanceThreshold, actualDistanceThreshold);
    }

    @Test
    void testCreate() {
        // GIVEN
        final Complex distanceThreshold = Complex.ONE;
        final FieldRelativeDistanceDetector<Complex> distanceDetector = new FieldRelativeDistanceDetector<>(
                mockProvider(), distanceThreshold);
        final FieldStopOnDecreasing<Complex> expectedHandler = new FieldStopOnDecreasing<>();
        // WHEN
        final FieldRelativeDistanceDetector<Complex> detector = distanceDetector.create(distanceDetector.getMaxCheckInterval(),
                distanceDetector.getThreshold(), distanceDetector.getMaxIterationCount(), expectedHandler);
        // THEN
        Assertions.assertEquals(expectedHandler, detector.getHandler());
    }

    @SuppressWarnings("unchecked")
    private FieldPVCoordinatesProvider<Complex> mockProvider() {
        return Mockito.mock(FieldPVCoordinatesProvider.class);
    }

    @Test
    void testG() {
        // GIVEN
        final CartesianOrbit initialOrbit = createOrbit(new Vector3D(1e7, 0, 0));
        final double distanceThreshold = 0.;
        final Binary64Field field = Binary64Field.getInstance();
        final FieldKeplerianPropagator<Binary64> fieldKeplerianPropagator = new FieldKeplerianPropagator<>(
                new FieldCartesianOrbit<>(field, initialOrbit));
        final FieldRelativeDistanceDetector<Binary64> fieldRelativeDistanceDetector = new FieldRelativeDistanceDetector<>(
                fieldKeplerianPropagator, field.getZero().newInstance(distanceThreshold));
        final CartesianOrbit orbit = createOrbit(new Vector3D(1e7, 1e3, 2e3));
        final FieldCartesianOrbit<Binary64> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        final FieldSpacecraftState<Binary64> fieldSpacecraftState = new FieldSpacecraftState<>(fieldOrbit);
        // WHEN
        final Binary64 g = fieldRelativeDistanceDetector.g(fieldSpacecraftState);
        final double actualDistance = g.getReal();
        // THEN
        final RelativeDistanceDetector relativeDistanceDetector = new RelativeDistanceDetector(
                new KeplerianPropagator(initialOrbit), distanceThreshold);
        final double expectedDistance = relativeDistanceDetector.g(fieldSpacecraftState.toSpacecraftState());
        Assertions.assertEquals(expectedDistance, actualDistance);
    }

    private CartesianOrbit createOrbit(final Vector3D position) {
        final Vector3D velocity = new Vector3D(0., 6e3, -1e2);
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                position, velocity);
        return new CartesianOrbit(pvCoordinates, FramesFactory.getGCRF(), Constants.EGM96_EARTH_MU);
    }

}
