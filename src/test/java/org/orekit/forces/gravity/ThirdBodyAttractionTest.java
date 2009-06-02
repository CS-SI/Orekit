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
package org.orekit.forces.gravity;

import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.orekit.bodies.SolarSystemBody;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FrameFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;

public class ThirdBodyAttractionTest {

    private double mu;

    @Test(expected= OrekitException.class)
    public void xxtestSunContrib() throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           Math.tan(0.001745329) * Math.cos(2 * Math.PI / 3),
                                           Math.tan(0.001745329) * Math.sin(2 * Math.PI / 3),
                                           0.1, 2, FrameFactory.getEME2000(), date, mu);
        double period = 2 * Math.PI * orbit.getA() * Math.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(SolarSystemBody.getSun()));

        // set up step handler to perform checks
        calc.setMasterMode(Math.floor(period), new ReferenceChecker(date) {
            private static final long serialVersionUID = 6539780121834779598L;
            protected double hXRef(double t) {
                return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 *
                Math.cos(3.9820426e-7*t) - 7.59983e-5 * Math.sin(3.9820426e-7*t);
            }
            protected double hYRef(double t) {
                return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 *
                Math.cos(3.9820426e-7*t) + 17.6083e-5 * Math.sin(3.9820426e-7*t);
            }
        });
        AbsoluteDate finalDate = new AbsoluteDate(date, 365 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    @Test
    public void testMoonContrib() throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit =
            new EquinoctialOrbit(42164000,10e-3,10e-3,
                                      Math.tan(0.001745329) * Math.cos(2 * Math.PI / 3),
                                      Math.tan(0.001745329) * Math.sin(2 * Math.PI / 3),
                                      0.1, 2, FrameFactory.getEME2000(), date, mu);
        double period = 2 * Math.PI * orbit.getA() * Math.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(SolarSystemBody.getMoon()));

        // set up step handler to perform checks
        calc.setMasterMode(Math.floor(period), new ReferenceChecker(date) {
            private static final long serialVersionUID = -4725658720642817168L;
            protected double hXRef(double t) {
                return  -0.000906173 + 1.93933e-11 * t +
                         1.0856e-06  * Math.cos(5.30637e-05 * t) -
                         1.22574e-06 * Math.sin(5.30637e-05 * t);
            }
            protected double hYRef(double t) {
                return 0.00151973 + 1.88991e-10 * t -
                       1.25972e-06  * Math.cos(5.30637e-05 * t) -
                       1.00581e-06 * Math.sin(5.30637e-05 * t);
            }
        });
        AbsoluteDate finalDate = new AbsoluteDate(date, 31 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    private static abstract class ReferenceChecker implements OrekitFixedStepHandler {

        private static final long serialVersionUID = -2167849325324684095L;
        private final AbsoluteDate reference;

        protected ReferenceChecker(AbsoluteDate reference) {
            this.reference = reference;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            double t = currentState.getDate().durationFrom(reference);
            assertEquals(hXRef(t), currentState.getHx(), 1e-4);
            assertEquals(hYRef(t), currentState.getHy(), 1e-4);
        }

        protected abstract double hXRef(double t);

        protected abstract double hYRef(double t);

    }

    @Before
    public void setUp() {
        mu = 3.986e14;
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
    }

}
