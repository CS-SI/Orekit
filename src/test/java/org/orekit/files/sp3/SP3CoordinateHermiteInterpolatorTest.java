/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.sp3;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.time.AbstractTimeInterpolator;

public class SP3CoordinateHermiteInterpolatorTest {

    @Test
    public void testNoRates() throws IOException, URISyntaxException {
        final String ex = "/sp3/gbm18432.sp3.Z";

        final SP3Parser           parser      = new SP3Parser();
        final DataSource          compressed  = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3                 file        = parser.parse(new UnixCompressFilter().filter(compressed));
        final List<SP3Coordinate> coordinates = file.getEphemeris("G03").getSegments().get(0).getCoordinates();

        Assertions.assertEquals(288, coordinates.size());

        SP3CoordinateHermiteInterpolator interpolator =
                        new SP3CoordinateHermiteInterpolator(6,
                                                             AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                                             false);

        // check one of the sample points
        SP3Coordinate pointA = coordinates.get(117);
        SP3Coordinate interpolated = interpolator.interpolate(pointA.getDate(), coordinates);

        // interpolation adds derivatives that were not present in the file
        SP3TestUtils.checkEquals(pointA,
                                 new SP3Coordinate(interpolated.getDate(),
                                                   interpolated.getPosition(), interpolated.getPositionAccuracy(),
                                                   Vector3D.ZERO, interpolated.getVelocityAccuracy(),
                                                   interpolated.getClockCorrection(), interpolated.getClockAccuracy(),
                                                   0.0, interpolated.getClockRateAccuracy(),
                                                   false, false, false, false));
        Assertions.assertEquals(2836.973124,  interpolated.getVelocity().getNorm(), 1.0e-6);
        Assertions.assertEquals(1.126333e-11, interpolated.getClockRateChange(),    1.0e-17);

        // check between sample points
        SP3Coordinate pointB  = coordinates.get(118);
        SP3Coordinate between = interpolator.interpolate(pointA.getDate().shiftedBy(0.5 * file.getHeader().getEpochInterval()),
                                                         coordinates);
        Assertions.assertEquals(426076.123,
                                Vector3D.distance(pointA.getPosition(), between.getPosition()),
                                1.0e-3);
        Assertions.assertEquals(427183.849,
                                Vector3D.distance(pointB.getPosition(), between.getPosition()),
                                1.0e-3);
        Assertions.assertEquals(853194.644,
                                Vector3D.distance(pointA.getPosition(), pointB.getPosition()),
                                1.0e-3);

    }

    @Test
    public void testRates() throws IOException, URISyntaxException {
        final String ex = "/sp3/issue895-minutes-increment.sp3";

        final SP3Parser           parser      = new SP3Parser();
        final DataSource          source      = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3                 file        = parser.parse(source);
        final List<SP3Coordinate> coordinates = file.getEphemeris("L51").getSegments().get(0).getCoordinates();

        Assertions.assertEquals(91, coordinates.size());

        SP3CoordinateHermiteInterpolator interpolator =
                        new SP3CoordinateHermiteInterpolator(6,
                                                             AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                                             true);

        // check one of the sample points
        SP3Coordinate pointA = coordinates.get(54);
        SP3Coordinate interpolated = interpolator.interpolate(pointA.getDate(), coordinates);

        SP3TestUtils.checkEquals(pointA, interpolated);
        Assertions.assertEquals(5996.687746,                       interpolated.getVelocity().getNorm(), 1.0e-6);
        Assertions.assertEquals(SP3Utils.DEFAULT_CLOCK_RATE_VALUE,
                                SP3Utils.CLOCK_RATE_UNIT.fromSI(interpolated.getClockRateChange()),
                                1.0e-6);

        // check between sample points
        SP3Coordinate pointB  = coordinates.get(55);
        SP3Coordinate between = interpolator.interpolate(pointA.getDate().shiftedBy(0.5 * file.getHeader().getEpochInterval()),
                                                         coordinates);
        Assertions.assertEquals(359788.254,
                                Vector3D.distance(pointA.getPosition(), between.getPosition()),
                                1.0e-3);
        Assertions.assertEquals(359793.589,
                                Vector3D.distance(pointB.getPosition(), between.getPosition()),
                                1.0e-3);
        Assertions.assertEquals(719498.945,
                                Vector3D.distance(pointA.getPosition(), pointB.getPosition()),
                                1.0e-3);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
