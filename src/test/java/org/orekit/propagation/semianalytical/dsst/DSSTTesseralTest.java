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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class DSSTTesseralTest {

    @Test
    public void testGetMeanElementRate() {

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 4);

        final Frame frame = FramesFactory.getEME2000();
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 4, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // a  = 26559890 m
        // ex = 2.719455286199036E-4
        // ey = 0.0041543085910249414
        // hx = -0.3412974060023717
        // hy = 0.3960084733107685
        // lM = 8.566537840341699 rad
        final Orbit orbit = new EquinoctialOrbit(2.655989E7,
                                                 2.719455286199036E-4,
                                                 0.0041543085910249414,
                                                 -0.3412974060023717,
                                                 0.3960084733107685,
                                                 8.566537840341699,
                                                 PositionAngleType.TRUE,
                                                 frame,
                                                 initDate,
                                                 provider.getMu());

        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        // Force model parameters
        final double[] parameters = tesseral.getParameters(orbit.getDate());

        // Initialize force model
        tesseral.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, parameters);

        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);

        final double[] daidt = tesseral.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(7.120011500375922E-5,   elements[0], 1.e-20);
        Assertions.assertEquals(-1.109767646425212E-11, elements[1], 1.e-26);
        Assertions.assertEquals(2.3036711391089307E-11, elements[2], 1.e-26);
        Assertions.assertEquals(2.499304852807308E-12,  elements[3], 1.e-27);
        Assertions.assertEquals(1.3899097178558372E-13, elements[4], 1.e-28);
        Assertions.assertEquals(5.795522421338584E-12,  elements[5], 1.e-27);

    }

    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException {

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngleType.MEAN,
                                         FramesFactory.getTOD(false),
                                         new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                         nshp.getMu());

        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));

        // Force model
        final DSSTForceModel force = new DSSTTesseral(earth.getBodyFrame(),
                                                      Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                      nshp, 8, 8, 4, 12, 8, 8, 4);

        // Initial state
        final SpacecraftState meanState = new SpacecraftState(orbit, 45.0);

        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(orbit, 1);

        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();

        force.registerAttitudeProvider(null);

        shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, force.getParameters(meanState.getDate())));
        force.updateShortPeriodTerms(force.getParametersAllValues(), meanState);

        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }

        Assertions.assertEquals(5.192409957353236,      y[0], 1.e-15);
        Assertions.assertEquals(9.660364749662076E-7,   y[1], 1.e-22);
        Assertions.assertEquals(1.542008987162059E-6,   y[2], 1.e-21);
        Assertions.assertEquals(-4.9944146013126755E-8, y[3], 1.e-22);
        Assertions.assertEquals(-4.500974242661177E-8,  y[4], 1.e-22);
        Assertions.assertEquals(-2.785213556107612E-7,  y[5], 1.e-21);
    }

    @Test
    public void testIssue625() {

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 4);

        final Frame frame = FramesFactory.getEME2000();
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 4, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // a  = 26559890 m
        // ex = 2.719455286199036E-4
        // ey = 0.0041543085910249414
        // hx = -0.3412974060023717
        // hy = 0.3960084733107685
        // lM = 8.566537840341699 rad
        final Orbit orbit = new EquinoctialOrbit(2.655989E7,
                                                 2.719455286199036E-4,
                                                 0.0041543085910249414,
                                                 -0.3412974060023717,
                                                 0.3960084733107685,
                                                 8.566537840341699,
                                                 PositionAngleType.TRUE,
                                                 frame,
                                                 initDate,
                                                 provider.getMu());

        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Tesseral force model
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        tesseral.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, tesseral.getParameters(orbit.getDate()));

        // Tesseral force model with default constructor
        final DSSTForceModel tesseralDefault = new DSSTTesseral(earthFrame,
                                                                Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                                provider);
        tesseralDefault.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, tesseralDefault.getParameters(orbit.getDate()));

        // Compute mean element rate for the tesseral force model
        final double[] elements = tesseral.getMeanElementRate(state, auxiliaryElements, tesseral.getParameters(orbit.getDate()));

        // Compute mean element rate for the "default" tesseral force model
        final double[] elementsDefault = tesseralDefault.getMeanElementRate(state, auxiliaryElements, tesseralDefault.getParameters(orbit.getDate()));

        // Verify
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(elements[i], elementsDefault[i], Double.MIN_VALUE);
        }

    }

    @Test
    public void testIssue736() {

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 4);

        // Frames and epoch
        final Frame frame = FramesFactory.getEME2000();
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 4, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // Initial orbit
        final Orbit orbit = new EquinoctialOrbit(2.655989E7, 2.719455286199036E-4, 0.0041543085910249414,
                                                 -0.3412974060023717, 0.3960084733107685,
                                                 8.566537840341699, PositionAngleType.TRUE,
                                                 frame, initDate, provider.getMu());

        // Force model
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);
        final double[] parameters = tesseral.getParameters(orbit.getDate());

        // Initialize force model
        tesseral.initializeShortPeriodTerms(new AuxiliaryElements(orbit, 1), PropagationType.MEAN, parameters);

        // Eccentricity shift
        final Orbit shfitedOrbit = new EquinoctialOrbit(2.655989E7, 0.02, 0.0041543085910249414,
                                                        -0.3412974060023717, 0.3960084733107685,
                                                        8.566537840341699, PositionAngleType.TRUE,
                                                        frame, initDate, provider.getMu());

        final double[] elements = tesseral.getMeanElementRate(new SpacecraftState(shfitedOrbit), new AuxiliaryElements(shfitedOrbit, 1), parameters);

        // The purpose of this test is not to verify a specific value.
        // Its purpose is to verify that a NullPointerException does not
        // occur when calculating initial values of Hansen Coefficients
        for (int i = 0; i < elements.length; i++) {
            Assertions.assertTrue(elements[i] != 0);
        }

    }

    /** Test issue 672:
     * DSST Propagator was crashing with tesseral harmonics of the gravity field
     * when the order is lower or equal to 3.
     */
    @Test
    public void testIssue672() {

        // GIVEN
        // -----

        // Test with a central Body geopotential of 2x2
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(2, 2);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        // GPS Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2007, 4, 16, 0, 46, 42.400,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(26559890.,
                                               0.0041632,
                                               FastMath.toRadians(55.2),
                                               FastMath.toRadians(315.4985),
                                               FastMath.toRadians(130.7562),
                                               FastMath.toRadians(44.2377),
                                               PositionAngleType.MEAN,
                                               FramesFactory.getEME2000(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // Tesseral force model
        final DSSTTesseral dsstTesseral =
                        new DSSTTesseral(earthFrame,
                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);

        // Initialize short period terms
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        final AuxiliaryElements aux = new AuxiliaryElements(orbit, 1);
        shortPeriodTerms.addAll(dsstTesseral.initializeShortPeriodTerms(aux,
                                                                        PropagationType.OSCULATING,
                                                                        dsstTesseral.getParameters(orbit.getDate())));
        // WHEN
        // ----

        // Updating short period terms
        dsstTesseral.updateShortPeriodTerms(dsstTesseral.getParametersAllValues(), initialState);

        // THEN
        // ----

        // Verify that no exception was raised
        Assertions.assertNotNull(shortPeriodTerms);

    }

    /** Test issue 672 with OUT_OF_RANGE_SIMPLE exception:
     * 1. DSSTTesseral should throw an exception if input "maxEccPowTesseralSP" is greater than the order of the gravity field.
     * 2. DSSTTesseral should not throw an exception if order = 0, even if input "maxEccPowTesseralSP" is greater than the
     *    order of the gravity field (0 in this case). This last behavior was added for non-regression purposes.
     */
    @Test
    public void testIssue672OutOfRangeException() {

        // Throwing exception
        // ------------------

        // GIVEN
        // Test with a central Body geopotential of 3x3
        int degree = 3;
        int order  = 2;
        UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(degree, order);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        try {

            // WHEN
            // Tesseral force model: force "maxEccPowTesseralSP" to 3 which is greater than the order (2)
            new DSSTTesseral(earthFrame,
                             Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                             degree, order, 3,  FastMath.min(12, degree + 4),
                             degree, order, FastMath.min(4, degree - 2));
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {

            // THEN
            Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
        }


        // NOT throwing exception
        // ----------------------

        // GIVEN
        // Test with a central Body geopotential of 2x0
        degree = 2;
        order  = 0;
        provider = GravityFieldFactory.getUnnormalizedProvider(degree, order);

        // WHEN
        // Tesseral force model: force "maxEccPowTesseralSP" to 4 which is greater than the order (0)
        final DSSTTesseral dsstTesseral = new DSSTTesseral(earthFrame,
                                                           Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                           degree, order, 4,  FastMath.min(12, degree + 4),
                                                           degree, order, FastMath.min(4, degree - 2));
        // THEN: Verify that no exception was raised
        Assertions.assertNotNull(dsstTesseral);

    }

    @Test
    public void testOutOfRangeException() {
        // Central Body geopotential 1x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(1, 0);
        // Earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getGTOD(false));
        try {
            @SuppressWarnings("unused")
            final DSSTForceModel tesseral = new DSSTTesseral(earth.getBodyFrame(),
                                                             Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                             provider);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
        }
    }

    @Test
    public void testGetMaxEccPow()
                    throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 4);;
                        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
                        final DSSTTesseral force = new DSSTTesseral(earthFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);
                        Method getMaxEccPow = DSSTTesseral.class.getDeclaredMethod("getMaxEccPow", Double.TYPE);
                        getMaxEccPow.setAccessible(true);
                        Assertions.assertEquals(3,  getMaxEccPow.invoke(force, 0.0));
                        Assertions.assertEquals(4,  getMaxEccPow.invoke(force, 0.01));
                        Assertions.assertEquals(7,  getMaxEccPow.invoke(force, 0.08));
                        Assertions.assertEquals(10, getMaxEccPow.invoke(force, 0.15));
                        Assertions.assertEquals(12, getMaxEccPow.invoke(force, 0.25));
                        Assertions.assertEquals(15, getMaxEccPow.invoke(force, 0.35));
                        Assertions.assertEquals(20, getMaxEccPow.invoke(force, 1.0));
    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
}
