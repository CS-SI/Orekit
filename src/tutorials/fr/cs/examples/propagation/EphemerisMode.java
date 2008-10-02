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
import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.UTCScale;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for ephemeris mode propagation.
 * <p>This tutorial shows a basic usage of the ephemeris mode in conjunction with a numerical propagator.<p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class EphemerisMode {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // Initial orbit parameters
            double a = 24396159; // semi major axis in meters
            double e = 0.72831215; // eccentricity
            double i = Math.toRadians(7); // inclination
            double omega = Math.toRadians(180); // perigee argument
            double raan = Math.toRadians(261); // right ascension of ascending node
            double lv = 0; // mean anomaly

            // Inertial frame
            Frame inertialFrame = Frame.getEME2000();

            // Initial date in UTC time scale
            TimeScale utc = UTCScale.getInstance();
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);

            // gravitation coefficient
            double mu =  3.986004415e+14;

            // Orbit construction as Keplerian
            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lv,
                                                    KeplerianOrbit.MEAN_ANOMALY,
                                                    inertialFrame, initialDate, mu);

            // Numerical propagation with no perturbation (only keplerian movement)
            // Using a very simple integrator with a fixed step: classical Runge-Kutta
            double stepSize = 10.;  // the step is ten seconds
            FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(stepSize);
            NumericalPropagator propagator = new NumericalPropagator(integrator);

            // Set the propagator to ephemeris mode
            propagator.setEphemerisMode();
            
            // Initialize propagation
            SpacecraftState initialState = new SpacecraftState(initialOrbit);
            propagator.setInitialState(initialState);
            
            // Propagation with storage of the results in an integrated ephemeris
            SpacecraftState finalState = propagator.propagate(new AbsoluteDate(initialDate, 6000.));
            BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
            
            System.out.println(" Ephemeris defined from " + ephemeris.getMinDate() + " to " + ephemeris.getMaxDate());
            
            AbsoluteDate intermediateDate = new AbsoluteDate(initialDate, 3000.);
            SpacecraftState intermediateState = ephemeris.propagate(intermediateDate);
            System.out.println("  date :  " + intermediateState.getDate());
            System.out.println("  " + intermediateState.getOrbit());
            
            intermediateDate = new AbsoluteDate(initialDate, -214.);
            intermediateState = ephemeris.propagate(intermediateDate);
            System.out.println("  date :  " + intermediateState.getDate());
            System.out.println("  " + intermediateState.getOrbit());
            
            intermediateDate = initialDate;
            intermediateState = ephemeris.propagate(intermediateDate);
            System.out.println("  date :  " + intermediateState.getDate());
            System.out.println("  " + intermediateState.getOrbit());

            EquinoctialOrbit orbini = new EquinoctialOrbit(initialOrbit);
            System.out.println("  " + orbini.toString());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }
}
