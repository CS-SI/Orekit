/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class DSSTTesseralTest {

    @Test
    public void testGetMeanElementRate() throws OrekitException {
        
        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);
        
        final Frame frame = FramesFactory.getEME2000();
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 4, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());
        
        // a  = 2655989.0 m
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
        tesseral.initialize(auxiliaryElements, true, parameters);

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
    public void testShortPeriodTerms() throws IllegalArgumentException, OrekitException {
        
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

        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-01", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                7120000.0, 1.0e-3, FastMath.toRadians(60.0),
                FastMath.toRadians(120.0), FastMath.toRadians(47.0),
                FastMath.toRadians(12.0),
                PositionAngle.TRUE, eci, initialDate, Constants.EIGEN5C_EARTH_MU);
        
        final SpacecraftState meanState = new SpacecraftState(orbit);
        
        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(orbit, 1);
       
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();

        force.registerAttitudeProvider(null);
        shortPeriodTerms.addAll(force.initialize(aux, false, force.getParameters()));
        force.updateShortPeriodTerms(force.getParameters(), meanState);
        
        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }
        
        Assert.assertEquals(-72.9028792607815,     y[0], 1.e-13);
        Assert.assertEquals(2.1249447786897624E-6, y[1], 1.e-21);
        Assert.assertEquals(-6.974560212491233E-6, y[2], 1.e-21);
        Assert.assertEquals(-1.997990379590397E-6, y[3], 1.e-21);
        Assert.assertEquals(9.602513303108225E-6,  y[4], 1.e-21);
        Assert.assertEquals(4.538526372438945E-5,  y[5], 1.e-20);
    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
}
