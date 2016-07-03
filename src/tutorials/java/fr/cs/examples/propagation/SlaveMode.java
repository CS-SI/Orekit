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

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for slave mode propagation.
 * <p>This tutorial shows a basic usage of the slave mode in which the user drives all propagation steps.<p>
 * @author Pascal Parraud
 */
public class SlaveMode {

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

            // Simple extrapolation with Keplerian motion
            KeplerianPropagator kepler = new KeplerianPropagator(initialOrbit);

            // Set the propagator to slave mode (could be omitted as it is the default mode)
            kepler.setSlaveMode();

            // Overall duration in seconds for extrapolation
            double duration = 600.;

            // Stop date
            final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

            // Step duration in seconds
            double stepT = 60.;

            // Extrapolation loop
            int cpt = 1;
            for (AbsoluteDate extrapDate = initialDate;
                 extrapDate.compareTo(finalDate) <= 0;
                 extrapDate = extrapDate.shiftedBy(stepT))  {

                SpacecraftState currentState = kepler.propagate(extrapDate);
                System.out.println("step " + cpt++);
                System.out.println(" time : " + currentState.getDate());
                System.out.println(" " + currentState.getOrbit());

            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
