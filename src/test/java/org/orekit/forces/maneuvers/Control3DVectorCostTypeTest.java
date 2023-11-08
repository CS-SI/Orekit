/* Copyright 2022-2023 Romain Serra
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

class Control3DVectorCostTypeTest {

    final Vector3D EXAMPLE_VECTOR = new Vector3D(-5., 2.4, -8.6);

    @Test
    public void testDoubleValue() {
        Assertions.assertEquals(Control3DVectorCostType.NONE.evaluate(EXAMPLE_VECTOR), 0.);
        Assertions.assertEquals(Control3DVectorCostType.ONE_NORM.evaluate(EXAMPLE_VECTOR), EXAMPLE_VECTOR.getNorm1());
        Assertions.assertEquals(Control3DVectorCostType.TWO_NORM.evaluate(EXAMPLE_VECTOR), EXAMPLE_VECTOR.getNorm());
        Assertions.assertEquals(Control3DVectorCostType.INF_NORM.evaluate(EXAMPLE_VECTOR), EXAMPLE_VECTOR.getNormInf());
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
        testTemplateNormField(field, Control3DVectorCostType.NONE);
        testTemplateNormField(field, Control3DVectorCostType.ONE_NORM);
        testTemplateNormField(field, Control3DVectorCostType.TWO_NORM);
        testTemplateNormField(field, Control3DVectorCostType.INF_NORM);
    }

    private <T extends CalculusFieldElement<T>> void testTemplateNormField(final Field<T> field,
                                                                           final Control3DVectorCostType control3DVectorCostType) {
        final FieldVector3D<T> fieldVector3D = new FieldVector3D<>(field, EXAMPLE_VECTOR);
        Assertions.assertEquals(control3DVectorCostType.evaluate(EXAMPLE_VECTOR), control3DVectorCostType.evaluate(fieldVector3D).getReal());
    }

}