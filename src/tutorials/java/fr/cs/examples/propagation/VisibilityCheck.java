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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for special event detection.
 * <p>This tutorial shows how to easily check for visibility between a satellite and a ground station.<p>
 * @author Pascal Parraud
 */
public class VisibilityCheck {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            //  Initial state definition : date, orbit
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
            double mu =  3.986004415e+14; // gravitation coefficient
            Frame inertialFrame = FramesFactory.getEME2000(); // inertial frame for orbit definition
            Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
            Orbit initialOrbit = new KeplerianOrbit(pvCoordinates, inertialFrame, initialDate, mu);

            // Propagator : consider a simple keplerian motion (could be more elaborate)
            Propagator kepler = new KeplerianPropagator(initialOrbit);

            // Earth and frame
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                   Constants.WGS84_EARTH_FLATTENING,
                                                   earthFrame);

            // Station
            final double longitude = FastMath.toRadians(45.);
            final double latitude  = FastMath.toRadians(25.);
            final double altitude  = 0.;
            final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
            final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");

            // Event definition
            final double maxcheck  = 60.0;
            final double threshold =  0.001;
            final double elevation = FastMath.toRadians(5.0);
            final EventDetector sta1Visi =
                    new ElevationDetector(maxcheck, threshold, sta1Frame).
                    withConstantElevation(elevation).
                    withHandler(new VisibilityHandler());

            // Add event to be detected
            kepler.addEventDetector(sta1Visi);

            // Propagate from the initial date to the first raising or for the fixed duration
            SpacecraftState finalState = kepler.propagate(initialDate.shiftedBy(1500.));

            System.out.println(" Final state : " + finalState.getDate().durationFrom(initialDate));

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

    /** Handler for visibility event. */
    private static class VisibilityHandler implements EventHandler<ElevationDetector> {

        public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                                    final boolean increasing) {
            if (increasing) {
                System.out.println(" Visibility on " + detector.getTopocentricFrame().getName()
                                                     + " begins at " + s.getDate());
                return Action.CONTINUE;
            } else {
                System.out.println(" Visibility on " + detector.getTopocentricFrame().getName()
                                                     + " ends at " + s.getDate());
                return Action.STOP;
            }
        }

    }

}
