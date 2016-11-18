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

package fr.cs.examples.conversion;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
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
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.conversion.FiniteDifferencePropagatorConverter;
import org.orekit.propagation.conversion.KeplerianPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorConverter;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for propagator conversion.
 * @author Pascal Parraud
 */
public class PropagatorConversion {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // gravity field
            NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(2, 0);
            double mu =  provider.getMu();

            // inertial frame
            Frame inertialFrame = FramesFactory.getEME2000();

            // Initial date
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000,
                                                        TimeScalesFactory.getUTC());

            // Initial orbit (GTO)
            final double a     = 24396159;                // semi major axis in meters
            final double e     = 0.72831215;              // eccentricity
            final double i     = FastMath.toRadians(7);   // inclination
            final double omega = FastMath.toRadians(180); // perigee argument
            final double raan  = FastMath.toRadians(261); // right ascention of ascending node
            final double lM    = 0;                       // mean anomaly
            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                    inertialFrame, initialDate, mu);
            final double period = initialOrbit.getKeplerianPeriod();

            // Initial state definition
            final SpacecraftState initialState = new SpacecraftState(initialOrbit);

            // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
            final double minStep    = 0.001;
            final double maxStep    = 1000.;
            final double dP         = 1.e-2;
            final OrbitType orbType = OrbitType.CARTESIAN;
            final double[][] tol = NumericalPropagator.tolerances(dP, initialOrbit, orbType);
            final AbstractIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep,
                                                                                 tol[0], tol[1]);

            // Propagator
            NumericalPropagator numProp = new NumericalPropagator(integrator);
            numProp.setInitialState(initialState);
            numProp.setOrbitType(orbType);

            // Force Models:
            // 1 - Perturbing gravity field (only J2 is considered here)
            ForceModel gravity = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);

            // Add force models to the propagator
            numProp.addForceModel(gravity);

            // Propagator factory
            PropagatorBuilder builder = new KeplerianPropagatorBuilder(initialOrbit, PositionAngle.TRUE, dP);

            // Propagator converter
            PropagatorConverter fitter = new FiniteDifferencePropagatorConverter(builder, 1.e-6, 5000);

            // Resulting propagator
            KeplerianPropagator kepProp = (KeplerianPropagator)fitter.convert(numProp, 2*period, 251);

            // Step handlers
            StatesHandler numStepHandler = new StatesHandler();
            StatesHandler kepStepHandler = new StatesHandler();

            // Set up operating mode for the propagator as master mode
            // with fixed step and specialized step handler
            numProp.setMasterMode(60., numStepHandler);
            kepProp.setMasterMode(60., kepStepHandler);

            // Extrapolate from the initial to the final date
            numProp.propagate(initialDate.shiftedBy(10.*period));
            kepProp.propagate(initialDate.shiftedBy(10.*period));

            // retrieve the states
            List<SpacecraftState> numStates = numStepHandler.getStates();
            List<SpacecraftState> kepStates = kepStepHandler.getStates();

            // Print the results on the output file
            File output = new File(new File(System.getProperty("user.home")), "elements.dat");
            try (final PrintStream stream = new PrintStream(output, "UTF-8")) {
                stream.println("# date Anum Akep Enum Ekep Inum Ikep LMnum LMkep");
                for (SpacecraftState numState : numStates) {
                    for (SpacecraftState kepState : kepStates) {
                        if (numState.getDate().compareTo(kepState.getDate()) == 0) {
                            stream.println(numState.getDate()
                                           + " " + numState.getA()
                                           + " " + kepState.getA()
                                           + " " + numState.getE()
                                           + " " + kepState.getE()
                                           + " " + FastMath.toDegrees(numState.getI())
                                           + " " + FastMath.toDegrees(kepState.getI())
                                           + " " + FastMath.toDegrees(MathUtils.normalizeAngle(numState.getLM(), FastMath.PI))
                                           + " " + FastMath.toDegrees(MathUtils.normalizeAngle(kepState.getLM(), FastMath.PI)));
                            break;
                        }
                    }
                }
            }
            System.out.println("Results saved as file " + output);

            File output1 = new File(new File(System.getProperty("user.home")), "elts_pv.dat");
            try (final PrintStream stream = new PrintStream(output1, "UTF-8")) {
                stream.println("# date pxn pyn pzn vxn vyn vzn pxk pyk pzk vxk vyk vzk");
                for (SpacecraftState numState : numStates) {
                    for (SpacecraftState kepState : kepStates) {
                        if (numState.getDate().compareTo(kepState.getDate()) == 0) {
                            final double pxn = numState.getPVCoordinates().getPosition().getX();
                            final double pyn = numState.getPVCoordinates().getPosition().getY();
                            final double pzn = numState.getPVCoordinates().getPosition().getZ();
                            final double vxn = numState.getPVCoordinates().getVelocity().getX();
                            final double vyn = numState.getPVCoordinates().getVelocity().getY();
                            final double vzn = numState.getPVCoordinates().getVelocity().getZ();
                            final double pxk = kepState.getPVCoordinates().getPosition().getX();
                            final double pyk = kepState.getPVCoordinates().getPosition().getY();
                            final double pzk = kepState.getPVCoordinates().getPosition().getZ();
                            final double vxk = kepState.getPVCoordinates().getVelocity().getX();
                            final double vyk = kepState.getPVCoordinates().getVelocity().getY();
                            final double vzk = kepState.getPVCoordinates().getVelocity().getZ();
                            stream.println(numState.getDate()
                                           + " " + pxn + " " + pyn + " " + pzn
                                           + " " + vxn + " " + vyn + " " + vzn
                                           + " " + pxk + " " + pyk + " " + pzk
                                           + " " + vxk + " " + vyk + " " + vzk);
                            break;
                        }
                    }
                }
            }
            System.out.println("Results saved as file " + output1);

        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            System.exit(1);
        }
    }

    /** Specialized step handler.
     * <p>This class extends the step handler in order to handle states at each step.<p>
     */
    private static class StatesHandler implements OrekitFixedStepHandler {

        /** Points. */
        private final List<SpacecraftState> states;

        public StatesHandler() {
            // prepare an empty list of SpacecraftState
            states = new ArrayList<SpacecraftState>();
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {

            // add the current state
            states.add(currentState);

        }

        /** Get the list of handled orbital elements.
         * @return orbital elements list
         */
        public List<SpacecraftState> getStates() {
            return states;
        }

    }

}
