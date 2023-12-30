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
package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeInterpolator;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.SingleBodyAbsoluteAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitHermiteInterpolator;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.time.TimeStampedDouble;
import org.orekit.time.TimeStampedDoubleHermiteInterpolator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AbsolutePVCoordinatesHermiteInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SpacecraftStateInterpolatorTest {

    private double                mass;
    private Orbit                 orbit;
    private AbsolutePVCoordinates absPV;
    private AttitudeProvider      attitudeLaw;
    private Propagator            orbitPropagator;
    private Propagator            absPVPropagator;

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data:potential/icgem-format");
            double mu  = 3.9860047e14;
            double ae  = 6.378137e6;
            double c20 = -1.08263e-3;
            double c30 = 2.54e-6;
            double c40 = 1.62e-6;
            double c50 = 2.3e-7;
            double c60 = -5.5e-7;

            mass = 2500;
            double a     = 7187990.1979844316;
            double e     = 0.5e-4;
            double i     = 1.7105407051081795;
            double omega = 1.9674147913622104;
            double OMEGA = FastMath.toRadians(261);
            double lv    = 0;

            AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                    TimeComponents.H00,
                    TimeScalesFactory.getUTC());
            final Frame frame = FramesFactory.getEME2000();
            orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE, frame, date, mu);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            absPV = new AbsolutePVCoordinates(frame, date, orbit.getPVCoordinates());

            attitudeLaw     = new BodyCenterPointing(orbit.getFrame(), earth);
            orbitPropagator =
                    new EcksteinHechlerPropagator(orbit, attitudeLaw, mass,
                            ae, mu, c20, c30, c40, c50, c60);

            absPVPropagator = setUpNumericalPropagator();

        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        mass            = Double.NaN;
        orbit           = null;
        attitudeLaw     = null;
        orbitPropagator = null;
    }

    @Test
    public void testOrbitInterpolation()
            throws OrekitException {

        // Given
        final int interpolationPoints1 = 2;
        final int interpolationPoints2 = 3;
        final int interpolationPoints3 = 4;

        final Frame intertialFrame = FramesFactory.getEME2000();

        // When & Then
        // Define state interpolators
        final SpacecraftStateInterpolator interpolator1 =
                new SpacecraftStateInterpolator(interpolationPoints1, intertialFrame, intertialFrame);

        final SpacecraftStateInterpolator interpolator2 =
                new SpacecraftStateInterpolator(interpolationPoints2, intertialFrame, intertialFrame);

        final SpacecraftStateInterpolator interpolator3 =
                new SpacecraftStateInterpolator(interpolationPoints3, intertialFrame, intertialFrame);

        // When & Then
        checkInterpolationError(interpolationPoints1, 106.46533, 0.40709287, 169847806.33e-9, 0.0, 450 * 450, 450 * 450,
                interpolator1);
        checkInterpolationError(interpolationPoints2, 0.00353, 0.00003250, 189886.01e-9, 0.0, 0.0, 0.0, interpolator2);
        checkInterpolationError(interpolationPoints3, 0.00002, 0.00000023, 232.25e-9, 0.0, 0.0, 0.0, interpolator3);
    }

    @Test
    public void testAbsPVAInterpolation()
            throws OrekitException {

        // Given
        final int interpolationPoints1 = 2;
        final int interpolationPoints2 = 3;
        final int interpolationPoints3 = 4;

        final Frame intertialFrame = absPV.getFrame();

        // Create interpolator with different number of interpolation points and derivative filters (P/R, PV/RR, PVA/RRR)
        final SpacecraftStateInterpolator[] interpolator1 =
                buildAllTypeOfInterpolator(interpolationPoints1, intertialFrame);
        final SpacecraftStateInterpolator[] interpolator2 =
                buildAllTypeOfInterpolator(interpolationPoints2, intertialFrame);
        final SpacecraftStateInterpolator[] interpolator3 =
                buildAllTypeOfInterpolator(interpolationPoints3, intertialFrame);

        // P and R
        checkAbsPVInterpolationError(interpolationPoints1, 766704.6033758943, 3385.895505018284,
                9.503905101141868, 0.0, interpolator1[0]);
        checkAbsPVInterpolationError(interpolationPoints2, 46190.78568215623, 531.3506621730367,
                0.5601906427491941, 0, interpolator2[0]);
        checkAbsPVInterpolationError(interpolationPoints3, 2787.7069621834926, 55.5146607205871,
                0.03372344505743245, 0.0, interpolator3[0]);

        // PV and RR
        checkAbsPVInterpolationError(interpolationPoints1, 14023.999059896296, 48.022197580401084,
                0.16984517369482555, 0.0, interpolator1[1]);
        checkAbsPVInterpolationError(interpolationPoints2, 16.186825338590722, 0.13418685366189476,
                1.898961129289559E-4, 0, interpolator2[1]);
        checkAbsPVInterpolationError(interpolationPoints3, 0.025110113133073413, 3.5069332429486154E-4,
                2.3306042475258594E-7, 0.0, interpolator3[1]);

        // PVA and RRR
        checkAbsPVInterpolationError(interpolationPoints1, 108.13907262943746, 0.4134494277844817,
                0.001389170843175492, 0.0, interpolator1[2]);
        checkAbsPVInterpolationError(interpolationPoints2, 0.002974408269435121, 2.6937387601886076E-5,
                2.051629855188969E-4, 0, interpolator2[2]);
        checkAbsPVInterpolationError(interpolationPoints3, 0, 0,
                1.3779131041190534E-4, 0.0, interpolator3[2]);
    }

    /**
     * Set up a numerical propagator for spacecraft state defined by an absolute position-velocity-acceleration. It is
     * designed to be similar to the EcksteinHechler propagator.
     * <p>
     * It has attraction towards Earth + 6x6 earth potential as forces.
     *
     * @return numerical propagator for spacecraft state defined by an absolute position-velocity-acceleration
     */
    private Propagator setUpNumericalPropagator() {

        final ODEIntegrator integrator = setUpIntegrator();

        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        // Configure propagator
        propagator.setOrbitType(null);

        // Add force models
        final Frame                                itrf      = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final NormalizedSphericalHarmonicsProvider provider  = GravityFieldFactory.getNormalizedProvider(6, 6);
        final HolmesFeatherstoneAttractionModel    potential = new HolmesFeatherstoneAttractionModel(itrf, provider);

        propagator.addForceModel(potential);
        propagator.addForceModel(new SingleBodyAbsoluteAttraction(CelestialBodyFactory.getEarth()));

        // Set initial state
        final SpacecraftState initialState = new SpacecraftState(absPV);

        propagator.setInitialState(initialState);

        // Set attitude law
        propagator.setAttitudeProvider(attitudeLaw);

        return propagator;
    }

    /**
     * Set up default integrator for numerical propagator
     *
     * @return default integrator for numerical propagator
     */
    private ODEIntegrator setUpIntegrator() {
        final double     dP         = 1;
        final double     minStep    = 0.001;
        final double     maxStep    = 100;
        final double[][] tolerances = NumericalPropagator.tolerances(dP, absPV);

        return new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
    }

    /**
     * Build spacecraft state Hermite interpolators using all kind of position-velocity-acceleration derivatives and angular
     * derivatives filter.
     *
     * @param interpolationPoints number of interpolation points
     * @param inertialFrame       inertial frame
     * @return array of spacecraft state Hermite interpolators containing all possible configuration (3 in total)
     */
    private SpacecraftStateInterpolator[] buildAllTypeOfInterpolator(final int interpolationPoints,
                                                                     final Frame inertialFrame) {

        final CartesianDerivativesFilter[] pvaFilters     = CartesianDerivativesFilter.values();
        final AngularDerivativesFilter[]   angularFilters = AngularDerivativesFilter.values();

        final int                           dim           = pvaFilters.length;
        final SpacecraftStateInterpolator[] interpolators = new SpacecraftStateInterpolator[dim];

        for (int i = 0; i < dim; i++) {
            interpolators[i] = new SpacecraftStateInterpolator(interpolationPoints,
                    AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                    inertialFrame, inertialFrame,
                    pvaFilters[i], angularFilters[i]);
        }

        return interpolators;
    }

    /**
     * Check interpolation error for position, velocity , attitude, mass, additional state and associated derivatives. This
     * method was designed to test interpolation on orbit defined spacecraft states.
     *
     * @param n              sample size
     * @param expectedErrorP expected position error
     * @param expectedErrorV expected velocity error
     * @param expectedErrorA expected attitude error
     * @param expectedErrorM expected mass error
     * @param expectedErrorQ expected additional state error
     * @param expectedErrorD expected additional state derivative error
     * @param interpolator   state interpolator
     */
    private void checkInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                         double expectedErrorA, double expectedErrorM,
                                         double expectedErrorQ, double expectedErrorD,
                                         final TimeInterpolator<SpacecraftState> interpolator) {
        AbsoluteDate          centerDate = orbit.getDate().shiftedBy(100.0);
        List<SpacecraftState> sample     = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            double          dt    = i * 900.0 / (n - 1);
            SpacecraftState state = orbitPropagator.propagate(centerDate.shiftedBy(dt));
            state = state.
                    addAdditionalState("quadratic", dt * dt).
                    addAdditionalStateDerivative("quadratic-dot", dt * dt);
            sample.add(state);
        }

        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        double maxErrorQ = 0;
        double maxErrorD = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            SpacecraftState interpolated = interpolator.interpolate(centerDate.shiftedBy(dt), sample);
            SpacecraftState propagated   = orbitPropagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates   dpv          = new PVCoordinates(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm());
            maxErrorA =
                    FastMath.max(maxErrorA, FastMath.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation(),
                            propagated.getAttitude().getRotation())));
            maxErrorM = FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass() - propagated.getMass()));
            maxErrorQ = FastMath.max(maxErrorQ, FastMath.abs(interpolated.getAdditionalState("quadratic")[0] - dt * dt));
            maxErrorD = FastMath.max(maxErrorD,
                    FastMath.abs(interpolated.getAdditionalStateDerivative("quadratic-dot")[0] - dt * dt));
        }
        Assertions.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assertions.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assertions.assertEquals(expectedErrorA, maxErrorA, 4.0e-10);
        Assertions.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
        Assertions.assertEquals(expectedErrorQ, maxErrorQ, 2.0e-10);
        Assertions.assertEquals(expectedErrorD, maxErrorD, 2.0e-10);
    }

    /**
     * Check interpolation error for position, velocity , attitude and mass only. This method was designed to test
     * interpolation on spacecraft states defined by absolute position-velocity-acceleration errors for better code
     * coverage.
     *
     * @param n              sample size
     * @param expectedErrorP expected position error
     * @param expectedErrorV expected velocity error
     * @param expectedErrorA expected attitude error
     * @param expectedErrorM expected mass error
     * @param interpolator   state interpolator
     */
    private void checkAbsPVInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                              double expectedErrorA, double expectedErrorM,
                                              final TimeInterpolator<SpacecraftState> interpolator) {
        AbsoluteDate          centerDate = absPV.getDate().shiftedBy(100.0);
        List<SpacecraftState> sample     = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            double          dt    = i * 900.0 / (n - 1);
            SpacecraftState state = absPVPropagator.propagate(centerDate.shiftedBy(dt));
            state = state.
                    addAdditionalState("quadratic", dt * dt).
                    addAdditionalStateDerivative("quadratic-dot", dt * dt);
            sample.add(state);
        }

        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            SpacecraftState interpolated = interpolator.interpolate(centerDate.shiftedBy(dt), sample);
            SpacecraftState propagated   = absPVPropagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates   dpv          = new PVCoordinates(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm());
            maxErrorA =
                    FastMath.max(maxErrorA, FastMath.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation(),
                            propagated.getAttitude().getRotation())));
            maxErrorM = FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass() - propagated.getMass()));
        }
        Assertions.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assertions.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assertions.assertEquals(expectedErrorA, maxErrorA, 4.0e-10);
        Assertions.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
    }

    @Test
    @DisplayName("test error thrown when using different state definition")
    void testErrorThrownWhenUsingDifferentStateDefinition() {
        // Given
        final AbsoluteDate interpolationDate = new AbsoluteDate();

        final SpacecraftState orbitDefinedState = Mockito.mock(SpacecraftState.class);
        final SpacecraftState absPVDefinedState = Mockito.mock(SpacecraftState.class);

        Mockito.when(orbitDefinedState.isOrbitDefined()).thenReturn(true);
        Mockito.when(absPVDefinedState.isOrbitDefined()).thenReturn(false);

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(orbitDefinedState);
        states.add(absPVDefinedState);

        // Create interpolator
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        final TimeInterpolator<SpacecraftState> stateInterpolator =
                new SpacecraftStateInterpolator(2, inertialFrameMock, inertialFrameMock);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                () -> stateInterpolator.interpolate(interpolationDate, states));

        Assertions.assertEquals(
                "one state is defined using an orbit while the other is defined using an absolute position-velocity-acceleration",
                thrown.getMessage());
    }

    @Test
    @DisplayName("test error thrown when using different state definition")
    void testErrorThrownWhenGivingNoInterpolatorForState() {
        // Given
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                () -> new SpacecraftStateInterpolator(
                        AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                        AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                        inertialFrameMock, null, null,
                        null, null, null));

        Assertions.assertEquals("creating a spacecraft state interpolator requires at least one orbit interpolator or an "
                + "absolute position-velocity-acceleration interpolator", thrown.getMessage());
    }

    @Test
    @DisplayName("test error thrown when giving wrong sample type for interpolation")
    void testErrorThrownWhenGivingWrongSampleTypeForInterpolator() {
        // Given
        final AbsoluteDate interpolationDate = new AbsoluteDate();

        final Frame inertialFrame = FramesFactory.getEME2000();

        final SpacecraftState absPVDefinedState1 = Mockito.mock(SpacecraftState.class);
        final SpacecraftState absPVDefinedState2 = Mockito.mock(SpacecraftState.class);

        Mockito.when(absPVDefinedState1.isOrbitDefined()).thenReturn(false);
        Mockito.when(absPVDefinedState2.isOrbitDefined()).thenReturn(false);

        Mockito.when(absPVDefinedState1.getDate()).thenReturn(interpolationDate);
        Mockito.when(absPVDefinedState2.getDate()).thenReturn(interpolationDate.shiftedBy(1));

        final List<SpacecraftState> states = new ArrayList<>();
        states.add(absPVDefinedState1);
        states.add(absPVDefinedState2);

        // Create interpolator
        @SuppressWarnings("unchecked") final TimeInterpolator<Orbit> orbitInterpolatorMock = Mockito.mock(TimeInterpolator.class);

        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                        AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                        inertialFrame, orbitInterpolatorMock, null, null, null, null);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class, () ->
                interpolator.interpolate(interpolationDate, states));

        Assertions.assertEquals("wrong interpolator defined for this spacecraft state type (orbit or absolute PV)",
                thrown.getMessage());
    }

    @Test
    void testGetNbInterpolationsWithMultipleSubInterpolators() {
        // GIVEN
        // Create mock interpolators
        final Frame frame = Mockito.mock(Frame.class);

        final TimeInterpolator<Orbit> orbitInterpolator = Mockito.mock(OrbitHermiteInterpolator.class);

        final TimeInterpolator<AbsolutePVCoordinates> absPVAInterpolator =
                Mockito.mock(AbsolutePVCoordinatesHermiteInterpolator.class);

        final TimeInterpolator<TimeStampedDouble> massInterpolator =
                Mockito.mock(TimeStampedDoubleHermiteInterpolator.class);

        final TimeInterpolator<Attitude> attitudeInterpolator = Mockito.mock(AttitudeInterpolator.class);

        final TimeInterpolator<TimeStampedDouble> additionalStateInterpolator =
                Mockito.mock(TimeStampedDoubleHermiteInterpolator.class);

        // Implement mocks behaviours
        final int orbitNbInterpolationPoints           = 2;
        final int absPVANbInterpolationPoints          = 3;
        final int massNbInterpolationPoints            = 4;
        final int AttitudeNbInterpolationPoints        = 5;
        final int AdditionalStateNbInterpolationPoints = 6;

        Mockito.when(orbitInterpolator.getNbInterpolationPoints()).thenReturn(orbitNbInterpolationPoints);
        Mockito.when(absPVAInterpolator.getNbInterpolationPoints()).thenReturn(absPVANbInterpolationPoints);
        Mockito.when(massInterpolator.getNbInterpolationPoints()).thenReturn(massNbInterpolationPoints);
        Mockito.when(attitudeInterpolator.getNbInterpolationPoints()).thenReturn(AttitudeNbInterpolationPoints);
        Mockito.when(additionalStateInterpolator.getNbInterpolationPoints()).thenReturn(AdditionalStateNbInterpolationPoints);

        Mockito.when(orbitInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(orbitInterpolator));
        Mockito.when(absPVAInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(absPVAInterpolator));
        Mockito.when(massInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(massInterpolator));
        Mockito.when(attitudeInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(attitudeInterpolator));
        Mockito.when(additionalStateInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(additionalStateInterpolator));

        final SpacecraftStateInterpolator stateInterpolator =
                new SpacecraftStateInterpolator(frame, orbitInterpolator, absPVAInterpolator, massInterpolator,
                        attitudeInterpolator, additionalStateInterpolator);

        // WHEN
        final int returnedNbInterpolationPoints = stateInterpolator.getNbInterpolationPoints();

        // THEN
        Assertions.assertEquals(AdditionalStateNbInterpolationPoints, returnedNbInterpolationPoints);
    }

    @Test
    @DisplayName("test error thrown when giving empty sample")
    void testErrorThrownWhenGivingEmptySample() {
        // Given
        final AbsoluteDate interpolationDate = new AbsoluteDate();

        final Frame inertialFrame = FramesFactory.getEME2000();

        final List<SpacecraftState> states = new ArrayList<>();

        // Create interpolator
        @SuppressWarnings("unchecked") final TimeInterpolator<Orbit> orbitInterpolatorMock = Mockito.mock(TimeInterpolator.class);

        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                        AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                        inertialFrame, orbitInterpolatorMock, null, null, null, null);

        // When & Then
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class, () ->
                interpolator.interpolate(interpolationDate, states));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(0, ((Integer) thrown.getParts()[0]).intValue());

    }

    @Test
    void testSpacecraftStateInterpolatorCreation() {
        // Given
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        @SuppressWarnings("unchecked") final TimeInterpolator<Orbit> orbitInterpolatorMock =
                Mockito.mock(TimeInterpolator.class);

        @SuppressWarnings("unchecked") final TimeInterpolator<AbsolutePVCoordinates> absPVInterpolatorMock =
                Mockito.mock(TimeInterpolator.class);

        @SuppressWarnings("unchecked") final TimeInterpolator<TimeStampedDouble> massInterpolatorMock =
                Mockito.mock(TimeInterpolator.class);

        @SuppressWarnings("unchecked") final TimeInterpolator<Attitude> attitudeInterpolatorMock =
                Mockito.mock(TimeInterpolator.class);

        @SuppressWarnings("unchecked") final TimeInterpolator<TimeStampedDouble> additionalInterpolatorMock =
                Mockito.mock(TimeInterpolator.class);

        // When
        final SpacecraftStateInterpolator interpolator =
                new SpacecraftStateInterpolator(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                        AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                        inertialFrameMock, orbitInterpolatorMock, absPVInterpolatorMock,
                        massInterpolatorMock, attitudeInterpolatorMock, additionalInterpolatorMock);

        // Then
        Assertions.assertEquals(inertialFrameMock, interpolator.getOutputFrame());
        Assertions.assertEquals(orbitInterpolatorMock, interpolator.getOrbitInterpolator().get());
        Assertions.assertEquals(absPVInterpolatorMock, interpolator.getAbsPVAInterpolator().get());
        Assertions.assertEquals(massInterpolatorMock, interpolator.getMassInterpolator().get());
        Assertions.assertEquals(attitudeInterpolatorMock, interpolator.getAttitudeInterpolator().get());
        Assertions.assertEquals(additionalInterpolatorMock, interpolator.getAdditionalStateInterpolator().get());

    }

    @Test
    void testIssue1266() {
        // Given
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);
        final int    interpolationPoints    = 3;
        final double extrapolationThreshold = 10;

        // When
        final SpacecraftStateInterpolator interpolator =
                new SpacecraftStateInterpolator(interpolationPoints, extrapolationThreshold,
                        inertialFrameMock, inertialFrameMock);

        // Then
        Assertions.assertEquals(extrapolationThreshold, interpolator.getExtrapolationThreshold());

    }

    @Test
    @DisplayName("Test error thrown when sub interpolator is not present")
    void testErrorThrownWhenSubInterpolatorIsNotPresent() {
        // GIVEN
        final FakeStateInterpolator fakeStateInterpolator = new FakeStateInterpolator();

        // WHEN & THEN
        Assertions.assertThrows(OrekitInternalError.class, fakeStateInterpolator::getNbInterpolationPoints);
    }

    private static class FakeStateInterpolator extends AbstractTimeInterpolator<SpacecraftState> {

        public FakeStateInterpolator() {
            super(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                  AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
        }

        @Override
        protected SpacecraftState interpolate(AbstractTimeInterpolator<SpacecraftState>.InterpolationData interpolationData) {
            return null;
        }

        @Override
        public List<TimeInterpolator<? extends TimeStamped>> getSubInterpolators() {
            return Collections.emptyList();
        }
    }
}