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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.LutherIntegrator;
import org.hipparchus.ode.nonstiff.MidpointIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.conversion.ClassicalRungeKuttaFieldIntegratorBuilder;
import org.orekit.propagation.conversion.ClassicalRungeKuttaIntegratorBuilder;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.LutherIntegratorBuilder;
import org.orekit.propagation.conversion.MidpointIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class ShootingIntegrationSettingsFactoryTest {

    @Test
    void testGetMidpointIntegratorSettings() {
        // GIVEN
        final double expectedStep = 1.;
        // WHEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getMidpointIntegratorSettings(expectedStep);
        final ODEIntegratorBuilder builder = integrationSettings.getIntegratorBuilder();
        // THEN
        Assertions.assertInstanceOf(MidpointIntegratorBuilder.class, builder);
        final ODEIntegrator integrator = builder.buildIntegrator(buildOrbit(), OrbitType.CARTESIAN);
        Assertions.assertEquals(expectedStep, ((MidpointIntegrator) integrator).getDefaultStep());
    }

    @Test
    void testGetClassicalRungeKuttaIntegratorSettings() {
        // GIVEN
        final double expectedStep = 1.;
        // WHEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getClassicalRungeKuttaIntegratorSettings(expectedStep);
        final ODEIntegratorBuilder builder = integrationSettings.getIntegratorBuilder();
        // THEN
        Assertions.assertInstanceOf(ClassicalRungeKuttaIntegratorBuilder.class, builder);
        final ODEIntegrator integrator = builder.buildIntegrator(buildOrbit(), OrbitType.CARTESIAN);
        Assertions.assertEquals(expectedStep, ((ClassicalRungeKuttaIntegrator) integrator).getDefaultStep());
    }

    @Test
    void testGetClassicalRungeKuttaIntegratorSettingsField() {
        // GIVEN
        final double expectedStep = 1.;
        // WHEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getClassicalRungeKuttaIntegratorSettings(expectedStep);
        final FieldODEIntegratorBuilder<Complex> builder = integrationSettings.getFieldIntegratorBuilder(ComplexField.getInstance());
        // THEN
        Assertions.assertInstanceOf(ClassicalRungeKuttaFieldIntegratorBuilder.class, builder);
        final FieldOrbit<Complex> mockedFieldOrbit = mockFieldOrbit();
        final FieldODEIntegrator<Complex> integrator = builder.buildIntegrator(mockedFieldOrbit, OrbitType.CARTESIAN);
        Assertions.assertEquals(expectedStep, ((ClassicalRungeKuttaFieldIntegrator<Complex>) integrator).getDefaultStep().getReal());
    }

    @Test
    void testGetLutherIntegratorSettings() {
        // GIVEN
        final double expectedStep = 1.;
        // WHEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getLutherIntegratorSettings(expectedStep);
        final ODEIntegratorBuilder builder = integrationSettings.getIntegratorBuilder();
        // THEN
        Assertions.assertInstanceOf(LutherIntegratorBuilder.class, builder);
        final ODEIntegrator integrator = builder.buildIntegrator(buildOrbit(), OrbitType.CARTESIAN);
        Assertions.assertEquals(expectedStep, ((LutherIntegrator) integrator).getDefaultStep());
    }

    @SuppressWarnings("unchecked")
    private static FieldOrbit<Complex> mockFieldOrbit() {
        final FieldOrbit<Complex> mockedFieldOrbit = Mockito.mock(FieldOrbit.class);
        Mockito.when(mockedFieldOrbit.getA()).thenReturn(Complex.ONE);
        return mockedFieldOrbit;
    }

    @Test
    void testGetDormandPrince54IntegratorSettings() {
        //
        final ComplexField field = ComplexField.getInstance();
        // WHEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getDormandPrince54IntegratorSettings(1., 10.,
                        ToleranceProvider.of(1, 2));
        final FieldODEIntegratorBuilder<Complex> builder = integrationSettings.getFieldIntegratorBuilder(field);
        // THEN
        final FieldODEIntegrator<Complex> fieldIntegrator = builder.buildIntegrator(new FieldAbsolutePVCoordinates<>(FramesFactory.getGCRF(),
                FieldAbsoluteDate.getArbitraryEpoch(field), new FieldVector3D<>(field, Vector3D.PLUS_I),
                new FieldVector3D<>(field, Vector3D.MINUS_J)));
        Assertions.assertInstanceOf(DormandPrince54FieldIntegrator.class, fieldIntegrator);
    }

    @Test
    void testGetDormandPrince853IntegratorSettings() {
        //
        final ComplexField field = ComplexField.getInstance();
        // WHEN
        final ShootingIntegrationSettings integrationSettings = ShootingIntegrationSettingsFactory
                .getDormandPrince853IntegratorSettings(1., 10.,
                        ToleranceProvider.of(1, 2));
        final FieldODEIntegratorBuilder<Complex> builder = integrationSettings.getFieldIntegratorBuilder(field);
        // THEN
        final FieldODEIntegrator<Complex> fieldIntegrator = builder.buildIntegrator(new FieldAbsolutePVCoordinates<>(FramesFactory.getGCRF(),
                FieldAbsoluteDate.getArbitraryEpoch(field), new FieldVector3D<>(field, Vector3D.PLUS_I),
                new FieldVector3D<>(field, Vector3D.MINUS_J)));
        Assertions.assertInstanceOf(DormandPrince853FieldIntegrator.class, fieldIntegrator);
    }

    private CartesianOrbit buildOrbit() {
        return new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                                                               new Vector3D(1.0e7, 0, 0),
                                                               new Vector3D(5e3, 6e3, 100)),
                                  FramesFactory.getEME2000(),
                                  Constants.EIGEN5C_EARTH_MU);
    }

}
