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
package org.orekit.forces;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParametersDriversProvider;

/** Interface for building event detectords for force models.
 *
 * <p>
 * Objects implementing this interface are {@link ForceModel} and {@link DSSTForceModel}.
 *
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Maxime Journot
 * @since 12.0
 */
public interface EventDetectorsProvider extends ParametersDriversProvider {

    /** Default maximum checking interval for event detectors (s). */
    double DEFAULT_EVENT_DETECTORS_MAXCHECK = 60;

    /** Get the discrete events related to the model.
     * A date detector is used to cleanly stop the propagator and reset
     * the state derivatives at transition dates, useful when force parameter
     * drivers contains several values.
     * @return stream of events detectors
     */
    default Stream<EventDetector> getEventDetectors() {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<AbsoluteDate> transitionDates = new ArrayList<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // Get the transitions' dates from the TimeSpanMap
            for (AbsoluteDate date : driver.getTransitionDates()) {
                transitionDates.add(date);
            }
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.size() == 0) {
            return Stream.empty();

        } else {
            transitionDates.sort(null);
            // Initialize the date detector
            final DateDetector datesDetector = new DateDetector(transitionDates.get(0)).
                    withMaxCheck(DEFAULT_EVENT_DETECTORS_MAXCHECK).
                    withHandler(( state, d, increasing) -> {
                        return Action.RESET_DERIVATIVES;
                    });
            // Add all transitions' dates to the date detector
            for (int i = 1; i < transitionDates.size(); i++) {
                datesDetector.addEventDate(transitionDates.get(i));
            }
            // Return the detector
            return Stream.of(datesDetector);
        }
    }

    /** Get the discrete events related to the model.
     * @param field field to which the state belongs
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return stream of events detectors
     */
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getEventDetectors(Field<T> field) {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<AbsoluteDate> transitionDates = new ArrayList<>();
        for (ParameterDriver driver : getParametersDrivers()) {
        // Get the transitions' dates from the TimeSpanMap
            for (AbsoluteDate date : driver.getTransitionDates()) {
                transitionDates.add(date);
            }
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.size() == 0) {
            return Stream.empty();

        } else {
            transitionDates.sort(null);
            // Initialize the date detector
            final FieldDateDetector<T> datesDetector =
                    new FieldDateDetector<>(new FieldAbsoluteDate<>(field, transitionDates.get(0))).
                    withMaxCheck(field.getZero().add(DEFAULT_EVENT_DETECTORS_MAXCHECK)).
                    withHandler(( state, d, increasing) -> {
                        return Action.RESET_DERIVATIVES;
                    });
            // Add all transitions' dates to the date detector
            for (int i = 1; i < transitionDates.size(); i++) {
                datesDetector.addEventDate(new FieldAbsoluteDate<>(field, transitionDates.get(i)));
            }
            // Return the detector
            return Stream.of(datesDetector);
        }
    }
}
