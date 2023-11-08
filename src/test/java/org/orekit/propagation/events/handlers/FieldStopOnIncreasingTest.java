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
package org.orekit.propagation.events.handlers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

public class FieldStopOnIncreasingTest {

    @Test
    public void testNoReset() {
        doTestNoReset(Binary64Field.getInstance());
    }

    @Test
    public void testIbcreasing() {
        doTestIncreasing(Binary64Field.getInstance());
    }

    @Test
    public void testDecreasing() {
        doTestDecreasing(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoReset(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.7311),
                                                                                         zero.add(0.122138),   zero.add(3.10686),
                                                                                         zero.add(1.00681), zero.add(0.048363),
                                                                                         PositionAngleType.MEAN,
                                                                                         FramesFactory.getEME2000(),
                                                                                         date,
                                                                                         zero.add(Constants.EIGEN5C_EARTH_MU)));
        Assertions.assertSame(s, new FieldStopOnIncreasing<T>().resetState(null, s));
    }

    private <T extends CalculusFieldElement<T>> void doTestIncreasing(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.7311),
                                                                                         zero.add(0.122138),   zero.add(3.10686),
                                                                                         zero.add(1.00681), zero.add(0.048363),
                                                                                         PositionAngleType.MEAN,
                                                                                         FramesFactory.getEME2000(),
                                                                                         date,
                                                                                         zero.add(Constants.EIGEN5C_EARTH_MU)));
        Assertions.assertSame(Action.STOP, new FieldStopOnIncreasing<T>().eventOccurred(s, null, true));
    }

    private <T extends CalculusFieldElement<T>> void doTestDecreasing(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.7311),
                                                                                         zero.add(0.122138),   zero.add(3.10686),
                                                                                         zero.add(1.00681), zero.add(0.048363),
                                                                                         PositionAngleType.MEAN,
                                                                                         FramesFactory.getEME2000(),
                                                                                         date,
                                                                                         zero.add(Constants.EIGEN5C_EARTH_MU)));
        Assertions.assertSame(Action.CONTINUE, new FieldStopOnIncreasing<T>().eventOccurred(s, null, false));

    }

}

