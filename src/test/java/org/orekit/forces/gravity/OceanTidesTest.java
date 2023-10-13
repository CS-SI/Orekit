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
import java.util.Map;
import java.util.stream.Collectors;

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
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
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

public class OceanTidesTest {

    @Test
    public void testDefaultInterpolation() {

        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(conventions, true);
        TimeScale utc = TimeScalesFactory.getUTC();
        UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
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
        SpacecraftState raw = propagate(orbit, target, hf, new OceanTides(itrf, gravityField.getAe(), gravityField.getMu(),
                       true, Double.NaN, -1,
                       6, 6, conventions, ut1));
        SpacecraftState interpolated = propagate(orbit, target, hf, new OceanTides(itrf, gravityField.getAe(), gravityField.getMu(),
                        6, 6, IERSConventions.IERS_2010, ut1));
        Assertions.assertEquals(0.0,
                            Vector3D.distance(raw.getPosition(),
                                              interpolated.getPosition()),
                            9.9e-6); // threshold would be 3.4e-5 for 30 days propagation

    }

    @Test
    public void testTideEffect1996() {
        doTestTideEffect(IERSConventions.IERS_1996, 3.66948, 0.00000);
    }

    @Test
    public void testTideEffect2003() {
        doTestTideEffect(IERSConventions.IERS_2003, 3.66941, 0.00000);
    }

    @Test
    public void testTideEffect2010() {
        doTestTideEffect(IERSConventions.IERS_2010, 3.66939, 0.08981);
    }

    private void doTestTideEffect(IERSConventions conventions, double delta1, double delta2) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(conventions, true);
        TimeScale utc = TimeScalesFactory.getUTC();
        UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        NormalizedSphericalHarmonicsProvider gravityField =
                GravityFieldFactory.getNormalizedProvider(5, 5);

        // initialization
        AbsoluteDate date = new AbsoluteDate(2003, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngleType.MEAN, eme2000, date,
                                         gravityField.getMu());

        AbsoluteDate target = date.shiftedBy(7 * Constants.JULIAN_DAY);
        ForceModel hf = new HolmesFeatherstoneAttractionModel(itrf, gravityField);
        SpacecraftState noTides              = propagate(orbit, target, hf);
        SpacecraftState oceanTidesNoPoleTide = propagate(orbit, target, hf, new OceanTides(itrf, gravityField.getAe(), gravityField.getMu(),
                        false, SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                        6, 6, conventions, ut1));
        SpacecraftState oceanTidesPoleTide = propagate(orbit, target, hf, new OceanTides(itrf, gravityField.getAe(), gravityField.getMu(),
                          true, SolidTides.DEFAULT_STEP, SolidTides.DEFAULT_POINTS,
                          6, 6, conventions, ut1));
        Assertions.assertEquals(delta1,
                            Vector3D.distance(noTides.getPosition(),
                                              oceanTidesNoPoleTide.getPosition()),
                            0.01);
        Assertions.assertEquals(delta2,
                            Vector3D.distance(oceanTidesNoPoleTide.getPosition(),
                                              oceanTidesPoleTide.getPosition()),
                            0.01);

    }

    @Test
    public void testNoGetParameter() {
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        ForceModel fm = new OceanTides(FramesFactory.getITRF(IERSConventions.IERS_1996, false),
                                       Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                       Constants.WGS84_EARTH_MU,
                                       5, 5, IERSConventions.IERS_1996,
                                       TimeScalesFactory.getUT1(IERSConventions.IERS_1996, false));
        Assertions.assertTrue(fm.dependsOnPositionOnly());
        Assertions.assertEquals(1, fm.getParametersDrivers().size());
        try {
            fm.getParameterDriver("unknown");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException miae) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, miae.getSpecifier());
        }
    }

    @Test
    public void testNoSetParameter() {
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        ForceModel fm = new OceanTides(FramesFactory.getITRF(IERSConventions.IERS_1996, false),
                                       Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                       Constants.WGS84_EARTH_MU,
                                       5, 5, IERSConventions.IERS_1996,
                                       TimeScalesFactory.getUT1(IERSConventions.IERS_1996, false));
        Assertions.assertEquals(1, fm.getParametersDrivers().size());
        try {
            fm.getParameterDriver("unknown").setValue(0.0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException miae) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, miae.getSpecifier());
        }
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

        final OneAxisEllipsoid body = ReferenceEllipsoid.getIers2010(itrf);

        final NormalizedSphericalHarmonicsProvider gravityField =
                        GravityFieldFactory.getNormalizedProvider(5, 5);
        
        // Add ocean tides data
        final AstronomicalAmplitudeReader aaReader =
                        new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataContext.getDefault().getDataProvidersManager().feed(aaReader.getSupportedNames(), aaReader);
        final Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        
        // Create ocean tides force model
        final ForceModel oceanTidesModel = new OceanTides(body.getBodyFrame(),
                                                          gravityField.getAe(), gravityField.getMu(),
                                                          5, 5, conventions,
                                                          TimeScalesFactory.getUT1(conventions, true));

        // When: Empty list
        List<EventDetector>                detectors      = oceanTidesModel.getEventDetectors().collect(Collectors.toList());
        List<FieldEventDetector<Binary64>> fieldDetectors = oceanTidesModel.getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());
        
        // Then
        Assertions.assertTrue(detectors.isEmpty());
        Assertions.assertTrue(fieldDetectors.isEmpty());
        
        // When: 1 span added to driver
        final List<ParameterDriver> drivers = oceanTidesModel.getParametersDrivers();
        
        for (final ParameterDriver driver : drivers) {
            driver.addSpanAtDate(t0);
        }
        
        detectors      = oceanTidesModel.getEventDetectors().collect(Collectors.toList());
        DateDetector dateDetector = (DateDetector) detectors.get(0);
        List<TimeStamped> dates = dateDetector.getDates();
        
        fieldDetectors = oceanTidesModel.getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());
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
        Utils.setDataRoot("regular-data:potential/icgem-format:tides");
    }

}
