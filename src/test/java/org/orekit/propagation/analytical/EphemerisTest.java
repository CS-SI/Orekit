/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.analytical;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudesSequence;
import org.orekit.attitudes.CelestialBodyPointed;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class EphemerisTest {

    private AbsoluteDate initDate;
    private AbsoluteDate finalDate;
    private Frame        inertialFrame;
    private Propagator   propagator;

    @Test
    public void testAttitudeOverride() throws IllegalArgumentException, OrekitException {
        final double positionTolerance = 1e-6;
        final double velocityTolerance = 1e-5;
        final double attitudeTolerance = 1e-6;

        int numberOfInterals = 1440;
        double deltaT = finalDate.durationFrom(initDate)/((double)numberOfInterals);

        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

        List<SpacecraftState> states = new ArrayList<SpacecraftState>(numberOfInterals + 1);
        for (int j = 0; j<= numberOfInterals; j++) {
            states.add(propagator.propagate(initDate.shiftedBy((j * deltaT))));
        }

        int numInterpolationPoints = 2;
        Ephemeris ephemPropagator = new Ephemeris(states, numInterpolationPoints);
        Assert.assertEquals(0, ephemPropagator.getManagedAdditionalStates().length);

        //First test that we got position, velocity and attitude nailed
        int numberEphemTestIntervals = 2880;
        deltaT = finalDate.durationFrom(initDate)/((double)numberEphemTestIntervals);
        for (int j = 0; j <= numberEphemTestIntervals; j++) {
            AbsoluteDate currentDate = initDate.shiftedBy(j * deltaT);
            SpacecraftState ephemState = ephemPropagator.propagate(currentDate);
            SpacecraftState keplerState = propagator.propagate(currentDate);
            double positionDelta = calculatePositionDelta(ephemState, keplerState);
            double velocityDelta = calculateVelocityDelta(ephemState, keplerState);
            double attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
            Assert.assertEquals("VVLH Unmatched Position at: " + currentDate, 0.0, positionDelta, positionTolerance);
            Assert.assertEquals("VVLH Unmatched Velocity at: " + currentDate, 0.0, velocityDelta, velocityTolerance);
            Assert.assertEquals("VVLH Unmatched Attitude at: " + currentDate, 0.0, attitudeDelta, attitudeTolerance);
        }

        //Now force an override on the attitude and check it against a Keplerian propagator
        //setup identically to the first but with a different attitude
        //If override isn't working this will fail.
        propagator = new KeplerianPropagator(propagator.getInitialState().getOrbit());
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));

        ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));
        for (int j = 0; j <= numberEphemTestIntervals; j++) {
            AbsoluteDate currentDate = initDate.shiftedBy(j * deltaT);
            SpacecraftState ephemState = ephemPropagator.propagate(currentDate);
            SpacecraftState keplerState = propagator.propagate(currentDate);
            double positionDelta = calculatePositionDelta(ephemState, keplerState);
            double velocityDelta = calculateVelocityDelta(ephemState, keplerState);
            double attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
            Assert.assertEquals("QSW Unmatched Position at: " + currentDate, 0.0, positionDelta, positionTolerance);
            Assert.assertEquals("QSW Unmatched Velocity at: " + currentDate, 0.0, velocityDelta, velocityTolerance);
            Assert.assertEquals("QSW Unmatched Attitude at: " + currentDate, 0.0, attitudeDelta, attitudeTolerance);
        }

    }
    
    @Test
    public void testAttitudeSequenceTransition() {
    	        
        // Initialize the orbit
    	final AbsoluteDate initialDate = new AbsoluteDate(2003, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-39098981.4866597, -15784239.3610601, 78908.2289853595);
        final Vector3D velocity  = new Vector3D(1151.00321021175, -2851.14864755189, -2.02133248357321);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getGCRF(), initialDate,
                                                      Constants.WGS84_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);        

        // Define attitude laws
        AttitudeProvider before = new CelestialBodyPointed(FramesFactory.getICRF(), CelestialBodyFactory.getSun(), Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);
        AttitudeProvider after = new CelestialBodyPointed(FramesFactory.getICRF(), CelestialBodyFactory.getEarth(), Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);

        // Define attitude sequence
        AbsoluteDate switchDate = initialDate.shiftedBy(86400.0);
        double transitionTime = 600;
        DateDetector switchDetector = new DateDetector(switchDate).withHandler(new ContinueOnEvent<DateDetector>());

        AttitudesSequence attitudeSequence = new AttitudesSequence();
        attitudeSequence.resetActiveProvider(before);
        attitudeSequence.addSwitchingCondition(before, after, switchDetector, true, false, transitionTime, AngularDerivativesFilter.USE_RR, null);

        NumericalPropagator propagator = new NumericalPropagator(new DormandPrince853Integrator(0.1, 500, 1e-9, 1e-9));
        propagator.setInitialState(initialState);
        
        // Propagate and build ephemeris
        final List<SpacecraftState> propagatedStates = new ArrayList<>();

        propagator.setMasterMode(60, new OrekitFixedStepHandler() {
          @Override
          public void handleStep(SpacecraftState currentState,
                                 boolean isLast)
            throws OrekitException
          {
            propagatedStates.add(currentState);
          }              
        });
        propagator.propagate(initialDate.shiftedBy(2*86400.0));
        final Ephemeris ephemeris = new Ephemeris(propagatedStates, 8);

        // Add attitude switch event to ephemeris
        ephemeris.setAttitudeProvider(attitudeSequence);
        attitudeSequence.registerSwitchEvents(ephemeris);

        // Propagate with a step during the transition
        AbsoluteDate endDate = initialDate.shiftedBy(2*86400.0);
        SpacecraftState stateBefore = ephemeris.getInitialState();
        ephemeris.propagate(switchDate.shiftedBy(transitionTime/2));
        SpacecraftState stateAfter = ephemeris.propagate(endDate);
        
        
        // Check that the attitudes are correct
        Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ0(),
        		stateBefore.getAttitude().getRotation().getQ0(),
        		1.0E-16);
        Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ1(),
        		stateBefore.getAttitude().getRotation().getQ1(),
        		1.0E-16);
        Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ2(),
        		stateBefore.getAttitude().getRotation().getQ2(),
        		1.0E-16);
        Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ3(),
        		stateBefore.getAttitude().getRotation().getQ3(),
        		1.0E-16);

        Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ0(),
        		stateAfter.getAttitude().getRotation().getQ0(),
        		1.0E-16);
        Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ1(),
        		stateAfter.getAttitude().getRotation().getQ1(),
        		1.0E-16);
        Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ2(),
        		stateAfter.getAttitude().getRotation().getQ2(),
        		1.0E-16);
        Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ3(),
        		stateAfter.getAttitude().getRotation().getQ3(),
        		1.0E-16);
    }

    @Test
    public void testNonResettableState() {
        try {
            propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

            List<SpacecraftState> states = new ArrayList<SpacecraftState>();
            for (double dt = 0; dt >= -1200; dt -= 60.0) {
                states.add(propagator.propagate(initDate.shiftedBy(dt)));
            }

            new Ephemeris(states, 2).resetInitialState(propagator.getInitialState());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    public void testAdditionalStates() {
        final String name1  = "dt0";
        final String name2  = "dt1";
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

        List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt >= -1200; dt -= 60.0) {
            final SpacecraftState original = propagator.propagate(initDate.shiftedBy(dt));
            final SpacecraftState expanded = original.addAdditionalState(name2, original.getDate().durationFrom(finalDate));
            states.add(expanded);
        }

        final Propagator ephem = new Ephemeris(states, 2);
        ephem.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() {
                return name1;
            }
            public double[] getAdditionalState(SpacecraftState state) {
                return new double[] { state.getDate().durationFrom(initDate) };
            }
        });

        final String[] additional = ephem.getManagedAdditionalStates();
        Arrays.sort(additional);
        Assert.assertEquals(2, additional.length);
        Assert.assertEquals(name1, ephem.getManagedAdditionalStates()[0]);
        Assert.assertEquals(name2, ephem.getManagedAdditionalStates()[1]);
        Assert.assertTrue(ephem.isAdditionalStateManaged(name1));
        Assert.assertTrue(ephem.isAdditionalStateManaged(name2));
        Assert.assertFalse(ephem.isAdditionalStateManaged("not managed"));

        SpacecraftState s = ephem.propagate(initDate.shiftedBy(-270.0));
        Assert.assertEquals(-270.0,   s.getAdditionalState(name1)[0], 1.0e-15);
        Assert.assertEquals(-86670.0, s.getAdditionalState(name2)[0], 1.0e-15);

    }

    @Test
    public void testProtectedMethods()
        throws SecurityException, NoSuchMethodException,
               InvocationTargetException, IllegalAccessException {
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

        List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt >= -1200; dt -= 60.0) {
            final SpacecraftState original = propagator.propagate(initDate.shiftedBy(dt));
            final SpacecraftState modified = new SpacecraftState(original.getOrbit(),
                                                                 original.getAttitude(),
                                                                 original.getMass() - 0.0625 * dt);
            states.add(modified);
        }

        final Propagator ephem = new Ephemeris(states, 2);
        Method propagateOrbit = Ephemeris.class.getDeclaredMethod("propagateOrbit", AbsoluteDate.class);
        propagateOrbit.setAccessible(true);
        Method getMass        = Ephemeris.class.getDeclaredMethod("getMass", AbsoluteDate.class);
        getMass.setAccessible(true);

        SpacecraftState s = ephem.propagate(initDate.shiftedBy(-270.0));
        Orbit  o = (Orbit) propagateOrbit.invoke(ephem, s.getDate());
        double m = ((Double) getMass.invoke(ephem, s.getDate())).doubleValue();
        Assert.assertEquals(0.0,
                            Vector3D.distance(s.getPVCoordinates().getPosition(),
                                              o.getPVCoordinates().getPosition()),
                            1.0e-15);
        Assert.assertEquals(s.getMass(), m, 1.0e-15);

    }

    @Test
    public void testExtrapolation() {
        double dt = finalDate.durationFrom(initDate);
        double timeStep = dt / 20.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for(double t = 0 ; t <= dt; t+=timeStep) {
            states.add(propagator.propagate(initDate.shiftedBy(t)));
        }

        final int interpolationPoints = 5;
        Ephemeris ephemeris = new Ephemeris(states, interpolationPoints);
        Assert.assertEquals(finalDate, ephemeris.getMaxDate());

        double tolerance = ephemeris.getExtrapolationThreshold();

        ephemeris.propagate(ephemeris.getMinDate());
        ephemeris.propagate(ephemeris.getMaxDate());
        ephemeris.propagate(ephemeris.getMinDate().shiftedBy(-tolerance / 2.0));
        ephemeris.propagate(ephemeris.getMaxDate().shiftedBy(tolerance / 2.0));

        try {
            ephemeris.propagate(ephemeris.getMinDate().shiftedBy(-2.0 * tolerance));
            Assert.fail("an exception should have been thrown");
        } catch (TimeStampedCacheException e) {
            //supposed to fail since out of bounds
        }

        try {
            ephemeris.propagate(ephemeris.getMaxDate().shiftedBy(2.0 * tolerance));
            Assert.fail("an exception should have been thrown");
        } catch (TimeStampedCacheException e) {
            //supposed to fail since out of bounds
        }
    }

    @Before
    public void setUp() throws IllegalArgumentException, OrekitException {
        Utils.setDataRoot("regular-data");

        initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

        finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                 TimeComponents.H00,
                 TimeScalesFactory.getUTC());

        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;
        double mu  = 3.9860047e14;
        inertialFrame = FramesFactory.getEME2000();

        Orbit initialState = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                            inertialFrame, initDate, mu);
        propagator = new KeplerianPropagator(initialState);

    }

    private double calculatePositionDelta(SpacecraftState state1, SpacecraftState state2) {
        return Vector3D.distance(state1.getPVCoordinates().getPosition(), state2.getPVCoordinates().getPosition());
    }

    private double calculateVelocityDelta(SpacecraftState state1, SpacecraftState state2) {
        return Vector3D.distance(state1.getPVCoordinates().getVelocity(), state2.getPVCoordinates().getVelocity());
    }

    private double calculateAttitudeDelta(SpacecraftState state1, SpacecraftState state2) {
        return Rotation.distance(state1.getAttitude().getRotation(), state2.getAttitude().getRotation());
    }

}
