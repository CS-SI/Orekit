/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.propagation.semianalytical.dsst;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.HarrisPriester;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class DSSTPropagatorTest {

    private double          mu;
    private AbsoluteDate    initDate;
    private SpacecraftState initialState;
    private DSSTPropagator  propagator;
    private boolean         gotHere;

    @Test
    public void testNoExtrapolation() throws OrekitException {

        // Propagate of the initial at the initial date
        final SpacecraftState finalState = propagator.propagate(initDate);

        // Initial orbit definition
        final Vector3D initialPosition = initialState.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = initialState.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition   = finalState.getPVCoordinates().getPosition();
        final Vector3D finalVelocity   = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assert.assertEquals(initialPosition.getX(), finalPosition.getX(), 0.0);
        Assert.assertEquals(initialPosition.getY(), finalPosition.getY(), 0.0);
        Assert.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 0.0);
        Assert.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 0.0);
        Assert.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 0.0);
        Assert.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 0.0);

    }

    @Test
    public void testKepler() throws OrekitException {

        // Propagation of the initial state at t + dt
        final double dt = 3200;
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),             finalState.getA(),             1.0e-15);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-15);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-15);
        Assert.assertEquals(initialState.getHx(),            finalState.getHx(),            1.0e-15);
        Assert.assertEquals(initialState.getHy(),            finalState.getHy(),            1.0e-15);
        Assert.assertEquals(initialState.getLM() + n * dt,   finalState.getLM(),            1.0e-15);

    }

    @Test
    public void testAccumulator() throws OrekitException {

        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        // Propagation of the initial state at t + 2*dt then back to t + dt, back to t - 2*dt and forth to t - dt
        final double dt = 3200;
        propagator.propagate(initDate.shiftedBy(2*dt));
        final SpacecraftState finalState1 = propagator.propagate(initDate.shiftedBy(dt));

        // Check results
        Assert.assertEquals(initialState.getA(),             finalState1.getA(),             1.0e-15);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState1.getEquinoctialEx(), 1.0e-15);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState1.getEquinoctialEy(), 1.0e-15);
        Assert.assertEquals(initialState.getHx(),            finalState1.getHx(),            1.0e-15);
        Assert.assertEquals(initialState.getHy(),            finalState1.getHy(),            1.0e-15);
        Assert.assertEquals(initialState.getLM() + n * dt,   finalState1.getLM(),            1.0e-15);

        // Continue propagation of the initial state back to t - 2*dt and forth to t - dt
        propagator.propagate(initDate.shiftedBy(-2*dt));
        final SpacecraftState finalState2 = propagator.propagate(initDate.shiftedBy(-dt));
        // Check results
        Assert.assertEquals(initialState.getA(),             finalState2.getA(),             1.0e-15);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState2.getEquinoctialEx(), 1.0e-15);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState2.getEquinoctialEy(), 1.0e-15);
        Assert.assertEquals(initialState.getHx(),            finalState2.getHx(),            1.0e-15);
        Assert.assertEquals(initialState.getHy(),            finalState2.getHy(),            1.0e-15);
        Assert.assertEquals(initialState.getLM() - n * dt,   finalState2.getLM(),            1.0e-15);

    }

    @Test
    public void testPropagationWithDrag() throws OrekitException {

        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, FramesFactory.getITRF2005(true));
        Atmosphere atm = new HarrisPriester(sun, earth);

        double sf = 5.0;
        double cd = 2.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, sf);

        // Propagation of the initial at t + dt
        final double dt = 86400;
        PVCoordinates pv = propagator.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        propagator.resetInitialState(initialState);
        propagator.addForceModel(drag);
        PVCoordinates pvd = propagator.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 0.0);
        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 0.0);
        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 0.0);
        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 0.0);
        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 0.0);
        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 0.0);
    }

    @Test
    public void testStopEvent() throws OrekitException {
        final AbsoluteDate stopDate = initDate.shiftedBy(1000);
        propagator.addEventDetector(new DateDetector(stopDate) {
            private static final long serialVersionUID = -5024861864672841095L;
            public EventDetector.Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.STOP;
            }
            public SpacecraftState resetState(SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        });
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3200));
        Assert.assertTrue(gotHere);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
    }

    @Test
    public void testResetStateEvent() throws OrekitException {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        propagator.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 6453983658076746705L;
            public EventDetector.Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.RESET_STATE;
            }
            public SpacecraftState resetState(SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        });
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3200));
        Assert.assertTrue(gotHere);
        Assert.assertEquals(initialState.getMass() - 200, finalState.getMass(), 1.0e-10);
    }

    @Test
    public void testContinueEvent() throws OrekitException {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        propagator.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 5959523015368708867L;
            public EventDetector.Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.CONTINUE;
            }
        });
        final double dt = 3200;
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = 
            propagator.propagate(initDate.shiftedBy(dt));
        Assert.assertTrue(gotHere);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    private void setGotHere(boolean gotHere) {
        this.gotHere = gotHere;
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        mu  = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 0.0, 0.0);
        final Vector3D velocity = new Vector3D(0.0, 5000.0, 5000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        final double step = 250.;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);
        propagator = new DSSTPropagator(integrator, orbit);
        gotHere = false;
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
        gotHere = false;
    }

}

