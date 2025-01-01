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
package org.orekit.forces.gravity;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class J2OnlyPerturbationTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

    @Test
    void testAcceleration() {
        // GIVEN
        final UnnormalizedSphericalHarmonicsProvider unnormalizedProvider = GravityFieldFactory.getUnnormalizedProvider(2 ,0);
        final Frame frame = FramesFactory.getGTOD(false);
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(unnormalizedProvider, frame);
        final SpacecraftState spacecraftState = buildState();
        // WHEN
        final Vector3D actualAcceleration = j2OnlyPerturbation.acceleration(spacecraftState, new double[0]);
        // THEN
        final HolmesFeatherstoneAttractionModel holmesFeatherstoneAttractionModel = new HolmesFeatherstoneAttractionModel(
                frame, GravityFieldFactory.getNormalizedProvider(unnormalizedProvider));
        final Vector3D expectedAcceleration = holmesFeatherstoneAttractionModel.acceleration(spacecraftState,
                new double[] { unnormalizedProvider.getMu() });
        final double tolerance = 1e-12;
        Assertions.assertEquals(expectedAcceleration.getX(), actualAcceleration.getX(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getY(), actualAcceleration.getY(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getZ(), actualAcceleration.getZ(), tolerance);
    }

    @Test
    void testAccelerationField() {
        // GIVEN
        final Frame frame = FramesFactory.getGTOD(false);
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(Constants.EGM96_EARTH_MU,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, frame);
        final SpacecraftState spacecraftState = buildState();
        final ComplexField field = ComplexField.getInstance();
        final FieldSpacecraftState<Complex> fieldState = new FieldSpacecraftState<>(field, spacecraftState);
        // WHEN
        final FieldVector3D<Complex> fieldAcceleration = j2OnlyPerturbation.acceleration(fieldState, null);
        final Vector3D actualAcceleration = fieldAcceleration.toVector3D();
        // THEN
        final Vector3D expectedAcceleration = j2OnlyPerturbation.acceleration(spacecraftState, new double[0]);
        final double tolerance = 1e-12;
        Assertions.assertEquals(expectedAcceleration.getX(), actualAcceleration.getX(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getY(), actualAcceleration.getY(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getZ(), actualAcceleration.getZ(), tolerance);
    }

    @Test
    void testAccelerationFieldAgainstGeopotential() {
        // GIVEN
        final UnnormalizedSphericalHarmonicsProvider unnormalizedProvider = GravityFieldFactory.getUnnormalizedProvider(2 ,0);
        final Frame frame = FramesFactory.getTOD(false);
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(unnormalizedProvider, frame);
        final SpacecraftState spacecraftState = buildState();
        final ComplexField field = ComplexField.getInstance();
        final FieldSpacecraftState<Complex> fieldState = new FieldSpacecraftState<>(field, spacecraftState);
        // WHEN
        final FieldVector3D<Complex> actualAcceleration = j2OnlyPerturbation.acceleration(fieldState, null);
        // THEN
        final Frame rotatingFrame = FramesFactory.getGTOD(false);
        final HolmesFeatherstoneAttractionModel holmesFeatherstoneAttractionModel = new HolmesFeatherstoneAttractionModel(
                rotatingFrame, GravityFieldFactory.getNormalizedProvider(unnormalizedProvider));
        final Complex[] mu = MathArrays.buildArray(field, 1);
        mu[0] = field.getOne().newInstance(unnormalizedProvider.getMu());
        final FieldVector3D<Complex> expectedAcceleration = holmesFeatherstoneAttractionModel.acceleration(fieldState,
                mu);
        final double tolerance = 1e-12;
        Assertions.assertEquals(expectedAcceleration.getX().getReal(), actualAcceleration.getX().getReal(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getY().getReal(), actualAcceleration.getY().getReal(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getZ().getReal(), actualAcceleration.getZ().getReal(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getX().getImaginary(), actualAcceleration.getX().getImaginary(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getY().getImaginary(), actualAcceleration.getY().getImaginary(), tolerance);
        Assertions.assertEquals(expectedAcceleration.getZ().getImaginary(), actualAcceleration.getZ().getImaginary(), tolerance);
    }

    @Test
    void testGetters() {
        // GIVEN
        final Frame expectedFrame = FramesFactory.getGTOD(false);
        final double expectedMu = Constants.EGM96_EARTH_MU;
        final double expectedrEq = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double expectedJ2 = -Constants.EGM96_EARTH_C20;
        // WHEN
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(expectedMu, expectedrEq,
                getJ2OverTime(expectedJ2), expectedFrame);
        // THEN
        Assertions.assertEquals(expectedFrame, j2OnlyPerturbation.getFrame());
        Assertions.assertEquals(expectedMu, j2OnlyPerturbation.getMu());
        Assertions.assertEquals(expectedrEq, j2OnlyPerturbation.getrEq());
        Assertions.assertTrue(j2OnlyPerturbation.getParametersDrivers().isEmpty());
        Assertions.assertTrue(j2OnlyPerturbation.dependsOnPositionOnly());
    }

    @Test
    void testGetJ2() {
        // GIVEN
        final Frame expectedFrame = FramesFactory.getGTOD(false);
        final double expectedMu = Constants.EGM96_EARTH_MU;
        final double expectedrEq = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double expectedJ2 = -Constants.EGM96_EARTH_C20;
        final AbsoluteDate absoluteDate = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final J2OnlyPerturbation j2OnlyPerturbation = new J2OnlyPerturbation(expectedMu, expectedrEq,
                getJ2OverTime(expectedJ2), expectedFrame);
        // THEN
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = new FieldAbsoluteDate<>(ComplexField.getInstance(),
                absoluteDate);
        Assertions.assertEquals(j2OnlyPerturbation.getJ2(absoluteDate),
                j2OnlyPerturbation.getJ2(fieldAbsoluteDate).getReal());
    }

    private TimeScalarFunction getJ2OverTime(final double constantJ2) {
        return new TimeScalarFunction() {
            @Override
            public double value(AbsoluteDate date) {
                return constantJ2;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T value(FieldAbsoluteDate<T> date) {
                return date.getField().getZero().newInstance(constantJ2);
            }
        };
    }

    private SpacecraftState buildState() {
        final Vector3D position = new Vector3D(1e7, 1e3, 1e2);
        final Vector3D velocity = new Vector3D(0., 4e3, 1e1);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        return new SpacecraftState(orbit);
    }

}
