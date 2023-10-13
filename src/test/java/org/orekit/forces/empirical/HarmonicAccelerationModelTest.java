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
package org.orekit.forces.empirical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.*;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldEphemerisGenerator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HarmonicAccelerationModelTest extends AbstractForceModelTest {

    private Orbit initialOrbit;

    @Test
    public void testEquivalentInertialManeuver() {
        final double   delta     = FastMath.toRadians(-7.4978);
        final double   alpha     = FastMath.toRadians(351);
        final Vector3D direction = new Vector3D(alpha, delta);
        final double mass        = 2500;
        final double isp         = Double.POSITIVE_INFINITY;
        final double duration    = 4000;
        final double f           = 400;

        final AttitudeProvider maneuverLaw = new FrameAlignedProvider(new Rotation(direction, Vector3D.PLUS_I));
        ConstantThrustManeuver maneuver = new ConstantThrustManeuver(initialOrbit.getDate().shiftedBy(-10.0),
                                                                     duration, f, isp, Vector3D.PLUS_I);
        final AttitudeProvider accelerationLaw = new FrameAlignedProvider(new Rotation(direction, Vector3D.PLUS_K));
        final AccelerationModel accelerationModel = new HarmonicAccelerationModel("", AbsoluteDate.J2000_EPOCH,
                                                                                          Double.POSITIVE_INFINITY, 1);
        final ParametricAcceleration inertialAcceleration = new ParametricAcceleration(direction, true, accelerationModel);
        Assertions.assertTrue(inertialAcceleration.dependsOnPositionOnly());
        inertialAcceleration.getParametersDrivers().get(0).setValue(f / mass);
        inertialAcceleration.getParametersDrivers().get(1).setValue(0.5 * FastMath.PI);
        doTestEquivalentManeuver(mass, maneuverLaw, maneuver, accelerationLaw, inertialAcceleration, 1.0e-15);
    }

    @Test
    public void testEquivalentTangentialManeuver() {
        final double mass     = 2500;
        final double isp      = Double.POSITIVE_INFINITY;
        final double duration = 4000;
        final double f        = 400;

        final AttitudeProvider commonLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VNC);
        ConstantThrustManeuver maneuver = new ConstantThrustManeuver(initialOrbit.getDate().shiftedBy(-10.0),
                                                                     duration, f, isp, Vector3D.PLUS_I);
        final AccelerationModel accelerationModel = new HarmonicAccelerationModel("", null,
                                                                                          Double.POSITIVE_INFINITY, 1);
        final ParametricAcceleration lofAcceleration = new ParametricAcceleration(Vector3D.PLUS_I, false, accelerationModel);
        Assertions.assertFalse(lofAcceleration.dependsOnPositionOnly());
        lofAcceleration.getParametersDrivers().get(0).setValue(f / mass);
        lofAcceleration.getParametersDrivers().get(1).setValue(0.5 * FastMath.PI);
        doTestEquivalentManeuver(mass, commonLaw, maneuver, commonLaw, lofAcceleration, 1.0e-15);
    }

    @Test
    public void testEquivalentTangentialOverriddenManeuver() {
        final double mass     = 2500;
        final double isp      = Double.POSITIVE_INFINITY;
        final double duration = 4000;
        final double f        = 400;

        final AttitudeProvider maneuverLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VNC);
        ConstantThrustManeuver maneuver = new ConstantThrustManeuver(initialOrbit.getDate().shiftedBy(-10.0),
                                                                     duration, f, isp, Vector3D.PLUS_I);
        final AttitudeProvider accelerationLaw = new CelestialBodyPointed(initialOrbit.getFrame(),
                                                                          CelestialBodyFactory.getSun(), Vector3D.PLUS_K,
                                                                          Vector3D.PLUS_I, Vector3D.PLUS_K);
        final AccelerationModel accelerationModel = new HarmonicAccelerationModel("prefix", null,
                                                                                          Double.POSITIVE_INFINITY, 1);
        final ParametricAcceleration lofAcceleration = new ParametricAcceleration(Vector3D.PLUS_I, maneuverLaw, accelerationModel);
        lofAcceleration.getParametersDrivers().get(0).setValue(f / mass);
        lofAcceleration.getParametersDrivers().get(1).setValue(0.5 * FastMath.PI);
        doTestEquivalentManeuver(mass, maneuverLaw, maneuver, accelerationLaw, lofAcceleration, 1.0e-15);
    }

    private void doTestEquivalentManeuver(final double mass,
                                          final AttitudeProvider maneuverLaw,
                                          final ConstantThrustManeuver maneuver,
                                          final AttitudeProvider accelerationLaw,
                                          final ParametricAcceleration parametricAcceleration,
                                          final double positionTolerance)
        {

        SpacecraftState initialState = new SpacecraftState(initialOrbit,
                                                           maneuverLaw.getAttitude(initialOrbit,
                                                                                   initialOrbit.getDate(),
                                                                                   initialOrbit.getFrame()),
                                                           mass);

        double[][] tolerance = NumericalPropagator.tolerances(10, initialOrbit, initialOrbit.getType());

        // propagator 0 uses a maneuver that is so efficient it does not consume any fuel
        // (hence mass remains constant)
        AdaptiveStepsizeIntegrator integrator0 =
            new DormandPrince853Integrator(0.001, 100, tolerance[0], tolerance[1]);
        integrator0.setInitialStepSize(60);
        final NumericalPropagator propagator0 = new NumericalPropagator(integrator0);
        propagator0.setInitialState(initialState);
        propagator0.setAttitudeProvider(maneuverLaw);
        propagator0.addForceModel(maneuver);

        // propagator 1 uses a constant acceleration
        AdaptiveStepsizeIntegrator integrator1 =
                        new DormandPrince853Integrator(0.001, 100, tolerance[0], tolerance[1]);
        integrator1.setInitialStepSize(60);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(accelerationLaw);
        propagator1.addForceModel(parametricAcceleration);

        MultiSatStepHandler handler = interpolators -> {
            Vector3D p0 = interpolators.get(0).getCurrentState().getPosition();
            Vector3D p1 = interpolators.get(1).getCurrentState().getPosition();
            Assertions.assertEquals(0.0, Vector3D.distance(p0, p1), positionTolerance);
        };
        PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(Arrays.asList(propagator0, propagator1),
                                                                           handler);

        parallelizer.propagate(initialOrbit.getDate(), initialOrbit.getDate().shiftedBy(1000.0));

    }

    @Test
    public void testEquivalentInertialManeuverField() {
        final double   delta     = FastMath.toRadians(-7.4978);
        final double   alpha     = FastMath.toRadians(351);
        final Vector3D direction = new Vector3D(alpha, delta);
        final double mass        = 2500;
        final double isp         = Double.POSITIVE_INFINITY;
        final double duration    = 4000;
        final double f           = 400;

        final AttitudeProvider maneuverLaw = new FrameAlignedProvider(new Rotation(direction, Vector3D.PLUS_I));
        ConstantThrustManeuver maneuver = new ConstantThrustManeuver(initialOrbit.getDate().shiftedBy(-10.0),
                                                                     duration, f, isp, Vector3D.PLUS_I);
        final AttitudeProvider accelerationLaw = new FrameAlignedProvider(new Rotation(direction, Vector3D.PLUS_K));
        final AccelerationModel accelerationModel = new HarmonicAccelerationModel("", AbsoluteDate.J2000_EPOCH,
                                                                                          Double.POSITIVE_INFINITY, 1);
        final ParametricAcceleration inertialAcceleration = new ParametricAcceleration(direction, true, accelerationModel);
        inertialAcceleration.getParametersDrivers().get(0).setValue(f / mass);
        inertialAcceleration.getParametersDrivers().get(1).setValue(0.5 * FastMath.PI);
        doTestEquivalentManeuver(Binary64Field.getInstance(),
                                 mass, maneuverLaw, maneuver, accelerationLaw, inertialAcceleration, 3.0e-9);
    }

    @Test
    public void testEquivalentTangentialManeuverField() {
        final double mass     = 2500;
        final double isp      = Double.POSITIVE_INFINITY;
        final double duration = 4000;
        final double f        = 400;

        final AttitudeProvider commonLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VNC);
        ConstantThrustManeuver maneuver = new ConstantThrustManeuver(initialOrbit.getDate().shiftedBy(-10.0),
                                                                     duration, f, isp, Vector3D.PLUS_I);
        final HarmonicAccelerationModel accelerationModel = new HarmonicAccelerationModel("", null,
                                                                                          Double.POSITIVE_INFINITY, 1);
        final ParametricAcceleration lofAcceleration = new ParametricAcceleration(Vector3D.PLUS_I, false, accelerationModel);
        lofAcceleration.getParametersDrivers().get(0).setValue(f / mass);
        lofAcceleration.getParametersDrivers().get(1).setValue(0.5 * FastMath.PI);
        doTestEquivalentManeuver(Binary64Field.getInstance(),
                                 mass, commonLaw, maneuver, commonLaw, lofAcceleration, 1.0e-15);
    }

    @Test
    public void testEquivalentTangentialOverriddenManeuverField() {
        final double mass     = 2500;
        final double isp      = Double.POSITIVE_INFINITY;
        final double duration = 4000;
        final double f        = 400;

        final AttitudeProvider maneuverLaw = new LofOffset(initialOrbit.getFrame(), LOFType.VNC);
        ConstantThrustManeuver maneuver = new ConstantThrustManeuver(initialOrbit.getDate().shiftedBy(-10.0),
                                                                     duration, f, isp, Vector3D.PLUS_I);
        final AttitudeProvider accelerationLaw = new CelestialBodyPointed(initialOrbit.getFrame(),
                                                                          CelestialBodyFactory.getSun(), Vector3D.PLUS_K,
                                                                          Vector3D.PLUS_I, Vector3D.PLUS_K);
        final HarmonicAccelerationModel accelerationModel = new HarmonicAccelerationModel( "prefix", null,
                                                                                           Double.POSITIVE_INFINITY, 1);
        final ParametricAcceleration lofAcceleration = new ParametricAcceleration(Vector3D.PLUS_I, maneuverLaw, accelerationModel);
        lofAcceleration.getParametersDrivers().get(0).setValue(f / mass);
        lofAcceleration.getParametersDrivers().get(1).setValue(0.5 * FastMath.PI);
        doTestEquivalentManeuver(Binary64Field.getInstance(),
                                 mass, maneuverLaw, maneuver, accelerationLaw, lofAcceleration, 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestEquivalentManeuver(final Field<T> field,
                                                                          final double mass,
                                                                          final AttitudeProvider maneuverLaw,
                                                                          final ConstantThrustManeuver maneuver,
                                                                          final AttitudeProvider accelerationLaw,
                                                                          final ParametricAcceleration parametricAcceleration,
                                                                          final double positionTolerance)
                                                                                          {

        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(field,
                                                                          new SpacecraftState(initialOrbit,
                                                                                              maneuverLaw.getAttitude(initialOrbit,
                                                                                                                      initialOrbit.getDate(),
                                                                                                                      initialOrbit.getFrame()),
                                                                                              mass));

        double[][] tolerance = FieldNumericalPropagator.tolerances(field.getZero().add(10),
                                                                   initialState.getOrbit(),
                                                                   initialState.getOrbit().getType());

        // propagator 0 uses a maneuver that is so efficient it does not consume any fuel
        // (hence mass remains constant)
        AdaptiveStepsizeFieldIntegrator<T> integrator0 =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 100, tolerance[0], tolerance[1]);
        integrator0.setInitialStepSize(60);
        final FieldNumericalPropagator<T> propagator0 = new FieldNumericalPropagator<>(field, integrator0);
        propagator0.setInitialState(initialState);
        propagator0.setAttitudeProvider(maneuverLaw);
        propagator0.addForceModel(maneuver);
        final FieldEphemerisGenerator<T> generator1 = propagator0.getEphemerisGenerator();
        propagator0.propagate(initialState.getDate(), initialState.getDate().shiftedBy(1000.0));
        FieldBoundedPropagator<T> ephemeris0 = generator1.getGeneratedEphemeris();

        // propagator 1 uses a constant acceleration
        AdaptiveStepsizeFieldIntegrator<T> integrator1 =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 100, tolerance[0], tolerance[1]);
        integrator1.setInitialStepSize(60);
        final FieldNumericalPropagator<T> propagator1 = new FieldNumericalPropagator<>(field, integrator1);
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(accelerationLaw);
        propagator1.addForceModel(parametricAcceleration);
        final FieldEphemerisGenerator<T> generator2 = propagator1.getEphemerisGenerator();
        propagator1.propagate(initialState.getDate(), initialState.getDate().shiftedBy(1000.0));
        FieldBoundedPropagator<T> ephemeris1 = generator2.getGeneratedEphemeris();

        for (double dt = 1; dt < 999; dt += 10) {
            FieldAbsoluteDate<T> t = initialState.getDate().shiftedBy(dt);
            FieldVector3D<T> p0 = ephemeris0.propagate(t).getPosition();
            FieldVector3D<T> p1 = ephemeris1.propagate(t).getPosition();
            Assertions.assertEquals(0, FieldVector3D.distance(p0, p1).getReal(), positionTolerance);
        }

    }

    @Test
    public void testParameterDerivative1T() {
        doTestParameterDerivative(1, 4.0e-14, 2.0e-11);
    }

    @Test
    public void testParameterDerivative2T() {
        doTestParameterDerivative(2, 3.0e-14, 7.0e-12);
    }

    @Test
    public void testParameterDerivative3T() {
        doTestParameterDerivative(3, 2.0e-14, 2.0e-11);
    }

    private void doTestParameterDerivative(final int harmonicMultiplier,
                                           final double amplitudeDerivativeTolerance,
                                           final double phaseDerivativeTolerance)
        {

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel), frame,
                date, Constants.EIGEN5C_EARTH_MU);
        final LofOffset lofOffset = new LofOffset(frame, LOFType.LVLH_CCSDS);
        final Attitude attitude = lofOffset.getAttitude(orbit, date, frame);  // necessary for non-regression
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        final HarmonicAccelerationModel accelerationModel = new HarmonicAccelerationModel("kT",
                                                                                          state.getDate().shiftedBy(-2.0),
                                                                                          state.getKeplerianPeriod(), harmonicMultiplier);
        final ParametricAcceleration hpa = new ParametricAcceleration(Vector3D.PLUS_K, false, accelerationModel);
        hpa.init(state, state.getDate().shiftedBy(3600.0));
        hpa.getParametersDrivers().get(0).setValue(0.00001);
        hpa.getParametersDrivers().get(1).setValue(0.00002);
        checkParameterDerivative(state, hpa, "kT γ", 1.0e-3, amplitudeDerivativeTolerance);
        checkParameterDerivative(state, hpa, "kT φ",     1.0e-3, phaseDerivativeTolerance);

    }

    @Test
    public void testCoefficientsDetermination() {

        final double mass = 2500;
        final Orbit orbit = new CircularOrbit(7500000.0, 1.0e-4, 1.0e-3, 1.7, 0.3, 0.5, PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(),
                                        new AbsoluteDate(new DateComponents(2004, 2, 3), TimeComponents.H00,
                                                         TimeScalesFactory.getUTC()),
                                        Constants.EIGEN5C_EARTH_MU);
        final double period = orbit.getKeplerianPeriod();
        AttitudeProvider maneuverLaw = new LofOffset(orbit.getFrame(), LOFType.VNC);
        SpacecraftState initialState = new SpacecraftState(orbit,
                                                           maneuverLaw.getAttitude(orbit,
                                                                                   orbit.getDate(),
                                                                                   orbit.getFrame()),
                                                           mass);

        double dP      = 10.0;
        double minStep = 0.001;
        double maxStep = 100;
        double[][] tolerance = NumericalPropagator.tolerances(dP, orbit, orbit.getType());

        // generate PV measurements corresponding to a tangential maneuver
        AdaptiveStepsizeIntegrator integrator0 =
            new DormandPrince853Integrator(minStep, maxStep, tolerance[0], tolerance[1]);
        integrator0.setInitialStepSize(60);
        final NumericalPropagator propagator0 = new NumericalPropagator(integrator0);
        propagator0.setInitialState(initialState);
        propagator0.setAttitudeProvider(maneuverLaw);
        final ParametricAcceleration hpaRefX1 = new ParametricAcceleration(Vector3D.PLUS_I, true,
                                                                           new HarmonicAccelerationModel("refX1", null, period, 1));
        final ParametricAcceleration hpaRefY1 = new ParametricAcceleration(Vector3D.PLUS_J, true,
                                                                           new HarmonicAccelerationModel("refY1", null, period, 1));
        final ParametricAcceleration hpaRefZ2 = new ParametricAcceleration(Vector3D.PLUS_K, true,
                                                                           new HarmonicAccelerationModel("refZ2", null, period, 2));
        hpaRefX1.getParametersDrivers().get(0).setValue(2.4e-2);
        hpaRefX1.getParametersDrivers().get(1).setValue(3.1);
        hpaRefY1.getParametersDrivers().get(0).setValue(4.0e-2);
        hpaRefY1.getParametersDrivers().get(1).setValue(0.3);
        hpaRefZ2.getParametersDrivers().get(0).setValue(1.0e-2);
        hpaRefZ2.getParametersDrivers().get(1).setValue(1.8);
        propagator0.addForceModel(hpaRefX1);
        propagator0.addForceModel(hpaRefY1);
        propagator0.addForceModel(hpaRefZ2);
        ObservableSatellite sat0 = new ObservableSatellite(0);
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
        propagator0.setStepHandler(10.0,
                                   state ->
                                   measurements.add(new PV(state.getDate(),
                                                           state.getPosition(), state.getPVCoordinates().getVelocity(),
                                                           1.0e-3, 1.0e-6, 1.0, sat0)));
        propagator0.propagate(orbit.getDate().shiftedBy(900));

        // set up an estimator to retrieve the maneuver as several harmonic accelerations in inertial frame
        final NumericalPropagatorBuilder propagatorBuilder =
                        new NumericalPropagatorBuilder(orbit,
                                                       new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                       PositionAngleType.TRUE, dP);
        propagatorBuilder.addForceModel(new ParametricAcceleration(Vector3D.PLUS_I, true,
                                                                   new HarmonicAccelerationModel("X1", null, period, 1)));
        propagatorBuilder.addForceModel(new ParametricAcceleration(Vector3D.PLUS_J, true,
                                                                   new HarmonicAccelerationModel("Y1", null, period, 1)));
        propagatorBuilder.addForceModel(new ParametricAcceleration(Vector3D.PLUS_K, true,
                                                                   new HarmonicAccelerationModel("Z2", null, period, 2)));
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(), propagatorBuilder);
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(20);
        estimator.setMaxEvaluations(100);
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }

        // we will estimate only the force model parameters, not the orbit
        for (final ParameterDriver d : estimator.getOrbitalParametersDrivers(false).getDrivers()) {
            d.setSelected(false);
        }
        setParameter(estimator, "X1 γ", 1.0e-2);
        setParameter(estimator, "X1 φ", 4.0);
        setParameter(estimator, "Y1 γ", 1.0e-2);
        setParameter(estimator, "Y1 φ", 0.0);
        setParameter(estimator, "Z2 γ", 1.0e-2);
        setParameter(estimator, "Z2 φ", 1.0);

        estimator.estimate();
        Assertions.assertTrue(estimator.getIterationsCount()  < 15);
        Assertions.assertTrue(estimator.getEvaluationsCount() < 15);
        Assertions.assertEquals(0.0, estimator.getOptimum().getRMS(), 1.0e-5);

        Assertions.assertEquals(hpaRefX1.getParametersDrivers().get(0).getValue(), getParameter(estimator, "X1 γ"), 1.e-12);
        Assertions.assertEquals(hpaRefX1.getParametersDrivers().get(1).getValue(), getParameter(estimator, "X1 φ"), 1.e-12);
        Assertions.assertEquals(hpaRefY1.getParametersDrivers().get(0).getValue(), getParameter(estimator, "Y1 γ"), 1.e-12);
        Assertions.assertEquals(hpaRefY1.getParametersDrivers().get(1).getValue(), getParameter(estimator, "Y1 φ"), 1.e-12);
        Assertions.assertEquals(hpaRefZ2.getParametersDrivers().get(0).getValue(), getParameter(estimator, "Z2 γ"), 1.e-12);
        Assertions.assertEquals(hpaRefZ2.getParametersDrivers().get(1).getValue(), getParameter(estimator, "Z2 φ"), 1.e-12);

    }

    private void setParameter(BatchLSEstimator estimator, String name, double value)
        {
        for (final ParameterDriver driver : estimator.getPropagatorParametersDrivers(false).getDrivers()) {
            if (driver.getName().equals(name)) {
                driver.setSelected(true);
                driver.setValue(value);
                return;
            }
        }
        Assertions.fail("unknown parameter " + name);
    }

    // if Pdriver has only 1 value driven
    private double getParameter(BatchLSEstimator estimator, String name)
    {
    for (final ParameterDriver driver : estimator.getPropagatorParametersDrivers(false).getDrivers()) {
        if (driver.getName().equals(name)) {
            return driver.getValue();
        }
    }
    Assertions.fail("unknown parameter " + name);
    return Double.NaN;
    }

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            final double a = 24396159;
            final double e = 0.72831215;
            final double i = FastMath.toRadians(7);
            final double omega = FastMath.toRadians(180);
            final double OMEGA = FastMath.toRadians(261);
            final double lv = 0;

            final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                           new TimeComponents(23, 30, 00.000),
                                                           TimeScalesFactory.getUTC());
            initialOrbit =
                            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                               FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }

    }

}
