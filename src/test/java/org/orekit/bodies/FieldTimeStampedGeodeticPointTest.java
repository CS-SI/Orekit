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
package org.orekit.bodies;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class FieldTimeStampedGeodeticPointTest {

    @Test
    void testConstructorFromNonField() {
        // GIVEN
        final GeodeticPoint point = new GeodeticPoint(1, 2, 3);
        final Binary64Field field = Binary64Field.getInstance();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final FieldTimeStampedGeodeticPoint<Binary64> fieldTimeStampedGeodeticPoint = new FieldTimeStampedGeodeticPoint<>(field,
                new TimeStampedGeodeticPoint(date, point));
        // THEN
        assertEquals(date, fieldTimeStampedGeodeticPoint.getDate().toAbsoluteDate());
        assertEquals(point, fieldTimeStampedGeodeticPoint.toGeodeticPoint());
    }

    @Test
    void testEquals() {
        // GIVEN
        final GeodeticPoint point = new GeodeticPoint(1, 2, 3);
        final Binary64Field field = Binary64Field.getInstance();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final FieldTimeStampedGeodeticPoint<Binary64> fieldTimeStampedGeodeticPoint = new FieldTimeStampedGeodeticPoint<>(new FieldAbsoluteDate<>(field, date),
                new FieldGeodeticPoint<>(field, point));
        // THEN
        assertNotEquals(fieldTimeStampedGeodeticPoint, point);
        assertEquals(fieldTimeStampedGeodeticPoint, new FieldTimeStampedGeodeticPoint<>(fieldTimeStampedGeodeticPoint.getDate(),
                fieldTimeStampedGeodeticPoint.getLatitude(), fieldTimeStampedGeodeticPoint.getLongitude(), fieldTimeStampedGeodeticPoint.getAltitude()));
    }
}
