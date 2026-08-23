/* Copyright 2002-2026 CS GROUP
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

/** Sequence of {@link ParameterDriver parameter drivers} along a timeline.
 * @see ParameterDriversSequenceBuilder
 * @author Luc Maisonobe
 * @since 14.0
 */
public class ParameterDriversSequence implements ParameterDriversProvider {

    /** Drivers map. */
    private final TimeSpanMap<ParameterDriver> timeSpanDrivers;

    /** Drivers list. */
    private final List<ParameterDriver> list;

    /** Index of the first non-null driver. */
    private final int firstNonNull;

    /** Simple constructor.
     * <p>
     * The content of the provided map will be <em>copied</em> into the instance.
     * Further modifications of the argument (adding or removing entries, resetting dates)
     * will therefore have no effect on the instance.
     * </p>
     * @param timeSpanDrivers drivers for drag coefficients valid on specified time spans
     */
    ParameterDriversSequence(final TimeSpanMap<ParameterDriver> timeSpanDrivers) {

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
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(list);
    }

    /** Get the driver that is active at date.
     * @param date date to check
     * @return driver active at this date
     */
    public ParameterDriver getActiveDriver(final AbsoluteDate date) {
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
