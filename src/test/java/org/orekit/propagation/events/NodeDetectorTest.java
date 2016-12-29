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

import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class NodeDetectorTest {

    @Test
    public void testIssue138() throws OrekitException {
        double a = 800000 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double e = 0.0001;
        double i = FastMath.toRadians(98);
        double w = -90;
        double raan = 0;
        double v = 0;
        Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initialDate = new AbsoluteDate(2014, 01, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        AbsoluteDate finalDate = initialDate.shiftedBy(5000);
        KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, w, raan, v, PositionAngle.TRUE, inertialFrame, initialDate, Constants.WGS84_EARTH_MU);
        SpacecraftState initialState = new SpacecraftState(initialOrbit, 1000);

        double[][] tol = NumericalPropagator.tolerances(10, initialOrbit, initialOrbit.getType());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        // Define 2 instances of NodeDetector:
        EventDetector rawDetector =
                new NodeDetector(1e-6,initialState.getOrbit(), initialState.getFrame()).
                withHandler(new ContinueOnEvent<NodeDetector>());

        EventsLogger logger1 = new EventsLogger();
        EventDetector node1 = logger1.monitorDetector(rawDetector);
        EventsLogger logger2 = new EventsLogger();
        EventDetector node2 = logger2.monitorDetector(rawDetector);

        propagator.addEventDetector(node1);
        propagator.addEventDetector(node2);

        // First propagation
        propagator.setEphemerisMode();
        propagator.propagate(finalDate);
        Assert.assertEquals(2, logger1.getLoggedEvents().size());
        Assert.assertEquals(2, logger2.getLoggedEvents().size());
        logger1.clearLoggedEvents();
        logger2.clearLoggedEvents();

        BoundedPropagator postpro = propagator.getGeneratedEphemeris();

        // Post-processing
        postpro.addEventDetector(node1);
        postpro.addEventDetector(node2);
        postpro.propagate(finalDate);
        Assert.assertEquals(2, logger1.getLoggedEvents().size());
        Assert.assertEquals(2, logger2.getLoggedEvents().size());

    }

    @Test
    public void testIssue158() throws OrekitException {

        double a          = 3.0e7;
        double e1         =  0.8;
        double e2         =  1.0e-4;
        double i          = 1.0;
        double pa         = 1.5 * FastMath.PI;
        double raan       = 5.0;
        double m          = 0;
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame       = FramesFactory.getEME2000();
        double mu         = Constants.EIGEN5C_EARTH_MU;

        // highly eccentric, inclined orbit
        final KeplerianOrbit orbit1 =
                new KeplerianOrbit(a, e1, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        EventDetector detector1 = new NodeDetector(orbit1, orbit1.getFrame());
        double t1 = orbit1.getKeplerianPeriod();
        Assert.assertEquals(t1 / 28.82, detector1.getMaxCheckInterval(), t1 / 10000);

        // nearly circular, inclined orbit
        final KeplerianOrbit orbit2 =
                new KeplerianOrbit(a, e2, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        EventDetector detector2 = new NodeDetector(orbit2, orbit2.getFrame());
        double t2 = orbit2.getKeplerianPeriod();
        Assert.assertEquals(t1, t2, t1 / 10000);
        Assert.assertEquals(t2 / 3, detector2.getMaxCheckInterval(), t2 / 10000);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

