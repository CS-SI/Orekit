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
package org.orekit.attitudes;


import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class AttitudesSequenceTest {

    private AbsoluteDate lastChange;
    private boolean inEclipse;

    @Test
    public void testDayNightSwitch() throws OrekitException {
        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        // Attitudes sequence definition
        EventsLogger logger = new EventsLogger();
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final AttitudeProvider dayObservationLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH,
                                                                 RotationOrder.XYZ, FastMath.toRadians(20), FastMath.toRadians(40), 0);
        final AttitudeProvider nightRestingLaw   = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
        final EclipseDetector ed = new EclipseDetector(sun, 696000000., earth, Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        final EventDetector dayNightEvent = logger.monitorDetector(ed.withHandler(new ContinueOnEvent<EclipseDetector>() {
            public EventDetector.Action eventOccurred(final SpacecraftState s, final EclipseDetector d, final boolean increasing) {
                setInEclipse(s.getDate(), !increasing);
                return EventDetector.Action.CONTINUE;
            }
        }));
        final EventDetector nightDayEvent = logger.monitorDetector(ed.withHandler(new ContinueOnEvent<EclipseDetector>()));
        attitudesSequence.addSwitchingCondition(dayObservationLaw, dayNightEvent, false, true, nightRestingLaw);
        attitudesSequence.addSwitchingCondition(nightRestingLaw, nightDayEvent, true, false, dayObservationLaw);
        if (dayNightEvent.g(new SpacecraftState(initialOrbit)) >= 0) {
            // initial position is in daytime
            setInEclipse(initialDate, false);
            attitudesSequence.resetActiveProvider(dayObservationLaw);
        } else {
            // initial position is in nighttime
            setInEclipse(initialDate, true);
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
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }
            public void handleStep(SpacecraftState currentState, boolean isLast) throws PropagationException {
                try {
                    // the Earth position in spacecraft frame should be along spacecraft Z axis
                    // during night time and away from it during day time due to roll and pitch offsets
                    final Vector3D earth = currentState.toTransform().transformPosition(Vector3D.ZERO);
                    final double pointingOffset = Vector3D.angle(earth, Vector3D.PLUS_K);

                    // the g function is the eclipse indicator, its an angle between Sun and Earth limb,
                    // positive when Sun is outside of Earth limb, negative when Sun is hidden by Earth limb
                    final double eclipseAngle = dayNightEvent.g(currentState);

                    if (currentState.getDate().compareTo(lastChange) > 0) {
                        if (inEclipse) {
                            Assert.assertTrue(eclipseAngle <= 0);
                            Assert.assertEquals(0.0, pointingOffset, 1.0e-6);
                        } else {
                            Assert.assertTrue(eclipseAngle >= 0);
                            Assert.assertEquals(0.767215, pointingOffset, 1.0e-6);
                        }
                    }
                } catch (OrekitException oe) {
                    throw new PropagationException(oe);
                }
            }
        });

        // Propagate from the initial date for the fixed duration
        propagator.propagate(initialDate.shiftedBy(12600.));

        Assert.assertEquals(8, logger.getLoggedEvents().size());

    }

    private void setInEclipse(AbsoluteDate lastChange, boolean inEclipse) {
        this.lastChange = lastChange;
        this.inEclipse = inEclipse;
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

