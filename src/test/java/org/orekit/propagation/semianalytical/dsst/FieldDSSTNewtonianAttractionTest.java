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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

public class FieldDSSTNewtonianAttractionTest {

    private static final double eps  = 1.0e-19;

    @Test
    public void testGetMeanElementRate() {
        doTestGetMeanElementRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field) {

        final T zero = field.getZero();

        final Frame earthFrame = FramesFactory.getEME2000();

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2007, 04, 16, 0, 46, 42.400,
                                                                  TimeScalesFactory.getUTC());

        final double mu = 3.986004415E14;
        final FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(2.655989E7),
                                                                           zero.add(2.719455286199036E-4),
                                                                           zero.add(0.0041543085910249414),
                                                                           zero.add(-0.3412974060023717),
                                                                           zero.add(0.3960084733107685),
                                                                           zero.add(FastMath.toRadians(44.2377)),
                                                                           PositionAngleType.MEAN,
                                                                           earthFrame,
                                                                           date,
                                                                           zero.add(mu));

        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);

        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);

        final DSSTForceModel newton = new DSSTNewtonianAttraction(mu);

        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);

        final T[] daidt = newton.getMeanElementRate(state, auxiliaryElements, newton.getParameters(field));
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(0.0,                   elements[0].getReal(), eps);
        Assertions.assertEquals(0.0,                   elements[1].getReal(), eps);
        Assertions.assertEquals(0.0,                   elements[2].getReal(), eps);
        Assertions.assertEquals(0.0,                   elements[3].getReal(), eps);
        Assertions.assertEquals(0.0,                   elements[4].getReal(), eps);
        Assertions.assertEquals(1.4585773985530907E-4, elements[5].getReal(), eps);

    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data");
    }

}
