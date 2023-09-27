/* Copyright 2022 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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

import java.io.IOException;
import java.text.ParseException;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTJ2SquaredClosedForm;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.forces.ZeisModel;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class DSSTJ2SquaredClosedFormTest {

    private UnnormalizedSphericalHarmonicsProvider provider;

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
    }

    /**
     * Non regression test for mean element rates using Zeis formulation of J2-squared
     */
    @Test
    public void testGetMeanElementRateZeis() {

        // Spacecraft state
        final Orbit orbit = createOrbit(200000.0, 210000.0);
        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);

        // Force model
        final DSSTForceModel j2Squared = new DSSTJ2SquaredClosedForm(new ZeisModel(), provider);

        // DSST auxiliary elements
        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Mean element rates
        final double[] elements = j2Squared.getMeanElementRate(state, auxiliaryElements, j2Squared.getParameters());
  
        // Verify
        Assertions.assertEquals(0.0,                     elements[0], 1.e-25);
        Assertions.assertEquals(2.5668006482691996E-14,  elements[1], 1.e-25);
        Assertions.assertEquals(7.052226821361117E-14,   elements[2], 1.e-25);
        Assertions.assertEquals(3.6576370779863025E-10,  elements[3], 1.e-25);
        Assertions.assertEquals(-4.3590021280959657E-10, elements[4], 1.e-25);
        Assertions.assertEquals(1.2618917692354564E-8,   elements[5], 1.e-25);
    }

    /**
     * Non regression test for "field" mean element rates using Zeis formulation of J2-squared
     */
    @Test
    public void testFieldGetMeanElementRateZeis() {

        // Field
        final Field<Binary64> field = Binary64Field.getInstance();
        final Binary64 zero = field.getZero();

        // Spacecraft state
        final FieldOrbit<Binary64> orbit = createOrbit(field, 200000.0, 210000.0);
        final FieldSpacecraftState<Binary64> state = new FieldSpacecraftState<>(orbit, zero.add(1000.0));

        // Force model
        final DSSTForceModel j2Squared = new DSSTJ2SquaredClosedForm(new ZeisModel(), provider);

        // DSST auxiliary elements
        final FieldAuxiliaryElements<Binary64> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);

        // Mean element rates
        final Binary64[] elements = j2Squared.getMeanElementRate(state, auxiliaryElements, j2Squared.getParameters(field));
  
        // Verify
        Assertions.assertEquals(0.0,                     elements[0].getReal(), 1.e-25);
        Assertions.assertEquals(2.5668006482691996E-14,  elements[1].getReal(), 1.e-25);
        Assertions.assertEquals(7.052226821361117E-14,   elements[2].getReal(), 1.e-25);
        Assertions.assertEquals(3.6576370779863025E-10,  elements[3].getReal(), 1.e-25);
        Assertions.assertEquals(-4.3590021280959657E-10, elements[4].getReal(), 1.e-25);
        Assertions.assertEquals(1.2618917692354564E-8,   elements[5].getReal(), 1.e-25);

    }

    private void doTestComparisonWithNumerical(final double perigeeAltitude, final double apogeeAltitude,
                                               final double currentDifferenceWithoutJ2Squared,
                                               final double currenDifferenceWithJ2Squared) {

        // Initial spacecraft state
        final Orbit initialOrbit = createOrbit(perigeeAltitude, apogeeAltitude);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, 1000.0);

        // Propagation duration
        final double duration = 2.0 * Constants.JULIAN_DAY;
        final AbsoluteDate end = initialOrbit.getDate().shiftedBy(duration);

        // Create numerical propagator
        final double[][] tolerances = NumericalPropagator.tolerances(1.0, initialOrbit, initialOrbit.getType());
        final ODEIntegrator numIntegrator = new DormandPrince853Integrator(0.001, 300.0, tolerances[0], tolerances[1]);
        final NumericalPropagator numPropagator = new NumericalPropagator(numIntegrator);
        numPropagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), GravityFieldFactory.getNormalizedProvider(provider)));
        numPropagator.setInitialState(initialState);

        // Create DSST propagator (without J2-squared)
        final ODEIntegrator dsstIntegrator = new DormandPrince853Integrator(initialOrbit.getKeplerianPeriod(), 10.0 * initialOrbit.getKeplerianPeriod(), tolerances[0], tolerances[1]);
        final DSSTPropagator dsstPropagatorWithoutJ2Squared = new DSSTPropagator(dsstIntegrator, PropagationType.OSCULATING);
        dsstPropagatorWithoutJ2Squared.addForceModel(new DSSTZonal(provider));
        dsstPropagatorWithoutJ2Squared.setInitialState(initialState, PropagationType.OSCULATING);

        // Create DSST propagator (with J2-squared)
        final DSSTPropagator dsstPropagatorWithJ2Squared = new DSSTPropagator(dsstIntegrator, PropagationType.OSCULATING);
        dsstPropagatorWithJ2Squared.addForceModel(new DSSTZonal(provider));
        dsstPropagatorWithJ2Squared.addForceModel(new DSSTJ2SquaredClosedForm(new ZeisModel(), provider));
        dsstPropagatorWithJ2Squared.setInitialState(initialState, PropagationType.OSCULATING);

        // Propagate
        final Vector3D propagatedNum                 = numPropagator.propagate(end).getPosition();
        final Vector3D propagatedDsstWitoutJ2Squared = dsstPropagatorWithoutJ2Squared.propagate(end).getPosition();
        final Vector3D propagatedDsstWithJ2Squared   = dsstPropagatorWithJ2Squared.propagate(end).getPosition();

        // Differences
        final double differenceWithoutJ2Squared = FastMath.abs(Vector3D.distance(propagatedNum, propagatedDsstWitoutJ2Squared));
        final double differenceWithJ2Squared    = FastMath.abs(Vector3D.distance(propagatedNum, propagatedDsstWithJ2Squared));

        // Verify
        Assertions.assertTrue(differenceWithJ2Squared < differenceWithoutJ2Squared);
        Assertions.assertEquals(0.0, differenceWithoutJ2Squared, currentDifferenceWithoutJ2Squared); // Difference between DSST and numerical without J2-Squared
        Assertions.assertEquals(0.0, differenceWithJ2Squared, currenDifferenceWithJ2Squared); // Difference between DSST and numerical with J2-Squared (not 0.0 due to J2-squared short periods which are not implemented)

    }

    /**
     * The purpose of the test is to verify that the addition the J2-squared terms
     * improve the consistency between the DSST and the numerical propagators.
     * Two DSST configurations are tested: (1) without J2-squared and (2) with J2-squared.
     * The objective is that (2) accuracy compared to the numerical propagator is better than (1)
     */
    @Test
    public void testComparisonWithNumericalVeryLeo() {
        doTestComparisonWithNumerical(200000.0, 210000.0, 20461.1, 6013.1);
    }

    /**
     * The purpose of the test is to verify that the addition the J2-squared terms
     * improve the consistency between the DSST and the numerical propagators.
     * Two DSST configurations are tested: (1) without J2-squared and (2) with J2-squared.
     * The objective is that (2) accuracy compared to the numerical propagator is better than (1)
     */
    @Test
    public void testComparisonWithNumericalLeo() {
        doTestComparisonWithNumerical(650000.0, 680000.0, 15291.5, 4653.4);
    }

    /**
     * The purpose of the test is to verify that the addition the J2-squared terms
     * improve the consistency between the DSST and the numerical propagators.
     * Two DSST configurations are tested: (1) without J2-squared and (2) with J2-squared.
     * The objective is that (2) accuracy compared to the numerical propagator is better than (1)
     */
    @Test
    public void testComparisonWithNumericalMeo() {
        doTestComparisonWithNumerical(5622000.0, 5959000.0, 1595.6, 689.6);
    }

    /**
     * The the computation of the mean element rate derivatives
     */
    @Test
    public void testMeanElementRateDerivatives() {

        // Spacecraft state
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
        final Orbit orbit = createOrbit(650000.0, 680000.0);
        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);

        // Force model
        final DSSTForceModel j2Squared = new DSSTJ2SquaredClosedForm(new ZeisModel(), provider);

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(state, Utils.defaultLaw());

        // Field parameters
        final FieldSpacecraftState<Gradient> dsState = converter.getState(j2Squared);
        final Gradient[] dsParameters                = converter.getParameters(dsState, j2Squared);

        final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

        // Compute state Jacobian using directly the method
        final Gradient[] meanRates = j2Squared.getMeanElementRate(dsState, fieldAuxiliaryElements, dsParameters);
        final double[][] meanElementRatesJacobian = new double[6][6];

        final double[] derivativesA  = meanRates[0].getGradient();
        final double[] derivativesEx = meanRates[1].getGradient();
        final double[] derivativesEy = meanRates[2].getGradient();
        final double[] derivativesHx = meanRates[3].getGradient();
        final double[] derivativesHy = meanRates[4].getGradient();
        final double[] derivativesL  = meanRates[5].getGradient();

        // Update Jacobian with respect to state
        addToRow(derivativesA,  0, meanElementRatesJacobian);
        addToRow(derivativesEx, 1, meanElementRatesJacobian);
        addToRow(derivativesEy, 2, meanElementRatesJacobian);
        addToRow(derivativesHx, 3, meanElementRatesJacobian);
        addToRow(derivativesHy, 4, meanElementRatesJacobian);
        addToRow(derivativesL,  5, meanElementRatesJacobian);

        // Compute reference state Jacobian using finite differences
        double[][] meanElementRatesJacobianRef = new double[6][6];
        double dP = 1.0;
        double[] steps = NumericalPropagator.tolerances(1000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {

            SpacecraftState stateM4 = shiftState(state, orbitType, -4 * steps[i], i);
            double[]  meanRatesM4   = meanElementsRates(stateM4, j2Squared);

            SpacecraftState stateM3 = shiftState(state, orbitType, -3 * steps[i], i);
            double[]  meanRatesM3   = meanElementsRates(stateM3, j2Squared);

            SpacecraftState stateM2 = shiftState(state, orbitType, -2 * steps[i], i);
            double[]  meanRatesM2   = meanElementsRates(stateM2, j2Squared);

            SpacecraftState stateM1 = shiftState(state, orbitType, -1 * steps[i], i);
            double[]  meanRatesM1   = meanElementsRates(stateM1, j2Squared);

            SpacecraftState stateP1 = shiftState(state, orbitType, 1 * steps[i], i);
            double[]  meanRatesP1   = meanElementsRates(stateP1, j2Squared);

            SpacecraftState stateP2 = shiftState(state, orbitType, 2 * steps[i], i);
            double[]  meanRatesP2   = meanElementsRates(stateP2, j2Squared);

            SpacecraftState stateP3 = shiftState(state, orbitType, 3 * steps[i], i);
            double[]  meanRatesP3   = meanElementsRates(stateP3, j2Squared);

            SpacecraftState stateP4 = shiftState(state, orbitType, 4 * steps[i], i);
            double[]  meanRatesP4   = meanElementsRates(stateP4, j2Squared);

            fillJacobianColumn(meanElementRatesJacobianRef, i, orbitType, steps[i],
                               meanRatesM4, meanRatesM3, meanRatesM2, meanRatesM1,
                               meanRatesP1, meanRatesP2, meanRatesP3, meanRatesP4);

        }

        for (int m = 0; m < 6; ++m) {
            for (int n = 0; n < 6; ++n) {
                if (meanElementRatesJacobian[m][n] != 0.0) {
                    double error = FastMath.abs((meanElementRatesJacobian[m][n] - meanElementRatesJacobianRef[m][n]) / meanElementRatesJacobianRef[m][n]);
                    Assertions.assertEquals(0, error, 3.19E-8);
                }
            }
        }

    }

    private Orbit createOrbit(final double perigeeAltitude, final double apogeeAltitude) {

        // Frame and epoch
        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate epoch = new AbsoluteDate(2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // Orbital elements
        final double apogee  = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + apogeeAltitude;
        final double perigee = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + perigeeAltitude;
        final double sma  = 0.5 * (apogee + perigee);
        final double ecc  = 1.0 - perigee / sma;
        final double inc  = FastMath.toRadians(10.0);
        final double raan = FastMath.toRadians(40.0);
        final double aop  = FastMath.toRadians(120.0);
        final double anom = 0.0;
        final PositionAngleType angleType = PositionAngleType.MEAN;

        // Keplerian
        final KeplerianOrbit kep = new KeplerianOrbit(sma, ecc, inc, aop, raan, anom, angleType, frame, epoch, provider.getMu());
        
        // Equinoctial
        return OrbitType.EQUINOCTIAL.convertType(kep);

    }

    private FieldOrbit<Binary64> createOrbit(final Field<Binary64> field, final double perigeeAltitude, final double apogeeAltitude) {

        // Zero
        final Binary64 zero = field.getZero();

        // Frame and epoch
        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate epoch = new AbsoluteDate(2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<Binary64> fieldEpoch = new FieldAbsoluteDate<Binary64>(field, epoch);

        // Orbital elements (very LEO orbit)
        final double apogee  = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + apogeeAltitude;
        final double perigee = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + perigeeAltitude;
        final double sma  = 0.5 * (apogee + perigee);
        final double ecc  = 1.0 - perigee / sma;
        final double inc  = FastMath.toRadians(10.0);
        final double raan = FastMath.toRadians(40.0);
        final double aop  = FastMath.toRadians(120.0);
        final double anom = 0.0;
        final PositionAngleType angleType = PositionAngleType.MEAN;

        // Keplerian
        final FieldKeplerianOrbit<Binary64> fieldKep = new FieldKeplerianOrbit<Binary64>(zero.add(sma), zero.add(ecc), zero.add(inc),
                                                                                           zero.add(aop), zero.add(raan), zero.add(anom),
                                                                                           angleType, frame, fieldEpoch, zero.add(provider.getMu()));
        
        // Equinoctial
        return OrbitType.EQUINOCTIAL.convertType(fieldKep);

    }

    private double[] meanElementsRates(SpacecraftState state, DSSTForceModel force) {
        AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);
        return force.getMeanElementRate(state, auxiliaryElements, force.getParameters());
    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, double h,
                                    double[] M4h, double[] M3h,
                                    double[] M2h, double[] M1h,
                                    double[] P1h, double[] P2h,
                                    double[] P3h, double[] P4h) {
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (P4h[i] - M4h[i]) +
                                    32 * (P3h[i] - M3h[i]) -
                                   168 * (P2h[i] - M2h[i]) +
                                   672 * (P1h[i] - M1h[i])) / (840 * h);
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

    private void addToRow(final double[] derivatives, final int index,
                          final double[][] jacobian) {
        for (int i = 0; i < 6; i++) {
            jacobian[index][i] += derivatives[i];
        }
    }

}
