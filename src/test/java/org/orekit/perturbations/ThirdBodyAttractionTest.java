/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.perturbations;

import java.io.FileNotFoundException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.GraggBulirschStoerIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.models.bodies.Moon;
import org.orekit.models.bodies.Sun;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.OrekitFixedStepHandler;
import org.orekit.propagation.numerical.forces.perturbations.ThirdBodyAttraction;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;


public class ThirdBodyAttractionTest extends TestCase {

    public void testSunContrib() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit = new EquinoctialOrbit(42164000,10e-3,10e-3,
                                           Math.tan(0.001745329)*Math.cos(2*Math.PI/3),
                                           Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                           0.1, 2, Frame.getJ2000(), date, mu);
        Sun sun = new Sun();

        // creation of the force model
        ThirdBodyAttraction TBA =  new ThirdBodyAttraction(sun);

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/orbit.getMu());

        // creation of the propagator
        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period, 0, 10e-5);
        NumericalPropagator calc = new NumericalPropagator(integrator);
        calc.addForceModel(TBA);

        // Step Handler
        calc.setMasterMode(Math.floor(period), new TBAStepHandler(TBAStepHandler.SUN, date));
        AbsoluteDate finalDate = new AbsoluteDate(date , 2*365*period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);
        assertTrue("incomplete test", false);

    }

    public void testMoonContrib()
        throws ParseException, OrekitException, DerivativeException,
               IntegratorException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit =
            new EquinoctialOrbit(42164000,10e-3,10e-3,
                                      Math.tan(0.001745329) * Math.cos(2 * Math.PI / 3),
                                      Math.tan(0.001745329) * Math.sin(2 * Math.PI / 3),
                                      0.1, 2, Frame.getJ2000(), date, mu);
        Moon moon = new Moon();

        // creation of the force model
        ThirdBodyAttraction TBA =  new ThirdBodyAttraction(moon);

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/orbit.getMu());

        // creation of the propagator
        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period, 0, 10e-5);
        NumericalPropagator calc = new NumericalPropagator(integrator);
        calc.addForceModel(TBA);

        // Step Handler
        calc.setMasterMode(Math.floor(period), new TBAStepHandler(TBAStepHandler.MOON, date));
        AbsoluteDate finalDate = new AbsoluteDate(date , 365*period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);
        assertTrue("incomplete test", false);

    }

    private double mu = 3.98600E14;

    private static class TBAStepHandler extends OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 8907114996643609848L;
        public static final int MOON = 1;
        public static final int SUN = 2;
        public static final int SUNandMOON = 3;
        private int type;
        AbsoluteDate date;

        private TBAStepHandler(int type, AbsoluteDate date) throws FileNotFoundException {
            this.type = type;
            this.date = date;
        }

        public void handleStep(double t, double[]y, boolean isLastStep) {
            if (type == MOON) {
                assertEquals(0, xMoon(t)-y[3], 1e-4);
                assertEquals(0, yMoon(t)-y[4], 1e-4);
            }
            if (type == SUN) {
                assertEquals(0, xSun(t)-y[3], 1e-4);
                assertEquals(0, ySun(t)-y[4], 1e-4);
            }
            if (type == SUNandMOON) {

            }
        }

        private double xMoon(double t) {
            return -0.909227e-3 - 0.309607e-10 * t + 2.68116e-5 *
            Math.cos(5.29808e-6*t) - 1.46451e-5 * Math.sin(5.29808e-6*t);
        }

        private double yMoon(double t) {
            return 1.48482e-3 + 1.57598e-10 * t + 1.47626e-5 *
            Math.cos(5.29808e-6*t) - 2.69654e-5 * Math.sin(5.29808e-6*t);
        }

        private double xSun(double t) {
            return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 *
            Math.cos(3.9820426e-7*t) - 7.59983e-5 * Math.sin(3.9820426e-7*t);
        }

        private double ySun(double t) {
            return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 *
            Math.cos(3.9820426e-7*t) + 17.6083e-5 * Math.sin(3.9820426e-7*t);
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            this.handleStep(currentState.getDate().minus(date), new double[] {0,0,0,currentState.getHx(), currentState.getHy()}, isLast);
        }

        public boolean requiresDenseOutput() {
            return false;
        }

        public void reset() {
        }

    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(ThirdBodyAttractionTest.class);
    }
}
