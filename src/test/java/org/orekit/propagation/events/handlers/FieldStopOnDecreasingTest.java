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
package org.orekit.propagation.events.handlers;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

public class FieldStopOnDecreasingTest {

    @Test
    public void test(){

    }

    public <T extends RealFieldElement<T>> void testNoReset(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldKeplerianOrbit<T>(zero.add(24464560.0),zero.add( 0.7311),
                        zero.add(0.122138), zero.add(3.10686), zero.add(1.00681),
zero.add(0.048363), PositionAngle.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   date,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assert.assertSame(s, new FieldStopOnDecreasing<FieldEventDetector<T>, T>().resetState(null, s));
    }

    public <T extends RealFieldElement<T>> void testIncreasing(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldKeplerianOrbit<T>(zero.add(24464560.0),zero.add( 0.7311),
                        zero.add(0.122138), zero.add(3.10686), zero.add(1.00681),
                        zero.add(0.048363), PositionAngle.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   date,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assert.assertSame(EventHandler.Action.CONTINUE, new FieldStopOnDecreasing<FieldEventDetector<T>, T>().eventOccurred(s, null, true));
    }

    public <T extends RealFieldElement<T>> void testDecreasing(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldKeplerianOrbit<T>(zero.add(24464560.0), zero.add(0.7311),
                        zero.add(0.122138), zero.add(3.10686), zero.add(1.00681),
zero.add(0.048363), PositionAngle.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   date,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assert.assertSame(FieldEventHandler.Action.STOP, new FieldStopOnDecreasing<FieldEventDetector<T>, T>().eventOccurred(s, null, false));
    }

}

