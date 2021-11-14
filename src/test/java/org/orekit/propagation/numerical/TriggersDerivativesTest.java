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


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class TriggersDerivativesTest {

    @Test
    public void testDerivativeWrtStartTime() {

        final AbsoluteDate firing = new AbsoluteDate(new DateComponents(2004, 1, 2),
                                                     new TimeComponents(4, 15, 34.080),
                                                     TimeScalesFactory.getUTC());

        final List<Propagator> propagators = new ArrayList<>();

        // propagators will be combined using finite differences to compute derivatives
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final double        duration      = 200.0;
        final double        h             = 1.0;
        final double        samplingtep   = 60.0;
        for (int k = -4; k <= 4; ++k) {
            propagators.add(buildPropagator(orbitType, positionAngle, firing, duration, k * h));
        }

        // the central propagator (k = 0) will compute derivatives autonomously using PartialDerivativesEquations and TriggersDerivatives
        final NumericalPropagator autonomous = (NumericalPropagator) propagators.get(4);
        PartialDerivativesEquations pde = new PartialDerivativesEquations("pde", autonomous);
        autonomous.
            getAllForceModels().
            forEach(fm -> fm.getParametersDrivers().
                             stream().
                             filter(d -> d.getName().equals("MAN_0_START")).
                             forEach(d -> d.setSelected(true)));
        autonomous.setInitialState(pde.setInitialJacobians(autonomous.getInitialState()));

        DerivativesSampler sampler = new DerivativesSampler(orbitType, positionAngle, firing, duration, h, samplingtep, pde.getMapper());
        new PropagatorsParallelizer(propagators, sampler).
        propagate(firing.shiftedBy(-120 * samplingtep), firing.shiftedBy(duration + 120 * samplingtep));

        for (final Entry entry : sampler.sample) {
            System.out.format(Locale.US, "%.6f", entry.date.durationFrom(firing));
            for (int i = 0; i < entry.finiteDifferences.length; ++i) {
                System.out.format(Locale.US, " %.9f %.9f", entry.finiteDifferences[i].getFirstDerivative(), entry.direct[i].getFirstDerivative());
            }
            System.out.format(Locale.US, "%n");
        }

    }

    private NumericalPropagator buildPropagator(final OrbitType orbitType,
                                                final PositionAngle positionAngle,
                                                final AbsoluteDate firing, final double duration,
                                                final double shift) {

        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        AttitudeProvider attitudeProvider = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final DateBasedManeuverTriggers triggers =
                        new DateBasedManeuverTriggers("MAN_0", firing.shiftedBy(shift), duration - shift);

        final double isp      = 318;
        final double f        = 420;
        PropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM");

        SpacecraftState initialState = buildInitialState(attitudeProvider);

        double[][] tol = NumericalPropagator.tolerances(0.01, initialState.getOrbit(), orbitType);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(positionAngle);
        propagator.setAttitudeProvider(attitudeProvider);
        propagator.addForceModel(new Maneuver(null, triggers, propulsionModel));
        propagator.setInitialState(initialState);
        return propagator;

    }

    private SpacecraftState buildInitialState(final AttitudeProvider attitudeProvider) {
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        return new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);
    }

    private class DerivativesSampler implements MultiSatStepHandler {

        final OrbitType       orbitType;
        final PositionAngle   positionAngle;
        final AbsoluteDate    firing;
        final double          duration;
        final double          h;
        final double          samplingtep;
        final JacobiansMapper mapper;
        final List<Entry>     sample;
        boolean               forward;
        AbsoluteDate          next;

        DerivativesSampler(final OrbitType orbitType, final PositionAngle positionAngle,
                           final AbsoluteDate firing, final double duration,
                           final double h, final double samplingtep, final JacobiansMapper mapper) {
            this.orbitType     = orbitType;
            this.positionAngle = positionAngle;
            this.firing        = firing;
            this.duration      = duration;
            this.h             = h;
            this.samplingtep   = samplingtep;
            this.mapper        = mapper;
            this.sample        = new ArrayList<>();
            this.next          = null;
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
            final OrekitStepInterpolator autonomous = interpolators.get((interpolators.size() - 1) / 2);
            while ( forward && (next.isAfter(autonomous.getPreviousState())  && next.isBeforeOrEqualTo(autonomous.getCurrentState())) ||
                   !forward && (next.isBefore(autonomous.getPreviousState()) && next.isAfterOrEqualTo(autonomous.getCurrentState()))) {
                if (!(surrounds(interpolators, firing) || surrounds(interpolators, firing.shiftedBy(duration)))) {
                    // don't sample points where finite differences are in an intermediate state (some before, some after discontinuity)
                    final double[][] o = new double[interpolators.size()][6];
                    for (int i = 0; i < o.length; ++i) {
                        orbitType.mapOrbitToArray(interpolators.get(i).getInterpolatedState(next).getOrbit(), positionAngle, o[i], null);
                    }
                    final double[][] jacobian = new double[o[0].length][1];
                    mapper.getParametersJacobian(interpolators.get(4).getInterpolatedState(next), jacobian);
                    UnivariateDerivative1[] finiteDifferences = new UnivariateDerivative1[6];
                    UnivariateDerivative1[] direct            = new UnivariateDerivative1[6];
                    for (int i = 0; i < o[0].length; ++i) {
                        direct[i]            = new UnivariateDerivative1(o[4][i], jacobian[i][0]);
                        finiteDifferences[i] = new UnivariateDerivative1(o[4][i],
                                                                         differential8(o[0][i], o[1][i], o[2][i], o[3][i],
                                                                                       o[5][i], o[6][i], o[7][i], o[8][i],
                                                                                       h));
                    }
                    sample.add(new Entry(next, direct, finiteDifferences));
                }
                next = next.shiftedBy(forward ? samplingtep : -samplingtep);
            }
        }

        private boolean surrounds(final List<OrekitStepInterpolator> interpolators, final AbsoluteDate discontinuity) {
            final AbsoluteDate date      = interpolators.get(0).getCurrentState().getDate(); // all interpolators are at same date
            final double       maxOffset = h * (interpolators.size() - 1) / 2;
            return date.shiftedBy(-maxOffset).isBeforeOrEqualTo(discontinuity) &&
                   date.shiftedBy(+maxOffset).isAfterOrEqualTo(discontinuity);
        }

    }

    private class Entry {
        private AbsoluteDate date;
        private UnivariateDerivative1[]     direct;
        private UnivariateDerivative1[]     finiteDifferences;
        Entry(final AbsoluteDate date, final UnivariateDerivative1[] direct, final UnivariateDerivative1[] finiteDifferences) {
            this.date              = date;
            this.direct            = direct.clone();
            this.finiteDifferences = finiteDifferences.clone();
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
        Utils.setDataRoot("regular-data");
    }

}
