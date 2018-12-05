/* Copyright 2002-2018 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;


/** Selector generating high rate bursts of dates separated by some rest period.
 * <p>
 * The dates can be aligned to whole steps in some time scale. So for example
 * if a rest period of 3600s is used and the alignment time scale is set to
 * {@link org.orekit.time.TimeScalesFactory#getUTC() UTC}, the first date of
 * each burst will occur at whole hours in UTC time.
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

    /** Number of selected dates in last burst. */
    private int lastSize;

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
        this.lastSize           = 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<AbsoluteDate> selectDates(final AbsoluteDate start, final AbsoluteDate end) {

        final List<AbsoluteDate> selected = new ArrayList<>();

        boolean reset = first == null || start.durationFrom(first) > burstPeriod;
        for (AbsoluteDate next = reset ? start : last.shiftedBy(highRateStep);
             next.compareTo(end) <= 0;
             next = last.shiftedBy(highRateStep)) {

            if (reset) {
                first    = null;
                lastSize = 0;
                reset    = false;
            }

            if (lastSize == maxBurstSize) {
                // we have exceeded burst size, jump to next burst
                next     = first.shiftedBy(burstPeriod);
                first    = null;
                lastSize = 0;
                if (next.compareTo(end) > 0) {
                    // next burst is out of current interval
                    break;
                }
            }

            if (first == null && alignmentTimeScale != null) {
                // align date to time scale
                final double t  = next.getComponents(alignmentTimeScale).getTime().getSecondsInLocalDay();
                final double dt = burstPeriod * FastMath.round(t / burstPeriod) - t;
                next = next.shiftedBy(dt);
                if (next.compareTo(start) < 0) {
                    // alignment shifted date before interval
                    next = next.shiftedBy(burstPeriod);
                }
            }

            if (next.compareTo(start) >= 0) {
                if (next.compareTo(end) <= 0) {
                    // the date is within range, select it
                    if (first == null) {
                        first    = next;
                        lastSize = 0;
                    }
                    selected.add(next);
                    ++lastSize;
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
