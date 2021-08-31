/*
 * Licensed to the Hipparchus project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.orekit.propagation.events;

import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue;
import org.orekit.propagation.events.handlers.RecordAndContinue;


/**
 * Test event handling on a {@link KeplerianPropagator}.
 *
 * @author Evan Ward
 */
public class FieldCloseEventsAnalyticalKeplerianTest extends FieldCloseEventsAbstractTest<Decimal64> {

    public FieldCloseEventsAnalyticalKeplerianTest(){
        super(Decimal64Field.getInstance());
    }

    @Override
    public FieldPropagator<Decimal64> getPropagator(double stepSize) {
        return new FieldKeplerianPropagator<>(initialOrbit);
    }

    /* Extra test for analytic propagator that take big steps. */

    /** Test Analytic propagators take big steps. #830 */
    @Test
    public void testBigStep() {
        // setup
        FieldPropagator<Decimal64> propagator = getPropagator(1e100);
        propagator.setStepHandler(interpolator -> {});
        double period = 2 * initialOrbit.getKeplerianPeriod().getReal();

        FieldRecordAndContinue<TimeDetector, Decimal64> handler =
                new FieldRecordAndContinue<>();
        TimeDetector detector = new TimeDetector(1, period - 1)
                .withHandler(handler)
                .withMaxCheck(v(1e100))
                .withThreshold(v(1));
        propagator.addEventDetector(detector);

        // action
        propagator.propagate(epoch.shiftedBy(period));

        // verify no events
        Assert.assertEquals(0, handler.getEvents().size());
    }

    /** Test Analytic propagators take big steps. #830 */
    @Test
    public void testBigStepReverse() {
        // setup
        FieldPropagator<Decimal64> propagator = getPropagator(1e100);
        propagator.setStepHandler(interpolator -> {});
        double period = -2 * initialOrbit.getKeplerianPeriod().getReal();

        FieldRecordAndContinue<TimeDetector, Decimal64> handler =
                new FieldRecordAndContinue<>();
        TimeDetector detector = new TimeDetector(-1, period + 1)
                .withHandler(handler)
                .withMaxCheck(v(1e100))
                .withThreshold(v(1));
        propagator.addEventDetector(detector);

        // action
        propagator.propagate(epoch.shiftedBy(period));

        // verify no events
        Assert.assertEquals(0, handler.getEvents().size());
    }

}
