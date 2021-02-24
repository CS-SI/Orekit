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
package org.orekit.propagation.analytical.tle;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class TLEPartialDerivativesTest {
    
    
    // build two TLEs in order to test SGP4 and SDP4 algorithms
    private TLE tleGPS;
    
    private TLE tleSPOT;

    @Test
    public void testStateJacobianSDP4() throws FileNotFoundException, UnsupportedEncodingException {
        doTestStateJacobian(4.63e-7, tleGPS);
    }

    @Test
    public void testStateJacobianSGP4() throws FileNotFoundException, UnsupportedEncodingException {
        doTestStateJacobian(4.71e-2, tleSPOT);
    }

    @Test
    public void testBStarDerivatives() throws ParseException, IOException {
        doTestParametersDerivatives(TLE.B_STAR,
                                    5.88e-4,
                                    tleGPS,
                                    PropagationType.MEAN);
    }
   
    @Test(expected=OrekitException.class)
    public void testNotInitialized() {
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";       
        TLE tle = new TLE(line1, line2);

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        new TLEPartialDerivativesEquations("partials", propagator).getMapper();
     }
    
    @Test(expected=OrekitException.class)
    public void testTooSmallDimension() {
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";       
        TLE tle = new TLE(line1, line2);

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[5][6], new double[6][2]);
     }

    @Test(expected=OrekitException.class)
    public void testTooLargeDimension() {
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";       
        TLE tle = new TLE(line1, line2);

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[8][6], new double[6][2]);
     }
    
    @Test(expected=OrekitException.class)
    public void testMismatchedDimensions() {
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";       
        TLE tle = new TLE(line1, line2);

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[6][6], new double[7][2]);
     }
    
    @Test
    public void testWrongParametersDimension() {
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";       
        TLE tle = new TLE(line1, line2);

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations("partials", propagator);
        try {
            partials.setInitialJacobians(propagator.getInitialState(),
                                         new double[6][6], new double[6][3]);
            partials.computeDerivatives(propagator.getInitialState(), new double[6]);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH,
                                oe.getSpecifier());
        }
    }
    
    private void doTestStateJacobian(double tolerance, TLE tle)
        throws FileNotFoundException, UnsupportedEncodingException {
               
        double dt = Constants.JULIAN_DAY;

        // compute state Jacobian using PartialDerivatives
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = propagator.getInitialState();
        final AbsoluteDate target = initialState.getDate().shiftedBy(dt);
        KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagator.getInitialState().getOrbit());
        TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations("partials", propagator);
        final SpacecraftState initState = partials.setInitialJacobians(propagator.getInitialState());
        final double[] stateVector = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(initState.getOrbit(), PositionAngle.MEAN, stateVector, null);
        final TLEJacobiansMapper mapper = partials.getMapper();
        double[][] dYdY0 =  new double[TLEJacobiansMapper.STATE_DIMENSION][TLEJacobiansMapper.STATE_DIMENSION];
        propagator.setInitialState(initState);
        mapper.analyticalDerivatives(propagator.propagate(target));
        mapper.getStateJacobian(initState, dYdY0);

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        TLEPropagator propagator2;
        double[] steps = NumericalPropagator.tolerances(10, orbit, OrbitType.KEPLERIAN)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, -4 * steps[i], i), tle));
            SpacecraftState sM4h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, -3 * steps[i], i), tle));
            SpacecraftState sM3h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, -2 * steps[i], i), tle));
            SpacecraftState sM2h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, -1 * steps[i], i), tle));
            SpacecraftState sM1h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, +1 * steps[i], i), tle));
            SpacecraftState sP1h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, +2 * steps[i], i), tle));
            SpacecraftState sP2h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, +3 * steps[i], i), tle));
            SpacecraftState sP3h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(TLE.stateToTLE(shiftState(initialState, OrbitType.KEPLERIAN, +4 * steps[i], i), tle));
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

    private void doTestParametersDerivatives(String parameterName, double tolerance, TLE tle,
                                             PropagationType type) {

        double dt = Constants.JULIAN_DAY;       
        // compute state Jacobian using PartialDerivatives
        ParameterDriversList bound = new ParameterDriversList();

        for (final ParameterDriver driver : tle.getParametersDrivers()) {
            if (driver.getName().equals(parameterName)) {
                driver.setSelected(true);
                bound.add(driver);
            } else {
                driver.setSelected(false);
            }
        }
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = propagator.getInitialState();
        final AbsoluteDate target = initialState.getDate().shiftedBy(dt);
        TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations("partials", propagator);
        final SpacecraftState endState = partials.setInitialJacobians(propagator.propagate(target));
        final double[] stateVector = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngle.MEAN, stateVector, null);
        final TLEJacobiansMapper mapper = partials.getMapper();
        double[][] dYdP =  new double[TLEJacobiansMapper.STATE_DIMENSION][mapper.getParameters()];
        mapper.analyticalDerivatives(endState);
        mapper.getParametersJacobian(initialState, dYdP);

        // compute reference Jacobian using finite differences
        
        OrbitType orbitType = OrbitType.KEPLERIAN;
        TLEPropagator propagator2 = TLEPropagator.selectExtrapolator(tle);
        final KeplerianOrbit initialOrbit = (KeplerianOrbit) propagator2.getInitialState().getOrbit();
        double[][] dYdPRef = new double[6][1];

        ParameterDriver selected = bound.getDrivers().get(0);
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();
        selected.setValue(p0 - 4 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sM4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 - 3 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sM3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 - 2 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sM2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 - 1 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sM1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 + 1 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sP1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 + 2 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sP2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 + 3 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sP3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        selected.setValue(p0 + 4 * h);
        propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                   initialState.getFrame(), initialState.getDate(),
                                                   initialState.getMu(), // the mu may have been reset above
                                                   initialState.getAttitude()));
        SpacecraftState sP4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
        fillJacobianColumn(dYdPRef, 0, orbitType, h,
                           sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
       
        for (int i = 0; i < 6; ++i) {
            Assert.assertEquals(dYdPRef[i][0], dYdP[i][0], FastMath.abs(tolerance));
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

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngle.MEAN, array[0], array[1]);
          return array;
      }


    private SpacecraftState arrayToState(double[][] array, 
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
        KeplerianOrbit orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.mapArrayToOrbit(array[0], array[1], PositionAngle.MEAN, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }
      

    @Before
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

}
