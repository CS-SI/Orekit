/* Copyright 2002-2023 CS GROUP
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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;

public class SingleLayerModelMappingFunctionTest {

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testMappingFactor() {
        // Model
        final IonosphericMappingFunction model = new SingleLayerModelMappingFunction();
        // z = 70°
        final double factor70 = model.mappingFactor(FastMath.toRadians(20.0));
        Assertions.assertEquals(2.09, factor70, 0.01);
        // z = 75°
        final double factor75 = model.mappingFactor(FastMath.toRadians(15.0));
        Assertions.assertEquals(2.32, factor75, 0.01);
        // z = 80°
        final double factor80 = model.mappingFactor(FastMath.toRadians(10.0));
        Assertions.assertEquals(2.55, factor80, 0.01);
        // z = 85°
        final double factor85 = model.mappingFactor(FastMath.toRadians(5.0));
        Assertions.assertEquals(2.73, factor85, 0.01);
    }

    @Test
    public void testFieldMappingFactor() {
        doTestFieldMappingFactor(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldMappingFactor(final Field<T> field) {
        final T zero = field.getZero();
        // Model
        final IonosphericMappingFunction model = new SingleLayerModelMappingFunction();
        // z = 70°
        final T factor70 = model.mappingFactor(zero.add(FastMath.toRadians(20.0)));
        Assertions.assertEquals(2.09, factor70.getReal(), 0.01);
        // z = 75°
        final T factor75 = model.mappingFactor(zero.add(FastMath.toRadians(15.0)));
        Assertions.assertEquals(2.32, factor75.getReal(), 0.01);
        // z = 80°
        final T factor80 = model.mappingFactor(zero.add(FastMath.toRadians(10.0)));
        Assertions.assertEquals(2.55, factor80.getReal(), 0.01);
        // z = 85°
        final T factor85 = model.mappingFactor(zero.add(FastMath.toRadians(5.0)));
        Assertions.assertEquals(2.73, factor85.getReal(), 0.01);
    }

}
