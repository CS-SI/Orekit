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

import org.hipparchus.ode.ODEIntegrator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.events.intervals.DateDetectionAdaptableIntervalFactory;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


abstract class AbstractCloseEventsNumericalTest extends CloseEventsAbstractTest {

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize   of integrator.
     * @return a usable propagator.
     */
    public Propagator getPropagator(double stepSize, OrbitParamsType orbitParamsType) {
        final NumericalPropagator propagator = new NumericalPropagator(getIntegrator(stepSize, orbitParamsType));
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setOrbitParamsType(orbitParamsType);
        return propagator;
    }

    @Override
    public Propagator getPropagator(double stepSize) {
        return getPropagator(stepSize, OrbitParamsType.CARTESIAN);
    }

    abstract ODEIntegrator getIntegrator(final double stepSize, final OrbitParamsType orbitParamsType);

    @ParameterizedTest
    @EnumSource(value = OrbitParamsType.class, names = {"EQUINOCTIAL", "CARTESIAN"})
    void testOrbitType(final OrbitParamsType orbitParamsType) {
        // GIVEN
        final double stepSize = initialOrbit.getKeplerianPeriod() / 200;
        final Propagator propagator = getPropagator(stepSize, orbitParamsType);
        final AbsoluteDate firstDateToDetect = initialOrbit.getDate().shiftedBy(stepSize * 5.2);
        final AbsoluteDate secondDateToDetect = firstDateToDetect.getDate().shiftedBy(stepSize * 0.5);
        final RecordAndContinue recorder = new RecordAndContinue();
        final EventDetector detector = new TimeDetector(firstDateToDetect, secondDateToDetect).withHandler(recorder)
                .withMaxCheck(DateDetectionAdaptableIntervalFactory.getDatesDetectionInterval(firstDateToDetect, secondDateToDetect));
        propagator.addEventDetector(detector);
        final AbsoluteDate terminalDate = secondDateToDetect.shiftedBy(stepSize * 2);
        // WHEN
        propagator.propagate(terminalDate);
        // THEN
        final List<RecordAndContinue.Event> eventList = recorder.getEvents();
        propagator.resetInitialState(new SpacecraftState(initialOrbit));
        recorder.clear();
        propagator.propagate(terminalDate);
        final List<RecordAndContinue.Event> expectedList = recorder.getEvents();
        assertEquals(expectedList.size(), eventList.size());
        assertEquals(expectedList.getFirst().getState().getDate(), eventList.getFirst().getState().getDate());
    }
}
