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
package org.orekit.propagation.analytical;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;

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

        ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame,LOFType.QSW));
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
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {

        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));
        int numberOfIntervals = 150;
        double deltaT = finalDate.durationFrom(initDate) / numberOfIntervals;

        List<SpacecraftState> states = new ArrayList<SpacecraftState>(numberOfIntervals + 1);
        for (int j = 0; j<= numberOfIntervals; j++) {
            states.add(propagator.propagate(initDate.shiftedBy((j * deltaT))));
        }

        int numInterpolationPoints = 2;
        Ephemeris ephemPropagator = new Ephemeris(states, numInterpolationPoints);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemPropagator);

        Assert.assertTrue(bos.size() > 30000);
        Assert.assertTrue(bos.size() < 31000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        Ephemeris deserialized  = (Ephemeris) ois.readObject();
        Assert.assertEquals(deserialized.getMinDate(), deserialized.getMinDate());
        Assert.assertEquals(deserialized.getMaxDate(), deserialized.getMaxDate());
        for (double dt = 0; dt < finalDate.durationFrom(initDate); dt += 10.0) {
            AbsoluteDate date = initDate.shiftedBy(dt);
            TimeStampedPVCoordinates pvRef = ephemPropagator.getPVCoordinates(date, inertialFrame);
            TimeStampedPVCoordinates pv    = deserialized.getPVCoordinates(date, inertialFrame);
            Assert.assertEquals(0.0, Vector3D.distance(pvRef.getPosition(),     pv.getPosition()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(pvRef.getVelocity(),     pv.getVelocity()),     1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(pvRef.getAcceleration(), pv.getAcceleration()), 1.0e-15);
        }

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
    public void testAdditionalStates() throws OrekitException {
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
        throws OrekitException, SecurityException, NoSuchMethodException,
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
