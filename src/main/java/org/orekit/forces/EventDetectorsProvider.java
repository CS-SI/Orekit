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
import java.util.List;
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

/** Interface for building event detectors for force models and maneuver parameters.
 *
 * <p>
 * Objects implementing this interface are mainly {@link ForceModel} and {@link DSSTForceModel}.
 *
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Maxime Journot
 * @since 12.0
 */
public interface EventDetectorsProvider {

    /** Default maximum checking interval for event detectors (s). */
    double DEFAULT_EVENT_DETECTORS_MAXCHECK = 60.;

    /** Get the discrete events related to the model.
     *
     * <p><b>This method is not intended to be called several time, only once by a propagator</b>,
     * as it has the side effect of rebuilding the events detectors when called
     *
     * @return stream of event detectors
     */
    Stream<EventDetector> getEventDetectors();

    /** Get the discrete events related to the model.
     *
     * <p><b>This method is not intended to be called several time, only once by a propagator</b>,
     * as it has the side effect of rebuilding the events detectors when called
     *
     * @param field field to which the state belongs
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return stream of event detectors
     */
    <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field);

    /** Get the discrete events related to the model from a list of {@link ParameterDriver}
     *
     * <p>Date detectors are used to cleanly stop the propagator and reset
     * the state derivatives at transition dates (if any) of the parameter drivers.
     *
     * <p><b>This method is not intended to be called several time, only once by a propagator</b>,
     * as it has the side effect of rebuilding the events detectors when called
     *
     * @param parameterDrivers list of parameter drivers
     * @return stream of event detectors
     */
    default Stream<EventDetector> getEventDetectors(List<ParameterDriver> parameterDrivers) {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<AbsoluteDate> transitionDates = new ArrayList<>();
        for (final ParameterDriver driver : parameterDrivers) {
            // Get the transitions' dates from the TimeSpanMap
            for (AbsoluteDate date : driver.getTransitionDates()) {
                transitionDates.add(date);
            }
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.size() == 0) {
            return Stream.empty();

        } else {
            // Sort transition dates chronologically
            transitionDates.sort(null);

            // Initialize the date detector
            final DateDetector datesDetector = new DateDetector(transitionDates.get(0)).
                            withMaxCheck(DEFAULT_EVENT_DETECTORS_MAXCHECK).
                            withHandler((state, d, increasing) -> {
                                return Action.RESET_DERIVATIVES;
                            });

            // Add all transitions' dates to the date detector
            for (int i = 1; i < transitionDates.size(); i++) {
                datesDetector.addEventDate(transitionDates.get(i));
            }
            // Return the detectors
            return Stream.of(datesDetector);
        }
    }

    /** Get the discrete events related to the model from a list of {@link ParameterDriver}
     *
     * <p>Date detectors are used to cleanly stop the propagator and reset
     * the state derivatives at transition dates (if any) of the parameter drivers.
     *
     * <p><b>This method is not intended to be called several time, only once by a propagator</b>,
     * as it has the side effect of rebuilding the events detectors when called
     *
     * @param parameterDrivers list of parameter drivers
     * @param field field to which the state belongs
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return stream of event detectors
     */
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field, List<ParameterDriver> parameterDrivers) {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<AbsoluteDate> transitionDates = new ArrayList<>();
        for (ParameterDriver driver : parameterDrivers) {
            // Get the transitions' dates from the TimeSpanMap
            for (AbsoluteDate date : driver.getTransitionDates()) {
                transitionDates.add(date);
            }
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.size() == 0) {
            return Stream.empty();

        } else {
            // Sort transition dates chronologically
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
            // Return the detectors
            return Stream.of(datesDetector);
        }
    }
}
