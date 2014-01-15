/* Copyright 2002-2014 CS Systèmes d'Information
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
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.YawSteering;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
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
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for computing spacecraft related data WITHOUT using the deprecated SpacecraftFrame.
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
public class Frames3 {

    public static void main(String[] args) {
        try {

            // configure Orekit and printing format
            Autoconfiguration.configureOrekit();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            final DecimalFormat d3 = new DecimalFormat("0.000", symbols);
            final DecimalFormat d7 = new DecimalFormat("0.0000000", symbols);

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
            double ae =  6378137.0; // equatorial radius in meter
            double f  =  1.0 / 298.257223563; // flattening
            Frame itrf2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
            BodyShape earth = new OneAxisEllipsoid(ae, f, itrf2005);

            // Target pointing attitude provider over satellite nadir at date, without yaw compensation
            NadirPointing nadirLaw = new NadirPointing(earth);

            // Target pointing attitude provider with yaw compensation
            final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            YawSteering yawSteeringLaw =
                new YawSteering(nadirLaw, sun, Vector3D.MINUS_I);

            // Propagator : Eckstein-Hechler analytic propagator
            final double c20 = -1.08263e-3;
            final double c30 = 2.54e-6;
            final double c40 = 1.62e-6;
            final double c50 = 2.3e-7;
            final double c60 = -5.5e-7;
            Propagator propagator =
                new EcksteinHechlerPropagator(orbit, yawSteeringLaw,
                                              ae, mu, c20, c30, c40, c50, c60);

            // Let's write the results in a file in order to draw some plots.
            final File file = new File(System.getProperty("user.home"), "XYZ.dat");
            final FileWriter fileRes = new FileWriter(file);
            propagator.setMasterMode(10, new OrekitFixedStepHandler() {
                
                public void init(SpacecraftState s0, AbsoluteDate t)
                    throws PropagationException {
                    try {
                        fileRes.write("#time X Y Z Wx Wy Wz\n");
                    } catch (IOException ioe) {
                        throw new PropagationException(ioe,
                                                       LocalizedFormats.SIMPLE_MESSAGE,
                                                       ioe.getLocalizedMessage());
                    }
                }
                
                public void handleStep(SpacecraftState currentState, boolean isLast)
                    throws PropagationException {
                    try {

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

                        fileRes.write(currentState.getDate()
                                      + "  " + d3.format(sunX)
                                      + "  " + d3.format(sunY)
                                      + "  " + d3.format(sunZ)
                                      + "  " + d7.format(spin.getX())
                                      + "  " + d7.format(spin.getY())
                                      + "  " + d7.format(spin.getZ())
                                      + System.getProperty("line.separator"));

                    } catch (OrekitException oe) {
                        throw new PropagationException(oe);
                    } catch (IOException ioe) {
                        throw new PropagationException(ioe,
                                                       LocalizedFormats.SIMPLE_MESSAGE,
                                                       ioe.getLocalizedMessage());
                    }
                }

            });

            System.out.println("Running...");
            propagator.propagate(initialDate.shiftedBy(6000));
            fileRes.close();
            System.out.println("Results written to file: " + file.getAbsolutePath());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

}
