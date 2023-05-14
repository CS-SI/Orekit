/* Copyright 2002-2023 Romain Serra
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
package org.orekit.forces.maneuvers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ControlVector3DNormTypeTest {

    final Vector3D EXAMPLE_VECTOR = new Vector3D(-5., 2.4, -8.6);

    @Test
    public void testDoubleValue() {
        Assertions.assertEquals(ControlVector3DNormType.NORM_1.evaluate(EXAMPLE_VECTOR), EXAMPLE_VECTOR.getNorm1());
        Assertions.assertEquals(ControlVector3DNormType.NORM_2.evaluate(EXAMPLE_VECTOR), EXAMPLE_VECTOR.getNorm());
        Assertions.assertEquals(ControlVector3DNormType.NORM_INF.evaluate(EXAMPLE_VECTOR), EXAMPLE_VECTOR.getNormInf());
    }

    @Test
    public void testGradient() {
        final GradientField gradientField = GradientField.getField(2);
        testTemplateField(gradientField);
    }

    @Test
    public void testComplex() {
        final ComplexField complexField = ComplexField.getInstance();
        testTemplateField(complexField);
    }

    private <T extends CalculusFieldElement<T>> void testTemplateField(final Field<T> field) {
        testTemplateNormField(field, ControlVector3DNormType.NORM_1);
        testTemplateNormField(field, ControlVector3DNormType.NORM_2);
        testTemplateNormField(field, ControlVector3DNormType.NORM_INF);
    }

    private <T extends CalculusFieldElement<T>> void testTemplateNormField(final Field<T> field,
                                                                           final ControlVector3DNormType controlVector3DNormType) {
        final FieldVector3D<T> fieldVector3D = new FieldVector3D<>(field, EXAMPLE_VECTOR);
        Assertions.assertEquals(controlVector3DNormType.evaluate(EXAMPLE_VECTOR), controlVector3DNormType.evaluate(fieldVector3D).getReal());
    }

}