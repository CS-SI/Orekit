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
import java.util.Arrays;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
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
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
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
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
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
        
        // Attitude
        final Rotation rotation = new Rotation(0.9999999999999984,
                                         1.653020584550675E-8,
                                         -4.028108631990782E-8,
                                         -3.539139805514139E-8,
                                         false);
        final AttitudeProvider attitudeProvider = new InertialProvider(rotation);
        
        
        // Force Model
        final DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            provider.getMu());
        // Register the attitude provider to the force model
        srp.registerAttitudeProvider(attitudeProvider);
        
        // Converter for derivatives
        final DSSTDSConverter converter = new DSSTDSConverter(state, attitudeProvider);
        
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

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

}
