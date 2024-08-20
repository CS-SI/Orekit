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

import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BoundedCartesianEnergyTest {

    @Test
    void getEventDetectorsSizeAndActionTest() {
        // GIVEN
        final double maximumThrustMagnitude = 1.;
        final double massFlowRateFactor = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massFlowRateFactor,
                maximumThrustMagnitude);
        // WHEN
        final Stream<EventDetector> eventDetectorStream = boundedCartesianEnergy.getEventDetectors();
        // THEN
        final List<EventDetector> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, eventDetectors.size());
        Assertions.assertInstanceOf(BoundedCartesianEnergy.EnergyCostAdjointSingularityDetector.class, eventDetectors.get(0));
        final BoundedCartesianEnergy.EnergyCostAdjointSingularityDetector singularityDetector =
                (BoundedCartesianEnergy.EnergyCostAdjointSingularityDetector) eventDetectors.get(0);
        Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, true));
    }

    @Test
    void getFieldEventDetectorsSizeAndActionTest() {
        // GIVEN
        final double maximumThrustMagnitude = 1.;
        final double massFlowRateFactor = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massFlowRateFactor,
                maximumThrustMagnitude);
        final Field<Complex> field = ComplexField.getInstance();
        // WHEN
        final Stream<FieldEventDetector<Complex>> eventDetectorStream = boundedCartesianEnergy.getFieldEventDetectors(field);
        // THEN
        final List<FieldEventDetector<Complex>> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, eventDetectors.size());
        Assertions.assertInstanceOf(BoundedCartesianEnergy.FieldEnergyCostAdjointSingularityDetector.class, eventDetectors.get(0));
        final BoundedCartesianEnergy.FieldEnergyCostAdjointSingularityDetector<Complex> singularityDetector =
                (BoundedCartesianEnergy.FieldEnergyCostAdjointSingularityDetector<Complex>) eventDetectors.get(0);
        Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, true));
    }

    @Test
    void getFieldEventDetectorsTest() {
        // GIVEN
        final double maximumThrustMagnitude = 1.;
        final double massFlowRateFactor = 2.;
        final String name = "a";
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy(name, massFlowRateFactor,
                maximumThrustMagnitude);
        final Field<Complex> field = ComplexField.getInstance();
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7e6, 1e3, 0), new Vector3D(10., 7e3, -200)),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, 10.).addAdditionalState(name, 1., 2., 3., 4., 5., 6., 7.);
        // WHEN
        final Stream<FieldEventDetector<Complex>> fieldEventDetectorStream = boundedCartesianEnergy.getFieldEventDetectors(field);
        // THEN
        final List<FieldEventDetector<Complex>> fieldEventDetectors = fieldEventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, fieldEventDetectors.size());
        Assertions.assertInstanceOf(BoundedCartesianEnergy.FieldEnergyCostAdjointSingularityDetector.class, fieldEventDetectors.get(0));
        final BoundedCartesianEnergy.FieldEnergyCostAdjointSingularityDetector<Complex> fieldSingularityDetector =
                (BoundedCartesianEnergy.FieldEnergyCostAdjointSingularityDetector<Complex>) fieldEventDetectors.get(0);
        final Complex gValue = fieldSingularityDetector.g(new FieldSpacecraftState<>(field, state));
        final List<EventDetector> eventDetectors = boundedCartesianEnergy.getEventDetectors().collect(Collectors.toList());
        final BoundedCartesianEnergy.EnergyCostAdjointSingularityDetector singularityDetector =
                (BoundedCartesianEnergy.EnergyCostAdjointSingularityDetector) eventDetectors.get(0);
        final double expectedG = singularityDetector.g(state);
        Assertions.assertEquals(expectedG, gValue.getReal());
    }

    @Test
    void testGetFieldThrustAccelerationVectorFieldFactor() {
        // GIVEN
        final double massRateFactor = 1.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                2.);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0, 0.0);
        fieldAdjoint[4] = new Complex(2.0, 0.0);
        final Complex mass = new Complex(3., 0.);
        // WHEN
        final FieldVector3D<Complex> fieldThrustVector = boundedCartesianEnergy.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final double[] adjoint = new double[7];
        for (int i = 0; i < adjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final Vector3D thrustVector = boundedCartesianEnergy.getThrustAccelerationVector(adjoint, mass.getReal());
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

    @Test
    void testUpdateDerivatives() {
        // GIVEN
        final double massRateFactor = 1.;
        final double maximumThrustMagnitude = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                maximumThrustMagnitude);
        final double[] adjoint = new double[7];
        adjoint[3] = 1e-2;
        adjoint[4] = 2e-2;
        adjoint[5] = 3e-2;
        adjoint[6] = 1.;
        final double mass = 3e-3;
        final double[] derivatives = new double[7];
        // WHEN
        boundedCartesianEnergy.updateAdjointDerivatives(adjoint, mass, derivatives);
        // THEN
        Assertions.assertNotEquals(0., derivatives[6]);
    }

    @Test
    void testGetFieldThrustAccelerationVectorVersusUnbounded() {
        // GIVEN
        final double massRateFactor = 1.;
        final double maximumThrustMagnitude = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                maximumThrustMagnitude);
        final double[] adjoint = new double[7];
        adjoint[3] = 1e-2;
        adjoint[4] = 2e-2;
        adjoint[5] = 3e-2;
        final double mass = 3e-3;
        // WHEN
        final Vector3D thrustVector = boundedCartesianEnergy.getThrustAccelerationVector(adjoint, mass);
        // THEN
        final Vector3D expectedThrustVector = new UnboundedCartesianEnergy("", massRateFactor)
                .getThrustAccelerationVector(adjoint, mass).scalarMultiply(maximumThrustMagnitude);
        Assertions.assertEquals(0., expectedThrustVector.subtract(thrustVector).getNorm(), 1e-12);
    }

    @Test
    void testFieldUpdateDerivatives() {
        // GIVEN
        final double massRateFactor = 1.;
        final double maximumThrustMagnitude = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                maximumThrustMagnitude);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0e-3, 0.0);
        fieldAdjoint[4] = new Complex(2.0e-3, 0.0);
        fieldAdjoint[6] = Complex.ONE;
        final Complex mass = new Complex(3.e-3, 0.);
        final Complex[] derivatives = MathArrays.buildArray(field, 7);
        // WHEN
        boundedCartesianEnergy.updateFieldAdjointDerivatives(fieldAdjoint, mass, derivatives);
        // THEN
        Assertions.assertNotEquals(0., derivatives[6].getReal());
    }

    @Test
    void testGetFieldThrustAccelerationVectorFieldVersusUnbounded() {
        // GIVEN
        final double massRateFactor = 1.;
        final double maximumThrustMagnitude = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massRateFactor,
                maximumThrustMagnitude);
        final ComplexField field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 7);
        fieldAdjoint[3] = new Complex(1.0e-3, 0.0);
        fieldAdjoint[4] = new Complex(2.0e-3, 0.0);
        final Complex mass = new Complex(3.e-3, 0.);
        // WHEN
        final FieldVector3D<Complex> fieldThrustVector = boundedCartesianEnergy.getFieldThrustAccelerationVector(fieldAdjoint, mass);
        // THEN
        final FieldVector3D<Complex> expectedThrustVector = new UnboundedCartesianEnergy("", massRateFactor)
                .getFieldThrustAccelerationVector(fieldAdjoint, mass).scalarMultiply(maximumThrustMagnitude);
        Assertions.assertEquals(expectedThrustVector, fieldThrustVector);
    }
}
