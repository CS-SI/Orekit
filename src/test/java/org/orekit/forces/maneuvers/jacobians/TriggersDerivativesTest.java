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
package org.orekit.forces.maneuvers.jacobians;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TriggersDerivativesTest {

    @Test
    public void testDerivativeWrtStartTimeCartesianForward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.CARTESIAN, true,
                                         4.3e-6, 2.8e-6, 4.3e-6, 1.5e-5, 8.1e-6, 1.5e-5);
    }

    @Test
    public void testDerivativeWrtStartTimeKeplerianForward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.KEPLERIAN, true,
                                         1.5e-5, 1.5e-5, 1.5e-5, 6.8e-6, 1.1e-4, 3.2e-5);
    }

    @Test
    public void testDerivativeWrtStartTimeCartesianBackward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.CARTESIAN, false,
                                         9.7e-5, 9.7e-5, 9.7e-5, 4.7e-8, 4.7e-8, 4.7e-8);
    }

    @Test
    public void testDerivativeWrtStartTimeKeplerianBackward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.KEPLERIAN, false,
                                         6.5e-8, 6.5e-8, 6.5e-8, 1.1e-6, 1.8e-6, 6.2e-7);
    }

    @Test
    public void testDerivativeWrtStopTimeCartesianForward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.CARTESIAN, true,
                                         5.2e-10, 6.0e-8, 6.6e-11, 6.8e-12, 3.9e-11, 7.5e-12);
    }

    @Test
    public void testDerivativeWrtStopTimeKeplerianForward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.KEPLERIAN, true,
                                         1.8e-11, 9.7e-12, 3.0e-12, 2.6e-9, 2.9e-9, 1.8e-9);
    }

    @Test
    public void testDerivativeWrtStopTimeCartesianBackward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.CARTESIAN, false,
                                         9.7e-5, 9.7e-5, 9.7e-5, 1.5e-5, 2.1e-5, 1.5e-5);
    }

    @Test
    public void testDerivativeWrtStopTimeKeplerianBackward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.KEPLERIAN, false,
                                         1.5e-5, 1.5e-5, 1.5e-5, 3.8e-6, 1.2e-4, 3.0e-4);
    }

    @Test
    public void testDerivativeWrtMedianCartesianForward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.CARTESIAN, true,
                                          1.8e-5, 1.2e-5, 1.8e-5, 2.0e-2, 1.7e-2, 2.0e-2);
    }

    @Test
    public void testDerivativeWrtMedianKeplerianForward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.KEPLERIAN, true,
                                          0.095, 0.13, 0.11, 9.0e-6, 4.8e-5, 3.2e-4);
    }

    @Test
    public void testDerivativeWrtMedianCartesianBackward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.CARTESIAN, false,
                                          9.7e-5, 9.7e-5, 9.7e-5, 1.9e-2, 2.1e-2, 2.0e-2);
    }

    @Test
    public void testDerivativeWrtMedianKeplerianBackward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.KEPLERIAN, false,
                                          0.091, 0.13, 0.11, 7.4e-6, 4.7e-5, 3.4e-4);
    }

    @Test
    public void testDerivativeWrtDurationCartesianForward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.CARTESIAN, true,
                                          2.5e-6, 1.6e-6, 2.5e-6, 7.3e-6, 4.1e-6, 7.2e-6);
    }

    @Test
    public void testDerivativeWrtDurationKeplerianForward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.KEPLERIAN, true,
                                          7.2e-6, 7.2e-6, 7.2e-6, 2.5e-6, 2.5e-5, 1.5e-5);
    }

    @Test
    public void testDerivativeWrtDurationCartesianBackward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.CARTESIAN, false,
                                          9.7e-5, 9.7e-5, 9.7e-5, 7.1e-6, 1.1e-5, 7.2e-6);
    }

    @Test
    public void testDerivativeWrtDurationKeplerianBackward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.KEPLERIAN, false,
                                          7.2e-6, 7.2e-6, 7.2e-6, 2.3e-6, 2.9e-5, 3.0e-4);
    }

    private void doTestDerivativeWrtStartStopTime(final boolean start, final OrbitType orbitType, final boolean forward,
                                                  final double...tolerance) {

        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final int           degree        = 20;
        final double        duration      = 200.0;
        final double        h             = 1.0;
        final double        samplingtep   = 2.0;

        final KeplerianOrbit initial    = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(buildInitialState(buildAttitudeProvider()).getOrbit());
        final double         firingM    = MathUtils.normalizeAngle(FastMath.PI, initial.getMeanAnomaly() + (forward ? FastMath.PI : -FastMath.PI));
        final AbsoluteDate   apogeeDate = initial.getDate().shiftedBy((firingM - initial.getMeanAnomaly()) / initial.getKeplerianMeanMotion());
        final AbsoluteDate   firing     = apogeeDate.shiftedBy(-0.5 * duration);

        final List<Propagator> propagators = new ArrayList<>();

        // propagators will be combined using finite differences to compute derivatives
        for (int k = -4; k <= 4; ++k) {
            final DateBasedManeuverTriggers trigger = start ?
                                                      new DateBasedManeuverTriggers("MAN_0", firing.shiftedBy(k * h), duration - k * h) :
                                                      new DateBasedManeuverTriggers("MAN_0", firing, duration + k * h);
            propagators.add(buildPropagator(orbitType, positionAngleType, degree, firing, duration, trigger));
        }

        // the central propagator (k = 4) will compute derivatives autonomously using State and TriggersDerivatives
        final NumericalPropagator autonomous = (NumericalPropagator) propagators.get(4);
        final MatricesHarvester   harvester  = autonomous.setupMatricesComputation("stm", null, null);
        autonomous.
        getAllForceModels().
        forEach(fm -> fm.
                          getParametersDrivers().
                          stream().
                          filter(d -> d.getName().equals(start ? "MAN_0_START" : "MAN_0_STOP") ||
                                      d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                          forEach(d -> d.setSelected(true)));

        DerivativesSampler sampler = start ?
                                     new DerivativesSampler(harvester, 4, null, -1, null, -1, null, -1, orbitType, positionAngleType,
                                                            firing, duration, h, samplingtep) :
                                     new DerivativesSampler(null, -1, harvester, 4, null, -1, null, -1, orbitType, positionAngleType,
                                                            firing, duration, h, samplingtep);

        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, sampler);
        if (forward) {
            parallelizer.propagate(firing.shiftedBy(-30 * samplingtep), firing.shiftedBy(duration + 300 * samplingtep));
        } else {
            parallelizer.propagate(firing.shiftedBy(duration + 30 * samplingtep), firing.shiftedBy(-300 * samplingtep));
        }

        double[] maxRelativeError = new double[tolerance.length];
        for (final Entry entry : sampler.sample) {
            final Result result = start ? entry.start : entry.stop;
            for (int i = 0; i < 6; ++i) {
                double f = result.finiteDifferences[i].getFirstDerivative();
                double c = result.closedForm[i].getFirstDerivative();
                maxRelativeError[i] = FastMath.max(maxRelativeError[i], FastMath.abs(f - c) / FastMath.max(1.0e-10, FastMath.abs(f)));
            }
        }

        for (int i = 0; i < tolerance.length; ++i) {
            Assertions.assertEquals(0.0, maxRelativeError[i], tolerance[i]);
        }

    }

    private void doTestDerivativeWrtMedianDuration(final boolean median, final OrbitType orbitType, final boolean forward,
                                                   final double...tolerance) {

        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final int           degree        = 20;
        final double        duration      = 200.0;
        final double        h             = 1.0;
        final double        samplingtep   = 2.0;

        final KeplerianOrbit initial    = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(buildInitialState(buildAttitudeProvider()).getOrbit());
        final double         firingM    = MathUtils.normalizeAngle(FastMath.PI, initial.getMeanAnomaly() + (forward ? FastMath.PI : -FastMath.PI));
        final AbsoluteDate   apogeeDate = initial.getDate().shiftedBy((firingM - initial.getMeanAnomaly()) / initial.getKeplerianMeanMotion());
        final AbsoluteDate   firing     = apogeeDate.shiftedBy(-0.5 * duration);

        final List<Propagator> propagators = new ArrayList<>();

        // propagators will be combined using finite differences to compute derivatives
        for (int k = -4; k <= 4; ++k) {
            final DateBasedManeuverTriggers triggers = median ?
                            new DateBasedManeuverTriggers("MAN_0", firing.shiftedBy(k * h), duration) :
                            new DateBasedManeuverTriggers("MAN_0", firing.shiftedBy(-0.5 * k * h), duration + k * h);
            propagators.add(buildPropagator(orbitType, positionAngleType, degree, firing, duration, triggers));
        }
        for (int k = -4; k <= 4; ++k) {
            final DateBasedManeuverTriggers triggers =
                            new DateBasedManeuverTriggers("MAN_1", firing.shiftedBy(k * h), duration - k * h);
            propagators.add(buildPropagator(orbitType, positionAngleType, degree, firing, duration, triggers));
        }
        for (int k = -4; k <= 4; ++k) {
            final DateBasedManeuverTriggers triggers =
                            new DateBasedManeuverTriggers("MAN_2", firing, duration + k * h);
            propagators.add(buildPropagator(orbitType, positionAngleType, degree, firing, duration, triggers));
        }

        // the central propagators (k = 4, 13, 22) will compute derivatives autonomously
        // using StateTransitionMatrixGenerator, TriggerDateJacobianColumnGenerator,
        // MedianDateJacobianColumnGenerator and DurationJacobianColumnGenerator
        final NumericalPropagator autonomous = (NumericalPropagator) propagators.get(4);
        final MatricesHarvester   harvester  = autonomous.setupMatricesComputation("stm-0", null, null);
        autonomous.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals(median ? "MAN_0_MEDIAN" : "MAN_0_DURATION") ||
                            d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));

        final NumericalPropagator autonomousStart = (NumericalPropagator) propagators.get(13);
        final MatricesHarvester   harvesterStart  = autonomousStart.setupMatricesComputation("stm-1", null, null);
        autonomousStart.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals("MAN_1_START")  ||
                            d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));

        final NumericalPropagator autonomousStop = (NumericalPropagator) propagators.get(22);
        final MatricesHarvester   harvesterStop  = autonomousStop.setupMatricesComputation("stm-2", null, null);
        autonomousStop.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals("MAN_2_STOP")   ||
                            d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));

        DerivativesSampler sampler = median ?
                                     new DerivativesSampler(harvesterStart, 13, harvesterStop, 22, harvester, 4, null, -1,
                                                            orbitType, positionAngleType,
                                                            firing, duration, h, samplingtep) :
                                     new DerivativesSampler(harvesterStart, 13, harvesterStop, 22, null, -1, harvester, 4,
                                                            orbitType, positionAngleType,
                                                            firing, duration, h, samplingtep);

        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, sampler);
        if (forward) {
            parallelizer.propagate(firing.shiftedBy(-30 * samplingtep), firing.shiftedBy(duration + 300 * samplingtep));
        } else {
            parallelizer.propagate(firing.shiftedBy(duration + 30 * samplingtep), firing.shiftedBy(-300 * samplingtep));
        }

        double[] maxRelativeError = new double[tolerance.length];
        for (final Entry entry : sampler.sample) {
            final Result result = median ? entry.median : entry.duration;
            for (int i = 0; i < 6; ++i) {
                double f = result.finiteDifferences[i].getFirstDerivative();
                double c = result.closedForm[i].getFirstDerivative();
                maxRelativeError[i] = FastMath.max(maxRelativeError[i], FastMath.abs(f - c) / FastMath.max(1.0e-10, FastMath.abs(f)));
            }
        }

        for (int i = 0; i < tolerance.length; ++i) {
            Assertions.assertEquals(0.0, maxRelativeError[i], tolerance[i]);
        }

    }

    private NumericalPropagator buildPropagator(final OrbitType orbitType, final PositionAngleType positionAngleType,
                                                final int degree, final AbsoluteDate firing, final double duration,
                                                final DateBasedManeuverTriggers triggers) {

        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        SpacecraftState initialState = buildInitialState(attitudeProvider);

        final double isp      = 318;
        final double f        = 420;
        PropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM");

        double[][] tol = NumericalPropagator.tolerances(0.01, initialState.getOrbit(), orbitType);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 600, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(positionAngleType);
        propagator.setAttitudeProvider(attitudeProvider);
        if (degree > 0) {
            propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                           GravityFieldFactory.getNormalizedProvider(degree, degree)));
        }
        final Maneuver maneuver = new Maneuver(null, triggers, propulsionModel);
        propagator.addForceModel(maneuver);
        propagator.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() { return triggers.getName().concat("-acc"); }
            public double[] getAdditionalState(SpacecraftState state) {
                double[] parameters = Arrays.copyOfRange(maneuver.getParameters(initialState.getDate()), 0, propulsionModel.getParametersDrivers().size());
                return new double[] {
                    propulsionModel.getAcceleration(state, state.getAttitude(), parameters).getNorm()
                };
            }
        });
        propagator.setInitialState(initialState);
        return propagator;

    }

    private SpacecraftState buildInitialState(final AttitudeProvider attitudeProvider) {
        final double mass  = 2500;
        final double a     = 24396159;
        final double e     = 0.72831215;
        final double i     = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv    = 0;

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 1, 1), new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit        orbit    = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                                         FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        return new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);
    }

    private AttitudeProvider buildAttitudeProvider() {
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        return new FrameAlignedProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));
    }

    private class DerivativesSampler implements MultiSatStepHandler {

        final MatricesHarvester harvesterStart;
        final int               indexStart;
        final MatricesHarvester harvesterStop;
        final int               indexStop;
        final MatricesHarvester harvesterMedian;
        final int               indexMedian;
        final MatricesHarvester harvesterDuration;
        final int               indexDuration;
        final OrbitType         orbitType;
        final PositionAngleType positionAngleType;
        final AbsoluteDate      firing;
        final double            duration;
        final double            h;
        final double            samplingtep;
        final List<Entry>       sample;
        boolean                 forward;
        AbsoluteDate            next;

        DerivativesSampler(final MatricesHarvester harvesterStart,    final int indexStart,
                           final MatricesHarvester harvesterStop,     final int indexStop,
                           final MatricesHarvester harvesterMedian,   final int indexMedian,
                           final MatricesHarvester harvesterDuration, final int indexDuration,
                           final OrbitType orbitType, final PositionAngleType positionAngleType,
                           final AbsoluteDate firing, final double duration,
                           final double h, final double samplingtep) {
            this.harvesterStart    = harvesterStart;
            this.indexStart        = indexStart;
            this.harvesterStop     = harvesterStop;
            this.indexStop         = indexStop;
            this.harvesterMedian   = harvesterMedian;
            this.indexMedian       = indexMedian;
            this.harvesterDuration = harvesterDuration;
            this.indexDuration     = indexDuration;
            this.orbitType         = orbitType;
            this.positionAngleType = positionAngleType;
            this.firing            = firing;
            this.duration          = duration;
            this.h                 = h;
            this.samplingtep       = samplingtep;
            this.sample            = new ArrayList<>();
            this.next              = null;
        }

        public void init(final List<SpacecraftState> states0, final AbsoluteDate t) {
            final AbsoluteDate t0 = states0.get(0).getDate();
            if (t.isAfterOrEqualTo(t0)) {
                forward = true;
                next    = t0.shiftedBy(samplingtep);
            } else {
                forward = false;
                next    = t0.shiftedBy(-samplingtep);
            }
        }

        public void handleStep(final List<OrekitStepInterpolator> interpolators) {
            final AbsoluteDate previousDate = interpolators.get(0).getPreviousState().getDate();
            final AbsoluteDate currentDate  = interpolators.get(0).getCurrentState().getDate();
            while ( forward && (next.isAfter(previousDate)  && next.isBeforeOrEqualTo(currentDate)) ||
                   !forward && (next.isBefore(previousDate) && next.isAfterOrEqualTo(currentDate))) {
                // don't sample points where finite differences are in an intermediate state (some before, some after discontinuity)
                if (!(tooClose(next, firing) || tooClose(next, firing.shiftedBy(duration)))) {
                    final Entry entry = new Entry(next.getDate());
                    fill(interpolators, harvesterMedian,   indexMedian,   entry.median);
                    fill(interpolators, harvesterStart,    indexStart,    entry.start);
                    fill(interpolators, harvesterStop,     indexStop,     entry.stop);
                    fill(interpolators, harvesterDuration, indexDuration, entry.duration);
                    sample.add(entry);
                }
                next = next.shiftedBy(forward ? samplingtep : -samplingtep);
            }
        }

        private void fill(final List<OrekitStepInterpolator> interpolators, final MatricesHarvester harvester, final int index, final Result result) {
            if (index >= 0) {
                final double[][] o = new double[9][6];
                for (int i = 0; i < o.length; ++i) {
                    orbitType.mapOrbitToArray(interpolators.get(index + i - 4).getInterpolatedState(next).getOrbit(),
                            positionAngleType, o[i], null);
                }
                final SpacecraftState centralState = interpolators.get(index).getInterpolatedState(next);
                final RealMatrix jacobian = harvester.getParametersJacobian(centralState);
                for (int i = 0; i < result.closedForm.length; ++i) {
                    result.closedForm[i]        = new UnivariateDerivative1(o[4][i], jacobian.getEntry(i, 0));
                    result.finiteDifferences[i] = new UnivariateDerivative1(o[4][i],
                                                                            differential8(o[0][i], o[1][i], o[2][i], o[3][i],
                                                                                          o[5][i], o[6][i], o[7][i], o[8][i],
                                                                                          h));
                }
            }
        }

        private boolean tooClose(final AbsoluteDate date, final AbsoluteDate discontinuity) {
            final double       maxOffset = h * 4;
            return date.shiftedBy(-maxOffset).isBeforeOrEqualTo(discontinuity) &&
                            date.shiftedBy(+maxOffset).isAfterOrEqualTo(discontinuity);
        }

    }

    private static class Entry {
        private Result       start;
        private Result       stop;
        private Result       median;
        private Result       duration;
        Entry(final AbsoluteDate date) {
            this.start    = new Result();
            this.stop     = new Result();
            this.median   = new Result();
            this.duration = new Result();
        }
    }

    private static class Result {
        private UnivariateDerivative1[] finiteDifferences;
        private UnivariateDerivative1[] closedForm;
        Result() {
            this.finiteDifferences = new UnivariateDerivative1[6];
            this.closedForm        = new UnivariateDerivative1[6];
        }
    }

    private double differential8(final double fM4h, final double fM3h, final double fM2h, final double fM1h,
                                 final double fP1h, final double fP2h, final double fP3h, final double fP4h,
                                 final double h) {

        // eight-points finite differences, the remaining error is -h⁸/630 d⁹f/dx⁹ + O(h^¹⁰)
        return (-3 * (fP4h - fM4h) + 32 * (fP3h - fM3h) - 168 * (fP2h - fM2h) + 672 * (fP1h - fM1h)) / (840 * h);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

}
