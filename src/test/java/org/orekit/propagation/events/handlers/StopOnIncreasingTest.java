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

import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class StopOnIncreasingTest {

    @Test
    public void testNoReset() {
        SpacecraftState s = new SpacecraftState(new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                                                   0.048363, PositionAngleType.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   AbsoluteDate.J2000_EPOCH,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assertions.assertSame(s, new StopOnIncreasing().resetState(null, s));
    }

    @Test
    public void testIncreasing() {
        SpacecraftState s = new SpacecraftState(new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                                                   0.048363, PositionAngleType.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   AbsoluteDate.J2000_EPOCH,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assertions.assertSame(Action.STOP, new StopOnIncreasing().eventOccurred(s, null, true));
    }

    @Test
    public void testDecreasing() {
        SpacecraftState s = new SpacecraftState(new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                                                   0.048363, PositionAngleType.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   AbsoluteDate.J2000_EPOCH,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assertions.assertSame(Action.CONTINUE, new StopOnIncreasing().eventOccurred(s, null, false));
    }

}

