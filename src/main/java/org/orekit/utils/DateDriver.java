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
package org.orekit.utils;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;


/** {@link ParameterDriver Parameter driver} allowing to drive a date.
 * @author Luc Maisonobe
 * @since 11.1
 */
public class DateDriver extends ParameterDriver implements TimeStamped {

    /** Base date corresponding to shift = 0. */
    private final AbsoluteDate base;

    /** Indicator for start date. */
    private boolean start;

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected,
     * the reference date is set to {@code null}, the value (i.e. the date offset)
     * is set to 0, the scale is set to 1 and the minimum and maximum values are
     * set to negative and positive infinity respectively.
     * </p>
     * @param base base date corresponding to shift = 0
     * @param name name of the parameter
     * @param start if true, the driver corresponds to a start date
     */
    public DateDriver(final AbsoluteDate base, final String name, final boolean start) {
        super(name, 0.0, 1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.base  = base;
        this.start = start;
    }

    /** Get the base (unshifted) date.
     * @return base (unshifted) date
     */
    public AbsoluteDate getBaseDate() {
        return base;
    }

    /** Check if driver corresponds to a start date.
     * @return true if driver corresponds to a start date
     */
    public boolean isStart() {
        return start;
    }

    /** Get the shifted date.
     * @return shifted date
     */
    public AbsoluteDate getDate() {
        // date driver has no validity period, only 1 value is estimated
        // over the all interval so there is no problem for calling getValue with null argument
        // or any date, it would give the same result as there is only 1 span on the valueSpanMap
        // of the driver
        return base.shiftedBy(getValue(base));
    }

}
