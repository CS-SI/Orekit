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
package org.orekit.forces.gravity;

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectorsProvider;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;


public class SolidTidesTest extends AbstractLegacyForceModelTest {

    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final FieldSpacecraftState<DerivativeStructure> state) {
        try {
            java.lang.reflect.Field attractionModelField = SolidTides.class.getDeclaredField("attractionModel");
            attractionModelField.setAccessible(true);
            ForceModel attractionModel = (ForceModel) attractionModelField.get(forceModel);
            Field<DerivativeStructure> field = state.getDate().getField();
            return attractionModel.acceleration(state, attractionModel.getParameters(field, state.getDate()));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final FieldSpacecraftState<Gradient> state) {
        try {
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field attractionModelField = SolidTides.class.getDeclaredField("attractionModel");
            attractionModelField.setAccessible(true);
            ForceModel attractionModel = (ForceModel) attractionModelField.get(forceModel);
            final int freeParameters = position.getX().getFreeParameters();
            Field<Gradient> field = GradientField.getField(freeParameters);
            return attractionModel.acceleration(state, attractionModel.getParameters(field, state.getDate()));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Test
    public void testDefaultInterpolation() {

        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(conventions, true);
        TimeScale utc = TimeScalesFactory.getUTC();
        UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                GravityFieldFactory.getNormalizedProvider(5, 5);

        // initialization
        AbsoluteDate date = new AbsoluteDate(1970, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         gravityField.getMu());

        AbsoluteDate target = date.shiftedBy(7 * Constants.JULIAN_DAY);
        ForceModel hf = new HolmesFeatherstoneAttractionModel(itrf, gravityField);
        SpacecraftState raw = propagate(orbit, target, hf,
                                        new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                                       gravityField.getTideSystem(), true, Double.NaN, -1,
                                                       conventions, ut1,
                                                       CelestialBodyFactory.getSun(),
                                                       CelestialBodyFactory.getMoon()));
        SpacecraftState interpolated = propagate(orbit, target, hf,
                                                 new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                                                gravityField.getTideSystem(),
                                                                conventions, ut1,
                                                                CelestialBodyFactory.getSun(),
                                                                CelestialBodyFactory.getMoon()));
        Assertions.assertEquals(0.0,
                            Vector3D.distance(raw.getPosition(),
                                              interpolated.getPosition()),
                            2.1e-5); // threshold would be 1.2e-3 for 30 days propagation

    }

    @Test
    public void testTideEffect1996() {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2003, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        doTestTideEffect(orbit, IERSConventions.IERS_1996, 44.09481, 0.00000);
    }

    @Test
    public void testTideEffect2003WithinAnnualPoleRange() {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(1969, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        doTestTideEffect(orbit, IERSConventions.IERS_2003, 73.14011, 0.87360);
    }

    @Test
    public void testTideEffect2003AfterAnnualPoleRange() {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2003, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        doTestTideEffect(orbit, IERSConventions.IERS_2003, 44.24999, 0.61752);
    }

    @Test
    public void testTideEffect2010BeforePoleModelChange() {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2003, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        doTestTideEffect(orbit, IERSConventions.IERS_2010, 44.25001, 0.70710);
    }

    @Test
    public void testTideEffect2010AfterModelChange() {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        doTestTideEffect(orbit, IERSConventions.IERS_2010, 24.02815, 30.37047);
    }

    @Test
    public void testStateJacobianVs80ImplementationNoPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), false,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());
        Assertions.assertTrue(forceModel.dependsOnPositionOnly());

        checkStateJacobianVs80Implementation(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             2.0e-15, false);

    }

    @Test
    public void testStateJacobianVs80ImplementationGradientNoPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), false,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());
        Assertions.assertTrue(forceModel.dependsOnPositionOnly());

        checkStateJacobianVs80ImplementationGradient(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             2.0e-15, false);

    }

    @Test
    public void testStateJacobianVs80ImplementationPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), true,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());

        checkStateJacobianVs80Implementation(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             2.0e-15, false);

    }

    @Test
    public void testStateJacobianVs80ImplementationGradientPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), true,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());

        checkStateJacobianVs80ImplementationGradient(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS),
                                             2.0e-15, false);

    }

    @Test
    public void testStateJacobianVsFiniteDifferencesNoPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), false,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());

        checkStateJacobianVsFiniteDifferences(new SpacecraftState(orbit), forceModel, DEFAULT_LAW,
                                              10.0, 2.0e-10, false);

    }

    @Test
    public void testStateJacobianVsFiniteDifferencesGradientNoPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), false,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());

        checkStateJacobianVsFiniteDifferencesGradient(new SpacecraftState(orbit), forceModel, DEFAULT_LAW,
                                              10.0, 2.0e-10, false);

    }

    @Test
    public void testStateJacobianVsFiniteDifferencesPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), true,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());

        checkStateJacobianVsFiniteDifferences(new SpacecraftState(orbit), forceModel, DEFAULT_LAW,
                                              10.0, 2.0e-10, false);

    }

    @Test
    public void testStateJacobianVsFiniteDifferencesGradientPoleTide()
        {
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2964, 8, 12, 11, 30, 00.000, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         Constants.EIGEN5C_EARTH_MU);
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);

        ForceModel forceModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                               gravityField.getTideSystem(), true,
                                               SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                               IERSConventions.IERS_2010, ut1,
                                               CelestialBodyFactory.getSun(),
                                               CelestialBodyFactory.getMoon());

        checkStateJacobianVsFiniteDifferencesGradient(new SpacecraftState(orbit), forceModel, DEFAULT_LAW,
                                              10.0, 2.0e-10, false);

    }
    
    /** Test added for <a href="https://gitlab.orekit.org/orekit/orekit/-/issues/1167">issue 1167</a>.
     * <p>Mostly for code coverage, with the introduction of interface {@link EventDetectorsProvider}
     */
    @Test
    public void testGetEventDetectors() {
        
        // Given
        // -----
        
        final IERSConventions conventions = IERSConventions.IERS_2010;
        final Frame itrf = FramesFactory.getITRF(conventions, true);
        final AbsoluteDate t0 = AbsoluteDate.ARBITRARY_EPOCH;

        final NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);
        final UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        
        // Create solid tides force model
        final ForceModel solidTidesModel = new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                                          gravityField.getTideSystem(), false,
                                                          SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                                          conventions, ut1,
                                                          CelestialBodyFactory.getSun(),
                                                          CelestialBodyFactory.getMoon());

        // When: Empty list
        List<EventDetector>                detectors      = solidTidesModel.getEventDetectors().collect(Collectors.toList());
        List<FieldEventDetector<Binary64>> fieldDetectors = solidTidesModel.getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());
        
        // Then
        Assertions.assertTrue(detectors.isEmpty());
        Assertions.assertTrue(fieldDetectors.isEmpty());
        
        // When: 1 span added to driver
        final List<ParameterDriver> drivers = solidTidesModel.getParametersDrivers();
        
        for (final ParameterDriver driver : drivers) {
            driver.addSpanAtDate(t0);
        }
        
        detectors      = solidTidesModel.getEventDetectors().collect(Collectors.toList());
        DateDetector dateDetector = (DateDetector) detectors.get(0);
        List<TimeStamped> dates = dateDetector.getDates();
        
        fieldDetectors = solidTidesModel.getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());
        FieldDateDetector<Binary64> fieldDateDetector = (FieldDateDetector<Binary64>) fieldDetectors.get(0);
        FieldAbsoluteDate<Binary64> fieldDate = fieldDateDetector.getDate();
        
        // Then
        Assertions.assertFalse(detectors.isEmpty());
        Assertions.assertEquals(1, detectors.size());
        Assertions.assertTrue(detectors.get(0) instanceof DateDetector);
        
        Assertions.assertEquals(1, dates.size());
        Assertions.assertEquals(0., dates.get(0).durationFrom(t0), 0.);
        
        Assertions.assertFalse(fieldDetectors.isEmpty());
        Assertions.assertEquals(1, fieldDetectors.size());
        Assertions.assertTrue(fieldDetectors.get(0) instanceof FieldDateDetector);
        Assertions.assertEquals(0., fieldDate.durationFrom(t0).getReal(), 0.);
    }

    private void doTestTideEffect(Orbit orbit, IERSConventions conventions, double delta1, double delta2) {

        Frame itrf    = FramesFactory.getITRF(conventions, true);
        UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        NormalizedSphericalHarmonicsProvider gravityField =
                GravityFieldFactory.getNormalizedProvider(5, 5);

        // initialization

        AbsoluteDate target = orbit.getDate().shiftedBy(7 * Constants.JULIAN_DAY);
        ForceModel hf = new HolmesFeatherstoneAttractionModel(itrf, gravityField);
        SpacecraftState noTides              = propagate(orbit, target, hf);
        SpacecraftState solidTidesNoPoleTide = propagate(orbit, target, hf,
                                                         new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                                                        gravityField.getTideSystem(), false,
                                                                        SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                                                        conventions, ut1,
                                                                        CelestialBodyFactory.getSun(),
                                                                        CelestialBodyFactory.getMoon()));
        SpacecraftState solidTidesPoleTide = propagate(orbit, target, hf,
                                                       new SolidTides(itrf, gravityField.getAe(), gravityField.getMu(),
                                                                      gravityField.getTideSystem(), true,
                                                                      SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                                                                      conventions, ut1,
                                                                      CelestialBodyFactory.getSun(),
                                                                      CelestialBodyFactory.getMoon()));
        Assertions.assertEquals(delta1,
                            Vector3D.distance(noTides.getPosition(),
                                              solidTidesNoPoleTide.getPosition()),
                            0.01);
        Assertions.assertEquals(delta2,
                            Vector3D.distance(solidTidesNoPoleTide.getPosition(),
                                              solidTidesPoleTide.getPosition()),
                            0.01);

    }

    private SpacecraftState propagate(Orbit orbit, AbsoluteDate target, ForceModel... forceModels)
        {
        double[][] tolerances = NumericalPropagator.tolerances(10, orbit, OrbitType.KEPLERIAN);
        AbstractIntegrator integrator = new DormandPrince853Integrator(1.0e-3, 300, tolerances[0], tolerances[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        for (ForceModel forceModel : forceModels) {
            propagator.addForceModel(forceModel);
        }
        propagator.setInitialState(new SpacecraftState(orbit));
        return propagator.propagate(target);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
    }

}
