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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.forces.Moon;
import org.orekit.forces.Sun;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.frames.Frame;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;


public class ThirdBodyAttractionTest extends TestCase {

    private double mu;

    public void testSunContrib() throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           Math.tan(0.001745329) * Math.cos(2 * Math.PI / 3),
                                           Math.tan(0.001745329) * Math.sin(2 * Math.PI / 3),
                                           0.1, 2, Frame.getJ2000(), date, mu);
        double period = 2 * Math.PI * orbit.getA() * Math.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(new Sun()));

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

    public void testMoonContrib() throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit =
            new EquinoctialOrbit(42164000,10e-3,10e-3,
                                      Math.tan(0.001745329) * Math.cos(2 * Math.PI / 3),
                                      Math.tan(0.001745329) * Math.sin(2 * Math.PI / 3),
                                      0.1, 2, Frame.getJ2000(), date, mu);
        double period = 2 * Math.PI * orbit.getA() * Math.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(new Moon()));

        // set up step handler to perform checks
        calc.setMasterMode(Math.floor(period), new ReferenceChecker(date) {
            private static final long serialVersionUID = -4725658720642817168L;
            protected double hXRef(double t) {
                return -0.909227e-3 - 0.309607e-10 * t + 2.68116e-5 *
                Math.cos(5.29808e-6*t) - 1.46451e-5 * Math.sin(5.29808e-6*t);
            }
            protected double hYRef(double t) {
                return 1.48482e-3 + 1.57598e-10 * t + 1.47626e-5 *
                Math.cos(5.29808e-6*t) - 2.69654e-5 * Math.sin(5.29808e-6*t);
            }
        });
        AbsoluteDate finalDate = new AbsoluteDate(date, 31 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    private static abstract class ReferenceChecker implements OrekitFixedStepHandler {

        private final AbsoluteDate reference;

        protected ReferenceChecker(AbsoluteDate reference) {
            this.reference = reference;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            double t = currentState.getDate().minus(reference);
            assertEquals(hXRef(t), currentState.getHx(), 1e-4);
            assertEquals(hYRef(t), currentState.getHy(), 1e-4);
        }

        protected abstract double hXRef(double t);

        protected abstract double hYRef(double t);

    }

    public void setUp() {
        mu = 3.986e14;
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(ThirdBodyAttractionTest.class);
    }
}
