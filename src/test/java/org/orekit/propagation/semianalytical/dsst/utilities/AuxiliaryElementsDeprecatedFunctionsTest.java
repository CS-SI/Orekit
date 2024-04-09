/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;

/** Test for deprecated functions of (Field)AuxiliaryElements.
 * 
 * @deprecated should be removed in 13.0
 * @since 12.1
 */
public class AuxiliaryElementsDeprecatedFunctionsTest {

    @Test
    @Deprecated
    public void testDeprecatedFunctions() {
       
        // GIVEN
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential");
        
        final EquinoctialOrbit orbit = new EquinoctialOrbit(context.initialOrbit);
        
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldEquinoctialOrbit<Binary64> fieldOrbit = new FieldEquinoctialOrbit<>(field, orbit);
        
        // WHEN
        final AuxiliaryElements aux = new AuxiliaryElements(context.initialOrbit, 1);
        final FieldAuxiliaryElements<Binary64> fieldAux = new FieldAuxiliaryElements<>(fieldOrbit, 1);
        
        // THEN
        Assertions.assertEquals(aux.getVectorF().getZ(), aux.getAlpha(), 0.);
        Assertions.assertEquals(aux.getVectorG().getZ(), aux.getBeta(), 0.);
        Assertions.assertEquals(aux.getVectorW().getZ(), aux.getGamma(), 0.);
        
        Assertions.assertEquals(fieldAux.getVectorF().getZ().getReal(), fieldAux.getAlpha().getReal(), 0.);
        Assertions.assertEquals(fieldAux.getVectorG().getZ().getReal(), fieldAux.getBeta().getReal(), 0.);
        Assertions.assertEquals(fieldAux.getVectorW().getZ().getReal(), fieldAux.getGamma().getReal(), 0.);
    }
}
