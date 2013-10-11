/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for basic frames support.
 * <p>This tutorial shows a simple usage of frames and transforms.</p>
 * @author Pascal Parraud
 */
public class Frames1 {

    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat d3 = new DecimalFormat("0.000", symbols);

            //  Initial state definition : date, orbit
            TimeScale utc = TimeScalesFactory.getUTC();
            AbsoluteDate initialDate = new AbsoluteDate(2008, 10, 01, 0, 0, 00.000, utc);
            double mu =  3.986004415e+14; // gravitation coefficient
            Frame inertialFrame = FramesFactory.getEME2000(); // inertial frame for orbit definition
            Vector3D posisat = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            Vector3D velosat = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            PVCoordinates pvsat = new PVCoordinates(posisat, velosat);
            Orbit initialOrbit = new CartesianOrbit(pvsat, inertialFrame, initialDate, mu);

            // Propagator : consider a simple keplerian motion
            Propagator kepler = new KeplerianPropagator(initialOrbit);

            // The local orbital frame (LOF) is related to the orbit propagated by the kepler propagator.
            LocalOrbitalFrame lof = new LocalOrbitalFrame(inertialFrame, LOFType.QSW, kepler, "QSW");

            // Earth and frame
            double ae =  6378137.0; // equatorial radius in meter
            double f  =  1.0 / 298.257223563; // flattening
            Frame ITRF2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
            BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);

            // Station
            final double longitude = FastMath.toRadians(45.);
            final double latitude  = FastMath.toRadians(25.);
            final double altitude  = 0.;
            final GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
            final TopocentricFrame staF = new TopocentricFrame(earth, station, "station1");

            System.out.println("          time           doppler (m/s)");

            // Stop date
            final AbsoluteDate finalDate = new AbsoluteDate(initialDate, 6000, utc);

            // Loop
            AbsoluteDate extrapDate = initialDate;
            while (extrapDate.compareTo(finalDate) <= 0)  {

                // We can simply get the position and velocity of station in LOF frame at any time
                PVCoordinates pv = staF.getTransformTo(lof, extrapDate).transformPVCoordinates(PVCoordinates.ZERO);

                // And then calculate the doppler signal
                double doppler = Vector3D.dotProduct(pv.getPosition(), pv.getVelocity()) / pv.getPosition().getNorm();

                System.out.println(extrapDate + "  " + d3.format(doppler));

                extrapDate = new AbsoluteDate(extrapDate, 600, utc);

            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
