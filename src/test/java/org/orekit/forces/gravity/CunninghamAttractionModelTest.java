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
package org.orekit.forces.gravity;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class CunninghamAttractionModelTest extends AbstractForceModelTest {

    // rough test to determine if J2 alone creates heliosynchronism
    @Test
    public void testHelioSynchronous()
        throws ParseException, FileNotFoundException, OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Transform itrfToEME2000 = itrf2008.getTransformTo(FramesFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FramesFactory.getEME2000(),
                                            new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned", true);

        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, poleAligned, date, mu);
        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = c20;
        double[][] s = new double[3][1];
        propagator.addForceModel(new CunninghamAttractionModel(itrf2008,
                                                               GravityFieldFactory.getUnnormalizedProvider(6378136.460, mu,
                                                                                                           TideSystem.UNKNOWN,
                                                                                                           c, s)));

        // let the step handler perform the test
        propagator.setMasterMode(Constants.JULIAN_DAY, new SpotStepHandler(date, mu));
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.propagate(date.shiftedBy(7 * Constants.JULIAN_DAY));
        Assert.assertTrue(propagator.getCalls() < 9200);

    }

    private static class SpotStepHandler implements OrekitFixedStepHandler {

        public SpotStepHandler(AbsoluteDate date, double mu) throws OrekitException {
            sun       = CelestialBodyFactory.getSun();
            previous  = Double.NaN;
        }

        private PVCoordinatesProvider sun;
        private double previous;
        public void init(SpacecraftState s0, AbsoluteDate t) {
        }
        public void handleStep(SpacecraftState currentState, boolean isLast)
            throws PropagationException {


            AbsoluteDate current = currentState.getDate();
            Vector3D sunPos;
            try {
                sunPos = sun.getPVCoordinates(current , FramesFactory.getEME2000()).getPosition();
            } catch (OrekitException e) {
                throw new PropagationException(e);
            }
            Vector3D normal = currentState.getPVCoordinates().getMomentum();
            double angle = Vector3D.angle(sunPos , normal);
            if (! Double.isNaN(previous)) {
                Assert.assertEquals(previous, angle, 0.0013);
            }
            previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    @Test
    public void testEcksteinHechlerReference()
        throws ParseException, FileNotFoundException, OrekitException {

        //  Definition of initial conditions with position and velocity
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Transform itrfToEME2000 = itrf2008.getTransformTo(FramesFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FramesFactory.getEME2000(),
                                            new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned", true);

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                poleAligned, date, mu);

        propagator.addForceModel(new CunninghamAttractionModel(itrf2008,
                                                               GravityFieldFactory.getUnnormalizedProvider(ae, mu,
                                                                                                           TideSystem.UNKNOWN,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        })));

        // let the step handler perform the test
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setMasterMode(20, new EckStepHandler(initialOrbit, ae, c20, c30, c40, c50, c60));
        propagator.propagate(date.shiftedBy(50000));
        Assert.assertTrue(propagator.getCalls() < 1300);

    }

    private static class EckStepHandler implements OrekitFixedStepHandler {

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
        public void init(SpacecraftState s0, AbsoluteDate t) {
        }
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {


                SpacecraftState EHPOrbit   = referencePropagator.propagate(currentState.getDate());
                Vector3D posEHP  = EHPOrbit.getPVCoordinates().getPosition();
                Vector3D posDROZ = currentState.getPVCoordinates().getPosition();
                Vector3D velEHP  = EHPOrbit.getPVCoordinates().getVelocity();
                Vector3D dif     = posEHP.subtract(posDROZ);

                Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
                Vector3D W = EHPOrbit.getPVCoordinates().getMomentum().normalize();
                Vector3D N = Vector3D.crossProduct(W, T);

                Assert.assertTrue(dif.getNorm() < 111);
                Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, T)) < 111);
                Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, N)) <  54);
                Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, W)) <  12);

            } catch (PropagationException e) {
                e.printStackTrace();
            }

        }

    }

    // test the difference with the Cunningham model
    @Test
    public void testZonalWithDrozinerReference()
        throws OrekitException, ParseException {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(1000));
        propagator.addForceModel(new CunninghamAttractionModel(itrf2008,
                                                               GravityFieldFactory.getUnnormalizedProvider(ae, mu,
                                                                                                           TideSystem.UNKNOWN,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        })));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState cunnOrb = propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        propagator.removeForceModels();

        propagator.addForceModel(new DrozinerAttractionModel(itrf2008,
                                                             GravityFieldFactory.getUnnormalizedProvider(ae, mu,
                                                                                                         TideSystem.UNKNOWN,
                                                             new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        })));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState drozOrb = propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        Vector3D dif = cunnOrb.getPVCoordinates().getPosition().subtract(drozOrb.getPVCoordinates().getPosition());
        Assert.assertEquals(0, dif.getNorm(), 3.1e-7);
        Assert.assertTrue(propagator.getCalls() < 400);
    }

    @Test
    public void testIssue97() throws IOException, ParseException, OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState spacecraftState =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        AccelerationRetriever accelerationRetriever = new AccelerationRetriever();
        for (int i = 2; i <= 69; i++) {
            // perturbing force (ITRF2008 central body frame)
            final ForceModel cunModel =
                    new CunninghamAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getUnnormalizedProvider(i, i));
            final ForceModel droModel =
                    new DrozinerAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                GravityFieldFactory.getUnnormalizedProvider(i, i));

            /**
             * Compute acceleration
             */
            cunModel.addContribution(spacecraftState, accelerationRetriever);
            final Vector3D cunGamma = accelerationRetriever.getAcceleration();
            droModel.addContribution(spacecraftState, accelerationRetriever);
            final Vector3D droGamma = accelerationRetriever.getAcceleration();
            Assert.assertEquals(0.0, cunGamma.subtract(droGamma).getNorm(), 2.2e-9 * droGamma.getNorm());

        }

    }

    @Test
    public void testTimeDependentField() throws IOException, ParseException, OrekitException {

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState spacecraftState =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        double dP = 0.1;
        double duration = 3 * Constants.JULIAN_DAY;
        BoundedPropagator fixedFieldEphemeris   = createEphemeris(dP, spacecraftState, duration,
                                                                  GravityFieldFactory.getConstantUnnormalizedProvider(8, 8));
        BoundedPropagator varyingFieldEphemeris = createEphemeris(dP, spacecraftState, duration,
                                                                  GravityFieldFactory.getUnnormalizedProvider(8, 8));

        double step = 60.0;
        double maxDeltaT = 0;
        double maxDeltaN = 0;
        double maxDeltaW = 0;
        for (AbsoluteDate date = fixedFieldEphemeris.getMinDate();
             date.compareTo(fixedFieldEphemeris.getMaxDate()) < 0;
             date = date.shiftedBy(step)) {
            PVCoordinates pvFixedField   = fixedFieldEphemeris.getPVCoordinates(date, FramesFactory.getGCRF());
            PVCoordinates pvVaryingField = varyingFieldEphemeris.getPVCoordinates(date, FramesFactory.getGCRF());
            Vector3D t = pvFixedField.getVelocity().normalize();
            Vector3D w = pvFixedField.getMomentum().normalize();
            Vector3D n = Vector3D.crossProduct(w, t);
            Vector3D delta = pvVaryingField.getPosition().subtract(pvFixedField.getPosition());
            maxDeltaT = FastMath.max(maxDeltaT, FastMath.abs(Vector3D.dotProduct(delta, t)));
            maxDeltaN = FastMath.max(maxDeltaN, FastMath.abs(Vector3D.dotProduct(delta, n)));
            maxDeltaW = FastMath.max(maxDeltaW, FastMath.abs(Vector3D.dotProduct(delta, w)));
        }
        Assert.assertTrue(maxDeltaT > 0.15);
        Assert.assertTrue(maxDeltaT < 0.25);
        Assert.assertTrue(maxDeltaN > 0.01);
        Assert.assertTrue(maxDeltaN < 0.02);
        Assert.assertTrue(maxDeltaW > 0.05);
        Assert.assertTrue(maxDeltaW < 0.10);

    }

    private BoundedPropagator createEphemeris(double dP, SpacecraftState initialState, double duration,
                                              UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {
        double[][] tol = NumericalPropagator.tolerances(dP, initialState.getOrbit(), OrbitType.CARTESIAN);
        AbstractIntegrator integrator =
                new DormandPrince853Integrator(0.001, 120.0, tol[0], tol[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setEphemerisMode();
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(new CunninghamAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider));
        propagator.setInitialState(initialState);
        propagator.propagate(initialState.getDate().shiftedBy(duration));
        return propagator.getGeneratedEphemeris();
    }

    @Test
    public void testParameterDerivative() throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        final CunninghamAttractionModel cunninghamModel =
                new CunninghamAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                              GravityFieldFactory.getUnnormalizedProvider(20, 20));

        final String name = NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        try {
            cunninghamModel.accelerationDerivatives(state, name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.STEPS_NOT_INITIALIZED_FOR_FINITE_DIFFERENCES,
                                oe.getSpecifier());
        }
        cunninghamModel.setSteps(1.0, 1.0e10);
        checkParameterDerivative(state, cunninghamModel, name, 1.0e-4, 5.0e-12);

    }

    @Test
    public void testStateJacobian()
        throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        CunninghamAttractionModel cuModel =
                new CunninghamAttractionModel(itrf2008, GravityFieldFactory.getUnnormalizedProvider(50, 50));
        Assert.assertEquals(TideSystem.UNKNOWN, cuModel.getTideSystem());
        propagator.addForceModel(cuModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        propagator.setInitialState(state0);
        try {
            DerivativeStructure one = new DerivativeStructure(7, 1, 1.0);
            cuModel.accelerationDerivatives(state0.getDate(), state0.getFrame(),
                                            new FieldVector3D<DerivativeStructure>(one, state0.getPVCoordinates().getPosition()),
                                            new FieldVector3D<DerivativeStructure>(one, state0.getPVCoordinates().getVelocity()),
                                            new FieldRotation<DerivativeStructure>(one.multiply(state0.getAttitude().getRotation().getQ0()),
                                                           one.multiply(state0.getAttitude().getRotation().getQ1()),
                                                           one.multiply(state0.getAttitude().getRotation().getQ2()),
                                                           one.multiply(state0.getAttitude().getRotation().getQ3()),
                                                           false),
                                            one.multiply(state0.getMass()));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.STEPS_NOT_INITIALIZED_FOR_FINITE_DIFFERENCES,
                                oe.getSpecifier());
        }
        cuModel.setSteps(1.0, 1.0e10);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           40000, tolerances[0], 2.0e-7);
    }

    @Before
    public void setUp() {
        itrf2008   = null;
        propagator = null;
        Utils.setDataRoot("regular-data");
        try {
            // Eigen c1 model truncated to degree 6
            mu =  3.986004415e+14;
            ae =  6378136.460;
            c20 = -1.08262631303e-3;
            c30 =  2.53248017972e-6;
            c40 =  1.61994537014e-6;
            c50 =  2.27888264414e-7;
            c60 = -5.40618601332e-7;

            itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
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
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        itrf2008   = null;
        propagator = null;
    }

    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;
    private double mu;
    private double ae;

    private Frame   itrf2008;
    private NumericalPropagator propagator;

}


