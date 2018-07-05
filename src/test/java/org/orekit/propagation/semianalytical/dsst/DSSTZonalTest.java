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
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class DSSTZonalTest {

    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {
        
        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);
        
        final Frame earthFrame = FramesFactory.getEME2000();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());
        
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
                                                 earthFrame,
                                                 initDate,
                                                 3.986004415E14);
        
        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);
        
        final DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);

        // Force model parameters
        final double[] parameters = zonal.getParameters();

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);
        
        // Initialize force model
        zonal.initialize(auxiliaryElements, true, parameters);
        
        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);
        
        final double[] daidt = zonal.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }
        
        Assert.assertEquals(0.0,                     elements[0], 1.e-25);
        Assert.assertEquals(1.3909396722346468E-11,  elements[1], 3.e-26);
        Assert.assertEquals(-2.0275977261372793E-13, elements[2], 3.e-27);
        Assert.assertEquals(3.087141512018238E-9,    elements[3], 1.e-24);
        Assert.assertEquals(2.6606317310148797E-9,   elements[4], 4.e-24);
        Assert.assertEquals(-3.659904725206694E-9,   elements[5], 1.e-24);
        
    }
    
    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
    
}
