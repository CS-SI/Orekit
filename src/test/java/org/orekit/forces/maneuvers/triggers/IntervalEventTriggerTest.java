/* Copyright 2002-2022 CS GROUP
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
package org.orekit.forces.maneuvers.triggers;


import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.forces.maneuvers.trigger.IntervalEventTrigger;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;

public class IntervalEventTriggerTest extends AbstractManeuverTriggersTest<IntervalEventTrigger<DateDetector>> {

    public static class IntervalDates extends IntervalEventTrigger<DateDetector> {

        public IntervalDates(final AbsoluteDate start, final AbsoluteDate stop) {
            super(new DateDetector(0.5 * stop.durationFrom(start), 1.0e-10, start, stop).
                  withHandler(new StopOnEvent<DateDetector>()));
        }

        @Override
        protected <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertIntervalDetector(Field<S> field, DateDetector detector) {
            final S                    maxCheck = field.getZero().newInstance(detector.getMaxCheckInterval());
            final FieldAbsoluteDate<S> d0 = new FieldAbsoluteDate<>(field, detector.getDates().get(0).getDate());
            final FieldAbsoluteDate<S> d1 = new FieldAbsoluteDate<>(field, detector.getDates().get(1).getDate());
            @SuppressWarnings("unchecked")
            final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldDateDetector<>(maxCheck, null, d0, d1);
            return converted;
        }

    }

    protected IntervalDates createTrigger(final AbsoluteDate start, final AbsoluteDate stop) {
        return new IntervalDates(start, stop);
    }

    @Test
    public void testComponents() {
        IntervalDates trigger = createTrigger(AbsoluteDate.J2000_EPOCH,
                                              AbsoluteDate.J2000_EPOCH.shiftedBy(100.0));
        final List<TimeStamped>    dates = trigger.getFiringIntervalDetector().getDates();
        Assert.assertEquals(1,     trigger.getEventsDetectors().count());
        Assert.assertEquals(1,     trigger.getFieldEventsDetectors(Decimal64Field.getInstance()).count());
        Assert.assertEquals(2,     dates.size());
        Assert.assertEquals(  0.0, dates.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assert.assertEquals(100.0, dates.get(1).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

}
