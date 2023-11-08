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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

public class DSSTNewtonianAttractionTest {

    private static final double eps  = 1.0e-19;

    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException {

        final Frame earthFrame = FramesFactory.getEME2000();

        final AbsoluteDate date = new AbsoluteDate(2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        final double mu = 3.986004415E14;
        final EquinoctialOrbit orbit = new EquinoctialOrbit(2.655989E7,
                                                            2.719455286199036E-4,
                                                            0.0041543085910249414,
                                                            -0.3412974060023717,
                                                            0.3960084733107685,
                                                            FastMath.toRadians(44.2377),
                                                            PositionAngleType.MEAN,
                                                            earthFrame,
                                                            date,
                                                            mu);

        final SpacecraftState state = new SpacecraftState(orbit);

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        final DSSTForceModel newton = new DSSTNewtonianAttraction(mu);

        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);

        final double[] daidt = newton.getMeanElementRate(state, auxiliaryElements, newton.getParameters());

        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(0.0,                   elements[0], eps);
        Assertions.assertEquals(0.0,                   elements[1], eps);
        Assertions.assertEquals(0.0,                   elements[2], eps);
        Assertions.assertEquals(0.0,                   elements[3], eps);
        Assertions.assertEquals(0.0,                   elements[4], eps);
        Assertions.assertEquals(1.4585773985530907E-4, elements[5], eps);

    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data");
    }

}
