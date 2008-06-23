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
import java.io.IOException;
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


public class DrozinerAttractionModelTest extends TestCase {

    public DrozinerAttractionModelTest(String name) {
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
        
        propagator.addForceModel(new DrozinerAttractionModel(itrf2000, 6378136.460, mu,
                                                             new double[][] { { 0.0 }, { 0.0 }, { c20 } },
                                                             new double[][] { { 0.0 }, { 0.0 }, { 0.0 } }));

        // let the step handler perform the test
        propagator.setMasterMode(86400, new SpotStepHandler());
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.propagate(new AbsoluteDate(date, 7 * 86400));

    }

    private static class SpotStepHandler implements OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -3917769828973243346L;

        public SpotStepHandler() {
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

        propagator.addForceModel(new DrozinerAttractionModel(itrf2000, ae, mu,
                                                             new double[][] {
                { 1.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        // let the step handler perform the test
        propagator.setMasterMode(20,
                                 new EckStepHandler(initialOrbit, ae, mu, c20, c30, c40, c50, c60));
        propagator.setInitialState(new SpacecraftState(initialOrbit, mu));
        propagator.propagate(new AbsoluteDate(date, 50000));

    }

    private static class EckStepHandler implements OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -7974453505641400294L;

        private EckStepHandler(Orbit initialOrbit, double ae, double mu,
                               double c20, double c30, double c40, double c50, double c60)
        throws FileNotFoundException, OrekitException {
            referencePropagator =
                new EcksteinHechlerPropagator(initialOrbit,
                                              ae, mu, c20, c30, c40, c50, c60);
        }

        public void handleStep(double t, double[] y, boolean isLastStep) {

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
    public void testTesserealWithCunninghamReference()
    throws OrekitException, IOException, DerivativeException, IntegratorException, ParseException {
        //  initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        double i     = Math.toRadians(98.7);
        double omega = Math.toRadians(93.0);
        double OMEGA = Math.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                                       0, KeplerianOrbit.MEAN_ANOMALY,
                                                       Frame.getJ2000(), date, mu);
        propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(100));
        propagator.addForceModel(new CunninghamAttractionModel(itrf2000, ae, mu, C, S));
        propagator.setInitialState(new SpacecraftState(orbit, mu));
        SpacecraftState cunnOrb = propagator.propagate(new AbsoluteDate(date, 86400));

        propagator.removeForceModels();
        propagator.addForceModel(new DrozinerAttractionModel(itrf2000, ae, mu, C, S));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState drozOrb = propagator.propagate(new AbsoluteDate(date,  86400));

        Vector3D dif = cunnOrb.getPVCoordinates().getPosition().subtract(drozOrb.getPVCoordinates().getPosition());
        assertEquals(0, dif.getNorm(), 1.0e-8);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
        try {
            mu  =  3.986004415e+14;
            ae  =  6378136.460;
            c20 = -1.08262631303e-3;
            c30 =  2.53248017972e-6;
            c40 =  1.61994537014e-6;
            c50 =  2.27888264414e-7;
            c60 = -5.40618601332e-7;
            itrf2000 = Frame.getITRF2000B();
            propagator = new NumericalPropagator(new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-4));

        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }
    }

    public void tearDown() {
        itrf2000   = null;
        propagator = null;
    }

    public static Test suite() {
        return new TestSuite(DrozinerAttractionModelTest.class);
    }

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    private double[][] C = new double[][] {
            {  1.000000000000e+00 },
            { -1.863039013786e-09, -5.934448524722e-10 },
            { -1.082626313026e-03, -5.880684168557e-10,  5.454582196865e-06 },
            {  2.532480179720e-06,  5.372084926301e-06,  2.393880978120e-06,  1.908327022943e-06 },
            {  1.619945370141e-06, -1.608435522852e-06,  1.051465706331e-06,  2.972622682182e-06,
                -5.654946679590e-07 },
                {  2.278882644141e-07, -2.086346283172e-07,  2.162761961684e-06, -1.498655671702e-06,
                    -9.794826452868e-07,  5.797035241535e-07 },
                    { -5.406186013322e-07, -2.736882085330e-07,  1.754209863998e-07,  2.063640268613e-07,
                        -3.101287736303e-07, -9.633248308263e-07,  3.414597413636e-08 }
    };
    private double[][] S  = new double[][] {
            {  0.000000000000e+00 },
            {  0.000000000000e+00,  1.953002572897e-10 },
            {  0.000000000000e+00,  3.277637296181e-09, -3.131184828481e-06 },
            {  0.000000000000e+00,  6.566367025901e-07, -1.637705321455e-06,  3.742073902553e-06 },
            {  0.000000000000e+00, -1.420694191113e-06,  1.987395414651e-06, -6.029325532200e-07,
                9.265045448070e-07 },
                {  0.000000000000e+00, -3.130219048314e-07, -1.072392243018e-06, -7.130099408898e-07,
                    1.651623310985e-07, -2.220047616004e-06 },
                    {  0.000000000000e+00,  9.562397128532e-08, -1.347688934659e-06,  3.220292843428e-08,
                        -1.699735804354e-06, -1.934323349167e-06, -8.559943406892e-07 }
    };

    private Frame   itrf2000;
    private NumericalPropagator propagator;

}


