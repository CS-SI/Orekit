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

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldAttitudeInterpolator;
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
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.FieldOrbitHermiteInterpolator;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractFieldTimeInterpolator;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStampedField;
import org.orekit.time.TimeStampedFieldHermiteInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinatesHermiteInterpolator;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FieldSpacecraftStateInterpolatorTest {

    final private Field<Binary64>                      field = Binary64Field.getInstance();
    private       Binary64                             mass;
    private       FieldOrbit<Binary64>                 orbit;
    private       FieldAbsolutePVCoordinates<Binary64> absPV;
    private       AttitudeProvider                     attitudeLaw;
    private       FieldPropagator<Binary64>            analyticalPropagator;
    private       FieldPropagator<Binary64>            absPVPropagator;

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data:potential/icgem-format");
            Binary64 mu  = new Binary64(3.9860047e14);
            double   ae  = 6.378137e6;
            double   c20 = -1.08263e-3;
            double   c30 = 2.54e-6;
            double   c40 = 1.62e-6;
            double   c50 = 2.3e-7;
            double   c60 = -5.5e-7;

            mass = new Binary64(2500);
            Binary64 a     = new Binary64(7187990.1979844316);
            Binary64 e     = new Binary64(0.5e-4);
            Binary64 i     = new Binary64(1.7105407051081795);
            Binary64 omega = new Binary64(1.9674147913622104);
            Binary64 OMEGA = new Binary64(FastMath.toRadians(261));
            Binary64 lv    = new Binary64(0);

            FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                                       TimeComponents.H00,
                                                                       TimeScalesFactory.getUTC());
            final Frame frame = FramesFactory.getEME2000();

            orbit = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                              frame, date, mu);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                          Constants.WGS84_EARTH_FLATTENING,
                                                          FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            absPV = new FieldAbsolutePVCoordinates<>(frame, date, orbit.getPVCoordinates());

            attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

            analyticalPropagator =
                    new FieldEcksteinHechlerPropagator<>(orbit, attitudeLaw, mass,
                                                         ae, mu, c20, c30, c40, c50, c60);

            absPVPropagator = setUpNumericalPropagator();

        }
        catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        mass                 = org.hipparchus.util.Binary64.NAN;
        orbit                = null;
        attitudeLaw          = null;
        analyticalPropagator = null;
    }

    @Test
    public void testOrbitInterpolation()
            throws OrekitException {

        // Given
        final Frame inertialFrame = orbit.getFrame();

        final FieldSpacecraftStateInterpolator<Binary64> interpolator1 =
                new FieldSpacecraftStateInterpolator<>(2, inertialFrame);

        final FieldSpacecraftStateInterpolator<Binary64> interpolator2 =
                new FieldSpacecraftStateInterpolator<>(3, inertialFrame);

        final FieldSpacecraftStateInterpolator<Binary64> interpolator3 =
                new FieldSpacecraftStateInterpolator<>(4, inertialFrame);

        // When & Then
        checkStandardInterpolationError(2, 106.46533, 0.40709287, 169847806.33e-9, 0.0, 450 * 450, 450 * 450, interpolator1);
        checkStandardInterpolationError(3, 0.00353, 0.00003250, 189886.01e-9, 0.0, 0.0, 0.0, interpolator2);
        checkStandardInterpolationError(4, 0.00002, 0.00000023, 232.25e-9, 0.0, 0.0, 0.0, interpolator3);

    }

    @Test
    public void testErrorThrownWhenOneInterpolatorIsNotConsistentWithSampleSize() {
        // GIVEN
        final Frame outputFrame = Mockito.mock(Frame.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> orbitInterpolator =
                Mockito.mock(FieldTimeInterpolator.class);
        Mockito.when(orbitInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(orbitInterpolator));
        Mockito.when(orbitInterpolator.getNbInterpolationPoints()).thenReturn(2);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> absPVInterpolator =
                Mockito.mock(FieldTimeInterpolator.class);
        Mockito.when(absPVInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(absPVInterpolator));
        Mockito.when(absPVInterpolator.getNbInterpolationPoints()).thenReturn(4);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> massInterpolator =
                Mockito.mock(FieldTimeInterpolator.class);
        Mockito.when(massInterpolator.getSubInterpolators()).thenReturn(Collections.singletonList(massInterpolator));
        Mockito.when(massInterpolator.getNbInterpolationPoints()).thenReturn(2);

        final FieldSpacecraftStateInterpolator<Binary64> stateInterpolator =
                new FieldSpacecraftStateInterpolator<>(outputFrame, orbitInterpolator, absPVInterpolator, massInterpolator,
                                                       null, null);

        // WHEN & THEN
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class, () ->
                AbstractFieldTimeInterpolator.checkInterpolatorCompatibilityWithSampleSize(stateInterpolator, 2));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(2, ((Integer) thrown.getParts()[0]).intValue());

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
        final FieldSpacecraftStateInterpolator<Binary64>[] interpolator1 =
                buildAllTypeOfInterpolator(interpolationPoints1, intertialFrame);
        final FieldSpacecraftStateInterpolator<Binary64>[] interpolator2 =
                buildAllTypeOfInterpolator(interpolationPoints2, intertialFrame);
        final FieldSpacecraftStateInterpolator<Binary64>[] interpolator3 =
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
        checkAbsPVInterpolationError(interpolationPoints3, 0, 0, 1.3779131041190534E-4,
                                     0.0, interpolator3[2]);
    }

    @Test
    public void testIssue775() {
        final Field<Binary64> field = Binary64Field.getInstance();
        final Binary64        zero  = field.getZero();

        // Conversion from double to Field
        FieldAbsoluteDate<Binary64> initDate = new FieldAbsoluteDate<>(field, new AbsoluteDate(
                new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
        FieldAbsoluteDate<Binary64> finalDate = new FieldAbsoluteDate<>(field, new AbsoluteDate(
                new DateComponents(2004, 01, 02), TimeComponents.H00, TimeScalesFactory.getUTC()));
        Frame inertialFrame = FramesFactory.getEME2000();

        // Initial PV coordinates
        final FieldVector3D<Binary64> position = new FieldVector3D<>(zero.add(-29536113.0),
                                                                     zero.add(30329259.0),
                                                                     zero.add(-100125.0));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(zero.add(-2194.0),
                                                                     zero.add(-2141.0),
                                                                     zero.add(-8.0));

        final FieldPVCoordinates<Binary64>         pv        = new FieldPVCoordinates<>(position, velocity);
        final FieldAbsolutePVCoordinates<Binary64> initAbsPV = new FieldAbsolutePVCoordinates<>(inertialFrame, initDate, pv);

        // Input parameters
        int      numberOfIntervals = 15;
        Binary64 deltaT            = finalDate.durationFrom(initDate).divide(numberOfIntervals);

        // Build the list of spacecraft states
        List<FieldSpacecraftState<Binary64>> states = new ArrayList<>(numberOfIntervals + 1);
        for (int j = 0; j <= numberOfIntervals; j++) {
            states.add(new FieldSpacecraftState<>(initAbsPV).shiftedBy(deltaT.multiply(j)));
        }

        // Get initial state without orbit
        FieldSpacecraftState<Binary64> withoutOrbit;

        // Create orbit Hermite interpolator
        final FieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64> interpolator =
                new FieldSpacecraftStateInterpolator<>(states.size(), inertialFrame);

        // Interpolation
        withoutOrbit = interpolator.interpolate(states.get(10).getDate(), states);
        Assertions.assertEquals(0.0, FieldVector3D.distance(withoutOrbit.getAbsPVA().getPosition(),
                                                            states.get(10).getAbsPVA().getPosition()).getReal(), 1.0e-10);
    }

    /**
     * Set up a numerical propagator for spacecraft state defined by an absolute position-velocity-acceleration. It is
     * designed to be similar to the EcksteinHechler propagator.
     * <p>
     * It has attraction towards Earth + 6x6 earth potential as forces.
     *
     * @return numerical propagator for spacecraft state defined by an absolute position-velocity-acceleration
     */
    private FieldPropagator<Binary64> setUpNumericalPropagator() {

        // Create propagator
        final FieldODEIntegrator<Binary64> integrator = setUpDefaultIntegrator();

        final FieldNumericalPropagator<Binary64> propagator = new FieldNumericalPropagator<>(field, integrator);

        // Configure propagator
        propagator.setOrbitType(null);

        // Add forces
        final Frame                                itrf      = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final NormalizedSphericalHarmonicsProvider provider  = GravityFieldFactory.getNormalizedProvider(6, 6);
        final HolmesFeatherstoneAttractionModel    potential = new HolmesFeatherstoneAttractionModel(itrf, provider);

        propagator.addForceModel(potential);
        propagator.addForceModel(new SingleBodyAbsoluteAttraction(CelestialBodyFactory.getEarth()));

        // Set initial state
        final FieldSpacecraftState<Binary64> initialState = new FieldSpacecraftState<>(absPV);

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
    private FieldODEIntegrator<Binary64> setUpDefaultIntegrator() {
        final Binary64   dP         = new Binary64(1);
        final double     minStep    = 0.001;
        final double     maxStep    = 100;
        final double[][] tolerances = FieldNumericalPropagator.tolerances(dP, orbit, OrbitType.CARTESIAN);

        return new DormandPrince853FieldIntegrator<>(field, minStep, maxStep, tolerances[0], tolerances[1]);
    }

    /**
     * Build spacecraft state Hermite interpolators using all kind of position-velocity-acceleration derivatives and angular
     * derivatives filter.
     *
     * @param interpolationPoints number of interpolation points
     * @param inertialFrame inertial frame
     *
     * @return array of spacecraft state Hermite interpolators containing all possible configuration (3 in total)
     */
    private FieldSpacecraftStateInterpolator<Binary64>[] buildAllTypeOfInterpolator(final int interpolationPoints,
                                                                                    final Frame inertialFrame) {

        final CartesianDerivativesFilter[] pvaFilters     = CartesianDerivativesFilter.values();
        final AngularDerivativesFilter[]   angularFilters = AngularDerivativesFilter.values();

        final int dim = pvaFilters.length;
        @SuppressWarnings("unchecked")
        final FieldSpacecraftStateInterpolator<Binary64>[] interpolators =
                new FieldSpacecraftStateInterpolator[dim];

        for (int i = 0; i < dim; i++) {
            interpolators[i] =
                    new FieldSpacecraftStateInterpolator<>(interpolationPoints,
                                                           AbstractFieldTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                                           inertialFrame, inertialFrame,
                                                           pvaFilters[i], angularFilters[i]);
        }

        return interpolators;
    }

    /**
     * Check interpolation error for position, velocity , attitude, mass, additional state and associated derivatives. This
     * method was designed to test interpolation on orbit defined spacecraft states.
     *
     * @param n sample size
     * @param expectedErrorP expected position error
     * @param expectedErrorV expected velocity error
     * @param expectedErrorA expected attitude error
     * @param expectedErrorM expected mass error
     * @param expectedErrorQ expected additional state error
     * @param expectedErrorD expected additional state derivative error
     * @param interpolator state interpolator
     */
    private void checkStandardInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                                 double expectedErrorA, double expectedErrorM,
                                                 double expectedErrorQ, double expectedErrorD,
                                                 final FieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64> interpolator) {
        FieldAbsoluteDate<Binary64>          centerDate = orbit.getDate().shiftedBy(100.0);
        List<FieldSpacecraftState<Binary64>> sample     = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            Binary64                       dt    = new Binary64(i * 900.0 / (n - 1));
            FieldSpacecraftState<Binary64> state = analyticalPropagator.propagate(centerDate.shiftedBy(dt));
            state = state.
                    addAdditionalState("quadratic", dt.multiply(dt)).
                    addAdditionalStateDerivative("quadratic-dot", dt.multiply(dt));
            sample.add(state);
        }

        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        double maxErrorQ = 0;
        double maxErrorD = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            FieldSpacecraftState<Binary64> interpolated = interpolator.interpolate(centerDate.shiftedBy(dt), sample);
            FieldSpacecraftState<Binary64> propagated   = analyticalPropagator.propagate(centerDate.shiftedBy(dt));
            FieldPVCoordinates<Binary64> dpv =
                    new FieldPVCoordinates<>(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm().getReal());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm().getReal());
            maxErrorA =
                    FastMath.max(maxErrorA,
                                 FastMath.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation().toRotation(),
                                                                      propagated.getAttitude().getRotation().toRotation())));
            maxErrorM =
                    FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass().getReal() - propagated.getMass().getReal()));
            maxErrorQ = FastMath.max(maxErrorQ,
                                     FastMath.abs(interpolated.getAdditionalState("quadratic")[0].getReal() - dt * dt));
            maxErrorD =
                    FastMath.max(maxErrorD,
                                 FastMath.abs(interpolated.getAdditionalStateDerivative("quadratic-dot")[0].getReal()
                                                      - dt * dt));
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
     * @param n sample size
     * @param expectedErrorP expected position error
     * @param expectedErrorV expected velocity error
     * @param expectedErrorA expected attitude error
     * @param expectedErrorM expected mass error
     * @param interpolator state interpolator
     */
    private void checkAbsPVInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                              double expectedErrorA, double expectedErrorM,
                                              final FieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64> interpolator) {
        FieldAbsoluteDate<Binary64>          centerDate = absPV.getDate().shiftedBy(100.0);
        List<FieldSpacecraftState<Binary64>> sample     = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            Binary64                       dt    = new Binary64(i * 900.0 / (n - 1));
            FieldSpacecraftState<Binary64> state = absPVPropagator.propagate(centerDate.shiftedBy(dt));
            sample.add(state);
        }

        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            FieldSpacecraftState<Binary64> interpolated = interpolator.interpolate(centerDate.shiftedBy(dt), sample);
            FieldSpacecraftState<Binary64> propagated   = absPVPropagator.propagate(centerDate.shiftedBy(dt));
            FieldPVCoordinates<Binary64> dpv =
                    new FieldPVCoordinates<>(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm().getReal());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm().getReal());
            maxErrorA =
                    FastMath.max(maxErrorA,
                                 FastMath.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation().toRotation(),
                                                                      propagated.getAttitude().getRotation().toRotation())));
            maxErrorM =
                    FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass().getReal() - propagated.getMass().getReal()));
        }
        Assertions.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assertions.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assertions.assertEquals(expectedErrorA, maxErrorA, 4.0e-10);
        Assertions.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
    }

    @Test
    void testErrorThrownWhenInterpolatingWithNonFieldDateAndEmptySample() {
        // GIVEN
        final FieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64> interpolator =
                new FieldSpacecraftStateInterpolator<>(FramesFactory.getEME2000());

        final List<FieldSpacecraftState<Binary64>> states = new ArrayList<>();

        // WHEN & THEN
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                                        () -> interpolator.interpolate(new AbsoluteDate(), states.stream()));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(0, ((Integer) thrown.getParts()[0]).intValue());

    }

    @Test
    void testGetNbInterpolationsWithMultipleSubInterpolators() {
        // GIVEN
        // Create mock interpolators
        final Frame frame = Mockito.mock(Frame.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> orbitInterpolator =
                Mockito.mock(FieldOrbitHermiteInterpolator.class);
        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> absPVAInterpolator =
                Mockito.mock(FieldAbsolutePVCoordinatesHermiteInterpolator.class);
        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> massInterpolator =
                Mockito.mock(TimeStampedFieldHermiteInterpolator.class);
        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldAttitude<Binary64>, Binary64> attitudeInterpolator =
                Mockito.mock(FieldAttitudeInterpolator.class);
        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> additionalStateInterpolator =
                Mockito.mock(TimeStampedFieldHermiteInterpolator.class);

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

        final FieldSpacecraftStateInterpolator<Binary64> stateInterpolator =
                new FieldSpacecraftStateInterpolator<>(frame, orbitInterpolator, absPVAInterpolator, massInterpolator,
                                                       attitudeInterpolator, additionalStateInterpolator);

        // WHEN
        final int returnedNbInterpolationPoints = stateInterpolator.getNbInterpolationPoints();

        // THEN
        Assertions.assertEquals(AdditionalStateNbInterpolationPoints, returnedNbInterpolationPoints);
    }

    @Test
    @DisplayName("test error thrown when using different state definition")
    void testErrorThrownWhenUsingDifferentStateDefinition() {
        // Given
        final Field<Binary64>             field             = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> interpolationDate = new FieldAbsoluteDate<>(field);

        @SuppressWarnings("unchecked")
        final FieldSpacecraftState<Binary64> orbitDefinedFieldState = Mockito.mock(FieldSpacecraftState.class);
        final SpacecraftState orbitDefinedState = Mockito.mock(SpacecraftState.class);

        Mockito.when(orbitDefinedState.isOrbitDefined()).thenReturn(true);
        Mockito.when(orbitDefinedFieldState.toSpacecraftState()).thenReturn(orbitDefinedState);

        @SuppressWarnings("unchecked")
        final FieldSpacecraftState<Binary64> absPVDefinedFieldState = Mockito.mock(FieldSpacecraftState.class);
        final SpacecraftState absPVDefinedState = Mockito.mock(SpacecraftState.class);

        Mockito.when(absPVDefinedState.isOrbitDefined()).thenReturn(false);
        Mockito.when(absPVDefinedFieldState.toSpacecraftState()).thenReturn(absPVDefinedState);

        final List<FieldSpacecraftState<Binary64>> states = new ArrayList<>();
        states.add(orbitDefinedFieldState);
        states.add(absPVDefinedFieldState);

        // Create interpolator
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        final FieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64> stateInterpolator =
                new FieldSpacecraftStateInterpolator<>(inertialFrameMock);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> stateInterpolator.interpolate(interpolationDate, states));

        Assertions.assertEquals(
                "one state is defined using an orbit while the other is defined using an absolute position-velocity-acceleration",
                thrown.getMessage());
    }

    @Test
    @DisplayName("test error thrown when using no interpolator for state")
    void testErrorThrownWhenGivingNoInterpolatorForState() {
        // Given
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        // When & Then
        Exception thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                   () -> new FieldSpacecraftStateInterpolator<>(inertialFrameMock,
                                                                                                null, null, null, null,
                                                                                                null));

        Assertions.assertEquals("creating a spacecraft state interpolator requires at least one orbit interpolator or an "
                                        + "absolute position-velocity-acceleration interpolator", thrown.getMessage());
    }

    @Test
    @DisplayName("test error thrown when giving empty sample")
    void testErrorThrownWhenGivingEmptySample() {
        // Given

        @SuppressWarnings("unchecked")
        final FieldAbsoluteDate<Binary64> interpolationDate = Mockito.mock(FieldAbsoluteDate.class);

        final Frame inertialFrame = FramesFactory.getEME2000();

        final List<FieldSpacecraftState<Binary64>> states = new ArrayList<>();

        // Create interpolator
        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> orbitInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        final FieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64> interpolator =
                new FieldSpacecraftStateInterpolator<>(inertialFrame, orbitInterpolatorMock, null, null, null, null);

        // When & Then
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class, () ->
                                                                        interpolator.interpolate(interpolationDate, states));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(0, ((Integer) thrown.getParts()[0]).intValue());

    }

    @Test
    void testFieldSpacecraftStateInterpolatorCreation() {
        // Given
        final Frame inertialFrameMock = Mockito.mock(Frame.class);
        Mockito.when(inertialFrameMock.isPseudoInertial()).thenReturn(true);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> orbitInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> absPVInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> massInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<FieldAttitude<Binary64>, Binary64> attitudeInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<TimeStampedField<Binary64>, Binary64> additionalInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        // When
        final FieldSpacecraftStateInterpolator<Binary64> interpolator =
                new FieldSpacecraftStateInterpolator<>(inertialFrameMock, orbitInterpolatorMock, absPVInterpolatorMock,
                                                       massInterpolatorMock, attitudeInterpolatorMock,
                                                       additionalInterpolatorMock);

        // Then
        Assertions.assertEquals(inertialFrameMock, interpolator.getOutputFrame());
        Assertions.assertEquals(orbitInterpolatorMock, interpolator.getOrbitInterpolator().get());
        Assertions.assertEquals(absPVInterpolatorMock, interpolator.getAbsPVAInterpolator().get());
        Assertions.assertEquals(massInterpolatorMock, interpolator.getMassInterpolator().get());
        Assertions.assertEquals(attitudeInterpolatorMock, interpolator.getAttitudeInterpolator().get());
        Assertions.assertEquals(additionalInterpolatorMock, interpolator.getAdditionalStateInterpolator().get());

    }

    @Test
    @DisplayName("Test error thrown when sub interpolator is not present")
    void testErrorThrownWhenSubInterpolatorIsNotPresent() {
        // GIVEN
        final FakeFieldStateInterpolator fakeStateInterpolator = new FakeFieldStateInterpolator();

        // WHEN & THEN
        Assertions.assertThrows(OrekitInternalError.class, fakeStateInterpolator::getNbInterpolationPoints);
    }

    @Test
    @DisplayName("Test does not throw error when checking interpolator compatibility")
    void testDoesNotThrowWhenCheckingInterpolatorCompatibility() {
        // GIVEN
        final FakeFieldStateInterpolator fakeStateInterpolator = new FakeFieldStateInterpolator();

        // WHEN & THEN
        Assertions.assertThrows(OrekitInternalError.class, fakeStateInterpolator::getNbInterpolationPoints);
    }

    private static class FakeFieldStateInterpolator extends AbstractFieldTimeInterpolator<FieldSpacecraftState<Binary64>,Binary64> {

        public FakeFieldStateInterpolator() {
            super(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                  AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
        }

        @Override
        public List<FieldTimeInterpolator<? extends FieldTimeStamped<Binary64>, Binary64>> getSubInterpolators() {
            return Collections.emptyList();
        }

        @Override
        protected FieldSpacecraftState<Binary64> interpolate(AbstractFieldTimeInterpolator<FieldSpacecraftState<Binary64>, Binary64>.InterpolationData interpolationData) {
            return null;
        }
    }
}