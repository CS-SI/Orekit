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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.List;
import java.util.stream.Collectors;

class FieldQuadraticallyPenalizedCartesianFuelTest {

    private static final String ADJOINT_NAME = "adjoint";

    @ParameterizedTest
    @ValueSource(doubles = {1, 2, 10, 100, 1000})
    void testEvaluateFieldPenaltyFunction(final double norm) {
        // GIVEN
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> penalizedCartesianFuel = new FieldQuadraticallyPenalizedCartesianFuel<>(
                ADJOINT_NAME, Binary64.ONE, Binary64.PI, Binary64.ZERO);
        // WHEN
        final Binary64 actualPenalty = penalizedCartesianFuel.evaluateFieldPenaltyFunction(Binary64.ONE.newInstance(norm));
        // THEN
        Assertions.assertEquals(norm * norm / 2, actualPenalty.getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1e-3, 1e-2, 0.5, 0.999})
    void testGetFieldHamiltonianContribution(final double epsilon) {
        // GIVEN
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> fieldCost = new FieldQuadraticallyPenalizedCartesianFuel<>(
                ADJOINT_NAME, Binary64.ONE, Binary64.PI, new Binary64(epsilon));
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6, 7};
        final Binary64[] fieldAdjoint = MathArrays.buildArray(Binary64Field.getInstance(), adjoint.length);
        for (int i = 0; i < adjoint.length; i++) {
            fieldAdjoint[i] = fieldCost.getEpsilon().newInstance(adjoint[i]);
        }
        final Binary64 mass = new Binary64(100);
        // WHEN
        final Binary64 actualPenalty = fieldCost.getFieldHamiltonianContribution(fieldAdjoint, mass);
        // THEN
        final QuadraticallyPenalizedCartesianFuel cost = fieldCost.toCartesianCost();
        Assertions.assertEquals(cost.getHamiltonianContribution(adjoint, mass.getReal()), actualPenalty.getReal());
    }

    @Test
    void testGetFieldEventDetectors() {
        // GIVEN
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> penalizedCartesianFuel = new FieldQuadraticallyPenalizedCartesianFuel<>(
                ADJOINT_NAME, Binary64.ONE, Binary64.PI, new Binary64(0.5));
        // WHEN
        final List<FieldEventDetector<Binary64>> actualDetectors = penalizedCartesianFuel
                .getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());
        // THEN
        Assertions.assertEquals(2, actualDetectors.size());
        final SpacecraftState state = buildState(10);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        final Binary64 g1 = actualDetectors.get(0).g(fieldState);
        final Binary64 g2 = actualDetectors.get(1).g(fieldState);
        final Binary64 difference = FastMath.abs(g2.subtract(g1));
        Assertions.assertEquals(0., penalizedCartesianFuel.getMaximumThrustMagnitude().subtract(difference).getReal(), 1e-12);
    }

    @Test
    void testGetThrustAccelerationVectorEpsilonCloseToZero() {
        // GIVEN
        final Binary64 massFlowRateFactor = new Binary64(1);
        final Binary64 maximumThrustMagnitude = new Binary64(10);
        final Binary64 epsilon = new Binary64(1e-6);
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> penalizedCartesianFuel = new FieldQuadraticallyPenalizedCartesianFuel<>(ADJOINT_NAME,
                massFlowRateFactor, maximumThrustMagnitude, epsilon);
        final Binary64 mass = new Binary64(100);
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6, 7};
        final Binary64[] fieldAdjoint = MathArrays.buildArray(Binary64Field.getInstance(), adjoint.length);
        for (int i = 0; i < adjoint.length; i++) {
            fieldAdjoint[i] = epsilon.newInstance(adjoint[i]);
        }
        // WHEN
        final FieldVector3D<Binary64> actualThrustVector = penalizedCartesianFuel.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final FieldCartesianFuelCost<Binary64> fuelCost = new FieldCartesianFuelCost<>(ADJOINT_NAME, massFlowRateFactor, maximumThrustMagnitude);
        final FieldVector3D<Binary64> expectedThrustVector = fuelCost.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        Assertions.assertEquals(0., expectedThrustVector.subtract(actualThrustVector).toVector3D().getNorm(), 1e-10);
    }

    @Test
    void testGetThrustAccelerationVectorEpsilonEqualToOne() {
        // GIVEN
        final Binary64 massFlowRateFactor = new Binary64(1);
        final Binary64 maximumThrustMagnitude = new Binary64(10);
        final Binary64 epsilon = Binary64.ONE;
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> penalizedCartesianFuel = new FieldQuadraticallyPenalizedCartesianFuel<>(ADJOINT_NAME,
                massFlowRateFactor, maximumThrustMagnitude, epsilon);
        final Binary64 mass = new Binary64(100);
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6, 7};
        final Binary64[] fieldAdjoint = MathArrays.buildArray(Binary64Field.getInstance(), adjoint.length);
        for (int i = 0; i < adjoint.length; i++) {
            fieldAdjoint[i] = epsilon.newInstance(adjoint[i]);
        }
        // WHEN
        final FieldVector3D<Binary64> actualThrustVector = penalizedCartesianFuel.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final FieldBoundedCartesianEnergy<Binary64> fuelCost = new FieldBoundedCartesianEnergy<>(ADJOINT_NAME, massFlowRateFactor, maximumThrustMagnitude);
        final FieldVector3D<Binary64> expectedThrustVector = fuelCost.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        Assertions.assertEquals(0., expectedThrustVector.subtract(actualThrustVector).toVector3D().getNorm());
    }

    @Test
    void testToCartesianCost() {
        // GIVEN
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> fieldCost = new FieldQuadraticallyPenalizedCartesianFuel<>(
                ADJOINT_NAME, Binary64.ONE, Binary64.PI, Binary64.ZERO);
        // WHEN
        final QuadraticallyPenalizedCartesianFuel cost = fieldCost.toCartesianCost();
        // THEN
        Assertions.assertEquals(fieldCost.getEpsilon().getReal(), cost.getEpsilon());
        Assertions.assertEquals(fieldCost.getMaximumThrustMagnitude().getReal(), cost.getMaximumThrustMagnitude());
        Assertions.assertEquals(fieldCost.getMassFlowRateFactor().getReal(), cost.getMassFlowRateFactor());
        Assertions.assertEquals(fieldCost.getAdjointName(), cost.getAdjointName());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1, 1e2, 1e4})
    void testAgainstNonField(final double mass) {
        // GIVEN
        final double massFlowRateFactor = 1.e-2;
        final double maximumThrustMagnitude = 1e-3;
        final double epsilon = 0.5;
        final Binary64 zero = Binary64.ZERO;
        final FieldQuadraticallyPenalizedCartesianFuel<Binary64> fieldCost = new FieldQuadraticallyPenalizedCartesianFuel<>(ADJOINT_NAME,
                zero.newInstance(massFlowRateFactor), zero.newInstance(maximumThrustMagnitude), zero.newInstance(epsilon));
        final SpacecraftState state = buildState(mass);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        final FieldAdditionalDerivativesProvider<Binary64> derivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(fieldCost);
        // WHEN
        final FieldCombinedDerivatives<Binary64> actualDerivatives = derivativesProvider.combinedDerivatives(fieldState);
        // THEN
        final QuadraticallyPenalizedCartesianFuel cost = new QuadraticallyPenalizedCartesianFuel(ADJOINT_NAME,
                massFlowRateFactor, maximumThrustMagnitude, epsilon);
        final CombinedDerivatives expectedDerivatives = new CartesianAdjointDerivativesProvider(cost)
                .combinedDerivatives(state);
        for (int i = 0; i < expectedDerivatives.getMainStateDerivativesIncrements().length; i++) {
            Assertions.assertEquals(expectedDerivatives.getMainStateDerivativesIncrements()[i],
                    actualDerivatives.getMainStateDerivativesIncrements()[i].getReal(), 1e-12);
        }
        for (int i = 0; i < expectedDerivatives.getAdditionalDerivatives().length; i++) {
            Assertions.assertEquals(expectedDerivatives.getAdditionalDerivatives()[i],
                    actualDerivatives.getAdditionalDerivatives()[i].getReal());
        }
    }

    private SpacecraftState buildState(final double mass) {
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6, 7};
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_I, Vector3D.MINUS_K),
                FramesFactory.getEME2000(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
        return new SpacecraftState(orbit, mass).addAdditionalState(ADJOINT_NAME, adjoint);
    }
}
