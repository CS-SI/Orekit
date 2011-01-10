/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.frames.EOPHistory;

/** Universal Time 1.
 * <p>UT1 is a time scale directly linked to the actual rotation of the Earth.
 * It is an irregular scale, reflecting Earth irregular rotation rate. The offset
 * between UT1 and {@link UTCScale UTC} is found in the Earth Orientation
 * Parameters published by IERS.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision$ $Date$
 * @since 5.1
 */
public class UT1Scale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 6263529270396325640L;

    /** UTC scale. */
    private final UTCScale utc;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Package private constructor for the factory.
     * @param eopHistory user supplied EOP history
     * @param utc UTC time scale
     */
    UT1Scale(final EOPHistory eopHistory, final UTCScale utc) {
        this.eopHistory = eopHistory;
        this.utc        = utc;
    }

    /** {@inheritDoc} */
    public synchronized double offsetFromTAI(final AbsoluteDate date) {
        final double dtu1        = eopHistory.getUT1MinusUTC(date);
        final double utcMinusTai = utc.offsetFromTAI(date);
        return utcMinusTai + dtu1;
    }

    /** {@inheritDoc} */
    public synchronized double offsetToTAI(final DateComponents date,
                                           final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < 3; i++) {
            offset = -offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
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
