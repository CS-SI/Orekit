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
package org.orekit.forces.maneuvers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.*;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.propulsion.ThrustPropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.List;

class ManeuverTest {

    @Test
    void testGetName() {
        // GIVEN
        final String tooShortName = "a";
        final String expectedName = "aa";
        final double arbitraryDuration = 0.;
        final DateBasedManeuverTriggers triggers = new DateBasedManeuverTriggers(tooShortName, AbsoluteDate.ARBITRARY_EPOCH, arbitraryDuration);
        final double arbitraryThrust = 1.;
        final double arbitraryIsp = 1.;
        final PropulsionModel propulsion = new BasicConstantThrustPropulsionModel(arbitraryThrust, arbitraryIsp, Vector3D.PLUS_I, expectedName);
        // WHEN
        final Maneuver maneuver = new Maneuver(null, triggers, propulsion);
        final String actualName = maneuver.getName();
        // THEN
        Assertions.assertEquals(expectedName, actualName);
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final ManeuverTriggers mockedTriggers = Mockito.mock(ManeuverTriggers.class);
        final PropulsionModel mockedPropulsion = Mockito.mock(PropulsionModel.class);
        final AttitudeProvider mocedkAttitudeProvider = Mockito.mock(AttitudeProvider.class);
        final List<ParameterDriver> driverList = new ArrayList<>();
        driverList.add(Mockito.mock(ParameterDriver.class));
        Mockito.when(mocedkAttitudeProvider.getParametersDrivers()).thenReturn(driverList);
        // WHEN
        final Maneuver maneuver = new Maneuver(mocedkAttitudeProvider, mockedTriggers, mockedPropulsion);
        final List<ParameterDriver> actualDrivers = maneuver.getParametersDrivers();
        // THEN
        Assertions.assertEquals(driverList.size(), actualDrivers.size());
    }

    @Test
    void testGetMassDerivativeNotFiring() {
        // GIVEN
        final ManeuverTriggers mockedTriggers = Mockito.mock(ManeuverTriggers.class);
        final double[] array = new double[0];
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        Mockito.when(mockedTriggers.isFiring(date, array)).thenReturn(false);
        final PropulsionModel mockedPropulsion = Mockito.mock(PropulsionModel.class);
        final SpacecraftState mockedState = Mockito.mock();
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        final Maneuver maneuver = new Maneuver(null, mockedTriggers, mockedPropulsion);
        final double rate = maneuver.getMassDerivative(mockedState, array);
        // THEN
        Assertions.assertEquals(0, rate);
    }

    @Test
    void testFieldGetMassDerivativeNotFiring() {
        // GIVEN
        final ManeuverTriggers mockedTriggers = Mockito.mock(ManeuverTriggers.class);
        final Binary64[] array = new Binary64[0];
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        Mockito.when(mockedTriggers.isFiring(date, array)).thenReturn(false);
        final PropulsionModel mockedPropulsion = Mockito.mock(PropulsionModel.class);
        @SuppressWarnings("unchecked")
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock();
        Mockito.when(mockedState.getDate()).thenReturn(date);
        Mockito.when(mockedState.getMass()).thenReturn(Binary64.ONE);
        // WHEN
        final Maneuver maneuver = new Maneuver(null, mockedTriggers, mockedPropulsion);
        final Binary64 rate = maneuver.getMassDerivative(mockedState, array);
        // THEN
        Assertions.assertEquals(Binary64.ZERO, rate);
    }

    @Test
    void testGetControl3DVectorCostType() {
        // GIVEN
        final ManeuverTriggers mockedTriggers = Mockito.mock(ManeuverTriggers.class);
        final PropulsionModel mockedPropulsion = Mockito.mock(PropulsionModel.class);
        Mockito.when(mockedPropulsion.getControl3DVectorCostType()).thenReturn(Control3DVectorCostType.TWO_NORM);
        // WHEN
        final Maneuver maneuver = new Maneuver(null, mockedTriggers, mockedPropulsion);
        final Control3DVectorCostType actualCostType = maneuver.getControl3DVectorCostType();
        // THEN
        final Control3DVectorCostType expectedCostType = mockedPropulsion.getControl3DVectorCostType();
        Assertions.assertEquals(expectedCostType, actualCostType);
    }

