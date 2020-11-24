/* Copyright 2020 Airbus Defence and Space
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Unit tests for {@link EventMultipleHandler}.
 *
 * @author Lara Hué
 */
public class EventMultipleHandlerTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    
     /**
     * check eventOccurred method.
     */
    @Test
    public void testEventOccurred() {
        // setup
        ContinueOnEvent<DateDetector> handler1 = new ContinueOnEvent<>();
        StopOnEvent<DateDetector> handler2 = new StopOnEvent<>();
        StopOnDecreasing<DateDetector> handler3 = new StopOnDecreasing<>();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        DateDetector detector = new DateDetector(date);
        Frame eci = FramesFactory.getGCRF();
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s = new SpacecraftState(orbit);

        // actions
        EventMultipleHandler<DateDetector> facade1 = new EventMultipleHandler<DateDetector>().addHandler(handler1).addHandler(handler2);
        Assert.assertEquals(Action.STOP, facade1.eventOccurred(s, detector, true));

        EventMultipleHandler<DateDetector> facade2 = new EventMultipleHandler<DateDetector>().addHandler(handler1).addHandler(handler3);
        Assert.assertEquals(Action.CONTINUE, facade2.eventOccurred(s, detector, true));
    }
    
    /**
     * check resetState method.
     */
    @Test
    public void testResetState() {
        // setup
        ContinueOnEvent<DateDetector> handler1 = new ContinueOnEvent<>();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        DateDetector detector = new DateDetector(date);
        Frame eci = FramesFactory.getGCRF();
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                                         PositionAngle.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s = new SpacecraftState(orbit);
        
        // actions
        EventHandler<DateDetector> handler2 = getHandler(10);
        EventHandler<DateDetector> handler3 = getHandler(20);
        EventMultipleHandler<DateDetector> facade = new EventMultipleHandler<DateDetector>().addHandlers(handler1, handler2, handler3);

        // verify
        Assert.assertEquals(Action.RESET_STATE, facade.eventOccurred(s, detector, true));
        Assert.assertEquals(s.shiftedBy(30).getOrbit().getDate(), facade.resetState(detector, s).getOrbit().getDate());
    }

    /**
     * get a handler that returns action RESET_STATE and shifts orbit
     */
    private EventHandler<DateDetector> getHandler(double timeShift) {

        return new EventHandler<DateDetector>() {

            @Override
            public Action eventOccurred(SpacecraftState s, DateDetector detector, boolean increasing) {
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(DateDetector detector, SpacecraftState oldState) {
                return oldState.shiftedBy(timeShift);
            }
        };
    }
}
