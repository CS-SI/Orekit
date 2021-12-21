/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.numerical;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
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
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.TriggerDateJacobianColumnGenerator;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class TriggersDerivativesTest {

    @Test
    public void testInterrupt() {
        final AbsoluteDate firing = new AbsoluteDate(new DateComponents(2004, 1, 2),
                                                     new TimeComponents(4, 15, 34.080),
                                                     TimeScalesFactory.getUTC());
        final double duration = 200.0;

        // first propagation, covering the maneuver
        DateBasedManeuverTriggers triggers1 = new DateBasedManeuverTriggers("MAN_0", firing, duration);
        final NumericalPropagator propagator1  = buildPropagator(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, 20,
                                                                 firing, duration, triggers1);
        propagator1.
        getAllForceModels().
        forEach(fm -> fm.
                          getParametersDrivers().
                          stream().
                          filter(d -> d.getName().equals("MAN_0_START") ||
                                      d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                          forEach(d -> d.setSelected(true)));
        final MatricesHarvester   harvester1   = propagator1.setupMatricesComputation("stm", null, null);
        final SpacecraftState     state1       = propagator1.propagate(firing.shiftedBy(2 * duration));
        final RealMatrix          stm1         = harvester1.getStateTransitionMatrix(state1);
        final RealMatrix          jacobian1    = harvester1.getParametersJacobian(state1);

        // second propagation, interrupted during maneuver
        DateBasedManeuverTriggers triggers2 = new DateBasedManeuverTriggers("MAN_0", firing, duration);
                final NumericalPropagator propagator2  = buildPropagator(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, 20,
                                                                 firing, duration, triggers2);
        propagator2.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals("MAN_0_START") ||
                       d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));

         // some additional providers for test coverage
        final StateTransitionMatrixGenerator dummyStmGenerator =
                        new StateTransitionMatrixGenerator("dummy-1",
                                                           Collections.emptyList(),
                                                           propagator2.getAttitudeProvider());
        propagator2.addAdditionalDerivativesProvider(dummyStmGenerator);
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState(dummyStmGenerator.getName(), new double[36]));
        propagator2.addAdditionalDerivativesProvider(new IntegrableJacobianColumnGenerator(dummyStmGenerator, "dummy-2"));
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState("dummy-2", new double[6]));
        propagator2.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {
            public String getName() { return "dummy-3"; }
            public int getDimension() { return 1; }
            public double[] derivatives(SpacecraftState s) { return new double[1]; }
        });
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState("dummy-3", new double[1]));
        propagator2.addAdditionalStateProvider(new TriggerDateJacobianColumnGenerator(dummyStmGenerator.getName(), "dummy-4", true, null, 1.0e-6));
        propagator2.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() { return "dummy-5"; }
            public double[] getAdditionalState(SpacecraftState s) { return new double[1]; }
        });
        final MatricesHarvester   harvester2   = propagator2.setupMatricesComputation("stm", null, null);
        final SpacecraftState     intermediate = propagator2.propagate(firing.shiftedBy(0.5 * duration));
        final RealMatrix          stmI         = harvester2.getStateTransitionMatrix(intermediate);
        final RealMatrix          jacobianI    = harvester2.getParametersJacobian(intermediate);

        // intermediate state has really different matrices, they are still building up
        Assert.assertEquals(0.1253, stmI.subtract(stm1).getNorm1() / stm1.getNorm1(),                1.0e-4);
        Assert.assertEquals(0.0172, jacobianI.subtract(jacobian1).getNorm1() / jacobian1.getNorm1(), 1.0e-4);

        // restarting propagation where we left it
        final SpacecraftState     state2       = propagator2.propagate(firing.shiftedBy(2 * duration));
        final RealMatrix          stm2         = harvester2.getStateTransitionMatrix(state2);
        final RealMatrix          jacobian2    = harvester2.getParametersJacobian(state2);

        // after completing the two-stage propagation, we get the same matrices
        Assert.assertEquals(0.0, stm2.subtract(stm1).getNorm1(), 1.0e-13 * stm1.getNorm1());
        Assert.assertEquals(0.0, jacobian2.subtract(jacobian1).getNorm1(), 2.0e-14 * jacobian1.getNorm1());

    }

    @Test
    public void testDerivativeWrtStartTimeCartesianForward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.CARTESIAN, true,
                                         0.022, 0.012, 0.012, 0.013, 0.012, 0.021);
    }

    @Test
    public void testDerivativeWrtStartTimeKeplerianForward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.KEPLERIAN, true,
                                         0.012, 0.011, 0.011, 0.012, 0.011, 0.017);
    }

    @Test
    public void testDerivativeWrtStartTimeCartesianBackward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.CARTESIAN, false,
                                         0.022, 0.012, 0.012, 0.013, 0.012, 0.021);
    }

    @Test
    public void testDerivativeWrtStartTimeKeplerianBackward() {
        doTestDerivativeWrtStartStopTime(true, OrbitType.KEPLERIAN, false,
                                         0.012, 0.011, 0.011, 0.012, 0.011, 0.017);
    }

    @Test
    public void testDerivativeWrtStopTimeCartesianForward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.CARTESIAN, true,
                                         0.00033, 0.00045, 0.00040, 0.00022, 0.00020, 0.00010);
    }

    @Test
    public void testDerivativeWrtStopTimeKeplerianForward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.KEPLERIAN, true,
                                         0.0011, 0.00020, 0.00002, 0.00082, 0.00008, 0.00019);
    }

    @Test
    public void testDerivativeWrtStopTimeCartesianBackward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.CARTESIAN, false,
                                         0.00033, 0.00045, 0.00040, 0.00022, 0.00020, 0.00010);
    }

    @Test
    public void testDerivativeWrtStopTimeKeplerianBackward() {
        doTestDerivativeWrtStartStopTime(false, OrbitType.KEPLERIAN, false,
                                         0.0011, 0.00020, 0.00002, 0.00082, 0.00008, 0.00019);
    }

    @Test
    public void testDerivativeWrtMedianCartesianForward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.CARTESIAN, true,
                                          0.0011, 0.00020, 0.00002, 0.00082, 0.00008, 0.00019);
    }

    @Test
    public void testDerivativeWrtMedianKeplerianForward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.KEPLERIAN, true,
                                          0.0011, 0.00020, 0.00002, 0.00082, 0.00008, 0.00019);
    }

    @Test
    public void testDerivativeWrtMedianCartesianBackward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.CARTESIAN, false,
                                          0.0011, 0.00020, 0.00002, 0.00082, 0.00008, 0.00019);
    }

    @Test
    public void testDerivativeWrtMedianKeplerianBackward() {
        doTestDerivativeWrtMedianDuration(true, OrbitType.KEPLERIAN, false,
                                          0.0011, 0.00020, 0.00002, 0.00082, 0.00008, 0.00019);
    }

    @Test
    public void testDerivativeWrtDurationCartesianForward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.CARTESIAN, true,
                                          0.00540, 0.00540, 0.00540, 0.00540, 0.00540, 0.00540);
    }

    @Test
    public void testDerivativeWrtDurationKeplerianForward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.KEPLERIAN, true,
                                          0.00577, 0.00540, 0.00540, 0.00570, 0.00541, 0.00543);
    }

    @Test
    public void testDerivativeWrtDurationCartesianBackward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.CARTESIAN, false,
                                          0.00540, 0.00540, 0.00540, 0.00540, 0.00540, 0.00540);
    }

    @Test
    public void testDerivativeWrtDurationKeplerianBackward() {
        doTestDerivativeWrtMedianDuration(false, OrbitType.KEPLERIAN, false,
                                          0.00577, 0.00540, 0.00540, 0.00570, 0.00541, 0.00543);
    }

    private void doTestDerivativeWrtStartStopTime(final boolean start, final OrbitType orbitType, final boolean forward,
                                                  final double...tolerance) {

        final PositionAngle positionAngle = PositionAngle.TRUE;
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
            propagators.add(buildPropagator(orbitType, positionAngle, degree, firing, duration, trigger));
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
                                     new DerivativesSampler(harvester, 4, null, -1, null, -1, null, -1, orbitType, positionAngle,
                                                            firing, duration, h, samplingtep) :
                                     new DerivativesSampler(null, -1, harvester, 4, null, -1, null, -1, orbitType, positionAngle,
                                                            firing, duration, h, samplingtep);

        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, sampler);
        if (forward) {
            parallelizer.propagate(firing.shiftedBy(-30 * samplingtep), firing.shiftedBy(duration + 300 * samplingtep));
        } else {
            parallelizer.propagate(firing.shiftedBy(duration + 30 * samplingtep), firing.shiftedBy(-300 * samplingtep));
        }

        double[] maxRelativeError = new double[tolerance.length];
        for (final Entry entry : sampler.sample) {
            for (int i = 0; i < 6; ++i) {
                double f = (start ? entry.startFiniteDifferences[i] : entry.stopFiniteDifferences[i]).getFirstDerivative();
                double c = (start ? entry.startClosedForm[i]        : entry.stopClosedForm[i]).getFirstDerivative();
                maxRelativeError[i] = FastMath.max(maxRelativeError[i], FastMath.abs(f - c) / FastMath.max(1.0e-10, FastMath.abs(f)));
            }
        }

        analyzeSample(sampler, orbitType, firing, forward, start ? "start time" : "stop time",
                      true, null);

        for (int i = 0; i < tolerance.length; ++i) {
            Assert.assertEquals(0.0, maxRelativeError[i], tolerance[i]);
        }

    }

    private void doTestDerivativeWrtMedianDuration(final boolean median, final OrbitType orbitType, final boolean forward,
                                                       final double...tolerance) {

        final PositionAngle positionAngle = PositionAngle.TRUE;
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
            propagators.add(buildPropagator(orbitType, positionAngle, degree, firing, duration, triggers));
        }
        for (int k = -4; k <= 4; ++k) {
            final DateBasedManeuverTriggers triggers =
                            new DateBasedManeuverTriggers("MAN_1", firing.shiftedBy(k * h), duration - k * h);
            propagators.add(buildPropagator(orbitType, positionAngle, degree, firing, duration, triggers));
        }
        for (int k = -4; k <= 4; ++k) {
            final DateBasedManeuverTriggers triggers =
                            new DateBasedManeuverTriggers("MAN_2", firing, duration + k * h);
            propagators.add(buildPropagator(orbitType, positionAngle, degree, firing, duration, triggers));
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
                                                            orbitType, positionAngle,
                                                            firing, duration, h, samplingtep) :
                                     new DerivativesSampler(harvesterStart, 13, harvesterStop, 22, null, -1, harvester, 4,
                                                            orbitType, positionAngle,
                                                            firing, duration, h, samplingtep);

        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, sampler);
        if (forward) {
            parallelizer.propagate(firing.shiftedBy(-30 * samplingtep), firing.shiftedBy(duration + 300 * samplingtep));
        } else {
            parallelizer.propagate(firing.shiftedBy(duration + 30 * samplingtep), firing.shiftedBy(-300 * samplingtep));
        }

        double[] maxRelativeError = new double[tolerance.length];
        for (final Entry entry : sampler.sample) {
            for (int i = 0; i < 6; ++i) {
                double f = (median ? entry.medianFiniteDifferences[i] : entry.durationFiniteDifferences[i]).getFirstDerivative();
                double c = (median ? entry.medianClosedForm[i]        : entry.durationClosedForm[i]).getFirstDerivative();
                maxRelativeError[i] = FastMath.max(maxRelativeError[i], FastMath.abs(f - c) / FastMath.max(1.0e-10, FastMath.abs(f)));
            }
        }

        analyzeSample(sampler, orbitType, firing, forward,
                      median ? "median time" : "duration",
                      true, null);

        for (int i = 0; i < tolerance.length; ++i) {
            Assert.assertEquals(0.0, maxRelativeError[i], tolerance[i]);
        }

    }

    private void analyzeSample(final DerivativesSampler sampler, final OrbitType orbitType, final AbsoluteDate firing,
                               final boolean forward, final String param, final boolean plot, final String outputDir) {
        if (!plot) {
            return;
        }
        final ProcessBuilder pb = new ProcessBuilder("gnuplot").
                        redirectOutput(ProcessBuilder.Redirect.INHERIT).
                        redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.environment().remove("XDG_SESSION_TYPE");
        Process gnuplot;
        try {
            gnuplot = pb.start();
            try (PrintStream out = new PrintStream(gnuplot.getOutputStream(), false, StandardCharsets.UTF_8.name())) {
                final String fileName;
                if (outputDir == null) {
                    fileName = null;
                    out.format(Locale.US, "set terminal qt size %d, %d title 'complex plotter'%n", 1000, 1000);
                } else {
                    fileName = new File(outputDir,
                                        "triggers-partials-" + param.replace(' ', '-') + '-' + orbitType +
                                        (forward ? "-forward" : "-backward") + ".png").
                               getAbsolutePath();
                    out.format(Locale.US, "set terminal pngcairo size %d, %d%n", 1000, 1000);
                    out.format(Locale.US, "set output '%s'%n", fileName);
                }
                out.format(Locale.US, "set offset graph 0.05, 0.05, 0.05, 0.05%n");
                out.format(Locale.US, "set view map scale 1%n");
                out.format(Locale.US, "set xlabel 't - t_{start}'%n");
                if (orbitType == OrbitType.CARTESIAN) {
                    out.format(Locale.US, "set ylabel 'd\\{X,Y,Z\\}/dt (m/s)'%n");
                    out.format(Locale.US, "set key bottom left%n");
                } else {
                    out.format(Locale.US, "set ylabel 'da/dt (m/s)'%n");
                    out.format(Locale.US, "set key top left%n");
                }
                out.format(Locale.US, "set title 'derivatives of %s state %s'%n", orbitType, forward ? "forward" : "backward");
                out.format(Locale.US, "$data <<EOD%n");
                for (final Entry entry : sampler.sample) {
                    out.format(Locale.US, "%.6f", entry.date.durationFrom(firing));
                    for (int i = 0; i < entry.medianFiniteDifferences.length; ++i) {
                        out.format(Locale.US, " %.12f %.12f %.12f %.12f %.12f %.12f",
                                   entry.medianFiniteDifferences[i] == null ? 0.0 : entry.medianFiniteDifferences[i].getFirstDerivative(),
                                   entry.medianClosedForm[i]        == null ? 0.0 : entry.medianClosedForm[i].getFirstDerivative(),
                                   entry.startFiniteDifferences[i]  == null ? 0.0 : entry.startFiniteDifferences[i].getFirstDerivative(),
                                   entry.startClosedForm[i]         == null ? 0.0 : entry.startClosedForm[i].getFirstDerivative(),
                                   entry.stopFiniteDifferences[i]   == null ? 0.0 : entry.stopFiniteDifferences[i].getFirstDerivative(),
                                   entry.stopClosedForm[i]          == null ? 0.0 : entry.stopClosedForm[i].getFirstDerivative());
                    }
                    out.format(Locale.US, "%n");
                }
                out.format(Locale.US, "EOD%n");
                final String obs = orbitType == OrbitType.CARTESIAN ? "dX" : "da";
                out.format(Locale.US, "plot ");
                if (sampler.indexMedian >= 0) {
                    out.format(Locale.US, "$data using 1:($2-$3) with points      title '%s/dt_m error'%s%n",
                               obs, (sampler.indexStart > 0 || sampler.indexStop > 0) ? ",\\" : "");
                }
                if (sampler.indexStart >= 0) {
                    out.format(Locale.US, "$data using 1:($4-$5) with points      title '%s/dt_{start} error'%s%n",
                               obs, (sampler.indexStop > 0) ? ",\\" : "");
                }
                if (sampler.indexStop >= 0) {
                    out.format(Locale.US, "$data using 1:($6-$7) with points      title '%s/dt_{stop} error'%n",
                               obs);
                }
                if (fileName == null) {
                    out.format(Locale.US, "pause mouse close%n");
                } else {
                    System.out.format(Locale.US, "output written to %s%n", fileName);
                }
            }
        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }

    }

    private NumericalPropagator buildPropagator(final OrbitType orbitType, final PositionAngle positionAngle,
                                                final int degree, final AbsoluteDate firing, final double duration,
                                                final DateBasedManeuverTriggers triggers) {

        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        SpacecraftState initialState = buildInitialState(attitudeProvider);

        final double isp      = 318;
        final double f        = 420;
        PropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM");

        double[][] tol = NumericalPropagator.tolerances(0.01, initialState.getOrbit(), orbitType);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(positionAngle);
        propagator.setAttitudeProvider(attitudeProvider);
        if (degree > 0) {
            propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                           GravityFieldFactory.getNormalizedProvider(degree, degree)));
        }
        propagator.addForceModel(new Maneuver(null, triggers, propulsionModel));
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
        final Orbit        orbit    = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                                         FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        return new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);
    }

    private AttitudeProvider buildAttitudeProvider() {
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        return new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));
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
        final PositionAngle     positionAngle;
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
                           final OrbitType orbitType, final PositionAngle positionAngle,
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
            this.positionAngle     = positionAngle;
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

                    final UnivariateDerivative1[] startFiniteDifferences    = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] startClosedForm           = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] stopFiniteDifferences     = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] stopClosedForm            = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] medianFiniteDifferences   = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] medianClosedForm          = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] durationFiniteDifferences = new UnivariateDerivative1[6];
                    final UnivariateDerivative1[] durationClosedForm        = new UnivariateDerivative1[6];

                    fill(interpolators, harvesterMedian,   indexMedian,   medianClosedForm,   medianFiniteDifferences);
                    fill(interpolators, harvesterStart,    indexStart,    startClosedForm,    startFiniteDifferences);
                    fill(interpolators, harvesterStop,     indexStop,     stopClosedForm,     stopFiniteDifferences);
                    fill(interpolators, harvesterDuration, indexDuration, durationClosedForm, durationFiniteDifferences);

                    sample.add(new Entry(next.getDate(),
                                         startFiniteDifferences,    startClosedForm,
                                         stopFiniteDifferences,     stopClosedForm,
                                         medianFiniteDifferences,   medianClosedForm,
                                         durationFiniteDifferences, durationClosedForm));

                }
                next = next.shiftedBy(forward ? samplingtep : -samplingtep);
            }
        }

        private void fill(final List<OrekitStepInterpolator> interpolators,
                          final MatricesHarvester harvester, final int index,
                          final UnivariateDerivative1[] closedForm,
                          final UnivariateDerivative1[] finiteDifferences) {
            if (index >= 0) {
                final double[][] o = new double[9][6];
                for (int i = 0; i < o.length; ++i) {
                    orbitType.mapOrbitToArray(interpolators.get(index + i - 4).getInterpolatedState(next).getOrbit(),
                                              positionAngle, o[i], null);
                }
                final RealMatrix jacobian = harvester.getParametersJacobian(interpolators.get(index).getInterpolatedState(next));
                for (int i = 0; i < closedForm.length; ++i) {
                    closedForm[i]        = new UnivariateDerivative1(o[4][i], jacobian.getEntry(i, 0));
                    finiteDifferences[i] = new UnivariateDerivative1(o[4][i],
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

    private class Entry {
        private AbsoluteDate date;
        private UnivariateDerivative1[]     startFiniteDifferences;
        private UnivariateDerivative1[]     startClosedForm;
        private UnivariateDerivative1[]     stopFiniteDifferences;
        private UnivariateDerivative1[]     stopClosedForm;
        private UnivariateDerivative1[]     medianFiniteDifferences;
        private UnivariateDerivative1[]     medianClosedForm;
        private UnivariateDerivative1[]     durationFiniteDifferences;
        private UnivariateDerivative1[]     durationClosedForm;
        Entry(final AbsoluteDate date,
              final UnivariateDerivative1[] startFiniteDifferences,
              final UnivariateDerivative1[] startClosedForm,
              final UnivariateDerivative1[] stopFiniteDifferences,
              final UnivariateDerivative1[] stopClosedForm,
              final UnivariateDerivative1[] medianFiniteDifferences,
              final UnivariateDerivative1[] medianClosedForm,
              final UnivariateDerivative1[] durationFiniteDifferences,
              final UnivariateDerivative1[] durationClosedForm) {
            this.date                      = date;
            this.startFiniteDifferences    = startFiniteDifferences.clone();
            this.startClosedForm           = startClosedForm.clone();
            this.stopFiniteDifferences     = stopFiniteDifferences.clone();
            this.stopClosedForm            = stopClosedForm.clone();
            this.medianFiniteDifferences   = medianFiniteDifferences.clone();
            this.medianClosedForm          = medianClosedForm.clone();
            this.durationFiniteDifferences = durationFiniteDifferences.clone();
            this.durationClosedForm        = durationClosedForm.clone();
        }
    }

    private double differential8(final double fM4h, final double fM3h, final double fM2h, final double fM1h,
                                 final double fP1h, final double fP2h, final double fP3h, final double fP4h,
                                 final double h) {

        // eight-points finite differences, the remaining error is -h⁸/630 d⁹f/dx⁹ + O(h^¹⁰)
        return (-3 * (fP4h - fM4h) + 32 * (fP3h - fM3h) - 168 * (fP2h - fM2h) + 672 * (fP1h - fM1h)) / (840 * h);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

}
