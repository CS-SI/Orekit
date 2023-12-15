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
package org.orekit.propagation.analytical;

import org.hipparchus.analysis.polynomials.SmoothStepFactory;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeInterpolator;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudesSequence;
import org.orekit.attitudes.CelestialBodyPointed;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.EarthStandardAtmosphereRefraction;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitBlender;
import org.orekit.orbits.OrbitHermiteInterpolator;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.StateCovariance;
import org.orekit.propagation.StateCovarianceBlender;
import org.orekit.propagation.StateCovarianceKeplerianHermiteInterpolator;
import org.orekit.propagation.StateCovarianceTest;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStampedDoubleHermiteInterpolator;
import org.orekit.time.TimeStampedPair;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AbsolutePVCoordinatesTest;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class EphemerisTest {

    private AbsoluteDate initDate;
    private AbsoluteDate finalDate;
    private Frame        inertialFrame;
    private Propagator   propagator;
    private Orbit        initialState;

    @Test
    @DisplayName("test default Ephemeris constructor")
    void testDefaultEphemerisConstructor() {
        // Given
        // Create spacecraft state sample
        final Orbit           defaultOrbit = TestUtils.getDefaultOrbit(new AbsoluteDate());
        final SpacecraftState firstState   = new SpacecraftState(defaultOrbit);
        final SpacecraftState secondState  = firstState.shiftedBy(1);

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(firstState);
        states.add(secondState);

        final Ephemeris defaultEphemeris = new Ephemeris(states, states.size());

        // When
        final SpacecraftStateInterpolator defaultStateInterpolator =
                (SpacecraftStateInterpolator) defaultEphemeris.getStateInterpolator();

        // Then
        final Frame                       inertialFrame              = defaultOrbit.getFrame();
        final SpacecraftStateInterpolator referenceStateInterpolator = new SpacecraftStateInterpolator(inertialFrame);

        Assertions.assertEquals(referenceStateInterpolator.getExtrapolationThreshold(),
                                defaultStateInterpolator.getExtrapolationThreshold());
        Assertions.assertEquals(referenceStateInterpolator.getOutputFrame(),
                                defaultStateInterpolator.getOutputFrame());
        Assertions.assertEquals(referenceStateInterpolator.getAttitudeInterpolator().isPresent(),
                                defaultStateInterpolator.getAttitudeInterpolator().isPresent());
        Assertions.assertEquals(referenceStateInterpolator.getAdditionalStateInterpolator().isPresent(),
                                defaultStateInterpolator.getAdditionalStateInterpolator().isPresent());

        Assertions.assertInstanceOf(OrbitHermiteInterpolator.class,
                                    defaultStateInterpolator.getOrbitInterpolator().get());
        Assertions.assertInstanceOf(TimeStampedDoubleHermiteInterpolator.class,
                                    defaultStateInterpolator.getMassInterpolator().get());
        Assertions.assertInstanceOf(AttitudeInterpolator.class,
                                    defaultStateInterpolator.getAttitudeInterpolator().get());
        Assertions.assertInstanceOf(TimeStampedDoubleHermiteInterpolator.class,
                                    defaultStateInterpolator.getAdditionalStateInterpolator().get());

        Assertions.assertFalse(defaultEphemeris.getCovarianceInterpolator().isPresent());

    }

    @Test
    @DisplayName("test error thrown when using an empty list to create an Ephemeris")
    void testErrorThrownWhenUsingEmptyStateList() {
        // Given
        final SpacecraftState firstState  = Mockito.mock(SpacecraftState.class);
        final SpacecraftState secondState = Mockito.mock(SpacecraftState.class);

        Mockito.when(firstState.isOrbitDefined()).thenReturn(true);
        Mockito.when(secondState.isOrbitDefined()).thenReturn(true);

        final List<SpacecraftState> states = new ArrayList<>();

        // Create interpolator
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        final int nbInterpolationPoints = 3;
        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(nbInterpolationPoints, inertialFrameMock, inertialFrameMock);

        // When & Then
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                                        () -> new Ephemeris(states, stateInterpolator));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(0, ((Integer) thrown.getParts()[0]).intValue());

    }

    @Test
    @DisplayName("test error thrown when using inconsistent states sample and state interpolator")
    void testErrorThrownWhenUsingInconsistentStatesSampleAndStateInterpolator() {
        // Given
        final SpacecraftState firstState  = Mockito.mock(SpacecraftState.class);
        final SpacecraftState secondState = Mockito.mock(SpacecraftState.class);

        Mockito.when(firstState.isOrbitDefined()).thenReturn(true);
        Mockito.when(secondState.isOrbitDefined()).thenReturn(true);

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(firstState);
        states.add(secondState);

        // Create interpolator
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        final int nbInterpolationPoints = 3;
        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(nbInterpolationPoints, inertialFrameMock, inertialFrameMock);

        // When & Then
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                                        () -> new Ephemeris(states, stateInterpolator));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(2, ((Integer) thrown.getParts()[0]).intValue());

    }

    @Test
    @DisplayName("test error thrown when using inconsistent states and covariances")
    void testErrorThrownWhenUsingInconsistentStatesAndCovariances() {
        // Given

        // Create spacecraft state sample
        final SpacecraftState firstState  = Mockito.mock(SpacecraftState.class);
        final SpacecraftState secondState = Mockito.mock(SpacecraftState.class);

        Mockito.when(firstState.isOrbitDefined()).thenReturn(true);
        Mockito.when(secondState.isOrbitDefined()).thenReturn(true);

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(firstState);
        states.add(secondState);

        // Create covariance state sample
        final StateCovariance firstCovariance  = Mockito.mock(StateCovariance.class);
        final StateCovariance secondCovariance = Mockito.mock(StateCovariance.class);

        final List<StateCovariance> covariances = new ArrayList<>();
        covariances.add(firstCovariance);
        covariances.add(secondCovariance);

        // Simulate inconsistency between state and covariance
        final AbsoluteDate referenceDate = new AbsoluteDate();
        Mockito.when(firstState.getDate()).thenReturn(referenceDate);
        Mockito.when(firstCovariance.getDate()).thenReturn(referenceDate.shiftedBy(1));

        // Create state interpolator
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        final TimeInterpolator<SpacecraftState> stateInterpolator = new SpacecraftStateInterpolator(inertialFrameMock);

        // Create covariance interpolator
        @SuppressWarnings("unchecked")
        final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator =
                Mockito.mock(TimeInterpolator.class);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalStateException.class,
                                                   () -> new Ephemeris(states, stateInterpolator,
                                                                       covariances, covarianceInterpolator));

        Assertions.assertEquals(
                "state date 2000-01-01T11:58:55.816Z does not match its covariance date 2000-01-01T11:58:56.816Z",
                thrown.getMessage());
    }

    @Test
    @DisplayName("test error thrown when using different states and covariances sample size ")
    void testErrorThrownWhenUsingDifferentStatesAndCovariancesSampleSize() {
        // Given

        // Create spacecraft state sample
        final SpacecraftState firstState  = Mockito.mock(SpacecraftState.class);
        final SpacecraftState secondState = Mockito.mock(SpacecraftState.class);

        Mockito.when(firstState.isOrbitDefined()).thenReturn(true);
        Mockito.when(secondState.isOrbitDefined()).thenReturn(true);

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(firstState);
        states.add(secondState);

        // Create covariance state sample
        final StateCovariance firstCovariance = Mockito.mock(StateCovariance.class);

        final List<StateCovariance> covariances = new ArrayList<>();
        covariances.add(firstCovariance);

        // Create state interpolator
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        final TimeInterpolator<SpacecraftState> stateInterpolator = new SpacecraftStateInterpolator(inertialFrameMock);

        // Create covariance interpolator
        @SuppressWarnings("unchecked")
        final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceInterpolator =
                Mockito.mock(TimeInterpolator.class);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> new Ephemeris(states, stateInterpolator,
                                                                       covariances, covarianceInterpolator));

        Assertions.assertEquals(
                "inconsistent dimensions: 2 != 1", thrown.getMessage());
    }

    @Test
    @DisplayName("test error thrown when trying to reset intermediate state ")
    void testErrorThrownWhenTryingToResetIntermediateState() {
        // Given
        // Create spacecraft state sample
        final Orbit           defaultOrbit = TestUtils.getDefaultOrbit(new AbsoluteDate());
        final SpacecraftState firstState   = new SpacecraftState(defaultOrbit);
        final SpacecraftState secondState  = firstState.shiftedBy(1);

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(firstState);
        states.add(secondState);

        // Create state interpolator
        final Frame inertialFrame = FramesFactory.getEME2000();

        final TimeInterpolator<SpacecraftState> stateInterpolator = new SpacecraftStateInterpolator(inertialFrame);

        final Ephemeris ephemeris = new Ephemeris(states, stateInterpolator);

        // When & Then
        Exception thrown =
                Assertions.assertThrows(OrekitException.class, () -> ephemeris.resetIntermediateState(firstState, true));

        Assertions.assertEquals("reset state not allowed", thrown.getMessage());
    }

    @Test
    @DisplayName("test that an empty optional is returned when getting covariance from a state only ephemeris")
    void testEmptyCovarianceGetter() {
        // GIVEN
        final AbsoluteDate initialDate  = new AbsoluteDate();
        final Orbit        initialOrbit = TestUtils.getDefaultOrbit(initialDate);

        // Setup propagator
        final Orbit                 finalOrbit = initialOrbit.shiftedBy(1);
        final List<SpacecraftState> states     = new ArrayList<>();
        states.add(new SpacecraftState(initialOrbit));
        states.add(new SpacecraftState(finalOrbit));

        final Ephemeris ephemeris = new Ephemeris(states, 2);

        // When
        final Optional<StateCovariance> optionalCovariance = ephemeris.getCovariance(Mockito.mock(AbsoluteDate.class));

        // Then
        Assertions.assertFalse(optionalCovariance.isPresent());

    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("test that the expected spacecraft state is returned when no attitude provider is given at construction")
    void testExpectedStateReturnedWhenNoAttitudeProvider() {
        // GIVEN
        final AbsoluteDate initialDate  = new AbsoluteDate();
        final Orbit        initialOrbit = TestUtils.getDefaultOrbit(initialDate);

        // Setup behaviour
        final AbsoluteDate    propDate          = initialDate.shiftedBy(0.5);
        final SpacecraftState mockExpectedState = Mockito.mock(SpacecraftState.class);

        // Setup propagator
        final Orbit                 finalOrbit = initialOrbit.shiftedBy(1);
        final List<SpacecraftState> states     = new ArrayList<>();

        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        final SpacecraftState finalState   = new SpacecraftState(finalOrbit);

        states.add(initialState);
        states.add(finalState);

        // Setup mock interpolator
        final TimeInterpolator<SpacecraftState> mockInterpolator = Mockito.mock(TimeInterpolator.class);

        Mockito.when(mockInterpolator.getNbInterpolationPoints()).thenReturn(2);
        Mockito.when(mockInterpolator.getExtrapolationThreshold()).thenReturn(0.001);

        Mockito.when(mockInterpolator.interpolate(Mockito.eq(initialDate), Mockito.any(Stream.class)))
               .thenReturn(initialState);
        Mockito.when(mockInterpolator.interpolate(Mockito.eq(propDate), Mockito.any(Stream.class)))
               .thenReturn(mockExpectedState);

        final Ephemeris ephemeris = new Ephemeris(states, mockInterpolator, null);

        // When
        final SpacecraftState propState = ephemeris.basicPropagate(propDate);

        // Then
        Assertions.assertEquals(mockExpectedState, propState);

    }

    @Test
    public void testAttitudeOverride() throws IllegalArgumentException, OrekitException {
        setUp();

        final double positionTolerance = 1e-6;
        final double velocityTolerance = 1e-5;
        final double attitudeTolerance = 1e-6;

        int    numberOfIntervals = 1440;
        double deltaT            = finalDate.durationFrom(initDate) / ((double) numberOfIntervals);

        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));

        List<SpacecraftState> states = new ArrayList<>(numberOfIntervals + 1);
        for (int j = 0; j <= numberOfIntervals; j++) {
            states.add(propagator.propagate(initDate.shiftedBy((j * deltaT))));
        }

        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(inertialFrame);

        // Create ephemeris with attitude override
        Ephemeris ephemPropagator = new Ephemeris(states, interpolator, new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));
        Assertions.assertEquals(0, ephemPropagator.getManagedAdditionalStates().length);

        //First test that we got position, velocity and attitude nailed
        int numberEphemTestIntervals = 2880;
        deltaT = finalDate.durationFrom(initDate) / ((double) numberEphemTestIntervals);
        doTestInterpolation(numberEphemTestIntervals, deltaT, ephemPropagator,
                            positionTolerance, velocityTolerance, attitudeTolerance,
                            "LVLH_CCSDS");

        //Now force an override on the attitude and check it against a Keplerian propagator
        //setup identically to the first but with a different attitude
        //If override isn't working this will fail.
        propagator = new KeplerianPropagator(propagator.getInitialState().getOrbit());
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));

        ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));
        doTestInterpolation(numberEphemTestIntervals, deltaT, ephemPropagator,
                            positionTolerance, velocityTolerance, attitudeTolerance,
                            "QSW");
    }

    private void doTestInterpolation(final int numberEphemTestIntervals, final double deltaT,
                                     final Ephemeris ephemPropagator,
                                     final double positionTolerance,
                                     final double velocityTolerance,
                                     final double attitudeTolerance,
                                     final String errorMessage) {
        for (int j = 0; j <= numberEphemTestIntervals; j++) {
            AbsoluteDate    currentDate   = initDate.shiftedBy(j * deltaT);
            SpacecraftState ephemState    = ephemPropagator.propagate(currentDate);
            SpacecraftState keplerState   = propagator.propagate(currentDate);
            double          positionDelta = calculatePositionDelta(ephemState, keplerState);
            double          velocityDelta = calculateVelocityDelta(ephemState, keplerState);
            double          attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
            Assertions.assertEquals(0.0, positionDelta, positionTolerance, errorMessage + " Unmatched Position at: " + currentDate);
            Assertions.assertEquals(0.0, velocityDelta, velocityTolerance, errorMessage + " Unmatched Velocity at: " + currentDate);
            Assertions.assertEquals(0.0, attitudeDelta, attitudeTolerance, errorMessage + " Unmatched Attitude at: " + currentDate);
        }
    }

    @Test
    public void testAttitudeSequenceTransition() {
        setUp();

        // Initialize the orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2003, 01, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        final Vector3D     position    = new Vector3D(-39098981.4866597, -15784239.3610601, 78908.2289853595);
        final Vector3D     velocity    = new Vector3D(1151.00321021175, -2851.14864755189, -2.02133248357321);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getGCRF(), initialDate,
                                                      Constants.WGS84_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Define attitude laws
        AttitudeProvider before =
                new CelestialBodyPointed(FramesFactory.getICRF(), CelestialBodyFactory.getSun(), Vector3D.PLUS_K,
                                         Vector3D.PLUS_I, Vector3D.PLUS_K);
        AttitudeProvider after =
                new CelestialBodyPointed(FramesFactory.getICRF(), CelestialBodyFactory.getEarth(), Vector3D.PLUS_K,
                                         Vector3D.PLUS_I, Vector3D.PLUS_K);

        // Define attitude sequence
        AbsoluteDate switchDate     = initialDate.shiftedBy(86400.0);
        double       transitionTime = 600;
        DateDetector switchDetector = new DateDetector(switchDate).withHandler(new ContinueOnEvent());

        AttitudesSequence attitudeSequence = new AttitudesSequence();
        attitudeSequence.resetActiveProvider(before);
        attitudeSequence.addSwitchingCondition(before, after, switchDetector, true, false, transitionTime,
                                               AngularDerivativesFilter.USE_RR, null);

        NumericalPropagator propagator = new NumericalPropagator(new DormandPrince853Integrator(0.1, 500, 1e-9, 1e-9));
        propagator.setInitialState(initialState);

        // Propagate and build ephemeris
        final List<SpacecraftState> propagatedStates = new ArrayList<>();

        propagator.setStepHandler(60, propagatedStates::add);
        propagator.propagate(initialDate.shiftedBy(2 * 86400.0));

        // Create interpolator
        final int nbInterpolationPoints = 8;
        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(nbInterpolationPoints, inertialFrame, inertialFrame);

        final Ephemeris ephemeris = new Ephemeris(propagatedStates, interpolator);

        // Add attitude switch event to ephemeris
        ephemeris.setAttitudeProvider(attitudeSequence);
        attitudeSequence.registerSwitchEvents(ephemeris);

        // Propagate with a step during the transition
        AbsoluteDate    endDate     = initialDate.shiftedBy(2 * 86400.0);
        SpacecraftState stateBefore = ephemeris.getInitialState();
        ephemeris.propagate(switchDate.shiftedBy(transitionTime / 2));
        SpacecraftState stateAfter = ephemeris.propagate(endDate);

        // Check that the attitudes are correct
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation()
                      .getQ0(),
                stateBefore.getAttitude().getRotation().getQ0(),
                1.0E-16);
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation()
                      .getQ1(),
                stateBefore.getAttitude().getRotation().getQ1(),
                1.0E-16);
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation()
                      .getQ2(),
                stateBefore.getAttitude().getRotation().getQ2(),
                1.0E-16);
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation()
                      .getQ3(),
                stateBefore.getAttitude().getRotation().getQ3(),
                1.0E-16);

        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ0(),
                stateAfter.getAttitude().getRotation().getQ0(),
                1.0E-16);
        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ1(),
                stateAfter.getAttitude().getRotation().getQ1(),
                1.0E-16);
        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ2(),
                stateAfter.getAttitude().getRotation().getQ2(),
                1.0E-16);
        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ3(),
                stateAfter.getAttitude().getRotation().getQ3(),
                1.0E-16);
    }

    @Test
    public void testNonResettableState() {
        setUp();
        try {
            propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));

            List<SpacecraftState> states = new ArrayList<>();
            for (double dt = 0; dt >= -1200; dt -= 60.0) {
                states.add(propagator.propagate(initDate.shiftedBy(dt)));
            }

            // Create interpolator
            final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(inertialFrame);

            new Ephemeris(states, interpolator).resetInitialState(propagator.getInitialState());
            Assertions.fail("an exception should have been thrown");
        }
        catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    public void testAdditionalStates() {
        setUp();

        final String name1 = "dt0";
        final String name2 = "dt1";
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));

        List<SpacecraftState> states = new ArrayList<>();
        for (double dt = 0; dt >= -1200; dt -= 60.0) {
            final SpacecraftState original = propagator.propagate(initDate.shiftedBy(dt));
            final SpacecraftState expanded = original.addAdditionalState(name2, original.getDate().durationFrom(finalDate));
            states.add(expanded);
        }

        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(inertialFrame);

        final Propagator ephem = new Ephemeris(states, interpolator);
        ephem.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() {
                return name1;
            }

            public double[] getAdditionalState(SpacecraftState state) {
                return new double[] { state.getDate().durationFrom(initDate) };
            }
        });

        final String[] additional = ephem.getManagedAdditionalStates();
        Arrays.sort(additional);
        Assertions.assertEquals(2, additional.length);
        Assertions.assertEquals(name1, ephem.getManagedAdditionalStates()[0]);
        Assertions.assertEquals(name2, ephem.getManagedAdditionalStates()[1]);
        Assertions.assertTrue(ephem.isAdditionalStateManaged(name1));
        Assertions.assertTrue(ephem.isAdditionalStateManaged(name2));
        Assertions.assertFalse(ephem.isAdditionalStateManaged("not managed"));

        SpacecraftState s = ephem.propagate(initDate.shiftedBy(-270.0));
        Assertions.assertEquals(-270.0, s.getAdditionalState(name1)[0], 1.0e-15);
        Assertions.assertEquals(-86670.0, s.getAdditionalState(name2)[0], 1.0e-15);

    }

    @Test
    public void testProtectedMethods()
            throws SecurityException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        setUp();

        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));

        List<SpacecraftState> states = new ArrayList<>();
        for (double dt = 0; dt >= -1200; dt -= 60.0) {
            final SpacecraftState original = propagator.propagate(initDate.shiftedBy(dt));
            final SpacecraftState modified = new SpacecraftState(original.getOrbit(),
                                                                 original.getAttitude(),
                                                                 original.getMass() - 0.0625 * dt);
            states.add(modified);
        }

        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(inertialFrame);

        final Propagator ephem          = new Ephemeris(states, interpolator);
        Method           propagateOrbit = Ephemeris.class.getDeclaredMethod("propagateOrbit", AbsoluteDate.class);
        propagateOrbit.setAccessible(true);
        Method getMass = Ephemeris.class.getDeclaredMethod("getMass", AbsoluteDate.class);
        getMass.setAccessible(true);

        SpacecraftState s = ephem.propagate(initDate.shiftedBy(-270.0));
        Orbit           o = (Orbit) propagateOrbit.invoke(ephem, s.getDate());
        double          m = (Double) getMass.invoke(ephem, s.getDate());
        Assertions.assertEquals(0.0,
                                Vector3D.distance(s.getPosition(),
                                                  o.getPosition()),
                                1.0e-15);
        Assertions.assertEquals(s.getMass(), m, 1.0e-15);

    }

    @Test
    public void testGetCovariance() {

        // Given
        setUp();

        double                dt          = finalDate.durationFrom(initDate);
        double                timeStep    = dt / 20.0;
        List<SpacecraftState> states      = new ArrayList<>();
        List<StateCovariance> covariances = new ArrayList<>();

        final StateCovariance initialCovariance = getDefaultCovariance(initDate, inertialFrame);

        for (double t = 0; t <= dt; t += timeStep) {
            final AbsoluteDate    currentDate       = initDate.shiftedBy(t);
            final SpacecraftState currentState      = propagator.propagate(currentDate);
            final StateCovariance currentCovariance = initialCovariance.shiftedBy(initialState, t);
            states.add(currentState);
            covariances.add(currentCovariance);
        }

        // Create state interpolator
        final int nbInterpolationPoints = 5;
        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(nbInterpolationPoints, inertialFrame, inertialFrame);

        // Create covariance interpolators
        final SmoothStepFactory.SmoothStepFunction blendingFunction = SmoothStepFactory.getQuadratic();

        final OrbitBlender orbitBlender =
                new OrbitBlender(blendingFunction, new KeplerianPropagator(initialState), inertialFrame);

        final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceBlender =
                new StateCovarianceBlender(blendingFunction, orbitBlender, inertialFrame, OrbitType.CARTESIAN,
                                           PositionAngleType.MEAN);

        final TimeInterpolator<TimeStampedPair<Orbit, StateCovariance>> covarianceHermite =
                new StateCovarianceKeplerianHermiteInterpolator(2, orbitBlender, CartesianDerivativesFilter.USE_PVA,
                                                                inertialFrame, OrbitType.CARTESIAN, PositionAngleType.MEAN);

        Ephemeris ephemerisUsingBlender = new Ephemeris(states, stateInterpolator, covariances, covarianceBlender);
        Ephemeris ephemerisUsingHermite = new Ephemeris(states, stateInterpolator, covariances, covarianceHermite);

        // When
        final AbsoluteDate initialDateShiftedByHalfTimeStep = initDate.shiftedBy(timeStep * 0.5);
        final AbsoluteDate initialDateShiftedByTimeStep     = initDate.shiftedBy(timeStep);

        final StateCovariance blendedCovarianceAtT0 = ephemerisUsingBlender.getCovariance(initDate).get();
        final StateCovariance blendedCovarianceBetweenT0AndT1 =
                ephemerisUsingBlender.getCovariance(initialDateShiftedByHalfTimeStep).get();
        final StateCovariance blendedCovarianceAtT1 =
                ephemerisUsingBlender.getCovariance(initialDateShiftedByTimeStep).get();

        final StateCovariance hermiteCovarianceAtT0 = ephemerisUsingHermite.getCovariance(initDate).get();
        final StateCovariance hermiteCovarianceBetweenT0AndT1 =
                ephemerisUsingHermite.getCovariance(initialDateShiftedByHalfTimeStep).get();
        final StateCovariance hermiteCovarianceAtT1 =
                ephemerisUsingHermite.getCovariance(initialDateShiftedByTimeStep).get();

        // Then
        final RealMatrix initialMatrix = initialCovariance.getMatrix();
        final RealMatrix initialMatrixShiftedByHalfTimeStep =
                initialCovariance.shiftedBy(initialState, timeStep * 0.5).getMatrix();
        final RealMatrix initialMatrixShiftedByTimeStep =
                initialCovariance.shiftedBy(initialState, timeStep).getMatrix();
        final RealMatrix zeroMatrix =
                MatrixUtils.createRealMatrix(StateCovariance.STATE_DIMENSION, StateCovariance.STATE_DIMENSION);

        StateCovarianceTest.compareCovariance(zeroMatrix,
                                              blendedCovarianceAtT0.getMatrix().subtract(initialMatrix),
                                              1e-11);
        StateCovarianceTest.compareCovariance(zeroMatrix,
                                              blendedCovarianceBetweenT0AndT1.getMatrix()
                                                                             .subtract(initialMatrixShiftedByHalfTimeStep),
                                              1e-9);
        StateCovarianceTest.compareCovariance(zeroMatrix, blendedCovarianceAtT1.getMatrix()
                                                                               .subtract(initialMatrixShiftedByTimeStep),
                                              1e-9);

        StateCovarianceTest.compareCovariance(zeroMatrix,
                                              hermiteCovarianceAtT0.getMatrix().subtract(initialMatrix),
                                              1e-11);
        StateCovarianceTest.compareCovariance(zeroMatrix,
                                              hermiteCovarianceBetweenT0AndT1.getMatrix()
                                                                             .subtract(initialMatrixShiftedByHalfTimeStep),
                                              1e-9);
        StateCovarianceTest.compareCovariance(zeroMatrix, hermiteCovarianceAtT1.getMatrix()
                                                                               .subtract(initialMatrixShiftedByTimeStep),
                                              1e-8);

    }

    private StateCovariance getDefaultCovariance(final AbsoluteDate date, final Frame frame) {

        final RealMatrix covarianceMatrix = new BlockRealMatrix(new double[][] { { 1, 0, 0, 0, 0, 0 },
                                                                                 { 0, 1, 0, 0, 0, 0 },
                                                                                 { 0, 0, 1, 0, 0, 0 },
                                                                                 { 0, 0, 0, 1e-3, 0, 0 },
                                                                                 { 0, 0, 0, 0, 1e-3, 0 },
                                                                                 { 0, 0, 0, 0, 0, 1e-3 }, });

        return new StateCovariance(covarianceMatrix, date, frame, OrbitType.CARTESIAN, PositionAngleType.MEAN);
    }

    @Test
    public void testExtrapolation() {
        setUp();

        double                dt       = finalDate.durationFrom(initDate);
        double                timeStep = dt / 20.0;
        List<SpacecraftState> states   = new ArrayList<>();

        for (double t = 0; t <= dt; t += timeStep) {
            states.add(propagator.propagate(initDate.shiftedBy(t)));
        }

        // Create interpolator
        final int nbInterpolationPoints = 5;
        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(nbInterpolationPoints, inertialFrame, inertialFrame);

        Ephemeris ephemeris = new Ephemeris(states, interpolator);
        Assertions.assertEquals(finalDate, ephemeris.getMaxDate());

        double tolerance = interpolator.getExtrapolationThreshold();

        final TimeStampedPVCoordinates interpolatedPV =
                ephemeris.getPVCoordinates(initDate.shiftedBy(timeStep), inertialFrame);

        final TimeStampedPVCoordinates propagatedPV = states.get(1).getPVCoordinates();

        // Assert getPVCoordinates method
        AbsolutePVCoordinatesTest.assertPV(propagatedPV, interpolatedPV, 1e-8);

        // Assert getFrame method
        Assertions.assertEquals(inertialFrame, ephemeris.getFrame());

        ephemeris.propagate(ephemeris.getMinDate());
        ephemeris.propagate(ephemeris.getMaxDate());
        ephemeris.propagate(ephemeris.getMinDate().shiftedBy(-tolerance / 2.0));
        ephemeris.propagate(ephemeris.getMaxDate().shiftedBy(tolerance / 2.0));

        try {
            ephemeris.propagate(ephemeris.getMinDate().shiftedBy(-2.0 * tolerance));
            Assertions.fail("an exception should have been thrown");
        }
        catch (TimeStampedCacheException e) {
            //supposed to fail since out of bounds
        }

        try {
            ephemeris.propagate(ephemeris.getMaxDate().shiftedBy(2.0 * tolerance));
            Assertions.fail("an exception should have been thrown");
        }
        catch (TimeStampedCacheException e) {
            //supposed to fail since out of bounds
        }
    }

    @Test
    public void testIssue662() {
        setUp();

        // Input parameters
        int    numberOfIntervals = 1440;
        double deltaT            = finalDate.durationFrom(initDate) / ((double) numberOfIntervals);

        // Build the list of spacecraft states
        String                additionalName  = "testValue";
        double                additionalValue = 1.0;
        List<SpacecraftState> states          = new ArrayList<>(numberOfIntervals + 1);
        for (int j = 0; j <= numberOfIntervals; j++) {
            states.add(propagator.propagate(initDate.shiftedBy((j * deltaT)))
                                 .addAdditionalState(additionalName, additionalValue));
        }

        // Build the ephemeris propagator
        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(inertialFrame);

        Ephemeris ephemPropagator = new Ephemeris(states, interpolator);

        // State before adding an attitude provider
        SpacecraftState stateBefore = ephemPropagator.propagate(ephemPropagator.getMaxDate().shiftedBy(-60.0));
        Assertions.assertEquals(1, stateBefore.getAdditionalState(additionalName).length);
        Assertions.assertEquals(additionalValue, stateBefore.getAdditionalState(additionalName)[0], Double.MIN_VALUE);

        // Set an attitude provider
        ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));

        // State after adding an attitude provider
        SpacecraftState stateAfter = ephemPropagator.propagate(ephemPropagator.getMaxDate().shiftedBy(-60.0));
        Assertions.assertEquals(1, stateAfter.getAdditionalState(additionalName).length);
        Assertions.assertEquals(additionalValue, stateAfter.getAdditionalState(additionalName)[0], Double.MIN_VALUE);

    }

    @Test
    public void testIssue680() {
        setUp();

        // Initial PV coordinates
        AbsolutePVCoordinates initPV = new AbsolutePVCoordinates(inertialFrame,
                                                                 new TimeStampedPVCoordinates(initDate,
                                                                                              new PVCoordinates(new Vector3D(
                                                                                                      -29536113.0,
                                                                                                      30329259.0, -100125.0),
                                                                                                                new Vector3D(
                                                                                                                        -2194.0,
                                                                                                                        -2141.0,
                                                                                                                        -8.0))));
        // Input parameters
        int    numberOfIntervals = 1440;
        double deltaT            = finalDate.durationFrom(initDate) / ((double) numberOfIntervals);

        // Build the list of spacecraft states
        List<SpacecraftState> states = new ArrayList<>(numberOfIntervals + 1);
        for (int j = 0; j <= numberOfIntervals; j++) {
            states.add(new SpacecraftState(initPV).shiftedBy(j * deltaT));
        }

        // Build the ephemeris propagator
        // Create interpolator
        final TimeInterpolator<SpacecraftState> interpolator = new SpacecraftStateInterpolator(inertialFrame);

        Ephemeris ephemPropagator = new Ephemeris(states, interpolator);

        // Get initial state without attitude provider
        SpacecraftState withoutAttitudeProvider = ephemPropagator.getInitialState();
        Assertions.assertEquals(0.0,
                                Vector3D.distance(withoutAttitudeProvider.getAbsPVA().getPosition(), initPV.getPosition()),
                                1.0e-10);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(withoutAttitudeProvider.getAbsPVA().getVelocity(), initPV.getVelocity()),
                                1.0e-10);

        // Set an attitude provider
        ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.LVLH_CCSDS));

        // Get initial state with attitude provider
        SpacecraftState withAttitudeProvider = ephemPropagator.getInitialState();
        Assertions.assertEquals(0.0, Vector3D.distance(withAttitudeProvider.getAbsPVA().getPosition(), initPV.getPosition()),
                                1.0e-10);
        Assertions.assertEquals(0.0, Vector3D.distance(withAttitudeProvider.getAbsPVA().getVelocity(), initPV.getVelocity()),
                                1.0e-10);

    }

    /** Error with specific propagators & additional state provider throwing a NullPointerException when propagating */
    @Test
    public void testIssue949() {
        // GIVEN
        final AbsoluteDate initialDate  = new AbsoluteDate();
        final Orbit        initialOrbit = TestUtils.getDefaultOrbit(initialDate);

        // Setup propagator
        final Orbit                 finalOrbit = initialOrbit.shiftedBy(1);
        final List<SpacecraftState> states     = new ArrayList<>();
        states.add(new SpacecraftState(initialOrbit));
        states.add(new SpacecraftState(finalOrbit));

        final Ephemeris ephemeris = new Ephemeris(states, 2);

        // Setup additional state provider which use the initial state in its init method
        final AdditionalStateProvider additionalStateProvider = TestUtils.getAdditionalProviderWithInit();
        ephemeris.addAdditionalStateProvider(additionalStateProvider);

        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> ephemeris.propagate(ephemeris.getMaxDate()), "No error should have been thrown");

    }

    @Test
    @Timeout(value = 5000, unit = TimeUnit.SECONDS)
    void testIssue1277() {
        // GIVEN
        // Create orbit
        double       mu    = Constants.IERS2010_EARTH_MU;
        AbsoluteDate date  = new AbsoluteDate();
        Frame        frame = FramesFactory.getEME2000();
        Orbit orbit = new KeplerianOrbit(6378000 + 500000, 0.01, FastMath.toRadians(15), 0, 0, 0,
                                         PositionAngleType.MEAN, frame,date, mu);
        // Create propagator
        Propagator propagator = new KeplerianPropagator(orbit);

        // Add step handler for saving states
        List<SpacecraftState> states = new ArrayList<>();
        propagator.getMultiplexer().add(10, states::add);

        // Run propagation over one week
        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY * 7));

        // Create event detector
        GeodeticPoint    station = new GeodeticPoint(0, 0, 0);
        Frame            itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid wgs84   = ReferenceEllipsoid.getWgs84(itrf);
        TopocentricFrame topo    = new TopocentricFrame(wgs84, station, "station");

        RecordAndContinue handler = new RecordAndContinue();
        ElevationDetector detector = new ElevationDetector(topo).withConstantElevation(FastMath.toRadians(5))
                                                                .withRefraction(new EarthStandardAtmosphereRefraction())
                                                                .withMaxCheck(60).withHandler(handler);

        // Create ephemeris from states
        Ephemeris ephemeris = new Ephemeris(states, 4);

        // Add detector
        ephemeris.addEventDetector(detector);

        // WHEN & THEN
        ephemeris.propagate(ephemeris.getMinDate(), ephemeris.getMaxDate());
    }

    public void setUp() throws IllegalArgumentException, OrekitException {
        Utils.setDataRoot("regular-data");

        initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

        finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                     TimeComponents.H00,
                                     TimeScalesFactory.getUTC());

        double a     = 7187990.1979844316;
        double e     = 0.5e-4;
        double i     = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv    = 0;
        double mu    = 3.9860047e14;
        inertialFrame = FramesFactory.getEME2000();

        initialState = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                          inertialFrame, initDate, mu);
        propagator   = new KeplerianPropagator(initialState);

    }

    private double calculatePositionDelta(SpacecraftState state1, SpacecraftState state2) {
        return Vector3D.distance(state1.getPosition(), state2.getPosition());
    }

    private double calculateVelocityDelta(SpacecraftState state1, SpacecraftState state2) {
        return Vector3D.distance(state1.getPVCoordinates().getVelocity(), state2.getPVCoordinates().getVelocity());
    }

    private double calculateAttitudeDelta(SpacecraftState state1, SpacecraftState state2) {
        return Rotation.distance(state1.getAttitude().getRotation(), state2.getAttitude().getRotation());
    }

}
