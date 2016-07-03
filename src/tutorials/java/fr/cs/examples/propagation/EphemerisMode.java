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

package fr.cs.examples.propagation;

import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for ephemeris mode propagation.
 * <p>This tutorial shows a basic usage of the ephemeris mode in conjunction with a numerical propagator.<p>
 * @author Pascal Parraud
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
            double i = FastMath.toRadians(7); // inclination
            double omega = FastMath.toRadians(180); // perigee argument
            double raan = FastMath.toRadians(261); // right ascension of ascending node
            double lM = 0; // mean anomaly

            // Inertial frame
            Frame inertialFrame = FramesFactory.getEME2000();

            // Initial date in UTC time scale
            TimeScale utc = TimeScalesFactory.getUTC();
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);

            // gravitation coefficient
            double mu =  3.986004415e+14;

            // Orbit construction as Keplerian
            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                    inertialFrame, initialDate, mu);

            // Initialize state
            SpacecraftState initialState = new SpacecraftState(initialOrbit);

            // Numerical propagation with no perturbation (only keplerian movement)
            // Using a very simple integrator with a fixed step: classical Runge-Kutta
            double stepSize = 10;  // the step is ten seconds
            AbstractIntegrator integrator = new ClassicalRungeKuttaIntegrator(stepSize);
            NumericalPropagator propagator = new NumericalPropagator(integrator);

            // Set the propagator to ephemeris mode
            propagator.setEphemerisMode();

            // Initialize propagation
            propagator.setInitialState(initialState);

            // Propagation with storage of the results in an integrated ephemeris
            SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(6000));

            System.out.println(" Numerical propagation :");
            System.out.println("  Final date : " + finalState.getDate());
            System.out.println("  " + finalState.getOrbit());

            // Getting the integrated ephemeris
            BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

            System.out.println(" Ephemeris defined from " + ephemeris.getMinDate() + " to " + ephemeris.getMaxDate());

            System.out.println(" Ephemeris propagation :");
            AbsoluteDate intermediateDate = initialDate.shiftedBy(3000);
            SpacecraftState intermediateState = ephemeris.propagate(intermediateDate);
            System.out.println("  date :  " + intermediateState.getDate());
            System.out.println("  " + intermediateState.getOrbit());

            intermediateDate = finalState.getDate();
            intermediateState = ephemeris.propagate(intermediateDate);
            System.out.println("  date :  " + intermediateState.getDate());
            System.out.println("  " + intermediateState.getOrbit());

            intermediateDate = initialDate.shiftedBy(-1000);
            System.out.println();
            System.out.println("Attempting to propagate to date " + intermediateDate +
                               " which is OUT OF RANGE");
            System.out.println("This propagation attempt should fail, " +
                               "so an error message shoud appear below, " +
                               "this is expected and shows that errors are handled correctly");
            intermediateState = ephemeris.propagate(intermediateDate);

            // these two print should never happen as en exception should have been triggered
            System.out.println("  date :  " + intermediateState.getDate());
            System.out.println("  " + intermediateState.getOrbit());

        } catch (OrekitException oe) {
            System.out.println(oe.getMessage());
        }
    }
}
