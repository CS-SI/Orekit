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
package org.orekit.forces.gravity;

import java.io.FileNotFoundException;
import java.text.ParseException;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.SolarSystemBody;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FrameFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class CunninghamAttractionModelTest {

    // rough test to determine if J2 alone creates heliosynchronism
    @Test
    public void testHelioSynchronous()
    throws ParseException, FileNotFoundException,
    OrekitException, DerivativeException, IntegratorException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             UTCScale.getInstance());
        Transform itrfToEME2000 = ITRF2005.getTransformTo(FrameFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FrameFactory.getEME2000(),
                                            new Transform(new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned");

        double i     = Math.toRadians(98.7);
        double omega = Math.toRadians(93.0);
        double OMEGA = Math.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                                       0, KeplerianOrbit.MEAN_ANOMALY,
                                                       poleAligned, date, mu);
        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = c20;
        double[][] s = new double[3][1];
        propagator.addForceModel(new CunninghamAttractionModel(ITRF2005, 6378136.460, mu, c, s));

        // let the step handler perform the test
        propagator.setMasterMode(86400, new SpotStepHandler(date, mu));
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.propagate(new AbsoluteDate(date, 7 * 86400));
        assertTrue(propagator.getCalls() < 9200);

    }

    private static class SpotStepHandler implements OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 6818305166004802991L;

        public SpotStepHandler(AbsoluteDate date, double mu) throws OrekitException {
            sun       = SolarSystemBody.getSun();
            previous  = Double.NaN;
        }

        private CelestialBody sun;
        private double previous;
        public void handleStep(SpacecraftState currentState, boolean isLast) {


            Vector3D pos = currentState.getPVCoordinates().getPosition();
            Vector3D vel = currentState.getPVCoordinates().getVelocity();
            AbsoluteDate current = currentState.getDate();
            Vector3D sunPos;
            try {
                sunPos = sun.getPVCoordinates(current , FrameFactory.getEME2000()).getPosition();
            } catch (OrekitException e) {
                sunPos = Vector3D.ZERO;
                System.out.println("exception during sun.getPosition");
                e.printStackTrace();
            }
            Vector3D normal = Vector3D.crossProduct(pos,vel);
            double angle = Vector3D.angle(sunPos , normal);
            if (! Double.isNaN(previous)) {
                assertEquals(previous, angle, 0.0013);
            }
            previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    @Test
    public void testEcksteinHechlerReference()
        throws ParseException, FileNotFoundException,
               OrekitException, DerivativeException, IntegratorException {

        //  Definition of initial conditions with position and velocity
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Transform itrfToEME2000 = ITRF2005.getTransformTo(FrameFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FrameFactory.getEME2000(),
                                            new Transform(new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned");

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                poleAligned, date, mu);

        propagator.addForceModel(new CunninghamAttractionModel(ITRF2005, ae, mu,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        // let the step handler perform the test
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setMasterMode(20, new EckStepHandler(initialOrbit, ae, c20, c30, c40, c50, c60));
        propagator.propagate(new AbsoluteDate(date , 50000));
        assertTrue(propagator.getCalls() < 1300);

    }

    private static class EckStepHandler implements OrekitFixedStepHandler {

        /**Serializable UID. */
        private static final long serialVersionUID = 6132817809836153771L;

        /** Body mu */
        private static final double mu =  3.986004415e+14;

        private EckStepHandler(Orbit initialOrbit, double ae, 
                               double c20, double c30, double c40, double c50, double c60)
        throws FileNotFoundException, OrekitException {
            referencePropagator =
                new EcksteinHechlerPropagator(initialOrbit,
                                              ae, mu, c20, c30, c40, c50, c60);
        }

        private EcksteinHechlerPropagator referencePropagator;
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {


                SpacecraftState EHPOrbit   = referencePropagator.propagate(currentState.getDate());
                Vector3D posEHP  = EHPOrbit.getPVCoordinates().getPosition();
                Vector3D posDROZ = currentState.getPVCoordinates().getPosition();
                Vector3D velEHP  = EHPOrbit.getPVCoordinates().getVelocity();
                Vector3D dif     = posEHP.subtract(posDROZ);

                Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
                Vector3D W = Vector3D.crossProduct(posEHP, velEHP).normalize();
                Vector3D N = Vector3D.crossProduct(W, T);

                assertTrue(dif.getNorm() < 111);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, T)) < 111);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, N)) <  54);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, W)) <  12);

            } catch (PropagationException e) {
                e.printStackTrace();
            }

        }

    }

    // test the difference with the Cunningham model
    @Test
    public void testZonalWithDrozinerReference()
    throws OrekitException, DerivativeException, IntegratorException, ParseException {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             UTCScale.getInstance());
        double i     = Math.toRadians(98.7);
        double omega = Math.toRadians(93.0);
        double OMEGA = Math.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                                       0, KeplerianOrbit.MEAN_ANOMALY,
                                                       FrameFactory.getEME2000(), date, mu);
        
        propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(1000));
        propagator.addForceModel(new CunninghamAttractionModel(ITRF2005, ae, mu,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState cunnOrb = propagator.propagate(new AbsoluteDate(date, 86400));

        propagator.removeForceModels();

        propagator.addForceModel(new DrozinerAttractionModel(ITRF2005, ae, mu,
                                                             new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState drozOrb = propagator.propagate(new AbsoluteDate(date, 86400));

        Vector3D dif = cunnOrb.getPVCoordinates().getPosition().subtract(drozOrb.getPVCoordinates().getPosition());
        assertEquals(0, dif.getNorm(), 3.1e-7);
        assertTrue(propagator.getCalls() < 400);
    }

    @Before
    public void setUp() {
        ITRF2005   = null;
        propagator = null;
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
        try {
            // Eigen c1 model truncated to degree 6
            mu =  3.986004415e+14;
            ae =  6378136.460;
            c20 = -1.08262631303e-3;
            c30 =  2.53248017972e-6;
            c40 =  1.61994537014e-6;
            c50 =  2.27888264414e-7;
            c60 = -5.40618601332e-7;

            ITRF2005 = FrameFactory.getITRF2005();
            double[] absTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
            };
            double[] relTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
            };
            AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            propagator = new NumericalPropagator(integrator);
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        ITRF2005   = null;
        propagator = null;
    }

    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;
    private double mu;
    private double ae;

    private Frame   ITRF2005;
    private NumericalPropagator propagator;

}


