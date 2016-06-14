/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.forces.gravity;

import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class OceanTidesTest {

    @Test
    public void testDefaultInterpolation() throws OrekitException {

        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(conventions, true);
        TimeScale utc = TimeScalesFactory.getUTC();
        UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        NormalizedSphericalHarmonicsProvider gravityField =
                GravityFieldFactory.getConstantNormalizedProvider(5, 5);

        // initialization
        AbsoluteDate date = new AbsoluteDate(1970, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngle.MEAN, eme2000, date,
                                         gravityField.getMu());

        AbsoluteDate target = date.shiftedBy(7 * Constants.JULIAN_DAY);
        ForceModel hf = new HolmesFeatherstoneAttractionModel(itrf, gravityField);
        SpacecraftState raw = propagate(orbit, target, hf, new OceanTides(itrf, gravityField.getAe(), gravityField.getMu(),
                       true, Double.NaN, -1,
                       6, 6, conventions, ut1));
        SpacecraftState interpolated = propagate(orbit, target, hf, new OceanTides(itrf, gravityField.getAe(), gravityField.getMu(),
                        6, 6, IERSConventions.IERS_2010, ut1));
        Assert.assertEquals(0.0,
                            Vector3D.distance(raw.getPVCoordinates().getPosition(),
                                              interpolated.getPVCoordinates().getPosition()),
                            9.9e-6); // threshold would be 3.4e-5 for 30 days propagation

    }

    @Test
    public void testTideEffect1996() throws OrekitException {
        doTestTideEffect(IERSConventions.IERS_1996, 3.66948, 0.00000);
    }

    @Test
    public void testTideEffect2003() throws OrekitException {
        doTestTideEffect(IERSConventions.IERS_2003, 3.66941, 0.00000);
    }

    @Test
    public void testTideEffect2010() throws OrekitException {
        doTestTideEffect(IERSConventions.IERS_2010, 3.66939, 0.08981);
    }

    private void doTestTideEffect(IERSConventions conventions, double delta1, double delta2) throws OrekitException {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(conventions, true);
        TimeScale utc = TimeScalesFactory.getUTC();
        UT1Scale  ut1 = TimeScalesFactory.getUT1(conventions, true);
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        NormalizedSphericalHarmonicsProvider gravityField =
                GravityFieldFactory.getConstantNormalizedProvider(5, 5);

        // initialization
        AbsoluteDate date = new AbsoluteDate(2003, 07, 01, 13, 59, 27.816, utc);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, FastMath.toRadians(98.7),
                                         FastMath.toRadians(93.0), FastMath.toRadians(15.0 * 22.5),
                                         0, PositionAngle.MEAN, eme2000, date,
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
        Assert.assertEquals(delta1,
                            Vector3D.distance(noTides.getPVCoordinates().getPosition(),
                                              oceanTidesNoPoleTide.getPVCoordinates().getPosition()),
                            0.01);
        Assert.assertEquals(delta2,
                            Vector3D.distance(oceanTidesNoPoleTide.getPVCoordinates().getPosition(),
                                              oceanTidesPoleTide.getPVCoordinates().getPosition()),
                            0.01);

    }

    @Test
    public void testNoGetParameter() throws OrekitException {
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
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
        Assert.assertEquals(0, fm.getParametersDrivers().length);
        try {
            fm.getParameterDriver("unknown");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException miae) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, miae.getSpecifier());
        }
    }

    @Test
    public void testNoSetParameter() throws OrekitException {
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
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
        Assert.assertEquals(0, fm.getParametersDrivers().length);
        try {
            fm.getParameterDriver("unknown").setValue(0.0);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException miae) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, miae.getSpecifier());
        }
    }

    private SpacecraftState propagate(Orbit orbit, AbsoluteDate target, ForceModel ... forceModels)
        throws OrekitException {
        double[][] tolerances = NumericalPropagator.tolerances(10, orbit, OrbitType.KEPLERIAN);
        AbstractIntegrator integrator = new DormandPrince853Integrator(1.0e-3, 300, tolerances[0], tolerances[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        for (ForceModel forceModel : forceModels) {
            propagator.addForceModel(forceModel);
        }
        propagator.setInitialState(new SpacecraftState(orbit));
        return propagator.propagate(target);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/icgem-format:tides");
    }

}
