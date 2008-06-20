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
package org.orekit.propagation;

import java.text.ParseException;

import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.Ephemeris;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class TabulatedEphemerisTest extends TestCase {

    public void testInterpolation() throws ParseException, OrekitException {

        double mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = Math.toRadians(261);
        double lv = 0;

        AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                 ChunkedTime.H00,
                                                 UTCScale.getInstance());
        AbsoluteDate finalDate = new AbsoluteDate(new ChunkedDate(2004, 01, 02),
                                                  ChunkedTime.H00,
                                                  UTCScale.getInstance());
        double deltaT = finalDate.minus(initDate);

        Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA,
                                            lv, KeplerianOrbit.TRUE_ANOMALY, 
                                            Frame.getJ2000(), initDate, mu);

        int nbIntervals = 720;
        EcksteinHechlerPropagator eck =
            new EcksteinHechlerPropagator(transPar, mass,
                                          ae, mu, c20, c30, c40, c50, c60);
        SpacecraftState[] tab = new SpacecraftState[nbIntervals+1];
        for (int j = 0; j<= nbIntervals; j++) {
            AbsoluteDate current = new AbsoluteDate(initDate, (j * deltaT) / nbIntervals);
            tab[j] = eck.propagate(current);
        }

        Ephemeris te = new Ephemeris(tab);

        assertEquals(te.getMaxDate(), finalDate);
        assertEquals(te.getMinDate(), initDate);

        checkEphemerides(eck, te, new AbsoluteDate(initDate, 3600),  0, true);
        checkEphemerides(eck, te, new AbsoluteDate(initDate, 3660), 30, false);
        checkEphemerides(eck, te, new AbsoluteDate(initDate, 3720),  0, true);

    }

    private void checkEphemerides(BasicPropagator eph1, BasicPropagator eph2, AbsoluteDate date,
                                  double threshold, boolean expectedBelow)
        throws PropagationException {
        SpacecraftState state1 = eph1.propagate(date);
        SpacecraftState state2 = eph2.propagate(date);
        double maxError = Math.abs(state1.getA() - state2.getA());
        maxError = Math.max(maxError, Math.abs(state1.getEquinoctialEx() - state2.getEquinoctialEx()));
        maxError = Math.max(maxError, Math.abs(state1.getEquinoctialEy() - state2.getEquinoctialEy()));
        maxError = Math.max(maxError, Math.abs(state1.getHx() - state2.getHx()));
        maxError = Math.max(maxError, Math.abs(state1.getHy() - state2.getHy()));
        maxError = Math.max(maxError, Math.abs(state1.getLv() - state2.getLv()));
        if (expectedBelow) {
            assertTrue(maxError <= threshold);
        } else {
            assertTrue(maxError >= threshold);
        }
    }

    public void setUp() {
        mu  = 3.9860047e14;
        ae  = 6.378137e6;
        c20 = -1.08263e-3;
        c30 = 2.54e-6;
        c40 = 1.62e-6;
        c50 = 2.3e-7;
        c60 = -5.5e-7;
    }

    public void tearDown() {
        mu  = Double.NaN;
        ae  = Double.NaN;
        c20 = Double.NaN;
        c30 = Double.NaN;
        c40 = Double.NaN;
        c50 = Double.NaN;
        c60 = Double.NaN;
    }

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    public static Test suite() {
        return new TestSuite(TabulatedEphemerisTest.class);
    }

}
