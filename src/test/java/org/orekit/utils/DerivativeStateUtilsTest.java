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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class DerivativeStateUtilsTest {

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testBuildOrbitGradient(final OrbitType orbitType) {
        // GIVEN
        final Orbit orbit = orbitType.convertType(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final GradientField field = GradientField.getField(6);
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        // WHEN
        final FieldOrbit<Gradient> fieldOrbit = DerivativeStateUtils.buildOrbitGradient(field, orbit);
        // THEN
        final double[] expected = new double[6];
        orbitType.mapOrbitToArray(orbit, positionAngleType, expected, null);
        final Gradient[] actual = MathArrays.buildArray(field, 6);
        orbitType.mapOrbitToArray(fieldOrbit, positionAngleType, actual, null);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i].getReal());
        }
        switch (orbitType) {

            case CARTESIAN:
                assertEquals(1., fieldOrbit.getPosition().getX().getGradient()[0]);
                assertEquals(1., fieldOrbit.getPosition().getY().getGradient()[1]);
                assertEquals(1., fieldOrbit.getPosition().getZ().getGradient()[2]);
                assertEquals(1., fieldOrbit.getPVCoordinates().getVelocity().getX().getGradient()[3]);
                assertEquals(1., fieldOrbit.getPVCoordinates().getVelocity().getY().getGradient()[4]);
                assertEquals(1., fieldOrbit.getPVCoordinates().getVelocity().getZ().getGradient()[5]);
                break;

            case EQUINOCTIAL:
                final FieldEquinoctialOrbit<Gradient> fieldEquinoctialOrbit = (FieldEquinoctialOrbit<Gradient>) fieldOrbit;
                assertEquals(1., fieldOrbit.getA().getGradient()[0]);
                assertEquals(1., fieldOrbit.getEquinoctialEx().getGradient()[1]);
                assertEquals(1., fieldOrbit.getEquinoctialEy().getGradient()[2]);
                assertEquals(1., fieldOrbit.getHx().getGradient()[3]);
                assertEquals(1., fieldOrbit.getHy().getGradient()[4]);
                assertEquals(1., fieldEquinoctialOrbit.getL(fieldEquinoctialOrbit.getCachedPositionAngleType()).getGradient()[5]);
                break;

            case CIRCULAR:
                final FieldCircularOrbit<Gradient> fieldCircularOrbit = (FieldCircularOrbit<Gradient>) fieldOrbit;
                assertEquals(1., fieldCircularOrbit.getA().getGradient()[0]);
                assertEquals(1., fieldCircularOrbit.getCircularEx().getGradient()[1]);
                assertEquals(1., fieldCircularOrbit.getCircularEy().getGradient()[2]);
                assertEquals(1., fieldCircularOrbit.getI().getGradient()[3]);
                assertEquals(1., fieldCircularOrbit.getRightAscensionOfAscendingNode().getGradient()[4]);
                assertEquals(1., fieldCircularOrbit.getAlpha(fieldCircularOrbit.getCachedPositionAngleType()).getGradient()[5]);
                break;


            case KEPLERIAN:
                final FieldKeplerianOrbit<Gradient> fieldKeplerianOrbit = (FieldKeplerianOrbit<Gradient>) fieldOrbit;
                assertEquals(1., fieldKeplerianOrbit.getA().getGradient()[0]);
                assertEquals(1., fieldKeplerianOrbit.getE().getGradient()[1]);
                assertEquals(1., fieldKeplerianOrbit.getI().getGradient()[2]);
                assertEquals(1., fieldKeplerianOrbit.getPerigeeArgument().getGradient()[3]);
                assertEquals(1., fieldKeplerianOrbit.getRightAscensionOfAscendingNode().getGradient()[4]);
                assertEquals(1., fieldKeplerianOrbit.getAnomaly(fieldKeplerianOrbit.getCachedPositionAngleType()).getGradient()[5]);
                break;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void testBuildOrbitGradientCartesian(final int freeParameters) {
        // GIVEN
        final Orbit orbit = OrbitType.CARTESIAN.convertType(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final GradientField field = GradientField.getField(freeParameters);
        // WHEN
        final FieldOrbit<Gradient> fieldOrbit = DerivativeStateUtils.buildOrbitGradient(field, orbit);
        // THEN
        assertEquals(orbit.getFrame(), fieldOrbit.getFrame());
        assertEquals(orbit.getDate(), fieldOrbit.getDate().toAbsoluteDate());
        assertArrayEquals(new double[freeParameters], fieldOrbit.getDate().durationFrom(orbit.getDate()).getGradient());
        final FieldVector3D<Gradient> fieldPosition = fieldOrbit.getPosition();
        final Vector3D position = orbit.getPosition();
        assertEquals(position.getX(), fieldPosition.getX().getReal());
        assertEquals(position.getY(), fieldPosition.getY().getReal());
        assertEquals(position.getZ(), fieldPosition.getZ().getReal());
        assertEquals(1., fieldOrbit.getPosition().getX().getGradient()[0]);
        for (int i = 1; i < freeParameters; i++) {
            assertEquals(0., fieldOrbit.getPosition().getX().getGradient()[i]);
        }
        if (freeParameters <= 3) {
            assertArrayEquals(orbit.getPVCoordinates().getVelocity().toArray(),
                    fieldOrbit.getPVCoordinates().getVelocity().toVector3D().toArray());
        } else {
            assertEquals(1., fieldOrbit.getPosition().getY().getGradient()[1]);
            assertEquals(1., fieldOrbit.getPosition().getZ().getGradient()[2]);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void testBuildOrbitGradientPV(final int freeParameters) {
        // GIVEN
        final Orbit orbit = OrbitType.CARTESIAN.convertType(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final GradientField field = GradientField.getField(freeParameters);
        final AbsolutePVCoordinates pvCoordinates = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getDate(),
                orbit.getPVCoordinates());
        // WHEN
        final FieldAbsolutePVCoordinates<Gradient> fieldPV = DerivativeStateUtils.buildAbsolutePVGradient(field, pvCoordinates);
        // THEN
        assertEquals(pvCoordinates.getFrame(), fieldPV.getFrame());
        assertEquals(pvCoordinates.getDate(), fieldPV.getDate().toAbsoluteDate());
        assertArrayEquals(new double[freeParameters], fieldPV.getDate().durationFrom(pvCoordinates.getDate()).getGradient());
        final FieldOrbit<Gradient> fieldOrbit = DerivativeStateUtils.buildOrbitGradient(field, orbit);
        assertArrayEquals(fieldPV.getPosition().toArray(), fieldOrbit.getPosition().toArray());
        assertArrayEquals(fieldPV.getPVCoordinates().getVelocity().toArray(),
                fieldOrbit.getPVCoordinates().getVelocity().toArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 7})
    void testBuildStateGradientNullAttitudeProvider(final int freeParameters) {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final GradientField field = GradientField.getField(freeParameters);
        final SpacecraftState state = new SpacecraftState(orbit);
        // WHEN
        final FieldSpacecraftState<Gradient> fieldState = DerivativeStateUtils.buildSpacecraftStateGradient(field, state, null);
        // THEN
        assertEquals(state.getMass(), fieldState.getMass().getReal());
        if (freeParameters == 7) {
            assertArrayEquals(new double[]{0., 0., 0., 0., 0., 0., 1.}, fieldState.getMass().getGradient());
        }
        assertEquals(state.getDate(), fieldState.getDate().toAbsoluteDate());
        assertEquals(state.getPosition(), fieldState.getPosition().toVector3D());
        assertEquals(state.getAttitude().getRotationAcceleration(), fieldState.getAttitude().getRotationAcceleration().toVector3D());
        assertEquals(state.getAttitude().getSpin(), fieldState.getAttitude().getSpin().toVector3D());
    }

    @Test
    void testBuildStateGradientNonNullAttitudeProvider() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final GradientField field = GradientField.getField(6);
        final SpacecraftState state = new SpacecraftState(orbit);
        final LofOffset lofOffset = new LofOffset(orbit.getFrame(), LOFType.TNW);
        // WHEN
        final FieldSpacecraftState<Gradient> fieldState = DerivativeStateUtils.buildSpacecraftStateGradient(field, state, lofOffset);
        // THEN
        assertEquals(state.getMass(), fieldState.getMass().getReal());
        assertEquals(state.getDate(), fieldState.getDate().toAbsoluteDate());
        assertEquals(state.getPosition(), fieldState.getPosition().toVector3D());
        final Attitude attitude = lofOffset.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        assertArrayEquals(attitude.getSpin().toArray(), fieldState.getAttitude().getSpin().toVector3D().toArray(), 1e-12);
        assertArrayEquals(attitude.getRotationAcceleration().toArray(),
                fieldState.getAttitude().getRotationAcceleration().toVector3D().toArray(), 1e-12);
    }
}

