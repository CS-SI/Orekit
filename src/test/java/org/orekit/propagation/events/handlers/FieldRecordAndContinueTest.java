/* Contributed in the public domain.
 * Licensed to CS Group (CS) under one or more
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

import java.util.List;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue.Event;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Unit tests for {@link FieldRecordAndContinue}.
 *
 * @author Evan Ward
 */
public class FieldRecordAndContinueTest {

    /** Field. */
    private static final Decimal64Field field = Decimal64Field.getInstance();

    /** check add and clear behavior. */
    @Test
    public void testGetEvents() {
        // setup
        FieldRecordAndContinue<FieldDateDetector<Decimal64>, Decimal64> handler =
                new FieldRecordAndContinue<>();
        FieldAbsoluteDate<Decimal64> date =
                new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        Decimal64 zero = date.getField().getZero();
        FieldDateDetector<Decimal64> detector = new FieldDateDetector<>(date);
        Frame eci = FramesFactory.getGCRF();
        FieldOrbit<Decimal64> orbit = new FieldKeplerianOrbit<>(
                v(6378137 + 500e3), v(0), v(0), v(0), v(0), v(0),
                PositionAngle.TRUE, eci, date, zero.add(Constants.EIGEN5C_EARTH_MU));
        FieldSpacecraftState<Decimal64> s1 = new FieldSpacecraftState<>(orbit);
        FieldSpacecraftState<Decimal64> s2 = s1.shiftedBy(-10);
        FieldSpacecraftState<Decimal64> s3 = s2.shiftedBy(1);
        FieldSpacecraftState<Decimal64> s4 = s3.shiftedBy(1);

        // actions
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s1, detector, true));
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s2, detector, true));
        Assert.assertEquals(Action.CONTINUE, handler.eventOccurred(s3, detector, false));

        // verify
        List<Event<FieldDateDetector<Decimal64>, Decimal64>> events = handler.getEvents();
        Assert.assertEquals(3, events.size());
        Assert.assertEquals(s1, events.get(0).getState());
        Assert.assertEquals(s2, events.get(1).getState());
        Assert.assertEquals(s3, events.get(2).getState());
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(false, events.get(2).isIncreasing());
        for (Event<FieldDateDetector<Decimal64>, Decimal64> event : events) {
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

    /**
     * Box a double value.
     *
     * @param value to copy.
     * @return boxed {@code value}.
     */
    private Decimal64 v(double value) {
        return new Decimal64(value);
    }

}
