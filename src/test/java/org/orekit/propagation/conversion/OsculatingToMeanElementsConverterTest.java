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
package org.orekit.propagation.conversion;

import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class OsculatingToMeanElementsConverterTest {

    @Test
    public void testTrivial() throws Exception {
        final AbsoluteDate date = new AbsoluteDate("2011-12-12T11:57:20.000", TimeScalesFactory.getUTC());
        final Orbit orbit1 = new CircularOrbit(7204535.848109436, -4.484755873986251E-4, 0.0011562979012178316,
                                               FastMath.toRadians(98.74341600466741), FastMath.toRadians(43.32990110790338),
                                               FastMath.toRadians(180.0), PositionAngle.MEAN, FramesFactory.getGCRF(),
                                               date, Constants.WGS84_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(orbit1);
        // Set up the numerical propagator
        final double[][] tol = NumericalPropagator.tolerances(1.0, initialState.getOrbit(), initialState.getOrbit().getType());
        final double minStep = 1.;
        final double maxStep = 200.;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);
        final NumericalPropagator prop = new NumericalPropagator(integrator);
        prop.setInitialState(initialState);

        final OsculatingToMeanElementsConverter converter = new OsculatingToMeanElementsConverter(initialState, 2, prop, 1.0);
        final SpacecraftState meanOrbit = converter.convert();

        final double eps  = 1.e-15;

        Assert.assertEquals(orbit1.getA(), meanOrbit.getA(), eps * orbit1.getA());
        Assert.assertEquals(orbit1.getEquinoctialEx(), meanOrbit.getEquinoctialEx(), eps);
        Assert.assertEquals(orbit1.getEquinoctialEy(), meanOrbit.getEquinoctialEy(), eps);
        Assert.assertEquals(orbit1.getHx(), meanOrbit.getHx(), eps);
        Assert.assertEquals(orbit1.getHy(), meanOrbit.getHy(), eps);
        Assert.assertEquals(MathUtils.normalizeAngle(orbit1.getLM(), FastMath.PI),
                            MathUtils.normalizeAngle(meanOrbit.getLM(), FastMath.PI), eps);
    }

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

}
