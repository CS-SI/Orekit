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
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnboundedCartesianEnergyNeglectingMassTest {

    @Test
    void testGetHamiltonianContribution() {
        // GIVEN
        final UnboundedCartesianEnergy mockedEnergy = Mockito.mock(UnboundedCartesianEnergy.class);
        final Vector3D vector = new Vector3D(1.0, 2.0, 3.0);
        final double mass = 1.;
        final double[] adjoint = new double[6];
        Mockito.when(mockedEnergy.getThrustAccelerationVector(adjoint, mass)).thenReturn(vector);
        Mockito.when(mockedEnergy.getHamiltonianContribution(adjoint, mass)).thenCallRealMethod();
        // WHEN
        final double contribution = mockedEnergy.getHamiltonianContribution(adjoint, mass);
        // THEN
        Assertions.assertEquals(-vector.getNormSq() * 0.5, contribution);
    }

    @Test
    void testGetFieldHamiltonianContribution() {
        // GIVEN
        final UnboundedCartesianEnergy mockedEnergy = Mockito.mock(UnboundedCartesianEnergy.class);
        final Vector3D vector = new Vector3D(1.0, 2.0, 3.0);
        final Field<Complex> field = ComplexField.getInstance();
        final FieldVector3D<Complex> fieldVector3D = new FieldVector3D<>(field, vector);
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Complex fieldMass = Complex.ONE;
        final double mass = fieldMass.getReal();
        final double[] adjoint = new double[fieldAdjoint.length];
        Mockito.when(mockedEnergy.getFieldThrustAccelerationVector(fieldAdjoint, fieldMass)).thenReturn(fieldVector3D);
        Mockito.when(mockedEnergy.getThrustAccelerationVector(adjoint, mass)).thenReturn(vector);
        Mockito.when(mockedEnergy.getFieldHamiltonianContribution(fieldAdjoint, fieldMass)).thenCallRealMethod();
        Mockito.when(mockedEnergy.getHamiltonianContribution(adjoint, mass)).thenCallRealMethod();
        // WHEN
        final Complex fieldContribution = mockedEnergy.getFieldHamiltonianContribution(fieldAdjoint, fieldMass);
        // THEN
        final double contribution = mockedEnergy.getHamiltonianContribution(adjoint, mass);
        Assertions.assertEquals(contribution, fieldContribution.getReal());
    }

    @Test
    void testGetFieldThrustAccelerationVector() {
        // GIVEN
        final UnboundedCartesianEnergyNeglectingMass energyNeglectingMass = new UnboundedCartesianEnergyNeglectingMass("");
        final Binary64[] adjoint = MathArrays.buildArray(Binary64Field.getInstance(), 6);
        adjoint[3] = Binary64.ONE;
        // WHEN
        final FieldVector3D<Binary64> fieldThrustVector = energyNeglectingMass.getFieldThrustAccelerationVector(adjoint, Binary64.ONE);
        // THEN
        final Vector3D thrustVector = energyNeglectingMass.getThrustAccelerationVector(new double[] { 0., 0., 0., 1., 0., 0.}, 1.);
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

}
