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
package org.orekit.propagation.conversion;

import org.hipparchus.complex.Complex;
import org.hipparchus.ode.nonstiff.LutherIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LutherFieldIntegratorBuilderTest {

    @Test
    void testToODEIntegratorBuilder() {
        // GIVEN
        final LutherFieldIntegratorBuilder<Complex> fieldIntegratorBuilder = new LutherFieldIntegratorBuilder<>(Complex.ONE);
        // WHEN
        final LutherIntegratorBuilder integratorBuilder = fieldIntegratorBuilder.toODEIntegratorBuilder();
        // THEN
        final LutherIntegrator integrator = (LutherIntegrator) integratorBuilder.buildIntegrator(null, null);
        Assertions.assertEquals(fieldIntegratorBuilder.getStep(), integrator.getDefaultStep());
    }
}
