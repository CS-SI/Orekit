/* Copyright 2022-2026 Romain Serra
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
package org.orekit.forces.drag;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractDragForceModelTest {

    @Test
    @SuppressWarnings("unchecked")
    void testGetFieldDensityFiniteDifferences() {
        // GIVEN
        final double expectedRealDensity = 1.;
        final GradientField field = GradientField.getField(6);
        final FieldOrbit<Gradient> fieldOrbit = DerivativeStateUtils.buildOrbitGradient(field,
                TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(fieldOrbit);
        final Atmosphere atmosphere = mock();
        when(atmosphere.getDensity(any(AbsoluteDate.class), any(Vector3D.class), any(Frame.class))).thenReturn(expectedRealDensity);
        final Atmosphere fieldAtmosphere = mock();
        when(fieldAtmosphere.getFrame()).thenReturn(fieldOrbit.getFrame());
        when(fieldAtmosphere.getDensity(any(FieldAbsoluteDate.class), any(FieldVector3D.class), any(Frame.class)))
                .thenReturn(Gradient.constant(field.getOne().getFreeParameters(), 2.));
        final TestDragForceModel dragForceModel = new TestDragForceModel(atmosphere, fieldAtmosphere);
        // WHEN
        final Gradient density = dragForceModel.getFieldDensity(fieldState);
        // THEN
        assertEquals(expectedRealDensity, density.getReal());
        verify(atmosphere, times(1)).getDensity(any(AbsoluteDate.class), any(Vector3D.class), any(Frame.class));
        verify(fieldAtmosphere, times(4)).getDensity(any(AbsoluteDate.class), any(Vector3D.class), any(Frame.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetFieldDensity() {
        // GIVEN
        final double expectedRealDensity = 1.;
        final Atmosphere atmosphere = mock();
        when(atmosphere.getDensity(any(AbsoluteDate.class), any(Vector3D.class), any(Frame.class))).thenReturn(expectedRealDensity);
        final Atmosphere fieldAtmosphere = mock();
        when(fieldAtmosphere.getDensity(any(FieldAbsoluteDate.class), any(FieldVector3D.class), any(Frame.class))).thenReturn(Gradient.constant(0, 2.));
        final TestDragForceModel dragForceModel = new TestDragForceModel(atmosphere, fieldAtmosphere);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(GradientField.getField(0), state);
        // WHEN
        final Gradient density = dragForceModel.getFieldDensity(fieldState);
        // THEN
        assertEquals(expectedRealDensity, density.getReal());
        verify(atmosphere, times(1)).getDensity(any(AbsoluteDate.class), any(Vector3D.class), any(Frame.class));
        verify(fieldAtmosphere, times(1)).getDensity(eq(fieldState.getDate()), any(FieldVector3D.class), any(Frame.class));
    }

    @Test
    void testDependsOnPositionOnly() {
        // GIVEN
        final TestAtmosphereModel atmosphereModel = new TestAtmosphereModel();
        final TestDragForceModel dragForceModel = new TestDragForceModel(atmosphereModel);
        // WHEN
        final boolean actual = dragForceModel.dependsOnPositionOnly();
        // THEN
        assertFalse(actual);
    }

    @Test
    void testGetAtmosphereForField() {
        // GIVEN
        final TestAtmosphereModel atmosphereModel = new TestAtmosphereModel();
        final TestAtmosphereModel otherAtmosphere = new TestAtmosphereModel();
        final TestDragForceModel dragForceModel = new TestDragForceModel(atmosphereModel, otherAtmosphere);
        // WHEN
        final Atmosphere atmosphere = dragForceModel.getAtmosphereForField();
        // THEN
        Assertions.assertEquals(otherAtmosphere, atmosphere);
    }

    @Test
    void testGetGradientDensityWrtState() {
        // GIVEN
        final TestDragForceModel testDragForceModel = new TestDragForceModel(new TestAtmosphereModel());
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D position = new Vector3D(100., 1, 0);
        final int freeParameters = 3;
        final FieldVector3D<Gradient> fieldPosition = new FieldVector3D<>(
                Gradient.variable(freeParameters, 0, position.getX()),
                Gradient.variable(freeParameters, 1, position.getY()),
                Gradient.variable(freeParameters, 2, position.getZ()));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Gradient actualGradient = testDragForceModel.getGradientDensityWrtState(date, frame, fieldPosition);
        // THEN
        final Gradient expectedGradient = testDragForceModel.getGradientDensityWrtStateUsingFiniteDifferences(date,
                frame, fieldPosition);
        Assertions.assertEquals(expectedGradient.getValue(), actualGradient.getValue());
        for (int i = 0; i < freeParameters; i++) {
            Assertions.assertEquals(expectedGradient.getPartialDerivative(i), actualGradient.getPartialDerivative(i), 1e-10);
        }
    }

    private static class TestAtmosphereModel implements Atmosphere {

        @Override
        public Frame getFrame() {
            return FramesFactory.getEME2000();
        }

        @Override
        public double getDensity(AbsoluteDate date, Vector3D position, Frame frame) {
            return FastMath.exp(-position.getNorm2Sq());
        }

        @Override
        public <T extends CalculusFieldElement<T>> T getDensity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame) {
            return FastMath.exp(position.getNorm2Sq().negate());
        }
    }

    private static class TestDragForceModel extends AbstractDragForceModel {

        TestDragForceModel(Atmosphere atmosphere) {
            super(atmosphere);
        }

        TestDragForceModel(Atmosphere atmosphere, Atmosphere atmosphereForDerivatives) {
            super(atmosphere, true, atmosphereForDerivatives);
        }

        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return null;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            return null;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    // This test was added to increase overall conditions coverage in the scope of issue 1453
    void testIssue1453() {
        // GIVEN
        // Load Orekit data
        Utils.setDataRoot("regular-data");

        // Create mock field
        final Field<Gradient> fakeField = new Field<Gradient>() {
            @Override
            public Gradient getZero() {
                return new Gradient(0, 0);
            }

            @Override
            public Gradient getOne() {
                return new Gradient(1, 1);
            }

            @Override
            public Class<Gradient> getRuntimeClass() {
                return null;
            }
        };

        // Create mock date
        final FieldAbsoluteDate<Gradient> fakeFieldAbsoluteDate = new FieldAbsoluteDate<>(fakeField);

        // Create fake PV
        final FieldVector3D<Gradient> pos =
                new FieldVector3D<>(new FakeGradient(1, 1), new FakeGradient(2, 20), new FakeGradient(3, 30));

        final FieldVector3D<Gradient> vel =
                new FieldVector3D<>(new FakeGradient(4, 40), new FakeGradient(5, 50), new FakeGradient(6, 60));

        final TimeStampedFieldPVCoordinates<Gradient> fakePV =
                new TimeStampedFieldPVCoordinates<>(fakeFieldAbsoluteDate, pos, vel, FieldVector3D.getZero(fakeField));

        // Create mock mass
        final Gradient mockMass = mock(Gradient.class);
        when(mockMass.getFreeParameters()).thenReturn(6);

        // Create mock spacecraft state
        final FieldSpacecraftState<Gradient> mockSpacecraftState = mock(FieldSpacecraftState.class);
        when(mockSpacecraftState.getPVCoordinates()).thenReturn(fakePV);
        when(mockSpacecraftState.getMass()).thenReturn(mockMass);

        // Create drag force
        final DragForce dragForce = new DragForce(null,null);

        // WHEN & THEN
        assertFalse(dragForce.isGradientStateDerivative(mockSpacecraftState));
        ((FakeGradient) pos.getY()).setMutableGradient(new double[] { 0, 1 });
        assertFalse(dragForce.isGradientStateDerivative(mockSpacecraftState));

        ((FakeGradient) pos.getZ()).setMutableGradient(new double[] { 0, 0, 1 });
        assertFalse(dragForce.isGradientStateDerivative(mockSpacecraftState));

        ((FakeGradient) vel.getX()).setMutableGradient(new double[] { 0, 0, 0, 1 });
        assertFalse(dragForce.isGradientStateDerivative(mockSpacecraftState));

        ((FakeGradient) vel.getY()).setMutableGradient(new double[] { 0, 0, 0, 0, 1 });
        assertFalse(dragForce.isGradientStateDerivative(mockSpacecraftState));

        ((FakeGradient) vel.getZ()).setMutableGradient(new double[] { 0, 0, 0, 0, 0, 1 });
        assertTrue(dragForce.isGradientStateDerivative(mockSpacecraftState));
    }

    private static class FakeGradient extends Gradient {

        private final double   mutableValue;
        private double[] mutableGradient;

        public FakeGradient(final double value, final double... gradient) {
            super(value, gradient);
            this.mutableValue    = value;
            this.mutableGradient = gradient;
        }

        @Override
        public double getValue() {
            return mutableValue;
        }

        @Override
        public double[] getGradient() {
            return mutableGradient;
        }

        public void setMutableGradient(final double[] mutableGradient) {
            this.mutableGradient = mutableGradient;
        }
    }

}
