/* Copyright 2002-2017 CS Systèmes d'Information
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

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class StopOnIncreasingTest {

    @Test
    public void testNoReset() throws OrekitException {
        SpacecraftState s = new SpacecraftState(new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                                                   0.048363, PositionAngle.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   AbsoluteDate.J2000_EPOCH,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assert.assertSame(s, new StopOnIncreasing<EventDetector>().resetState(null, s));
    }

    @Test
    public void testIncreasing() throws OrekitException {
        SpacecraftState s = new SpacecraftState(new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                                                   0.048363, PositionAngle.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   AbsoluteDate.J2000_EPOCH,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assert.assertSame(EventHandler.Action.STOP, new StopOnIncreasing<EventDetector>().eventOccurred(s, null, true));
    }

    @Test
    public void testDecreasing() throws OrekitException {
        SpacecraftState s = new SpacecraftState(new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                                                   0.048363, PositionAngle.MEAN,
                                                                   FramesFactory.getEME2000(),
                                                                   AbsoluteDate.J2000_EPOCH,
                                                                   Constants.EIGEN5C_EARTH_MU));
        Assert.assertSame(EventHandler.Action.CONTINUE, new StopOnIncreasing<EventDetector>().eventOccurred(s, null, false));
    }

}

