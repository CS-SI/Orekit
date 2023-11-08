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
package org.orekit.propagation.analytical;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;

public class EphemerisEventsTest {

    @Test
    public void testEphemKeplerian() throws IllegalArgumentException, OrekitException {
        checkEphem(OrbitType.KEPLERIAN);
    }

    @Test
    public void testEphemCircular() throws IllegalArgumentException, OrekitException {
        checkEphem(OrbitType.CIRCULAR);
    }

    @Test
    public void testEphemEquinoctial() throws IllegalArgumentException, OrekitException {
        checkEphem(OrbitType.EQUINOCTIAL);
    }

    @Test
    public void testEphemCartesian() throws IllegalArgumentException, OrekitException {
        checkEphem(OrbitType.CARTESIAN);
    }

    private Ephemeris buildEphem(OrbitType type)
        throws IllegalArgumentException, OrekitException {

        double mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        double deltaT = finalDate.durationFrom(initDate);

        final Frame frame = FramesFactory.getEME2000();

        Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE, frame, initDate, mu);

        int nbIntervals = 720;
        Propagator propagator =
                new EcksteinHechlerPropagator(transPar, mass, ae, mu, c20, c30, c40, c50, c60);

        List<SpacecraftState> tab = new ArrayList<SpacecraftState>(nbIntervals + 1);
        for (int j = 0; j<= nbIntervals; j++) {
            SpacecraftState state = propagator.propagate(initDate.shiftedBy((j * deltaT) / nbIntervals));
            tab.add(new SpacecraftState(type.convertType(state.getOrbit()),
                                        state.getAttitude(), state.getMass()));
        }

        final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(2, frame, frame);

        return new Ephemeris(tab, interpolator);
    }

    private EclipseDetector buildEclipseDetector(final OrbitType type) {

        double sunRadius = 696000000.;
        double earthRadius = 6400000.;

        EclipseDetector ecl = new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                                  new OneAxisEllipsoid(earthRadius,
                                                                       0.0,
                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                              withMaxCheck(60.0).
                              withThreshold(1.0e-3).
                              withHandler(new EventHandler() {
                                public Action eventOccurred(SpacecraftState s, EventDetector detector,
                                                            boolean increasing) {
                                    Assertions.assertEquals(type, s.getOrbit().getType());
                                    if (increasing) {
                                        ++inEclipsecounter;
                                    } else {
                                        ++outEclipsecounter;
                                    }
                                    return Action.CONTINUE;
                                }
                            });

        return ecl;
    }

    private void checkEphem(OrbitType type)
        throws IllegalArgumentException, OrekitException {

        initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

        finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                     TimeComponents.H00,
                                     TimeScalesFactory.getUTC());



        BoundedPropagator ephem = buildEphem(type);

        ephem.addEventDetector(buildEclipseDetector(type));

        AbsoluteDate computeEnd = new AbsoluteDate(finalDate, -1000.0);

        ephem.clearStepHandlers();
        SpacecraftState state = ephem.propagate(computeEnd);
        Assertions.assertEquals(computeEnd, state.getDate());
        Assertions.assertEquals(14, inEclipsecounter);
        Assertions.assertEquals(14, outEclipsecounter);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        inEclipsecounter = 0;
        outEclipsecounter = 0;
    }

    private AbsoluteDate initDate;
    private AbsoluteDate finalDate;
    private int inEclipsecounter;
    private int outEclipsecounter;

}
