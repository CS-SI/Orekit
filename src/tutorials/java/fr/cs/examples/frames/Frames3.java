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

package fr.cs.examples.frames;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Locale;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.YawSteering;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

/** Orekit tutorial for computing spacecraft related data.
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
public class Frames3 {

    public static void main(String[] args) {
        try {

            // configure Orekit and printing format
            String className = "/" + Frames3.class.getName().replaceAll("\\.", "/") + ".class";
            File f = new File(Frames3.class.getResource(className).toURI().getPath());
            File resourcesDir = null;
            while (resourcesDir == null || !resourcesDir.exists()) {
                f = f.getParentFile();
                if (f == null) {
                    System.err.println("cannot find resources directory");
                }
                resourcesDir = new File(new File(new File(new File(f, "src"), "test"), "resources"), "regular-data");
            }
            DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(resourcesDir));

            // Initial state definition :
            // ==========================

            // Date
            // ****
            AbsoluteDate initialDate =
                new AbsoluteDate(new DateComponents(1970, 04, 07),
                                 TimeComponents.H00, TimeScalesFactory.getUTC());

            // Orbit
            // *****
            // The Sun is in the orbital plane for raan ~ 202
            double mu = 3.986004415e+14; // gravitation coefficient
            Frame eme2000 = FramesFactory.getEME2000(); // inertial frame
            Orbit orbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4,
                                            FastMath.toRadians(50.),
                                            FastMath.toRadians(220.),
                                            FastMath.toRadians(5.300), PositionAngle.MEAN,
                                            eme2000,
                                            initialDate,
                                            mu);

            // Attitude laws
            // *************

            // Earth
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                   Constants.WGS84_EARTH_FLATTENING,
                                                   earthFrame);

            // Target pointing attitude provider over satellite nadir at date, without yaw compensation
            NadirPointing nadirLaw = new NadirPointing(eme2000, earth);

            // Target pointing attitude provider with yaw compensation
            final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            YawSteering yawSteeringLaw =
                new YawSteering(eme2000, nadirLaw, sun, Vector3D.MINUS_I);

            // Propagator : Eckstein-Hechler analytic propagator
            Propagator propagator =
                new EcksteinHechlerPropagator(orbit, yawSteeringLaw,
                                              Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                              Constants.EIGEN5C_EARTH_MU,
                                              Constants.EIGEN5C_EARTH_C20,
                                              Constants.EIGEN5C_EARTH_C30,
                                              Constants.EIGEN5C_EARTH_C40,
                                              Constants.EIGEN5C_EARTH_C50,
                                              Constants.EIGEN5C_EARTH_C60);

            // Let's write the results in a file in order to draw some plots.
            propagator.setMasterMode(10, new OrekitFixedStepHandler() {

                PrintStream out = null;

                public void init(SpacecraftState s0, AbsoluteDate t, double step)
                    throws OrekitException {
                    try {
                        File file = new File(System.getProperty("user.home"), "XYZ.dat");
                        System.out.println("Results written to file: " + file.getAbsolutePath());
                        out = new PrintStream(file, "UTF-8");
                        out.println("#time X Y Z Wx Wy Wz");
                    } catch (IOException ioe) {
                        throw new OrekitException(ioe,
                                                       LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                       ioe.getLocalizedMessage());
                    }
                }

                public void handleStep(SpacecraftState currentState, boolean isLast)
                    throws OrekitException {

                    // get the transform from orbit/attitude reference frame to spacecraft frame
                    Transform inertToSpacecraft = currentState.toTransform();

                    // get the position of the Sun in orbit/attitude reference frame
                    Vector3D sunInert = sun.getPVCoordinates(currentState.getDate(), currentState.getFrame()).getPosition();

                    // convert Sun position to spacecraft frame
                    Vector3D sunSat = inertToSpacecraft.transformPosition(sunInert);

                    // and the spacecraft rotational rate also
                    Vector3D spin = inertToSpacecraft.getRotationRate();

                    // Lets calculate the reduced coordinates
                    double sunX = sunSat.getX() / sunSat.getNorm();
                    double sunY = sunSat.getY() / sunSat.getNorm();
                    double sunZ = sunSat.getZ() / sunSat.getNorm();

                    out.format(Locale.US, "%s %12.3f %12.3f %12.3f %12.7f %12.7f %12.7f%n",
                               currentState.getDate(), sunX, sunY, sunZ,
                               spin.getX(), spin.getY(), spin.getZ());

                    if (isLast) {
                        out.close();
                    }
                }

            });

            System.out.println("Running...");
            propagator.propagate(initialDate.shiftedBy(6000));

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
        }
    }

}
