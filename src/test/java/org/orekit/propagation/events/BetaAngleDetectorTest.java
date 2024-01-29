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
package org.orekit.propagation.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class BetaAngleDetectorTest {
    private Propagator propagator;
    private AbsoluteDate date;

    @BeforeEach
    void setup() {
        Utils.setDataRoot("regular-data");
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final AbsoluteDate iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                    FramesFactory.getGCRF(), iniDate, 3.9860047e14);
        final SpacecraftState initialState = new SpacecraftState(orbit);
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
        ((NumericalPropagator) propagator).setInitialState(initialState);
        date = iniDate;
    }

    @Test
    void evaluate() {
        final BetaAngleDetector detector = new BetaAngleDetector(0);
        
        AbsoluteDate d = date;
        for (int i = 0; i < 50; i++) {
            final SpacecraftState state = propagator.propagate(d);
            final double g = detector.g(state);
            final double beta = BetaAngleDetector.calculateBetaAngle(
                    state, CelestialBodyFactory.getSun(), FramesFactory.getGCRF());

            final double expectedBeta = MathUtils.SEMI_PI - Vector3D.angle(
                    state.getPVCoordinates(FramesFactory.getGCRF()).getMomentum().normalize(),
                    CelestialBodyFactory.getSun().getPosition(state.getDate(), FramesFactory.getGCRF()).normalize());
            assertEquals(-beta, g, 1e-9);
            assertEquals(expectedBeta, beta, 1e-9);

            d = d.shiftedBy(86400);
        }
    }

    @Test
    void simpleStop() {
        final BetaAngleDetector detector = new BetaAngleDetector(0,
                CelestialBodyFactory.getSun(),
                FramesFactory.getGCRF());
        
        propagator.addEventDetector(detector);
        
        final SpacecraftState state = propagator.propagate(date, date.shiftedBy(30 * 86400));
        assertEquals(1883928.588393031, state.getDate().durationFrom(date), 1e-9);

        assertEquals(0, BetaAngleDetector.calculateBetaAngle(state, propagator), 1e-9);
    }

    @Test
    void record() {
        final RecordAndContinue handler = new RecordAndContinue();
        final BetaAngleDetector detector = new BetaAngleDetector(0,
                CelestialBodyFactory.getMoon(),
                FramesFactory.getGCRF())
            .withBetaThreshold(FastMath.toRadians(1))
            .withCelestialProvider(CelestialBodyFactory.getSun())
            .withInertialFrame(FramesFactory.getEME2000())
            .withHandler(handler);
        
        propagator.addEventDetector(detector);
        
        final SpacecraftState state = propagator.propagate(date, date.shiftedBy(30 * 86400));
        assertEquals(30 * 86400, state.getDate().durationFrom(date), 1e-9);
        assertEquals(1, handler.getEvents().size());
    }
}
