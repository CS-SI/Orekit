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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.events.FieldEventsLogger.FieldLoggedEvent;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

public class FieldApsideDetectorTest {

    @Test
    public void testSimple() {
        doTestSimple(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSimple(Field<T> field) {

        final FieldPropagator<T> propagator = createPropagator(field);
        FieldEventDetector<T> detector = new FieldApsideDetector<>(propagator.getInitialState().getOrbit()).
                                         withMaxCheck(600.0).
                                         withThreshold(field.getZero().newInstance(1.0e-12)).
                                         withHandler(new FieldContinueOnEvent<T>());

        Assertions.assertEquals(600.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-12, detector.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(30, logger.getLoggedEvents().size());
        for (FieldLoggedEvent<T> e : logger.getLoggedEvents()) {
            FieldKeplerianOrbit<T> o = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(e.getState().getOrbit());
            double expected = e.isIncreasing() ? 0.0 : FastMath.PI;
            Assertions.assertEquals(expected, MathUtils.normalizeAngle(o.getMeanAnomaly().getReal(), expected), 4.0e-14);
        }

    }

    @Test
    public void testFixedMaxCheck() {
        doTestMaxcheck(Binary64Field.getInstance(), s -> 20.0, 4682);
    }

    @Test
    public void testAnomalyAwareMaxCheck() {
        doTestMaxcheck(Binary64Field.getInstance(),
                       s -> {
                           final double         baseMaxCheck             = 20.0;
                           final KeplerianOrbit orbit                    = ((FieldKeplerianOrbit<Binary64>) OrbitType.KEPLERIAN.convertType(s.getOrbit())).toOrbit();
                           final double         period                   = orbit.getKeplerianPeriod();
                           final double         timeSincePreviousPerigee = MathUtils.normalizeAngle(orbit.getMeanAnomaly(), FastMath.PI) /
                                           orbit.getKeplerianMeanMotion();
                           final double         timeToNextPerigee        = period - timeSincePreviousPerigee;
                           final double         timeToApogee             = FastMath.abs(0.5 * period - timeSincePreviousPerigee);
                           final double         timeToClosestApside      = FastMath.min(timeSincePreviousPerigee,
                                                                                        FastMath.min(timeToApogee, timeToNextPerigee));
                           return (timeToClosestApside < 2 * baseMaxCheck) ? baseMaxCheck : timeToClosestApside - 0.5 * baseMaxCheck;
        }, 671);
    }

    private <T extends CalculusFieldElement<T>> void doTestMaxcheck(final Field<T> field,
                                                                    final FieldAdaptableInterval<T> maxCheck,
                                                                    int expectedCalls) {
        final FieldPropagator<T> propagator = createPropagator(field);
        CountingApsideDetector<T> detector = new CountingApsideDetector<>(propagator, maxCheck);
        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(30, logger.getLoggedEvents().size());
        Assertions.assertEquals(expectedCalls, detector.count);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private <T extends CalculusFieldElement<T>> FieldPropagator<T> createPropagator(final Field<T> field) {
        final T zero = field.getZero();

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(506.0), zero.add(943.0), zero.add(7450));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                              FramesFactory.getEME2000(), date,
                                                              zero.add(Constants.EIGEN5C_EARTH_MU));
        return new FieldEcksteinHechlerPropagator<>(orbit,
                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                    zero.add(Constants.EIGEN5C_EARTH_MU),
                                                    Constants.EIGEN5C_EARTH_C20,
                                                    Constants.EIGEN5C_EARTH_C30,
                                                    Constants.EIGEN5C_EARTH_C40,
                                                    Constants.EIGEN5C_EARTH_C50,
                                                    Constants.EIGEN5C_EARTH_C60);
    }

    private class CountingApsideDetector<T extends CalculusFieldElement<T>> extends FieldAdapterDetector<T> {

        private int count;
        
        public CountingApsideDetector(final FieldPropagator<T> propagator, final FieldAdaptableInterval<T> maxCheck) {
            super(new FieldApsideDetector<>(propagator.getInitialState().getOrbit()).
                  withMaxCheck(maxCheck).
                  withThreshold(propagator.getInitialState().getDate().getField().getZero().newInstance(1.0e-12)).
                  withHandler(new FieldContinueOnEvent<>()));
        }

        public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
            super.init(s0, t);
            count = 0;
        }

        public T g(final FieldSpacecraftState<T> s) {
            ++count;
            return super.g(s);
        }

    }

}

