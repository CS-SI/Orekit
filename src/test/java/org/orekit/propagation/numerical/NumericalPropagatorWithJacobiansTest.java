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

import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModelWithJacobians;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class NumericalPropagatorWithJacobiansTest {

    private AbsoluteDate                     initDate;
    private SphericalSpacecraft              spaceCraft;
    private SpacecraftState                  initialState;
    private NumericalPropagatorWithJacobians propagator;

    // Integrator parameters
    private final double minStep = 0.001;
    private final double maxStep = 1000.0;
    private final double[] absTolV = {0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
    private final double[] relTolV = {1.0e-10, 1.0e-7, 1.0e-7, 1.0e-10, 1.0e-10, 1.0e-10, 1.0e-10};

    // Orbit parameters : position = (7.0e6, 1.0e6, 4.0e6) ; velocity = (-500.0, 8000.0, 1000.0)
    private final double a  =  1.2123382253763368E7;
    private final double ex =  0.33088598908457206;
    private final double ey = -0.119544860585116;
    private final double hx =  0.07403074327464344;
    private final double hy = -0.25499478239043855;
    private final double lv =  0.1602263984451438;
    private final double mu =  3.9860047e14;

    // Force models parameters
    private final double drag = 0.4; // drag coeff.
    private final double srpr = 0.9; // solar radiation pressure coeff.

    // Threshold for test acceptance
    private final double ERRMAX = FastMath.sqrt(MathUtils.EPSILON);

    @Test
    public void testWithoutJacobian() throws OrekitException {

        // Propagation of the initial at t + dt
        // NumericalPropagatorWithJacobians behaves just the
        // same as NumericalPropagator for simple propagation
        final double dt = 300;
        final SpacecraftState finalState = 
            propagator.propagate(initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 2.0e-8);
    }

    @Test
    public void testKeplerianJacobian() throws OrekitException {
    	double[][] dFdY = new double[7][7];
    	double[][] dFdP = new double[7][0];
        final double dt = 300;
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
    public void testOrbitalParametersJacobian() throws OrekitException {

    	double[][] dFdY = new double[7][7];
    	double[][] dFdP = new double[7][0];
    	final double dt = 3;

    	ForceModelWithJacobians sunPressure =
        	new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                       Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spaceCraft);
        propagator.addForceModel(sunPressure);
        propagator.setInitialState(initialState);
        final SpacecraftState finalState = 
            propagator.propagate(initDate.shiftedBy(dt), dFdY, dFdP);

    	final double da = absTolV[0]*1.e1;
        propagator.resetInitialState(
            new SpacecraftState(new EquinoctialOrbit(a + da, ex, ey, hx, hy, lv,
                                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                                     FramesFactory.getEME2000(), initDate, mu)));
        final SpacecraftState endStateDa = propagator.propagate(initDate.shiftedBy(dt));
        // Check results
	    final double dAda0  = (endStateDa.getA() - finalState.getA()) / da;
        final double dExda0 = (endStateDa.getEquinoctialEx() - finalState.getEquinoctialEx()) / da;
        final double dEyda0 = (endStateDa.getEquinoctialEy() - finalState.getEquinoctialEy()) / da;
        final double dHxda0 = (endStateDa.getHx() - finalState.getHx()) / da;
        final double dHyda0 = (endStateDa.getHy() - finalState.getHy()) / da;
        final double dLvda0 = (endStateDa.getLv() - finalState.getLv()) / da;
        final double dMda0  = (endStateDa.getMass() - finalState.getMass()) / da;

        Assert.assertTrue(FastMath.abs((dAda0  - dFdY[0][0])/finalState.getA()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dExda0 - dFdY[1][0])/finalState.getEquinoctialEx()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dEyda0 - dFdY[2][0])/finalState.getEquinoctialEy()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dHxda0 - dFdY[3][0])/finalState.getHx()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dHyda0 - dFdY[4][0])/finalState.getHy()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dLvda0 - dFdY[5][0])/finalState.getHy()) < ERRMAX);
        Assert.assertEquals(dMda0, dFdY[6][0], 0.0);

    	final double dex = absTolV[1]*1.e1;
        propagator.resetInitialState(
            new SpacecraftState(new EquinoctialOrbit(a, ex + dex, ey, hx, hy, lv,
                                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                                     FramesFactory.getEME2000(), initDate, mu)));
        final SpacecraftState endStateDex = propagator.propagate(initDate.shiftedBy(dt));
        // Check results
	    final double dAdex0  = (endStateDex.getA() - finalState.getA()) / dex;
        final double dExdex0 = (endStateDex.getEquinoctialEx() - finalState.getEquinoctialEx()) / dex;
        final double dEydex0 = (endStateDex.getEquinoctialEy() - finalState.getEquinoctialEy()) / dex;
        final double dHxdex0 = (endStateDex.getHx() - finalState.getHx()) / dex;
        final double dHydex0 = (endStateDex.getHy() - finalState.getHy()) / dex;
        final double dLvdex0 = (endStateDex.getLv() - finalState.getLv()) / dex;
        final double dMdex0  = (endStateDex.getMass() - finalState.getMass()) / dex;

        Assert.assertTrue(FastMath.abs((dAdex0  - dFdY[0][1])/finalState.getA()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dExdex0 - dFdY[1][1])/finalState.getEquinoctialEx()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dEydex0 - dFdY[2][1])/finalState.getEquinoctialEy()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dHxdex0 - dFdY[3][1])/finalState.getHx()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dHydex0 - dFdY[4][1])/finalState.getHy()) < ERRMAX);
        Assert.assertTrue(FastMath.abs((dLvdex0 - dFdY[5][1])/finalState.getHy()) < ERRMAX);
        Assert.assertEquals(dMdex0, dFdY[6][1], 0.0);

    	final double dey = absTolV[2]*1.e1;
        propagator.resetInitialState(
            new SpacecraftState(new EquinoctialOrbit(a, ex, ey + dey, hx, hy, lv,
                                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                                     FramesFactory.getEME2000(), initDate, mu)));
        final SpacecraftState endStateDey = propagator.propagate(initDate.shiftedBy(dt));
        // Check results
	    final double dAdey0  = (endStateDey.getA() - finalState.getA()) / dey;
        final double dExdey0 = (endStateDey.getEquinoctialEx() - finalState.getEquinoctialEx()) / dey;
        final double dEydey0 = (endStateDey.getEquinoctialEy() - finalState.getEquinoctialEy()) / dey;
        final double dHxdey0 = (endStateDey.getHx() - finalState.getHx()) / dey;
        final double dHydey0 = (endStateDey.getHy() - finalState.getHy()) / dey;
        final double dLvdey0 = (endStateDey.getLv() - finalState.getLv()) / dey;
        final double dMdey0  = (endStateDey.getMass() - finalState.getMass()) / dey;

        Assert.assertTrue(FastMath.abs(dAdey0  - dFdY[0][2])/finalState.getA() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dExdey0 - dFdY[1][2])/finalState.getEquinoctialEx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dEydey0 - dFdY[2][2])/finalState.getEquinoctialEy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHxdey0 - dFdY[3][2])/finalState.getHx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHydey0 - dFdY[4][2])/finalState.getHy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dLvdey0 - dFdY[5][2])/finalState.getLv() < ERRMAX);
        Assert.assertEquals(dMdey0, dFdY[6][2], 0.0);

    	final double dhx = absTolV[3]*1.e1;
        propagator.resetInitialState(
            new SpacecraftState(new EquinoctialOrbit(a, ex, ey, hx + dhx, hy, lv,
                                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                                     FramesFactory.getEME2000(), initDate, mu)));
        final SpacecraftState endStateDhx = propagator.propagate(initDate.shiftedBy(dt));
        // Check results
	    final double dAdhx0  = (endStateDhx.getA() - finalState.getA()) / dhx;
        final double dExdhx0 = (endStateDhx.getEquinoctialEx() - finalState.getEquinoctialEx()) / dhx;
        final double dEydhx0 = (endStateDhx.getEquinoctialEy() - finalState.getEquinoctialEy()) / dhx;
        final double dHxdhx0 = (endStateDhx.getHx() - finalState.getHx()) / dhx;
        final double dHydhx0 = (endStateDhx.getHy() - finalState.getHy()) / dhx;
        final double dLvdhx0 = (endStateDhx.getLv() - finalState.getLv()) / dhx;
        final double dMdhx0  = (endStateDhx.getMass() - finalState.getMass()) / dhx;

        Assert.assertTrue(FastMath.abs(dAdhx0  - dFdY[0][3])/finalState.getA() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dExdhx0 - dFdY[1][3])/finalState.getEquinoctialEx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dEydhx0 - dFdY[2][3])/finalState.getEquinoctialEy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHxdhx0 - dFdY[3][3])/finalState.getHx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHydhx0 - dFdY[4][3])/finalState.getHy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dLvdhx0 - dFdY[5][3])/finalState.getLv() < ERRMAX);
        Assert.assertEquals(dMdhx0, dFdY[6][3], 0.0);

    	final double dhy = absTolV[4]*1.e1;
        propagator.resetInitialState(
            new SpacecraftState(new EquinoctialOrbit(a, ex, ey, hx, hy + dhy, lv,
                                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                                     FramesFactory.getEME2000(), initDate, mu)));
        final SpacecraftState endStateDhy = propagator.propagate(initDate.shiftedBy(dt));
        // Check results
	    final double dAdhy0  = (endStateDhy.getA() - finalState.getA()) / dhy;
        final double dExdhy0 = (endStateDhy.getEquinoctialEx() - finalState.getEquinoctialEx()) / dhy;
        final double dEydhy0 = (endStateDhy.getEquinoctialEy() - finalState.getEquinoctialEy()) / dhy;
        final double dHxdhy0 = (endStateDhy.getHx() - finalState.getHx()) / dhy;
        final double dHydhy0 = (endStateDhy.getHy() - finalState.getHy()) / dhy;
        final double dLvdhy0 = (endStateDhy.getLv() - finalState.getLv()) / dhy;
        final double dMdhy0  = (endStateDhy.getMass() - finalState.getMass()) / dhy;

        Assert.assertTrue(FastMath.abs(dAdhy0  - dFdY[0][4])/finalState.getA() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dExdhy0 - dFdY[1][4])/finalState.getEquinoctialEx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dEydhy0 - dFdY[2][4])/finalState.getEquinoctialEy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHxdhy0 - dFdY[3][4])/finalState.getHx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHydhy0 - dFdY[4][4])/finalState.getHy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dLvdhy0 - dFdY[5][4])/finalState.getLv() < ERRMAX);
        Assert.assertEquals(dMdhy0, dFdY[6][4], 0.0);

    	final double dlv = absTolV[5]*1.e1;
        propagator.resetInitialState(
            new SpacecraftState(new EquinoctialOrbit(a, ex, ey, hx, hy, lv + dlv,
                                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                                     FramesFactory.getEME2000(), initDate, mu)));
        final SpacecraftState endStateDlv = propagator.propagate(initDate.shiftedBy(dt));
        // Check results
	    final double dAdlv0  = (endStateDlv.getA() - finalState.getA()) / dlv;
        final double dExdlv0 = (endStateDlv.getEquinoctialEx() - finalState.getEquinoctialEx()) / dlv;
        final double dEydlv0 = (endStateDlv.getEquinoctialEy() - finalState.getEquinoctialEy()) / dlv;
        final double dHxdlv0 = (endStateDlv.getHx() - finalState.getHx()) / dlv;
        final double dHydlv0 = (endStateDlv.getHy() - finalState.getHy()) / dlv;
        final double dLvdlv0 = (endStateDlv.getLv() - finalState.getLv()) / dlv;
        final double dMdlv0  = (endStateDlv.getMass() - finalState.getMass()) / dlv;

        Assert.assertTrue(FastMath.abs(dAdlv0  - dFdY[0][5])/finalState.getA() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dExdlv0 - dFdY[1][5])/finalState.getEquinoctialEx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dEydlv0 - dFdY[2][5])/finalState.getEquinoctialEy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHxdlv0 - dFdY[3][5])/finalState.getHx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHydlv0 - dFdY[4][5])/finalState.getHy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dLvdlv0 - dFdY[5][5])/finalState.getLv() < ERRMAX);
        Assert.assertEquals(dMdlv0, dFdY[6][5], 0.0);
    }

    @Test
    public void testForceParameterJacobian() throws OrekitException {
    	double[][] dFdY = new double[7][7];
    	double[][] dFdP = new double[7][1];
        final double dt = 300;

        ForceModelWithJacobians sunPressure =
        	new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                       Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spaceCraft);
        propagator.addForceModel(sunPressure);
    	propagator.selectParameters(new String[] {SolarRadiationPressure.ABSORPTION_COEFFICIENT});
        propagator.setInitialState(initialState);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(dt), dFdY, dFdP);

    	propagator.removeForceModels();
        final double dp = srpr * 0.1;
        final double srprDP = srpr + dp;
    	sunPressure.setParameter(SolarRadiationPressure.ABSORPTION_COEFFICIENT, srprDP);
        propagator.addForceModel(sunPressure);
        propagator.setInitialState(initialState);
        final SpacecraftState endStateDP = propagator.propagate(initDate.shiftedBy(dt));

        // Check results
        final double dAdP  = (endStateDP.getA() - finalState.getA()) / dp;
        final double dExdP = (endStateDP.getEquinoctialEx() - finalState.getEquinoctialEx()) / dp;
        final double dEydP = (endStateDP.getEquinoctialEy() - finalState.getEquinoctialEy()) / dp;
        final double dHxdP = (endStateDP.getHx() - finalState.getHx()) / dp;
        final double dHydP = (endStateDP.getHy() - finalState.getHy()) / dp;
        final double dLvdP = (endStateDP.getLv() - finalState.getLv()) / dp;

        Assert.assertTrue(FastMath.abs(dAdP  - dFdP[0][0])/finalState.getA() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dExdP - dFdP[1][0])/finalState.getEquinoctialEx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dEydP - dFdP[2][0])/finalState.getEquinoctialEy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHxdP - dFdP[3][0])/finalState.getHx() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dHydP - dFdP[4][0])/finalState.getHy() < ERRMAX);
        Assert.assertTrue(FastMath.abs(dLvdP - dFdP[5][0])/finalState.getLv() < ERRMAX);
    }

    @Test(expected=PropagationException.class)
    public void testException() throws OrekitException {
    	double[][] dFdY = null;
    	double[][] dFdP = null;
        final double dt = 300;
    	propagator.removeForceModels();
    	propagator.selectParameters(new String[] {DragForce.DRAG_COEFFICIENT});
        propagator.propagate(initDate.shiftedBy(dt), dFdY, dFdP);
    }

    @Before
    public void setUp() {

    	Utils.setDataRoot("regular-data");

        initDate = new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getTAI());
        initialState =
            new SpacecraftState(
                new EquinoctialOrbit(a, ex, ey, hx, hy, lv,
                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                     FramesFactory.getEME2000(), initDate, mu));
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxStep, absTolV, relTolV);
        propagator = new NumericalPropagatorWithJacobians(integrator);
        propagator.setInitialState(initialState);

        // Spacecraft definition for force models
        final double surf = 25.;
        spaceCraft = new SphericalSpacecraft(surf, drag, srpr, srpr);
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
    }

}

