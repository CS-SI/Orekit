/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
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

public class FieldDSSTZonalTest {
    
    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {
        doTestGetMeanElementRate(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        
        final T zero = field.getZero();
        
        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);
        
        final Frame earthFrame = FramesFactory.getEME2000();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());
        
        // a  = 2655989.0 m
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
                                                                PositionAngle.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                zero.add(3.986004415E14));
        
        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, mass);

        final DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);
        
        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);
        
        // Initialize force model
        zonal.initialize(new AuxiliaryElements(orbit.toOrbit(), 1),
                         true, zonal.getParameters());

        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        
        final T[] daidt = zonal.getMeanElementRate(state, auxiliaryElements, zonal.getParameters(field));
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assert.assertEquals(0.0,                     elements[0].getReal(), 1.e-25);
        Assert.assertEquals(1.3909396722346468E-11,  elements[1].getReal(), 3.e-26);
        Assert.assertEquals(-2.0275977261372793E-13, elements[2].getReal(), 3.e-27);
        Assert.assertEquals(3.087141512018238E-9,    elements[3].getReal(), 1.e-24);
        Assert.assertEquals(2.6606317310148797E-9,   elements[4].getReal(), 4.e-24);
        Assert.assertEquals(-3.659904725206694E-9,   elements[5].getReal(), 1.e-24);
        
    }
    
    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException, OrekitException {
        doTestShortPeriodTerms(Decimal64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends RealFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        final T zero = field.getZero();
 
        final FieldSpacecraftState<T> meanState = getGEOState(field);
        
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);

        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanState.getOrbit(), 1);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();

        zonal.registerAttitudeProvider(null);
        shortPeriodTerms.addAll(zonal.initialize(aux, false, zonal.getParameters(field)));
        zonal.updateShortPeriodTerms(zonal.getParameters(field), meanState);

        T[] y = MathArrays.buildArray(field, 6);
        Arrays.fill(y, zero);
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }
        
        Assert.assertEquals(35.005618980090276,     y[0].getReal(), 1.e-15);
        Assert.assertEquals(3.75891551882889E-5,    y[1].getReal(), 1.e-20);
        Assert.assertEquals(3.929119925563796E-6,   y[2].getReal(), 1.e-21);
        Assert.assertEquals(-1.1781951949124315E-8, y[3].getReal(), 1.e-24);
        Assert.assertEquals(-3.2134924513679615E-8, y[4].getReal(), 1.e-24);
        Assert.assertEquals(-1.1607392915997098E-6, y[5].getReal(), 1.e-21);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testShortPeriodTermsDerivatives() throws OrekitException {
        
        // Initial spacecraft state
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());

        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngle.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);
        
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
       
        final SpacecraftState meanState = new SpacecraftState(orbit);
        
        // Force model
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final DSSTForceModel zonal   = new DSSTZonal(provider, 2, 1, 5);
        final double[] parameters    = zonal.getParameters();
                        
        // Converter for derivatives
        final DSSTDSConverter converter = new DSSTDSConverter(meanState, InertialProvider.EME2000_ALIGNED);
        
        // Field parameters
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(zonal);
        final DerivativeStructure[] dsParameters                = converter.getParameters(dsState, zonal);
        
        final FieldAuxiliaryElements<DerivativeStructure> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);
        
        // Zero
        final DerivativeStructure zero = dsState.getDate().getField().getZero();
        
        // Compute state Jacobian using directly the method
        final List<FieldShortPeriodTerms<DerivativeStructure>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<DerivativeStructure>>();
        shortPeriodTerms.addAll(zonal.initialize(fieldAuxiliaryElements, false, dsParameters));
        zonal.updateShortPeriodTerms(dsParameters, dsState);
        final DerivativeStructure[] shortPeriod = new DerivativeStructure[6];
        Arrays.fill(shortPeriod, zero);
        for (final FieldShortPeriodTerms<DerivativeStructure> spt : shortPeriodTerms) {
            final DerivativeStructure[] spVariation = spt.value(dsState.getOrbit());
            for (int i = 0; i < spVariation .length; i++) {
                shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
            }
        }
        
        final double[][] shortPeriodJacobian = new double[6][6];
      
        final double[] derivativesASP  = shortPeriod[0].getAllDerivatives();
        final double[] derivativesExSP = shortPeriod[1].getAllDerivatives();
        final double[] derivativesEySP = shortPeriod[2].getAllDerivatives();
        final double[] derivativesHxSP = shortPeriod[3].getAllDerivatives();
        final double[] derivativesHySP = shortPeriod[4].getAllDerivatives();
        final double[] derivativesLSP  = shortPeriod[5].getAllDerivatives();

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
            final AuxiliaryElements auxiliaryElementsM4 = new AuxiliaryElements(stateM4.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM4 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM4.addAll(zonal.initialize(auxiliaryElementsM4, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateM4);
            final double[] shortPeriodM4 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM4) {
                final double[] spVariation = spt.value(stateM4.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM4[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateM3 = shiftState(meanState, orbitType, -3 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM3 = new AuxiliaryElements(stateM3.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM3 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM3.addAll(zonal.initialize(auxiliaryElementsM3, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateM3);
            final double[] shortPeriodM3 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM3) {
                final double[] spVariation = spt.value(stateM3.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM3[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateM2 = shiftState(meanState, orbitType, -2 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM2 = new AuxiliaryElements(stateM2.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM2 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM2.addAll(zonal.initialize(auxiliaryElementsM2, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateM2);
            final double[] shortPeriodM2 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM2) {
                final double[] spVariation = spt.value(stateM2.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM2[j] += spVariation[j];
                }
            }
 
            SpacecraftState stateM1 = shiftState(meanState, orbitType, -1 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsM1 = new AuxiliaryElements(stateM1.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsM1 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsM1.addAll(zonal.initialize(auxiliaryElementsM1, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateM1);
            final double[] shortPeriodM1 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsM1) {
                final double[] spVariation = spt.value(stateM1.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodM1[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP1 = shiftState(meanState, orbitType, 1 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP1 = new AuxiliaryElements(stateP1.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP1 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP1.addAll(zonal.initialize(auxiliaryElementsP1, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateP1);
            final double[] shortPeriodP1 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP1) {
                final double[] spVariation = spt.value(stateP1.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP1[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP2 = shiftState(meanState, orbitType, 2 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP2 = new AuxiliaryElements(stateP2.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP2 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP2.addAll(zonal.initialize(auxiliaryElementsP2, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateP2);
            final double[] shortPeriodP2 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP2) {
                final double[] spVariation = spt.value(stateP2.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP2[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP3 = shiftState(meanState, orbitType, 3 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP3 = new AuxiliaryElements(stateP3.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP3 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP3.addAll(zonal.initialize(auxiliaryElementsP3, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateP3);
            final double[] shortPeriodP3 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP3) {
                final double[] spVariation = spt.value(stateP3.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP3[j] += spVariation[j];
                }
            }
            
            SpacecraftState stateP4 = shiftState(meanState, orbitType, 4 * steps[i], i);
            final AuxiliaryElements auxiliaryElementsP4 = new AuxiliaryElements(stateP4.getOrbit(), 1);
            final List<ShortPeriodTerms> shortPeriodTermsP4 = new ArrayList<ShortPeriodTerms>();
            shortPeriodTermsP4.addAll(zonal.initialize(auxiliaryElementsP4, false, parameters));
            zonal.updateShortPeriodTerms(parameters, stateP4);
            final double[] shortPeriodP4 = new double[6];
            for (final ShortPeriodTerms spt : shortPeriodTermsP4) {
                final double[] spVariation = spt.value(stateP4.getOrbit());
                for (int j = 0; j < spVariation .length; j++) {
                    shortPeriodP4[j] += spVariation[j];
                }
            }
            
            fillJacobianColumn(shortPeriodJacobianRef, i, orbitType, steps[i],
                               shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                               shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);
            
        }
        
        for (int m = 0; m < 6; ++m) {
            for (int n = 0; n < 6; ++n) {
                double error = FastMath.abs((shortPeriodJacobian[m][n] - shortPeriodJacobianRef[m][n]) / shortPeriodJacobianRef[m][n]);
                Assert.assertEquals(0, error, 9.6e-10);
            }
        }

    }

    private <T extends RealFieldElement<T>> FieldSpacecraftState<T> getGEOState(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        
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
                                                                PositionAngle.TRUE,
                                                                FramesFactory.getEME2000(),
                                                                initDate,
                                                                zero.add(3.986004415E14));
        return new FieldSpacecraftState<>(orbit);
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

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngle.MEAN, array[0], array[1]);
          return array;
      }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
          EquinoctialOrbit orbit = (EquinoctialOrbit) orbitType.mapArrayToOrbit(array[0], array[1], PositionAngle.MEAN, date, mu, frame);
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
            jacobian[index][i] += derivatives[i + 1];
        }

    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
                    
}
