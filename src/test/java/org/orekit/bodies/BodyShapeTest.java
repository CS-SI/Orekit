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
package org.orekit.bodies;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.models.earth.Geoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BodyShapeTest {

    @Test
    void testGetLongitude() {
        // GIVEN
        final Geoid bodyShape = mock();
        final Vector3D point = mock();
        final AbsoluteDate date = mock();
        final Frame frame = mock();
        final GeodeticPoint geodeticPoint = new GeodeticPoint(0., 1., 2);
        when(bodyShape.transform(point, frame, date)).thenReturn(geodeticPoint);
        when(bodyShape.getLongitude(point, frame, date)).thenCallRealMethod();
        // WHEN
        final double longitude = bodyShape.getLongitude(point, frame, date);
        // THEN
        assertEquals(longitude, geodeticPoint.getLongitude());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetLongitudeField() {
        // GIVEN
        final Geoid bodyShape = mock();
        final FieldVector3D<Binary64> point = mock();
        final FieldAbsoluteDate<Binary64> date = mock();
        final Frame frame = mock();
        final FieldGeodeticPoint<Binary64> geodeticPoint = new FieldGeodeticPoint<>(Binary64Field.getInstance(), new GeodeticPoint(0., 1., 2));
        when(bodyShape.transform(point, frame, date)).thenReturn(geodeticPoint);
        when(bodyShape.getLongitude(point, frame, date)).thenCallRealMethod();
        // WHEN
        final Binary64 longitude = bodyShape.getLongitude(point, frame, date);
        // THEN
        assertEquals(longitude, geodeticPoint.getLongitude());
    }
}
