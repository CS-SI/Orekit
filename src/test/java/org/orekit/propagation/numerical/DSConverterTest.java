/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.numerical;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.Relativity;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class DSConverterTest {

    @Test
    @SuppressWarnings("deprecation")
    public void testConversion() {

        // Define a spacecraft state
        double mass = 2500;
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        KeplerianOrbit orbit = new KeplerianOrbit(7187990.2, 0.5e-4, 1.71, 1.96, FastMath.toRadians(261),
                                                  0., PositionAngle.TRUE, FramesFactory.getEME2000(),
                                                  date, Constants.WGS84_EARTH_MU);
        SpacecraftState state = new SpacecraftState(orbit, mass);

        // Force model
        final ForceModel force = new Relativity(Constants.WGS84_EARTH_MU);
        force.getParametersDrivers()[0].setSelected(true);

        // Convert state
        DSConverter converter = new DSConverter(state, 6, Propagator.DEFAULT_LAW);
        FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(force);
        DerivativeStructure[] parameters = converter.getParameters(dsState, force);

        // Verify
        Assert.assertEquals(7, dsState.getA().getFreeParameters());
        Assert.assertEquals(1, dsState.getA().getOrder());
        Assert.assertEquals(1, parameters.length);
        Assert.assertEquals(6, converter.getFreeStateParameters());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
