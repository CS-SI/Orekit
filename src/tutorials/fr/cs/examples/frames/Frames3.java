/* Copyright 2002-2010 CS Communication & Systèmes
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

package fr.cs.examples.frames;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.YawSteering;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.SpacecraftFrame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinatesProvider;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for spacecraft frame support.
 * <p>This tutorial shows the very simple usage of spacecraft frame.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class Frames3 {

    public static void main(String[] args) {
        try {

            // configure Orekit and printing format
            Autoconfiguration.configureOrekit();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat d3 = new DecimalFormat("0.000", symbols);
            DecimalFormat d7 = new DecimalFormat("0.0000000", symbols);

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
                                            Math.toRadians(50.),
                                            Math.toRadians(220.),
                                            Math.toRadians(5.300),
                                            CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                            eme2000,
                                            initialDate,
                                            mu);

            // Attitude laws
            // *************

            // Earth  
            double ae =  6378137.0; // equatorial radius in meter
            double f  =  1.0 / 298.257223563; // flattening
            Frame itrf2005 = FramesFactory.getITRF2005(true); // terrestrial frame at an arbitrary date
            BodyShape earth = new OneAxisEllipsoid(ae, f, itrf2005);

            // Target pointing attitude law over satellite nadir at date, without yaw compensation
            NadirPointing nadirLaw = new NadirPointing(earth);
     
            // Target pointing attitude law with yaw compensation
            PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
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

            // The spacecraft frame is associated with the propagator.
            SpacecraftFrame scFrame = new SpacecraftFrame(propagator, "Spacecraft");

            // Let's write the results in a file in order to draw some plots. 
            final String homeRep = System.getProperty("user.home");
            FileWriter fileRes = new FileWriter(new File(homeRep, "XYZ.dat"));

            fileRes.write("#time X Y Z Wx Wy Wz\n");

            System.out.println("...");

            // Loop
            // from
            AbsoluteDate extrapDate = initialDate;
            // to
            final AbsoluteDate finalDate = extrapDate.shiftedBy(6000);

            while (extrapDate.compareTo(finalDate) <= 0)  {
                // We can simply get the position of the Sun in spacecraft frame at any time
                Vector3D sunSat = sun.getPVCoordinates(extrapDate, scFrame).getPosition();

                // and the spacecraft rotational rate also 
                Vector3D spin = eme2000.getTransformTo(scFrame, extrapDate).getRotationRate();

                // Lets calculate the reduced coordinates
                double sunX = sunSat.getX() / sunSat.getNorm();
                double sunY = sunSat.getY() / sunSat.getNorm();
                double sunZ = sunSat.getZ() / sunSat.getNorm();

                fileRes.write(extrapDate
                              + "  " + d3.format(sunX)
                              + "  " + d3.format(sunY)
                              + "  " + d3.format(sunZ)
                              + "  " + d7.format(spin.getX())
                              + "  " + d7.format(spin.getY())
                              + "  " + d7.format(spin.getZ()) + "\n");

                extrapDate = extrapDate.shiftedBy(10);

            }

            fileRes.close();

            System.out.println("Done");

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

}
