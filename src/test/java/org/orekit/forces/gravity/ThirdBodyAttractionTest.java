/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class ThirdBodyAttractionTest extends AbstractLegacyForceModelTest {

    private double mu;

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final FieldSpacecraftState<DerivativeStructure> state) {
        try {
            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<DerivativeStructure> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field bodyField = ThirdBodyAttraction.class.getDeclaredField("body");
            bodyField.setAccessible(true);
            CelestialBody body = (CelestialBody) bodyField.get(forceModel);
            double gm = forceModel.
                        getParameterDriver(body.getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX).
                        getValue(date);

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody    = body.getPosition(date, state.getFrame());
            final double r2Central          = centralToBody.getNormSq();
            final FieldVector3D<DerivativeStructure> satToBody = position.subtract(centralToBody).negate();
            final DerivativeStructure r2Sat = satToBody.getNormSq();

            // compute relative acceleration
            final FieldVector3D<DerivativeStructure> satAcc =
                    new FieldVector3D<>(r2Sat.sqrt().multiply(r2Sat).reciprocal().multiply(gm), satToBody);
            final Vector3D centralAcc =
                    new Vector3D(gm / (r2Central * FastMath.sqrt(r2Central)), centralToBody);
            return satAcc.subtract(centralAcc);


        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final FieldSpacecraftState<Gradient> state) {
        try {
            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field bodyField = ThirdBodyAttraction.class.getDeclaredField("body");
            bodyField.setAccessible(true);
            CelestialBody body = (CelestialBody) bodyField.get(forceModel);
            double gm = forceModel.
                        getParameterDriver(body.getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX).
                        getValue(date);

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody    = body.getPosition(date, state.getFrame());
            final double r2Central          = centralToBody.getNormSq();
            final FieldVector3D<Gradient> satToBody = position.subtract(centralToBody).negate();
            final Gradient r2Sat = satToBody.getNormSq();

            // compute relative acceleration
            final FieldVector3D<Gradient> satAcc =
                    new FieldVector3D<>(r2Sat.sqrt().multiply(r2Sat).reciprocal().multiply(gm), satToBody);
            final Vector3D centralAcc =
                    new Vector3D(gm / (r2Central * FastMath.sqrt(r2Central)), centralToBody);
            return satAcc.subtract(centralAcc);


        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }


    @Test
    public void testSunContrib() {
        Assertions.assertThrows(OrekitException.class, () -> {
            // initialization
            AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                    new TimeComponents(13, 59, 27.816),
                    TimeScalesFactory.getUTC());
            Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                    FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                    FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3),
                    0.1, PositionAngleType.TRUE, FramesFactory.getEME2000(), date, mu);
            double period = 2 * FastMath.PI * orbit.getA() * FastMath.sqrt(orbit.getA() / orbit.getMu());

            // set up propagator
            NumericalPropagator calc =
                    new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
            calc.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));

            // set up step handler to perform checks
            calc.setStepHandler(FastMath.floor(period), new ReferenceChecker(date) {
                protected double hXRef(double t) {
                    return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 *
                            FastMath.cos(3.9820426e-7*t) - 7.59983e-5 * FastMath.sin(3.9820426e-7*t);
                }
                protected double hYRef(double t) {
                    return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 *
                            FastMath.cos(3.9820426e-7*t) + 17.6083e-5 * FastMath.sin(3.9820426e-7*t);
                }
            });
            AbsoluteDate finalDate = date.shiftedBy(365 * period);
            calc.setInitialState(new SpacecraftState(orbit));
            calc.propagate(finalDate);
        });
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);
        DerivativeStructure mu  = factory.constant(Constants.EIGEN5C_EARTH_MU);

        Field<DerivativeStructure> field = a_0.getField();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 mu);

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(CelestialBodyFactory.getSun());

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 1005., NP, FNP,
                                  1.0e-16, 5.0e-10, 3.0e-11, 3.0e-10,
                                  1, false);
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldGradientTest() {
        int freeParameters = 6;
        Gradient a_0 = Gradient.variable(freeParameters, 0, 7e7);
        Gradient e_0 = Gradient.variable(freeParameters, 1, 0.4);
        Gradient i_0 = Gradient.variable(freeParameters, 2, 85 * FastMath.PI / 180);
        Gradient R_0 = Gradient.variable(freeParameters, 3, 0.7);
        Gradient O_0 = Gradient.variable(freeParameters, 4, 0.5);
        Gradient n_0 = Gradient.variable(freeParameters, 5, 0.1);
        Gradient mu  = Gradient.constant(freeParameters, Constants.EIGEN5C_EARTH_MU);

        Field<Gradient> field = a_0.getField();

        FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                      PositionAngleType.MEAN,
                                                                      EME,
                                                                      J2000,
                                                                      mu);

        FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<Gradient> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(CelestialBodyFactory.getSun());

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 1005., NP, FNP,
                                  1.0e-16, 1.3e-2, 2.9e-4, 1.4e-3,
                                  1, false);
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test.
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(CelestialBodyFactory.getSun());

        FNP.addForceModel(forceModel);
     //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }
    @Test
    public void testMoonContrib() {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit =
            new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3),
                                 0.1, PositionAngleType.TRUE, FramesFactory.getEME2000(), date, mu);
        double period = 2 * FastMath.PI * orbit.getA() * FastMath.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        // set up step handler to perform checks
        calc.setStepHandler(FastMath.floor(period), new ReferenceChecker(date) {
            protected double hXRef(double t) {
                return  -0.000906173 + 1.93933e-11 * t +
                         1.0856e-06  * FastMath.cos(5.30637e-05 * t) -
                         1.22574e-06 * FastMath.sin(5.30637e-05 * t);
            }
            protected double hYRef(double t) {
                return 0.00151973 + 1.88991e-10 * t -
                       1.25972e-06  * FastMath.cos(5.30637e-05 * t) -
                       1.00581e-06 * FastMath.sin(5.30637e-05 * t);
            }
        });
        AbsoluteDate finalDate = date.shiftedBy(31 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    private static abstract class ReferenceChecker implements OrekitFixedStepHandler {

        private final AbsoluteDate reference;

        protected ReferenceChecker(AbsoluteDate reference) {
            this.reference = reference;
        }

        public void handleStep(SpacecraftState currentState) {
            double t = currentState.getDate().durationFrom(reference);
            Assertions.assertEquals(hXRef(t), currentState.getHx(), 1e-4);
            Assertions.assertEquals(hYRef(t), currentState.getHy(), 1e-4);
        }

        protected abstract double hXRef(double t);

        protected abstract double hYRef(double t);

    }

    @Test
    public void testParameterDerivative() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        Assertions.assertTrue(forceModel.dependsOnPositionOnly());
        final String name = moon.getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX;
        checkParameterDerivative(state, forceModel, name, 1.0, 7.0e-15);

    }

    @Test
    public void testParameterDerivativeGradient() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        Assertions.assertTrue(forceModel.dependsOnPositionOnly());
        final String name = moon.getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX;
        checkParameterDerivativeGradient(state, forceModel, name, 1.0, 7.0e-15);

    }

    @Test
    public void testJacobianVs80Implementation() {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        checkStateJacobianVs80Implementation(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-50, false);
    }

    @Test
    public void testJacobianVs80ImplementationGradient() {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        checkStateJacobianVs80ImplementationGradient(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-15, false);
    }

    @Test
    public void testGlobalStateJacobian()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e4, tolerances[0], 2.0e-9);

    }

    @Test
    @DisplayName("Test that acceleration derivatives with respect to absolute date are not equal to zero.")
    public void testIssue1070() {
        // GIVEN
        // Define possibly shifted absolute date
        final int freeParameters = 1;
        final GradientField field = GradientField.getField(freeParameters);
        final Gradient zero = field.getZero();
        final Gradient variable = Gradient.variable(freeParameters, 0, 0.);
        final FieldAbsoluteDate<Gradient> fieldAbsoluteDate = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH).
                shiftedBy(variable);

        // Define mock state
        @SuppressWarnings("unchecked")
        final FieldSpacecraftState<Gradient> stateMock = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(stateMock.getDate()).thenReturn(fieldAbsoluteDate);
        Mockito.when(stateMock.getPosition()).thenReturn(new FieldVector3D<>(zero, zero));
        Mockito.when(stateMock.getFrame()).thenReturn(FramesFactory.getGCRF());

        // Define third body attraction
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);

        // WHEN
        final Gradient gm = zero.add(moon.getGM());
        final FieldVector3D<Gradient> accelerationVector = forceModel.acceleration(stateMock, new Gradient[] { gm });

        // THEN
        final double[] derivatives = accelerationVector.getNormSq().getGradient();
        Assertions.assertNotEquals(0., MatrixUtils.createRealVector(derivatives).getNorm());
    }

    @BeforeEach
    public void setUp() {
        mu = 3.986e14;
        Utils.setDataRoot("regular-data");
    }

}
