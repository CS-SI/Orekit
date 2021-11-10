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
package org.orekit.utils;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;


/** {@link ParameterDriver Parameter driver} allowing to drive a start or stop date.
 * @author Luc Maisonobe
 * @since 11.1
 */
public class StartStopDriver extends ParameterDriver implements TimeStamped {

    /** Start/stop indicator. */
    private final boolean isStart;

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected,
     * the reference date is set to {@code null} and the value is set to the
     * {@code referenceValue}.
     * </p>
     * @param reference reference date
     * @param isStart if true, the date is a start date
     * @param name name of the parameter
     * @param referenceValue reference value of the parameter
     * @param scale scaling factor to convert the parameters value to
     * non-dimensional (typically set to the expected standard deviation of the
     * parameter), it must be non-zero
     * @param minValue minimum value
     * @param maxValue maximum value
     */
    public StartStopDriver(final AbsoluteDate reference, final boolean isStart,
                           final String name, final double referenceValue,
                           final double scale, final double minValue,
                           final double maxValue) {
        super(name, referenceValue, scale, minValue, maxValue);
        setReferenceDate(reference);
        this.isStart   = isStart;
    }

    /** Check if driven date is a start date.
     * @return true if driven date is a start date
     */
    public boolean isStart() {
        return isStart;
    }

    /** Get the shifted date.
     * @return shifted date
     */
    public AbsoluteDate getDate() {
        return getReferenceDate().shiftedBy(getValue());
    }

}
