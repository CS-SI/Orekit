/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DateDriver;

/** Detector for date intervals that may be offset thanks to parameter drivers.
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 11.1
 */
public class FieldParameterDrivenDateIntervalDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldParameterDrivenDateIntervalDetector<T>, T> {

    /** Default suffix for start driver. */
    public static final String START_SUFFIX = "_START";

    /** Default suffix for stop driver. */
    public static final String STOP_SUFFIX = "_STOP";

    /** Reference interval start driver. */
    private DateDriver start;

    /** Reference interval stop driver. */
    private DateDriver stop;

    /** Build a new instance.
     * @param field field to which the elements belong
     * @param prefix prefix to use for parameter drivers names
     * @param refStart reference interval start date
     * @param refStop reference interval stop date
     */
    public FieldParameterDrivenDateIntervalDetector(final Field<T> field, final String prefix,
                                                    final AbsoluteDate refStart, final AbsoluteDate refStop) {
        this(field.getZero().newInstance(DEFAULT_MAXCHECK),
             field.getZero().newInstance(DEFAULT_THRESHOLD),
             DEFAULT_MAX_ITER,
             new FieldStopOnEvent<FieldParameterDrivenDateIntervalDetector<T>, T>(),
             new DateDriver(refStart, prefix + START_SUFFIX, true),
             new DateDriver(refStop, prefix + STOP_SUFFIX, false));
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
     */
    private FieldParameterDrivenDateIntervalDetector(final T maxCheck, final T threshold, final int maxIter,
                                                     final FieldEventHandler<? super FieldParameterDrivenDateIntervalDetector<T>, T> handler,
                                                     final DateDriver start, final DateDriver stop) {
        super(maxCheck, threshold, maxIter, handler);
        this.start = start;
        this.stop  = stop;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldParameterDrivenDateIntervalDetector<T> create(final T newMaxCheck, final T newThreshold, final int newMaxIter,
                                                                 final FieldEventHandler<? super FieldParameterDrivenDateIntervalDetector<T>, T> newHandler) {
        return new FieldParameterDrivenDateIntervalDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                                              start, stop);
    }

    /** Get the driver for start date.
     * @return driver for start date
     */
    public DateDriver getStartDriver() {
        return start;
    }

    /** Get the driver for stop date.
     * @return driver for stop date
     */
    public DateDriver getStopDriver() {
        return stop;
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

}
