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
package org.orekit.control.indirect.adjoint;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.bodies.CelestialBody;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

class CartesianAdjointThirdBodyTermTest {

    @Test
    void testGetAcceleration() {
        // GIVEN
        final double mu = 2.;
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final CelestialBody mockedPositionProvider = Mockito.mock(CelestialBody.class);
        final Vector3D bodyPosition = new Vector3D(1, -2, 10);
        Mockito.when(mockedPositionProvider.getPosition(date, frame)).thenReturn(bodyPosition);
        final CartesianAdjointThirdBodyTerm thirdBodyTerm = new CartesianAdjointThirdBodyTerm(mu,
                mockedPositionProvider);
        final CartesianOrbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(date, Vector3D.MINUS_J, Vector3D.MINUS_K),
                frame, mu);
        final double[] state = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(orbit, PositionAngleType.ECCENTRIC, state, null);
        // WHEN
        final Vector3D acceleration = thirdBodyTerm.getAcceleration(date, state, frame);
        // THEN
        final ThirdBodyAttraction thirdBodyAttraction = new ThirdBodyAttraction(mockedPositionProvider);
        final Vector3D expectedAcceleration = thirdBodyAttraction.acceleration(new SpacecraftState(orbit), new double[] {mu});
        final double tolerance = 1e-15;
        Assertions.assertEquals(expectedAcceleration.getX(), acceleration.getX(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getY(), acceleration.getY(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getZ(), acceleration.getZ(), tolerance);
    }

    @Test
    void testGetFieldAcceleration() {
        // GIVEN
        final double mu = 3e14;
        final Frame frame = FramesFactory.getGCRF();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final ExtendedPositionProvider mockedPositionProvider = Mockito.mock(ExtendedPositionProvider.class);
        final AbsoluteDate date = fieldDate.toAbsoluteDate();
        final Vector3D bodyPosition = new Vector3D(1e4, -2e5, 1e4);
        Mockito.when(mockedPositionProvider.getPosition(date, frame)).thenReturn(bodyPosition);
        Mockito.when(mockedPositionProvider.getPosition(fieldDate, frame)).thenReturn(new FieldVector3D<>(field, bodyPosition));
        final CartesianAdjointThirdBodyTerm thirdBodyTerm = new CartesianAdjointThirdBodyTerm(mu,
                mockedPositionProvider);
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(i*10);
            fieldAdjoint[i] = field.getZero().newInstance(i*2+1);
        }
        // WHEN
        final FieldVector3D<Binary64> fieldAcceleration = thirdBodyTerm.getFieldAcceleration(fieldDate,
                fieldState, frame);
        // THEN
        final double[] state = new double[fieldState.length];
        for (int i = 0; i < fieldState.length; i++) {
            state[i] = fieldState[i].getReal();
        }
        final Vector3D acceleration = thirdBodyTerm.getAcceleration(date, state, frame);
        Assertions.assertEquals(fieldAcceleration.getX().getReal(), acceleration.getX());
        Assertions.assertEquals(fieldAcceleration.getY().getReal(), acceleration.getY());
        Assertions.assertEquals(fieldAcceleration.getZ().getReal(), acceleration.getZ());
    }

    @Test
    void testGetPositionAdjointFieldContributionAgainstKeplerian() {
        // GIVEN
        final double mu = 1.32712440017987E20;
        final Frame frame = FramesFactory.getGCRF();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final ExtendedPositionProvider mockedPositionProvider = Mockito.mock(ExtendedPositionProvider.class);
        Mockito.when(mockedPositionProvider.getPosition(fieldDate, frame)).thenReturn(FieldVector3D.getZero(field));
        final CartesianAdjointThirdBodyTerm thirdBodyTerm = new CartesianAdjointThirdBodyTerm(mu,
                mockedPositionProvider);
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i*20);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        // WHEN
        final Binary64[] fieldContribution = thirdBodyTerm.getPositionAdjointFieldContribution(fieldDate,
                fieldState, fieldAdjoint, frame);
        // THEN
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(mu);
        final Binary64[] contribution = keplerianTerm.getPositionAdjointFieldContribution(fieldDate, fieldState,
                fieldAdjoint, frame);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i], contribution[i]);
        }
    }
}
