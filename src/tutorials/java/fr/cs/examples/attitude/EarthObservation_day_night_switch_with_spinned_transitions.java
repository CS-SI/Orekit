/* Copyright 2002-2012 CS Systèmes d'Information
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3DFormat;
import org.apache.commons.math3.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudesSequence;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.SpinStabilized;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.DateDetector;
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
 */
public class EarthObservation_day_night_switch_with_spinned_transitions {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();
            final SortedSet<String> output = new TreeSet<String>();

            //----------------------------------------
            //  Initial state definition : date, orbit
            //----------------------------------------
            final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 02, 00, 00, 00.000, TimeScalesFactory.getUTC());
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                          FramesFactory.getEME2000(), initialDate,
                                                          Constants.EIGEN5C_EARTH_MU);

            //------------------------------
            // Attitudes sequence definition
            //------------------------------
            final AttitudesSequence attitudesSequence = new AttitudesSequence();

            // Attitude laws definition
            final double settingRate = FastMath.toRadians(1.0);
            final AttitudeProvider dayObservationLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH,
                                                                     RotationOrder.XYZ, FastMath.toRadians(20), FastMath.toRadians(40), 0);
            final AttitudeProvider nightRestingLaw   = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH);
            final AttitudeProvider transitionLaw     = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH,
                                                                     RotationOrder.XYZ, FastMath.toRadians(20), 0, 0);
            final AttitudeProvider rollSetUpLaw      = new SpinStabilized(nightRestingLaw, AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, settingRate);
            final AttitudeProvider pitchSetUpLaw     = new SpinStabilized(transitionLaw, AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_J, settingRate);
            final AttitudeProvider pitchTearDownLaw  = new SpinStabilized(dayObservationLaw, AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_J, -settingRate);
            final AttitudeProvider rollTearDownLaw   = new SpinStabilized(transitionLaw, AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, -settingRate);

            // Event detectors definition
            //---------------------------
            final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();

            // Detectors : end day-night rdv 2
            final DateDetector endDayNightRdV2Event_increase = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (increasing) {
                		output.add(s.getDate() + ": switching to night law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-day-night-2 night-mode");
                	}
                    return Action.CONTINUE;
                }
            };

            final DateDetector endDayNightRdV2Event_decrease = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (!increasing) {
                		output.add(s.getDate() + ": switching to night law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-day-night-2 night-mode");
                	}
                    return Action.CONTINUE;
                }
            };

            // Detectors : end day-night rdv 1
            final DateDetector endDayNightRdV1Event_increase = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (increasing) {
                		output.add(s.getDate() + ": switching to day-night rdv 2 law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-day-night-1 day-night-rdv2-mode");
                		endDayNightRdV2Event_increase.addEventDate(s.getDate().shiftedBy(20));
                		endDayNightRdV2Event_decrease.addEventDate(s.getDate().shiftedBy(20));
                	}
                    return Action.CONTINUE;
                }
            };

            final DateDetector endDayNightRdV1Event_decrease = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (!increasing) {
                		output.add(s.getDate() + ": switching to day-night rdv 2 law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-day-night-1 day-night-rdv2-mode");
                		endDayNightRdV2Event_increase.addEventDate(s.getDate().shiftedBy(20));
                		endDayNightRdV2Event_decrease.addEventDate(s.getDate().shiftedBy(20));
                	}
                    return Action.CONTINUE;
                }
            };

            // Detector : eclipse entry
            final EventDetector dayNightEvent = new EclipseDetector(sun, 696000000., earth, Constants.WGS84_EARTH_EQUATORIAL_RADIUS) {
                private static final long serialVersionUID = 8091992101063392941L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                    if (!increasing) {
                        output.add(s.getDate() + ": switching to day-night rdv 1 law");
                        System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " eclipse-entry day-night-rdv1-mode");
                        endDayNightRdV1Event_increase.addEventDate(s.getDate().shiftedBy(40));
                        endDayNightRdV1Event_decrease.addEventDate(s.getDate().shiftedBy(40));
                    }
                    return Action.CONTINUE;
                }
            };

            // Detectors : end night-day rdv 2
            final DateDetector endNightDayRdV2Event_increase = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (increasing) {
                		output.add(s.getDate() + ": switching to day law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-night-day-2 day-mode");
                	}
                    return Action.CONTINUE;
                }
            };

            final DateDetector endNightDayRdV2Event_decrease = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (!increasing) {
                		output.add(s.getDate() + ": switching to day law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-night-day-2 day-mode");
                	}
                    return Action.CONTINUE;
                }
            };

            // Detectors : end night-day rdv 1
            final DateDetector endNightDayRdV1Event_increase = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (increasing) {
                		output.add(s.getDate() + ": switching to night-day rdv 2 law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-night-day-1 night-day-rdv2-mode");
                		endNightDayRdV2Event_increase.addEventDate(s.getDate().shiftedBy(40));
                		endNightDayRdV2Event_decrease.addEventDate(s.getDate().shiftedBy(40));
                	}
                    return Action.CONTINUE;
                }
            };

            final DateDetector endNightDayRdV1Event_decrease = new DateDetector(10, 1e-04) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                	if (!increasing) {
                		output.add(s.getDate() + ": switching to night-day rdv 2 law");
                		System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " end-night-day-1 night-day-rdv2-mode");
                		endNightDayRdV2Event_increase.addEventDate(s.getDate().shiftedBy(40));
                		endNightDayRdV2Event_decrease.addEventDate(s.getDate().shiftedBy(40));
                	}
                    return Action.CONTINUE;
                }
            };

            // Detector : eclipse exit
            final EventDetector nightDayEvent = new EclipseDetector(sun, 696000000., earth, Constants.WGS84_EARTH_EQUATORIAL_RADIUS) {
                private static final long serialVersionUID = -377454330129772997L;
                public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
                    if (increasing) {
                        output.add(s.getDate() + ": switching to night-day rdv 1 law");
                        System.out.println("# " + (s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " eclipse-exit night-day-rdv1-mode");
                        endNightDayRdV1Event_increase.addEventDate(s.getDate().shiftedBy(20));
                        endNightDayRdV1Event_decrease.addEventDate(s.getDate().shiftedBy(20));
                    }
                    return Action.CONTINUE;
                }
            };

            // Attitude sequences definition
            //------------------------------
            attitudesSequence.addSwitchingCondition(dayObservationLaw, dayNightEvent, false, true, pitchTearDownLaw);
            attitudesSequence.addSwitchingCondition(pitchTearDownLaw, endDayNightRdV1Event_increase, true, false, rollTearDownLaw);
            attitudesSequence.addSwitchingCondition(pitchTearDownLaw, endDayNightRdV1Event_decrease, false, true, rollTearDownLaw);
            attitudesSequence.addSwitchingCondition(rollTearDownLaw, endDayNightRdV2Event_increase, true, false, nightRestingLaw);
            attitudesSequence.addSwitchingCondition(rollTearDownLaw, endDayNightRdV2Event_decrease, false, true, nightRestingLaw);
            attitudesSequence.addSwitchingCondition(nightRestingLaw, nightDayEvent, true, false, rollSetUpLaw);
            attitudesSequence.addSwitchingCondition(rollSetUpLaw, endNightDayRdV1Event_increase, true, false, pitchSetUpLaw);
            attitudesSequence.addSwitchingCondition(rollSetUpLaw, endNightDayRdV1Event_decrease, false, true, pitchSetUpLaw);
            attitudesSequence.addSwitchingCondition(pitchSetUpLaw, endNightDayRdV2Event_increase, true, false, dayObservationLaw);
            attitudesSequence.addSwitchingCondition(pitchSetUpLaw, endNightDayRdV2Event_decrease, false, true, dayObservationLaw);

            // Initialisation
            //---------------
            if (dayNightEvent.g(new SpacecraftState(initialOrbit)) >= 0) {
                // initial position is in daytime
                attitudesSequence.resetActiveProvider(dayObservationLaw);
                System.out.println("# " + (initialDate.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " begin with day law");
            } else {
                // initial position is in nighttime
                attitudesSequence.resetActiveProvider(nightRestingLaw);
                System.out.println("# " + (initialDate.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY) + " begin with night law");
            }

            //----------------------
            // Propagator definition
            //----------------------

            // Propagator : consider the analytical Eckstein-Hechler model
            final Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, attitudesSequence,
                                                                        Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                        Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20,
                                                                        Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                        Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
            // Register the switching events to the propagator
            attitudesSequence.registerSwitchEvents(propagator);

            propagator.setMasterMode(10.0, new OrekitFixedStepHandler() {
                private static final long serialVersionUID = -5740543464313002093L;
                private DecimalFormat f1 = new DecimalFormat("0.0000000000000000E00",
                                                             new DecimalFormatSymbols(Locale.US));
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
                           " " + f1.format(r.getQ1()) +
                           " " + f1.format(r.getQ2()) +
                           " " + f1.format(r.getQ3()) +
                           " " + f1.format(r.getQ0());
                }
                private String printRotation2(final String name, final Rotation r) {
                    return name +
                           " " + f1.format(-r.getQ1()) +
                           " " + f1.format(-r.getQ2()) +
                           " " + f1.format(-r.getQ3()) +
                           " " + f1.format(-r.getQ0());
                }
                public void init(final SpacecraftState s0, final AbsoluteDate t) {
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

                        final double endNightDayTimer1 = endNightDayRdV1Event_decrease.g(currentState);
                        final double endNightDayTimer2 = endNightDayRdV2Event_decrease.g(currentState);
                        final double endDayNightTimer1 = endDayNightRdV1Event_decrease.g(currentState);
                        final double endDayNightTimer2 = endDayNightRdV2Event_decrease.g(currentState);

                        output.add(currentState.getDate() +
                                   " " + FastMath.toDegrees(eclipseAngle) +
                                   " " + endNightDayTimer1 +
                                   " " + endNightDayTimer2 +
                                   " " + endDayNightTimer1 +
                                   " " + endDayNightTimer2 +
                                   " " + FastMath.toDegrees(pointingOffset));
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
                                           printRotation2("", currentState.getAttitude().getRotation()) +
                                           " " + printVector3D("", currentState.getAttitude().getSpin()));
                    } catch (OrekitException oe) {
                        throw new PropagationException(oe);
                    }
                }
            });

            //----------
            // Propagate
            //----------

            // Propagate from the initial date for the fixed duration
            propagator.propagate(initialDate.shiftedBy(1.75*3600.));

            //--------------
            // Print results
            //--------------

            // we print the lines according to lexicographic order, which is chronological order here
            // to make sure out of orders calls between step handler and event handlers don't mess things up
            for (final String line : output) {
                System.out.println(line);
            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
