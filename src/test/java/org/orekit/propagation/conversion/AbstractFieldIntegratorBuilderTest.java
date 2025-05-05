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
package org.orekit.propagation.conversion;

import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;

class AbstractFieldIntegratorBuilderTest {

    @Test
    void testBuildIntegrator() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Vector3D.MINUS_I, Vector3D.MINUS_K);
        final double step = 10;
        final Field<Complex> field = ComplexField.getInstance();
        final FieldAbsolutePVCoordinates<Complex> fieldAbsolutePVCoordinates = new FieldAbsolutePVCoordinates<>(field, absolutePVCoordinates);
        final ClassicalRungeKuttaFieldIntegratorBuilder<Complex> builder = new ClassicalRungeKuttaFieldIntegratorBuilder<>(field.getOne().newInstance(step));
        // WHEN
        final FieldODEIntegrator<Complex> integrator = builder.buildIntegrator(fieldAbsolutePVCoordinates);
        // THEN
        final ClassicalRungeKuttaFieldIntegrator<Complex> actualIntegrator = (ClassicalRungeKuttaFieldIntegrator<Complex>) integrator;
        final CartesianOrbit orbit = new CartesianOrbit(absolutePVCoordinates.getPVCoordinates(), absolutePVCoordinates.getFrame(),
                absolutePVCoordinates.getDate(), 1.);
        final FieldCartesianOrbit<Complex> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        final ClassicalRungeKuttaFieldIntegrator<Complex> expectedIntegrator = (ClassicalRungeKuttaFieldIntegrator<Complex>) builder.buildIntegrator(fieldOrbit, OrbitType.CARTESIAN);
        Assertions.assertEquals(expectedIntegrator.getCurrentSignedStepsize(), actualIntegrator.getCurrentSignedStepsize());
    }

}
