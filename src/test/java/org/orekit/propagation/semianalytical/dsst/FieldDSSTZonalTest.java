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
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
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
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
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
import java.util.List;

public class FieldDSSTZonalTest {

    @Test
    public void testGetMeanElementRate() {
        doTestGetMeanElementRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field) {

        final T zero = field.getZero();

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);

        final Frame earthFrame = FramesFactory.getEME2000();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // a  = 26559890 m
        // ey = 0.0041543085910249414
        // ex = 2.719455286199036E-4
        // hy = 0.3960084733107685
        // hx = -0.3412974060023717
        // lM = 8.566537840341699 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(2.655989E7),
                                                                zero.add(2.719455286199036E-4),
                                                                zero.add(0.0041543085910249414),
                                                                zero.add(-0.3412974060023717),
                                                                zero.add(0.3960084733107685),
                                                                zero.add(8.566537840341699),
                                                                PositionAngleType.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                zero.add(3.986004415E14));

        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, mass);

        final DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);

        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);

        // Force model parameters
        final T[] parameters = zonal.getParameters(field, state.getDate());
        // Initialize force model
        zonal.initializeShortPeriodTerms(auxiliaryElements,
                         PropagationType.MEAN, parameters);

        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);

        final T[] daidt = zonal.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(0.0,                     elements[0].getReal(), 1.e-25);
        Assertions.assertEquals(1.3909396722346468E-11,  elements[1].getReal(), 3.e-26);
        Assertions.assertEquals(-2.0275977261372793E-13, elements[2].getReal(), 3.e-27);
        Assertions.assertEquals(3.087141512018238E-9,    elements[3].getReal(), 1.e-24);
        Assertions.assertEquals(2.6606317310148797E-9,   elements[4].getReal(), 4.e-24);
        Assertions.assertEquals(-3.659904725206694E-9,   elements[5].getReal(), 1.e-24);

    }

    @Test
    public void testShortPeriodTerms() {
        doTestShortPeriodTerms(Binary64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
	private <T extends CalculusFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field) {
	    final T zero = field.getZero();
	
	    final FieldSpacecraftState<T> meanState = getGEOState(field);
	    
	    final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
	    final DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
	
	    //Create the auxiliary object
	    final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanState.getOrbit(), 1);
	
	    // Set the force models
	    final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();
	
	    zonal.registerAttitudeProvider(null);
	    shortPeriodTerms.addAll(zonal.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, zonal.getParameters(field)));
	    zonal.updateShortPeriodTerms(zonal.getParametersAllValues(field), meanState);
	
	    T[] y = MathArrays.buildArray(field, 6);
	    Arrays.fill(y, zero);
	    for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
	        final T[] shortPeriodic = spt.value(meanState.getOrbit());
	        for (int i = 0; i < shortPeriodic.length; i++) {
	            y[i] = y[i].add(shortPeriodic[i]);
	        }
	    }
	    
        Assertions.assertEquals(35.005618980090276,     y[0].getReal(), 1.e-15);
        Assertions.assertEquals(3.75891551882889E-5,    y[1].getReal(), 1.e-20);
        Assertions.assertEquals(3.929119925563796E-6,   y[2].getReal(), 1.e-21);
        Assertions.assertEquals(-1.1781951949124315E-8, y[3].getReal(), 1.e-24);
        Assertions.assertEquals(-3.2134924513679615E-8, y[4].getReal(), 1.e-24);
        Assertions.assertEquals(-1.1607392915997098E-6, y[5].getReal(), 1.e-21);
    }

    @Test
    public void testIssue625() {
        doTestIssue625(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue625(final Field<T> field) {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        final T zero = field.getZero();

        final Frame earthFrame = FramesFactory.getEME2000();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // a  = 2.655989E6 m
        // ex = 2.719455286199036E-4
        // ey = 0.0041543085910249414
        // hx = -0.3412974060023717
        // hy = 0.3960084733107685
        // lM = 8.566537840341699 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(2.655989E6),
                                                                zero.add(2.719455286199036E-4),
                                                                zero.add(0.0041543085910249414),
                                                                zero.add(-0.3412974060023717),
                                                                zero.add(0.3960084733107685),
                                                                zero.add(8.566537840341699),
                                                                PositionAngleType.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                zero.add(3.986004415E14));

        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, zero.add(1000.0));

        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);

        // Central Body geopotential 32x32
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(32, 32);

        // Zonal force model
        final DSSTZonal zonal = new DSSTZonal(provider, 32, 4, 65);
        zonal.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, zonal.getParameters(field, state.getDate()));

        // Zonal force model with default constructor
        final DSSTZonal zonalDefault = new DSSTZonal(provider);
        zonalDefault.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, zonalDefault.getParameters(field, state.getDate()));

        // Compute mean element rate for the zonal force model
        final T[] elements = zonal.getMeanElementRate(state, auxiliaryElements, zonal.getParameters(field, state.getDate()));

        // Compute mean element rate for the "default" zonal force model
        final T[] elementsDefault = zonalDefault.getMeanElementRate(state, auxiliaryElements, zonalDefault.getParameters(field, state.getDate()));

        // Verify
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(elements[i].getReal(), elementsDefault[i].getReal(), Double.MIN_VALUE);
        }

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
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final DSSTForceModel zonal   = new DSSTZonal(provider, 2, 1, 5);

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(meanState, Utils.defaultLaw());

        // Field parameters
        final FieldSpacecraftState<Gradient> dsState = converter.getState(zonal);
        
        final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

        // Zero
        final Gradient zero = dsState.getDate().getField().getZero();

        // Compute state Jacobian using directly the method
        final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<Gradient>>();
        shortPeriodTerms.addAll(zonal.initializeShortPeriodTerms(fieldAuxiliaryElements, PropagationType.OSCULATING,
                                converter.getParametersAtStateDate(dsState, zonal)));
        zonal.updateShortPeriodTerms(converter.getParameters(dsState, zonal), dsState);
        final Gradient[] shortPeriod = new Gradient[6];
        Arrays.fill(shortPeriod, zero);
        for (final FieldShortPeriodTerms<Gradient> spt : shortPeriodTerms) {
            final Gradient[] spVariation = spt.value(dsState.getOrbit());
            for (int i = 0; i < spVariation .length; i++) {
                shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
            }
        }

        final double[][] shortPeriodJacobian = new double[6][6];

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

        // Compute reference state Jacobian using finite differences
        double[][] shortPeriodJacobianRef = new double[6][6];
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {

            SpacecraftState stateM4 = shiftState(meanState, orbitType, -4 * steps[i], i);
            double[]  shortPeriodM4 = computeShortPeriodTerms(stateM4, zonal);

            SpacecraftState stateM3 = shiftState(meanState, orbitType, -3 * steps[i], i);
            double[]  shortPeriodM3 = computeShortPeriodTerms(stateM3, zonal);

            SpacecraftState stateM2 = shiftState(meanState, orbitType, -2 * steps[i], i);
            double[]  shortPeriodM2 = computeShortPeriodTerms(stateM2, zonal);

            SpacecraftState stateM1 = shiftState(meanState, orbitType, -1 * steps[i], i);
            double[]  shortPeriodM1 = computeShortPeriodTerms(stateM1, zonal);

            SpacecraftState stateP1 = shiftState(meanState, orbitType, 1 * steps[i], i);
            double[]  shortPeriodP1 = computeShortPeriodTerms(stateP1, zonal);

            SpacecraftState stateP2 = shiftState(meanState, orbitType, 2 * steps[i], i);
            double[]  shortPeriodP2 = computeShortPeriodTerms(stateP2, zonal);

            SpacecraftState stateP3 = shiftState(meanState, orbitType, 3 * steps[i], i);
            double[]  shortPeriodP3 = computeShortPeriodTerms(stateP3, zonal);

            SpacecraftState stateP4 = shiftState(meanState, orbitType, 4 * steps[i], i);
            double[]  shortPeriodP4 = computeShortPeriodTerms(stateP4, zonal);

            fillJacobianColumn(shortPeriodJacobianRef, i, orbitType, steps[i],
                               shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                               shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);

        }

        for (int m = 0; m < 6; ++m) {
            for (int n = 0; n < 6; ++n) {
                double error = FastMath.abs((shortPeriodJacobian[m][n] - shortPeriodJacobianRef[m][n]) / shortPeriodJacobianRef[m][n]);
                Assertions.assertEquals(0, error, 9.6e-10);
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
        // State vector used for validation
        final double[] stateVector = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(meanState.getOrbit(), PositionAngleType.MEAN, stateVector, null);

        // Force model
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final DSSTForceModel zonal   = new DSSTZonal(provider, 2, 1, 5);

        for (final ParameterDriver driver : zonal.getParametersDrivers()) {
            driver.setValue(driver.getReferenceValue());
            driver.setSelected(driver.getName().equals(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT));
        }

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(meanState, Utils.defaultLaw());

        // Field parameters
        final FieldSpacecraftState<Gradient> dsState = converter.getState(zonal);
      
        final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

        // Zero
        final Gradient zero = dsState.getDate().getField().getZero();

        // Compute Jacobian using directly the method
        final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<Gradient>>();
        shortPeriodTerms.addAll(zonal.initializeShortPeriodTerms(fieldAuxiliaryElements, PropagationType.OSCULATING,
                                converter.getParametersAtStateDate(dsState, zonal)));
        zonal.updateShortPeriodTerms(converter.getParameters(dsState, zonal), dsState);
        final Gradient[] shortPeriod = new Gradient[6];
        Arrays.fill(shortPeriod, zero);
        for (final FieldShortPeriodTerms<Gradient> spt : shortPeriodTerms) {
            final Gradient[] spVariation = spt.value(dsState.getOrbit());
            for (int i = 0; i < spVariation .length; i++) {
                shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
            }
        }

        final double[][] shortPeriodJacobian = new double[6][1];

        final double[] derivativesASP  = shortPeriod[0].getGradient();
        final double[] derivativesExSP = shortPeriod[1].getGradient();
        final double[] derivativesEySP = shortPeriod[2].getGradient();
        final double[] derivativesHxSP = shortPeriod[3].getGradient();
        final double[] derivativesHySP = shortPeriod[4].getGradient();
        final double[] derivativesLSP  = shortPeriod[5].getGradient();

        int index = converter.getFreeStateParameters();
        for (ParameterDriver driver : zonal.getParametersDrivers()) {
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

        // Compute reference Jacobian using finite differences
        double[][] shortPeriodJacobianRef = new double[6][1];
        ParameterDriversList bound = new ParameterDriversList();
        for (final ParameterDriver driver : zonal.getParametersDrivers()) {
            if (driver.getName().equals(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)) {
                driver.setSelected(true);
                bound.add(driver);
            } else {
                driver.setSelected(false);
            }
        }

        ParameterDriver selected = bound.getDrivers().get(0);
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();
      
        selected.setValue(p0 - 4 * h);
        final double[] shortPeriodM4 = computeShortPeriodTerms(meanState, zonal);
  
        selected.setValue(p0 - 3 * h);
        final double[] shortPeriodM3 = computeShortPeriodTerms(meanState, zonal);
      
        selected.setValue(p0 - 2 * h);
        final double[] shortPeriodM2 = computeShortPeriodTerms(meanState, zonal);
      
        selected.setValue(p0 - 1 * h);
        final double[] shortPeriodM1 = computeShortPeriodTerms(meanState, zonal);
      
        selected.setValue(p0 + 1 * h);
        final double[] shortPeriodP1 = computeShortPeriodTerms(meanState, zonal);
      
        selected.setValue(p0 + 2 * h);
        final double[] shortPeriodP2 = computeShortPeriodTerms(meanState, zonal);
      
        selected.setValue(p0 + 3 * h);
        final double[] shortPeriodP3 = computeShortPeriodTerms(meanState, zonal);
      
        selected.setValue(p0 + 4 * h);
        final double[] shortPeriodP4 = computeShortPeriodTerms(meanState, zonal);

        fillJacobianColumn(shortPeriodJacobianRef, 0, orbitType, h,
                           shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                           shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);

        for (int i = 0; i < 6; ++i) {
            double error = FastMath.abs((shortPeriodJacobian[i][0] - shortPeriodJacobianRef[i][0]) / stateVector[i]) * h;
            Assertions.assertEquals(0, error, 1.3e-18);
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
                                             DSSTForceModel force) {

        AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        shortPeriodTerms.addAll(force.initializeShortPeriodTerms(auxiliaryElements, PropagationType.OSCULATING, force.getParameters(state.getDate())));
        force.updateShortPeriodTerms(force.getParametersAllValues(), state);
        
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
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

}
