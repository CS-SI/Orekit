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
package org.orekit.time;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;

/** Selector generating a continuous stream of dates separated by a constant step.
 * <p>
 * The dates can be aligned to whole steps in some time scale. So for example
 * if a step of 60s is used and the alignment time scale is set to
 * {@link org.orekit.time.TimeScales#getUTC() UTC}, dates will be selected
 * at whole minutes in UTC time.
 * </p>
 * <p>
 * BEWARE! This class stores internally the last selected dates, so it is <em>neither</em>
 * reusable across several {@link org.orekit.estimation.measurements.generation.EventBasedScheduler
 * fixed step} or {@link org.orekit.estimation.measurements.generation.ContinuousScheduler
 * continuous} schedulers, <em>nor</em> thread-safe. A separate selector should be used for each
 * scheduler and for each thread in multi-threading context.
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class FixedStepSelector implements DatesSelector {

    /** Step between two consecutive dates. */
    private final double step;

    /** Alignment time scale (null is alignment is not needed). */
    private final TimeScale alignmentTimeScale;

    /** Last selected date. */
    private AbsoluteDate last;

    /** Simple constructor.
     * @param step step between two consecutive dates (s)
     * @param alignmentTimeScale alignment time scale (null is alignment is not needed)
     */
    public FixedStepSelector(final double step, final TimeScale alignmentTimeScale) {
        this.step               = step;
        this.alignmentTimeScale = alignmentTimeScale;
        this.last               = null;
    }

    /** {@inheritDoc} */
    @Override
    public List<AbsoluteDate> selectDates(final AbsoluteDate start, final AbsoluteDate end) {

        final double sign = FastMath.copySign(1, end.durationFrom(start));
        final List<AbsoluteDate> selected = new ArrayList<>();

        final boolean reset = last == null || sign * start.durationFrom(last) > step;
        for (AbsoluteDate next = reset ? start : last.shiftedBy(sign * step);
             sign * next.durationFrom(end) <= 0;
             next = last.shiftedBy(sign * step)) {

            if (alignmentTimeScale != null) {
                // align date to time scale
                final double t  = next.getComponents(alignmentTimeScale).getTime().getSecondsInLocalDay();
                final double dt = step * FastMath.round(t / step) - t;
                next = next.shiftedBy(dt);
            }

            if (sign * next.durationFrom(start) >= 0) {
                if (sign * next.durationFrom(end) <= 0) {
                    // the date is within range, select it
                    selected.add(next);
                } else {
                    // we have exceeded date range
                    break;
                }
            }
            last = next;

        }

        return selected;

    }

}
