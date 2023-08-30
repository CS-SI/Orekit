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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.events.FieldEventsLogger.FieldLoggedEvent;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

public class FieldElevationExtremumDetectorTest {

    @Test
    public void testLEO() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(51.0), FastMath.toRadians(66.6), 300.0);
        final FieldElevationExtremumDetector<Binary64> raw =
                new FieldElevationExtremumDetector<>(Binary64Field.getInstance(), new TopocentricFrame(earth, gp, "test")).
                withMaxCheck(60.0).
                withThreshold(new Binary64(1.e-6)).
                withHandler(new FieldContinueOnEvent<>());
        final FieldEventSlopeFilter<FieldElevationExtremumDetector<Binary64>, Binary64> maxElevationDetector =
                new FieldEventSlopeFilter<>(raw, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);

        Assertions.assertEquals(60.0, raw.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, raw.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, raw.getMaxIterationCount());
        Assertions.assertEquals("test", raw.getTopocentricFrame().getName());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<Binary64> position = new FieldVector3D<>(new Binary64(-6142438.668),
                                                                     new Binary64(3492467.56),
                                                                     new Binary64(-25767.257));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(new Binary64(505.848),
                                                                     new Binary64(942.781),
                                                                     new Binary64(7435.922));
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                         new AbsoluteDate(2003, 9, 16, utc));
        final FieldOrbit<Binary64> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                       FramesFactory.getEME2000(), date,
                                                                       new Binary64(Constants.EIGEN5C_EARTH_MU));

        FieldPropagator<Binary64> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit,
                                                 Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                 new Binary64(Constants.EIGEN5C_EARTH_MU),
                                                 Constants.EIGEN5C_EARTH_C20,
                                                 Constants.EIGEN5C_EARTH_C30,
                                                 Constants.EIGEN5C_EARTH_C40,
                                                 Constants.EIGEN5C_EARTH_C50,
                                                 Constants.EIGEN5C_EARTH_C60);

        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(maxElevationDetector));

        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        int visibleEvents = 0;
        for (FieldLoggedEvent<Binary64> e : logger.getLoggedEvents()) {
            final double eMinus = raw.getElevation(e.getState().shiftedBy(-10.0)).getReal();
            final double e0     = raw.getElevation(e.getState()).getReal();
            final double ePlus  = raw.getElevation(e.getState().shiftedBy(+10.0)).getReal();
            if (e0 > FastMath.toRadians(5.0)) {
                ++visibleEvents;
            }
            Assertions.assertTrue(e0 > eMinus);
            Assertions.assertTrue(e0 > ePlus);
        }
        Assertions.assertEquals(15, logger.getLoggedEvents().size());
        Assertions.assertEquals( 6, visibleEvents);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

