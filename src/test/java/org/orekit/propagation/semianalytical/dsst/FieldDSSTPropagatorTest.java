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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldEphemerisGenerator;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldAltitudeDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldLatitudeCrossingDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.AbstractGaussianContribution;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldDSSTPropagatorTest {

    /**
     * Test issue #1029 about DSST short period terms computation.
     * Issue #1029 is a regression introduced in version 10.0
     * Test case built from Christophe Le Bris example: https://gitlab.orekit.org/orekit/orekit/-/issues/1029
     */
    @Test
    public void testIssue1029() {
        doTestIssue1029(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue1029(Field<T> field) {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // Zero
        final T zero = field.getZero();

        // initial state
        final FieldAbsoluteDate<T> orbitEpoch = new FieldAbsoluteDate<>(field, 2023, 2, 18, TimeScalesFactory.getUTC());
        final Frame inertial = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        final FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(42166000.0), zero.add(0.00028), zero.add(FastMath.toRadians(0.05)),
                                                                     zero.add(FastMath.toRadians(66.0)), zero.add(FastMath.toRadians(270.0)),
                                                                     zero.add(FastMath.toRadians(11.94)), PositionAngleType.MEAN,
                                                                     inertial, orbitEpoch, zero.add(Constants.WGS84_EARTH_MU));
        final FieldEquinoctialOrbit<T> equinoctial = new FieldEquinoctialOrbit<>(orbit);

        // create propagator
        final double[][] tol = FieldDSSTPropagator.tolerances(zero.add(0.001), equinoctial);
        final AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 3600.0, 86400.0, tol[0], tol[1]);
        final FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator, PropagationType.OSCULATING);

        // add force models
        final Frame ecefFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final UnnormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        propagator.addForceModel(new DSSTZonal(gravityProvider));
        propagator.addForceModel(new DSSTTesseral(ecefFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityProvider));
        propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getSun(), gravityProvider.getMu()));
        propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon(), gravityProvider.getMu()));

        // propagate
        propagator.setInitialState(new FieldSpacecraftState<>(equinoctial, zero.add(6000.0)), PropagationType.MEAN);
        FieldSpacecraftState<T> propagated = propagator.propagate(orbitEpoch.shiftedBy(20.0 * Constants.JULIAN_DAY));

        // The purpose is not verifying propagated values, but to check that no exception occurred
        Assertions.assertEquals(0.0, propagated.getDate().durationFrom(orbitEpoch.shiftedBy(20.0 * Constants.JULIAN_DAY)).getReal(), Double.MIN_VALUE);
        Assertions.assertEquals(4.216464862956647E7, propagated.getA().getReal(), Double.MIN_VALUE);

    }

    @Test
    public void testIssue363() {
        doTestIssue363(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue363(Field<T> field) {
        Utils.setDataRoot("regular-data");
        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2003-06-18T00:00:00.000", TimeScalesFactory.getUTC());
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(zero.add(7389068.5), zero.add(1.0e-15), zero.add(1.0e-15), zero.add(1.709573), zero.add(1.308398), zero.add(0), PositionAngleType.MEAN,
                        FramesFactory.getTOD(IERSConventions.IERS_2010, false),
                        date, zero.add(Constants.WGS84_EARTH_MU));
        FieldSpacecraftState<T> osculatingState = new FieldSpacecraftState<>(orbit, zero.add(1116.2829));

        List<DSSTForceModel> dsstForceModels = new ArrayList<DSSTForceModel>();

        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getMoon(), orbit.getMu().getReal()));
        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getSun(), orbit.getMu().getReal()));

        FieldSpacecraftState<T> meanState = FieldDSSTPropagator.computeMeanState(osculatingState, null, dsstForceModels);
        Assertions.assertEquals( 0.421,   osculatingState.getA().subtract(meanState.getA()).getReal(),                         1.0e-3);
        Assertions.assertEquals(-5.23e-8, osculatingState.getEquinoctialEx().subtract(meanState.getEquinoctialEx()).getReal(), 1.0e-10);
        Assertions.assertEquals(15.22e-8, osculatingState.getEquinoctialEy().subtract(meanState.getEquinoctialEy()).getReal(), 1.0e-10);
        Assertions.assertEquals(-3.15e-8, osculatingState.getHx().subtract(meanState.getHx()).getReal(),                       1.0e-10);
        Assertions.assertEquals( 2.83e-8, osculatingState.getHy().subtract(meanState.getHy()).getReal(),                       1.0e-10);
        Assertions.assertEquals(15.96e-8, osculatingState.getLM().subtract(meanState.getLM()).getReal(),                       1.0e-10);

    }

    @Test
    public void testIssue364() {
        doTestIssue364(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue364(Field<T> field) {
        Utils.setDataRoot("regular-data");
        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2003-06-18T00:00:00.000", TimeScalesFactory.getUTC());
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(zero.add(7389068.5), zero.add(0.0), zero.add(0.0), zero.add(1.709573), zero.add(1.308398), zero.add(0), PositionAngleType.MEAN,
                        FramesFactory.getTOD(IERSConventions.IERS_2010, false),
                        date, zero.add(Constants.WGS84_EARTH_MU));
        FieldSpacecraftState<T> osculatingState = new FieldSpacecraftState<>(orbit, zero.add(1116.2829));

        List<DSSTForceModel> dsstForceModels = new ArrayList<DSSTForceModel>();

        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getMoon(), orbit.getMu().getReal()));
        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getSun(), orbit.getMu().getReal()));

        FieldSpacecraftState<T> meanState = FieldDSSTPropagator.computeMeanState(osculatingState, null, dsstForceModels);
        Assertions.assertEquals( 0.421,   osculatingState.getA().subtract(meanState.getA()).getReal(),                         1.0e-3);
        Assertions.assertEquals(-5.23e-8, osculatingState.getEquinoctialEx().subtract(meanState.getEquinoctialEx()).getReal(), 1.0e-10);
        Assertions.assertEquals(15.22e-8, osculatingState.getEquinoctialEy().subtract(meanState.getEquinoctialEy()).getReal(), 1.0e-10);
        Assertions.assertEquals(-3.15e-8, osculatingState.getHx().subtract(meanState.getHx()).getReal(),                       1.0e-10);
        Assertions.assertEquals( 2.83e-8, osculatingState.getHy().subtract(meanState.getHy()).getReal(),                       1.0e-10);
        Assertions.assertEquals(15.96e-8, osculatingState.getLM().subtract(meanState.getLM()).getReal(),                       1.0e-10);

    }

    @Test
    public void testHighDegreesSetting() {
        doTestHighDegreesSetting(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestHighDegreesSetting(Field<T> field) {

        final T zero = field.getZero();
        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        int earthDegree = 36;
        int earthOrder  = 36;
        int eccPower    = 4;
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(earthDegree, earthOrder);
        final org.orekit.frames.Frame earthFrame =
                        FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame
        final DSSTForceModel force =
                        new DSSTTesseral(earthFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                         earthDegree, earthOrder, eccPower, earthDegree + eccPower,
                                         earthDegree, earthOrder, eccPower);
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(force);
        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, "2015-07-01", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(
                        zero.add(7120000.0), zero.add(1.0e-3), zero.add(FastMath.toRadians(60.0)),
                        zero.add(FastMath.toRadians(120.0)), zero.add(FastMath.toRadians(47.0)),
                        zero.add(FastMath.toRadians(12.0)),
                        PositionAngleType.TRUE, eci, initialDate, zero.add(Constants.EIGEN5C_EARTH_MU));
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);
        FieldSpacecraftState<T> oscuState = FieldDSSTPropagator.computeOsculatingState(state, null, forces);
        Assertions.assertEquals(7119927.097122, oscuState.getA().getReal(), 0.001);
    }

    @Test
    public void testEphemerisDates() {
        doTestEphemerisDates(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEphemerisDates(Field<T> field) {
        final T zero = field.getZero();
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, "2015-07-01", tai);
        FieldAbsoluteDate<T> startDate   = new FieldAbsoluteDate<>(field, "2015-07-03", tai).shiftedBy(-0.1);
        FieldAbsoluteDate<T> endDate     = new FieldAbsoluteDate<>(field, "2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(
                        zero.add(600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS), zero, zero, zero, zero, zero,
                        PositionAngleType.TRUE, eci, initialDate, zero.add(Constants.EIGEN5C_EARTH_MU));
        double[][] tol = FieldDSSTPropagator
                        .tolerances(zero.add(1.), orbit);
        FieldPropagator<T> prop = new FieldDSSTPropagator<>(field,
                        new DormandPrince853FieldIntegrator<>(field, 0.1, 500, tol[0], tol[1]));
        prop.resetInitialState(new FieldSpacecraftState<>(new FieldCartesianOrbit<>(orbit)));

        //action
        final FieldEphemerisGenerator<T> generator = prop.getEphemerisGenerator();
        prop.propagate(startDate, endDate);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();

        //verify
        TimeStampedFieldPVCoordinates<T> actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedFieldPVCoordinates<T> expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition().toVector3D(),
                                 OrekitMatchers.vectorCloseTo(expectedPV.getPosition().toVector3D(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity().toVector3D(),
                                 OrekitMatchers.vectorCloseTo(expectedPV.getVelocity().toVector3D(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate).getReal(),
                                 OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate).getReal(),
                                 OrekitMatchers.closeTo(0, 0));
        //test date
        FieldAbsoluteDate<T> date = endDate.shiftedBy(-0.11);
        Assertions.assertEquals(
                                ephemeris.propagate(date).getDate().durationFrom(date).getReal(), 0, 0);
    }

    @Test
    public void testNoExtrapolation() {
        doTestNoExtrapolation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoExtrapolation(Field<T> field) {
        FieldSpacecraftState<T> state = getLEOState(field);
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, state);

        // Propagation of the initial state at the initial date
        final FieldSpacecraftState<T> finalState = dsstPropagator.propagate(state.getDate());

        // Initial orbit definition
        final FieldVector3D<T> initialPosition = state.getPosition();
        final FieldVector3D<T> initialVelocity = state.getPVCoordinates().getVelocity();

        // Final orbit definition
        final FieldVector3D<T> finalPosition = finalState.getPosition();
        final FieldVector3D<T> finalVelocity = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assertions.assertEquals(initialPosition.getX().getReal(), finalPosition.getX().getReal(), 0.0);
        Assertions.assertEquals(initialPosition.getY().getReal(), finalPosition.getY().getReal(), 0.0);
        Assertions.assertEquals(initialPosition.getZ().getReal(), finalPosition.getZ().getReal(), 0.0);
        Assertions.assertEquals(initialVelocity.getX().getReal(), finalVelocity.getX().getReal(), 0.0);
        Assertions.assertEquals(initialVelocity.getY().getReal(), finalVelocity.getY().getReal(), 0.0);
        Assertions.assertEquals(initialVelocity.getZ().getReal(), finalVelocity.getZ().getReal(), 0.0);
    }

    @Test
    public void testKepler() {
        doTestKepler(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestKepler(Field<T> field) {
        FieldSpacecraftState<T> state = getLEOState(field);
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, state);

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final FieldSpacecraftState<T> finalState = dsstPropagator.propagate(state.getDate().shiftedBy(dt));

        // Check results
        final T n = FastMath.sqrt(state.getA().reciprocal().multiply(state.getMu())).divide(state.getA());
        Assertions.assertEquals(state.getA().getReal(),                      finalState.getA().getReal(), 0.);
        Assertions.assertEquals(state.getEquinoctialEx().getReal(),          finalState.getEquinoctialEx().getReal(), 0.);
        Assertions.assertEquals(state.getEquinoctialEy().getReal(),          finalState.getEquinoctialEy().getReal(), 0.);
        Assertions.assertEquals(state.getHx().getReal(),                     finalState.getHx().getReal(), 0.);
        Assertions.assertEquals(state.getHy().getReal(),                     finalState.getHy().getReal(), 0.);
        Assertions.assertEquals(state.getLM().add(n.multiply(dt)).getReal(), finalState.getLM().getReal(), 1.e-14);

    }

    @Test
    public void testEphemeris() {
        doTestEphemeris(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEphemeris(Field<T> field) {
        FieldSpacecraftState<T> state = getGEOState(field);
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, state);

        // Set ephemeris mode
        final FieldEphemerisGenerator<T> generator = dsstPropagator.getEphemerisGenerator();

        // Propagation of the initial state at t + 10 days
        final double dt = 2. * Constants.JULIAN_DAY;
        dsstPropagator.propagate(state.getDate().shiftedBy(5. * dt));

        // Get ephemeris
        FieldBoundedPropagator<T> ephem = generator.getGeneratedEphemeris();

        // Propagation of the initial state with ephemeris at t + 2 days
        final FieldSpacecraftState<T> s = ephem.propagate(state.getDate().shiftedBy(dt));

        // Check results
        final T n = FastMath.sqrt(state.getA().reciprocal().multiply(state.getMu())).divide(state.getA());
        Assertions.assertEquals(state.getA().getReal(),                      s.getA().getReal(), 0.);
        Assertions.assertEquals(state.getEquinoctialEx().getReal(),          s.getEquinoctialEx().getReal(), 0.);
        Assertions.assertEquals(state.getEquinoctialEy().getReal(),          s.getEquinoctialEy().getReal(), 0.);
        Assertions.assertEquals(state.getHx().getReal(),                     s.getHx().getReal(), 0.);
        Assertions.assertEquals(state.getHy().getReal(),                     s.getHy().getReal(), 0.);
        Assertions.assertEquals(state.getLM().add(n.multiply(dt)).getReal(), s.getLM().getReal(), 1.5e-14);

    }

    @Test
    public void testPropagationWithCentralBody() {
        doTestPropagationWithCentralBody(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPropagationWithCentralBody(Field<T> field) {

        final T zero = field.getZero();

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 4);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        // GPS Orbit
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2007, 4, 16, 0, 46, 42.400,
                        TimeScalesFactory.getUTC());
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(26559890.),
                        zero.add(0.0041632),
                        zero.add(FastMath.toRadians(55.2)),
                        zero.add(FastMath.toRadians(315.4985)),
                        zero.add(FastMath.toRadians(130.7562)),
                        zero.add(FastMath.toRadians(44.2377)),
                        PositionAngleType.MEAN,
                        FramesFactory.getEME2000(),
                        initDate,
                        zero.add(provider.getMu()));

        // Set propagator with state and force model
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, new FieldSpacecraftState<>(orbit));
        dsstPropagator.addForceModel(new DSSTZonal(provider, 4, 3, 9));
        dsstPropagator.addForceModel(new DSSTTesseral(earthFrame,
                                                      Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                      4, 4, 4, 8, 4, 4, 2));

        // 5 days propagation
        final FieldSpacecraftState<T> state = dsstPropagator.propagate(initDate.shiftedBy(5. * 86400.));

        // Ref GTDS_DSST:
        // a    = 26559.92081 km
        // h/ey =   0.2731622444E-03
        // k/ex =   0.4164167597E-02
        // p/hy =  -0.3399607878
        // q/hx =   0.3971568634
        // lM   = 140.6375352°
        Assertions.assertEquals(2.655992081E7, state.getA().getReal(), 1.e2);
        Assertions.assertEquals(0.2731622444E-03, state.getEquinoctialEx().getReal(), 2.e-8);
        Assertions.assertEquals(0.4164167597E-02, state.getEquinoctialEy().getReal(), 2.e-8);
        Assertions.assertEquals(-0.3399607878, state.getHx().getReal(), 5.e-8);
        Assertions.assertEquals(0.3971568634, state.getHy().getReal(), 2.e-6);
        Assertions.assertEquals(140.6375352,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), zero.add(FastMath.PI)).getReal()),
                                5.e-3);
    }

    @Test
    public void testPropagationWithThirdBody() throws IOException {
        doTestPropagationWithThirdBody(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPropagationWithThirdBody(Field<T> field) throws IOException {

        final T zero = field.getZero();

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        // Third Bodies Force Model (Moon + Sun)
        DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu());
        DSSTForceModel sun  = new DSSTThirdBody(CelestialBodyFactory.getSun(), provider.getMu());

        // SIRIUS Orbit
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 7, 1, 0, 0, 00.000,
                        TimeScalesFactory.getUTC());
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(42163393.),
                        zero.add(0.2684),
                        zero.add(FastMath.toRadians(63.435)),
                        zero.add(FastMath.toRadians(270.0)),
                        zero.add(FastMath.toRadians(285.0)),
                        zero.add(FastMath.toRadians(344.0)),
                        PositionAngleType.MEAN,
                        FramesFactory.getEME2000(),
                        initDate,
                        zero.add(provider.getMu()));

        // Set propagator with state and force model
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, new FieldSpacecraftState<>(orbit));
        dsstPropagator.addForceModel(zonal);
        dsstPropagator.addForceModel(tesseral);
        dsstPropagator.addForceModel(moon);
        dsstPropagator.addForceModel(sun);

        // 5 days propagation
        final FieldSpacecraftState<T> state = dsstPropagator.propagate(initDate.shiftedBy(5. * 86400.));

        // Ref Standalone_DSST:
        // a    = 42163393.0 m
        // h/ey =  -0.06893353670734315
        // k/ex =  -0.2592789733084587
        // p/hy =  -0.5968524904937771
        // q/hx =   0.1595005111738418
        // lM   = 183°9386620425922
        Assertions.assertEquals(42163393.0, state.getA().getReal(), 1.e-1);
        Assertions.assertEquals(-0.2592789733084587, state.getEquinoctialEx().getReal(), 5.e-7);
        Assertions.assertEquals(-0.06893353670734315, state.getEquinoctialEy().getReal(), 2.e-7);
        Assertions.assertEquals( 0.1595005111738418, state.getHx().getReal(), 2.e-7);
        Assertions.assertEquals(-0.5968524904937771, state.getHy().getReal(), 5.e-8);
        Assertions.assertEquals(183.9386620425922,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), zero.add(FastMath.PI)).getReal()),
                                3.e-2);
    }

    @Test
    public void testTooSmallMaxDegree() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0), 1, 0, 3);
        });
    }

    @Test
    public void testTooLargeMaxDegree() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0), 8, 0, 8);
        });
    }

    @Test
    public void testWrongMaxPower() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(8, 8), 4, 4, 4);
        });
    }

    @Test
    public void testPropagationWithDrag() {
        doTestPropagationWithDrag(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPropagationWithDrag(Field<T> field) {

        final T zero = field.getZero();
        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 0, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        // Drag Force Model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(provider.getAe(),
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            earthFrame);
        final Atmosphere atm = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);

        final double cd = 2.0;
        final double area = 25.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, area, provider.getMu());

        // LEO Orbit
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 7, 1, 0, 0, 00.000,
                        TimeScalesFactory.getUTC());
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7204535.848109440),
                        zero.add(0.0012402238462686),
                        zero.add(FastMath.toRadians(98.74341600466740)),
                        zero.add(FastMath.toRadians(111.1990175076630)),
                        zero.add(FastMath.toRadians(43.32990110790340)),
                        zero.add(FastMath.toRadians(68.66852509725620)),
                        PositionAngleType.MEAN,
                        FramesFactory.getEME2000(),
                        initDate,
                        zero.add(provider.getMu()));

        // Set propagator with state and force model
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, new FieldSpacecraftState<>(orbit));
        dsstPropagator.addForceModel(zonal);
        dsstPropagator.addForceModel(tesseral);
        dsstPropagator.addForceModel(drag);

        // 5 days propagation
        final FieldSpacecraftState<T> state = dsstPropagator.propagate(initDate.shiftedBy(5. * 86400.));

        // Ref Standalone_DSST:
        // a    = 7204521.657141485 m
        // h/ey =  0.0007093755541595772
        // k/ex = -0.001016800430994036
        // p/hy =  0.8698955648709271
        // q/hx =  0.7757573478894775
        // lM   = 193°0939742953394
        Assertions.assertEquals(7204521.657141485, state.getA().getReal(), 6.e-1);
        Assertions.assertEquals(-0.001016800430994036, state.getEquinoctialEx().getReal(), 5.e-8);
        Assertions.assertEquals(0.0007093755541595772, state.getEquinoctialEy().getReal(), 2.e-8);
        Assertions.assertEquals(0.7757573478894775, state.getHx().getReal(), 5.e-8);
        Assertions.assertEquals(0.8698955648709271, state.getHy().getReal(), 5.e-8);
        Assertions.assertEquals(193.0939742953394,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), zero.add(FastMath.PI)).getReal()),
                                2.e-3);
        //Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getCd(), cd, 1e-9);
        //Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getArea(), area, 1e-9);
        Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getAtmosphere(), atm);

        final double atmosphericMaxConstant = 1000000.0; //DSSTAtmosphericDrag.ATMOSPHERE_ALTITUDE_MAX
        Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getRbar(), atmosphericMaxConstant + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 1e-9);
    }

    @Test
    public void testPropagationWithSolarRadiationPressure() {
        doTestPropagationWithSolarRadiationPressure(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPropagationWithSolarRadiationPressure(Field<T> field) {

        final T zero = field.getZero();
        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(CelestialBodyFactory.getEarth().getBodyOrientedFrame(),
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                            provider.getMu());

        // GEO Orbit
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 9, 16, 0, 0, 00.000,
                        TimeScalesFactory.getUTC());
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(42166258.),
                        zero.add(0.0001),
                        zero.add(FastMath.toRadians(0.001)),
                        zero.add(FastMath.toRadians(315.4985)),
                        zero.add(FastMath.toRadians(130.7562)),
                        zero.add(FastMath.toRadians(44.2377)),
                        PositionAngleType.MEAN,
                        FramesFactory.getGCRF(),
                        initDate,
                        zero.add(provider.getMu()));

        // Set propagator with state and force model
        final FieldDSSTPropagator<T> dsstPropagatorp = new FieldDSSTPropagator<>(field, new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(86400.)));
        dsstPropagatorp.setInitialState(new FieldSpacecraftState<>(orbit), PropagationType.MEAN);
        dsstPropagatorp.addForceModel(zonal);
        dsstPropagatorp.addForceModel(tesseral);
        dsstPropagatorp.addForceModel(srp);

        // 10 days propagation
        final FieldSpacecraftState<T> state = dsstPropagatorp.propagate(initDate.shiftedBy(10. * 86400.));

        // Ref Standalone_DSST:
        // a    = 42166257.99807995 m
        // h/ey = -0.1191876027555493D-03
        // k/ex = -0.1781865038201885D-05
        // p/hy =  0.6618387121369373D-05
        // q/hx = -0.5624363171289686D-05
        // lM   = 140°3496229467104
        Assertions.assertEquals(42166257.99807995, state.getA().getReal(), 1.2);
        Assertions.assertEquals(-0.1781865038201885e-05, state.getEquinoctialEx().getReal(), 3.e-7);
        Assertions.assertEquals(-0.1191876027555493e-03, state.getEquinoctialEy().getReal(), 4.e-6);
        Assertions.assertEquals(-0.5624363171289686e-05, state.getHx().getReal(), 4.e-9);
        Assertions.assertEquals( 0.6618387121369373e-05, state.getHy().getReal(), 3.e-10);
        Assertions.assertEquals(140.3496229467104,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), zero.add(FastMath.PI)).getReal()),
                                2.e-4);
    }

    @Test
    public void testStopEvent() {
        doTestStopEvent(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestStopEvent(Field<T> field) {
        FieldSpacecraftState<T> state = getLEOState(field);
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, state);

        final FieldAbsoluteDate<T> stopDate = state.getDate().shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.STOP);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> detector = new FieldDateDetector<>(field, stopDate).withHandler(checking);
        dsstPropagator.addEventDetector(detector);
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState = dsstPropagator.propagate(state.getDate().shiftedBy(3200));
        checking.assertEvent(true);
        Assertions.assertEquals(0, finalState.getDate().durationFrom(stopDate).getReal(), 1.0e-10);
    }

    @Test
    public void testContinueEvent() {
        doTestContinueEvent(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestContinueEvent(Field<T> field) {
        FieldSpacecraftState<T> state = getLEOState(field);
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, state);

        final FieldAbsoluteDate<T> resetDate = state.getDate().shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.CONTINUE);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> detector = new FieldDateDetector<>(field, resetDate).withHandler(checking);
        dsstPropagator.addEventDetector(detector);
        final double dt = 3200;
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState = dsstPropagator.propagate(state.getDate().shiftedBy(dt));
        checking.assertEvent(true);
        final T n = FastMath.sqrt(state.getA().reciprocal().multiply(state.getMu())).divide(state.getA());
        Assertions.assertEquals(state.getA().getReal(), finalState.getA().getReal(), 1.0e-10);
        Assertions.assertEquals(state.getEquinoctialEx().getReal(), finalState.getEquinoctialEx().getReal(), 1.0e-10);
        Assertions.assertEquals(state.getEquinoctialEy().getReal(), finalState.getEquinoctialEy().getReal(), 1.0e-10);
        Assertions.assertEquals(state.getHx().getReal(), finalState.getHx().getReal(), 1.0e-10);
        Assertions.assertEquals(state.getHy().getReal(), finalState.getHy().getReal(), 1.0e-10);
        Assertions.assertEquals(state.getLM().add(n.multiply(dt)).getReal(), finalState.getLM().getReal(), 6.0e-10);
    }

    @Test
    public void testIssue157() {
        doTestIssue157(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue157(Field<T> field) {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        final T zero = field.getZero();
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(13378000), zero.add(0.05), zero.add(0), zero.add(0), zero.add(FastMath.PI), zero.add(0), PositionAngleType.MEAN,
                        FramesFactory.getTOD(false),
                        new FieldAbsoluteDate<>(field, 2003, 5, 6, TimeScalesFactory.getUTC()),
                        zero.add(nshp.getMu()));
        T period = orbit.getKeplerianPeriod();
        double[][] tolerance = FieldDSSTPropagator.tolerances(zero.add(1.), orbit);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                        new DormandPrince853FieldIntegrator<>(field, period.getReal() / 100, period.getReal() * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(period.multiply(10.).getReal());
        FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator, PropagationType.MEAN);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 8, 7, 17));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 8, 8, 4, 12, 8, 8, 4));
        propagator.addForceModel(new DSSTThirdBody(sun, nshp.getMu()));
        propagator.addForceModel(new DSSTThirdBody(moon, nshp.getMu()));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180, nshp.getMu()));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth, nshp.getMu()));


        propagator.setInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)), PropagationType.OSCULATING);
        FieldSpacecraftState<T> finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        // the following comparison is in fact meaningless
        // the initial orbit is osculating the final orbit is a mean orbit
        // and they are not considered at the same epoch
        // we keep it only as is was an historical test
        Assertions.assertEquals(2187.2, orbit.getA().subtract(finalState.getA()).getReal(), 1.0);

        propagator.setInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)), PropagationType.MEAN);
        finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        // the following comparison is realistic
        // both the initial orbit and final orbit are mean orbits
        Assertions.assertEquals(1475.90, orbit.getA().subtract(finalState.getA()).getReal(), 1.0);

    }

    /**
     * Compare classical propagation with a fixed-step handler with ephemeris generation on the same points.
     */
    @Test
    public void testEphemerisGeneration() {
        doTestEphemerisGeneration(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEphemerisGeneration(Field<T> field){

        // GIVEN
        // -----

        Utils.setDataRoot("regular-data:potential/icgem-format");
        final T zero = field.getZero();
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(13378000), zero.add(0.05), zero.add(0), zero.add(0), zero.add(FastMath.PI), zero.add(0), PositionAngleType.MEAN,
                        FramesFactory.getTOD(false),
                        new FieldAbsoluteDate<>(field, 2003, 5, 6, TimeScalesFactory.getUTC()),
                        zero.add(nshp.getMu()));
        T period = orbit.getKeplerianPeriod();
        double[][] tolerance = FieldDSSTPropagator.tolerances(zero.add(1.), orbit);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                        new DormandPrince853FieldIntegrator<>(field, period.getReal() / 100, period.getReal() * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(period.multiply(10.).getReal());
        FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator, PropagationType.OSCULATING);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 8, 7, 17));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 8, 8, 4, 12, 8, 8, 4));
        propagator.addForceModel(new DSSTThirdBody(sun, nshp.getMu()));
        propagator.addForceModel(new DSSTThirdBody(moon, nshp.getMu()));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180, nshp.getMu()));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth, nshp.getMu()));
        propagator.setInterpolationGridToMaxTimeGap(zero.add(0.5 * Constants.JULIAN_DAY));

        // WHEN
        // ----

        // Number of days of propagation
        // Was 30 days but was reduced for issue 1106
        final double nDays = 5.;

        // direct generation of states
        propagator.setInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)), PropagationType.MEAN);
        final List<FieldSpacecraftState<T>> states = new ArrayList<FieldSpacecraftState<T>>();
        propagator.setStepHandler(zero.add(600), currentState -> states.add(currentState));
        propagator.propagate(orbit.getDate().shiftedBy(nDays * Constants.JULIAN_DAY));

        // ephemeris generation
        propagator.setInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)), PropagationType.MEAN);
        final FieldEphemerisGenerator<T> generator = propagator.getEphemerisGenerator();
        propagator.propagate(orbit.getDate().shiftedBy(nDays * Constants.JULIAN_DAY));
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();

        T maxError = zero;
        for (final FieldSpacecraftState<T> state : states) {
            final FieldSpacecraftState<T> fromEphemeris = ephemeris.propagate(state.getDate());
            final T error = FieldVector3D.distance(state.getPosition(), fromEphemeris.getPosition());
            maxError = FastMath.max(maxError, error);
        }

        // THEN
        // ----

        // Check on orbits' distances was 1e-10 m but was reduced during issue 1106
        Assertions.assertEquals(0.0, maxError.getReal(), Precision.SAFE_MIN);
    }

    @Test
    public void testGetInitialOsculatingState() {
        doTestGetInitialOsculatingState(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetInitialOsculatingState(Field<T> field) {
        final FieldSpacecraftState<T> initialState = getGEOState(field);

        final T zero = field.getZero();
        // build integrator
        final T minStep = initialState.getKeplerianPeriod().multiply(0.1);
        final T maxStep = initialState.getKeplerianPeriod().multiply(10.0);
        final double[][] tol = FieldDSSTPropagator.tolerances(zero.add(0.1), initialState.getOrbit());
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, minStep.getReal(), maxStep.getReal(), tol[0], tol[1]);

        // build the propagator for the propagation of the mean elements
        FieldDSSTPropagator<T> prop = new FieldDSSTPropagator<>(field, integrator, PropagationType.MEAN);

        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 4, 3, 9);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 4, 0, 4, 8, 4, 0, 2);
        prop.addForceModel(zonal);
        prop.addForceModel(tesseral);

        // Set the initial state
        prop.setInitialState(initialState, PropagationType.MEAN);
        // Check the stored initial state is the osculating one
        Assertions.assertEquals(initialState, prop.getInitialState());
        // Check that no propagation, i.e. propagation to the initial date, provides the initial
        // osculating state although the propagator is configured to propagate mean elements !!!
        Assertions.assertEquals(initialState, prop.propagate(initialState.getDate()));
    }

    @Test
    public void testMeanToOsculatingState() {
        doTestMeanToOsculatingState(Binary64Field.getInstance());
    }
    private <T extends CalculusFieldElement<T>> void doTestMeanToOsculatingState(Field<T> field) {
        final FieldSpacecraftState<T> meanState = getGEOState(field);

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                         provider, 2, 0, 0, 2, 2, 0, 0);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);

        final FieldSpacecraftState<T> osculatingState = FieldDSSTPropagator.computeOsculatingState(meanState, null, forces);
        Assertions.assertEquals(1559.1,
                                FieldVector3D.distance(meanState.getPosition(),
                                                       osculatingState.getPosition()).getReal(),
                                1.0);
    }

    @Test
    public void testOsculatingToMeanState() {
        doTestOsculatingToMeanState(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestOsculatingToMeanState(Field<T> field) {
        final FieldSpacecraftState<T> meanState = getGEOState(field);

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);

        final FieldSpacecraftState<T> osculatingState = FieldDSSTPropagator.computeOsculatingState(meanState, null, forces);

        // there are no Gaussian force models, we don't need an attitude provider
        final FieldSpacecraftState<T> computedMeanState = FieldDSSTPropagator.computeMeanState(osculatingState, null, forces);

        Assertions.assertEquals(meanState.getA().getReal(), computedMeanState.getA().getReal(), 2.0e-8);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(meanState.getPosition(),
                                                       computedMeanState.getPosition()).getReal(),
                                2.0e-8);
    }

    @Test
    public void testShortPeriodCoefficients() {
        doTestShortPeriodCoefficients(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestShortPeriodCoefficients(Field<T> field) {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        final T zero = field.getZero();
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(4, 4);
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(13378000), zero.add(0.05), zero.add(0), zero.add(0), zero.add(FastMath.PI), zero.add(0), PositionAngleType.MEAN,
                        FramesFactory.getTOD(false),
                        new FieldAbsoluteDate<>(field, 2003, 5, 6, TimeScalesFactory.getUTC()),
                        zero.add(nshp.getMu()));
        T period = orbit.getKeplerianPeriod();
        double[][] tolerance = FieldDSSTPropagator.tolerances(zero.add(1.), orbit);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                        new DormandPrince853FieldIntegrator<>(field, period.getReal() / 100, period.getReal() * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(period.multiply(10).getReal());
        FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator, PropagationType.OSCULATING);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 4, 3, 9));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 4, 4, 4, 8, 4, 4, 2));
        propagator.addForceModel(new DSSTThirdBody(sun, nshp.getMu()));
        propagator.addForceModel(new DSSTThirdBody(moon, nshp.getMu()));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180, nshp.getMu()));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth, nshp.getMu()));

        final FieldAbsoluteDate<T> finalDate = orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY);
        propagator.resetInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)));
        final FieldSpacecraftState<T> stateNoConfig = propagator.propagate(finalDate);
        Assertions.assertEquals(0, stateNoConfig.getAdditionalStatesValues().size());

        propagator.setSelectedCoefficients(new HashSet<String>());
        propagator.resetInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)));
        final FieldSpacecraftState<T> stateConfigEmpty = propagator.propagate(finalDate);
        Assertions.assertEquals(234, stateConfigEmpty.getAdditionalStatesValues().size());

        final Set<String> selected = new HashSet<String>();
        selected.add("DSST-3rd-body-Moon-s[7]");
        selected.add("DSST-central-body-tesseral-c[-2][3]");
        propagator.setSelectedCoefficients(selected);
        propagator.resetInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)));
        final FieldSpacecraftState<T> stateConfigeSelected = propagator.propagate(finalDate);
        Assertions.assertEquals(selected.size(), stateConfigeSelected.getAdditionalStatesValues().size());

        propagator.setSelectedCoefficients(null);
        propagator.resetInitialState(new FieldSpacecraftState<>(orbit, zero.add(45.0)));
        final FieldSpacecraftState<T> stateConfigNull = propagator.propagate(finalDate);
        Assertions.assertEquals(0, stateConfigNull.getAdditionalStatesValues().size());

    }

    @Test
    public void testIssueMeanInclination() {
        doTestIssueMeanInclination(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssueMeanInclination(Field<T> field) {

        final T zero = field.getZero();
        final double earthAe = 6378137.0;
        final double earthMu = 3.9860044E14;
        final double earthJ2 = 0.0010826;

        // Initialize the DSST propagator with only J2 perturbation
        FieldOrbit<T> orb = new FieldKeplerianOrbit<>(new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, "1992-10-08T15:20:38.821",
                        TimeScalesFactory.getUTC()),
                        new FieldVector3D<>(zero.add(5392808.809823), zero.add(-4187618.3357927715), zero.add(-44206.638015847195)),
                        new FieldVector3D<>(zero.add(2337.4472786270794), zero.add(2474.0146611860464), zero.add(6778.507766114648)),
                        FieldVector3D.getZero(field)),
                        FramesFactory.getTOD(false), zero.add(earthMu));
        final FieldSpacecraftState<T> ss = new FieldSpacecraftState<>(orb);
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(earthAe, earthMu, TideSystem.UNKNOWN,
                                                                    new double[][] { { 0.0 }, { 0.0 }, { -earthJ2 } },
                                                                    new double[][] { { 0.0 }, { 0.0 }, { 0.0 } });
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);
        // Computes J2 mean elements using the DSST osculating to mean converter
        final FieldOrbit<T> meanOrb = FieldDSSTPropagator.computeMeanState(ss, null, forces).getOrbit();
        Assertions.assertEquals(0.0164196, FastMath.toDegrees(orb.getI().subtract(meanOrb.getI()).getReal()), 1.0e-7);
    }

    @Test
    public void testIssue257() {
        doTestIssue257(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue257(Field<T> field) {
        final FieldSpacecraftState<T> meanState = getGEOState(field);

        // Third Bodies Force Model (Moon + Sun)
        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu().getReal());
        final DSSTForceModel sun  = new DSSTThirdBody(CelestialBodyFactory.getSun(), meanState.getMu().getReal());

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(moon);
        forces.add(sun);

        final FieldSpacecraftState<T> osculatingState = FieldDSSTPropagator.computeOsculatingState(meanState, null, forces);
        Assertions.assertEquals(734.3,
                                FieldVector3D.distance(meanState.getPosition(),
                                                       osculatingState.getPosition()).getReal(),
                                1.0);

        final FieldSpacecraftState<T> computedMeanState = FieldDSSTPropagator.computeMeanState(osculatingState, null, forces);
        Assertions.assertEquals(734.3,
                                FieldVector3D.distance(osculatingState.getPosition(),
                                                       computedMeanState.getPosition()).getReal(),
                                1.0);

        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(computedMeanState.getPosition(),
                                                       meanState.getPosition()).getReal(),
                                5.0e-6);

    }

    @Test
    public void testIssue339() {
        doTestIssue339(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue339(Field<T> field) {

        final FieldSpacecraftState<T> osculatingState = getLEOState(field);

        final CelestialBody    sun   = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                  true));
        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0,
                                                                                     sun,
                                                                                     50.0, Vector3D.PLUS_J,
                                                                                     2.0, 0.1,
                                                                                     0.2, 0.6);
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
        final AttitudeProvider attitudeProvider = new LofOffset(osculatingState.getFrame(),
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        // Surface force models that require an attitude provider
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(new DSSTSolarRadiationPressure(sun, earth, boxAndWing, osculatingState.getMu().getReal()));
        forces.add(new DSSTAtmosphericDrag(atmosphere, boxAndWing, osculatingState.getMu().getReal()));

        final FieldSpacecraftState<T> meanState = FieldDSSTPropagator.computeMeanState(osculatingState, attitudeProvider, forces);
        Assertions.assertEquals(0.522,
                                FieldVector3D.distance(osculatingState.getPosition(),
                                                       meanState.getPosition()).getReal(),
                                0.001);

        final FieldSpacecraftState<T> computedOsculatingState = FieldDSSTPropagator.computeOsculatingState(meanState, attitudeProvider, forces);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(osculatingState.getPosition(),
                                                       computedOsculatingState.getPosition()).getReal(),
                                5.0e-6);

    }

    @Test
    public void testIssue339WithAccelerations() {
        doTestIssue339WithAccelerations(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue339WithAccelerations(Field<T> field) {
        final FieldSpacecraftState<T> osculatingState = getLEOStatePropagatedBy30Minutes(field);
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0, sun, 50.0, Vector3D.PLUS_J, 2.0, 0.1, 0.2, 0.6);
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
        final AttitudeProvider attitudeProvider = new LofOffset(osculatingState.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ, 0.0, 0.0, 0.0);
        // Surface force models that require an attitude provider
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(new DSSTAtmosphericDrag(atmosphere, boxAndWing, osculatingState.getMu().getReal()));
        final FieldSpacecraftState<T> meanState = FieldDSSTPropagator.computeMeanState(osculatingState, attitudeProvider, forces);
        final FieldSpacecraftState<T> computedOsculatingState = FieldDSSTPropagator.computeOsculatingState(meanState, attitudeProvider, forces);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(osculatingState.getPosition(), computedOsculatingState.getPosition()).getReal(),
                                5.0e-6);
    }

    @Test
    public void testIssue613() {
        doTestIssue613(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue613(final Field<T> field) {
        final T zero = field.getZero();
        // Spacecraft state
        final FieldSpacecraftState<T> state = getLEOState(field);

        // Body frame
        final Frame itrf = FramesFactory .getITRF(IERSConventions.IERS_2010, true);

        // Earth
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(4, 4);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        // Detectors
        final List<FieldEventDetector<T>> events = new ArrayList<>();
        events.add(new FieldAltitudeDetector<>(zero.add(85.5), earth));
        events.add(new FieldLatitudeCrossingDetector<>(field, earth, 0.0));

        // Force models
        final List<DSSTForceModel> forceModels = new ArrayList<>();
        forceModels.add(new DSSTZonal(provider));
        forceModels.add(new DSSTTesseral(itrf, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider));
        forceModels.add(new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu()));
        forceModels.add(new DSSTThirdBody(CelestialBodyFactory.getSun(), provider.getMu()));

        // Set up DSST propagator
        final double[][] tol = FieldDSSTPropagator.tolerances(zero.add(10.0), state.getOrbit());
        final FieldODEIntegrator<T> integrator = new DormandPrince54FieldIntegrator<>(field, 60.0, 3600.0, tol[0], tol[1]);
        final FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator, PropagationType.OSCULATING);
        for (DSSTForceModel force : forceModels) {
            propagator.addForceModel(force);
        }
        for (FieldEventDetector<T> event : events) {
            propagator.addEventDetector(event);
        }
        propagator.setInitialState(state);

        // Propagation
        final FieldSpacecraftState<T> finalState = propagator.propagate(state.getDate().shiftedBy(86400.0));

        // Verify is the propagation is correctly performed
        Assertions.assertEquals(finalState.getMu().getReal(), 3.986004415E14, Double.MIN_VALUE);
    }

    @Test
    public void testIssue704() {
        doTestIssue704(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue704(final Field<T> field) {

        // Coordinates
        final FieldOrbit<T>         orbit = getLEOState(field).getOrbit();
        final FieldPVCoordinates<T> pv    = orbit.getPVCoordinates();

        // dP
        final T dP = field.getZero().add(10.0);

        // Computes dV
        final T r2 = pv.getPosition().getNormSq();
        final T v  = pv.getVelocity().getNorm();
        final T dV = dP.multiply(orbit.getMu()).divide(v.multiply(r2));

        // Verify
        final double[][] tol1 = FieldDSSTPropagator.tolerances(dP, orbit);
        final double[][] tol2 = FieldDSSTPropagator.tolerances(dP, dV, orbit);
        for (int i = 0; i < tol1.length; i++) {
            Assertions.assertArrayEquals(tol1[i], tol2[i], Double.MIN_VALUE);
        }

    }

    /** This test is based on the example given by Orekit user kris06 in https://gitlab.orekit.org/orekit/orekit/-/issues/670. */
    @Test
    public void testIssue670() {
        doTestIssue670(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue670(final Field<T> field) {

        final NumericalForce force     = new NumericalForce();
        final DSSTForce      dsstForce = new DSSTForce(force, Constants.WGS84_EARTH_MU);

        FieldSpacecraftState<T> state = getLEOState(field);

        // Set propagator with state and force model
        final FieldDSSTPropagator<T> dsstPropagator = setDSSTProp(field, state);
        dsstPropagator.addForceModel(dsstForce);

        // Verify flag are false
        Assertions.assertFalse(force.initialized);
        Assertions.assertFalse(force.accComputed);

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final FieldAbsoluteDate<T> target = state.getDate().shiftedBy(dt);

        dsstPropagator.propagate(target);

        // Flag must be true
        Assertions.assertTrue(force.initialized);
        Assertions.assertTrue(force.accComputed);

    }
    
    /** Test issue 672:
     * DSST Propagator was crashing with tesseral harmonics of the gravity field
     * when the order is lower or equal to 3.
     */
    @Test
    public void testIssue672() {
        doTestIssue672(Binary64Field.getInstance());
    }
    
    private <T extends CalculusFieldElement<T>> void doTestIssue672(final Field<T> field) {

        // GIVEN
        // -----

        // Test with a central Body geopotential of 3x3
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(3, 3);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        // LEO spacecraft stateOrbit
        FieldSpacecraftState<T> initialState = getLEOState(field);

        // Set propagator with state and force model
        final T zero = field.getZero();
        initialState.getDate();
        final T minStep = initialState.getKeplerianPeriod();
        final T maxStep = minStep.multiply(100.);
        final double[][] tol = FieldDSSTPropagator.tolerances(zero.add(1.), initialState.getOrbit());
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, minStep.getReal(), maxStep.getReal(), tol[0], tol[1]);
        final FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator, PropagationType.OSCULATING);
        propagator.setInitialState(initialState, PropagationType.OSCULATING);
        propagator.addForceModel(new DSSTZonal(provider));
        propagator.addForceModel(new DSSTTesseral(earthFrame,
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider));

        // WHEN
        // ----

        // 1h propagation
        final FieldSpacecraftState<T> state = propagator.propagate(initialState.getDate().shiftedBy(3600.));

        // THEN
        // -----

        // Verify that no exception occurred
        Assertions.assertNotNull(state);
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

    private <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> getLEOState(final Field<T> field) {
        final T zero = field.getZero();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        // Spring equinoxe 21st mars 2003 1h00m
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        return new FieldSpacecraftState<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                        FramesFactory.getEME2000(),
                        initDate,
                        zero.add(3.986004415E14)));
    }

    private <T extends CalculusFieldElement<T>> FieldDSSTPropagator<T> setDSSTProp(Field<T> field,
                                                                                   FieldSpacecraftState<T> initialState) {
        final T zero = field.getZero();
        initialState.getDate();
        final T minStep = initialState.getKeplerianPeriod();
        final T maxStep = minStep.multiply(100.);
        final double[][] tol = FieldDSSTPropagator.tolerances(zero.add(1.), initialState.getOrbit());
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, minStep.getReal(), maxStep.getReal(), tol[0], tol[1]);
        final FieldDSSTPropagator<T> dsstProp = new FieldDSSTPropagator<>(field, integrator);
        dsstProp.setInitialState(initialState, PropagationType.MEAN);

        return dsstProp;
    }

    private class CheckingHandler<D extends FieldEventDetector<T>, T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

        private final Action actionOnEvent;
        private boolean gotHere;

        public CheckingHandler(final Action actionOnEvent) {
            this.actionOnEvent = actionOnEvent;
            this.gotHere       = false;
        }

        public void assertEvent(boolean expected) {
            Assertions.assertEquals(expected, gotHere);
        }

        @Override
        public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T> detector, boolean increasing) {
            gotHere = true;
            return actionOnEvent;
        }

    }

    /** This class is based on the example given by Orekit user kris06 in https://gitlab.orekit.org/orekit/orekit/-/issues/670. */
    private class DSSTForce extends AbstractGaussianContribution {

        DSSTForce(ForceModel contribution, double mu) {
            super("DSST mock -", 6.0e-10, contribution, mu);
        }

        /** {@inheritDoc} */
        @Override
        protected List<ParameterDriver> getParametersDriversWithoutMu() {
            return Collections.emptyList();
        }

        /** {@inheritDoc} */
        @Override
        protected double[] getLLimits(SpacecraftState state,
                                      AuxiliaryElements auxiliaryElements) {
            return new double[] { -FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0),
                FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0) };
        }

        /** {@inheritDoc} */
        @Override
        protected <T extends CalculusFieldElement<T>> T[] getLLimits(FieldSpacecraftState<T> state,
                                                                     FieldAuxiliaryElements<T> auxiliaryElements) {
            final Field<T> field = state.getDate().getField();
            final T zero = field.getZero();
            final T[] tab = MathArrays.buildArray(field, 2);
            tab[0] = MathUtils.normalizeAngle(state.getLv(), zero).subtract(FastMath.PI);
            tab[1] = MathUtils.normalizeAngle(state.getLv(), zero).add(FastMath.PI);
            return tab;
        }

    }

    /** This class is based on the example given by Orekit user kris06 in https://gitlab.orekit.org/orekit/orekit/-/issues/670. */
    private class NumericalForce implements ForceModel {

        private boolean initialized;
        private boolean accComputed;

        NumericalForce() {
            this.initialized = false;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.initialized = true;
            this.accComputed = false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return Vector3D.ZERO;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            this.accComputed = true;
            return FieldVector3D.getZero(s.getDate().getField());
        }

        /** {@inheritDoc} */
        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
            return Stream.empty();
        }


        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    private <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> getLEOStatePropagatedBy30Minutes(Field<T> field) {

        final T zero = field.getZero();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        // Spring equinoxe 21st mars 2003 1h00m
        final FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final FieldCartesianOrbit<T> osculatingOrbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position, velocity), FramesFactory.getTOD(IERSConventions.IERS_1996, false),
                        initialDate, zero.add(Constants.WGS84_EARTH_MU));
        // Adaptive step integrator
        // with a minimum step of 0.001 and a maximum step of 1000
        double minStep = 0.001;
        double maxstep = 1000.0;
        T positionTolerance = zero.add(10.0);
        OrbitType propagationType = OrbitType.EQUINOCTIAL;
        double[][] tolerances = FieldNumericalPropagator.tolerances(positionTolerance, osculatingOrbit, propagationType);
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, minStep, maxstep, tolerances[0], tolerances[1]);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(propagationType);

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel holmesFeatherstone = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        propagator.addForceModel(holmesFeatherstone);
        propagator.setInitialState(new FieldSpacecraftState<>(osculatingOrbit));

        return propagator.propagate(new FieldAbsoluteDate<>(initialDate, 1800.));
    }

}
