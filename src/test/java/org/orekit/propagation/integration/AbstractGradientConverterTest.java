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
package org.orekit.propagation.integration;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

class AbstractGradientConverterTest {

    @Test
    void getStateOrbitTest() {
        templateGetStateTest(true);
    }

    @Test
    void getStateAPVTest() {
        templateGetStateTest(false);
    }

    private void templateGetStateTest(final boolean isOrbitDefined) {
        // GIVEN
        final int freeParameters = 6;
        final FieldSpacecraftState<Gradient> initGradientState = createGradientState(isOrbitDefined, freeParameters);
        final TestGradientConverter testGradientConverter = new TestGradientConverter(freeParameters);
        testGradientConverter.initStates(initGradientState);
        final ParameterDriversProvider mockedDriversProvider = mockParameterDriversProvider();
        // WHEN
        final FieldSpacecraftState<Gradient> actualGradientState = testGradientConverter.getState(mockedDriversProvider);
        // THEN
        Assertions.assertEquals(isOrbitDefined, actualGradientState.isOrbitDefined());
        Assertions.assertEquals(initGradientState.getFrame(), actualGradientState.getFrame());
        Assertions.assertEquals(initGradientState.getMass().getReal(), actualGradientState.getMass().getReal());
        Assertions.assertEquals(initGradientState.getDate().toAbsoluteDate(),
                actualGradientState.getDate().toAbsoluteDate());
        Assertions.assertEquals(initGradientState.getPosition().toVector3D(),
                actualGradientState.getPosition().toVector3D());
        Assertions.assertTrue(freeParameters < actualGradientState.getMass().getGradient().length);
    }

    @Test
    void testBuildBasicGradientSpacecraftStateOrbit() {
        templateTestBuildBasicGradientSpacecraftState(true);
    }

    @Test
    void testBuildBasicGradientSpacecraftStateAPV() {
        templateTestBuildBasicGradientSpacecraftState(false);
    }

    private void templateTestBuildBasicGradientSpacecraftState(final boolean isOrbitDefined) {
        // GIVEN
        final int freeParameters = 6;
        final Frame frame = FramesFactory.getGCRF();
        final SpacecraftState state = buildState(isOrbitDefined, frame);
        final AttitudeProvider provider = new FrameAlignedProvider(frame);
        // WHEN
        final FieldSpacecraftState<Gradient> fieldState = AbstractGradientConverter
                .buildBasicGradientSpacecraftState(state, freeParameters, provider);
        // THEN
        Assertions.assertEquals(state.getFrame(), fieldState.getFrame());
        Assertions.assertEquals(state.getDate(), fieldState.getDate().toAbsoluteDate());
        Assertions.assertEquals(state.getPosition(), fieldState.getPosition().toVector3D());
        Assertions.assertEquals(state.isOrbitDefined(), fieldState.isOrbitDefined());
        if (isOrbitDefined) {
            Assertions.assertEquals(state.getOrbit().getMu(), fieldState.getOrbit().getMu().getReal());
        }
        final Gradient expectedFieldZero = Gradient.constant(freeParameters, 0.);
        Assertions.assertEquals(expectedFieldZero, fieldState.getDate().getField().getZero());
        final Gradient expectedFieldMass = Gradient.constant(freeParameters, state.getMass());
        Assertions.assertEquals(expectedFieldMass, fieldState.getMass());
    }

    private SpacecraftState buildState(final boolean isOrbitDefined, final Frame frame) {
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        if (isOrbitDefined) {
            final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(date, new Vector3D(1e7, 0, 0), new Vector3D(0, 7e3, 1));
            return new SpacecraftState(new CartesianOrbit(pvCoordinates, frame, Constants.EGM96_EARTH_MU)).withMass(1000.);
        }
        final SpacecraftState state = buildState(true, frame);
        return new SpacecraftState(new AbsolutePVCoordinates(state.getFrame(), state.getDate(), state.getPVCoordinates()))
                .withMass(state.getMass()).withAttitude(state.getAttitude());
    }

    private FieldSpacecraftState<Gradient> createGradientState(final boolean isOrbitDefined,
                                                               final int freeParameters) {
        final Frame frame = FramesFactory.getGCRF();
        final AttitudeProvider provider = new FrameAlignedProvider(frame);
        final SpacecraftState state = buildState(isOrbitDefined, frame);
        return AbstractGradientConverter.buildBasicGradientSpacecraftState(state, freeParameters, provider);
    }

    private ParameterDriversProvider mockParameterDriversProvider() {
        final int nonZeroNumberOfValues = 1;
        final ParameterDriver mockedParameterDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(mockedParameterDriver.isSelected()).thenReturn(true);
        Mockito.when(mockedParameterDriver.getNbOfValues()).thenReturn(nonZeroNumberOfValues);
        final ParameterDriversProvider mockedDriversProvider = Mockito.mock(ParameterDriversProvider.class);
        final List<ParameterDriver> parameterDriverList = new ArrayList<>();
        parameterDriverList.add(mockedParameterDriver);
        Mockito.when(mockedDriversProvider.getParametersDrivers()).thenReturn(parameterDriverList);
        return mockedDriversProvider;
    }

    private static class TestGradientConverter extends AbstractGradientConverter{

        protected TestGradientConverter(int freeStateParameters) {
            super(freeStateParameters);
        }

    }

}
