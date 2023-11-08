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
package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

/** Unit tests for {@link DSSTStateTransitionMatrixGenerator}. */
public class DSSTStateTransitionMatrixGeneratorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

    @Test
    public void testInterrupt() {

        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        double dt = 900;
        double dP = 0.001;

        // first propagation, covering full time range
        DSSTPropagator propagator1 = setUpPropagator(PropagationType.MEAN, dP, provider);
        AbsoluteDate t0 = propagator1.getInitialState().getDate();
        propagator1.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));
        final MatricesHarvester   harvester1   = propagator1.setupMatricesComputation("stm", null, null);
        initializeShortPeriod(harvester1, propagator1);
        final SpacecraftState     state1       = propagator1.propagate(t0.shiftedBy(dt));
        final RealMatrix          stm1         = harvester1.getStateTransitionMatrix(state1);
        final RealMatrix          jacobian1    = harvester1.getParametersJacobian(state1);

        // second propagation, interrupted at mid time
        DSSTPropagator propagator2 = setUpPropagator(PropagationType.MEAN, dP, provider);

        // some additional providers for test coverage
        final DSSTStateTransitionMatrixGenerator dummyStmGenerator =
                        new DSSTStateTransitionMatrixGenerator("dummy-1",
                                                               Collections.emptyList(),
                                                               propagator2.getAttitudeProvider());
        propagator2.addAdditionalDerivativesProvider(dummyStmGenerator);
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState(dummyStmGenerator.getName(), new double[36]),
                                    propagator2.getPropagationType());
        propagator2.addAdditionalDerivativesProvider(new DSSTIntegrableJacobianColumnGenerator(dummyStmGenerator, "dummy-2"));
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState("dummy-2", new double[6]),
                                    propagator2.getPropagationType());
        propagator2.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {
            public String getName() { return "dummy-3"; }
            public int getDimension() { return 1; }
            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[1], null);
            }
        });
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState("dummy-3", new double[1]),
                                    propagator2.getPropagationType());
        propagator2.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));
        final MatricesHarvester   harvester2   = propagator2.setupMatricesComputation("stm", null, null);
        initializeShortPeriod(harvester2, propagator2);
        final SpacecraftState     intermediate = propagator2.propagate(t0.shiftedBy(dt / 2));
        final RealMatrix          stmI         = harvester2.getStateTransitionMatrix(intermediate);
        final RealMatrix          jacobianI    = harvester2.getParametersJacobian(intermediate);

        // intermediate state has really different matrices, they are still building up
        Assertions.assertEquals(0.158497, stmI.subtract(stm1).getNorm1() / stm1.getNorm1(),                1.0e-6);
        Assertions.assertEquals(0.499960, jacobianI.subtract(jacobian1).getNorm1() / jacobian1.getNorm1(), 1.0e-6);

        // restarting propagation where we left it
        final SpacecraftState     state2       = propagator2.propagate(t0.shiftedBy(dt));
        final RealMatrix          stm2         = harvester2.getStateTransitionMatrix(state2);
        final RealMatrix          jacobian2    = harvester2.getParametersJacobian(state2);

        // after completing the two-stage propagation, we get the same matrices
        Assertions.assertEquals(0.0, stm2.subtract(stm1).getNorm1(), 2.0e-11 * stm1.getNorm1());
        Assertions.assertEquals(0.0, jacobian2.subtract(jacobian1).getNorm1(), 7.0e-10 * jacobian1.getNorm1());

    }

    @Test
    public void testPropagationTypesElliptical() throws FileNotFoundException, UnsupportedEncodingException, OrekitException {
        doTestPropagation(PropagationType.MEAN, 7.0e-16);
    }

    @Test
    public void testPropagationTypesEllipticalWithShortPeriod() throws FileNotFoundException, UnsupportedEncodingException, OrekitException {
        doTestPropagation(PropagationType.OSCULATING, 3.3e-4);
    }

    private void doTestPropagation(PropagationType type, double tolerance)
        throws FileNotFoundException, UnsupportedEncodingException {

        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        double dt = 900;
        double dP = 0.001;
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;

        // compute state Jacobian using DSSTStateTransitionMatrixGenerator
        DSSTPropagator propagator = setUpPropagator(type,  dP, provider);
        propagator.setMu(provider.getMu());
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        final AbsoluteDate target = propagator.getInitialState().getDate().shiftedBy(dt);
        PickUpHandler pickUp = new PickUpHandler(propagator, null, null, null);
        propagator.getMultiplexer().add(pickUp);
        propagator.propagate(target);

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        DSSTPropagator propagator2 = setUpPropagator(type, dP, provider);
        propagator2.setMu(provider.getMu());
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, initialState.getOrbit(), orbitType)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2.setInitialState(shiftState(initialState, orbitType, -4 * steps[i], i), type);
            SpacecraftState sM4h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType, -3 * steps[i], i), type);
            SpacecraftState sM3h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType, -2 * steps[i], i), type);
            SpacecraftState sM2h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType, -1 * steps[i], i), type);
            SpacecraftState sM1h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  1 * steps[i], i), type);
            SpacecraftState sP1h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  2 * steps[i], i), type);
            SpacecraftState sP2h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  3 * steps[i], i), type);
            SpacecraftState sP3h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  4 * steps[i], i), type);
            SpacecraftState sP4h = propagator2.propagate(target);
            fillJacobianColumn(dYdY0Ref, i, orbitType, steps[i],
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (stateVector[i] != 0) {
                    double error = FastMath.abs((pickUp.getStm().getEntry(i, j) - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                    Assertions.assertEquals(0, error, tolerance);
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

        return arrayToState(array, orbitType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngleType.MEAN, array[0], array[1]);
          return array;
      }

      private SpacecraftState arrayToState(double[][] array, OrbitType orbitType,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
          EquinoctialOrbit orbit = (EquinoctialOrbit) orbitType.mapArrayToOrbit(array[0], array[1], PositionAngleType.MEAN, date, mu, frame);
          return new SpacecraftState(orbit, attitude);
      }

    private DSSTPropagator setUpPropagator(PropagationType type, double dP, UnnormalizedSphericalHarmonicsProvider provider) {

        final double minStep = 6000.0;
        final double maxStep = 86400.0;

        Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);

        DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                            provider.getMu());

        DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu());

        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());
        final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(initialOrbit);

        // compute state Jacobian using DSSTStateTransitionMatrixGenerator
        double[][] tol = NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
        DSSTPropagator propagator =
            new DSSTPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]), type);
        propagator.addForceModel(srp);
        propagator.addForceModel(tesseral);
        propagator.addForceModel(zonal);
        propagator.addForceModel(moon);

        propagator.setInitialState(new SpacecraftState(orbit), type);
        return propagator;
    }

    private void initializeShortPeriod(final MatricesHarvester harvester, final DSSTPropagator propagator) {
        // Mean orbit
        final SpacecraftState initial = propagator.initialIsOsculating() ?
                       DSSTPropagator.computeMeanState(propagator.getInitialState(), propagator.getAttitudeProvider(), propagator.getAllForceModels()) :
                           propagator.getInitialState();
        ((DSSTHarvester) harvester).initializeFieldShortPeriodTerms(initial); // Initial state is MEAN
    }

    /** Test to ensure correct Jacobian values.
     * In MEAN case, Jacobian should be a 6x6 identity matrix.
     * In OSCULATING cas, first and last lines are compared to reference values.
     */
    @Test
    public void testIssue713() {
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        double dP = 0.001;

        // Test MEAN case
        DSSTPropagator propagatorMEAN = setUpPropagator(PropagationType.MEAN,  dP, provider);
        propagatorMEAN.setMu(provider.getMu());
        SpacecraftState initialStateMEAN = propagatorMEAN.getInitialState();
        DSSTHarvester harvesterMEAN = (DSSTHarvester) propagatorMEAN.setupMatricesComputation("stm", null, null);
        propagatorMEAN.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));
        harvesterMEAN.setReferenceState(initialStateMEAN);
        SpacecraftState finalMEAN = propagatorMEAN.propagate(initialStateMEAN.getDate()); // dummy zero duration propagation, to ensure haverster initialization
        RealMatrix dYdY0MEAN = harvesterMEAN.getStateTransitionMatrix(finalMEAN);
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                Assertions.assertEquals(i == j ? 1.0 : 0.0, dYdY0MEAN.getEntry(i, j), 1e-9);
            }
        }
        RealMatrix dYdPMEAN = harvesterMEAN.getParametersJacobian(finalMEAN);
        Assertions.assertEquals(6, dYdPMEAN.getRowDimension());
        Assertions.assertEquals(1, dYdPMEAN.getColumnDimension());
        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(0.0, dYdPMEAN.getEntry(i, 0), 1e-9);
        }
        Assertions.assertEquals(OrbitType.EQUINOCTIAL, harvesterMEAN.getOrbitType());
        Assertions.assertEquals(PositionAngleType.MEAN, harvesterMEAN.getPositionAngleType());

        // FIXME With the addition of the Extended Semi-analytical Kalman Filter, the following
        //       test doesn't work.
