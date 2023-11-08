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

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DateDriver;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/** Detector for date intervals that may be offset thanks to parameter drivers.
 * <p>
 * Two dual views can be used for date intervals: either start date/stop date or
 * median date/duration. {@link #getStartDriver() start}/{@link #getStopDriver() stop}
 * drivers and {@link #getMedianDriver() median}/{@link #getDurationDriver() duration}
 * drivers work in pair. Both drivers in one pair can be selected and their changes will
 * be propagated to the other pair, but attempting to select drivers in both
 * pairs at the same time will trigger an exception. Changing the value of a driver
 * that is not selected should be avoided as it leads to inconsistencies between the pairs.
 * </p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 11.1
 */
public class FieldParameterDrivenDateIntervalDetector<T extends CalculusFieldElement<T>>
    extends FieldAbstractDetector<FieldParameterDrivenDateIntervalDetector<T>, T> {

    /** Default suffix for start driver. */
    public static final String START_SUFFIX = "_START";

    /** Default suffix for stop driver. */
    public static final String STOP_SUFFIX = "_STOP";

    /** Default suffix for median driver. */
    public static final String MEDIAN_SUFFIX = "_MEDIAN";

    /** Default suffix for duration driver. */
    public static final String DURATION_SUFFIX = "_DURATION";

    /** Reference interval start driver. */
    private DateDriver start;

    /** Reference interval stop driver. */
    private DateDriver stop;

    /** Median date driver. */
    private DateDriver median;

    /** Duration driver. */
    private ParameterDriver duration;

    /** Build a new instance.
     * @param field field to which the elements belong
     * @param prefix prefix to use for parameter drivers names
     * @param refMedian reference interval median date
     * @param refDuration reference duration
     */
    public FieldParameterDrivenDateIntervalDetector(final Field<T> field, final String prefix,
                                                    final AbsoluteDate refMedian, final double refDuration) {
        this(field, prefix,
             refMedian.shiftedBy(-0.5 * refDuration),
             refMedian.shiftedBy(+0.5 * refDuration));
    }

    /** Build a new instance.
     * @param field field to which the elements belong
     * @param prefix prefix to use for parameter drivers names
     * @param refStart reference interval start date
     * @param refStop reference interval stop date
     */
    public FieldParameterDrivenDateIntervalDetector(final Field<T> field, final String prefix,
                                                    final AbsoluteDate refStart, final AbsoluteDate refStop) {
        this(s -> DEFAULT_MAXCHECK,
             field.getZero().newInstance(DEFAULT_THRESHOLD),
             DEFAULT_MAX_ITER,
             new FieldStopOnEvent<>(),
             new DateDriver(refStart, prefix + START_SUFFIX, true),
             new DateDriver(refStop, prefix + STOP_SUFFIX, false),
             new DateDriver(refStart.shiftedBy(0.5 * refStop.durationFrom(refStart)), prefix + MEDIAN_SUFFIX, true),
             new ParameterDriver(prefix + DURATION_SUFFIX, refStop.durationFrom(refStart), 1.0, 0.0, Double.POSITIVE_INFINITY));
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param start reference interval start driver
     * @param stop reference interval stop driver
     * @param median median date driver
     * @param duration duration driver
     */
    protected FieldParameterDrivenDateIntervalDetector(final FieldAdaptableInterval<T> maxCheck, final T threshold, final int maxIter,
                                                       final FieldEventHandler<T> handler,
                                                       final DateDriver start, final DateDriver stop,
                                                       final DateDriver median, final ParameterDriver duration) {
        super(maxCheck, threshold, maxIter, handler);
        this.start    = start;
        this.stop     = stop;
        this.median   = median;
        this.duration = duration;

        // set up delegation between drivers
        replaceBindingObserver(start,    new StartObserver());
        replaceBindingObserver(stop,     new StopObserver());
        replaceBindingObserver(median,   new MedianObserver());
        replaceBindingObserver(duration, new DurationObserver());

    }

    /** Replace binding observers.
     * @param driver driver for whose binding observers should be replaced
     * @param bindingObserver new binding observer
     */
    private void replaceBindingObserver(final ParameterDriver driver, final BindingObserver bindingObserver) {

        // remove the previous binding observers
        final List<ParameterObserver> original = driver.
                                                 getObservers().
                                                 stream().
                                                 filter(observer -> observer instanceof FieldParameterDrivenDateIntervalDetector.BindingObserver).
                                                 collect(Collectors.toList());
        original.forEach(observer -> driver.removeObserver(observer));

        driver.addObserver(bindingObserver);

    }

    /** {@inheritDoc} */
    @Override
    protected FieldParameterDrivenDateIntervalDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold, final int newMaxIter,
                                                                 final FieldEventHandler<T> newHandler) {
        return new FieldParameterDrivenDateIntervalDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                                              start, stop, median, duration);
    }

    /** Get the driver for start date.
     * <p>
     * Note that the start date is automatically adjusted if either
     * {@link #getMedianDriver() median date} or {@link #getDurationDriver() duration}
     * are {@link ParameterDriver#isSelected() selected} and changed.
     * </p>
     * @return driver for start date
     */
    public DateDriver getStartDriver() {
        return start;
    }

    /** Get the driver for stop date.
     * <p>
     * Note that the stop date is automatically adjusted if either
     * {@link #getMedianDriver() median date} or {@link #getDurationDriver() duration}
     * are {@link ParameterDriver#isSelected() selected} changed.
     * </p>
     * @return driver for stop date
     */
    public DateDriver getStopDriver() {
        return stop;
    }

    /** Get the driver for median date.
     * <p>
     * Note that the median date is automatically adjusted if either
     * {@link #getStartDriver()} start date or {@link #getStopDriver() stop date}
     * are {@link ParameterDriver#isSelected() selected} changed.
     * </p>
     * @return driver for median date
     */
    public DateDriver getMedianDriver() {
        return median;
    }

    /** Get the driver for duration.
     * <p>
     * Note that the duration is automatically adjusted if either
     * {@link #getStartDriver()} start date or {@link #getStopDriver() stop date}
     * are {@link ParameterDriver#isSelected() selected} changed.
     * </p>
     * @return driver for duration
     */
    public ParameterDriver getDurationDriver() {
        return duration;
    }

    /** Compute the value of the switching function.
     * <p>
     * The function is positive for dates within the interval defined
     * by applying the parameter drivers shifts to reference dates,
     * and negative for dates outside of this interval. Note that
     * if Δt_start - Δt_stop is less than ref_stop.durationFrom(ref_start),
     * then the interval degenerates to empty and the function never
     * reaches positive values.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public T g(final FieldSpacecraftState<T> s) {
        return FastMath.min(s.getDate().durationFrom(start.getDate()),
                            s.getDate().durationFrom(stop.getDate()).negate());
    }

    /** Base observer. */
    private abstract class BindingObserver implements ParameterObserver {

        /** {@inheritDoc} */
        @Override
        public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
            if (driver.isSelected()) {
                setDelta(driver.getValue(date) - previousValue, date);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void valueSpanMapChanged(final TimeSpanMap<Double> previousValue, final ParameterDriver driver) {
            if (driver.isSelected()) {
                for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    setDelta(span.getData() - previousValue.get(span.getStart()), span.getStart());
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void selectionChanged(final boolean previousSelection, final ParameterDriver driver) {
            if ((start.isSelected()  || stop.isSelected()) &&
                (median.isSelected() || duration.isSelected())) {
                throw new OrekitException(OrekitMessages.INCONSISTENT_SELECTION,
                                          start.getName(), stop.getName(),
                                          median.getName(), duration.getName());
            }
        }

        /** Change a value.
         * @param date date for which the value wants to be change
         * @param delta change of value
         */
        protected abstract void setDelta(double delta, AbsoluteDate date);

    }

    /** Observer for start date. */
    private class StartObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta, final AbsoluteDate date) {
            median.setValue(median.getValue(date) + 0.5 * delta, date);
            duration.setValue(duration.getValue(date) - delta, date);
        }
    }

    /** Observer for stop date. */
    private class StopObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta, final AbsoluteDate date) {
            median.setValue(median.getValue(date) + 0.5 * delta, date);
            duration.setValue(duration.getValue(date) + delta, date);
        }
    }

    /** Observer for median date. */
    private class MedianObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta, final AbsoluteDate date) {
            start.setValue(start.getValue(date) + delta, date);
            stop.setValue(stop.getValue(date) + delta, date);
        }
    }

    /** Observer for duration. */
    private class DurationObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta, final AbsoluteDate date) {
            start.setValue(start.getValue(date) - 0.5 * delta, date);
            stop.setValue(stop.getValue(date) + 0.5 * delta, date);
        }
    }

}
