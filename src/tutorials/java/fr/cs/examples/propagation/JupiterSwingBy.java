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
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.SingleBodyAbsoluteAttraction;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;

/** Swing-by trajectory about Jupiter compared in EME2000, ICRF and
 * Jupiter-centered inertial reference frame.
 *
 * <p>
 * Case 1:
 * <ul>
 * <li>Integration frame: EME2000</li>
 * <li>Representation of the trajectory: AbsolutePVCoordinates</li>
 * <li>No central body</li>
 * <li>SingleBodyAbsoluteAttraction with Jupiter</li>
 * <li>SingleBodyAbsoluteAttraction with the Sun and all remaining planets</li>
 * <li>InertialForces between EME2000 and ICRF</li>
 * </ul>
 *
 * Case 2:
 * <ul>
 * <li>Integration frame: ICRF</li>
 * <li> Representation of the trajectory: AbsolutePVCoordinates</li>
 * <li>No central body</li>
 * <li>SingleBodyAbsoluteAttraction with Jupiter</li>
 * <li>SingleBodyAbsoluteAttraction with the Sun and all remaining planets</li>
 * </ul>
 *
 * Case 3:
 * <ul>
 * <li>Integration frame: Jupiter-centered inertial</li>
 * <li>Representation of the trajectory: Cartesian orbit</li>
 * <li>Central body: Jupiter</li>
 * <li>ThirdBodyAttraction with the Sun and all remaining planets</li>
 * </ul>
 *
 * All trajectories output in Jovian frame.
 *
 * @author Guillaume Obrecht
 * @author Luc Maisonobe
 *
 */

public class JupiterSwingBy {

    public static void main(String[] args) throws OrekitException {

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

        // Time settings
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        double integrationTime = 100000;
        double outputStep = 500;

        // Initial conditions
        final double x = 69911000 + 100000000;
        final double y = -2000000000;
        final double z = 0;
        final double Vx = 0;
        final double Vy = 50000;
        final double Vz = 0;
        final PVCoordinates initialScWrthJupiter = new PVCoordinates(new Vector3D(x,y,z), new Vector3D(Vx,Vy,Vz));

        // Integration parameters
        final double minStep = 1.0;
        final double maxstep = 7200.0;
        final double positionTolerance = 1.0;
        final double initialStepSize = 120.0;

        // Load Celestial bodies
        final CelestialBody   jupiter     = CelestialBodyFactory.getJupiter();
        final CelestialBody[] otherBodies = {
            CelestialBodyFactory.getSun(),
            CelestialBodyFactory.getMercury(),
            CelestialBodyFactory.getVenus(),
            CelestialBodyFactory.getEarthMoonBarycenter(),
            CelestialBodyFactory.getMars(),
            CelestialBodyFactory.getSaturn(),
            CelestialBodyFactory.getUranus(),
            CelestialBodyFactory.getNeptune()
        };
        final Frame eme2000     = FramesFactory.getEME2000();
        final Frame icrf        = FramesFactory.getICRF();
        final Frame jovianFrame = jupiter.getInertiallyOrientedFrame();

        final Frame outputFrame = jovianFrame;

        // 1: Propagation in Earth-centered inertial reference frame

        final Frame integrationFrame1 = eme2000;

        System.out.print("1- Propagation in Earth-centered inertial reference frame (pseudo_inertial: " + integrationFrame1.isPseudoInertial() + ")\n");

        final Transform initialTransform1 = jovianFrame.getTransformTo(integrationFrame1, initialDate);
        final PVCoordinates initialConditions1 = initialTransform1.transformPVCoordinates(initialScWrthJupiter);

        final AbsolutePVCoordinates initialAbsPva1 = new AbsolutePVCoordinates(integrationFrame1, initialDate, initialConditions1);
        final double[][] tolerances1 = NumericalPropagator.tolerances(positionTolerance, initialAbsPva1);

        AdaptiveStepsizeIntegrator integrator1 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances1[0], tolerances1[1]);
        integrator1.setInitialStepSize(initialStepSize);

        NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setOrbitType(null);  // propagate as absolute position-velocity-acceleration
        propagator1.addForceModel(new SingleBodyAbsoluteAttraction(jupiter));
        for (final CelestialBody body : otherBodies) {
            propagator1.addForceModel(new SingleBodyAbsoluteAttraction(body));
        }
        propagator1.addForceModel(new InertialForces(icrf));
        propagator1.setIgnoreCentralAttraction(true);
        propagator1.setInitialState(new SpacecraftState(initialAbsPva1));
        propagator1.setMasterMode(outputStep, new TutorialStepHandler("testJupiter1.txt", outputFrame));

        SpacecraftState finalState1 = propagator1.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv1 = finalState1.getPVCoordinates(outputFrame);



        // 2: Propagation in Celestial reference frame

        final Frame integrationFrame2 = icrf;

