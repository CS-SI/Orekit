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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

class CartesianAdjointKeplerianTermTest {

    @Test
    @SuppressWarnings("unchecked")
    void testGetPositionAdjointContribution() {
        // GIVEN
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(Constants.EGM96_EARTH_MU);
        final double[] adjoint = new double[6];
        final double[] state = new double[6];
        for (int i = 0; i < adjoint.length; i++) {
            state[i] = -i+1;
            adjoint[i] = i;
        }
        // WHEN
        final double[] contribution = keplerianTerm.getPositionAdjointContribution(Mockito.mock(AbsoluteDate.class),
                state, adjoint, FramesFactory.getGCRF());
        // THEN
        final NewtonianAttraction newtonianAttraction = new NewtonianAttraction(keplerianTerm.getMu());
        final int dimension = 3;
        final GradientField field = GradientField.getField(dimension);
        final FieldSpacecraftState<Gradient> mockedState = Mockito.mock(FieldSpacecraftState.class);
        final FieldVector3D<Gradient> fieldPosition = new FieldVector3D<>(
                Gradient.variable(dimension, 0, state[0]),
                Gradient.variable(dimension, 1, state[1]),
                Gradient.variable(dimension, 2, state[2]));
        Mockito.when(mockedState.getPosition()).thenReturn(fieldPosition);
        final Gradient[] fieldMu = MathArrays.buildArray(field, 1);
        fieldMu[0] = Gradient.constant(dimension, keplerianTerm.getMu());
        final FieldVector3D<Gradient> acceleration = newtonianAttraction.acceleration(mockedState, fieldMu);
        Assertions.assertEquals(-contribution[0], acceleration.getX().getGradient()[0] * adjoint[3]
                + acceleration.getY().getGradient()[0] * adjoint[4] + acceleration.getZ().getGradient()[0] * adjoint[5]);
        Assertions.assertEquals(-contribution[1], acceleration.getX().getGradient()[1] * adjoint[3]
                + acceleration.getY().getGradient()[1] * adjoint[4] + acceleration.getZ().getGradient()[1] * adjoint[5]);
        Assertions.assertEquals(-contribution[2], acceleration.getX().getGradient()[2] * adjoint[3]
                + acceleration.getY().getGradient()[2] * adjoint[4] + acceleration.getZ().getGradient()[2] * adjoint[5]);
    }

    @Test
    void testGetPositionAdjointFieldContribution() {
        // GIVEN
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(Constants.EGM96_EARTH_MU);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i+1);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        final Frame frame = FramesFactory.getGCRF();
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final Binary64[] fieldContribution = keplerianTerm.getPositionAdjointFieldContribution(fieldDate, fieldState,
                fieldAdjoint, frame);
        // THEN
        final double[] state = new double[fieldState.length];
        for (int i = 0; i < fieldState.length; i++) {
            state[i] = fieldState[i].getReal();
        }
        final double[] adjoint = new double[fieldAdjoint.length];
        for (int i = 0; i < fieldAdjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final double[] contribution = keplerianTerm.getPositionAdjointContribution(fieldDate.toAbsoluteDate(), state,
                adjoint, frame);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i].getReal(), contribution[i]);
        }
    }
}
