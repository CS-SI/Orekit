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
package org.orekit.propagation.numerical;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class NumericalPropagatorWithJacobiansTest {

    private AbsoluteDate                      initDate;
    private SphericalSpacecraft               spaceCraft;
    private SphericalSpacecraft               spaceCraftP;
    private SpacecraftState                   initialState;
    private NumericalPropagatorWithJacobians  propagator;

    @Test
    public void testWOJacobian() throws OrekitException {

        // Propagation of the initial at t + dt
        // NumericalPropagatorWithJacobians behaves just the
        // same as NumericalPropagator for simple propagation
        final double dt = 3200;
        final SpacecraftState finalState = 
            propagator.propagate(initDate.shiftedBy(dt));

        // Check results
        final double n = Math.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 2.0e-8);

    }

    @Test
    public void testJacobian() throws OrekitException {
    	double[][] dFdY = new double[7][7];
    	double[][] dFdP = new double[7][0];
        final double dt = 3200;
        propagator.setInitialState(initialState);
        propagator.propagate(initDate.shiftedBy(dt), dFdY, dFdP);

        // Check results
        // without forces, the orbit is keplerian
        // all elements but the latitude argument are constant 
        for (int i = 0; i < dFdY.length; i++) {
        	if (i != 5) {
                for (int j = 0; j < dFdY[i].length; j++) {
                	if (i != j) {
                        Assert.assertEquals(dFdY[i][j], 0.0, MathUtils.EPSILON);
                	} else {
                        Assert.assertEquals(dFdY[i][j], 1.0, MathUtils.EPSILON);
                	}
                }
        	}
        }

    }

    @Test
    public void testParameter() throws OrekitException {
    	double[][] dFdY = new double[7][7];
    	double[][] dFdP = new double[7][1];
        final double dt = 3200;
        propagator.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                 Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spaceCraft));
    	propagator.selectParameters(new String[] {SolarRadiationPressure.ABSORPTION_COEFFICIENT});
        propagator.setInitialState(initialState);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(dt), dFdY, dFdP);

        // Check results
        // with some forces, the orbit is no more keplerian
        for (int i = 0; i < dFdY.length; i++) {
        	if (i != 5) {
                Assert.assertEquals(dFdY[i][i], 1.0, 2.3e-7);
        	}
        }

    	propagator.removeForceModels();
        propagator.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                 Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spaceCraftP));
        propagator.setInitialState(initialState);
        final SpacecraftState finalStateP = propagator.propagate(initDate.shiftedBy(dt));

    	Assert.assertTrue(Math.abs(finalStateP.getA() - finalState.getA()) < Math.abs(dFdP[0][0]));
    	Assert.assertTrue(Math.abs(finalStateP.getEquinoctialEx() - finalState.getEquinoctialEx()) < Math.abs(dFdP[1][0]));
    	Assert.assertTrue(Math.abs(finalStateP.getEquinoctialEy() - finalState.getEquinoctialEy()) < Math.abs(dFdP[2][0]));
    	Assert.assertTrue(Math.abs(finalStateP.getHx() - finalState.getHx()) < Math.abs(dFdP[3][0]));
    	Assert.assertTrue(Math.abs(finalStateP.getHy() - finalState.getHy()) < Math.abs(dFdP[4][0]));
    	Assert.assertTrue(Math.abs(finalStateP.getLv() - finalState.getLv()) < Math.abs(dFdP[5][0]));
    	Assert.assertTrue(Math.abs(finalStateP.getMass() - finalState.getMass()) == Math.abs(dFdP[6][0]));

    }

    @Test(expected=PropagationException.class)
    public void testException() throws OrekitException {
    	double[][] dFdY = null;
    	double[][] dFdP = null;
        final double dt = 3200;
    	propagator.removeForceModels();
    	propagator.selectParameters(new String[] {DragForce.DRAG_COEFFICIENT});
        propagator.propagate(initDate.shiftedBy(dt), dFdY, dFdP);
    }

    @Before
    public void setUp() {

    	Utils.setDataRoot("regular-data");

    	final double mu  = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getTAI());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[] absTolV = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolV = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolV, relTolV);
        integrator.setInitialStepSize(120);
        propagator = new NumericalPropagatorWithJacobians(integrator);
        propagator.setInitialState(initialState);
        // Spacecraft definition
        final double surf = 25.;
        final double drag =  0.4; // drag coeff.
        final double srpr =  0.9; // solar radiation pressure coeff.
        final double srprP =  0.9 * (1. + Math.sqrt(MathUtils.EPSILON)); // augmented solar radiation pressure coeff.
        spaceCraft = new SphericalSpacecraft(surf, drag, srpr, srpr);
        spaceCraftP = new SphericalSpacecraft(surf, drag, srprP, srpr);
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
    }

}

