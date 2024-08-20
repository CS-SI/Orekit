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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class UnboundedCartesianEnergyTest {

    @Test
    void testGetMassFlowRateFactor() {
        // GIVEN
        final double expectedRateFactor = 1.;
        final UnboundedCartesianEnergy unboundedCartesianEnergy = new UnboundedCartesianEnergy("", expectedRateFactor);
        // WHEN
        final double actualRateFactor = unboundedCartesianEnergy.getMassFlowRateFactor();
        // THEN
        Assertions.assertEquals(expectedRateFactor, actualRateFactor);
    }

    @Test
    void testGetFieldThrustAccelerationVectorFieldFactor() {
        // GIVEN
        final double massRateFactor = 1.;
        final UnboundedCartesianEnergy unboundedCartesianEnergy = new UnboundedCartesianEnergy("", massRateFactor);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0, 0.0);
        fieldAdjoint[4] = new Complex(2.0, 0.0);
        final Complex mass = new Complex(3., 0.);
        // WHEN
        final FieldVector3D<Complex> fieldThrustVector = unboundedCartesianEnergy.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final double[] adjoint = new double[7];
        for (int i = 0; i < adjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final Vector3D thrustVector = unboundedCartesianEnergy.getThrustAccelerationVector(adjoint, mass.getReal());
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

    @Test
    void testDerivatives() {
        // GIVEN
        final String name = "a";
        final UnboundedCartesianEnergy energy = new UnboundedCartesianEnergy(name, 1.);
        final FieldCartesianAdjointDerivativesProvider<Gradient> fieldAdjointDerivativesProvider = new FieldCartesianAdjointDerivativesProvider<>(energy);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, 10.).addAdditionalState(name, 1., 2., 3., 4., 5., 6., 7.);
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(GradientField.getField(1), state);
        // WHEN
        final FieldCombinedDerivatives<Gradient>fieldCombinedDerivatives = fieldAdjointDerivativesProvider.combinedDerivatives(fieldState);
        // THEN
        final CartesianAdjointDerivativesProvider adjointDerivativesProvider = new CartesianAdjointDerivativesProvider(energy);
        final CombinedDerivatives combinedDerivatives = adjointDerivativesProvider.combinedDerivatives(state);
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(combinedDerivatives.getMainStateDerivativesIncrements()[i],
                    fieldCombinedDerivatives.getMainStateDerivativesIncrements()[i].getReal());
        }
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(combinedDerivatives.getAdditionalDerivatives()[i],
                    fieldCombinedDerivatives.getAdditionalDerivatives()[i].getReal(), 1e-12);
        }
    }

}
