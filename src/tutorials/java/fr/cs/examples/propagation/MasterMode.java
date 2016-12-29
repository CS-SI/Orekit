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

import java.util.Locale;

import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for master mode propagation.
 * <p>This tutorial shows the interest of the master mode which hides the complex
 * internal mechanic of the propagation and just fulfills the user main needs.<p>
 * @author Fabien Maussion
 * @author Pascal Parraud
 */
public class MasterMode {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // gravitation coefficient
            double mu =  3.986004415e+14;

            // inertial frame
            Frame inertialFrame = FramesFactory.getEME2000();

            // Initial date
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000,
                                                        TimeScalesFactory.getUTC());

            // Initial orbit
            double a = 24396159; // semi major axis in meters
            double e = 0.72831215; // eccentricity
            double i = FastMath.toRadians(7); // inclination
            double omega = FastMath.toRadians(180); // perigee argument
            double raan = FastMath.toRadians(261); // right ascention of ascending node
            double lM = 0; // mean anomaly
            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                    inertialFrame, initialDate, mu);

            // Initial state definition
            SpacecraftState initialState = new SpacecraftState(initialOrbit);

            // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
            final double minStep = 0.001;
            final double maxstep = 1000.0;
            final double positionTolerance = 10.0;
            final OrbitType propagationType = OrbitType.KEPLERIAN;
            final double[][] tolerances =
                    NumericalPropagator.tolerances(positionTolerance, initialOrbit, propagationType);
            AdaptiveStepsizeIntegrator integrator =
                    new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

            // Propagator
            NumericalPropagator propagator = new NumericalPropagator(integrator);
            propagator.setOrbitType(propagationType);

            // Force Model (reduced to perturbing gravity field)
            final NormalizedSphericalHarmonicsProvider provider =
                    GravityFieldFactory.getNormalizedProvider(10, 10);
            ForceModel holmesFeatherstone =
                    new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                true),
                                                          provider);

            // Add force model to the propagator
            propagator.addForceModel(holmesFeatherstone);

            // Set up initial state in the propagator
            propagator.setInitialState(initialState);

            // Set up operating mode for the propagator as master mode
            // with fixed step and specialized step handler
            propagator.setMasterMode(60., new TutorialStepHandler());

            // Extrapolate from the initial to the final date
            SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(630.));
            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(finalState.getOrbit());
            System.out.format(Locale.US, "Final state:%n%s %12.3f %10.8f %10.6f %10.6f %10.6f %10.6f%n",
                              finalState.getDate(),
                              o.getA(), o.getE(),
                              FastMath.toDegrees(o.getI()),
                              FastMath.toDegrees(o.getPerigeeArgument()),
                              FastMath.toDegrees(o.getRightAscensionOfAscendingNode()),
                              FastMath.toDegrees(o.getTrueAnomaly()));

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

    /** Specialized step handler.
     * <p>This class extends the step handler in order to print on the output stream at the given step.<p>
     * @author Pascal Parraud
     */
    private static class TutorialStepHandler implements OrekitFixedStepHandler {

        private TutorialStepHandler() {
            //private constructor
        }

        public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
            System.out.println("          date                a           e" +
                               "           i         \u03c9          \u03a9" +
                               "          \u03bd");
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
            System.out.format(Locale.US, "%s %12.3f %10.8f %10.6f %10.6f %10.6f %10.6f%n",
                              currentState.getDate(),
                              o.getA(), o.getE(),
                              FastMath.toDegrees(o.getI()),
                              FastMath.toDegrees(o.getPerigeeArgument()),
                              FastMath.toDegrees(o.getRightAscensionOfAscendingNode()),
                              FastMath.toDegrees(o.getTrueAnomaly()));
            if (isLast) {
                System.out.println("this was the last step ");
                System.out.println();
            }
        }

    }

}
