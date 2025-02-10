/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.data.CommonGnssData;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

public class GnssGradientConverterTest {

    private GNSSPropagator propagator;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final GalileoNavigationMessage goe = new GalileoNavigationMessage(DataContext.getDefault().getTimeScales(), SatelliteSystem.GALILEO);
        goe.setPRN(4);
        goe.setWeek(1024);
        goe.setTime(293400.0);
        goe.setSqrtA(5440.602949142456);
        goe.setDeltaN0(3.7394414770330066E-9);
        goe.setE(2.4088891223073006E-4);
        goe.setI0(0.9531656087278083);
        goe.setIDot(-2.36081262303612E-10);
        goe.setOmega0(-0.36639513583951266);
        goe.setOmegaDot(-5.7695260382035525E-9);
        goe.setPa(-1.6870064194345724);
        goe.setM0(-0.38716557650888);
        goe.setCuc(-8.903443813323975E-7);
        goe.setCus(6.61797821521759E-6);
        goe.setCrc(194.0625);
        goe.setCrs(-18.78125);
        goe.setCic(3.166496753692627E-8);
        goe.setCis(-1.862645149230957E-8);
        propagator = new GNSSPropagatorBuilder(goe, DataContext.getDefault().getFrames()).build();
    }

    @Test
    public void testInitialStateStmNoSelectedParameters() {
        final FieldGnssPropagator<Gradient> gPropagator = new GnssGradientConverter(propagator).getPropagator();
        Assertions.assertEquals(9, gPropagator.getParametersDrivers().size());
        Assertions.assertEquals(0, gPropagator.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count());
        Assertions.assertEquals(6, gPropagator.getInitialState().getOrbit().getA().getFreeParameters());
        checkUnitaryInitialSTM(gPropagator.getInitialState());
    }

    @Test
    public void testInitialStateStmAllParametersSelected() {
        propagator.getOrbitalElements().getParametersDrivers().forEach(p -> p.setSelected(true));
        final FieldGnssPropagator<Gradient> gPropagator = new GnssGradientConverter(propagator).getPropagator();
        Assertions.assertEquals(9, gPropagator.getParametersDrivers().size());
        Assertions.assertEquals( 9, gPropagator.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count());
        Assertions.assertEquals(15, gPropagator.getInitialState().getOrbit().getA().getFreeParameters());
        checkUnitaryInitialSTM(gPropagator.getInitialState());
    }

    @Test
    public void testStmAndJacobian() {
        // Initial GPS orbital elements (Ref: IGS)
        final GPSLegacyNavigationMessage goe = new GPSLegacyNavigationMessage(DataContext.getDefault().getTimeScales(),
                                                                              SatelliteSystem.GPS);
        goe.setPRN(7);
        goe.setWeek(0);
        goe.setTime(288000);
        goe.setSqrtA(5153.599830627441);
        goe.setE(0.012442796607501805);
        goe.setDeltaN0(4.419469802942352E-9);
        goe.setI0(0.9558937988021613);
        goe.setIDot(-2.4608167886110235E-10);
        goe.setOmega0(1.0479401362158658);
        goe.setOmegaDot(-7.967117576712062E-9);
        goe.setPa(-2.4719019944000538);
        goe.setM0(-1.0899023379614294);
        goe.setCuc(4.3995678424835205E-6);
        goe.setCus(1.002475619316101E-5);
        goe.setCrc(183.40625);
        goe.setCrs(87.03125);
        goe.setCic(3.203749656677246E-7);
        goe.setCis(4.0978193283081055E-8);
        GNSSPropagator propagator = goe.getPropagator();

        // we want to compute the partial derivatives with respect to Crs and Crc parameters
        Assertions.assertEquals(9, propagator.getOrbitalElements().getParameters().length);
        propagator.getOrbitalElements().getParameterDriver(CommonGnssData.RADIUS_SINE).setSelected(true);
        propagator.getOrbitalElements().getParameterDriver(CommonGnssData.RADIUS_COSINE).setSelected(true);
        final DoubleArrayDictionary initialJacobianColumns = new DoubleArrayDictionary();
        initialJacobianColumns.put(CommonGnssData.RADIUS_SINE,   new double[6]);
        initialJacobianColumns.put(CommonGnssData.RADIUS_COSINE, new double[6]);
        final MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, initialJacobianColumns);

        // harvester sorts the columns lexicographically, and wraps them as SpanXxx##
        Assertions.assertEquals(2, harvester.getJacobiansColumnsNames().size());
        Assertions.assertEquals("Span" + CommonGnssData.RADIUS_COSINE + "0", harvester.getJacobiansColumnsNames().get(0));
        Assertions.assertEquals("Span" + CommonGnssData.RADIUS_SINE   + "0", harvester.getJacobiansColumnsNames().get(1));

        // propagate orbit
        final SpacecraftState state = propagator.propagate(goe.getDate().shiftedBy(3600.0));

        // check STM against finite differences
        final RealMatrix stm = harvester.getStateTransitionMatrix(state);
        final double hP   = 100000.0;
        final double hV   = 100.0;
        double maxErrorPP = 0.0;
        double maxErrorPV = 0.0;
        double maxErrorVP = 0.0;
        double maxErrorVV = 0.0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                final double h = j < 3 ? hP : hV;
                final double error = differentiate(propagator, state.getDate(), h, i, j) - stm.getEntry(i, j);
                if (i < 3) {
                    if (j < 3) {
                        maxErrorPP = FastMath.max(maxErrorPP, error);
                    } else {
                        maxErrorPV = FastMath.max(maxErrorPV, error);
                    }
                } else {
                    if (j < 3) {
                        maxErrorVP = FastMath.max(maxErrorVP, error);
                    } else {
                        maxErrorVV = FastMath.max(maxErrorVV, error);
                    }
                }
            }
        }
        Assertions.assertEquals(0.0, maxErrorPP, 6.5e-13);
        Assertions.assertEquals(0.0, maxErrorPV, 6.5e-10);
        Assertions.assertEquals(0.0, maxErrorVP, 8.4e-17);
        Assertions.assertEquals(0.0, maxErrorVV, 3.8e-13);

        // check Jacobian against finite differences
        final RealMatrix jacobian = harvester.getParametersJacobian(state);
        final double h = 100000.0;
        double maxErrorP = 0.0;
        double maxErrorV = 0.0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 2; j++) {
                final ToDoubleFunction<GNSSOrbitalElements<?>> getter;
                final BiConsumer<GNSSOrbitalElements<?>, Double> setter;
                if (j == 0) {
                    getter = GNSSOrbitalElements::getCrc;
                    setter = GNSSOrbitalElements::setCrc;
                } else {
                    getter = GNSSOrbitalElements::getCrs;
                    setter = GNSSOrbitalElements::setCrs;
                }
                final double error = differentiate(propagator, state.getDate(), getter, setter, h, i) -
                                     jacobian.getEntry(i, j);
                if (i < 3) {
                    maxErrorP = FastMath.max(maxErrorP, error);
                } else {
                    maxErrorV = FastMath.max(maxErrorV, error);
                }
            }
        }
        Assertions.assertEquals(0.0, maxErrorP, 6.9e-14);
        Assertions.assertEquals(0.0, maxErrorV, 1.7e-17);

    }

    private void checkUnitaryInitialSTM(final FieldSpacecraftState<Gradient> initialState) {
        final FieldPVCoordinates<Gradient> pv0 = initialState.getPVCoordinates();
        checkUnitary(pv0.getPosition().getX().getGradient(), 0, 4.0e-13, 2.0e-8);
        checkUnitary(pv0.getPosition().getY().getGradient(), 1, 4.0e-13, 2.0e-8);
        checkUnitary(pv0.getPosition().getZ().getGradient(), 2, 4.0e-13, 2.0e-8);
        checkUnitary(pv0.getVelocity().getX().getGradient(), 3, 2.0e-12, 2.0e-12);
        checkUnitary(pv0.getVelocity().getY().getGradient(), 4, 2.0e-12, 2.0e-12);
        checkUnitary(pv0.getVelocity().getZ().getGradient(), 5, 2.0e-12, 2.0e-12);
    }

    private void checkUnitary(final double[] gradient, final int index,
                              final double tolDiag, final double tolNonDiag) {
        // beware! we intentionally check only the first 6 parameters
        // the next ones correspond to non-Keplerian parameters,
        // derivatives are NOT unitary for these extra parameters
        for (int i = 0; i < 6; i++) {
            if (i == index) {
                // diagonal element
                Assertions.assertEquals(1.0, gradient[i], tolDiag);
            } else {
                // non-diagonal element
                Assertions.assertEquals(0.0, gradient[i], tolNonDiag);
            }
        }
    }

    private double differentiate(final GNSSPropagator basePropagator, final AbsoluteDate target,
                                 final double step, final int outIndex, final int inIndex) {

        // function that converts a shift in one element of initial state (i.e. Px, Py, Pz, Vx, Vy, Vz)
        // into one element of propagated state
        final UnivariateFunction f = h -> {

            // get initial state
            final SpacecraftState original = basePropagator.getInitialState();

            // shift element at specified index
            final double[] in = new double[6];
            OrbitType.CARTESIAN.mapOrbitToArray(original.getOrbit(), PositionAngleType.MEAN, in, null);
            in[inIndex] += h;

            // build shifted initial state
            final SpacecraftState shiftedState =
                new SpacecraftState(OrbitType.CARTESIAN.mapArrayToOrbit(in, null, PositionAngleType.MEAN,
                                                                        original.getDate(),
                                                                        original.getOrbit().getMu(), original.getFrame()),
                                    original.getAttitude(), original.getMass());

            // build shifted propagator
            final GNSSPropagator shiftedPropagator = new GNSSPropagator(shiftedState,
                                                                        basePropagator.getOrbitalElements(),
                                                                        basePropagator.getECEF(),
                                                                        basePropagator.getAttitudeProvider(),
                                                                        shiftedState.getMass());

            // propagated state
            final SpacecraftState outState = shiftedPropagator.propagate(target);

            // return desired coordinate
            final double[] out = new double[6];
            OrbitType.CARTESIAN.mapOrbitToArray(outState.getOrbit(), PositionAngleType.MEAN, out, null);
            return out[outIndex];

        };

        final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, step);
        final UnivariateDifferentiableFunction df = differentiator.differentiate(f);

        return df.value(new UnivariateDerivative1(0.0, 1.0)).getFirstDerivative();

    }

    private double differentiate(final GNSSPropagator basePropagator, final AbsoluteDate target,
                                 final ToDoubleFunction<GNSSOrbitalElements<?>> getter,
                                 final BiConsumer<GNSSOrbitalElements<?>, Double> setter,
                                 final double step, final int outIndex) {

        // function that converts a shift in one element of initial state (i.e Px, Py, Pz, Vx, Vy, Vz)
        // into one element of propagated state
        final UnivariateFunction f = h -> {

            // get initial parameter value
            final double initialValue = getter.applyAsDouble(basePropagator.getOrbitalElements());

            // shift parameter
            setter.accept(basePropagator.getOrbitalElements(), initialValue + h);

            // propagated state
            final SpacecraftState outState = basePropagator.propagate(target);

            // reset parameter
            setter.accept(basePropagator.getOrbitalElements(), initialValue);

            // return desired coordinate
            final double[] out = new double[6];
            OrbitType.CARTESIAN.mapOrbitToArray(outState.getOrbit(), PositionAngleType.MEAN, out, null);
            return out[outIndex];

        };

        final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, step);
        final UnivariateDifferentiableFunction df = differentiator.differentiate(f);

        return df.value(new UnivariateDerivative1(0.0, 1.0)).getFirstDerivative();

    }

}
