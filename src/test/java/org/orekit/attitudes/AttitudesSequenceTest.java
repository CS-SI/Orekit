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
package org.orekit.attitudes;


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
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

        final

        // Attitudes sequence definition
        EventsLogger logger = new EventsLogger();
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final AttitudeProvider dayObservationLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH,
                                                                 RotationOrder.XYZ, FastMath.toRadians(20), FastMath.toRadians(40), 0);
        final AttitudeProvider nightRestingLaw   = new LofOffset(initialOrbit.getFrame(), LOFType.VVLH);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
        final EclipseDetector ed =
                new EclipseDetector(sun, 696000000., earth, Constants.WGS84_EARTH_EQUATORIAL_RADIUS).
                withHandler(new ContinueOnEvent<EclipseDetector>() {
                    private static final long serialVersionUID = 1L;
                    public EventHandler.Action eventOccurred(final SpacecraftState s, final EclipseDetector d, final boolean increasing) {
                        setInEclipse(s.getDate(), !increasing);
                        return EventHandler.Action.RESET_STATE;
                    }
                });
        final EventDetector monitored = logger.monitorDetector(ed);
        final Handler dayToNightHandler = new Handler(dayObservationLaw, nightRestingLaw);
        final Handler nightToDayHandler = new Handler(nightRestingLaw, dayObservationLaw);
        attitudesSequence.addSwitchingCondition(dayObservationLaw, nightRestingLaw,
                                                monitored, false, true, 300.0,
                                                AngularDerivativesFilter.USE_RRA, dayToNightHandler);
        attitudesSequence.addSwitchingCondition(nightRestingLaw, dayObservationLaw,
                                                monitored, true, false, 300.0,
                                                AngularDerivativesFilter.USE_RRA, nightToDayHandler);
        SpacecraftState initialState = new SpacecraftState(initialOrbit);
        initialState = initialState.addAdditionalState("fortyTwo", 42.0);
        if (ed.g(initialState) >= 0) {
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
                                                                    Constants.EIGEN5C_EARTH_MU,  Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);

        // Register the switching events to the propagator
        attitudesSequence.registerSwitchEvents(propagator);

        propagator.setMasterMode(60.0, new OrekitFixedStepHandler() {
            public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
                // the Earth position in spacecraft frame should be along spacecraft Z axis
                // during night time and away from it during day time due to roll and pitch offsets
                final Vector3D earth = currentState.toTransform().transformPosition(Vector3D.ZERO);
                final double pointingOffset = Vector3D.angle(earth, Vector3D.PLUS_K);

                // the g function is the eclipse indicator, its an angle between Sun and Earth limb,
                // positive when Sun is outside of Earth limb, negative when Sun is hidden by Earth limb
                final double eclipseAngle = ed.g(currentState);

                if (currentState.getDate().durationFrom(lastChange) > 300) {
                    if (inEclipse) {
                        Assert.assertTrue(eclipseAngle <= 0);
                        Assert.assertEquals(0.0, pointingOffset, 1.0e-6);
                    } else {
                        Assert.assertTrue(eclipseAngle >= 0);
                        Assert.assertEquals(0.767215, pointingOffset, 1.0e-6);
                    }
                } else {
                    // we are in transition
                    Assert.assertTrue(pointingOffset + " " + (0.767215 - pointingOffset),
                                      pointingOffset <= 0.7672155);
                }
            }
        });

        // Propagate from the initial date for the fixed duration
        propagator.propagate(initialDate.shiftedBy(12600.));

        // as we have 2 switch events (even if they share the same underlying event detector),
        // and these events are triggered at both eclipse entry and exit, we get 8
        // raw events on 2 orbits
        Assert.assertEquals(8, logger.getLoggedEvents().size());

        // we have 4 attitudes switch on 2 orbits, 2 of each type
        Assert.assertEquals(2, dayToNightHandler.dates.size());
        Assert.assertEquals(2, nightToDayHandler.dates.size());

    }

    @Test
    public void testBackwardPropagation() throws OrekitException {

        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final AttitudeProvider past    = new InertialProvider(Rotation.IDENTITY);
        final AttitudeProvider current = new InertialProvider(Rotation.IDENTITY);
        final AttitudeProvider future  = new InertialProvider(Rotation.IDENTITY);
        final Handler handler = new Handler(current, past);
        attitudesSequence.addSwitchingCondition(past, current,
                                                new DateDetector(initialDate.shiftedBy(-500.0)),
                                                true, false, 10.0, AngularDerivativesFilter.USE_R, handler);
        attitudesSequence.addSwitchingCondition(current, future,
                                                new DateDetector(initialDate.shiftedBy(+500.0)),
                                                true, false, 10.0, AngularDerivativesFilter.USE_R, null);
        attitudesSequence.resetActiveProvider(current);

        SpacecraftState initialState = new SpacecraftState(initialOrbit);
        initialState = initialState.addAdditionalState("fortyTwo", 42.0);
        final Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, attitudesSequence,
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,  Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
        propagator.resetInitialState(initialState);
        Assert.assertEquals(42.0, propagator.getInitialState().getAdditionalState("fortyTwo")[0], 1.0e-10);

        // Register the switching events to the propagator
        attitudesSequence.registerSwitchEvents(propagator);

        SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(-10000.0));
        Assert.assertEquals(42.0, finalState.getAdditionalState("fortyTwo")[0], 1.0e-10);
        Assert.assertEquals(1, handler.dates.size());
        Assert.assertEquals(-500.0, handler.dates.get(0).durationFrom(initialDate), 1.0e-3);
        Assert.assertEquals(-490.0, finalState.getDate().durationFrom(initialDate), 1.0e-3);

    }

    @Test
    public void testTooShortTransition() {
        double threshold      = 1.5;
        double transitionTime = 0.5;
        try {
            new AttitudesSequence().addSwitchingCondition(new InertialProvider(Rotation.IDENTITY),
                                                          new InertialProvider(Rotation.IDENTITY),
                                                          new DateDetector(1000.0, threshold,
                                                                           AbsoluteDate.J2000_EPOCH),
                                                          true, false, transitionTime,
                                                          AngularDerivativesFilter.USE_R, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH,
                                oe.getSpecifier());
            Assert.assertEquals(transitionTime, ((Double) oe.getParts()[0]).doubleValue(), 1.0e-10);
            Assert.assertEquals(threshold,      ((Double) oe.getParts()[1]).doubleValue(), 1.0e-10);
        }
    }

    private static class Handler implements AttitudesSequence.SwitchHandler {

        private AttitudeProvider   expectedPrevious;
        private AttitudeProvider   expectedNext;
        private List<AbsoluteDate> dates;

        public Handler(final AttitudeProvider expectedPrevious, final AttitudeProvider expectedNext) {
            this.expectedPrevious = expectedPrevious;
            this.expectedNext     = expectedNext;
            this.dates            = new ArrayList<AbsoluteDate>();
        }

        @Override
        public void switchOccurred(AttitudeProvider previous, AttitudeProvider next,
                                   SpacecraftState state) {
            Assert.assertTrue(previous == expectedPrevious);
            Assert.assertTrue(next     == expectedNext);
            dates.add(state.getDate());
        }

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

