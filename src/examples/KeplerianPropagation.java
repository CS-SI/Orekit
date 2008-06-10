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


import java.text.ParseException;

import org.apache.commons.math.ode.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.ode.FirstOrderIntegrator;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.KeplerianOrbit;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.analytical.KeplerianPropagator;
import fr.cs.orekit.propagation.numerical.NumericalModel;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;

/** The aim of this tutorial is to manipulate spacecraft states, orbital parameters
 * and keplerian propagation
 * @version $Revision$ $Date$
 */
public class KeplerianPropagation {

    public static void keplerianPropagation() throws ParseException, OrekitException {

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

        double mass = 2500; // mass of the spacecraft in Kg

        // date and frame

        AbsoluteDate initialDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                    new ChunkedTime(23, 30, 00.000),
                                                    UTCScale.getInstance());

        Frame inertialFrame = Frame.getJ2000();

        // OREKIT objects construction:

        Orbit initialOrbit =
            new KeplerianOrbit(a, e, i, omega, raan, lv,
                                    KeplerianOrbit.MEAN_ANOMALY, inertialFrame, initialDate, mu);

        SpacecraftState initialState = new SpacecraftState(initialOrbit, mass);

        /* ***************** */
        /*   Extrapolation   */
        /* ***************** */

        // Simple Keplerian extrapolation

        KeplerianPropagator kepler = new KeplerianPropagator(initialState);

        double deltaT = 1000; // extrapolation lenght in seconds

        AbsoluteDate finalDate = new AbsoluteDate(initialDate, deltaT);
        SpacecraftState finalState = kepler.propagate(finalDate);

        System.out.println(" Final parameters with deltaT = +1000 s : " +
                           finalState.getOrbit());

        deltaT = -1000; // extrapolation lenght

        finalDate = new AbsoluteDate(initialDate, deltaT);
        finalState = kepler.propagate(finalDate);

        System.out.println(" Final parameters with deltaT = -1000 s : " +
                           finalState.getOrbit());

        // numerical propagation with no perturbation (only keplerian movement)
        // we use a very simple integrator with a fixed step : Runge Kutta

        FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(1); // the step is one second

        NumericalModel propagator = new NumericalModel(mu, integrator);

        finalState = propagator.propagate(initialState, finalDate);

        System.out.println(" Final parameters with deltaT = -1000 s : " +
                           finalState.getOrbit());
    }

}
