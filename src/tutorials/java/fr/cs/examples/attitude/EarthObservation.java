/* Copyright 2002-2019 CS Systèmes d'Information
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

package fr.cs.examples.attitude;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudesSequence;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Orekit tutorial for Earth observation attitude sequence.
 * <p>This tutorial shows how to easily switch between day and night attitude modes.<p>
 * @author Luc Maisonobe
 */
public class EarthObservation {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            File home       = new File(System.getProperty("user.home"));
            File orekitData = new File(home, "orekit-data");
            if (!orekitData.exists()) {
                System.err.format(Locale.US, "Failed to find %s folder%n",
                                  orekitData.getAbsolutePath());
                System.err.format(Locale.US, "You need to download %s from %s, unzip it in %s and rename it 'orekit-data' for this tutorial to work%n",
                                  "orekit-data-master.zip", "https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip",
                                  home.getAbsolutePath());
                System.exit(1);
            }
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            final SortedSet<String> output = new TreeSet<String>();

            //  Initial state definition : date, orbit
            final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                          FramesFactory.getEME2000(), initialDate,
                                                          Constants.EIGEN5C_EARTH_MU);

            // Attitudes sequence definition
            final AttitudeProvider dayObservationLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH,
                                                                     RotationOrder.XYZ, FastMath.toRadians(20), FastMath.toRadians(40), 0);
            final AttitudeProvider nightRestingLaw   = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH);
            final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            final EventDetector dayNightEvent =
                new EclipseDetector(sun, 696000000.,
                                    new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                         0.0,
                                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                withHandler(new ContinueOnEvent<EclipseDetector>());
            final EventDetector nightDayEvent =
                new EclipseDetector(sun, 696000000.,
                                    new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                         0.0,
                                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                withHandler(new ContinueOnEvent<EclipseDetector>());

            final AttitudesSequence attitudesSequence = new AttitudesSequence();
            final AttitudesSequence.SwitchHandler switchHandler =
                            new AttitudesSequence.SwitchHandler() {
                                public void switchOccurred(AttitudeProvider preceding, AttitudeProvider following,
                                                           SpacecraftState s) {
                                    if (preceding == dayObservationLaw) {
                                        output.add(s.getDate() + ": switching to night law");
                                    } else {
                                        output.add(s.getDate() + ": switching to day law");
                                    }
                                }
                            };
            attitudesSequence.addSwitchingCondition(dayObservationLaw, nightRestingLaw, dayNightEvent,
                                                    false, true, 10.0,
                                                    AngularDerivativesFilter.USE_R, switchHandler);
            attitudesSequence.addSwitchingCondition(nightRestingLaw, dayObservationLaw, nightDayEvent,
                                                    true, false, 10.0,
                                                    AngularDerivativesFilter.USE_R, switchHandler);
            if (dayNightEvent.g(new SpacecraftState(initialOrbit)) >= 0) {
                // initial position is in daytime
                attitudesSequence.resetActiveProvider(dayObservationLaw);
            } else {
                // initial position is in nighttime
                attitudesSequence.resetActiveProvider(nightRestingLaw);
            }

            // Propagator : consider the analytical Eckstein-Hechler model
            final Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, attitudesSequence,
                                                                        Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                        Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20,
                                                                        Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                        Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);

            // Register the switching events to the propagator
            attitudesSequence.registerSwitchEvents(propagator);

            propagator.setMasterMode(180.0, new OrekitFixedStepHandler() {
                public void handleStep(SpacecraftState currentState, boolean isLast) {
                    DecimalFormatSymbols angleDegree = new DecimalFormatSymbols(Locale.US);
                    angleDegree.setDecimalSeparator('\u00b0');
                    DecimalFormat ad = new DecimalFormat(" 00.000;-00.000", angleDegree);
                    // the Earth position in spacecraft frame should be along spacecraft Z axis
                    // during nigthtime and away from it during daytime due to roll and pitch offsets
                    final Vector3D earth = currentState.toTransform().transformPosition(Vector3D.ZERO);
                    final double pointingOffset = Vector3D.angle(earth, Vector3D.PLUS_K);

                    // the g function is the eclipse indicator, its an angle between Sun and Earth limb,
                    // positive when Sun is outside of Earth limb, negative when Sun is hidden by Earth limb
                    final double eclipseAngle = dayNightEvent.g(currentState);

                    output.add(currentState.getDate() +
                               " " + ad.format(FastMath.toDegrees(eclipseAngle)) +
                               " " + ad.format(FastMath.toDegrees(pointingOffset)));
                }
            });

            // Propagate from the initial date for the fixed duration
            SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(12600.));

            // we print the lines according to lexicographic order, which is chronological order here
            // to make sure out of orders calls between step handler and event handlers don't mess things up
            for (final String line : output) {
                System.out.println(line);
            }

            System.out.println("Propagation ended at " + finalState.getDate());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
