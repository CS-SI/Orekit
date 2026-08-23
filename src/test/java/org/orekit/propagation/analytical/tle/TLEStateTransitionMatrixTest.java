/* Copyright 2002-2026 CS GROUP
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

package org.orekit.propagation.analytical.tle;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.hipparchus.CalculusFieldElement;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.drivers.ParameterDriver;
import org.orekit.utils.drivers.ParameterDriversList.DelegatingDriver;

import java.util.List;

public class TLEStateTransitionMatrixTest {

    // build two TLEs in order to test SGP4 and SDP4 algorithms
    private TLE tleGPS;
    private TLE tleSPOT;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        // GPS TLE propagation will use SDP4
        String line1GPS = "1 11783U 80032A   03300.87313441  .00000062  00000-0  10000-3 0  6416";
        String line2GPS = "2 11783  62.0472 164.2367 0320924  39.0039 323.3716  2.03455768173530";
        tleGPS = new TLE(line1GPS, line2GPS);

        // SPOT TLE propagation will use SGP4
        String line1SPOT = "1 22823U 93061A   03339.49496229  .00000173  00000-0  10336-3 0   133";
        String line2SPOT = "2 22823  98.4132 359.2998 0017888 100.4310 259.8872 14.18403464527664";
        tleSPOT = new TLE(line1SPOT, line2SPOT);
    }

    @Test
    public void testPropagationSGP4() {
        doTestStateJacobian(8.37e-10, tleSPOT);
    }

    @Test
    public void testPropagationSDP4() {
        doTestStateJacobian(2.53e-9, tleGPS);
    }

    @Test
    public void testInitialVsBuildSGP4() {
        doTestInitialVsBuild(3.5e-13, tleSPOT);
    }

    @Test
    public void testInitialVsBuildSDP4() {
        doTestInitialVsBuild(3.3e-12, tleGPS);
    }

    @Test
    public void testNullStmName() {
        Assertions.assertThrows(OrekitException.class, () -> {
            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleSPOT);
            propagator.setupMatricesComputation(null, null, null);
        });
    }

    private void doTestStateJacobian(double tolerance, TLE tle) {

        // compute state Jacobian using PartialDerivatives
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitParamsType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        final AbsoluteDate target = initialState.getDate().shiftedBy(initialState.getOrbit().getKeplerianPeriod());
        MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, null);
        RealMatrix dYdY0 = harvester.getStateTransitionMatrix(initialState);
        Assertions.assertNull(dYdY0);
        final SpacecraftState finalState = propagator.propagate(target);
        dYdY0 = harvester.getStateTransitionMatrix(finalState);

        // TLE generation algorithm
        TleGenerationAlgorithm algorithm = new FixedPointTleGenerationAlgorithm(tle);

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        TLEPropagator propagator2;
        double[] steps = ToleranceProvider.getDefaultToleranceProvider(10.).getTolerances(initialState.getOrbit(), OrbitParamsType.CARTESIAN)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, -4 * steps[i], i), tle));
            SpacecraftState sM4h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, -3 * steps[i], i), tle));
            SpacecraftState sM3h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, -2 * steps[i], i), tle));
            SpacecraftState sM2h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, -1 * steps[i], i), tle));
            SpacecraftState sM1h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, +1 * steps[i], i), tle));
            SpacecraftState sP1h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, +2 * steps[i], i), tle));
            SpacecraftState sP2h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, +3 * steps[i], i), tle));
            SpacecraftState sP3h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitParamsType.CARTESIAN, +4 * steps[i], i), tle));
            SpacecraftState sP4h = propagator2.propagate(target);
            fillJacobianColumn(dYdY0Ref, i, OrbitParamsType.CARTESIAN, steps[i],
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (stateVector[i] != 0) {
                    double error = FastMath.abs((dYdY0.getEntry(i, j) - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                    Assertions.assertEquals(0, error, tolerance);
                }
            }
        }
    }

    private void doTestInitialVsBuild(double tolerance, TLE tle) {

        // compute state Jacobian using PartialDerivatives
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitParamsType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, null);
        RealMatrix dY0dB0 = harvester.getStateJacobianVsBuilderParameters(initialState);

        // TLE generation algorithm
        TleGenerationAlgorithm algorithm = new FixedPointTleGenerationAlgorithm(tle);
        final List<DelegatingDriver> drivers = algorithm.getOrbitalParametersDrivers().getDrivers();

        // compute reference state Jacobian using finite differences
        double[][] dY0dB0Ref = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            final ParameterDriver driver = drivers.get(i);
            final double referenceParameter = driver.getValue();
            final double h = 100 * driver.getScale();
            driver.setValue(referenceParameter - 4 * h);
            SpacecraftState sM4h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter - 3 * h);
            SpacecraftState sM3h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter - 2 * h);
            SpacecraftState sM2h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter - 1 * h);
            SpacecraftState sM1h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter + 1 * h);
            SpacecraftState sP1h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter + 2 * h);
            SpacecraftState sP2h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter + 3 * h);
            SpacecraftState sP3h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter + 4 * h);
            SpacecraftState sP4h = TLEPropagator.selectExtrapolator(algorithm.createFromDrivers()).getBaseInitialState();
            driver.setValue(referenceParameter);
            fillJacobianColumn(dY0dB0Ref, i, OrbitParamsType.CARTESIAN, h,
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (stateVector[i] != 0) {
                    double error = FastMath.abs((dY0dB0.getEntry(i, j) - dY0dB0Ref[i][j]) / stateVector[i]) *
                                   drivers.get(j).getScale();
                    Assertions.assertEquals(0, error, tolerance);
                }
            }
        }
    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitParamsType orbitParamsType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        double[] aM4h = stateToArray(sM4h, orbitParamsType)[0];
        double[] aM3h = stateToArray(sM3h, orbitParamsType)[0];
        double[] aM2h = stateToArray(sM2h, orbitParamsType)[0];
        double[] aM1h = stateToArray(sM1h, orbitParamsType)[0];
        double[] aP1h = stateToArray(sP1h, orbitParamsType)[0];
        double[] aP2h = stateToArray(sP2h, orbitParamsType)[0];
        double[] aP3h = stateToArray(sP3h, orbitParamsType)[0];
        double[] aP4h = stateToArray(sP4h, orbitParamsType)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitParamsType orbitParamsType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitParamsType);
        array[0][column] += delta;

        return arrayToState(array, state.getFrame(), state.getDate(),
                            state.getOrbit().getMu(), state.getAttitude());

    }



    private double[][] stateToArray(SpacecraftState state, OrbitParamsType orbitParamsType) {
          double[][] array = new double[2][6];

          orbitParamsType.mapOrbitToArray(state.getOrbit(), PositionAngleType.MEAN, array[0], array[1]);
          return array;
      }


    private SpacecraftState arrayToState(double[][] array,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
        CartesianOrbit orbit = (CartesianOrbit) OrbitParamsType.CARTESIAN.mapArrayToOrbit(array[0], array[1], PositionAngleType.MEAN, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }

    /** Counts how many times the algo is called. */
    private static class CountingTleGenerationAlgorithm extends TleGenerationAlgorithm {

        private final TleGenerationAlgorithm delegate;
        int count;

        CountingTleGenerationAlgorithm(final TleGenerationAlgorithm delegate) {
            super(delegate.getTemplateTLE(), delegate.getFrame(), delegate.getConverter());
            this.delegate = delegate;
        }

        @Override
        public TLE createFromDrivers() {
            count++;
            return delegate.createFromDrivers();
        }

        @Override
        public TLE generate(final SpacecraftState state, final TLE previous) {
            count++;
            return delegate.generate(state, previous);
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldTLE<T> generate(final FieldSpacecraftState<T> state,
                                                                        final FieldTLE<T> previous) {
            count++;
            return delegate.generate(state, previous);
        }

    }

    // makes sure the custom generation algo is actually used in the deep-space path
    @Test
    public void testConfiguredAlgorithmUsedDeepSpace() {
        final CountingTleGenerationAlgorithm counter =
                new CountingTleGenerationAlgorithm(new FixedPointTleGenerationAlgorithm(tleGPS));
        final TLEPropagator propagator =
                TLEPropagator.selectExtrapolator(tleGPS,
                                                  FrameAlignedProvider.of(FramesFactory.getTEME()),
                                                  1000.0,
                                                  DataContext.getDefault().getFrames().getTEME());
        propagator.setTleGenerationAlgorithm(counter);
        final AbsoluteDate target = tleGPS.getDate().shiftedBy(120.0);
        propagator.setupMatricesComputation("stm", null, null);
        propagator.propagate(target);
        // if this fails, DeepSDP4's setter isn't being reached
        Assertions.assertTrue(counter.count > 0);
    }

}
