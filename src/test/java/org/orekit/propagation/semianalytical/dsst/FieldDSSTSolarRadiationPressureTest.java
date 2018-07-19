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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.InertialProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.AbstractGaussianContribution;
import org.orekit.propagation.semianalytical.dsst.forces.AbstractGaussianContributionContext;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.FieldAbstractGaussianContributionContext;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

public class FieldDSSTSolarRadiationPressureTest {
    
    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {
        doTestGetMeanElementRate(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        
        final T zero = field.getZero();
        
        final Frame earthFrame = FramesFactory.getGCRF();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 9, 16, 0, 0, 0, TimeScalesFactory.getUTC());
        final double mu = 3.986004415E14;
        // a  = 42166258 m
        // ex = 6.532127416888538E-6
        // ey = 9.978642849310487E-5
        // hx = -5.69711879850274E-6
        // hy = 6.61038518895005E-6
        // lM = 8.56084687583949 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(4.2166258E7),
                                                                zero.add(6.532127416888538E-6),
                                                                zero.add(9.978642849310487E-5),
                                                                zero.add(-5.69711879850274E-6),
                                                                zero.add(6.61038518895005E-6),
                                                                zero.add(8.56084687583949),
                                                                PositionAngle.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                zero.add(mu));

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            mu);

        // Register the attitude provider to the force model
        Rotation rotation =  new Rotation(0.9999999999999984,
                                          1.653020584550675E-8,
                                          -4.028108631990782E-8,
                                          -3.539139805514139E-8,
                                          false);
        AttitudeProvider attitudeProvider = new InertialProvider(rotation);
        srp.registerAttitudeProvider(attitudeProvider);

        // Attitude of the satellite
        FieldRotation<T> fieldRotation = new FieldRotation<>(field, rotation);
        FieldVector3D<T> rotationRate = new FieldVector3D<>(zero, zero, zero);
        FieldVector3D<T> rotationAcceleration = new FieldVector3D<>(zero, zero, zero);
        TimeStampedFieldAngularCoordinates<T> orientation = new TimeStampedFieldAngularCoordinates<>(initDate,
                                                                                                     fieldRotation,
                                                                                                     rotationRate,
                                                                                                     rotationAcceleration);
        final FieldAttitude<T> att = new FieldAttitude<>(earthFrame, orientation);
        
        // Spacecraft state
        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, att, mass);
        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);
        
        // Compute the mean element rate
        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        final T[] daidt = srp.getMeanElementRate(state, auxiliaryElements, srp.getParameters(field));
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assert.assertEquals(6.843966348263062E-8,    elements[0].getReal(), 1.1e-11);
        Assert.assertEquals(-2.990913371084091E-11,  elements[1].getReal(), 2.2e-19);
        Assert.assertEquals(-2.538374405334012E-10,  elements[2].getReal(), 8.0e-19);
        Assert.assertEquals(2.0384702426501394E-13,  elements[3].getReal(), 2.0e-20);
        Assert.assertEquals(-2.3346333406116967E-14, elements[4].getReal(), 8.5e-22);
        Assert.assertEquals(1.6087485237156322E-11,  elements[5].getReal(), 1.7e-18);

    }

    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException, OrekitException {
        doTestShortPeriodTerms(Decimal64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends RealFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
 
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(7069219.9806427825),
                                                                zero.add(-4.5941811292223825E-4),
                                                                zero.add(1.309932339472599E-4),
                                                                zero.add(-1.002996107003202),
                                                                zero.add(0.570979900577994),
                                                                zero.add(2.62038786211518),
                                                                PositionAngle.TRUE,
                                                                FramesFactory.getEME2000(),
                                                                initDate,
                                                                zero.add(3.986004415E14));

        final FieldSpacecraftState<T> meanState = new FieldSpacecraftState<>(orbit);

        final CelestialBody    sun   = CelestialBodyFactory.getSun();
        
        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0,
                                                                                     sun,
                                                                                     50.0, Vector3D.PLUS_J,
                                                                                     2.0, 0.1,
                                                                                     0.2, 0.6);
        
        final AttitudeProvider attitudeProvider = new LofOffset(meanState.getFrame(),
                                                                LOFType.VVLH, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        final DSSTForceModel srp = new DSSTSolarRadiationPressure(sun,
                                                                   Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                   boxAndWing,
                                                                   meanState.getMu().getReal());
        
        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanState.getOrbit(), 1);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();

        srp.registerAttitudeProvider(attitudeProvider);
        shortPeriodTerms.addAll(srp.initialize(aux, false, srp.getParameters(field)));
        srp.updateShortPeriodTerms(srp.getParameters(field), meanState);

        T[] y = MathArrays.buildArray(field, 6);
        Arrays.fill(y, zero);
        
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }

        Assert.assertEquals(0.36637346843285684,     y[0].getReal(), 0.5e-12);
        Assert.assertEquals(-2.4294913010512626E-10, y[1].getReal(), 2.6e-20);
        Assert.assertEquals(-3.858954680824408E-9,   y[2].getReal(), 7.e-20);
        Assert.assertEquals(-3.0648619902684686E-9,  y[3].getReal(), 0.9e-21);
        Assert.assertEquals(-4.9023731169635814E-9,  y[4].getReal(), 1.1e-19);
        Assert.assertEquals(-2.385357916413363E-9,   y[5].getReal(), 1.3e-20);
    }

    @Test
    public void testGetLLimits() throws NoSuchMethodException, SecurityException, OrekitException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        // Access to private methods, both double and RealFieldElement versions
        Method limitsDS;
        Method limits;
        
        limitsDS = AbstractGaussianContribution.class.getDeclaredMethod("getLLimits",
                                                                       FieldSpacecraftState.class,
                                                                       FieldAbstractGaussianContributionContext.class);
        limits   = AbstractGaussianContribution.class.getDeclaredMethod("getLLimits",
                                                                        SpacecraftState.class,
                                                                        AbstractGaussianContributionContext.class);
        
        limitsDS.setAccessible(true);
        limits.setAccessible(true);

        // Spacecraft State
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        
        Orbit initialOrbit = new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                                                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                provider.getMu());
        final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(initialOrbit);
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
        
        final SpacecraftState state = new SpacecraftState(orbit);        
        
        // Force Model
        final DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            provider.getMu());
        
        // Converter for derivatives
        final DSSTDSConverter converter = new DSSTDSConverter(state, InertialProvider.EME2000_ALIGNED);
        
        // Field parameters
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(srp);
        final DerivativeStructure[] parameters = converter.getParameters(dsState, srp);
        final FieldAuxiliaryElements<DerivativeStructure> auxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);
        final FieldAbstractGaussianContributionContext<DerivativeStructure> context = new FieldAbstractGaussianContributionContext<>(auxiliaryElements, parameters);
        
        // Compute values using getLLimits method
        DerivativeStructure[] methodLimits = (DerivativeStructure[]) limitsDS.invoke(srp, dsState, context);
        final double[] limitsInf           = methodLimits[0].getAllDerivatives();
        final double[] limitsSup           = methodLimits[1].getAllDerivatives();
          
        // Compute reference values using finite differences
        double[][] refLimits = new double[2][6];
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; ++i) {
            
            SpacecraftState stateM4 = shiftState(state, orbitType, -4 * steps[i], i);
            AuxiliaryElements auxiliaryElementsM4         = new AuxiliaryElements(stateM4.getOrbit(), 1);
            AbstractGaussianContributionContext contextM4 = new AbstractGaussianContributionContext(auxiliaryElementsM4, srp.getParameters());
            double[] M4h = (double[]) limits.invoke(srp, stateM4, contextM4);
            
            SpacecraftState stateM3 = shiftState(state, orbitType, -3 * steps[i], i);
            AuxiliaryElements auxiliaryElementsM3         = new AuxiliaryElements(stateM3.getOrbit(), 1);
            AbstractGaussianContributionContext contextM3 = new AbstractGaussianContributionContext(auxiliaryElementsM3, srp.getParameters());
            double[] M3h = (double[]) limits.invoke(srp, stateM3, contextM3);
            
            SpacecraftState stateM2 = shiftState(state, orbitType, -2 * steps[i], i);
            AuxiliaryElements auxiliaryElementsM2         = new AuxiliaryElements(stateM2.getOrbit(), 1);
            AbstractGaussianContributionContext contextM2 = new AbstractGaussianContributionContext(auxiliaryElementsM2, srp.getParameters());
            double[] M2h = (double[]) limits.invoke(srp, stateM2, contextM2);
            
            SpacecraftState stateM1 = shiftState(state, orbitType, -1 * steps[i], i);
            AuxiliaryElements auxiliaryElementsM1         = new AuxiliaryElements(stateM1.getOrbit(), 1);
            AbstractGaussianContributionContext contextM1 = new AbstractGaussianContributionContext(auxiliaryElementsM1, srp.getParameters());
            double[] M1h = (double[]) limits.invoke(srp, stateM1, contextM1);
            
            SpacecraftState stateP1 = shiftState(state, orbitType,  1 * steps[i], i);
            AuxiliaryElements auxiliaryElementsP1         = new AuxiliaryElements(stateP1.getOrbit(), 1);
            AbstractGaussianContributionContext contextP1 = new AbstractGaussianContributionContext(auxiliaryElementsP1, srp.getParameters());
            double[] P1h = (double[]) limits.invoke(srp, stateP1, contextP1);
            
            SpacecraftState stateP2 = shiftState(state, orbitType,  2 * steps[i], i);
            AuxiliaryElements auxiliaryElementsP2         = new AuxiliaryElements(stateP2.getOrbit(), 1);
            AbstractGaussianContributionContext contextP2 = new AbstractGaussianContributionContext(auxiliaryElementsP2, srp.getParameters());
            double[] P2h = (double[]) limits.invoke(srp, stateP2, contextP2);
            
            SpacecraftState stateP3 = shiftState(state, orbitType,  3 * steps[i], i);
            AuxiliaryElements auxiliaryElementsP3         = new AuxiliaryElements(stateP3.getOrbit(), 1);
            AbstractGaussianContributionContext contextP3 = new AbstractGaussianContributionContext(auxiliaryElementsP3, srp.getParameters());
            double[] P3h = (double[]) limits.invoke(srp, stateP3, contextP3);
            
            SpacecraftState stateP4 = shiftState(state, orbitType,  4 * steps[i], i);
            AuxiliaryElements auxiliaryElementsP4         = new AuxiliaryElements(stateP4.getOrbit(), 1);
            AbstractGaussianContributionContext contextP4 = new AbstractGaussianContributionContext(auxiliaryElementsP4, srp.getParameters());
            double[] P4h = (double[]) limits.invoke(srp, stateP4, contextP4);
            
            fillJacobianColumn(refLimits, i, orbitType, steps[i],
                               M4h, M3h, M2h, M1h, P1h, P2h, P3h, P4h);
            
        }

            for (int j = 0; j < 6; ++j) {
                // We don't want to divide by 0
                if(refLimits[0][j] != 0 && refLimits[1][j] != 0) {
                    double errorInf = FastMath.abs((refLimits[0][j] - limitsInf[j + 1]) / refLimits[0][j]);
                    double errorSup = FastMath.abs((refLimits[1][j] - limitsSup[j + 1]) / refLimits[1][j]);
                    Assert.assertEquals(0, errorInf, 6.0e-2);
                    Assert.assertEquals(0, errorSup, 6.0e-2);
                }
            }

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
        
        // Attitude
        final AttitudeProvider attitudeProvider = new LofOffset(meanState.getFrame(),
                                                                LOFType.VVLH, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        // Force model
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        final DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                                  Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                  provider.getMu());
        srp.registerAttitudeProvider(attitudeProvider);
        final double[] parameters    = srp.getParameters();
                        
        // Converter for derivatives
        final DSSTDSConverter converter = new DSSTDSConverter(meanState, InertialProvider.EME2000_ALIGNED);
        
        // Field parameters
        final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(srp);
        final DerivativeStructure[] dsParameters                = converter.getParameters(dsState, srp);
        
        final FieldAuxiliaryElements<DerivativeStructure> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);
        
        // Zero
        final DerivativeStructure zero = dsState.getDate().getField().getZero();
        
        // Compute state Jacobian using directly the method
        final List<FieldShortPeriodTerms<DerivativeStructure>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<DerivativeStructure>>();
        shortPeriodTerms.addAll(srp.initialize(fieldAuxiliaryElements, false, dsParameters));
        srp.updateShortPeriodTerms(dsParameters, dsState);
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
            shortPeriodTermsM4.addAll(srp.initialize(auxiliaryElementsM4, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateM4);
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
            shortPeriodTermsM3.addAll(srp.initialize(auxiliaryElementsM3, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateM3);
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
            shortPeriodTermsM2.addAll(srp.initialize(auxiliaryElementsM2, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateM2);
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
            shortPeriodTermsM1.addAll(srp.initialize(auxiliaryElementsM1, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateM1);
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
            shortPeriodTermsP1.addAll(srp.initialize(auxiliaryElementsP1, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateP1);
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
            shortPeriodTermsP2.addAll(srp.initialize(auxiliaryElementsP2, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateP2);
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
            shortPeriodTermsP3.addAll(srp.initialize(auxiliaryElementsP3, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateP3);
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
            shortPeriodTermsP4.addAll(srp.initialize(auxiliaryElementsP4, false, parameters));
            srp.updateShortPeriodTerms(parameters, stateP4);
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
                Assert.assertEquals(0, error, 8.3e-10);
            }
        }

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
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

}
