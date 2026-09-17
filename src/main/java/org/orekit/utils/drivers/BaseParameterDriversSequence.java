/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Sequence of {@link FieldParameterDriver parameter drivers} along a timeline.
 * @param <P> type of the parameter driver
 * @param <O> type of the parameter observer
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BaseParameterDriversSequence<P extends BaseParameterDriver<P, O>,
                                          O extends BaseParameterObserver<P, O>>
    implements BaseParameterDriversProvider<P, O> {

    /** Drivers map. */
    private final TimeSpanMap<P> timeSpanDrivers;

    /** Drivers list. */
    private final List<P> list;

    /** Index of the first non-null driver. */
    private final int firstNonNull;

    /** Simple constructor.
     * <p>
     * The content of the provided map will be <em>copied</em> into the instance.
     * Further modifications of the argument (adding or removing entries, resetting dates)
     * will therefore have no effect on the instance.
     * </p>
     * @param timeSpanDrivers drivers valid on specified time spans
     */
    protected BaseParameterDriversSequence(final TimeSpanMap<P> timeSpanDrivers) {

        // copy the map into an independent one to protect against modifications
        this.timeSpanDrivers =
            timeSpanDrivers.extractRange(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);

        // convert to a list, ignoring null drivers at start or end
        list = new ArrayList<>(timeSpanDrivers.getSpansNumber());
        timeSpanDrivers.forEach(list::add);
        firstNonNull = this.timeSpanDrivers.getFirstNonNullSpan().getIndex();

    }

    /** {@inheritDoc} */
    @Override
    public List<P> getParametersDrivers() {
        return Collections.unmodifiableList(list);
    }

    /** Get the driver that is active at date.
     * @param date date to check
     * @return driver active at this date
     */
    public P getActiveDriver(final AbsoluteDate date) {
        return timeSpanDrivers.get(date);
    }

    /** Get the index of driver that is active at date in the {@link #getParametersDrivers()} list.
     * @param date date to check
     * @return driver active at this date
     */
    public int getActiveDriverIndex(final AbsoluteDate date) {
        final int index = timeSpanDrivers.getSpan(date).getIndex() - firstNonNull;
        if (index < 0 || index >= list.size()) {
            // the date is outside the covered range
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DATE,
                                      date,
                                      timeSpanDrivers.getFirstNonNullSpan().getStart(),
                                      timeSpanDrivers.getLastNonNullSpan().getEnd());
        }
        return index;
    }

}
