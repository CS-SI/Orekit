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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;

class FieldBetaAngleDetectorTest {
    private FieldPropagator<Binary64> propagator;
    private FieldAbsoluteDate<Binary64> date;

    @BeforeEach
    void setup() {
        Utils.setDataRoot("regular-data");
        final FieldVector3D<Binary64> position  = new FieldVector3D<>(
            new Binary64(-6142438.668), new Binary64(3492467.560), new Binary64(-25767.25680));
        final FieldVector3D<Binary64> velocity  = new FieldVector3D<Binary64>(
            new Binary64(505.8479685), new Binary64(942.7809215), new Binary64(7435.922231));
        final FieldAbsoluteDate<Binary64> iniDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<Binary64> orbit = 
                new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                    FramesFactory.getGCRF(), iniDate, new Binary64(3.9860047e14));
        final FieldSpacecraftState<Binary64> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<Binary64> integrator =
            new DormandPrince853FieldIntegrator<>(Binary64Field.getInstance(), 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        propagator = new FieldNumericalPropagator<>(Binary64Field.getInstance(), integrator);
        ((FieldNumericalPropagator<Binary64>) propagator).setInitialState(initialState);
        date = iniDate;
    }

    @Test
    void evaluate() {
        final FieldPVCoordinatesProvider<Binary64> sun = CelestialBodyFactory.getSun().toFieldPVCoordinatesProvider(Binary64Field.getInstance());
        final FieldBetaAngleDetector<Binary64> detector = new FieldBetaAngleDetector<>(Binary64.ZERO);
        
        FieldAbsoluteDate<Binary64> d = date;
        for (int i = 0; i < 50; i++) {
            final FieldSpacecraftState<Binary64> state = propagator.propagate(d);
            final Binary64 g = detector.g(state);
            final Binary64 beta = FieldBetaAngleDetector.calculateBetaAngle(
                    state, sun, FramesFactory.getGCRF());
            
            final Binary64 expectedBeta = FieldVector3D.angle(
                    state.getPVCoordinates(FramesFactory.getGCRF()).getMomentum().normalize(),
                    sun.getPosition(state.getDate(), FramesFactory.getGCRF()).normalize()).negate().add(MathUtils.SEMI_PI);
            assertEquals(beta.negate(), g);
            assertEquals(expectedBeta.getReal(), beta.getReal(), 1e-9);
            d = d.shiftedBy(86400);
        }
    }

    @Test
    void simpleStop() {
        final FieldPVCoordinatesProvider<Binary64> sun = CelestialBodyFactory.getSun().toFieldPVCoordinatesProvider(Binary64Field.getInstance());
        final FieldBetaAngleDetector<Binary64> detector = new FieldBetaAngleDetector<>(
                Binary64Field.getInstance(),
                Binary64.ZERO,
                sun,
                FramesFactory.getGCRF());
        
        propagator.addEventDetector(detector);
        
        final FieldSpacecraftState<Binary64> state = propagator.propagate(date, date.shiftedBy(30 * 86400));
        assertEquals(1883928.588393031, state.getDate().durationFrom(date).getReal(), 1e-9);

        assertEquals(0, FieldBetaAngleDetector.calculateBetaAngle(state, sun).getReal(), 1e-9);
    }

    @Test
    void record() {
        final FieldPVCoordinatesProvider<Binary64> sun = CelestialBodyFactory.getSun().toFieldPVCoordinatesProvider(Binary64Field.getInstance());
        final FieldRecordAndContinue<Binary64> handler = new FieldRecordAndContinue<>();
        final FieldBetaAngleDetector<Binary64> detector = new FieldBetaAngleDetector<>(
                Binary64Field.getInstance(),
                Binary64.ZERO,
                CelestialBodyFactory.getMoon().toFieldPVCoordinatesProvider(Binary64Field.getInstance()),
                FramesFactory.getGCRF())
            .withBetaThreshold(FastMath.toRadians(Binary64.ONE))
            .withCelestialProvider(sun)
            .withInertialFrame(FramesFactory.getEME2000())
            .withHandler(handler);
        
        propagator.addEventDetector(detector);
        
        final FieldSpacecraftState<Binary64> state = propagator.propagate(date, date.shiftedBy(30 * 86400));
        assertEquals(30 * 86400, state.getDate().durationFrom(date).getReal(), 1e-9);
        assertEquals(1, handler.getEvents().size());
    }
}
