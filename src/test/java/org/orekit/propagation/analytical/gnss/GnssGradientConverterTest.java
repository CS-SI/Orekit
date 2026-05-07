/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessageFactory;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessageFactory;
import org.orekit.propagation.analytical.gnss.data.NonKeplerianDriversFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

class GnssGradientConverterTest {

    private DataContext context;
    private GNSSPropagator<GalileoNavigationMessage> propagator;

    @DefaultDataContext
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        context = DataContext.getDefault();
        final GalileoNavigationMessageFactory factory =
            new GalileoNavigationMessageFactory(context.getTimeScales(),
                                                SatelliteSystem.GALILEO,
                                                GalileoNavigationMessage.FNAV,
                                                context.getFrames().getEME2000(),
                                                context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        factory.setPrn(4);
        factory.setWeekAndTime(1024, 293400.0);
        final double sqrtA = 5440.602949142456;
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        factory.getDeltaN0Driver().setValue(3.7394414770330066E-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(2.4088891223073006E-4);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(0.9531656087278083);
        factory.getIDotDriver().setValue(-2.36081262303612E-10);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(-0.36639513583951266);
        factory.getOmegaDotDriver().setValue(-5.7695260382035525E-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(-1.6870064194345724);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(-0.38716557650888);
        factory.getCucDriver().setValue(-8.903443813323975E-7);
        factory.getCusDriver().setValue(6.61797821521759E-6);
        factory.getCrcDriver().setValue(194.0625);
        factory.getCrsDriver().setValue(-18.78125);
        factory.getCicDriver().setValue(3.166496753692627E-8);
        factory.getCisDriver().setValue(-1.862645149230957E-8);
        propagator = new GNSSPropagator<>(factory);
    }

    @Test
    void testInitialStateStmNoSelectedParameters() {
        GnssGradientConverter<GalileoNavigationMessage> converter = new GnssGradientConverter<>(propagator);
        final FieldGnssPropagator<Gradient, GalileoNavigationMessage> gPropagator = converter.getPropagator();
        Assertions.assertEquals(15, gPropagator.getParametersDrivers().size());
        Assertions.assertEquals(0, gPropagator.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count());
        Assertions.assertEquals(6, gPropagator.getInitialState().getOrbit().getA().getFreeParameters());
        checkUnitaryInitialSTM(gPropagator.getInitialState());
    }

    @Test
    void testInitialStateStmAllParametersSelected() {
        propagator.getParametersDrivers().forEach(p -> p.setSelected(true));
        GnssGradientConverter<GalileoNavigationMessage> converter = new GnssGradientConverter<>(propagator);
        final FieldGnssPropagator<Gradient, GalileoNavigationMessage> gPropagator = converter.getPropagator();
        Assertions.assertEquals(15, gPropagator.getParametersDrivers().size());
        Assertions.assertEquals(15, gPropagator.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count());
        Assertions.assertEquals(21, gPropagator.getInitialState().getOrbit().getA().getFreeParameters());
        checkUnitaryInitialSTM(gPropagator.getInitialState());
    }

    @Test
    void testStmAndJacobian() {
        // Initial GPS orbital elements (Ref: IGS)
        final GPSLegacyNavigationMessageFactory factory =
            new GPSLegacyNavigationMessageFactory(context.getTimeScales(), SatelliteSystem.GPS,
                                                  GPSLegacyNavigationMessage.LNAV,
                                                  context.getFrames().getEME2000(),
                                                  context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        factory.setPrn(7);
        factory.setWeekAndTime(0, 288000);
        final double sqrtA = 5153.599830627441;
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(0.012442796607501805);
        factory.getDeltaN0Driver().setValue(4.419469802942352E-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(0.9558937988021613);
        factory.getIDotDriver().setValue(-2.4608167886110235E-10);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(1.0479401362158658);
        factory.getOmegaDotDriver().setValue(-7.967117576712062E-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(-2.4719019944000538);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(-1.0899023379614294);
        factory.getCucDriver().setValue(4.3995678424835205E-6);
        factory.getCusDriver().setValue(1.002475619316101E-5);
        factory.getCrcDriver().setValue(183.40625);
        factory.getCrsDriver().setValue(87.03125);
        factory.getCicDriver().setValue(3.203749656677246E-7);
        factory.getCisDriver().setValue(4.0978193283081055E-8);
        GNSSPropagator<GPSLegacyNavigationMessage> propagator = new GNSSPropagator<>(factory);

        // we want to compute the partial derivatives with respect to Crs and Crc parameters
        Assertions.assertEquals(15, propagator.getParameters().length);
        propagator.getParametersDrivers().get(NonKeplerianDriversFactory.CRS_INDEX).setSelected(true);
        propagator.getParametersDrivers().get(NonKeplerianDriversFactory.CRC_INDEX).setSelected(true);
        final DoubleArrayDictionary initialJacobianColumns = new DoubleArrayDictionary();
        initialJacobianColumns.put(NonKeplerianDriversFactory.RADIUS_SINE,   new double[6]);
        initialJacobianColumns.put(NonKeplerianDriversFactory.RADIUS_COSINE, new double[6]);
        final MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, initialJacobianColumns);

        // harvester sorts the columns lexicographically, and wraps them as SpanXxx##
        Assertions.assertEquals(2, harvester.getJacobiansColumnsNames().size());
        Assertions.assertEquals("Span" + NonKeplerianDriversFactory.RADIUS_COSINE + "0", harvester.getJacobiansColumnsNames().get(0));
        Assertions.assertEquals("Span" + NonKeplerianDriversFactory.RADIUS_SINE   + "0", harvester.getJacobiansColumnsNames().get(1));

        // propagate orbit
        final SpacecraftState state = propagator.propagate(factory.getDate().shiftedBy(3600.0));

        // check STM against finite differences
        final RealMatrix stm = harvester.getStateTransitionMatrix(state);
        OrbitType type = harvester.getOrbitType();
        Assertions.assertEquals(OrbitType.KEPLERIAN, type);
        final double [] steps = ToleranceProvider.
                                getDefaultToleranceProvider(100.0).
                                getTolerances(state.getOrbit(), type)[0];
        double maxRelativeError = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                final double finiteDifferences = differentiate(propagator, type, state.getDate(), steps[j], i, j);
                final double relativeError = (finiteDifferences - stm.getEntry(i, j)) / finiteDifferences;
                System.out.format(Locale.ROOT, "%s%12.3f (%.4e %.4e)",
                                  j == 3 ? "     " : " ", relativeError, finiteDifferences, stm.getEntry(i, j));
                maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs(relativeError));
            }
            System.out.format(Locale.ROOT, "%n");
        }
        System.out.format(Locale.ROOT, "maxRelativeError = %10.3e%n", maxRelativeError);
        Assertions.assertEquals(0.0, maxRelativeError, 6.5e-13);

        // check Jacobian against finite differences
        final RealMatrix jacobian = harvester.getParametersJacobian(state);
        final double h = 100000.0;
        double maxErrorP = 0.0;
        double maxErrorV = 0.0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 2; j++) {
                final ToDoubleFunction<GNSSOrbitalElementsFactory<GPSLegacyNavigationMessage>> getter;
                final BiConsumer<GNSSOrbitalElementsFactory<GPSLegacyNavigationMessage>, Double> setter;
                if (j == 0) {
                    getter = f      -> f.getCrcDriver().getValue();
                    setter = (f, d) -> f.getCrcDriver().setValue(d);
                } else {
                    getter = f      -> f.getCrsDriver().getValue();
                    setter = (f, d) -> f.getCrsDriver().setValue(d);
                }
                final double error = differentiate(factory, state.getDate(), getter, setter, h, i) -
                                     jacobian.getEntry(i, j);
                if (i < 3) {
                    maxErrorP = FastMath.max(maxErrorP, FastMath.abs(error));
                } else {
                    maxErrorV = FastMath.max(maxErrorV, FastMath.abs(error));
                }
            }
        }
        Assertions.assertEquals(0.0, maxErrorP, 4.7e-14);
        Assertions.assertEquals(0.0, maxErrorV, 2.2e-17);

    }

    private void checkUnitaryInitialSTM(final FieldSpacecraftState<Gradient> initialState) {
        final FieldPVCoordinates<Gradient> pv0 = initialState.getPVCoordinates();
        checkUnitary(pv0.getPosition().getX().getGradient(), 0, 4.0e-13, 2.0e-8);
        checkUnitary(pv0.getPosition().getY().getGradient(), 1, 4.0e-13, 2.0e-8);
        checkUnitary(pv0.getPosition().getZ().getGradient(), 2, 4.0e-13, 2.0e-8);
        checkUnitary(pv0.getVelocity().getX().getGradient(), 3, 2.0e-12, 2.0e-12);
        checkUnitary(pv0.getVelocity().getY().getGradient(), 4, 2.0e-12, 2.0e-12);
        checkUnitary(pv0.getVelocity().getZ().getGradient(), 5, 2.0e-12, 2.0e-12);
    }

    private void checkUnitary(final double[] gradient, final int index,
                              final double tolDiag, final double tolNonDiag) {
        // beware! we intentionally check only the first 6 parameters
        // the next ones correspond to non-Keplerian parameters,
        // derivatives are NOT unitary for these extra parameters
        for (int i = 0; i < 6; i++) {
            if (i == index) {
                // diagonal element
                Assertions.assertEquals(1.0, gradient[i], tolDiag);
            } else {
                // non-diagonal element
                Assertions.assertEquals(0.0, gradient[i], tolNonDiag);
            }
        }
    }

    private <O extends GNSSOrbitalElements<O>> double differentiate(final GNSSPropagator<O> basePropagator,
                                                                    final OrbitType type,
                                                                    final AbsoluteDate target, final double step,
                                                                    final int outIndex, final int inIndex) {

        // function that converts a shift in one element of initial state
        // into one element of propagated state
        final UnivariateFunction f = h -> {

            // get initial state
            final SpacecraftState original = basePropagator.getInitialState();

            // shift element at specified index
            final double[] in = new double[6];
            type.mapOrbitToArray(original.getOrbit(), PositionAngleType.MEAN, in, null);
            in[inIndex] += h;

            // build shifted initial state
            final SpacecraftState shiftedState =
                new SpacecraftState(type.mapArrayToOrbit(in, null, PositionAngleType.MEAN,
                                                         original.getDate(),
                                                         original.getOrbit().getMu(), original.getFrame()),
                                    original.getAttitude()).withMass(original.getMass());

            // build shifted propagator
            final GNSSPropagator<O> shiftedPropagator =
                new GNSSPropagator<>(shiftedState,
                                     basePropagator.getOrbitalElements(),
                                     basePropagator.getECEF(),
                                     basePropagator.getAttitudeProvider(),
                                     shiftedState.getMass());

            // propagated state
            final SpacecraftState outState = shiftedPropagator.propagate(target);

            // return desired coordinate
            final double[] out = new double[6];
            type.mapOrbitToArray(outState.getOrbit(), PositionAngleType.MEAN, out, null);
            return out[outIndex];

        };

        final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, step);
        final UnivariateDifferentiableFunction df = differentiator.differentiate(f);

        return df.value(new UnivariateDerivative1(0.0, 1.0)).getFirstDerivative();

    }

    private <O extends GNSSOrbitalElements<O>> double differentiate(final GNSSOrbitalElementsFactory<O> factory,
                                                                    final AbsoluteDate target,
                                                                    final ToDoubleFunction<GNSSOrbitalElementsFactory<O>> getter,
                                                                    final BiConsumer<GNSSOrbitalElementsFactory<O>, Double> setter,
                                                                    final double step, final int outIndex) {

        // function that converts a shift in one element of initial state (i.e Px, Py, Pz, Vx, Vy, Vz)
        // into one element of propagated state
        final UnivariateFunction f = h -> {

            // get initial parameter value
            final double initialValue = getter.applyAsDouble(factory);

            // shift parameter
            setter.accept(factory, initialValue + h);

            // propagated state
            final SpacecraftState outState = new GNSSPropagator<>(factory).propagate(target);

            // reset parameter
            setter.accept(factory, initialValue);

            // return desired coordinate
            final double[] out = new double[6];
            OrbitType.CARTESIAN.mapOrbitToArray(outState.getOrbit(), PositionAngleType.MEAN, out, null);
            return out[outIndex];

        };

        final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, step);
        final UnivariateDifferentiableFunction df = differentiator.differentiate(f);

        return df.value(new UnivariateDerivative1(0.0, 1.0)).getFirstDerivative();

    }

}
