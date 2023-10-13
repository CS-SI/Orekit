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
package org.orekit.propagation.events;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStamped;
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

    /** Accuracy of switching events dates (s). */
    double DATATION_ACCURACY = 1.0e-10;

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
     * <p><b>This method is not intended to be called several times, only once by a propagator</b>,
     * as it has the side effect of rebuilding the events detectors when called.
     *
     * @param parameterDrivers list of parameter drivers
     * @return stream of event detectors
     */
    default Stream<EventDetector> getEventDetectors(List<ParameterDriver> parameterDrivers) {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<TimeStamped> transitionDates = new ArrayList<>();
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

            // Find shortest duration between 2 consecutive dates
            double shortestDuration = AbstractDetector.DEFAULT_MAXCHECK;
            for (int i = 1; i < transitionDates.size(); i++) {
                // Duration from current to previous date
                shortestDuration = FastMath.min(shortestDuration,
                                                transitionDates.get(i).durationFrom(transitionDates.get(i - 1)));
            }

            // Create the date detector containing all transition dates and return it
            // Max check set to half the shortest duration between 2 consecutive dates
            final DateDetector datesDetector = new DateDetector(transitionDates.toArray(new TimeStamped[0])).
                            withMaxCheck(0.5 * shortestDuration).
                            withMinGap(0.5 * shortestDuration).
                            withThreshold(DATATION_ACCURACY).
                            withHandler((state, d, increasing) -> {
                                return Action.RESET_DERIVATIVES;
                            });
            return Stream.of(datesDetector);
        }
    }

    /** Get the discrete events related to the model from a list of {@link ParameterDriver}
     *
     * <p>Date detectors are used to cleanly stop the propagator and reset
     * the state derivatives at transition dates (if any) of the parameter drivers.
     *
     * <p><b>This method is not intended to be called several times, only once by a propagator</b>,
     * as it has the side effect of rebuilding the events detectors when called.
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

            // Find shortest duration between 2 consecutive dates
            double shortestDuration = AbstractDetector.DEFAULT_MAXCHECK;
            for (int i = 1; i < transitionDates.size(); i++) {
                // Duration from current to previous date
                shortestDuration = FastMath.min(shortestDuration,
                                                transitionDates.get(i).durationFrom(transitionDates.get(i - 1)));
            }

            // Initialize the date detector
            // Max check set to half the shortest duration between 2 consecutive dates
            @SuppressWarnings("unchecked")
            final FieldDateDetector<T> datesDetector =
                            new FieldDateDetector<>(field, (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 0)).
                            withMaxCheck(0.5 * shortestDuration).
                            withMinGap(0.5 * shortestDuration).
                            withThreshold(field.getZero().newInstance(DATATION_ACCURACY)).
                            withHandler(( state, d, increasing) -> {
                                return Action.RESET_DERIVATIVES;
                            });
            // Add all transitions' dates to the date detector
            for (int i = 0; i < transitionDates.size(); i++) {
                datesDetector.addEventDate(new FieldAbsoluteDate<>(field, transitionDates.get(i)));
            }
            // Return the detectors
            return Stream.of(datesDetector);
        }
    }
}
