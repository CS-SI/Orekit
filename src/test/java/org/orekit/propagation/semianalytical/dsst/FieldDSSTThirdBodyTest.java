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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class FieldDSSTThirdBodyTest {

    private static final double eps  = 3.5e-25;

    @Test
    public void testGetMeanElementRate() {
        doTestGetMeanElementRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field)  {

        final T zero = field.getZero();

        final Frame earthFrame = FramesFactory.getEME2000();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 07, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());

        final double mu = 3.986004415E14;
        // a    = 42163393.0 m
        // ex =  -0.25925449177598586
        // ey =  -0.06946703170551687
        // hx =   0.15995912655021305
        // hy =  -0.5969755874197339
        // lM   = 15.47576793123677 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(4.2163393E7),
                                                                zero.add(-0.25925449177598586),
                                                                zero.add(-0.06946703170551687),
                                                                zero.add(0.15995912655021305),
                                                                zero.add(-0.5969755874197339),
                                                                zero.add(15.47576793123677),
                                                                PositionAngleType.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                zero.add(mu));

        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, mass);

        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), mu);

        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);

        // Force model parameters
        final T[] parameters = moon.getParameters(field, state.getDate());
        // Initialize force model
        moon.initializeShortPeriodTerms(auxiliaryElements,
                        PropagationType.MEAN, parameters);

        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);

        final T[] daidt = moon.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(0.0,                    elements[0].getReal(), eps);
        Assertions.assertEquals(4.346622384804537E-10,  elements[1].getReal(), eps);
        Assertions.assertEquals(7.293879548440941E-10,  elements[2].getReal(), eps);
        Assertions.assertEquals(7.465699631747887E-11,  elements[3].getReal(), eps);
        Assertions.assertEquals(3.9170221137233836E-10, elements[4].getReal(), eps);
        Assertions.assertEquals(-3.178319341840074E-10, elements[5].getReal(), eps);

    }

    @Test
    public void testShortPeriodTerms() {
        doTestShortPeriodTerms(Binary64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends CalculusFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field) {
        final T zero = field.getZero();

        final FieldSpacecraftState<T> meanState = getGEOState(field);

        final DSSTForceModel moon    = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu().getReal());

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(moon);

        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanState.getOrbit(), 1);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();

        for (final DSSTForceModel force : forces) {
            force.registerAttitudeProvider(null);
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, force.getParameters(field, meanState.getDate())));
            force.updateShortPeriodTerms(force.getParametersAllValues(field), meanState);
        }

        T[] y = MathArrays.buildArray(field, 6);
        Arrays.fill(y, zero);
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }

        Assertions.assertEquals(-413.20633326933154,    y[0].getReal(), 1.0e-15);
        Assertions.assertEquals(-1.8060137920197483E-5, y[1].getReal(), 1.0e-20);
        Assertions.assertEquals(-2.8416367511811057E-5, y[2].getReal(), 1.4e-20);
        Assertions.assertEquals(-2.791424363476855E-6,  y[3].getReal(), 1.0e-21);
        Assertions.assertEquals(1.8817187527805853E-6,  y[4].getReal(), 1.0e-21);
        Assertions.assertEquals(-3.423664701811889E-5,  y[5].getReal(), 1.0e-20);

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testShortPeriodTermsStateDerivatives() {

        // Initial spacecraft state
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());

        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngleType.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);

        final OrbitType orbitType = OrbitType.EQUINOCTIAL;

        final SpacecraftState meanState = new SpacecraftState(orbit);

        // Force model
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        final DSSTForceModel moon    = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu());
        final DSSTForceModel sun     = new DSSTThirdBody(CelestialBodyFactory.getSun(),  meanState.getMu());
        forces.add(moon);
        forces.add(sun);

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(meanState, Utils.defaultLaw());

        // Compute state Jacobian using directly the method
        final double[][] shortPeriodJacobian = new double[6][6];
        for (DSSTForceModel force : forces) {

            // Field parameters
            final FieldSpacecraftState<Gradient> dsState = converter.getState(force);
            
            final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

            // Array for short period terms
            final Gradient[] shortPeriod = new Gradient[6];
            final Gradient zero = dsState.getA().getField().getZero();
            Arrays.fill(shortPeriod, zero);

            final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<Gradient>>();
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(fieldAuxiliaryElements, PropagationType.OSCULATING,
                                    converter.getParametersAtStateDate(dsState, force)));
            force.updateShortPeriodTerms(converter.getParameters(dsState, force), dsState);
            
            for (final FieldShortPeriodTerms<Gradient> spt : shortPeriodTerms) {
                final Gradient[] spVariation = spt.value(dsState.getOrbit());
                for (int i = 0; i < spVariation .length; i++) {
                    shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
                }
            }

            final double[] derivativesASP  = shortPeriod[0].getGradient();
            final double[] derivativesExSP = shortPeriod[1].getGradient();
            final double[] derivativesEySP = shortPeriod[2].getGradient();
            final double[] derivativesHxSP = shortPeriod[3].getGradient();
            final double[] derivativesHySP = shortPeriod[4].getGradient();
            final double[] derivativesLSP  = shortPeriod[5].getGradient();

            // Update Jacobian with respect to state
            addToRow(derivativesASP,  0, shortPeriodJacobian);
            addToRow(derivativesExSP, 1, shortPeriodJacobian);
            addToRow(derivativesEySP, 2, shortPeriodJacobian);
            addToRow(derivativesHxSP, 3, shortPeriodJacobian);
            addToRow(derivativesHySP, 4, shortPeriodJacobian);
            addToRow(derivativesLSP,  5, shortPeriodJacobian);

        }

        // Compute reference state Jacobian using finite differences
        double[][] shortPeriodJacobianRef = new double[6][6];
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {

            SpacecraftState stateM4 = shiftState(meanState, orbitType, -4 * steps[i], i);
            double[]  shortPeriodM4 = computeShortPeriodTerms(stateM4, forces);

            SpacecraftState stateM3 = shiftState(meanState, orbitType, -3 * steps[i], i);
            double[]  shortPeriodM3 = computeShortPeriodTerms(stateM3, forces);

            SpacecraftState stateM2 = shiftState(meanState, orbitType, -2 * steps[i], i);
            double[]  shortPeriodM2 = computeShortPeriodTerms(stateM2, forces);

            SpacecraftState stateM1 = shiftState(meanState, orbitType, -1 * steps[i], i);
            double[]  shortPeriodM1 = computeShortPeriodTerms(stateM1, forces);

            SpacecraftState stateP1 = shiftState(meanState, orbitType, 1 * steps[i], i);
            double[]  shortPeriodP1 = computeShortPeriodTerms(stateP1, forces);

            SpacecraftState stateP2 = shiftState(meanState, orbitType, 2 * steps[i], i);
            double[]  shortPeriodP2 = computeShortPeriodTerms(stateP2, forces);

            SpacecraftState stateP3 = shiftState(meanState, orbitType, 3 * steps[i], i);
            double[]  shortPeriodP3 = computeShortPeriodTerms(stateP3, forces);

            SpacecraftState stateP4 = shiftState(meanState, orbitType, 4 * steps[i], i);
            double[]  shortPeriodP4 = computeShortPeriodTerms(stateP4, forces);

            fillJacobianColumn(shortPeriodJacobianRef, i, orbitType, steps[i],
                               shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                               shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);

        }

        for (int m = 0; m < 6; ++m) {
            for (int n = 0; n < 6; ++n) {
                double error = FastMath.abs((shortPeriodJacobian[m][n] - shortPeriodJacobianRef[m][n]) / shortPeriodJacobianRef[m][n]);
                Assertions.assertEquals(0, error, 7.7e-11);
            }
        }

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testShortPeriodTermsMuParametersDerivatives() {

        // Initial spacecraft state
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());

        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngleType.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);

        final OrbitType orbitType = OrbitType.EQUINOCTIAL;

        final SpacecraftState meanState = new SpacecraftState(orbit);

        // Force model
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        final DSSTForceModel moon    = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu());
        final DSSTForceModel sun     = new DSSTThirdBody(CelestialBodyFactory.getSun(),  meanState.getMu());
        forces.add(moon);
        forces.add(sun);

        for (final DSSTForceModel forceModel : forces) {
            for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                driver.setValue(driver.getReferenceValue());
                driver.setSelected(driver.getName().equals(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT));
            }
        }

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(meanState, Utils.defaultLaw());

        final double[][] shortPeriodJacobian = new double[6][1];

        for (final DSSTForceModel forceModel : forces) {

            // Field parameters
            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
          
            final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

            // Zero
            final Gradient zero = dsState.getDate().getField().getZero();

            // Compute Jacobian using directly the method
            final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<Gradient>>();
            shortPeriodTerms.addAll(forceModel.initializeShortPeriodTerms(fieldAuxiliaryElements, PropagationType.OSCULATING,
                                    converter.getParametersAtStateDate(dsState, forceModel)));
            forceModel.updateShortPeriodTerms(converter.getParameters(dsState, forceModel), dsState);
            final Gradient[] shortPeriod = new Gradient[6];
            Arrays.fill(shortPeriod, zero);
            for (final FieldShortPeriodTerms<Gradient> spt : shortPeriodTerms) {
                final Gradient[] spVariation = spt.value(dsState.getOrbit());
                for (int i = 0; i < spVariation .length; i++) {
                    shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
                }
            }

            final double[] derivativesASP  = shortPeriod[0].getGradient();
            final double[] derivativesExSP = shortPeriod[1].getGradient();
            final double[] derivativesEySP = shortPeriod[2].getGradient();
            final double[] derivativesHxSP = shortPeriod[3].getGradient();
            final double[] derivativesHySP = shortPeriod[4].getGradient();
            final double[] derivativesLSP  = shortPeriod[5].getGradient();

            int index = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {
                    shortPeriodJacobian[0][0] += derivativesASP[index];
                    shortPeriodJacobian[1][0] += derivativesExSP[index];
                    shortPeriodJacobian[2][0] += derivativesEySP[index];
                    shortPeriodJacobian[3][0] += derivativesHxSP[index];
                    shortPeriodJacobian[4][0] += derivativesHySP[index];
                    shortPeriodJacobian[5][0] += derivativesLSP[index];
                    ++index;
                }
            }
        }

        // Compute reference Jacobian using finite differences
        double[][] shortPeriodJacobianRef = new double[6][1];
        ParameterDriversList bound = new ParameterDriversList();
        for (final DSSTForceModel forceModel : forces) {
            for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.getName().equals(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)) {
                    driver.setSelected(true);
                    bound.add(driver);
                } else {
                    driver.setSelected(false);
                }
            }
        }

        ParameterDriver selected = bound.getDrivers().get(0);
        double[] parameters = new double[1];
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();
      
        selected.setValue(p0 - 4 * h);
        final double[] shortPeriodM4 = computeShortPeriodTerms(meanState, forces);
  
        selected.setValue(p0 - 3 * h);
        final double[] shortPeriodM3 = computeShortPeriodTerms(meanState, forces);
      
        selected.setValue(p0 - 2 * h);
        final double[] shortPeriodM2 = computeShortPeriodTerms(meanState, forces);
      
        selected.setValue(p0 - 1 * h);
        final double[] shortPeriodM1 = computeShortPeriodTerms(meanState, forces);
      
        selected.setValue(p0 + 1 * h);
        final double[] shortPeriodP1 = computeShortPeriodTerms(meanState, forces);
      
        selected.setValue(p0 + 2 * h);
        final double[] shortPeriodP2 = computeShortPeriodTerms(meanState, forces);
      
        selected.setValue(p0 + 3 * h);
        parameters[0] = selected.getValue();
        final double[] shortPeriodP3 = computeShortPeriodTerms(meanState, forces);
      
        selected.setValue(p0 + 4 * h, null);
        final double[] shortPeriodP4 = computeShortPeriodTerms(meanState, forces);

        fillJacobianColumn(shortPeriodJacobianRef, 0, orbitType, h,
                           shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                           shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);

        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(shortPeriodJacobianRef[i][0],
                                shortPeriodJacobian[i][0],
                                FastMath.abs(shortPeriodJacobianRef[i][0] * 2.5e-11));
        }

    }

    private <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> getGEOState(final Field<T> field) {

        final T zero = field.getZero();
        // No shadow at this date
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                                      TimeScalesFactory.getUTC());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(42164000),
                                                                zero.add(10e-3),
                                                                zero.add(10e-3),
                                                                zero.add(FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3)),
                                                                zero.add(FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3)),
                                                                zero.add(0.1),
                                                                PositionAngleType.TRUE,
                                                                FramesFactory.getEME2000(),
                                                                initDate,
                                                                zero.add(3.986004415E14));
        return new FieldSpacecraftState<>(orbit);
    }

    private double[] computeShortPeriodTerms(SpacecraftState state,
                                             Collection<DSSTForceModel> forces) {

        AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        for (final DSSTForceModel force : forces) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(auxiliaryElements, PropagationType.OSCULATING, force.getParameters(state.getDate())));
            force.updateShortPeriodTerms(force.getParametersAllValues(), state);
        }

        double[] shortPeriod = new double[6];
        for (ShortPeriodTerms spt : shortPeriodTerms) {
            double[] spVariation = spt.value(state.getOrbit());
            for (int i = 0; i < spVariation.length; i++) {
                shortPeriod[i] += spVariation[i];
            }
        }

        return shortPeriod;

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

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     * @param jacobian Jacobian of short period terms with respect to state
     */
    private void addToRow(final double[] derivatives, final int index,
                          final double[][] jacobian) {

        for (int i = 0; i < 6; i++) {
            jacobian[index][i] += derivatives[i];
        }

    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data");
    }

}
