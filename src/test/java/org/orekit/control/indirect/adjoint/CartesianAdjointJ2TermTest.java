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

class CartesianAdjointJ2TermTest {

    @Test
    void testGetters() {
        // GIVEN
        final double expectedMu = 1.;
        final double expectedrEq = 2.;
        final double expectedJ2 = 3.;
        final Frame frame = Mockito.mock(Frame.class);
        final CartesianAdjointJ2Term cartesianAdjointJ2Term = new CartesianAdjointJ2Term(expectedMu, expectedrEq,
                expectedJ2, frame);
        // WHEN
        final double actualMu = cartesianAdjointJ2Term.getMu();
        final double actualrEq = cartesianAdjointJ2Term.getrEq();
        final double actualJ2 = cartesianAdjointJ2Term.getJ2();
        // THEN
        Assertions.assertEquals(expectedJ2, actualJ2);
        Assertions.assertEquals(expectedMu, actualMu);
        Assertions.assertEquals(expectedrEq, actualrEq);
    }

    @Test
    void testGetPositionAdjointContributionLinearity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(Constants.EGM96_EARTH_MU,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, frame);
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6};
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final double[] positionVelocity = new double[] {1, 1, 1, 1, 1, 1};
        // WHEN
        final double[] contribution = j2Term.getPositionAdjointContribution(date, positionVelocity, adjoint, frame);
        // THEN
        final double[] doubleAdjoint = new double[6];
        for (int i = 0; i < 6; i++) {
            doubleAdjoint[i] = adjoint[i] * 2;
        }
        final double[] contributionDouble = j2Term.getPositionAdjointContribution(date, positionVelocity, doubleAdjoint,
                frame);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(contribution[i] * 2, contributionDouble[i]);
        }
    }

    @Test
    void testGetPositionAdjointFieldContribution() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(Constants.EGM96_EARTH_MU,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, frame);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i+1);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final Binary64[] fieldContribution = j2Term.getPositionAdjointFieldContribution(fieldDate, fieldState,
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
        final double[] contribution = j2Term.getPositionAdjointContribution(fieldDate.toAbsoluteDate(), state, adjoint,
                frame);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i].getReal(), contribution[i]);
        }
    }

    @Test
    void testGetFieldHamiltonianContribution() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final CartesianAdjointJ2Term cartesianAdjointJ2Term = new CartesianAdjointJ2Term(1., 1., 0.001, frame);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i + 1);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final Binary64 fieldContribution = cartesianAdjointJ2Term.getFieldHamiltonianContribution(fieldDate,
                fieldState, fieldAdjoint, frame);
        // THEN
        final double[] adjoint = new double[fieldAdjoint.length];
        final double[] state = adjoint.clone();
        for (int i = 0; i < fieldAdjoint.length; i++) {
            state[i] = fieldState[i].getReal();
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final double contribution = cartesianAdjointJ2Term.getHamiltonianContribution(fieldDate.toAbsoluteDate(),
                state, adjoint, frame);
        Assertions.assertEquals(contribution, fieldContribution.getReal());
    }

}