//        // Test OSCULATING case
//        DSSTPropagator propagatorOSC = setUpPropagator(PropagationType.OSCULATING,  dP, provider);
//        propagatorOSC.setMu(provider.getMu());
//        final SpacecraftState initialStateOSC = propagatorOSC.getInitialState();
//        DSSTHarvester harvesterOSC = (DSSTHarvester) propagatorOSC.setupMatricesComputation("stm", null, null);
//        initializeShortPeriod(harvesterOSC, propagatorOSC);
//        propagatorOSC.
//        getAllForceModels().
//        forEach(fm -> fm.
//                getParametersDrivers().
//                stream().
//                filter(d -> d.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)).
//                forEach(d -> d.setSelected(true)));
//        SpacecraftState finalOSC = propagatorOSC.propagate(initialStateOSC.getDate()); // dummy zero duration propagation, to ensure haverster initialization
//        harvesterOSC.setReferenceState(initialStateOSC);
//        RealMatrix dYdY0OSC =   harvesterOSC.getStateTransitionMatrix(finalOSC);
//        final double[] refLine1 = new double[] {1.0000, -5750.3478, 15270.6488, -2707.1208, -2165.0148, -178.3653};
//        final double[] refLine6 = new double[] {0.0000, 0.0035, 0.0013, -0.0005, 0.0005, 1.0000};
//        for (int i = 0; i < 6; ++i) {
//            Assertions.assertEquals(refLine1[i], dYdY0OSC.getEntry(0, i), 1e-4);
//            Assertions.assertEquals(refLine6[i], dYdY0OSC.getEntry(5, i), 1e-4);
//        }
//        RealMatrix dYdPOSC = harvesterOSC.getParametersJacobian(finalOSC);
//        final double[] refCol = new double[] { 0.813996593833, -16.479e-9, -2.901e-9, 7.801e-9, 1.901e-9, -26.769e-9};
//        Assertions.assertEquals(6, dYdPOSC.getRowDimension());
//        Assertions.assertEquals(1, dYdPOSC.getColumnDimension());
//        for (int i = 0; i < 6; ++i) {
//            Assertions.assertEquals(refCol[i], dYdPOSC.getEntry(i, 0), 1e-12);
//        }

    }

}
