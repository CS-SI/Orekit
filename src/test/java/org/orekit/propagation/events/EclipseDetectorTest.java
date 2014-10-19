/* Copyright 2002-2014 CS Systèmes d'Information
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

import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
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
        EclipseDetector e = new EclipseDetector(60., 1.e-3,
                                                CelestialBodyFactory.getSun(), sunRadius,
                                                CelestialBodyFactory.getEarth(), earthRadius).
                            withHandler(new StopOnDecreasing<EclipseDetector>());
        Assert.assertEquals(60.0, e.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-3, e.getThreshold(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, e.getMaxIterationCount());
        propagator.addEventDetector(e);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate), 1.0e-3);
    }

    @Test
    public void testPenumbra() throws OrekitException {
        EclipseDetector e = new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                                CelestialBodyFactory.getEarth(), earthRadius).
                            withPenumbra();
        propagator.addEventDetector(e);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(4388.155852, finalState.getDate().durationFrom(iniDate), 1.0e-6);
    }

    @Test
    public void testWithMethods() throws OrekitException {
        EclipseDetector e = new EclipseDetector(60., 1.e-3,
                                                CelestialBodyFactory.getSun(), sunRadius,
                                                CelestialBodyFactory.getEarth(), earthRadius).
                             withHandler(new StopOnDecreasing<EclipseDetector>()).
                             withMaxCheck(120.0).
                             withThreshold(1.0e-4).
                             withMaxIter(12);
        Assert.assertEquals(120.0, e.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-4, e.getThreshold(), 1.0e-15);
        Assert.assertEquals(12, e.getMaxIterationCount());
        propagator.addEventDetector(e);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate), 1.0e-3);

    }

    @Test
    public void testTooSmallMaxIterationCount() throws OrekitException {
        EclipseDetector e = new EclipseDetector(60., 1.e-3,
                                                CelestialBodyFactory.getSun(), sunRadius,
                                                CelestialBodyFactory.getEarth(), earthRadius).
                             withHandler(new StopOnDecreasing<EclipseDetector>()).
                             withMaxCheck(120.0).
                             withThreshold(1.0e-4).
                             withMaxIter(8);
       propagator.addEventDetector(e);
        try {
            propagator.propagate(iniDate.shiftedBy(6000));
            Assert.fail("an exception should have been thrown");
        } catch (PropagationException pe) {
            Assert.assertEquals(8, ((TooManyEvaluationsException) pe.getCause()).getMax());
        }
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            mu  = 3.9860047e14;
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                     FramesFactory.getGCRF(), iniDate, mu);
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
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        iniDate = null;
        initialState = null;
        propagator = null;
    }

}

