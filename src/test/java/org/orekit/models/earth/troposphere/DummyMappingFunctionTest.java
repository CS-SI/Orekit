/* Copyright 2022-2025 Thales Alenia Space
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

public class DummyMappingFunctionTest extends AbstractMappingFunctionTest<DummyMappingFunction> {

    protected DummyMappingFunction buildMappingFunction() {
        return new DummyMappingFunction();
    }

    @Test
    public void testMappingFactors() {
        doTestMappingFactors(defaultDate, defaultPoint, defaultTrackingCoordinates,
                             1.0, 1.0);
    }

    @Test
    public void testFixedHeight() {
        // this function does *not* decrease with elevation
        // so we disable this test
    }

    @Test
    public void testDerivatives() {
        doTestDerivatives(1.0e-100, 1.0e-100, 1.0e-100, 1.0e-100, 1.0e-100);
    }

}
