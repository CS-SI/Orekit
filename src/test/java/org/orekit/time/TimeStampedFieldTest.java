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
package org.orekit.time;

import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeStampedFieldTest {

    private final Field<Binary64> field = Binary64Field.getInstance();

    @Test
    @DisplayName("Test constructor with non fielded date")
    void testConstructorWithNonFieldedDate() {
        // Given
        final AbsoluteDate date  = new AbsoluteDate();
        final Binary64     value = field.getOne();

        // When
        final TimeStampedField<Binary64> timeStampedField = new TimeStampedField<>(value, date);

        // Then
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, date), timeStampedField.getDate());
        Assertions.assertEquals(value, timeStampedField.getValue());
    }
}