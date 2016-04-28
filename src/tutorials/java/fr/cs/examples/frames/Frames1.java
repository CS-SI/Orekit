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

import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
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
import org.orekit.utils.Constants;
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
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                   Constants.WGS84_EARTH_FLATTENING,
                                                   earthFrame);

            // Station
            final double longitude = FastMath.toRadians(45.);
            final double latitude  = FastMath.toRadians(25.);
            final double altitude  = 0.;
            final GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
            final TopocentricFrame staF = new TopocentricFrame(earth, station, "station");

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

                System.out.format(Locale.US, "%s   %9.3f%n", extrapDate, doppler);

                extrapDate = new AbsoluteDate(extrapDate, 600, utc);

            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
