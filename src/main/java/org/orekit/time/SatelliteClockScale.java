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
package org.orekit.time;

import org.hipparchus.RealFieldElement;
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

    /** Create a linear model for satellite clock.
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
        final double timeSinceEpoch = (delta - countAtEpoch) / (1 + drift);
        return -(offsetAtEpoch + drift * timeSinceEpoch);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
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