        System.out.print("2- Propagation in Celestial reference frame (pseudo_inertial: " + integrationFrame2.isPseudoInertial() + ")\n");

        final Transform initialTransform2 = jovianFrame.getTransformTo(integrationFrame2, initialDate);
        final PVCoordinates initialConditions2 = initialTransform2.transformPVCoordinates(initialScWrthJupiter);

        final AbsolutePVCoordinates initialAbsPva2 = new AbsolutePVCoordinates(integrationFrame2, initialDate, initialConditions2);

        final double[][] tolerances2 = NumericalPropagator.tolerances(positionTolerance, initialAbsPva2);

        AdaptiveStepsizeIntegrator integrator2 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances2[0], tolerances2[1]);
        integrator2.setInitialStepSize(initialStepSize);

        NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
        propagator2.setOrbitType(null);
        propagator2.addForceModel(new SingleBodyAbsoluteAttraction(jupiter));
        for (final CelestialBody body : otherBodies) {
            propagator2.addForceModel(new SingleBodyAbsoluteAttraction(body));
        }
        propagator2.setIgnoreCentralAttraction(true);
        propagator2.setInitialState(new SpacecraftState(initialAbsPva2));
        propagator2.setMasterMode(outputStep, new TutorialStepHandler("testJupiter2.txt", outputFrame));


        SpacecraftState finalState2 = propagator2.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv2 = finalState2.getPVCoordinates(outputFrame);



        // 3: Propagation in Jupiter centered frame: inertial, central body

        final Frame integrationFrame3 = jovianFrame;

        System.out.print("3- Propagation in Jupiter-centered reference frame (pseudo_inertial: " + integrationFrame3.isPseudoInertial() + ")\n");

        final Orbit initialOrbit3 = new CartesianOrbit(initialScWrthJupiter, integrationFrame3, initialDate, jupiter.getGM());

        final double[][] tolerances3 = NumericalPropagator.tolerances(positionTolerance, initialOrbit3, OrbitType.CARTESIAN);
        AdaptiveStepsizeIntegrator integrator3 =
                new DormandPrince853Integrator(minStep, maxstep, tolerances3[0], tolerances3[1]);
        integrator3.setInitialStepSize(initialStepSize);

        NumericalPropagator propagator3 = new NumericalPropagator(integrator3);
        propagator3.setOrbitType(OrbitType.CARTESIAN);     // propagate as regular orbit
        propagator3.setMu(jupiter.getGM());
        for (final CelestialBody body : otherBodies) {
            propagator3.addForceModel(new ThirdBodyAttraction(body));
        }
        propagator3.setInitialState(new SpacecraftState(initialOrbit3));
        propagator3.setMasterMode(outputStep, new TutorialStepHandler("testJupiter3.txt", outputFrame));

        SpacecraftState finalState3 = propagator3.propagate(initialDate.shiftedBy(integrationTime));
        final PVCoordinates pv3 = finalState3.getPVCoordinates(outputFrame);


        // Compare final position
        final Vector3D pos1 = pv1.getPosition();
        final Vector3D pos2 = pv2.getPosition();
        final Vector3D pos3 = pv3.getPosition();
        System.out.format(Locale.US, "Differences between trajectories:%n");
        System.out.format(Locale.US, "    1/3: %10.6f [m]%n", Vector3D.distance(pos1, pos3));
        System.out.format(Locale.US, "    2/3: %10.6f [m]%n", Vector3D.distance(pos2, pos3));
        System.out.format(Locale.US, "    1/2: %10.6f [m]%n", Vector3D.distance(pos1, pos2));        

    }






    private static class TutorialStepHandler implements OrekitFixedStepHandler {

        private File        outFile;
        private PrintWriter out;

        private Frame outputFrame;

        private TutorialStepHandler(final String fileName, final Frame frame)
            throws OrekitException {
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
                // Choose here in which reference frame to output the trajectory

                System.out.print(".");

                final PVCoordinates pv = currentState.getPVCoordinates(outputFrame);
                final AbsoluteDate d = currentState.getDate();

                out.format(Locale.US, "%s %9.3f %9.3f %9.3f%n",
                            d,
                            pv.getPosition().getX(),
                            pv.getPosition().getY(),
                            pv.getPosition().getZ());

                if (isLast) {
                    out.close();
                    System.out.println();
                    System.out.format(Locale.US, "%s %12.0f %12.0f %12.0f %12.0f %12.0f %12.0f%n",
                            d,
                            pv.getPosition().getX(),
                            pv.getPosition().getY(),
                            pv.getPosition().getZ(),
                            pv.getVelocity().getX(),
                            pv.getVelocity().getY(),
                            pv.getVelocity().getZ());
                    System.out.println();
                    System.out.println("trajectory saved in " + outFile.getAbsolutePath());
                }
            } catch (OrekitException oe) {
                System.err.println(oe.getMessage());
            }
        }
    }

}
