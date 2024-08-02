/* Copyright 2002-2024 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;

import java.util.PrimitiveIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Random;
import java.util.stream.DoubleStream;

/** Unit tests for {@link ConstantPVCoordinatesProvider}. */
class ConstantPVCoordinatesProviderTest {
    /** Set up test data. */
    @BeforeEach
    void setup() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void verifyEllipsoidLocation() {
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid body = ReferenceEllipsoid.getWgs84(itrf);

        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(39.952330), FastMath.toRadians(-75.16379), 12.192);

        final Vector3D posItrf = body.transform(point);
        final Vector3D velItrf = Vector3D.ZERO;
        final PVCoordinates pvItrf = new PVCoordinates(posItrf, velItrf);

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame eme2000 = FramesFactory.getEME2000();

        final UTCScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate epoch = new AbsoluteDate(DateTimeComponents.parseDateTime("2022-06-01T10:35:00Z"), utc);

        final PVCoordinatesProvider pvProv = new ConstantPVCoordinatesProvider(point, body);

        // verify at epoch
        final TimeStampedPVCoordinates tpvItrf = pvProv.getPVCoordinates(epoch, itrf);
        assertEquals(epoch, tpvItrf.getDate());
        assertEquals(tpvItrf.getPosition(), posItrf);
        assertEquals(tpvItrf.getVelocity(), velItrf);
        assertEquals(Vector3D.ZERO, tpvItrf.getAcceleration());

        final PVCoordinates pvGcrf = itrf.getTransformTo(gcrf, epoch).transformPVCoordinates(pvItrf);
        final TimeStampedPVCoordinates tpvGcrf = pvProv.getPVCoordinates(epoch, gcrf);
        assertEquals(epoch, tpvGcrf.getDate());
        assertEquals(pvGcrf.getPosition(), tpvGcrf.getPosition());
        assertEquals(pvGcrf.getVelocity(), tpvGcrf.getVelocity());
        assertEquals(pvGcrf.getAcceleration(), tpvGcrf.getAcceleration());

        final PVCoordinates pvEme2000 = itrf.getTransformTo(eme2000, epoch).transformPVCoordinates(pvItrf);
        final TimeStampedPVCoordinates tpvEME2000 = pvProv.getPVCoordinates(epoch, eme2000);
        assertEquals(epoch, tpvEME2000.getDate());
        assertEquals(pvEme2000.getPosition(), tpvEME2000.getPosition());
        assertEquals(pvEme2000.getVelocity(), tpvEME2000.getVelocity());
        assertEquals(pvEme2000.getAcceleration(), tpvEME2000.getAcceleration());

        final Random rand = new Random();
        final DoubleStream stream = rand.doubles(1., 604800.); // stream of seconds within a week
        final PrimitiveIterator.OfDouble iter = stream.limit(100).iterator();
        for (int i = 0; i < 100; i++) {
            final AbsoluteDate date = epoch.shiftedBy(iter.nextDouble());

            // verify itrf
            final TimeStampedPVCoordinates actualItrf = pvProv.getPVCoordinates(date, itrf);
            assertEquals(date, actualItrf.getDate());
            assertEquals(posItrf, actualItrf.getPosition());
            assertEquals(velItrf, actualItrf.getVelocity());
            assertEquals(Vector3D.ZERO, actualItrf.getAcceleration());

            // verify gcrf
            final PVCoordinates expectedGcrf = itrf.getTransformTo(gcrf, date).transformPVCoordinates(pvItrf);
            final TimeStampedPVCoordinates actualGcrf = pvProv.getPVCoordinates(date, gcrf);
            assertEquals(date, actualGcrf.getDate());
            assertEquals(expectedGcrf.getPosition(), actualGcrf.getPosition());
            assertEquals(expectedGcrf.getVelocity(), actualGcrf.getVelocity());
            assertEquals(expectedGcrf.getAcceleration(), actualGcrf.getAcceleration());

            // verify eme2000
            final PVCoordinates expectedEme2000 = itrf.getTransformTo(eme2000, date).transformPVCoordinates(pvItrf);
            final TimeStampedPVCoordinates actualEme2000 = pvProv.getPVCoordinates(date, eme2000);
            assertEquals(date, actualEme2000.getDate());
            assertEquals(expectedEme2000.getPosition(), actualEme2000.getPosition());
            assertEquals(expectedEme2000.getVelocity(), actualEme2000.getVelocity());
            assertEquals(expectedEme2000.getAcceleration(), actualEme2000.getAcceleration());
        }
    }
}
