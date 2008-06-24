/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.propagation.numerical;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class NumericalPropagatorTest extends TestCase {

    private double               mu;
    private AbsoluteDate         initDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;
    private boolean              gotHere;
    
    public NumericalPropagatorTest(String name) {
        super(name);
    }

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
        assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
        assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
        assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
        assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
        assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
        assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);

    }

    public void testNotInitialised() {
        try {
            final NumericalPropagator notInitialised =
                new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
            notInitialised.propagate(AbsoluteDate.J2000_EPOCH);
            fail("an exception should have been thrown");
        } catch (PropagationException pe) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testKepler() throws OrekitException {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final SpacecraftState finalState = 
            propagator.propagate(new AbsoluteDate(initDate, dt));

        // Check results
        final double n = Math.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 4.0e-10);

    }

    public void testStopEvent() throws OrekitException {
        final AbsoluteDate stopDate = new AbsoluteDate(initDate, 1000);
        propagator.addEventDetector(new DateDetector(stopDate) {
            private static final long serialVersionUID = -5024861864672841095L;
            public int eventOccurred(SpacecraftState s) throws OrekitException {
                setGotHere(true);
                return STOP;
            }
            public SpacecraftState resetState(SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        });
        assertFalse(gotHere);
        final SpacecraftState finalState = propagator.propagate(new AbsoluteDate(initDate, 3200));
        assertTrue(gotHere);
        assertEquals(0, finalState.getDate().minus(stopDate), 1.0e-10);
    }

    public void testResetStateEvent() throws OrekitException {
        final AbsoluteDate resetDate = new AbsoluteDate(initDate, 1000);
        propagator.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 6453983658076746705L;
            public int eventOccurred(SpacecraftState s) throws OrekitException {
                setGotHere(true);
                return RESET_STATE;
            }
            public SpacecraftState resetState(SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        });
        assertFalse(gotHere);
        final SpacecraftState finalState = propagator.propagate(new AbsoluteDate(initDate, 3200));
        assertTrue(gotHere);
        assertEquals(initialState.getMass() - 200, finalState.getMass(), 1.0e-10);
    }

    public void testResetDerivativesEvent() throws OrekitException {
        final AbsoluteDate resetDate = new AbsoluteDate(initDate, 1000);
        propagator.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 4217482936692909475L;
            public int eventOccurred(SpacecraftState s) throws OrekitException {
                setGotHere(true);
                return RESET_DERIVATIVES;
            }
        });
        final double dt = 3200;
        assertFalse(gotHere);
        final SpacecraftState finalState = 
            propagator.propagate(new AbsoluteDate(initDate, dt));
        assertTrue(gotHere);
        final double n = Math.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 4.0e-10);
    }

    public void testContinueEvent() throws OrekitException {
        final AbsoluteDate resetDate = new AbsoluteDate(initDate, 1000);
        propagator.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 5959523015368708867L;
            public int eventOccurred(SpacecraftState s) throws OrekitException {
                setGotHere(true);
                return CONTINUE;
            }
        });
        final double dt = 3200;
        assertFalse(gotHere);
        final SpacecraftState finalState = 
            propagator.propagate(new AbsoluteDate(initDate, dt));
        assertTrue(gotHere);
        final double n = Math.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 4.0e-10);
    }

    private void setGotHere(boolean gotHere) {
        this.gotHere = gotHere;
    }

    public void setUp() {
        mu  = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 Frame.getJ2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        propagator =
            new NumericalPropagator(new DormandPrince853Integrator(0.0, 10000.0, 1.0e-8, 1.0e-8));
        propagator.setInitialState(initialState);
        gotHere = false;
    }

    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
        gotHere = false;
    }
    
    public static Test suite() {
        return new TestSuite(NumericalPropagatorTest.class);
    }

}

