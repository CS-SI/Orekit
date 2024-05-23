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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;

class AbstractGaussianContributionTest {

    @Test
    @DisplayName("Coverage test for attitude building when not depending on rate")
    void testGetMeanElementRateWhenNotDependingOnAttitudeRate() {
        testTemplateGetMeanElementRate(false);
    }

    @Test
    @DisplayName("Coverage test for attitude building when depending on rate")
    void testGetMeanElementRateWhenDependingOnAttitudeRate() {
        testTemplateGetMeanElementRate(true);
    }

    void testTemplateGetMeanElementRate(final boolean dependsOnAttitudeRate) {
        // GIVEN
        final double mu = Constants.EIGEN5C_EARTH_MU;
        final EquinoctialOrbit orbit = new EquinoctialOrbit(7e6, 0., 0.001, 0.01, 0., 0., PositionAngleType.ECCENTRIC,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, mu);
        final ForceModel mockedForce = Mockito.mock(ForceModel.class);
        Mockito.when(mockedForce.dependsOnAttitudeRate()).thenReturn(dependsOnAttitudeRate);
        Mockito.when(mockedForce.acceleration(Mockito.any(SpacecraftState.class), Mockito.any())).thenReturn(Vector3D.ZERO);
        final TestContribution testContribution = new TestContribution("", 1., mockedForce,
                mu);
        final SpacecraftState state = new SpacecraftState(orbit);
        final AuxiliaryElements elements = new AuxiliaryElements(orbit, 1);
        testContribution.registerAttitudeProvider(new FrameAlignedProvider(orbit.getFrame()));
        // WHEN
        final double[] rates = testContribution.getMeanElementRate(state, elements, new double[1]);
        // THEN
        Assertions.assertEquals(6, rates.length);
    }

    @Test
    @DisplayName("Coverage test for Field attitude building when depending on rate")
    void testFieldGetMeanElementRateWhenDependingOnAttitudeRate() {
        testTemplateFieldGetMeanElementRate(true);
    }

    @Test
    @DisplayName("Coverage test for Field attitude building when not depending on rate")
    void testFieldGetMeanElementRateWhenNotDependingOnAttitudeRate() {
        testTemplateFieldGetMeanElementRate(false);
    }

    @SuppressWarnings("unchecked")
    void testTemplateFieldGetMeanElementRate(final boolean dependsOnAttitudeRate) {
        // GIVEN
        final Field<Complex> field = ComplexField.getInstance();
        final double mu = Constants.EIGEN5C_EARTH_MU;
        final EquinoctialOrbit orbit = new EquinoctialOrbit(7e6, 0., 0.001, 0.01, 0., 0., PositionAngleType.ECCENTRIC,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, mu);
        final FieldOrbit<Complex> fieldOrbit = new FieldEquinoctialOrbit<>(field, orbit);
        final ForceModel mockedForce = Mockito.mock(ForceModel.class);
        Mockito.when(mockedForce.dependsOnAttitudeRate()).thenReturn(dependsOnAttitudeRate);
        Mockito.when(mockedForce.acceleration(Mockito.any(FieldSpacecraftState.class), Mockito.any()))
                .thenReturn(FieldVector3D.getZero(field));
        final TestContribution testContribution = new TestContribution("", 1., mockedForce,
                mu);
        final FieldAuxiliaryElements<Complex> elements = new FieldAuxiliaryElements<>(fieldOrbit, 1);
        testContribution.registerAttitudeProvider(new FrameAlignedProvider(orbit.getFrame()));
        final Complex[] parameters = MathArrays.buildArray(field, 1);
        parameters[0] = field.getOne();
        // WHEN
        final Complex[] rates = testContribution.getMeanElementRate(new FieldSpacecraftState<>(fieldOrbit), elements, parameters);
        // THEN
        Assertions.assertEquals(6, rates.length);
    }

    private static class TestContribution extends AbstractGaussianContribution {

        protected TestContribution(String coefficientsKeyPrefix, double threshold, ForceModel contribution, double mu) {
            super(coefficientsKeyPrefix, threshold, contribution, mu);
        }

        @Override
        protected List<ParameterDriver> getParametersDriversWithoutMu() {
            return Collections.emptyList();
        }

        @Override
        protected double[] getLLimits(SpacecraftState state, AuxiliaryElements auxiliaryElements) {
            return new double[] {0., 1.};
        }

        @Override
        protected <T extends CalculusFieldElement<T>> T[] getLLimits(FieldSpacecraftState<T> state, FieldAuxiliaryElements<T> auxiliaryElements) {
            final Field<T> field = state.getDate().getField();
            final T[] array = MathArrays.buildArray(field, 2);
            array[0] = field.getZero();
            array[1] = field.getOne();
            return array;

        }
    }

}
