/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

import java.util.concurrent.atomic.AtomicInteger;

class DetectorModifierTest {

    private double maxCheck;
    private double threshold;
    private double dt;
    private Orbit iniOrbit;
    private AbsoluteDate iniDate;
    private NumericalPropagator propagator;

    @Test
    void testGetDetectionSettings() {
        // GIVEN
        final EventDetector detector = Mockito.mock(EventDetector.class);
        final EventDetectionSettings detectionSettings = Mockito.mock(EventDetectionSettings.class);
        Mockito.when(detector.getDetectionSettings()).thenReturn(detectionSettings);
        final DetectorModifier detectorModifier = new TestDetectorModifier(detector);
        // WHEN
        final EventDetectionSettings actualSettings = detectorModifier.getDetectionSettings();
        // THEN
        Assertions.assertEquals(detectionSettings, actualSettings);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDependsOnTimeOnly(final boolean value) {
        // GIVEN
        final EventDetector detector = Mockito.mock(EventDetector.class);
        Mockito.when(detector.dependsOnTimeOnly()).thenReturn(value);
        final DetectorModifier detectorModifier = new TestDetectorModifier(detector);
        // WHEN
        final boolean actual = detectorModifier.dependsOnTimeOnly();
        // THEN
        Assertions.assertEquals(value, actual);
    }

    @Test
    void testInit() {
        // GIVEN
        final EventDetector detector = Mockito.mock(EventDetector.class);
        Mockito.when(detector.getHandler()).thenReturn(new StopOnEvent());
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final AbsoluteDate mockedDate = Mockito.mock(AbsoluteDate.class);
        final DetectorModifier detectorModifier = new TestDetectorModifier(detector);
        // WHEN
        detectorModifier.init(mockedState, mockedDate);
        // THEN
        Mockito.verify(detector).init(mockedState, mockedDate);
    }

    @Test
    void testReset() {
        // GIVEN
        final EventDetector detector = Mockito.mock(EventDetector.class);
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final AbsoluteDate mockedDate = Mockito.mock(AbsoluteDate.class);
        final DetectorModifier detectorModifier = new TestDetectorModifier(detector);
        // WHEN
        detectorModifier.reset(mockedState, mockedDate);
        // THEN
        Mockito.verify(detector).reset(mockedState, mockedDate);
    }

    @Test
    void testG() {
        // GIVEN
        final EventDetector detector = Mockito.mock(EventDetector.class);
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final double expectedG = 10.;
        Mockito.when(detector.g(mockedState)).thenReturn(expectedG);
        final DetectorModifier detectorModifier = new TestDetectorModifier(detector);
        // WHEN
        final double actualG = detectorModifier.g(mockedState);
        // THEN
        Assertions.assertEquals(expectedG, actualG);
    }

    @Test
    void testFinish() {
        // GIVEN
        final EventDetector detector = Mockito.mock(EventDetector.class);
        Mockito.when(detector.getHandler()).thenReturn(new StopOnEvent());
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final DetectorModifier detectorModifier = new TestDetectorModifier(detector);
        // WHEN
        detectorModifier.finish(mockedState);
        // THEN
        Mockito.verify(detector).finish(mockedState);
    }

    @Test
    void testSimpleTimer() {
        DateDetector dateDetector = new DateDetector(iniDate.shiftedBy(2.0*dt)).
                                    withMaxCheck(maxCheck).
                                    withThreshold(threshold);
        DetectorModifier adapter = new TestDetectorModifier(dateDetector);
        Assertions.assertSame(dateDetector, adapter.getDetector());
        Assertions.assertEquals(2 * dt, dateDetector.getDate().durationFrom(iniDate), 1.0e-10);
        propagator.addEventDetector(adapter);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assertions.assertEquals(2.0*dt, finalState.getDate().durationFrom(iniDate), threshold);
    }

    @Test
    void testOverrideHandler() {
        AtomicInteger count = new AtomicInteger(0);
        DateDetector dateDetector = new DateDetector(iniDate.shiftedBy(2.0*dt)).
                                    withMaxCheck(maxCheck).
                                    withThreshold(threshold);
        DetectorModifier adapter = new TestDetectorModifier(dateDetector) {
            /** {@inheritDoc} */
            @Override
            public EventHandler getHandler() {
                return new EventHandler() {
                    /** {@inheritDoc} */
                    @Override
                    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
                        count.incrementAndGet();
                        return Action.RESET_STATE;
                    }
                };
            }
        };
        Assertions.assertSame(dateDetector, adapter.getDetector());
        Assertions.assertEquals(2 * dt, dateDetector.getDate().durationFrom(iniDate), 1.0e-10);
        propagator.addEventDetector(adapter);
        Assertions.assertEquals(0, count.get());
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));
        Assertions.assertEquals(1, count.get());

        Assertions.assertEquals(100.0*dt, finalState.getDate().durationFrom(iniDate), threshold);
    }

    @Deprecated
    @Test
    void testAdapterDetector() {
        // GIVEN
        final DateDetector detector = new DateDetector();
        // WHEN
        final AdapterDetector adapterDetector = new AdapterDetector(detector);
        // THEN
        final TestDetectorModifier detectorModifier = new TestDetectorModifier(detector);
        Assertions.assertEquals(detectorModifier.getDetector(), adapterDetector.getDetector());
    }
    
    private static class TestDetectorModifier implements DetectorModifier {

        private final EventDetector detector;

        TestDetectorModifier(final EventDetector detector) {
            this.detector = detector;
        }

        @Override
        public EventDetector getDetector() {
            return detector;
        }
    }

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            final double mu = 3.9860047e14;
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            iniDate  = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            iniOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                            FramesFactory.getEME2000(), iniDate, mu);
            SpacecraftState initialState = new SpacecraftState(iniOrbit);
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
            dt = 60.;
            maxCheck  = 10.;
            threshold = 10.e-10;
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        iniDate = null;
        propagator = null;
    }

}
