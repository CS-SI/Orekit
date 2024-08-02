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
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue.Event;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FieldRecordAndContinue}.
 *
 * @author Evan Ward
 */
class FieldRecordAndContinueTest {

    /** Field. */
    private static final Binary64Field field = Binary64Field.getInstance();

    /** check add and clear behavior. */
    @Test
    void testGetEvents() {
        // setup
        FieldRecordAndContinue<Binary64> handler = new FieldRecordAndContinue<>();
        FieldAbsoluteDate<Binary64> date =
                new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        Binary64 zero = date.getField().getZero();
        FieldDateDetector<Binary64> detector = new FieldDateDetector<>(field, date);
        Frame eci = FramesFactory.getGCRF();
        FieldOrbit<Binary64> orbit = new FieldKeplerianOrbit<>(
                v(6378137 + 500e3), v(0), v(0), v(0), v(0), v(0),
                PositionAngleType.TRUE, eci, date, zero.add(Constants.EIGEN5C_EARTH_MU));
        FieldSpacecraftState<Binary64> s1 = new FieldSpacecraftState<>(orbit);
        FieldSpacecraftState<Binary64> s2 = s1.shiftedBy(-10);
        FieldSpacecraftState<Binary64> s3 = s2.shiftedBy(1);
        FieldSpacecraftState<Binary64> s4 = s3.shiftedBy(1);

        // actions
        assertEquals(Action.CONTINUE, handler.eventOccurred(s1, detector, true));
        assertEquals(Action.CONTINUE, handler.eventOccurred(s2, detector, true));
        assertEquals(Action.CONTINUE, handler.eventOccurred(s3, detector, false));

        // verify
        List<Event<Binary64>> events = handler.getEvents();
        assertEquals(3, events.size());
        assertEquals(s1, events.get(0).getState());
        assertEquals(s2, events.get(1).getState());
        assertEquals(s3, events.get(2).getState());
        assertTrue(events.get(0).isIncreasing());
        assertTrue(events.get(1).isIncreasing());
        assertFalse(events.get(2).isIncreasing());
        for (Event<Binary64> event : events) {
            assertEquals(detector, event.getDetector());
        }

        // action: clear
        handler.clear();

        // verify is empty
        assertEquals(0, handler.getEvents().size());

        // action add more
        assertEquals(Action.CONTINUE, handler.eventOccurred(s4, detector, false));

        // verify new events
        events = handler.getEvents();
        assertEquals(1, events.size());
        assertEquals(s4, events.get(0).getState());
        assertFalse(events.get(0).isIncreasing());
        assertEquals(detector, events.get(0).getDetector());
    }

    /**
     * Box a double value.
     *
     * @param value to copy.
     * @return boxed {@code value}.
     */
    private Binary64 v(double value) {
        return new Binary64(value);
    }

}
