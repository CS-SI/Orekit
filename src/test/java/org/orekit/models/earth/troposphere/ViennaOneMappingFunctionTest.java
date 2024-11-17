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
package org.orekit.models.earth.troposphere;

import org.junit.jupiter.api.Test;
import org.orekit.time.TimeScalesFactory;

public class ViennaOneMappingFunctionTest extends AbstractMappingFunctionTest<ViennaOne> {

    protected ViennaOne buildMappingFunction() {
        return new ViennaOne(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
                             new ConstantAzimuthalGradientProvider(null),
                             new ConstantTroposphericModel(new TroposphericDelay(2.0966, 0.2140, 0, 0)),
                             TimeScalesFactory.getUTC());
    }

    @Test
    public void testMappingFactors() {
        doTestMappingFactors(3.425088, 3.448300);
    }

    @Override
    public void testDerivatives() {
        doTestDerivatives(1.0e-100, 1.0e-100, 1.0e-100, 1.0e-100, 1.0e-100);
    }

}
