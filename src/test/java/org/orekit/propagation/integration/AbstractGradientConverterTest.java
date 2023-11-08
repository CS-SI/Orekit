/* Copyright 2022-2023 Romain Serra
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.*;

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
        final SpacecraftState mockedState = mockState(isOrbitDefined, frame);
        final AttitudeProvider provider = new FrameAlignedProvider(frame);
        // WHEN
        final FieldSpacecraftState<Gradient> fieldState = AbstractGradientConverter
                .buildBasicGradientSpacecraftState(mockedState, freeParameters, provider);
        // THEN
        Assertions.assertEquals(mockedState.getFrame(), fieldState.getFrame());
        Assertions.assertEquals(mockedState.getDate(), fieldState.getDate().toAbsoluteDate());
        Assertions.assertEquals(mockedState.getPosition(), fieldState.getPosition().toVector3D());
        Assertions.assertEquals(mockedState.isOrbitDefined(), fieldState.isOrbitDefined());
        if (isOrbitDefined) {
            Assertions.assertEquals(mockedState.getMu(), fieldState.getMu().getReal());
        }
        final Gradient expectedFieldZero = Gradient.constant(freeParameters, 0.);
        Assertions.assertEquals(expectedFieldZero, fieldState.getDate().getField().getZero());
        final Gradient expectedFieldMass = Gradient.constant(freeParameters, mockedState.getMass());
        Assertions.assertEquals(expectedFieldMass, fieldState.getMass());
    }

    private SpacecraftState mockState(final boolean isOrbitDefined, final Frame frame) {
        final SpacecraftState state = Mockito.mock(SpacecraftState.class);
        Mockito.when(state.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(state.getDate(),
                new PVCoordinates());
        Mockito.when(state.getPVCoordinates()).thenReturn(pvCoordinates);
        Mockito.when(state.getPosition()).thenReturn(pvCoordinates.getPosition());
        Mockito.when(state.isOrbitDefined()).thenReturn(isOrbitDefined);
        Mockito.when(state.getMass()).thenReturn(1000.);
        Mockito.when(state.getFrame()).thenReturn(frame);
        if (isOrbitDefined) {
            Mockito.when(state.getMu()).thenReturn(Constants.EGM96_EARTH_MU);
        }
        return state;
    }

    private FieldSpacecraftState<Gradient> createGradientState(final boolean isOrbitDefined,
                                                               final int freeParameters) {
        final Frame frame = FramesFactory.getGCRF();
        final AttitudeProvider provider = new FrameAlignedProvider(frame);
        final SpacecraftState mockedState = mockState(isOrbitDefined, frame);
        return AbstractGradientConverter.buildBasicGradientSpacecraftState(mockedState, freeParameters, provider);
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