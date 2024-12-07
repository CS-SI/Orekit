/* Copyright 2002-2024 CS GROUP
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.propagation.events.intervals.DateDetectionAdaptableIntervalFactory;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
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
    double DATATION_ACCURACY = DateDetector.DEFAULT_THRESHOLD;

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
            transitionDates.addAll(Arrays.asList(driver.getTransitionDates()));
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.isEmpty()) {
            return Stream.empty();

        } else {
            // Create the date detector containing all transition dates and return it
            final DateDetector detector = getDateDetector(transitionDates.toArray(new TimeStamped[0]));
            return Stream.of(detector);
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
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field,
                                                                                                     List<ParameterDriver> parameterDrivers) {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<AbsoluteDate> transitionDates = new ArrayList<>();
        for (ParameterDriver driver : parameterDrivers) {
            // Get the transitions' dates from the TimeSpanMap
            transitionDates.addAll(Arrays.asList(driver.getTransitionDates()));
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.isEmpty()) {
            return Stream.empty();

        } else {
            // Initialize the date detector
            final FieldDateDetector<T> datesDetector = getFieldDateDetector(field,
                    transitionDates.toArray(new AbsoluteDate[0]));
            // Return the detectors
            return Stream.of(datesDetector);
        }
    }

    /**
     * Method building dates' detector.
     * @param timeStampeds dates to detect
     * @return dates detector
     * @since 13.0
     */
    default DateDetector getDateDetector(final TimeStamped... timeStampeds) {
        final AdaptableInterval maxCheck = DateDetectionAdaptableIntervalFactory.getDatesDetectionInterval(
                timeStampeds);
        final double minGap = DateDetectionAdaptableIntervalFactory.getMinGap(timeStampeds) / 2;
        final DateDetector dateDetector = new DateDetector().withMaxCheck(maxCheck).withMinGap(minGap).
                withThreshold(DATATION_ACCURACY).withHandler(new ResetDerivativesOnEvent());
        final SortedSet<AbsoluteDate> sortedDates = new TreeSet<>(new ChronologicalComparator());
        sortedDates.addAll(Arrays.stream(timeStampeds).map(TimeStamped::getDate).collect(Collectors.toList()));
        for (final AbsoluteDate date : sortedDates) {
            dateDetector.addEventDate(date);
        }
        return dateDetector;
    }

    /**
     * Method building dates' detector.
     * @param field field
     * @param timeStampeds dates to detect
     * @param <T> field type
     * @return dates detector
     * @since 13.0
     */
    default <T extends CalculusFieldElement<T>> FieldDateDetector<T> getFieldDateDetector(final Field<T> field,
                                                                                          final TimeStamped... timeStampeds) {
        @SuppressWarnings("unchecked")
        final FieldAdaptableInterval<T> maxCheck = DateDetectionAdaptableIntervalFactory.getDatesDetectionFieldInterval(
                Arrays.stream(timeStampeds).map(timeStamped -> new FieldAbsoluteDate<>(field, timeStamped.getDate()))
                        .toArray(FieldTimeStamped[]::new));
        final double minGap = DateDetectionAdaptableIntervalFactory.getMinGap(timeStampeds) / 2;
        final FieldDateDetector<T> fieldDateDetector = new FieldDateDetector<>(field).
                withHandler(new FieldResetDerivativesOnEvent<>()).withMaxCheck(maxCheck).withMinGap(minGap).
                withThreshold(field.getZero().newInstance(DATATION_ACCURACY));
        final SortedSet<AbsoluteDate> sortedDates = new TreeSet<>(new ChronologicalComparator());
        sortedDates.addAll(Arrays.stream(timeStampeds).map(TimeStamped::getDate).collect(Collectors.toList()));
        for (final AbsoluteDate date : sortedDates) {
            fieldDateDetector.addEventDate(new FieldAbsoluteDate<>(field, date));
        }
        return fieldDateDetector;
    }
}
