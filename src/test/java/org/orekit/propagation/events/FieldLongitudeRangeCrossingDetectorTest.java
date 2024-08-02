/* Copyright 2023-2024 Alberto Ferrero
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Alberto Ferrero licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.orekit.orbits.PositionAngleType.MEAN;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/** Unit tests for {@link FieldLongitudeRangeCrossingDetector}. */
class FieldLongitudeRangeCrossingDetectorTest {

    /**
     * Arbitrary Field.
     */
    private static final Binary64Field field = Binary64Field.getInstance();


    @Test
    void testRegularCrossing() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        FieldLongitudeRangeCrossingDetector<Binary64> d =
            new FieldLongitudeRangeCrossingDetector<>(v(60.0), v(1.e-6), earth,
                FastMath.toRadians(10.0),
                FastMath.toRadians(18.0)).
                withHandler(new FieldContinueOnEvent<>());

        assertEquals(60.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        assertEquals(1.0e-6, d.getThreshold().getReal(), 1.0e-15);
        assertEquals(10.0, FastMath.toDegrees(d.getFromLongitude()), 1.0e-14);
        assertEquals(18.0, FastMath.toDegrees(d.getToLongitude()), 1.0e-14);
        assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());
        assertSame(earth, d.getBody());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<Binary64> orbit = new FieldEquinoctialOrbit<>(
            new FieldPVCoordinates<>(v(1), new PVCoordinates(position, velocity)),
            FramesFactory.getEME2000(), date,
            v(Constants.EIGEN5C_EARTH_MU));

        FieldPropagator<Binary64> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit,
                Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                v(Constants.EIGEN5C_EARTH_MU),
                Constants.EIGEN5C_EARTH_C20,
                Constants.EIGEN5C_EARTH_C30,
                Constants.EIGEN5C_EARTH_C40,
                Constants.EIGEN5C_EARTH_C50,
                Constants.EIGEN5C_EARTH_C60);

        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        for (FieldEventsLogger.FieldLoggedEvent<Binary64> e : logger.getLoggedEvents()) {
            FieldSpacecraftState<Binary64> state = e.getState();
            double longitude = earth.transform(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                earth.getBodyFrame(), date).getLongitude().getReal();
            if (e.isIncreasing()) {
                // retrograde orbit, enter
                assertEquals(18.0, FastMath.toDegrees(longitude), 3.5e-7);
            } else {
                // retrograde orbit, exit
                assertEquals(10.0, FastMath.toDegrees(longitude), 3.5e-7);
            }
            assertEquals(28, logger.getLoggedEvents().size());
        }
    }

    @Test
    void testZigZag() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        FieldLongitudeRangeCrossingDetector<Binary64> d =
            new FieldLongitudeRangeCrossingDetector<>(v(600.0), v(1.e-6), earth,
                FastMath.toRadians(-120.0),
                FastMath.toRadians(-100.0)).
                withHandler(new FieldContinueOnEvent<>());

        assertEquals(600.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        assertEquals(1.0e-6, d.getThreshold().getReal(), 1.0e-15);
        assertEquals(-120.0, FastMath.toDegrees(d.getFromLongitude()), 1.0e-8);
        assertEquals(-100.0, FastMath.toDegrees(d.getToLongitude()), 1.0e-8);
        assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldOrbit<Binary64> orbit =
            new FieldKeplerianOrbit<>(v(26464560.0), v(0.8311), v(0.122138), v(3.10686), v(1.00681),
                v(0.048363), MEAN,
                FramesFactory.getEME2000(),
                date,
                v(Constants.EIGEN5C_EARTH_MU));


        FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);

        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(orbit.getDate().shiftedBy(1 * Constants.JULIAN_DAY));
        assertEquals(4, logger.getLoggedEvents().size());

        // eccentric orbit, at apogee Earth rotation makes as reversed effect
        double[] expectedLongitude = new double[]{d.getFromLongitude(), d.getToLongitude(),
            d.getToLongitude(), d.getFromLongitude()};
        for (int i = 0; i < 4; ++i) {
            FieldSpacecraftState<Binary64> state = logger.getLoggedEvents().get(i).getState();
            FieldGeodeticPoint<Binary64> gp = earth.transform(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                earth.getBodyFrame(), date);
            assertEquals(expectedLongitude[i], gp.getLongitude().getReal(), 1.2e-9);
        }

    }

    /**
     * Convert double to field value.
     *
     * @param value to box.
     * @return boxed value.
     */
    private static Binary64 v(double value) {
        return new Binary64(value);
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

