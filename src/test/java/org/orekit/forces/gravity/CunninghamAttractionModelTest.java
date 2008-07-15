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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.Sun;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.DrozinerAttractionModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class CunninghamAttractionModelTest extends TestCase {

    public CunninghamAttractionModelTest(String name) {
        super(name);
        itrf2000   = null;
        propagator = null;
    }

    // rough test to determine if J2 alone creates heliosynchronism
    public void testHelioSynchronous()
    throws ParseException, FileNotFoundException,
    OrekitException, DerivativeException, IntegratorException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        Transform itrfToJ2000  = itrf2000.getTransformTo(Frame.getJ2000(), date);
        Vector3D pole          = itrfToJ2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned      = new Frame(Frame.getJ2000(),
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
        propagator.addForceModel(new CunninghamAttractionModel(itrf2000, 6378136.460, mu, c, s));

        // let the step handler perform the test
        propagator.setMasterMode(86400, new SpotStepHandler(date, mu));
        propagator.setInitialState(new SpacecraftState(orbit, mu));
        propagator.propagate(new AbsoluteDate(date, 7 * 86400));

    }

    private static class SpotStepHandler implements OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 6818305166004802991L;

        public SpotStepHandler(AbsoluteDate date, double mu) {
            sun       = new Sun();
            previous  = Double.NaN;
        }

        private Sun sun;
        private double previous;
        public void handleStep(SpacecraftState currentState, boolean isLast) {


            Vector3D pos = currentState.getPVCoordinates().getPosition();
            Vector3D vel = currentState.getPVCoordinates().getVelocity();
            AbsoluteDate current = currentState.getDate();
            Vector3D sunPos;
            try {
                sunPos = sun.getPosition(current , Frame.getJ2000());
            } catch (OrekitException e) {
                sunPos = Vector3D.ZERO;
                System.out.println("exception during sun.getPosition");
                e.printStackTrace();
            }
            Vector3D normal = Vector3D.crossProduct(pos,vel);
            double angle = Vector3D.angle(sunPos , normal);
            if (! Double.isNaN(previous)) {
                assertEquals(previous, angle, 0.0005);
            }
            previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    public void testEcksteinHechlerReference()
        throws ParseException, FileNotFoundException,
               OrekitException, DerivativeException, IntegratorException {

        //  Definition of initial conditions with position and velocity
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Transform itrfToJ2000  = itrf2000.getTransformTo(Frame.getJ2000(), date);
        Vector3D pole          = itrfToJ2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned      = new Frame(Frame.getJ2000(),
                                           new Transform(new Rotation(pole, Vector3D.PLUS_K)),
                                           "pole aligned");

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                poleAligned, date, mu);

        propagator.addForceModel(new CunninghamAttractionModel(itrf2000, ae, mu,
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

                assertTrue(dif.getNorm() < 104);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, T)) < 104);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, N)) <  53);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, W)) <  13);

            } catch (PropagationException e) {
                e.printStackTrace();
            }

        }

    }

    // test the difference with the Cunningham model
    public void testZonalWithDrozinerReference()
    throws OrekitException, DerivativeException, IntegratorException, ParseException {
//      initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        double i     = Math.toRadians(98.7);
        double omega = Math.toRadians(93.0);
        double OMEGA = Math.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                                       0, KeplerianOrbit.MEAN_ANOMALY,
                                                       Frame.getJ2000(), date, mu);
        
        propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(1000));
        propagator.addForceModel(new CunninghamAttractionModel(itrf2000, ae, mu,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        propagator.setInitialState(new SpacecraftState(orbit, mu));
        SpacecraftState cunnOrb = propagator.propagate(new AbsoluteDate(date, 86400));

        propagator.removeForceModels();

        propagator.addForceModel(new DrozinerAttractionModel(itrf2000, ae, mu,
                                                             new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        propagator.setInitialState(new SpacecraftState(orbit, mu));
        SpacecraftState drozOrb = propagator.propagate(new AbsoluteDate(date, 86400));

        Vector3D dif = cunnOrb.getPVCoordinates().getPosition().subtract(drozOrb.getPVCoordinates().getPosition());
        assertEquals(0, dif.getNorm(), 1.1e-7);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
        try {
            // Eigen c1 model truncated to degree 6
            mu =  3.986004415e+14;
            ae =  6378136.460;
            c20 = -1.08262631303e-3;
            c30 =  2.53248017972e-6;
            c40 =  1.61994537014e-6;
            c50 =  2.27888264414e-7;
            c60 = -5.40618601332e-7;

            itrf2000 = Frame.getITRF2000B();
            propagator =
                new NumericalPropagator(new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-4));
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }
    }

    public void tearDown() {
        itrf2000   = null;
        propagator = null;
    }

    public static Test suite() {
        return new TestSuite(CunninghamAttractionModelTest.class);
    }

    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;
    private double mu;
    private double ae;

    private Frame   itrf2000;
    private NumericalPropagator propagator;

}


