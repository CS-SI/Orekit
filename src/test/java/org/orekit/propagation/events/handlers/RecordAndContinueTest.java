/* Contributed in the public domain.
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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.RecordAndContinue.Event;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.List;

/**
 * Unit tests for {@link RecordAndContinue}.
 *
 * @author Evan Ward
 */
public class RecordAndContinueTest {

    /**
     * check add and clear behavior.
     */
    @Test
    public void testGetEvents() {
        // setup
        RecordAndContinue handler = new RecordAndContinue();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        DateDetector detector = new DateDetector(date);
        Frame eci = FramesFactory.getGCRF();
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                PositionAngleType.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s1 = new SpacecraftState(orbit);
        SpacecraftState s2 = s1.shiftedBy(-10);
        SpacecraftState s3 = s2.shiftedBy(1);
        SpacecraftState s4 = s3.shiftedBy(1);

        // actions
        Assertions.assertEquals(Action.CONTINUE, handler.eventOccurred(s1, detector, true));
        Assertions.assertEquals(Action.CONTINUE, handler.eventOccurred(s2, detector, true));
        Assertions.assertEquals(Action.CONTINUE, handler.eventOccurred(s3, detector, false));

        // verify
        List<Event> events = handler.getEvents();
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals(s1, events.get(0).getState());
        Assertions.assertEquals(s2, events.get(1).getState());
        Assertions.assertEquals(s3, events.get(2).getState());
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertEquals(false, events.get(2).isIncreasing());
        for (Event event : events) {
            Assertions.assertEquals(detector, event.getDetector());
        }

        // action: clear
        handler.clear();

        // verify is empty
        Assertions.assertEquals(0, handler.getEvents().size());

        // action add more
        Assertions.assertEquals(Action.CONTINUE, handler.eventOccurred(s4, detector, false));

        // verify new events
        events = handler.getEvents();
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(s4, events.get(0).getState());
        Assertions.assertEquals(false, events.get(0).isIncreasing());
        Assertions.assertEquals(detector, events.get(0).getDetector());
    }

}
