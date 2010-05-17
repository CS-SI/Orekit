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

package fr.cs.examples.attitude;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.geometry.Vector3DFormat;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.AttitudesSequence;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for Earth observation attitude sequence.
 * <p>This tutorial shows how to easily switch between day and night attitude modes.<p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class EarthObservation {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();
            final SortedSet<String> output = new TreeSet<String>();

            //  Initial state definition : date, orbit
            final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
            System.out.println(initialDate.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY);
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                          FramesFactory.getEME2000(), initialDate,
                                                          Constants.EIGEN5C_EARTH_MU);

            // Attitudes sequence definition
            final AttitudesSequence attitudesSequence = new AttitudesSequence();
            final AttitudeLaw dayObservationLaw = new LofOffset(RotationOrder.XYZ, Math.toRadians(20), Math.toRadians(40), 0);
            final AttitudeLaw nightRestingLaw   = LofOffset.LOF_ALIGNED;
            final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
            final EventDetector dayNightEvent = new EclipseDetector(sun, 696000000., earth, Constants.WGS84_EARTH_EQUATORIAL_RADIUS) {
                private static final long serialVersionUID = 8091992101063392941L;
                public int eventOccurred(final SpacecraftState s, final boolean increasing) {
                    if (!increasing) {
                        output.add(s.getDate() + ": switching to night law");
                    }
                    return CONTINUE;
                }
            };
            final EventDetector nightDayEvent = new EclipseDetector(sun, 696000000., earth, Constants.WGS84_EARTH_EQUATORIAL_RADIUS) {
                private static final long serialVersionUID = -377454330129772997L;
                public int eventOccurred(final SpacecraftState s, final boolean increasing) {
                    if (increasing) {
                        output.add(s.getDate() + ": switching to day law");
                    }
                    return CONTINUE;
                }
            };
            attitudesSequence.addSwitchingCondition(dayObservationLaw, dayNightEvent, false, nightRestingLaw);
            attitudesSequence.addSwitchingCondition(nightRestingLaw, nightDayEvent, true, dayObservationLaw);
            if (dayNightEvent.g(new SpacecraftState(initialOrbit)) >= 0) {
                // initial position is in daytime
                attitudesSequence.resetActiveLaw(dayObservationLaw);
            } else {
                // initial position is in nighttime
                attitudesSequence.resetActiveLaw(nightRestingLaw);
            }

            // Propagator : consider a simple keplerian motion (could be more elaborate)
            final Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, attitudesSequence,
                                                                        Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                        Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20,
                                                                        Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                        Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
            attitudesSequence.registerSwitchEvents(propagator);

            propagator.setMasterMode(180.0, new OrekitFixedStepHandler() {
                private static final long serialVersionUID = -5740543464313002093L;
                private DecimalFormat f1 = new DecimalFormat("0.0000000000000000E00",
                                                             DecimalFormatSymbols.getInstance(Locale.US));
                private Vector3DFormat f2 = new Vector3DFormat(" ", " ", " ", f1);
                private PVCoordinatesProvider sun  = CelestialBodyFactory.getSun();
                private PVCoordinatesProvider moon = CelestialBodyFactory.getMoon();
                private Frame eme2000 = FramesFactory.getEME2000();
                private Frame itrf2005 = FramesFactory.getITRF2005();
                private String printVector3D(final String name, final Vector3D v) {
                    return name + " " + f2.format(v);
                }
                private String printRotation(final String name, final Rotation r) {
                    return name +
                           " " + f1.format(r.getQ3()) +
                           " " + f1.format(r.getQ0()) +
                           " " + f1.format(r.getQ1()) +
                           " " + f1.format(r.getQ2());
                }
                public void handleStep(SpacecraftState currentState, boolean isLast) throws PropagationException {
                    try {
                        // the Earth position in spacecraft should be along spacecraft Z axis
                        // during nigthtime and away from it during daytime due to roll and pitch offsets
                        final Vector3D earth = currentState.toTransform().transformPosition(Vector3D.ZERO);
                        final double pointingOffset = Vector3D.angle(earth, Vector3D.PLUS_K);

                        // the g function is the eclipse indicator, its an angle between Sun and Earth limb,
                        // positive when Sun is outside of Earth limb, negative when Sun is hidden by Earth limb
                        final double eclipseAngle = dayNightEvent.g(currentState);

                        output.add(currentState.getDate() +
                                   " " + Math.toDegrees(eclipseAngle) +
                                   " " + Math.toDegrees(pointingOffset));
                        final AbsoluteDate date = currentState.getDate();
                        final PVCoordinates pv = currentState.getPVCoordinates(eme2000);
                        final Rotation lvlhRot = new Rotation(pv.getPosition(), pv.getMomentum(), Vector3D.MINUS_K, Vector3D.MINUS_J);
                        final Rotation earthRot = eme2000.getTransformTo(itrf2005, date).getRotation();
                        System.out.println("Scenario::setVectorMap 0x960b7e0 " +
                                   (date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) +
                                   " " + printVector3D("sun", sun.getPVCoordinates(date, eme2000).getPosition()) +
                                   " " + printVector3D("moon", moon.getPVCoordinates(date, eme2000).getPosition()) +
                                   " " + printVector3D("satPos", pv.getPosition()) +
                                   " " + printVector3D("satVel", pv.getVelocity()) +
                                   " " + printVector3D("orbMom", pv.getMomentum()));
                        System.out.println("Scenario::setQuatMap 0x960b7e0 " +
                                           (date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) +
                                   " " + printRotation("earthFrame", earthRot) +
                                   " " + printRotation("LVLHFrame", lvlhRot));
                        System.out.println("Scenario::computeStep 0x960b7e0 " +
                                           (date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY));
                        System.out.println("  -> " +
                                           printRotation("", currentState.getAttitude().getRotation()) +
                                           " " + printVector3D("", currentState.getAttitude().getSpin()));
                    } catch (OrekitException oe) {
                        throw new PropagationException(oe.getLocalizedMessage(), oe);
                    }
                }
            });

            // Propagate from the initial date to the first raising or for the fixed duration
            propagator.propagate(initialDate.shiftedBy(20000.));

            // we print the lines according to lexicographic order, which is chronological order here
            // to make sure out of orders calls between step handler and event handlers don't mess things up
//            for (final String line : output) {
//                System.out.println(line);
//            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
