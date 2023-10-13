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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
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
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
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
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldDSSTSolarRadiationPressureTest {

    @Test
    public void testGetMeanElementRate() {
        doTestGetMeanElementRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field) {

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
                                                                PositionAngleType.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                zero.add(mu));

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                            mu);

        // Register the attitude provider to the force model
        Rotation rotation =  new Rotation(0.9999999999999984,
                                          1.653020584550675E-8,
                                          -4.028108631990782E-8,
                                          -3.539139805514139E-8,
                                          false);
        AttitudeProvider attitudeProvider = new FrameAlignedProvider(rotation);
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

        // Force model parameters
        final T[] parameters = srp.getParameters(field);
        // Initialize force model
        srp.initializeShortPeriodTerms(auxiliaryElements,
                        PropagationType.MEAN, parameters);

        // Compute the mean element rate
        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        final T[] daidt = srp.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals( 6.839644084174288E-8,    elements[0].getReal(), 1.0e-23);
        Assertions.assertEquals(-2.990943651374133E-11,   elements[1].getReal(), 1.0e-27);
        Assertions.assertEquals(-2.5384000825777143E-10,  elements[2].getReal(), 1.0e-26);
        Assertions.assertEquals( 2.0378397296268635E-13,  elements[3].getReal(), 1.0e-29);
        Assertions.assertEquals(-2.3338910700820843E-14,  elements[4].getReal(), 1.0e-30);
        Assertions.assertEquals( 1.6082477070964212E-11,  elements[5].getReal(), 1.0e-27);

    }

    @Test
    public void testShortPeriodTerms() {
        doTestShortPeriodTerms(Binary64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends CalculusFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field) {

        final T zero = field.getZero();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(7069219.9806427825),
                                                                zero.add(-4.5941811292223825E-4),
                                                                zero.add(1.309932339472599E-4),
                                                                zero.add(-1.002996107003202),
                                                                zero.add(0.570979900577994),
                                                                zero.add(2.62038786211518),
                                                                PositionAngleType.TRUE,
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
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        final DSSTForceModel srp = new DSSTSolarRadiationPressure(sun,
                                                                  new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                       Constants.WGS84_EARTH_FLATTENING,
                                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                                  boxAndWing,
                                                                  meanState.getMu().getReal());

        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanState.getOrbit(), 1);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();

        srp.registerAttitudeProvider(attitudeProvider);
        shortPeriodTerms.addAll(srp.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, srp.getParameters(field)));
        srp.updateShortPeriodTerms(srp.getParametersAllValues(field), meanState);

        T[] y = MathArrays.buildArray(field, 6);
        Arrays.fill(y, zero);

        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }

        Assertions.assertEquals(0.3668654523023674,    y[0].getReal(), 1.0e-16);
        Assertions.assertEquals(-2.5673332283029E-10,  y[1].getReal(), 1.0e-23);
        Assertions.assertEquals(-3.84959877691874E-9,  y[2].getReal(), 1.0e-23);
        Assertions.assertEquals(-3.069285299519465E-9, y[3].getReal(), 1.0e-23);
        Assertions.assertEquals(-4.90887054227722E-9,  y[4].getReal(), 1.0e-23);
        Assertions.assertEquals(-2.38549338428378E-9,  y[5].getReal(), 1.0e-23);
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

        // Attitude
        final AttitudeProvider attitudeProvider = new LofOffset(meanState.getFrame(),
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        // Force model
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        final DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                                  new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                       Constants.WGS84_EARTH_FLATTENING,
                                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                                  provider.getMu());
        srp.registerAttitudeProvider(attitudeProvider);

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(meanState, Utils.defaultLaw());

        // Field parameters
        final FieldSpacecraftState<Gradient> dsState = converter.getState(srp);
        
        final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

        // Zero
        final Gradient zero = dsState.getDate().getField().getZero();

        // Compute state Jacobian using directly the method
        final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<Gradient>>();
        shortPeriodTerms.addAll(srp.initializeShortPeriodTerms(fieldAuxiliaryElements, PropagationType.OSCULATING,
                                converter.getParametersAtStateDate(dsState, srp)));
        srp.updateShortPeriodTerms(converter.getParameters(dsState, srp), dsState);
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
            double[]  shortPeriodM4 = computeShortPeriodTerms(stateM4, srp);

            SpacecraftState stateM3 = shiftState(meanState, orbitType, -3 * steps[i], i);
            double[]  shortPeriodM3 = computeShortPeriodTerms(stateM3, srp);

            SpacecraftState stateM2 = shiftState(meanState, orbitType, -2 * steps[i], i);
            double[]  shortPeriodM2 = computeShortPeriodTerms(stateM2, srp);

            SpacecraftState stateM1 = shiftState(meanState, orbitType, -1 * steps[i], i);
            double[]  shortPeriodM1 = computeShortPeriodTerms(stateM1, srp);

            SpacecraftState stateP1 = shiftState(meanState, orbitType, 1 * steps[i], i);
            double[]  shortPeriodP1 = computeShortPeriodTerms(stateP1, srp);

            SpacecraftState stateP2 = shiftState(meanState, orbitType, 2 * steps[i], i);
            double[]  shortPeriodP2 = computeShortPeriodTerms(stateP2, srp);

            SpacecraftState stateP3 = shiftState(meanState, orbitType, 3 * steps[i], i);
            double[]  shortPeriodP3 = computeShortPeriodTerms(stateP3, srp);

            SpacecraftState stateP4 = shiftState(meanState, orbitType, 4 * steps[i], i);
            double[]  shortPeriodP4 = computeShortPeriodTerms(stateP4, srp);

            fillJacobianColumn(shortPeriodJacobianRef, i, orbitType, steps[i],
                               shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                               shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);

        }

        for (int m = 0; m < 6; ++m) {
            for (int n = 0; n < 6; ++n) {
                double error = FastMath.abs((shortPeriodJacobian[m][n] - shortPeriodJacobianRef[m][n]) / shortPeriodJacobianRef[m][n]);
                Assertions.assertEquals(0, error, 8.3e-10);
            }
        }

    }

    @Test
    public void testSRPParametersDerivatives() throws ParseException, IOException {
        doTestShortPeriodTermsParametersDerivatives(RadiationSensitive.REFLECTION_COEFFICIENT, 9.e-15);
    }

    @Test
    public void testMuParametersDerivatives() throws ParseException, IOException {
        doTestShortPeriodTermsParametersDerivatives(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT, 5.e-11);
    }

    @SuppressWarnings("unchecked")
    private void doTestShortPeriodTermsParametersDerivatives(String parameterName, double tolerance) {

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

        // Attitude
        final AttitudeProvider attitudeProvider = new LofOffset(meanState.getFrame(),
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        // Force model
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        final DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                                  new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                       Constants.WGS84_EARTH_FLATTENING,
                                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                                  provider.getMu());
        srp.registerAttitudeProvider(attitudeProvider);

        for (final ParameterDriver driver : srp.getParametersDrivers()) {
            driver.setValue(driver.getReferenceValue());
            driver.setSelected(driver.getName().equals(parameterName));
        }

        // Converter for derivatives
        final DSSTGradientConverter converter = new DSSTGradientConverter(meanState, Utils.defaultLaw());

        // Field parameters
        final FieldSpacecraftState<Gradient> dsState = converter.getState(srp);
      
        final FieldAuxiliaryElements<Gradient> fieldAuxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), 1);

        // Zero
        final Gradient zero = dsState.getDate().getField().getZero();

        // Compute Jacobian using directly the method
        final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<Gradient>>();
        shortPeriodTerms.addAll(srp.initializeShortPeriodTerms(fieldAuxiliaryElements, PropagationType.OSCULATING, converter.getParametersAtStateDate(dsState, srp)));
        srp.updateShortPeriodTerms(converter.getParameters(dsState, srp), dsState);
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
        for (ParameterDriver driver : srp.getParametersDrivers()) {
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
        for (final ParameterDriver driver : srp.getParametersDrivers()) {
            if (driver.getName().equals(parameterName)) {
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
        final double[] shortPeriodM4 = computeShortPeriodTerms(meanState, srp);
  
        selected.setValue(p0 - 3 * h);
        final double[] shortPeriodM3 = computeShortPeriodTerms(meanState, srp);
      
        selected.setValue(p0 - 2 * h);
        final double[] shortPeriodM2 = computeShortPeriodTerms(meanState, srp);
      
        selected.setValue(p0 - 1 * h);
        final double[] shortPeriodM1 = computeShortPeriodTerms(meanState, srp);
      
        selected.setValue(p0 + 1 * h);
        final double[] shortPeriodP1 = computeShortPeriodTerms(meanState, srp);
      
        selected.setValue(p0 + 2 * h);
        final double[] shortPeriodP2 = computeShortPeriodTerms(meanState, srp);
      
        selected.setValue(p0 + 3 * h);
        final double[] shortPeriodP3 = computeShortPeriodTerms(meanState, srp);
      
        selected.setValue(p0 + 4 * h);
        final double[] shortPeriodP4 = computeShortPeriodTerms(meanState, srp);

        fillJacobianColumn(shortPeriodJacobianRef, 0, orbitType, h,
                           shortPeriodM4, shortPeriodM3, shortPeriodM2, shortPeriodM1,
                           shortPeriodP1, shortPeriodP2, shortPeriodP3, shortPeriodP4);

        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(shortPeriodJacobianRef[i][0],
                                shortPeriodJacobian[i][0],
                                FastMath.abs(shortPeriodJacobianRef[i][0] * tolerance));
        }

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
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

}
