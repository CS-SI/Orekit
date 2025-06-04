/* Copyright 2002-2025 CS GROUP
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
package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.TimeStampedGeodeticPoint;
import org.orekit.time.AbsoluteDate;

class TimeStampedGeodeticPointTest {

    @Test
    void testEquals() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedGeodeticPoint geo = new TimeStampedGeodeticPoint(date, 1.0, 2.0, 3.0);
        Assertions.assertEquals(geo, new TimeStampedGeodeticPoint(geo.getDate(), geo.getLatitude(), geo.getLongitude(), geo.getAltitude()));
        Assertions.assertNotEquals(geo, date);
        Assertions.assertNotEquals(geo, new TimeStampedGeodeticPoint(geo.getDate().shiftedBy(1), geo.getLatitude(), geo.getLongitude(), geo.getAltitude()));
    }

    @Test
    void testFullConstructor() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedGeodeticPoint geo = new TimeStampedGeodeticPoint(date, 1.0, 2.0, 3.0);
        Assertions.assertEquals(date, geo.getDate());
        Assertions.assertEquals(1.0, geo.getLatitude());
        Assertions.assertEquals(2.0, geo.getLongitude());
        Assertions.assertEquals(3.0, geo.getAltitude());
    }

    @Test
    void testGeodeticPointConstructor() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final GeodeticPoint geo = new GeodeticPoint(1.0, 2.0, 3.0);
        final TimeStampedGeodeticPoint timedGeo = new TimeStampedGeodeticPoint(date, geo);
        Assertions.assertEquals(date, timedGeo.getDate());
        Assertions.assertEquals(geo.getLatitude(), timedGeo.getLatitude());
        Assertions.assertEquals(geo.getLongitude(), timedGeo.getLongitude());
        Assertions.assertEquals(geo.getAltitude(), timedGeo.getAltitude());
    }

    @Test
    void testShiftedBy() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final GeodeticPoint geo = new GeodeticPoint(1.0, 2.0, 3.0);
        final TimeStampedGeodeticPoint timedGeo = new TimeStampedGeodeticPoint(date, geo);

        // Shift point in time
        final double dt = 3600;
        final AbsoluteDate newDate = date.shiftedBy(dt);
        final TimeStampedGeodeticPoint timedGeoShifted = timedGeo.shiftedBy(dt);

        // Check that shifted point has not moved but date has been updated
        Assertions.assertEquals(newDate, timedGeoShifted.getDate());
        Assertions.assertEquals(geo.getLatitude(), timedGeoShifted.getLatitude());
        Assertions.assertEquals(geo.getLongitude(), timedGeoShifted.getLongitude());
        Assertions.assertEquals(geo.getAltitude(), timedGeoShifted.getAltitude());
    }

    @Test
    void testToString() {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedGeodeticPoint geo = new TimeStampedGeodeticPoint(date, 1.0, 2.0, 3.0);
        Assertions.assertEquals("{date: 2000-01-01T11:58:55.816Z, lat: 57.29577951308232 deg, lon: 114.59155902616465 deg, alt: 3.0}", geo.toString());
    }
}
