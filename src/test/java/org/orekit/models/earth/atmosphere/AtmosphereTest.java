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
package org.orekit.models.earth.atmosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.IERSConventions;

class AtmosphereTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testFieldGetVelocity() {
        // GIVEN
        final GradientField field = GradientField.getField(1);
        final FieldAbsoluteDate<Gradient> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Gradient> fieldPosition = FieldVector3D.getMinusI(field).scalarMultiply(1e3);
        final Frame frame = FramesFactory.getGCRF();
        final TestAtmosphere testAtmosphere = new TestAtmosphere();
        // WHEN
        final FieldVector3D<Gradient> fieldVelocity = testAtmosphere.getVelocity(fieldDate, fieldPosition, frame);
        // THEN
        final Vector3D actualVelocity = fieldVelocity.toVector3D();
        final Vector3D expectedVelocity = testAtmosphere.getVelocity(fieldDate.toAbsoluteDate(),
                fieldPosition.toVector3D(), frame);
        final double tolerance = 1e-10;
        Assertions.assertEquals(expectedVelocity.getX(), actualVelocity.getX(), tolerance);
        Assertions.assertEquals(expectedVelocity.getY(), actualVelocity.getY(), tolerance);
        Assertions.assertEquals(expectedVelocity.getZ(), actualVelocity.getZ(), tolerance);
    }

    @Test
    void testGetVelocityNonConstantFieldAbsoluteDate() {
        // GIVEN
        final UnivariateDerivative1 variable = new UnivariateDerivative1(0., 1.);
        final Field<UnivariateDerivative1> field = variable.getField();
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field)
                .shiftedBy(variable);
        final FieldVector3D<UnivariateDerivative1> fieldPosition = FieldVector3D.getMinusI(field).scalarMultiply(1e3);
        final Frame frame = FramesFactory.getGCRF();
        final TestAtmosphere testAtmosphere = new TestAtmosphere();
        // WHEN
        final FieldVector3D<UnivariateDerivative1> fieldVelocity = testAtmosphere.getVelocity(fieldDate, fieldPosition, frame);
        // THEN
        Assertions.assertNotEquals(0., fieldVelocity.getNorm().getFirstDerivative(), 0.0);
    }

    private static class TestAtmosphere implements Atmosphere {

        @Override
        public Frame getFrame() {
            return FramesFactory.getITRF(IERSConventions.IERS_2003, false);
        }

        @Override
        public double getDensity(AbsoluteDate date, Vector3D position, Frame frame) {
            return 1.;
        }

        @Override
        public <T extends CalculusFieldElement<T>> T getDensity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame) {
            final CalculusFieldElement<T> zero = date.getField().getZero();
            return zero.add(getDensity(date.toAbsoluteDate(), position.toVector3D(), frame));
        }
    }

}