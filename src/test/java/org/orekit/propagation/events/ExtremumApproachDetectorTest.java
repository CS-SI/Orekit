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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

public class ExtremumApproachDetectorTest {

    /**
    * Test the detector on a keplerian orbit and detect extremum approach with Earth.
    */
    @Test
    public void testStopPropagationClosestApproachByDefault() {
        // Given
        // Loading Orekit data
        Utils.setDataRoot("regular-data");

        // Generating orbit
        final AbsoluteDate initialDate = new AbsoluteDate();
        final Frame frame = FramesFactory.getEME2000();
        final double mu = 398600e9; //m**3/s**2

        final double rp = (6378 + 400) * 1000; //m
        final double ra = (6378 + 800) * 1000; //m

        final double a = (ra + rp) / 2; //m
        final double e = (ra - rp) / (ra + rp); //m
        final double i = 0; //rad
        final double pa = 0; //rad
        final double raan = 0; //rad
        final double anomaly = FastMath.toRadians(0); //rad
        final Orbit orbit =
                new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngleType.TRUE, frame, initialDate, mu);

        // Will detect extremum approaches with Earth
        final PVCoordinatesProvider earthPVProvider = CelestialBodyFactory.getEarth();

        // Initializing detector
        final ExtremumApproachDetector detector = new ExtremumApproachDetector(earthPVProvider);

        // Initializing propagator
        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);

        // When
        final SpacecraftState stateAtEvent =
                propagator.propagate(initialDate.shiftedBy(orbit.getKeplerianPeriod() * 2));

        // Then
        Assertions.assertEquals(stateAtEvent.getDate().durationFrom(initialDate),orbit.getKeplerianPeriod(),1e-9);

    }

    /**
    * Test the detector on a keplerian orbit and detect extremum approach with Earth.
    */
    @Test
    public void testStopPropagationFarthestApproachWithHandler() {
        
        // Given
        // Loading Orekit data
        Utils.setDataRoot("regular-data");

        // Generating orbit
        final AbsoluteDate initialDate = new AbsoluteDate();
        final Frame frame = FramesFactory.getEME2000();
        final double mu = 398600e9; //m**3/s**2

        final double rp = (6378 + 400) * 1000; //m
        final double ra = (6378 + 800) * 1000; //m

        final double a = (ra + rp) / 2; //m
        final double e = (ra - rp) / (ra + rp); //m
        final double i = 0; //rad
        final double pa = 0; //rad
        final double raan = 0; //rad
        final double anomaly = FastMath.toRadians(0); //rad
        final Orbit orbit =
                new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngleType.TRUE, frame, initialDate, mu);

        // Will detect extremum approaches with Earth
        final PVCoordinatesProvider earthPVProvider = CelestialBodyFactory.getEarth();

        // Initializing detector with custom handler
        final ExtremumApproachDetector detector =
                new ExtremumApproachDetector(earthPVProvider).withHandler(new StopOnEvent());

        // Initializing propagator
        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);

        // When
        final SpacecraftState stateAtEvent =
                propagator.propagate(initialDate.shiftedBy(orbit.getKeplerianPeriod() * 2));

        // Then
        Assertions.assertEquals(stateAtEvent.getDate().durationFrom(initialDate),orbit.getKeplerianPeriod() / 2,1e-7);

    }

    @Test
    void testSecondaryPVCoordinatesProviderGetter() {
        // Given
        final PVCoordinatesProvider    secondaryPVProvider      = Mockito.mock(PVCoordinatesProvider.class);
        final ExtremumApproachDetector extremumApproachDetector = new ExtremumApproachDetector(secondaryPVProvider);

        // When
        final PVCoordinatesProvider returnedSecondaryPVProvider = extremumApproachDetector.getSecondaryPVProvider();

        // Then
        Assertions.assertEquals(secondaryPVProvider, returnedSecondaryPVProvider);
    }
}
