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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

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

        Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                            FramesFactory.getEME2000(), initDate, mu);

        int nbIntervals = 720;
        Propagator propagator =
                new EcksteinHechlerPropagator(transPar, mass, ae, mu, c20, c30, c40, c50, c60);

        List<SpacecraftState> tab = new ArrayList<SpacecraftState>(nbIntervals + 1);
        for (int j = 0; j<= nbIntervals; j++) {
            SpacecraftState state = propagator.propagate(initDate.shiftedBy((j * deltaT) / nbIntervals));
            tab.add(new SpacecraftState(type.convertType(state.getOrbit()),
                                        state.getAttitude(), state.getMass()));
        }

        return new Ephemeris(tab, 2);
    }

    private EclipseDetector buildEclipsDetector(final OrbitType type) throws OrekitException {

        double sunRadius = 696000000.;
        double earthRadius = 6400000.;

        EclipseDetector ecl = new EclipseDetector(60., 1.e-3,
                                                  CelestialBodyFactory.getSun(), sunRadius,
                                                  CelestialBodyFactory.getEarth(), earthRadius).
                              withHandler(new EventHandler<EclipseDetector>() {
                                public Action eventOccurred(SpacecraftState s, EclipseDetector detector,
                                                            boolean increasing)
                                    throws OrekitException {
                                    Assert.assertEquals(type, s.getOrbit().getType());
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

        ephem.addEventDetector(buildEclipsDetector(type));

        AbsoluteDate computeEnd = new AbsoluteDate(finalDate, -1000.0);

        ephem.setSlaveMode();
        SpacecraftState state = ephem.propagate(computeEnd);
        Assert.assertEquals(computeEnd, state.getDate());
        Assert.assertEquals(14, inEclipsecounter);
        Assert.assertEquals(14, outEclipsecounter);
    }

    @Before
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
