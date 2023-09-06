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
package org.orekit.forces.maneuvers.triggers;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.forces.maneuvers.trigger.StartStopEventsTrigger;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;

public class StartStopEventTriggerTest extends AbstractManeuverTriggersTest<StartStopEventsTrigger<DateDetector, DateDetector>> {

    public static class StartStopDates extends StartStopEventsTrigger<DateDetector, DateDetector> {

        public StartStopDates(final AbsoluteDate start, final AbsoluteDate stop) {
            super(new DateDetector(start, stop.shiftedBy(10.0)).
                  withMaxCheck(5.0).
                  withThreshold(1.0e-10).
                  withHandler(new StopOnEvent()),
                  new DateDetector(stop, stop.shiftedBy(20.0)).
                  withMaxCheck(5.0).
                  withThreshold(1.0e-10).
                  withHandler(new StopOnEvent()));
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertStartDetector(Field<S> field, DateDetector detector) {
            final FieldAbsoluteDate<S> target = new FieldAbsoluteDate<>(field, detector.getDates().get(0).getDate());
            @SuppressWarnings("unchecked")
            final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldDateDetector<>(field, target);
            return converted;
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertStopDetector(Field<S> field, DateDetector detector) {
            final FieldAbsoluteDate<S> target = new FieldAbsoluteDate<>(field, detector.getDates().get(0).getDate());
            @SuppressWarnings("unchecked")
            final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldDateDetector<>(field, target);
            return converted;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

    }

    protected StartStopEventsTrigger<DateDetector, DateDetector> createTrigger(final AbsoluteDate start, final AbsoluteDate stop) {
        return new StartStopDates(start, stop);
    }

    @Test
    public void testComponents() {
        StartStopEventsTrigger<DateDetector, DateDetector> trigger = createTrigger(AbsoluteDate.J2000_EPOCH,
                                                                                   AbsoluteDate.J2000_EPOCH.shiftedBy(100.0));
        final List<TimeStamped>    startDates = trigger.getStartDetector().getDates();
        final List<TimeStamped>    stopDates  = trigger.getStopDetector().getDates();
        Assertions.assertEquals(2,     trigger.getEventDetectors().count());
        Assertions.assertEquals(2,     trigger.getFieldEventDetectors(Binary64Field.getInstance()).count());
        Assertions.assertEquals(2,     startDates.size());
        Assertions.assertEquals(  0.0, startDates.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(110.0, startDates.get(1).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(2,     stopDates.size());
        Assertions.assertEquals(100.0, stopDates.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(120.0, stopDates.get(1).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

}
