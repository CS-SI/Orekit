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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;

class AbstractCartesianAdjointNonCentralBodyTermTest {

    @Test
    void testGetPositionAdjointFieldContribution() {
        // GIVEN
        final double mu = Constants.JPL_SSD_SUN_GM;
        final Frame frame = FramesFactory.getGCRF();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final AbsoluteDate date = fieldDate.toAbsoluteDate();
        final ExtendedPositionProvider mockedPositionProvider = Mockito.mock(ExtendedPositionProvider.class);
        final Vector3D position = new Vector3D(1e3, 2e4, -3e5);
        Mockito.when(mockedPositionProvider.getPosition(date, frame)).thenReturn(position);
        Mockito.when(mockedPositionProvider.getPosition(fieldDate, frame)).thenReturn(new FieldVector3D<>(field, position));
        final TestNonCentralBodyTerm centralBodyTerm = new TestNonCentralBodyTerm(mu,
                mockedPositionProvider);
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(100 * i);
            fieldAdjoint[i] = field.getZero().newInstance(-i+3);
        }
        // WHEN
        final Binary64[] fieldContribution = centralBodyTerm.getPositionAdjointFieldContribution(fieldDate,
                fieldState, fieldAdjoint, frame);
        // THEN
        final double[] state = new double[fieldState.length];
        final double[] adjoint = new double[fieldAdjoint.length];
        for (int i = 0; i < fieldAdjoint.length; i++) {
            state[i] = fieldState[i].getReal();
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final double[] contribution = centralBodyTerm.getPositionAdjointContribution(date, state, adjoint, frame);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i].getReal(), contribution[i]);
        }
    }

    @Test
    void testGetPositionAdjointFieldContributionAgainstKeplerian() {
        // GIVEN
        final double mu = Constants.JPL_SSD_SUN_GM;
        final Frame frame = FramesFactory.getGCRF();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final ExtendedPositionProvider mockedPositionProvider = Mockito.mock(ExtendedPositionProvider.class);
        Mockito.when(mockedPositionProvider.getPosition(fieldDate, frame)).thenReturn(FieldVector3D.getZero(field));
        final TestNonCentralBodyTerm centralBodyTerm = new TestNonCentralBodyTerm(mu,
                mockedPositionProvider);
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i+1);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        // WHEN
        final Binary64[] fieldContribution = centralBodyTerm.getPositionAdjointFieldContribution(fieldDate,
                fieldState, fieldAdjoint, frame);
        // THEN
        final CartesianAdjointKeplerianTerm keplerianTerm = new CartesianAdjointKeplerianTerm(mu);
        final Binary64[] contribution = keplerianTerm.getPositionAdjointFieldContribution(fieldDate, fieldState,
                fieldAdjoint, frame);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i], contribution[i]);
        }
    }

    private static class TestNonCentralBodyTerm extends AbstractCartesianAdjointNonCentralBodyTerm {

        protected TestNonCentralBodyTerm(double mu, ExtendedPositionProvider bodyPositionProvider) {
            super(mu, bodyPositionProvider);
        }

        @Override
        protected Vector3D getAcceleration(AbsoluteDate date, double[] stateVariables, Frame frame) {
            return null;
        }

        @Override
        protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(FieldAbsoluteDate<T> date, T[] stateVariables, Frame frame) {
            return null;
        }
    }
}
