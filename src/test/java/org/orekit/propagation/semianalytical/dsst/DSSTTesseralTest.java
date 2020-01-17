/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.util.Arrays;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import org.orekit.orbits.PositionAngle;
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
                                                 PositionAngle.TRUE,
                                                 frame,
                                                 initDate,
                                                 provider.getMu());
        
        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);
        
        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);
        
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        // Force model parameters
        final double[] parameters = tesseral.getParameters();

        // Initialize force model
        tesseral.initialize(auxiliaryElements, PropagationType.MEAN, parameters);

        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);
        
        final double[] daidt = tesseral.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }
        
        Assert.assertEquals(7.120011500375922E-5,   elements[0], 1.e-20);
        Assert.assertEquals(-1.109767646425212E-11, elements[1], 1.e-26);
        Assert.assertEquals(2.3036711391089307E-11, elements[2], 1.e-26);
        Assert.assertEquals(2.499304852807308E-12,  elements[3], 1.e-27);
        Assert.assertEquals(1.3899097178558372E-13, elements[4], 1.e-28);
        Assert.assertEquals(5.795522421338584E-12,  elements[5], 1.e-27);
        
    }
    
    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException {
        
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngle.MEAN,
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
        shortPeriodTerms.addAll(force.initialize(aux, PropagationType.OSCULATING, force.getParameters()));
        force.updateShortPeriodTerms(force.getParameters(), meanState);
        
        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }
        
        Assert.assertEquals(5.192409957353236,      y[0], 1.e-15);
        Assert.assertEquals(9.660364749662076E-7,   y[1], 1.e-22);
        Assert.assertEquals(1.542008987162059E-6,   y[2], 1.e-21);
        Assert.assertEquals(-4.9944146013126755E-8, y[3], 1.e-23);
        Assert.assertEquals(-4.500974242661177E-8,  y[4], 1.e-23);
        Assert.assertEquals(-2.785213556107612E-7,  y[5], 1.e-22);
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
                                                 PositionAngle.TRUE,
                                                 frame,
                                                 initDate,
                                                 provider.getMu());
        
        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);
        
        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Tesseral force model
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        tesseral.initialize(auxiliaryElements, PropagationType.MEAN, tesseral.getParameters());

        // Tesseral force model with default constructor
        final DSSTForceModel tesseralDefault = new DSSTTesseral(earthFrame,
                                                                Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                                provider);
        tesseralDefault.initialize(auxiliaryElements, PropagationType.MEAN, tesseralDefault.getParameters());

        // Compute mean element rate for the tesseral force model
        final double[] elements = tesseral.getMeanElementRate(state, auxiliaryElements, tesseral.getParameters());

        // Compute mean element rate for the "default" tesseral force model
        final double[] elementsDefault = tesseralDefault.getMeanElementRate(state, auxiliaryElements, tesseralDefault.getParameters());

        // Verify
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(elements[i], elementsDefault[i], Double.MIN_VALUE);
        }

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
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
        }
    }

    @Before
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
}
