/* Copyright 2022-2026 Romain Serra
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
package org.orekit.time.clocks;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.time.FieldAbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantFieldClockModelTest {

    @Test
    void testGetOffset() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> t0 = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final ConstantFieldClockModel<Binary64> constantFieldClockModel = new ConstantFieldClockModel<>(t0, Binary64.ONE);
        // WHEN
        final FieldClockOffset<Binary64> offset = constantFieldClockModel.getOffset(t0);
        // THEN
        assertEquals(1., offset.getBias().getReal());
        assertEquals(0., offset.getRate().getReal());
        assertEquals(0., offset.getAcceleration().getReal());
    }
}
