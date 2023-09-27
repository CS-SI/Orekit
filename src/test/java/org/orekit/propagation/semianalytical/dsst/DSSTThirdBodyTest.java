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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DSSTThirdBodyTest {

    private static final double eps  = 3.5e-25;

    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException {

        final Frame earthFrame = FramesFactory.getEME2000();
        final AbsoluteDate initDate = new AbsoluteDate(2003, 07, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());

        final double mu = 3.986004415E14;
        // a    = 42163393.0 m
        // ex =  -0.25925449177598586
        // ey =  -0.06946703170551687
        // hx =   0.15995912655021305
        // hy =  -0.5969755874197339
        // lM   = 15.47576793123677 rad
        final Orbit orbit = new EquinoctialOrbit(4.2163393E7,
                                                 -0.25925449177598586,
                                                 -0.06946703170551687,
                                                 0.15995912655021305,
                                                 -0.5969755874197339,
                                                 15.47576793123677,
                                                 PositionAngleType.TRUE,
                                                 earthFrame,
                                                 initDate,
                                                 mu);

        final SpacecraftState state = new SpacecraftState(orbit, 1000.0);

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), mu);

        // Force model parameters
        final double[] parameters = moon.getParameters(orbit.getDate());

        // Initialize force model
        moon.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, parameters);

        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);

        final double[] daidt = moon.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(0.0,                    elements[0], eps);
        Assertions.assertEquals(4.346622384804537E-10,  elements[1], eps);
        Assertions.assertEquals(7.293879548440941E-10,  elements[2], eps);
        Assertions.assertEquals(7.465699631747887E-11,  elements[3], eps);
        Assertions.assertEquals(3.9170221137233836E-10, elements[4], eps);
        Assertions.assertEquals(-3.178319341840074E-10, elements[5], eps);

    }

    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException {
        final SpacecraftState meanState = getGEOState();

        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu());

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(moon);

        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(meanState.getOrbit(), 1);

        // Set the force models
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();

        for (final DSSTForceModel force : forces) {
            force.registerAttitudeProvider(null);
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, force.getParameters(meanState.getDate())));
            force.updateShortPeriodTerms(force.getParametersAllValues(), meanState);
        }

        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }

        Assertions.assertEquals(-413.20633326933154,    y[0], 1.e-14);
        Assertions.assertEquals(-1.8060137920197483E-5, y[1], 1.e-20);
        Assertions.assertEquals(-2.8416367511811057E-5, y[2], 1.e-20);
        Assertions.assertEquals(-2.791424363476855E-6,  y[3], 1.e-21);
        Assertions.assertEquals(1.8817187527805853E-6,  y[4], 1.e-21);
        Assertions.assertEquals(-3.423664701811889E-5,  y[5], 1.e-20);
    }

    private SpacecraftState getGEOState() throws IllegalArgumentException {
        // No shadow at this date
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngleType.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);
        return new SpacecraftState(orbit);
    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data");
    }

}
