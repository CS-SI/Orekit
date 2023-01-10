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


/** Selector generating high rate bursts of dates separated by some rest period.
 * <p>
 * The dates can be aligned to whole steps in some time scale. So for example
 * if a rest period of 3600s is used and the alignment time scale is set to
 * {@link org.orekit.time.TimeScales#getUTC() UTC}, the earliest date of
 * each burst will occur at whole hours in UTC time.
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
public class BurstSelector implements DatesSelector {

    /** Maximum number of selected dates in a burst. */
    private final int maxBurstSize;

    /** Step between two consecutive dates within a burst. */
    private final double highRateStep;

    /** Period between the start of each burst. */
    private final double burstPeriod;

    /** Alignment time scale (null is alignment is not needed). */
    private final TimeScale alignmentTimeScale;

    /** First date in last burst. */
    private AbsoluteDate first;

    /** Last selected date. */
    private AbsoluteDate last;

    /** Index of last selected date in current burst. */
    private int index;

    /** Simple constructor.
     * <p>
     * The {@code burstPeriod} ignores the duration of the burst itself. This
     * means that if burst of {@code maxBurstSize=256} dates each separated by
     * {@code highRateStep=100ms} should be selected with {@code burstPeriod=300s},
     * then the first burst would contain 256 dates from {@code t0} to {@code t0+25.5s}
     * and the second burst would start at {@code t0+300s}, <em>not</em> at
     * {@code t0+325.5s}.
     * </p>
     * <p>
     * If alignment to some time scale is needed, it applies only to the first date in
     * each burst.
     * </p>
     * @param maxBurstSize maximum number of selected dates in a burst
     * @param highRateStep step between two consecutive dates within a burst (s)
     * @param burstPeriod period between the start of each burst (s)
     * @param alignmentTimeScale alignment time scale for first date in burst
     * (null is alignment is not needed)
     */
    public BurstSelector(final int maxBurstSize, final double highRateStep,
                         final double burstPeriod, final TimeScale alignmentTimeScale) {
        this.maxBurstSize       = maxBurstSize;
        this.highRateStep       = highRateStep;
        this.burstPeriod        = burstPeriod;
        this.alignmentTimeScale = alignmentTimeScale;
        this.last               = null;
        this.first              = null;
        this.index              = 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<AbsoluteDate> selectDates(final AbsoluteDate start, final AbsoluteDate end) {

        final int    increment          = end.durationFrom(start) > 0 ? +1 : -1;
        final int    firstIndex         = increment > 0 ? 0 : maxBurstSize - 1;
        final int    lastIndex          = maxBurstSize - 1 - firstIndex;
        final double signedHighRateStep = FastMath.copySign(highRateStep, increment);
        final double signedBurstPeriod  = FastMath.copySign(burstPeriod, increment);

        final List<AbsoluteDate> selected = new ArrayList<>();

        final boolean reset = first == null || increment * start.durationFrom(first) > burstPeriod;
        if (reset) {
            first = null;
            index = firstIndex;
        }

        for (AbsoluteDate next = reset ? start : last.shiftedBy(signedHighRateStep);
             increment * next.durationFrom(end) <= 0;
             next = last.shiftedBy(signedHighRateStep)) {

            if (index == lastIndex + increment) {
                // we have exceeded burst size, jump to next burst
                next  = first.shiftedBy(signedBurstPeriod);
                first = null;
                index = firstIndex;
                if (increment * next.durationFrom(end) > 0) {
                    // next burst is out of current interval
                    break;
                }
            }

            if (first == null && alignmentTimeScale != null) {
                // align earliest burst date to time scale
                final double offset = firstIndex * highRateStep;
                final double t      = next.getComponents(alignmentTimeScale).getTime().getSecondsInLocalDay() - offset;
                final double dt     = burstPeriod * FastMath.round(t / burstPeriod) - t;
                next = next.shiftedBy(dt);
                while (index != lastIndex && increment * next.durationFrom(start) < 0) {
                    next = next.shiftedBy(signedHighRateStep);
                    index += increment;
                }
                if (increment * next.durationFrom(start) < 0) {
                    // alignment shifted date out of interval
                    next  = next.shiftedBy(signedBurstPeriod - (maxBurstSize - 1) * signedHighRateStep);
                    index = firstIndex;
                }
            }

            if (increment * next.durationFrom(start) >= 0) {
                if (increment * next.durationFrom(end) <= 0) {
                    // the date is within range, select it
                    if (first == null) {
                        first = next.shiftedBy(-signedHighRateStep * index);
                    }
                    selected.add(next);
                } else {
                    // we have exceeded date range
                    break;
                }
            }
            last   = next;
            index += increment;

        }

        return selected;

    }

}
