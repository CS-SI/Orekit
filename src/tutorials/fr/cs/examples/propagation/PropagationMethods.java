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

package fr.cs.examples.propagation;

import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;

/** Orekit tutorial for various propagation methods.
 * <p>The aim of this tutorial is to manipulate spacecraft states
 * and to propagate them using different methods.<p>
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class PropagationMethods {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // physical constants :
            double mu =  3.9860064e+14; // gravitation coefficient

            //  Initial state definition :

            // parameters :
            double a = 24396159; // semi major axis in meters
            double e = 0.72831215; // eccentricity
            double i = Math.toRadians(7); // inclination
            double omega = Math.toRadians(180); // perigee argument
            double raan = Math.toRadians(261); // right ascention of ascending node
            double lv = 0; // mean anomaly

            // date and frame
            AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                        new TimeComponents(23, 30, 00.000),
                                                        UTCScale.getInstance());

            Frame inertialFrame = Frame.getEME2000();

            // OREKIT objects construction:

            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lv,
                                                    KeplerianOrbit.MEAN_ANOMALY,
                                                    inertialFrame, initialDate, mu);

            /* ***************** */
            /*   Extrapolation   */
            /* ***************** */

            // Simple Keplerian extrapolation
            KeplerianPropagator kepler = new KeplerianPropagator(initialOrbit);

            double deltaT = 1000; // extrapolation length in seconds

            AbsoluteDate finalDate = new AbsoluteDate(initialDate, deltaT);
            SpacecraftState finalState = kepler.propagate(finalDate);
            System.out.println(" Final parameters with deltaT = " + deltaT + " s : " +
                               finalState.getOrbit());

            deltaT = -1000; // extrapolation length in seconds

            finalDate = new AbsoluteDate(initialDate, deltaT);
            finalState = kepler.propagate(finalDate);
            System.out.println(" Final parameters with deltaT = " + deltaT + " s : " +
                               finalState.getOrbit());

            // numerical propagation with no perturbation (only keplerian movement)
            // we use a very simple integrator with a fixed step: classical Runge-Kutta
            double stepSize = 1.;  // the step is one second
            FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(stepSize);
            NumericalPropagator propagator = new NumericalPropagator(integrator);
            propagator.setInitialState(new SpacecraftState(initialOrbit));
            finalState = propagator.propagate(finalDate);
            System.out.println(" Final parameters with deltaT = " + deltaT + " s : " +
                               finalState.getOrbit());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
