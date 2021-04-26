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
package org.orekit.propagation.analytical;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class KeplerianPartialDerivativesTest {
    
    private KeplerianOrbit initialOrbit;
    
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");    
        
     // orbit
        Frame inertialFrame = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);
        double mu = Constants.EIGEN5C_EARTH_MU;
        double a = 24396159;                     // semi major axis in meters
        double e = 0.72831215;                   // eccentricity
        double i = FastMath.toRadians(7);        // inclination
        double omega = FastMath.toRadians(180);  // perigee argument
        double raan = FastMath.toRadians(261);   // right ascension of ascending node
        double trueAnomaly = 0;                  // true anomaly
        initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, trueAnomaly, PositionAngle.TRUE, inertialFrame, initialDate, mu);

    }
    
    @Test
    public void testStateJacobian() throws FileNotFoundException, UnsupportedEncodingException {
        doTestStateJacobian(5.86E-6, initialOrbit);
    }

    @Test(expected=OrekitException.class)
    public void testNotInitialized() {
        
        // propagator
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        
        new KeplerianPartialDerivativesEquations("partials", propagator).getMapper();
     }
    
    @Test(expected=OrekitException.class)
    public void testTooSmallDimension() {
        
     // propagator
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        KeplerianPartialDerivativesEquations partials = new KeplerianPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[5][6]);
       
     }

    @Test(expected=OrekitException.class)
    public void testTooLargeDimension() {
        
     // propagator
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        KeplerianPartialDerivativesEquations partials = new KeplerianPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[8][6]);
       
     }
    
    private void doTestStateJacobian(double tolerance, KeplerianOrbit initialOrbit)
                    throws FileNotFoundException, UnsupportedEncodingException {
                           
                    double dt = Constants.JULIAN_DAY;

                    // compute state Jacobian using PartialDerivatives
                    
                    // propagator
                    KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
                    final SpacecraftState initialState = propagator.getInitialState();
                    final AbsoluteDate target = initialState.getDate().shiftedBy(dt);
                    KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagator.getInitialState().getOrbit());
                    KeplerianPartialDerivativesEquations partials = new KeplerianPartialDerivativesEquations("partials", propagator);
                    final SpacecraftState initState = partials.setInitialJacobians(propagator.getInitialState());
                    final double[] stateVector = new double[6];
                    OrbitType.KEPLERIAN.mapOrbitToArray(initState.getOrbit(), PositionAngle.TRUE, stateVector, null);
                    final KeplerianJacobiansMapper mapper = partials.getMapper();
                    double[][] dYdY0 =  new double[KeplerianJacobiansMapper.STATE_DIMENSION][KeplerianJacobiansMapper.STATE_DIMENSION];
                    propagator.resetInitialState(initState);
                    mapper.analyticalDerivatives(propagator.propagate(target));
                    mapper.getStateJacobian(initState, dYdY0);

                    // compute reference state Jacobian using finite differences
                    double[][] dYdY0Ref = new double[6][6];
                    KeplerianPropagator propagator2;
                    double[] steps = NumericalPropagator.tolerances(10, orbit, OrbitType.KEPLERIAN)[0];
                    for (int i = 0; i < 6; ++i) {
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, -4 * steps[i], i).getOrbit());
                        SpacecraftState sM4h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, -3 * steps[i], i).getOrbit());
                        SpacecraftState sM3h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, -2 * steps[i], i).getOrbit());
                        SpacecraftState sM2h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, -1 * steps[i], i).getOrbit());
                        SpacecraftState sM1h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, +1 * steps[i], i).getOrbit());
                        SpacecraftState sP1h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, +2 * steps[i], i).getOrbit());
                        SpacecraftState sP2h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, +3 * steps[i], i).getOrbit());
                        SpacecraftState sP3h = propagator2.propagate(target);
                        propagator2 = new KeplerianPropagator(shiftState(initialState, OrbitType.KEPLERIAN, +4 * steps[i], i).getOrbit());
                        SpacecraftState sP4h = propagator2.propagate(target);
                        fillJacobianColumn(dYdY0Ref, i, OrbitType.KEPLERIAN, steps[i],
                                           sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
                    }

                    for (int i = 0; i < 6; ++i) {
                        for (int j = 0; j < 6; ++j) {
                            if (stateVector[i] != 0) {
                                double error = FastMath.abs((dYdY0[i][j] - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                                Assert.assertEquals(0, error, tolerance);
                            }
                        }
                    }
                }
    
    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        double[] aM4h = stateToArray(sM4h, orbitType)[0];
        double[] aM3h = stateToArray(sM3h, orbitType)[0];
        double[] aM2h = stateToArray(sM2h, orbitType)[0];
        double[] aM1h = stateToArray(sM1h, orbitType)[0];
        double[] aP1h = stateToArray(sP1h, orbitType)[0];
        double[] aP2h = stateToArray(sP2h, orbitType)[0];
        double[] aP3h = stateToArray(sP3h, orbitType)[0];
        double[] aP4h = stateToArray(sP4h, orbitType)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType);
        array[0][column] += delta;

        return arrayToState(array, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }
    
    
    
    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngle.TRUE, array[0], array[1]);
          return array;
      }


    private SpacecraftState arrayToState(double[][] array, 
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
        KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.mapArrayToOrbit(array[0], array[1], PositionAngle.TRUE, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }
    

}
