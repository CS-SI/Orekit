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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Unit tests for {@link EventMultipleHandler}.
 *
 * @author Lara Hué
 */
public class EventMultipleHandlerTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

     /**
     * check eventOccurred method.
     */
    @Test
    public void testEventOccurred() {
        // setup
        ContinueOnEvent handler1 = new ContinueOnEvent();
        StopOnEvent handler2 = new StopOnEvent();
        StopOnDecreasing handler3 = new StopOnDecreasing();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        DateDetector detector = new DateDetector(date);
        Frame eci = FramesFactory.getGCRF();
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                PositionAngleType.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s = new SpacecraftState(orbit);

        // actions
        EventMultipleHandler facade1 = new EventMultipleHandler().addHandler(handler1).addHandler(handler2);
        Assertions.assertEquals(Action.STOP, facade1.eventOccurred(s, detector, true));

        EventMultipleHandler facade2 = new EventMultipleHandler().addHandler(handler1).addHandler(handler3);
        Assertions.assertEquals(Action.CONTINUE, facade2.eventOccurred(s, detector, true));
    }

    /**
     * check resetState method.
     */
    @Test
    public void testResetState() {
        // setup
        ContinueOnEvent handler1 = new ContinueOnEvent();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        DateDetector detector = new DateDetector(date);
        Frame eci = FramesFactory.getGCRF();
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                                         PositionAngleType.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s = new SpacecraftState(orbit);

        // actions
        EventHandler handler2 = getHandler(10);
        EventHandler handler3 = getHandler(20);
        EventMultipleHandler facade = new EventMultipleHandler().addHandlers(handler1, handler2, handler3);

        // verify
        Assertions.assertEquals(Action.RESET_STATE, facade.eventOccurred(s, detector, true));
        Assertions.assertEquals(s.shiftedBy(30).getOrbit().getDate(), facade.resetState(detector, s).getOrbit().getDate());
    }

    /**
     * get a handler that returns action RESET_STATE and shifts orbit
     */
    private EventHandler getHandler(double timeShift) {

        return new EventHandler() {

            @Override
            public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
                return oldState.shiftedBy(timeShift);
            }
        };
    }
}
