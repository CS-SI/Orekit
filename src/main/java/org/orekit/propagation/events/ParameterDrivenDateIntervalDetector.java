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
package org.orekit.propagation.events;

import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DateDriver;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

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
 * @author Luc Maisonobe
 * @since 11.1
 */
public class ParameterDrivenDateIntervalDetector extends AbstractDetector<ParameterDrivenDateIntervalDetector> {

    /** Default suffix for start driver. */
    public static final String START_SUFFIX = "_START";

    /** Default suffix for stop driver. */
    public static final String STOP_SUFFIX = "_STOP";

    /** Default suffix for median driver. */
    public static final String MEDIAN_SUFFIX = "_MEDIAN";

    /** Default suffix for duration driver. */
    public static final String DURATION_SUFFIX = "_DURATION";

    /** Detection threshold. */
    private static final double THRESHOLD = 1.0e-10;

    /** Reference interval start driver. */
    private DateDriver start;

    /** Reference interval stop driver. */
    private DateDriver stop;

    /** Median date driver. */
    private DateDriver median;

    /** Duration driver. */
    private ParameterDriver duration;

    /** Build a new instance.
     * @param prefix prefix to use for parameter drivers names
     * @param refMedian reference interval median date
     * @param refDuration reference duration
     */
    public ParameterDrivenDateIntervalDetector(final String prefix,
                                               final AbsoluteDate refMedian, final double refDuration) {
        this(prefix,
             refMedian.shiftedBy(-0.5 * refDuration),
             refMedian.shiftedBy(+0.5 * refDuration));
    }

    /** Build a new instance.
     * @param prefix prefix to use for parameter drivers names
     * @param refStart reference interval start date
     * @param refStop reference interval stop date
     */
    public ParameterDrivenDateIntervalDetector(final String prefix,
                                               final AbsoluteDate refStart, final AbsoluteDate refStop) {
        this(FastMath.max(0.5 * refStop.durationFrom(refStart), THRESHOLD),
             THRESHOLD, DEFAULT_MAX_ITER,
             new StopOnDecreasing<ParameterDrivenDateIntervalDetector>(),
             new DateDriver(refStart, prefix + START_SUFFIX, true),
             new DateDriver(refStop, prefix + STOP_SUFFIX, false),
             new DateDriver(refStart.shiftedBy(0.5 * refStop.durationFrom(refStart)), prefix + MEDIAN_SUFFIX, true),
             new ParameterDriver(prefix + DURATION_SUFFIX, refStop.durationFrom(refStart), 1.0, 0.0, Double.POSITIVE_INFINITY));
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param start reference interval start driver
     * @param stop reference interval stop driver
     * @param median median date driver
     * @param duration duration driver
     */
    private ParameterDrivenDateIntervalDetector(final double maxCheck, final double threshold, final int maxIter,
                                                final EventHandler<? super ParameterDrivenDateIntervalDetector> handler,
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
                                                 filter(observer -> observer instanceof ParameterDrivenDateIntervalDetector.BindingObserver).
                                                 collect(Collectors.toList());
        original.forEach(observer -> driver.removeObserver(observer));

        driver.addObserver(bindingObserver);

    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDrivenDateIntervalDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                                                         final EventHandler<? super ParameterDrivenDateIntervalDetector> newHandler) {
        return new ParameterDrivenDateIntervalDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
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
    public double g(final SpacecraftState s) {
        return FastMath.min(s.getDate().durationFrom(start.getDate()),
                            stop.getDate().durationFrom(s.getDate()));
    }

    /** Base observer. */
    private abstract class BindingObserver implements ParameterObserver {

        /** {@inheritDoc} */
        @Override
        public void valueChanged(final double previousValue, final ParameterDriver driver) {
            if (driver.isSelected()) {
                setDelta(driver.getValue() - previousValue);
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
         * @param delta change of value
         */
        protected abstract void setDelta(double delta);

    }

    /** Observer for start date. */
    private class StartObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta) {
            median.setValue(median.getValue() + 0.5 * delta);
            duration.setValue(duration.getValue() - delta);
        }
    }

    /** Observer for stop date. */
    private class StopObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta) {
            median.setValue(median.getValue() + 0.5 * delta);
            duration.setValue(duration.getValue() + delta);
        }
    }

    /** Observer for median date. */
    private class MedianObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta) {
            start.setValue(start.getValue() + delta);
            stop.setValue(stop.getValue() + delta);
        }
    }

    /** Observer for duration. */
    private class DurationObserver extends BindingObserver {
        /** {@inheritDoc} */
        @Override
        protected void setDelta(final double delta) {
            start.setValue(start.getValue() - 0.5 * delta);
            stop.setValue(stop.getValue() + 0.5 * delta);
        }
    }

}
