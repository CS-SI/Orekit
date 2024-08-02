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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EulerTest {

    // This test was added to increase overall conditions coverage in the scope of issue 1453
    @Test
    void testIssue1453() {
        // GIVEN
        final Euler euler = new Euler();

        final String KEY_ANGLES_V1 = "{X|Y|Z}_ANGLE";
        final String KEY_ANGLES_V2 = "ANGLE_{1|2|3}";

        final String KEY_RATES_V1 = "{X|Y|Z}_RATES";
        final String KEY_RATES_V2 = "ANGLE_{1|2|3}_DOT";

        // WHEN & THEN
        // Assert validation method
        // Assert thrown exceptions for empty angles depending on version
        assertThrows(OrekitException.class, () -> euler.validate(1),
                     String.format(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY.getSourceString(), KEY_ANGLES_V1));
        assertThrows(OrekitException.class, () -> euler.validate(2),
                     String.format(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY.getSourceString(), KEY_RATES_V2));

        // Assert thrown exceptions when no angles and rates are defined depending on version
        assertThrows(OrekitException.class, () -> euler.validate(1),
                     String.format(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY.getSourceString(), KEY_RATES_V1 + "/" + KEY_RATES_V1));
        assertThrows(OrekitException.class, () -> euler.validate(2),
                     String.format(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY.getSourceString(), KEY_ANGLES_V2));

        // Assert thrown exceptions for empty rates depending on version
        euler.setIndexedRotationAngle(0, 10);
        euler.setIndexedRotationAngle(1, 11);
        euler.setIndexedRotationAngle(2, 12);

        assertThrows(OrekitException.class, () -> euler.validate(1),
                     String.format(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY.getSourceString(), KEY_RATES_V1));
        assertThrows(OrekitException.class, () -> euler.validate(2),
                     String.format(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY.getSourceString(), KEY_RATES_V2));

        // Assert labeled rotation rate setting method
        euler.setEulerRotSeq(RotationOrder.XYZ);
        euler.setIndexedRotationRate(0,Double.NaN);
        euler.setIndexedRotationRate(1,-2);
        euler.setIndexedRotationRate(2,-3);

        euler.setLabeledRotationRate('A', 1);
        euler.setLabeledRotationRate('X', 4);
        euler.setLabeledRotationRate('X', 5);

        assertEquals(4, euler.getRotationRates()[0]);
        assertEquals(-2, euler.getRotationRates()[1]);
        assertEquals(-3, euler.getRotationRates()[2]);
    }

}