    @Test
    @DisplayName("Test comparing Field acceleration w/ and w/o attitude override.")
    @SuppressWarnings("unchecked")
    void testAccelerationFieldAttitudeOverride() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();

        // mock triggers
        final ManeuverTriggers mockedTriggers = Mockito.mock(ManeuverTriggers.class);
        Mockito.when(mockedTriggers.isFiring(Mockito.any(AbsoluteDate.class), Mockito.any(double[].class))).
                thenReturn(true);
        Mockito.when(mockedTriggers.isFiring(Mockito.any(FieldAbsoluteDate.class), Mockito.any(Complex[].class))).
                thenReturn(true);
        Mockito.when(mockedTriggers.getParametersDrivers()).thenReturn(new ArrayList<>());

        // mock propulsion model
        final ThrustPropulsionModel mockedPropulsion = Mockito.mock(ThrustPropulsionModel.class);
        final Vector3D returnedVector = Vector3D.MINUS_I;
        final FieldVector3D<Complex> returnedFieldVector = new FieldVector3D<>(field, returnedVector);
        Mockito.when(mockedPropulsion.getAcceleration(Mockito.any(SpacecraftState.class), Mockito.any(Attitude.class),
                Mockito.any(double[].class))).thenReturn(returnedVector);
        Mockito.when(mockedPropulsion.getAcceleration(Mockito.any(FieldSpacecraftState.class), Mockito.any(FieldAttitude.class),
                Mockito.any(Complex[].class))).thenReturn(returnedFieldVector);
        Mockito.when(mockedPropulsion.getParametersDrivers()).thenReturn(new ArrayList<>());

        // create FieldSpacecraftState
        final int arbitraryNumber = 6;
        final AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final FieldSpacecraftState<Complex> fieldState = createFieldState(field, frame, epoch);

        // create Maneuver
        final FrameAlignedProvider attitudeOverride = new FrameAlignedProvider(frame);
        final Maneuver maneuverWithOverride = new Maneuver(attitudeOverride, mockedTriggers, mockedPropulsion);
        final Complex[] fieldParameters = MathArrays.buildArray(field, arbitraryNumber);
        
        // WHEN
        final FieldVector3D<Complex> actualAcceleration = maneuverWithOverride.acceleration(fieldState, fieldParameters);
        
