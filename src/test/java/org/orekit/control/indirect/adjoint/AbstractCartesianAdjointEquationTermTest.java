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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

class AbstractCartesianAdjointEquationTermTest {

    @Test
    void testBuildGradientCartesianVector() {
        // GIVEN
        final double[] variables = {1, 2, 3, 4, 5, 6};
        // WHEN
        final Gradient[] gradients = AbstractCartesianAdjointEquationTerm.buildGradientCartesianVector(variables);
        // THEN
        for (int i = 0; i < gradients.length; i++) {
            Assertions.assertEquals(6, gradients[i].getFreeParameters());
            Assertions.assertEquals(gradients[i].getValue(), variables[i]);
            Assertions.assertEquals(1, gradients[i].getGradient()[i]);
        }
    }

    @Test
    void testBuildFieldGradientCartesianVector() {
        // GIVEN
        final Complex[] variables = MathArrays.buildArray(ComplexField.getInstance(), 6);
        for (int i = 0; i < variables.length; i++) {
            variables[i] = Complex.ONE.multiply(i);
        }
        // WHEN
        final FieldGradient<Complex>[] gradients = AbstractCartesianAdjointEquationTerm.buildFieldGradientCartesianVector(variables);
        // THEN
        for (int i = 0; i < gradients.length; i++) {
            Assertions.assertEquals(6, gradients[i].getFreeParameters());
            Assertions.assertEquals(gradients[i].getValue(), variables[i]);
            Assertions.assertEquals(Complex.ONE, gradients[i].getGradient()[i]);
        }
    }

    @Test
    void testGetFieldRatesContribution() {
        // GIVEN
        final Complex[] fieldState = MathArrays.buildArray(ComplexField.getInstance(), 6);
        final Complex[] fieldAdjoint = MathArrays.buildArray(ComplexField.getInstance(), 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = Complex.MINUS_ONE;
            fieldAdjoint[i] = Complex.ONE.multiply(i);
        }
        final TestAdjointTerm adjointTerm = new TestAdjointTerm();
        final FieldAbsoluteDate<Complex> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance());
        final Frame frame = Mockito.mock(Frame.class);
        // WHEN
        final Complex[] fieldRatesContribution = adjointTerm.getFieldRatesContribution(fieldDate, fieldState, fieldAdjoint, frame);
        // THEN
        final double[] states = new double[fieldAdjoint.length];
        final double[] adjoint = new double[fieldAdjoint.length];
        for (int i = 0; i < adjoint.length; i++) {
            states[i] = fieldState[i].getReal();
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final double[] ratesContribution = adjointTerm.getRatesContribution(fieldDate.toAbsoluteDate(), states, adjoint, frame);
        for (int i = 0; i < fieldRatesContribution.length; i++) {
            Assertions.assertEquals(fieldRatesContribution[i].getReal(), ratesContribution[i]);
        }
    }

    private static class TestAdjointTerm extends AbstractCartesianAdjointEquationTerm {
        public TestAdjointTerm() {}

        @Override
        protected Vector3D getAcceleration(AbsoluteDate date, double[] stateVariables, Frame frame) {
            return new Vector3D(stateVariables[0], stateVariables[1], stateVariables[5]);
        }

        @Override
        protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(FieldAbsoluteDate<T> date, T[] stateVariables, Frame frame) {
            return new FieldVector3D<>(date.getField(),
                    new Vector3D(stateVariables[0].getReal(), stateVariables[1].getReal(), stateVariables[5].getReal()));
        }
    }
}
