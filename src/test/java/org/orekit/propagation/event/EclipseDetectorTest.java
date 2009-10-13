/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.event;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.SolarSystemBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class EclipseDetectorTest {

    private double               mu;
    private AbsoluteDate         iniDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;

    private double sunRadius = 696000000.;
    private double earthRadius = 6400000.;

    @Test
    public void testEclipse() throws OrekitException {
        propagator.addEventDetector(new EclipseDetector(60., 1.e-3,
                SolarSystemBody.getSun(), sunRadius,
                SolarSystemBody.getEarth(), earthRadius) {
            private static final long serialVersionUID = 1L;
			public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
		        return increasing ? CONTINUE : STOP;
            }
        });
        final SpacecraftState finalState = propagator.propagate(new AbsoluteDate(iniDate, 6000));
        Assert.assertEquals(2533.078, finalState.getDate().durationFrom(iniDate), 1.0e-3);

//        System.out.println(" Ini date : " + iniDate.getDate());
//        System.out.println(" End date : " + finalState.getDate());
//        System.out.println(" Duration : " + finalState.getDate().durationFrom(iniDate));

    }

    @Test
    public void testPenumbra() throws OrekitException {
        propagator.addEventDetector(new EclipseDetector(
                SolarSystemBody.getSun(), sunRadius,
                SolarSystemBody.getEarth(), earthRadius, false));
        final SpacecraftState finalState = propagator.propagate(new AbsoluteDate(iniDate, 6000));
        Assert.assertEquals(4158.358490, finalState.getDate().durationFrom(iniDate), 1.0e-6);

//        System.out.println(" Ini date : " + iniDate.getDate());
//        System.out.println(" End date : " + finalState.getDate());
//        System.out.println(" Duration : " + finalState.getDate().durationFrom(iniDate));
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        initialState = new SpacecraftState(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
    }

    @After
    public void tearDown() {
        iniDate = null;
        initialState = null;
        propagator = null;
    }

}

