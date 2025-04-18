/* Copyright 2002-2025 CS GROUP
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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.EOPHistory;

/** Universal Time 1.
 * <p>UT1 is a time scale directly linked to the actual rotation of the Earth.
 * It is an irregular scale, reflecting Earth irregular rotation rate. The offset
 * between UT1 and {@link UTCScale UTC} is found in the Earth Orientation
 * Parameters published by IERS.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @since 5.1
 */
public class UT1Scale implements TimeScale {

    /** UTC scale. */
    private final UTCScale utc;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Simple constructor.
     * @param eopHistory user supplied EOP history (may be null)
     * @param utc UTC time scale
     */
    protected UT1Scale(final EOPHistory eopHistory, final UTCScale utc) {
        this.eopHistory = eopHistory;
        this.utc        = utc;
    }

    /** Get the associated UTC scale.
     * @return associated UTC scale.
     * @since 9.1
     */
    public UTCScale getUTCScale() {
        return utc;
    }

    /** Get the EOP history.
     * @return eop history (may be null)
     */
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public TimeOffset offsetFromTAI(final AbsoluteDate date) {
        final double    dtu1        = eopHistory == null ? 0 : eopHistory.getUT1MinusUTC(date);
        final TimeOffset utcMinusTai = utc.offsetFromTAI(date);
        return utcMinusTai.add(new TimeOffset(dtu1));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        final T dtu1        = eopHistory == null ? date.getField().getZero() : eopHistory.getUT1MinusUTC(date);
        final T utcMinusTai = utc.offsetFromTAI(date);
        return utcMinusTai.add(dtu1);
    }

    /** {@inheritDoc} */
    public String getName() {
        return "UT1";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
