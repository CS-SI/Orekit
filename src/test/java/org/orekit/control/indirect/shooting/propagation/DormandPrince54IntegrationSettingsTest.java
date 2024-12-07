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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.conversion.DormandPrince54FieldIntegratorBuilder;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAbsolutePVCoordinates;

class DormandPrince54IntegrationSettingsTest {

    @Test
    void testFieldGetIntegratorBuilder() {
        // GIVEN
        final DormandPrince54IntegrationSettings integrationSettings = new DormandPrince54IntegrationSettings(1., 10.,
                ToleranceProvider.of(1, 2));
        final ComplexField field = ComplexField.getInstance();
        // WHEN
        final DormandPrince54FieldIntegratorBuilder<Complex> builder = integrationSettings.getFieldIntegratorBuilder(field);
        // THEN
        final FieldODEIntegrator<Complex> fieldIntegrator = builder.buildIntegrator(new FieldAbsolutePVCoordinates<>(FramesFactory.getGCRF(),
                FieldAbsoluteDate.getArbitraryEpoch(field), new FieldVector3D<>(field, Vector3D.PLUS_I),
                new FieldVector3D<>(field, Vector3D.MINUS_J)));
        Assertions.assertInstanceOf(DormandPrince54FieldIntegrator.class, fieldIntegrator);
    }

}
