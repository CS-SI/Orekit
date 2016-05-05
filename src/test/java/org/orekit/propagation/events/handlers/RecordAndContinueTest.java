/* Contributed in the public domain.
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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.EventHandler.Action;
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
     *
     * @throws OrekitException on error.
     */
    @Test
    public void testGetEvents() throws OrekitException {
        // setup
        RecordAndContinue<DateDetector> handler =
                new RecordAndContinue<DateDetector>();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        DateDetector detector = new DateDetector(date);
        Frame eci = FramesFactory.getGCRF();
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s1 = new SpacecraftState(orbit);
        SpacecraftState s2 = s1.shiftedBy(-10);
        SpacecraftState s3 = s2.shiftedBy(1);
        SpacecraftState s4 = s3.shiftedBy(1);

        // actions
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s1, detector, true));
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s2, detector, true));
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s3, detector, false));

        // verify
        List<Event<DateDetector>> events = handler.getEvents();
        Assert.assertEquals(3, events.size());
        Assert.assertEquals(s1, events.get(0).getState());
        Assert.assertEquals(s2, events.get(1).getState());
        Assert.assertEquals(s3, events.get(2).getState());
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(false, events.get(2).isIncreasing());
        for (Event<DateDetector> event : events) {
            Assert.assertEquals(detector, event.getDetector());
        }

        // action: clear
        handler.clear();

        // verify is empty
        Assert.assertEquals(0, handler.getEvents().size());

        // action add more
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s4, detector, false));

        // verify new events
        events = handler.getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(s4, events.get(0).getState());
        Assert.assertEquals(false, events.get(0).isIncreasing());
        Assert.assertEquals(detector, events.get(0).getDetector());
    }

}
