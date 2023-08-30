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
import org.orekit.forces.maneuvers.trigger.IntervalEventTrigger;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldAdaptableInterval;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;

public class IntervalEventTriggerTest extends AbstractManeuverTriggersTest<IntervalEventTrigger<DateDetector>> {

    public static class IntervalDates extends IntervalEventTrigger<DateDetector> {

        public IntervalDates(final AbsoluteDate start, final AbsoluteDate stop) {
            super(new DateDetector(start, stop).
                  withMaxCheck(0.5 * stop.durationFrom(start)).
                  withThreshold(1.0e-10).
                  withHandler(new StopOnEvent()));
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertIntervalDetector(Field<S> field, DateDetector detector) {
            final FieldAdaptableInterval<S> maxCheck  = s -> detector.getMaxCheckInterval().currentInterval(s.toSpacecraftState());
            final S                    threshold = field.getZero().newInstance(detector.getThreshold());
            final FieldAbsoluteDate<S> d0 = new FieldAbsoluteDate<>(field, detector.getDates().get(0).getDate());
            final FieldAbsoluteDate<S> d1 = new FieldAbsoluteDate<>(field, detector.getDates().get(1).getDate());
            @SuppressWarnings("unchecked")
            final FieldAbstractDetector<D, S> converted =
                (FieldAbstractDetector<D, S>) new FieldDateDetector<>(field, d0, d1).
                                              withMaxCheck(maxCheck).
                                              withThreshold(threshold);
            return converted;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
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
        Assertions.assertEquals(1,     trigger.getEventDetectors().count());
        Assertions.assertEquals(1,     trigger.getFieldEventDetectors(Binary64Field.getInstance()).count());
        Assertions.assertEquals(2,     dates.size());
        Assertions.assertEquals(  0.0, dates.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(100.0, dates.get(1).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

}
