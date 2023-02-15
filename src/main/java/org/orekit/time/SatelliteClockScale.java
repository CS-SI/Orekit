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

import org.hipparchus.CalculusFieldElement;
import org.orekit.utils.Constants;

/** Scale for on-board clock.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class SatelliteClockScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20210309L;

    /** Name of the scale. */
    private final String name;

    /** Reference epoch. */
    private final AbsoluteDate epoch;

    /** Reference epoch. */
    private final DateTimeComponents epochDT;

    /** Offset from TAI at epoch. */
    private final double offsetAtEpoch;

    /** Clock count at epoch. */
    private final double countAtEpoch;

    /** Clock drift (i.e. clock count per SI second minus 1.0). */
    private final double drift;

    /** Clock rate. */
    private final double rate;

    /** Create a linear model for satellite clock.
     * <p>
     * Beware that we specify the model using its drift with respect to
     * flow of time. For a perfect clock without any drift, the clock
     * count would be one tick every SI second. A clock that is fast, say
     * for example it generates 1000001 ticks every 1000000 SI second, would
     * have a rate of 1.000001 tick per SI second and hence a drift of
     * 1.0e-6 tick per SI second. In this constructor we use the drift
     * (1.0e-6 in the previous example) rather than the rate (1.000001
     * in the previous example) to specify the clock. The rationale is
     * that for clocks that are intended to be used for representing absolute
     * time, the drift is expected to be small (much smaller that 1.0e-6
     * for a good clock), so using drift is numerically more stable than
     * using rate and risking catastrophic cancellation when subtracting
     * 1.0 in the internal computation.
     * </p>
     * <p>
     * Despite what is explained in the previous paragraph, this class can
     * handle spacecraft clocks that are not intended to be synchronized with
     * SI seconds, for example clocks that ticks at 10 Hz. In such cases the
     * drift would need to be set at 10.0 - 1.0 = 9.0, which is not intuitive.
     * For these clocks, the methods {@link #countAtDate(AbsoluteDate)} and
     * {@link #dateAtCount(double)} and perhaps {@link #offsetFromTAI(AbsoluteDate)}
     * are still useful, whereas {@link #offsetToTAI(DateComponents, TimeComponents)}
     * is probably not really meaningful.
     * </p>
     * @param name of the scale
     * @param epoch reference epoch
     * @param epochScale time scale in which the {@code epoch} was defined
     * @param countAtEpoch clock count at {@code epoch}
     * @param drift clock drift rate (i.e. clock count change per SI second minus 1.0)
     */
    public SatelliteClockScale(final String name,
                               final AbsoluteDate epoch, final TimeScale epochScale,
                               final double countAtEpoch, final double drift) {
        this.name          = name;
        this.epoch         = epoch;
        this.epochDT       = epoch.getComponents(epochScale);
        this.offsetAtEpoch = epochScale.offsetFromTAI(epoch) + countAtEpoch;
        this.countAtEpoch  = countAtEpoch;
        this.drift         = drift;
        this.rate          = 1.0 + drift;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return offsetAtEpoch + drift * date.durationFrom(epoch);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final double delta          = Constants.JULIAN_DAY * (date.getJ2000Day() - epochDT.getDate().getJ2000Day()) +
                                      time.getSecondsInUTCDay() - epochDT.getTime().getSecondsInUTCDay();
        final double timeSinceEpoch = (delta - countAtEpoch) / rate;
        return -(offsetAtEpoch + drift * timeSinceEpoch);
    }

    /** Compute date corresponding to some clock count.
     * @param count clock count
     * @return date at {@code count}
     */
    public AbsoluteDate dateAtCount(final double count) {
        return epoch.shiftedBy((count - countAtEpoch) / rate);
    }

    /** Compute clock count corresponding to some date.
     * @param date date
     * @return clock count at {@code date}
     */
    public double countAtDate(final AbsoluteDate date) {
        return countAtEpoch + rate * date.durationFrom(epoch);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        return date.durationFrom(epoch).multiply(drift).add(offsetAtEpoch);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
