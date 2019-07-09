/* Copyright 2002-2017 CS Systèmes d'Information
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.attitudes.Attitude;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** Compared propagation of a LEO in Earth-centered inertial and
 * non-inertial frames: EME2000 and ITRF.
 * <p>
 * All trajectories output in EME2000.
 * @author Guillaume Obrecht
 *
 */
public class TestCaseRotatingEarth {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            File home       = new File(System.getProperty("user.home"));
            File orekitData = new File(home, "orekit-data");
            if (!orekitData.exists()) {
                System.err.format(Locale.US, "Failed to find %s folder%n",
                                  orekitData.getAbsolutePath());
                System.err.format(Locale.US, "You need to download %s from the %s page and unzip it in %s for this tutorial to work%n",
                                  "orekit-data.zip", "https://www.orekit.org/forge/projects/orekit/files",
                                  home.getAbsolutePath());
                System.exit(1);
            }
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            // gravitation coefficient
            double mu =  3.986004415e+14;

            final Frame inertialFrame    = FramesFactory.getEME2000();
            final Frame nonInertialFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            final Frame outputFrame      = inertialFrame;

            // Initial date
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());

            // Initial position-velocity
            final PVCoordinates inertialInitialPV = new PVCoordinates(new Vector3D(6371000 + 300000, 0, 1000),
                                                                      new Vector3D(0, 7800, 0));
            final PVCoordinates nonInertialInitialPV =
                            inertialFrame.getTransformTo(nonInertialFrame, initialDate).transformPVCoordinates(inertialInitialPV);

            // Integrator parameters
            final double outputStep = 30;
            final double minStep = 0.001;
            final double maxstep = 1000.0;
            final double positionTolerance = 0.001;
            final OrbitType propagationType = OrbitType.CARTESIAN;

            // Get orbital period
            final Orbit referenceOrbit = new KeplerianOrbit(inertialInitialPV, inertialFrame, initialDate, mu);
            final double integrationTime = referenceOrbit.getKeplerianPeriod();



            // Integration in inertial Earth-centered frame
            System.out.println("1- Propagation in EME2000 (pseudo_inertial: " + inertialFrame.isPseudoInertial() + ")");

            // Define orbits with and without central body
            final Orbit initialOrbit1 = new CartesianOrbit(inertialInitialPV, inertialFrame, initialDate, mu);

            // Initial spacecraft state definition
            SpacecraftState initialState1 = new SpacecraftState(initialOrbit1);

            // Create integrator
            double[][] tolerances1 = NumericalPropagator.tolerances(positionTolerance, initialOrbit1, propagationType);
            AdaptiveStepsizeIntegrator integrator =
                    new DormandPrince853Integrator(minStep, maxstep, tolerances1[0], tolerances1[1]);

            // Create propagator
            NumericalPropagator propagator1 = new NumericalPropagator(integrator);
            propagator1.setOrbitType(propagationType);
            propagator1.setInitialState(initialState1);
            propagator1.setMasterMode(outputStep, new TutorialStepHandler("testEarth1.txt", outputFrame));

            // Propagation
            SpacecraftState finalState1 = propagator1.propagate(initialDate.shiftedBy(integrationTime));

            // Integration in rotating (non-inertial) Earth-centered frame
            System.out.println("2- Propagation in ITRF (pseudo_inertial: " + nonInertialFrame.isPseudoInertial() + ")");

            // Define orbits with and without central body
            final AbsolutePVCoordinates initialOrbit2 = new AbsolutePVCoordinates(nonInertialFrame, initialDate, nonInertialInitialPV);
            
            // Initial spacecraft state definition
            // Arbitrary attitude to define SpacecraftState (not yet modified to provide default attitude in non-inertial frames)
            Attitude arbitraryAttitude2 = new Attitude(nonInertialFrame,
                                                       new TimeStampedAngularCoordinates(initialDate,
                                                                                         new PVCoordinates(Vector3D.PLUS_I,
                                                                                                           Vector3D.PLUS_J),
                                                                                         new PVCoordinates(Vector3D.PLUS_I,
                                                                                                           Vector3D.PLUS_J)));

            SpacecraftState initialState2 = new SpacecraftState(initialOrbit2, arbitraryAttitude2);

            // Create integrator
            double[][] tolerances2 = NumericalPropagator.tolerances(positionTolerance, initialOrbit2);
            AdaptiveStepsizeIntegrator integrator2 =
                    new DormandPrince853Integrator(minStep, maxstep, tolerances2[0], tolerances2[1]);

            // Create propagator
            NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
            propagator2.setOrbitType(null);
            propagator2.setInitialState(initialState2);
            propagator2.setMasterMode(outputStep, new TutorialStepHandler("testEarth2.txt", outputFrame));
            propagator2.setMu(mu);

            // Inertial force model

            ForceModel inertia = new InertialForces(inertialFrame);
            propagator2.addForceModel(inertia);

            // Propagation
            SpacecraftState finalState2 = propagator2.propagate(initialDate.shiftedBy(integrationTime));

            // Compare final position
            final Vector3D pos1 = finalState1.getPVCoordinates(outputFrame).getPosition();
            final Vector3D pos2 = finalState2.getPVCoordinates(outputFrame).getPosition();
            System.out.format(Locale.US, "Errors of trajectory 2 wrt trajectory 1: %.6f [m]%n",
                              Vector3D.distance(pos1, pos2));

        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
        }
    }

    /** Specialized step handler.
     * <p>This class extends the step handler in order to print on the output stream at the given step.<p>
     * @author Pascal Parraud
     */
    private static class TutorialStepHandler implements OrekitFixedStepHandler {

        private File        outFile;
        private PrintWriter out;

        private Frame outputFrame;

        private TutorialStepHandler(final String fileName, final Frame frame) throws OrekitException {
            try {
                outFile = new File(new File(System.getProperty("user.home")), fileName);
                out = new PrintWriter(outFile, "UTF-8");
                outputFrame = frame;
            } catch (IOException ioe) {
                throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
            }
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {

                System.out.print(".");

                final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);

                out.format(Locale.US, "%s %14.6f %14.6f %14.6f %14.9f %14.9f %14.9f%n",
                           currentState.getDate(),
                           pv.getPosition().getX(),
                           pv.getPosition().getY(),
                           pv.getPosition().getZ(),
                           pv.getVelocity().getX(),
                           pv.getVelocity().getY(),
                           pv.getVelocity().getZ());


                if (isLast) {
                    final PVCoordinates finalPv = currentState.getPVCoordinates(outputFrame);
                    System.out.println();
                    System.out.format(Locale.US, "%s %14.6f %14.6f %14.6f %14.9f %14.9f %14.9f%n",
                                      currentState.getDate(),
                                      finalPv.getPosition().getX(),
                                      finalPv.getPosition().getY(),
                                      finalPv.getPosition().getZ(),
                                      finalPv.getVelocity().getX(),
                                      finalPv.getVelocity().getY(),
                            finalPv.getVelocity().getZ());
                    System.out.println();
                    out.close();
                    System.out.println("trajectory saved in " + outFile.getAbsolutePath());
                }
            } catch (OrekitException oe) {
                System.err.println(oe.getMessage());
            }
        }
    }

}
