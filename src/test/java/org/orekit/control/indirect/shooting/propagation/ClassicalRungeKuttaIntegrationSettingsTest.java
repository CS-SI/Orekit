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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.conversion.ClassicalRungeKuttaFieldIntegratorBuilder;
import org.orekit.propagation.conversion.ClassicalRungeKuttaIntegratorBuilder;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;

class ClassicalRungeKuttaIntegrationSettingsTest {

    @Test
    void testGetIntegratorBuilder() {
        // GIVEN
        final double expectedStep = 1.;
        final ClassicalRungeKuttaIntegrationSettings integrationSettings = new ClassicalRungeKuttaIntegrationSettings(expectedStep);
        // WHEN
        final ODEIntegratorBuilder builder = integrationSettings.getIntegratorBuilder();
        // THEN
        Assertions.assertInstanceOf(ClassicalRungeKuttaIntegratorBuilder.class, builder);
        final ODEIntegrator integrator = builder.buildIntegrator(Mockito.mock(Orbit.class),
                Mockito.mock(OrbitType.class));
        Assertions.assertEquals(expectedStep, ((ClassicalRungeKuttaIntegrator) integrator).getDefaultStep());
    }

    @Test
    void testFieldGetIntegratorBuilder() {
        // GIVEN
        final double expectedStep = 1.;
        final ClassicalRungeKuttaIntegrationSettings integrationSettings = new ClassicalRungeKuttaIntegrationSettings(expectedStep);
        // WHEN
        final FieldODEIntegratorBuilder<Complex> builder = integrationSettings.getFieldIntegratorBuilder(ComplexField.getInstance());
        // THEN
        Assertions.assertInstanceOf(ClassicalRungeKuttaFieldIntegratorBuilder.class, builder);
        final FieldOrbit<Complex> mockedFieldOrbit = mockFieldOrbit();
        final FieldODEIntegrator<Complex> integrator = builder.buildIntegrator(mockedFieldOrbit, Mockito.mock(OrbitType.class));
        Assertions.assertEquals(expectedStep, ((ClassicalRungeKuttaFieldIntegrator<Complex>) integrator).getDefaultStep().getReal());
    }

    @SuppressWarnings("unchecked")
    private static FieldOrbit<Complex> mockFieldOrbit() {
        final FieldOrbit<Complex> mockedFieldOrbit = Mockito.mock(FieldOrbit.class);
        Mockito.when(mockedFieldOrbit.getA()).thenReturn(Complex.ONE);
        return mockedFieldOrbit;
    }

}
