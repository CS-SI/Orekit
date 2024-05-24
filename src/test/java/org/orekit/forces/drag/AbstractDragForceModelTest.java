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
package org.orekit.forces.drag;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;

class AbstractDragForceModelTest {

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

    @Test
    void testGetDSDensityWrtState() {
        // GIVEN
        final TestDragForceModel testDragForceModel = new TestDragForceModel(new TestAtmosphereModel());
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D position = new Vector3D(100., 1, 0);
        final int freeParameters = 3;
        final DSFactory factory = new DSFactory(freeParameters, 1);
        final FieldVector3D<DerivativeStructure> fieldPosition = new FieldVector3D<>(
                factory.variable(0, position.getX()),
                factory.variable(1, position.getY()),
                factory.variable(2, position.getZ()));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final DerivativeStructure actualDS = testDragForceModel.getDSDensityWrtState(date, frame, fieldPosition);
        // THEN
        final DerivativeStructure expectedDS = testDragForceModel.getDSDensityWrtStateUsingFiniteDifferences(date,
                frame, fieldPosition);
        Assertions.assertEquals(expectedDS.getValue(), actualDS.getValue());
        final double[] actualDerivatives = actualDS.getAllDerivatives();
        final double[] expectedDerivatives = expectedDS.getAllDerivatives();
        for (int i = 1; i < expectedDerivatives.length; i++) {
            Assertions.assertEquals(expectedDerivatives[i], actualDerivatives[i], 1e-10);
        }
    }

    private static class TestAtmosphereModel implements Atmosphere {

        @Override
        public Frame getFrame() {
            return FramesFactory.getEME2000();
        }

        @Override
        public double getDensity(AbsoluteDate date, Vector3D position, Frame frame) {
            return FastMath.exp(-position.getNormSq());
        }

        @Override
        public <T extends CalculusFieldElement<T>> T getDensity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame) {
            return FastMath.exp(position.getNormSq().negate());
        }
    }

    private static class TestDragForceModel extends AbstractDragForceModel {

        protected TestDragForceModel(Atmosphere atmosphere) {
            super(atmosphere);
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

}
