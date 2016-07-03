/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.time;

import java.io.Serializable;

/** Interface for time scales.
 * <p>This is the interface representing all time scales. Time scales are related
 * to each other by some offsets that may be discontinuous (for example
 * the {@link UTCScale UTC scale} with respect to the {@link TAIScale
 * TAI scale}).</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public interface TimeScale extends Serializable {

    /** Get the offset to convert locations from {@link TAIScale} to instance.
     * @param date conversion date
     * @return offset in seconds to add to a location in <em>{@link TAIScale}
     * time scale</em> to get a location in <em>instance time scale</em>
     * @see #offsetToTAI(DateComponents, TimeComponents)
     */
    double offsetFromTAI(AbsoluteDate date);

    /** Get the offset to convert locations from instance to {@link TAIScale}.
     * @param date date location in the time scale
     * @param time time location in the time scale
     * @return offset in seconds to add to a location in <em>instance time scale</em>
     * to get a location in <em>{@link TAIScale} time scale</em>
     * @see #offsetFromTAI(AbsoluteDate)
     */
    default double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < 8; i++) {
            offset = -offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
    }

    /** Check if date is within a leap second introduction <em>in this time scale</em>.
     * <p>
     * This method will return false for all time scales that do <em>not</em>
     * implement leap seconds, even if the date corresponds to a leap second
     * in {@link UTCScale UTC scale}.
     * </p>
     * @param date date to check
     * @return true if time is within a leap second introduction
     */
    default boolean insideLeap(final AbsoluteDate date) {
        return false;
    }

    /** Check length of the current minute <em>in this time scale</em>.
     * <p>
     * This method will return 60 for all time scales that do <em>not</em>
     * implement leap seconds, even if the date corresponds to a leap second
     * in {@link UTCScale UTC scale}, and 61 for time scales that do implement
     * leap second when the current date is within the last minute before the
     * leap, or during the leap itself.
     * </p>
     * @param date date to check
     * @return 60 or 61 depending on leap seconds introduction
     */
    default int minuteDuration(final AbsoluteDate date) {
        return 60;
    }

    /** Get the value of the previous leap.
     * <p>
     * This method will return 0.0 for all time scales that do <em>not</em>
     * implement leap seconds.
     * </p>
     * @param date date to check
     * @return value of the previous leap
     */
    default double getLeap(final AbsoluteDate date) {
        return 0;
    }

    /** Get the name time scale.
     * @return name of the time scale
     */
    String getName();

}