        // THEN
        final Maneuver maneuverWithoutOverride = new Maneuver(null, mockedTriggers, mockedPropulsion);
        Assertions.assertNotEquals(0., actualAcceleration.toVector3D().getNorm());
        final Vector3D expectedAcceleration = maneuverWithoutOverride.acceleration(fieldState.toSpacecraftState(),
                new double[fieldParameters.length]);
        Assertions.assertEquals(expectedAcceleration, actualAcceleration.toVector3D());
        Assertions.assertEquals(returnedVector, actualAcceleration.toVector3D());
    }

    @Test
    @DisplayName("Test comparing Field acceleration to standard one.")
    @SuppressWarnings("unchecked")
    void testAccelerationField() {
        // GIVEN
        final GradientField field = GradientField.getField(1);

        // mock triggers
        final ManeuverTriggers mockedTriggers = Mockito.mock(ManeuverTriggers.class);
        Mockito.when(mockedTriggers.isFiring(Mockito.any(AbsoluteDate.class), Mockito.any(double[].class))).
                thenReturn(true);
        Mockito.when(mockedTriggers.isFiring(Mockito.any(FieldAbsoluteDate.class), Mockito.any(Gradient[].class))).
                thenReturn(true);
        Mockito.when(mockedTriggers.getParametersDrivers()).thenReturn(new ArrayList<>());

        // mock propulsion model
        final ThrustPropulsionModel mockedPropulsion = Mockito.mock(ThrustPropulsionModel.class);
        final Vector3D returnedVector = Vector3D.MINUS_I;
        final FieldVector3D<Gradient> returnedFieldVector = new FieldVector3D<>(field, returnedVector);
        Mockito.when(mockedPropulsion.getAcceleration(Mockito.any(SpacecraftState.class), Mockito.any(Attitude.class),
                        Mockito.any(double[].class))).thenReturn(returnedVector);
        Mockito.when(mockedPropulsion.getAcceleration(Mockito.any(FieldSpacecraftState.class), Mockito.any(FieldAttitude.class),
                Mockito.any(Gradient[].class))).thenReturn(returnedFieldVector);
        Mockito.when(mockedPropulsion.getParametersDrivers()).thenReturn(new ArrayList<>());

        // create FieldSpacecraftState
        final int arbitraryNumber = 6;
        final AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldSpacecraftState<Gradient> fieldState = createFieldState(field, FramesFactory.getEME2000(), epoch);

        // create Maneuver
        final Maneuver maneuver = new Maneuver(null, mockedTriggers, mockedPropulsion);
        final Gradient[] fieldParameters = MathArrays.buildArray(field, arbitraryNumber);
        
        // WHEN
        final FieldVector3D<Gradient> acceleration = maneuver.acceleration(fieldState, fieldParameters);
        
        // THEN
        final Vector3D expectedAcceleration = maneuver.acceleration(fieldState.toSpacecraftState(), new double[arbitraryNumber]);
        Assertions.assertNotEquals(0., acceleration.toVector3D().getNorm());
        Assertions.assertEquals(expectedAcceleration, acceleration.toVector3D());
    }

    private <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> createFieldState(final Field<T> field, 
                                                                                         final Frame frame, 
                                                                                         final AbsoluteDate epoch) {
        final double arbitraryMu = 1.;
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, frame, epoch, arbitraryMu);
        final SpacecraftState state = new SpacecraftState(orbit);
        return new FieldSpacecraftState<>(field, state);
    }

    @Test
    void testGetAttitudeModelParametersNull() {
        final double[] parameters = new double[] {1};
        final Maneuver maneuver = new Maneuver(null, null, null);
        final double[] actualDrivers = maneuver.getAttitudeModelParameters(parameters);
        Assertions.assertArrayEquals(new double[0], actualDrivers);
    }

    @Test
    void testGetAttitudeModelParameters() {
        final AttitudeRotationModel mockedRotationModel = Mockito.mock(AttitudeRotationModel.class);
        final double[] parameters = new double[] {1};
        Mockito.when(mockedRotationModel.getParameters()).thenReturn(parameters);
        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(Mockito.mock(ParameterDriver.class));
        Mockito.when(mockedRotationModel.getParametersDrivers()).thenReturn(drivers);
        final Maneuver maneuver = new Maneuver(mockedRotationModel, null, null);
        final double[] actualDrivers = maneuver.getAttitudeModelParameters(parameters);
        Assertions.assertArrayEquals(parameters, actualDrivers);
    }

    @Test
    void testGetAttitudeModelParametersFieldNull() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] parameters = MathArrays.buildArray(field, 1);
        parameters[0] = Binary64.ONE;
        final Maneuver maneuver = new Maneuver(null, null, null);
        final Binary64[] actualDrivers = maneuver.getAttitudeModelParameters(parameters);
        Assertions.assertEquals(0, actualDrivers.length);
    }

    @Test
    void testGetAttitudeModelParametersField() {
        final AttitudeRotationModel mockedRotationModel = Mockito.mock(AttitudeRotationModel.class);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] parameters = MathArrays.buildArray(field, 1);
        parameters[0] = Binary64.ONE;
        Mockito.when(mockedRotationModel.getParameters(field)).thenReturn(parameters);
        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(Mockito.mock(ParameterDriver.class));
        Mockito.when(mockedRotationModel.getParametersDrivers()).thenReturn(drivers);
        final Maneuver maneuver = new Maneuver(mockedRotationModel, null, null);
        final Binary64[] actualDrivers = maneuver.getAttitudeModelParameters(parameters);
        Assertions.assertArrayEquals(parameters, actualDrivers);
    }
}
