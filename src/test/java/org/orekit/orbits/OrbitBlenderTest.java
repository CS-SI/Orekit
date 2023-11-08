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
package org.orekit.orbits;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.analysis.polynomials.SmoothStepFactory;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.StateCovariance;
import org.orekit.propagation.StateCovarianceKeplerianHermiteInterpolatorTest;
import org.orekit.propagation.StateCovarianceMatrixProvider;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.AbsolutePVCoordinatesTest;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class OrbitBlenderTest {

    private static SpacecraftState sergeiState;
    private static Orbit           sergeiOrbit;
    private static Frame           sergeiFrame;
    // Constants
    private final  double          DEFAULT_SERGEI_PROPAGATION_TIME   = 2400;
    private final  double          DEFAUTL_SERGEI_TABULATED_TIMESTEP = 2400;

    @BeforeAll
    public static void setUp() {
        StateCovarianceKeplerianHermiteInterpolatorTest.setUp();
        sergeiState = StateCovarianceKeplerianHermiteInterpolatorTest.generateSergeiReferenceState();
        sergeiOrbit = sergeiState.getOrbit();
        sergeiFrame = sergeiOrbit.getFrame();
    }

    public static void assertOrbit(final Orbit expected, final Orbit actual, final double epsilon) {
        Assertions.assertEquals(expected.getMu(), actual.getMu());
        Assertions.assertEquals(expected.getType(), actual.getType());
        AbsolutePVCoordinatesTest.assertPV(expected.getPVCoordinates(expected.getFrame()),
                                           actual.getPVCoordinates(expected.getFrame()),
                                           epsilon);
    }

    private DescriptiveStatistics[] computeStatisticsOrbitInterpolationOnSergeiCase(
            final double propagationDuration,
            final double tabulatedTimeStep,
            final TimeInterpolator<SpacecraftState> stateInterpolator) {

        // Given
        final List<SpacecraftState> referenceStates    = new ArrayList<>();
        final List<SpacecraftState> tabulatedStates    = new ArrayList<>();
        final List<SpacecraftState> interpolatedStates = new ArrayList<>();

        // Initialize reference state
        final AbsoluteDate initialDate = sergeiOrbit.getDate();

        // Initialize reference covariance matrix
        final RealMatrix sergeiCovarianceMatrix =
                StateCovarianceKeplerianHermiteInterpolatorTest.generateSergeiCovarianceMatrix();

        // Initialize propagator
        final NumericalPropagator propagator = new NumericalPropagator(
                StateCovarianceKeplerianHermiteInterpolatorTest.generateDefaultIntegrator(sergeiOrbit, OrbitType.CARTESIAN));

        propagator.setOrbitType(OrbitType.CARTESIAN);

        // Initialize harvester
        final MatricesHarvester harvester =
                propagator.setupMatricesComputation("harvester", null, null);

        // Initialize state covariance matrix provider
        final StateCovariance sergeiCovariance =
                new StateCovariance(sergeiCovarianceMatrix, sergeiState.getDate(), sergeiState.getFrame(),
                                    OrbitType.CARTESIAN, PositionAngleType.MEAN);

        final StateCovarianceMatrixProvider stateCovarianceMatrixProvider =
                new StateCovarianceMatrixProvider("covariance", "harvester", harvester, sergeiCovariance);

        // Configuring propagator
        propagator.setInitialState(sergeiState);

        StateCovarianceKeplerianHermiteInterpolatorTest.configurePropagatorForSergeiCase(propagator);

        propagator.addAdditionalStateProvider(stateCovarianceMatrixProvider);

        propagator.getMultiplexer().add(1, (currentState) -> {
            referenceStates.add(currentState);

            // Save tabulated state and covariance
            final double durationFromStart = currentState.getDate().durationFrom(sergeiState.getDate());
            if (durationFromStart % tabulatedTimeStep == 0) {
                tabulatedStates.add(currentState);
            }
        });

        // Propagation
        propagator.propagate(initialDate.shiftedBy(propagationDuration));

        // Create custom Ephemeris
        final Ephemeris ephemeris =
                new Ephemeris(tabulatedStates, stateInterpolator);

        // Interpolate
        for (int dt = 0; dt < referenceStates.size(); dt++) {
            final AbsoluteDate currentInterpolationDate = initialDate.shiftedBy(dt);

            interpolatedStates.add(ephemeris.propagate(currentInterpolationDate));
        }

        // Make statistics
        return computeRelativeError(referenceStates, interpolatedStates);
    }

    private DescriptiveStatistics[] computeRelativeError(final List<SpacecraftState> referenceStates,
                                                         final List<SpacecraftState> interpolatedStates) {

        final DescriptiveStatistics[] statistics =
                new DescriptiveStatistics[] { new DescriptiveStatistics(), new DescriptiveStatistics() };

        final List<PVCoordinates> relativePV = new ArrayList<>();
        for (int i = 0; i < referenceStates.size(); i++) {
            final PVCoordinates currentReferencePV    = referenceStates.get(i).getPVCoordinates();
            final PVCoordinates currentInterpolatedPV = interpolatedStates.get(i).getPVCoordinates();

            relativePV.add(new PVCoordinates(currentReferencePV, currentInterpolatedPV));

            // Add position error norm
            statistics[0].addValue(
                    100 * relativePV.get(i).getPosition().getNorm() / currentReferencePV.getPosition().getNorm());

            // Add velocity error norm
            statistics[1].addValue(
                    100 * relativePV.get(i).getVelocity().getNorm() / currentReferencePV.getVelocity().getNorm());
        }

        return statistics;
    }

    private Orbit getDefaultOrbitAtDate(final AbsoluteDate date, final Frame inertialFrame) {
        final double DEFAULT_MU   = Constants.IERS2010_EARTH_MU;
        final double EARTH_RADIUS = Constants.IERS2010_EARTH_EQUATORIAL_RADIUS;

        final Vector3D      position = new Vector3D(EARTH_RADIUS + 4000000, 0, 0);
        final Vector3D      velocity = new Vector3D(0, FastMath.sqrt(DEFAULT_MU / position.getNorm()), 0);
        final PVCoordinates pv       = new PVCoordinates(position, velocity);

        return new CartesianOrbit(pv, inertialFrame, date, DEFAULT_MU);
    }

    private void doTestInterpolation(final TimeInterpolator<SpacecraftState> stateInterpolator,
                                     final double propagationHorizon,
                                     final double tabulatedTimeStep,
                                     final double expectedMeanRMSPositionError,
                                     final double expectedMeanRMSVelocityError,
                                     final double expectedMedianRMSPositionError,
                                     final double expectedMedianRMSVelocityError,
                                     final double expectedMaxRMSPositionError,
                                     final double expectedMaxRMSVelocityError,
                                     final double tolerance,
                                     final boolean showResults) {
        final DescriptiveStatistics[] statistics =
                computeStatisticsOrbitInterpolationOnSergeiCase(propagationHorizon, tabulatedTimeStep, stateInterpolator);

        // Then
        if (showResults) {
            System.out.format(Locale.US, "%.17f%n", statistics[0].getMean());
            System.out.format(Locale.US, "%.17f%n", statistics[1].getMean());
            System.out.format(Locale.US, "%.17f%n", statistics[0].getPercentile(50));
            System.out.format(Locale.US, "%.17f%n", statistics[1].getPercentile(50));
            System.out.format(Locale.US, "%.17f%n", statistics[0].getMax());
            System.out.format(Locale.US, "%.17f%n", statistics[1].getMax());
        }
        Assertions.assertEquals(expectedMeanRMSPositionError, statistics[0].getMean(), tolerance);
        Assertions.assertEquals(expectedMeanRMSVelocityError, statistics[1].getMean(), tolerance);
        Assertions.assertEquals(expectedMedianRMSPositionError, statistics[0].getPercentile(50), tolerance);
        Assertions.assertEquals(expectedMedianRMSVelocityError, statistics[1].getPercentile(50), tolerance);
        Assertions.assertEquals(expectedMaxRMSPositionError, statistics[0].getMax(), tolerance);
        Assertions.assertEquals(expectedMaxRMSVelocityError, statistics[1].getMax(), tolerance);
    }

    @Test
    @DisplayName("non regression test on Keplerian quadratic orbit blending on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testKeplerianQuadraticBlendingOnSergeiCase() {
        // Given
        final SmoothStepFactory.SmoothStepFunction quadratic    = SmoothStepFactory.getQuadratic();
        final AbstractAnalyticalPropagator         propagator   = new KeplerianPropagator(sergeiOrbit);
        final OrbitBlender                         orbitBlender = new OrbitBlender(quadratic, propagator, sergeiFrame);

        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(sergeiFrame, orbitBlender, null, null, null, null);

        // When & Then
        doTestInterpolation(stateInterpolator, DEFAULT_SERGEI_PROPAGATION_TIME, DEFAUTL_SERGEI_TABULATED_TIMESTEP,
                            0.05185755740700528,
                            0.08169252246167892,
                            0.05262772652596856,
                            0.08349987869494085,
                            0.10151652739088853,
                            0.14827634525717634,
                            1e-17, false);
    }

    @Test
    @DisplayName("non regression test on Brouwer-Lyddane quadratic orbit blending on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testBrouwerLyddaneQuadraticBlendingOnSergeiCase() {
        // Given
        final SpacecraftState sergeiState = StateCovarianceKeplerianHermiteInterpolatorTest.generateSergeiReferenceState();
        final Frame           sergeiFrame = sergeiState.getFrame();

        final SmoothStepFactory.SmoothStepFunction quadratic = SmoothStepFactory.getQuadratic();
        final AbstractAnalyticalPropagator propagator =
                new BrouwerLyddanePropagator(sergeiState.getOrbit(), sergeiState.getMass(),
                                             Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                             Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20,
                                             Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                             Constants.EIGEN5C_EARTH_C50, 0);
        final OrbitBlender orbitBlender = new OrbitBlender(quadratic, propagator, sergeiFrame);

        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(sergeiFrame, orbitBlender, null, null, null, null);

        // When & Then
        doTestInterpolation(stateInterpolator, DEFAULT_SERGEI_PROPAGATION_TIME, DEFAUTL_SERGEI_TABULATED_TIMESTEP,
                            0.05106377388516059,
                            0.03671310671380644,
                            0.05451875412478483,
                            0.03654640625064279,
                            0.09412869297314610,
                            0.06642996306635666,
                            1e-17, false);
    }

    @Test
    @DisplayName("non regression test on Eckstein-Hechler quadratic orbit blending on full force model test case from : "
            + "TANYGIN, Sergei. Efficient covariance interpolation using blending of approximate covariance propagations. "
            + "The Journal of the Astronautical Sciences, 2014, vol. 61, no 1, p. 107-132.")
    void testEcksteinHechlerQuadraticBlendingOnSergeiCase() {
        // Given
        final SmoothStepFactory.SmoothStepFunction quadratic = SmoothStepFactory.getQuadratic();
        final AbstractAnalyticalPropagator propagator =
                new EcksteinHechlerPropagator(sergeiState.getOrbit(), sergeiState.getMass(),
                                              Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                              Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20,
                                              Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                              Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
        final OrbitBlender orbitBlender = new OrbitBlender(quadratic, propagator, sergeiFrame);

        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(sergeiFrame, orbitBlender, null, null, null, null);

        // When & Then
        doTestInterpolation(stateInterpolator, DEFAULT_SERGEI_PROPAGATION_TIME, DEFAUTL_SERGEI_TABULATED_TIMESTEP,
                            0.00854503692536256,
                            0.01192593187393609,
                            0.00895077301610845,
                            0.01299681289409554,
                            0.01600030634518512,
                            0.01743228687362160,
                            1e-17, false);
                            
    }

    @Test
    @DisplayName("test blending case with Keplerian dynamic (exact results expected)")
    void testBlendingWithKeplerianDynamic() {
        // Given
        final Frame orbitFrame  = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();

        final AbsoluteDate previousTabulatedDate = new AbsoluteDate();
        final AbsoluteDate nextTabulatedDate     = previousTabulatedDate.shiftedBy(3600);

        final AbsoluteDate interpolationDate1 = previousTabulatedDate.shiftedBy(1200);
        final AbsoluteDate interpolationDate2 = previousTabulatedDate.shiftedBy(1500);
        final AbsoluteDate interpolationDate3 = previousTabulatedDate.shiftedBy(2000);

        final Orbit                        previousTabulatedOrbit = getDefaultOrbitAtDate(previousTabulatedDate, orbitFrame);
        final AbstractAnalyticalPropagator propagator             = new KeplerianPropagator(previousTabulatedOrbit);

        final Orbit       nextTabulatedOrbit = propagator.propagate(nextTabulatedDate).getOrbit();
        final List<Orbit> orbitSample        = new ArrayList<>();
        orbitSample.add(previousTabulatedOrbit);
        orbitSample.add(nextTabulatedOrbit);

        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();
        final TimeInterpolator<Orbit> orbitBlender = new OrbitBlender(blendingFunction, propagator, outputFrame);

        // When & Then
        final double epsilon = 1e-8; // 10 nm

        assertOrbit(propagator.propagate(interpolationDate1).getOrbit(),
                    orbitBlender.interpolate(interpolationDate1, orbitSample), epsilon);
        assertOrbit(propagator.propagate(interpolationDate2).getOrbit(),
                    orbitBlender.interpolate(interpolationDate2, orbitSample), epsilon);
        assertOrbit(propagator.propagate(interpolationDate3).getOrbit(),
                    orbitBlender.interpolate(interpolationDate3, orbitSample), epsilon);

    }

    @Test
    @DisplayName("test specific case (blending at tabulated date)")
    void testBlendingAtTabulatedDate() {
        // Given
        final Frame orbitFrame  = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();

        final AbsoluteDate previousTabulatedDate = new AbsoluteDate();
        final AbsoluteDate nextTabulatedDate     = previousTabulatedDate.shiftedBy(3600);

        final Orbit       previousTabulatedOrbit = getDefaultOrbitAtDate(previousTabulatedDate, orbitFrame);
        final Orbit       nextTabulatedOrbit     = getDefaultOrbitAtDate(nextTabulatedDate, orbitFrame);
        final List<Orbit> orbitSample            = new ArrayList<>();
        orbitSample.add(previousTabulatedOrbit);
        orbitSample.add(nextTabulatedOrbit);

        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();
        final OrbitBlender orbitBlender = new OrbitBlender(blendingFunction,
                                                           new KeplerianPropagator(previousTabulatedOrbit),
                                                           outputFrame);

        // When
        final Orbit blendedOrbit = orbitBlender.interpolate(previousTabulatedDate, orbitSample);

        // Then
        assertOrbit(previousTabulatedOrbit, blendedOrbit, 1e-11);
        Assertions.assertEquals(outputFrame, orbitBlender.getOutputInertialFrame());
    }

    @Test
    @DisplayName("Test error thrown when using non inertial frame")
    void testErrorThrownWhenUsingNonInertialFrame() {
        // Given
        final SmoothStepFactory.SmoothStepFunction blendingFunctionMock = Mockito.mock(
              SmoothStepFactory.SmoothStepFunction.class);

        final AbstractAnalyticalPropagator propagatorMock =
              Mockito.mock(AbstractAnalyticalPropagator.class);

        final Frame nonInertialFrame = Mockito.mock(Frame.class);
        final String frameName = "frameName";
        Mockito.when(nonInertialFrame.isPseudoInertial()).thenReturn(false);
        Mockito.when(nonInertialFrame.getName()).thenReturn(frameName);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> new OrbitBlender(blendingFunctionMock,
                                                                          propagatorMock,
                                                                          nonInertialFrame));

        Assertions.assertEquals(MessageFormat.format(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME.getSourceString(), frameName),
                                thrown.getMessage());
    }

    @Test
    @DisplayName("Test error thrown when using orbits with different gravitational parameters")
    void testErrorThrownWhenUsingOrbitsWithDifferentMus() {
        // Given

        // Create blender
        final SmoothStepFactory.SmoothStepFunction blendingFunctionMock = Mockito.mock(
              SmoothStepFactory.SmoothStepFunction.class);

        final AbstractAnalyticalPropagator propagatorMock =
              Mockito.mock(AbstractAnalyticalPropagator.class);

        final Frame inertialFrame = Mockito.mock(Frame.class);
        Mockito.when(inertialFrame.isPseudoInertial()).thenReturn(true);

        final OrbitBlender interpolator = new OrbitBlender(blendingFunctionMock, propagatorMock, inertialFrame);

        // Create sample
        final List<Orbit> sample = new ArrayList<>();

        final Orbit orbit1Mock = Mockito.mock(Orbit.class);
        final Orbit orbit2Mock = Mockito.mock(Orbit.class);

        final double firstMu  = 1.;
        final double secondMu = 2.;

        Mockito.when(orbit1Mock.getMu()).thenReturn(firstMu);
        Mockito.when(orbit2Mock.getMu()).thenReturn(secondMu);

        sample.add(orbit1Mock);
        sample.add(orbit2Mock);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> interpolator.interpolate(new AbsoluteDate(), sample));

        Assertions.assertEquals(MessageFormat.format(OrekitMessages.ORBITS_MUS_MISMATCH.getSourceString(), firstMu, secondMu),
                                thrown.getMessage());
    }

}
