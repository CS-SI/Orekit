/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class FieldNodeDetectorTest {

    @Test
    public void testIssue138() throws OrekitException{
        doTestIssue138(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue158() throws OrekitException{
        doTestIssue158(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>void doTestIssue138(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        T a = zero.add(800000 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        T e = zero.add(0.0001);
        T i = zero.add(FastMath.toRadians(98));
        T w = zero.add(-90);
        T raan = zero;
        T v = zero;
        Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<T>(field, 2014, 01, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        FieldAbsoluteDate<T> finalDate = initialDate.shiftedBy(5000);
        FieldKeplerianOrbit<T> initialOrbit = new FieldKeplerianOrbit<T>(a, e, i, w, raan, v, PositionAngle.TRUE, inertialFrame, initialDate, Constants.WGS84_EARTH_MU);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(initialOrbit, zero.add(1000));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(initialOrbit);
//        propagator..setInitialState(initialState);

        // Define 2 instances of NodeDetector:
        FieldEventDetector<T> rawDetector =
                new FieldNodeDetector<T>(zero.add(1e-6),initialState.getOrbit(), initialState.getFrame()).
                withHandler(new FieldContinueOnEvent<FieldNodeDetector<T>,T>());

        FieldEventsLogger<T> logger1 = new FieldEventsLogger<T>();
        FieldEventDetector<T> node1 = logger1.monitorDetector(rawDetector);
        FieldEventsLogger<T> logger2 = new FieldEventsLogger<T>();
        FieldEventDetector<T> node2 = logger2.monitorDetector(rawDetector);

        propagator.addEventDetector(node1);
        propagator.addEventDetector(node2);

        // First propagation
        propagator.setEphemerisMode();
        propagator.propagate(finalDate);
        Assert.assertEquals(2, logger1.getLoggedEvents().size());
        Assert.assertEquals(2, logger2.getLoggedEvents().size());
        logger1.clearLoggedEvents();
        logger2.clearLoggedEvents();
        FieldBoundedPropagator<T> postpro = propagator.getGeneratedEphemeris();

        // Post-processing
        postpro.addEventDetector(node1);
        postpro.addEventDetector(node2);
        postpro.propagate(finalDate);
        Assert.assertEquals(2, logger1.getLoggedEvents().size());
        Assert.assertEquals(2, logger2.getLoggedEvents().size());

    }

    private <T extends RealFieldElement<T>>void doTestIssue158(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T a          = zero.add(3.0e7);
        T e1         = zero.add( 0.8);
        T e2         = zero.add( 1.0e-4);
        T i          = zero.add(1.0);
        T pa         = zero.add(1.5 * FastMath.PI);
        T raan       = zero.add(5.0);
        T m          = zero.add(0);
        Frame frame       = FramesFactory.getEME2000();
        double mu         = Constants.EIGEN5C_EARTH_MU;

        // highly eccentric, inclined orbit
        final FieldKeplerianOrbit<T> orbit1 =
                new FieldKeplerianOrbit<T>(a, e1, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        FieldEventDetector<T> detector1 = new FieldNodeDetector<T>(orbit1, orbit1.getFrame());
        T t1 = orbit1.getKeplerianPeriod();
        Assert.assertEquals(t1.getReal() / 28.82, detector1.getMaxCheckInterval().getReal(), t1.getReal() / 10000);

        // nearly circular, inclined orbit
        final FieldKeplerianOrbit<T> orbit2 =
                new FieldKeplerianOrbit<T>(a, e2, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        FieldEventDetector<T> detector2 = new FieldNodeDetector<T>(orbit2, orbit2.getFrame());
        T t2 = orbit2.getKeplerianPeriod();
        Assert.assertEquals(t1.getReal(), t2.getReal(), t1.getReal() / 10000);
        Assert.assertEquals(t2.getReal() / 3, detector2.getMaxCheckInterval().getReal(), t2.getReal() / 10000);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

