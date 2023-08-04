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
package org.orekit.orbits;

import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.SmoothStepFactory;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

class FieldOrbitBlenderTest {

    // Constants
    final double DEFAULT_MU   = Constants.IERS2010_EARTH_MU;
    final double EARTH_RADIUS = Constants.IERS2010_EARTH_EQUATORIAL_RADIUS;

    final private Field<Binary64> field = Binary64Field.getInstance();
    final private Binary64        one   = field.getOne();

    private FieldOrbit<Binary64> getDefaultOrbitAtDate(final FieldAbsoluteDate<Binary64> date, final Frame inertialFrame) {
        final Vector3D      position = new Vector3D(EARTH_RADIUS + 4000000, 0, 0);
        final Vector3D      velocity = new Vector3D(0, FastMath.sqrt(DEFAULT_MU / position.getNorm()), 0);
        final PVCoordinates pv       = new PVCoordinates(position, velocity);

        return new FieldCartesianOrbit<>(new FieldPVCoordinates<>(field, pv), inertialFrame, date, one.multiply(DEFAULT_MU));
    }

    @Test
    @DisplayName("test blending case with Keplerian dynamic (exact results expected)")
    void testBlendingWithKeplerianDynamic() {
        // Given
        final Frame orbitFrame  = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();

        final FieldAbsoluteDate<Binary64> previousTabulatedDate = new FieldAbsoluteDate<>(field, new AbsoluteDate());
        final FieldAbsoluteDate<Binary64> nextTabulatedDate     = previousTabulatedDate.shiftedBy(3600);
        final FieldAbsoluteDate<Binary64> interpolationDate1    = previousTabulatedDate.shiftedBy(1200);
        final FieldAbsoluteDate<Binary64> interpolationDate2    = previousTabulatedDate.shiftedBy(1500);
        final FieldAbsoluteDate<Binary64> interpolationDate3    = previousTabulatedDate.shiftedBy(2000);

        final FieldOrbit<Binary64> previousTabulatedOrbit =
                getDefaultOrbitAtDate(previousTabulatedDate, orbitFrame);
        final FieldAbstractAnalyticalPropagator<Binary64> propagator =
                new FieldKeplerianPropagator<>(previousTabulatedOrbit);

        final FieldOrbit<Binary64>       nextTabulatedOrbit = propagator.propagate(nextTabulatedDate).getOrbit();
        final List<FieldOrbit<Binary64>> orbitSample        = new ArrayList<>();
        orbitSample.add(previousTabulatedOrbit);
        orbitSample.add(nextTabulatedOrbit);

        final SmoothStepFactory.FieldSmoothStepFunction<Binary64> blendingFunction = SmoothStepFactory.getQuadratic(field);
        final FieldOrbitBlender<Binary64> orbitBlender =
                new FieldOrbitBlender<>(blendingFunction, propagator, outputFrame);

        // When & Then
        final double epsilon = 1e-8; // 10 nm

        OrbitBlenderTest.assertOrbit(propagator.propagate(interpolationDate1).getOrbit().toOrbit(),
                                     orbitBlender.interpolate(interpolationDate1, orbitSample).toOrbit(), epsilon);
        OrbitBlenderTest.assertOrbit(propagator.propagate(interpolationDate2).getOrbit().toOrbit(),
                                     orbitBlender.interpolate(interpolationDate2, orbitSample).toOrbit(), epsilon);
        OrbitBlenderTest.assertOrbit(propagator.propagate(interpolationDate3).getOrbit().toOrbit(),
                                     orbitBlender.interpolate(interpolationDate3, orbitSample).toOrbit(), epsilon);

        Assertions.assertEquals(outputFrame, orbitBlender.getOutputInertialFrame());
    }

    @Test
    @DisplayName("test specific case (blending at tabulated date)")
    void testBlendingAtTabulatedDate() {
        // Given
        final Frame orbitFrame  = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();

        final FieldAbsoluteDate<Binary64> previousTabulatedDate = new FieldAbsoluteDate<>(field, new AbsoluteDate());
        final FieldAbsoluteDate<Binary64> nextTabulatedDate     = previousTabulatedDate.shiftedBy(3600);

        final FieldOrbit<Binary64>       previousTabulatedOrbit = getDefaultOrbitAtDate(previousTabulatedDate, orbitFrame);
        final FieldOrbit<Binary64>       nextTabulatedOrbit     = getDefaultOrbitAtDate(nextTabulatedDate, orbitFrame);
        final List<FieldOrbit<Binary64>> orbitSample            = new ArrayList<>();
        orbitSample.add(previousTabulatedOrbit);
        orbitSample.add(nextTabulatedOrbit);

        final SmoothStepFactory.FieldSmoothStepFunction<Binary64> blendingFunction = SmoothStepFactory.getQuadratic(field);
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> orbitBlender = new FieldOrbitBlender<>(blendingFunction,
                                                                                                           new FieldKeplerianPropagator<>(
                                                                                                                   previousTabulatedOrbit),
                                                                                                           outputFrame);

        // When
        final FieldOrbit<Binary64> blendedOrbit = orbitBlender.interpolate(previousTabulatedDate, orbitSample);

        // Then
        OrbitBlenderTest.assertOrbit(previousTabulatedOrbit.toOrbit(), blendedOrbit.toOrbit(), 1e-11);
    }

    @Test
    @DisplayName("Test error thrown when using non inertial frame")
    void testErrorThrownWhenUsingNonInertialFrame() {
        // Given
        @SuppressWarnings("unchecked")
        final SmoothStepFactory.FieldSmoothStepFunction<Binary64> blendingFunctionMock = Mockito.mock(
                SmoothStepFactory.FieldSmoothStepFunction.class);

        @SuppressWarnings("unchecked")
        final FieldAbstractAnalyticalPropagator<Binary64> propagatorMock =
                Mockito.mock(FieldAbstractAnalyticalPropagator.class);

        final Frame nonInertialFrame = Mockito.mock(Frame.class);
        Mockito.when(nonInertialFrame.isPseudoInertial()).thenReturn(false);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> new FieldOrbitBlender<>(blendingFunctionMock,
                                                                                 propagatorMock,
                                                                                 nonInertialFrame));

        Assertions.assertEquals("non pseudo-inertial frame \"null\"", thrown.getMessage());
    }

}