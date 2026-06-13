/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link BeidouLegacyNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouLegacyFactory extends GNSSOrbitalElementsFactory<BeidouLegacyNavigationMessage> {

    /** Indicator for D2 messages. */
    private final boolean d2;

    /** Simple constructor.
     * @param d2         indicator for D2 messages
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     * @param inertial   inertial frame
     * @param bodyFixed  body fixed frame, corresponding to the navigation message
     * @param date       date of the orbital parameters
     * @param mu         central attraction coefficient (m³/s²)
     */
    public BeidouLegacyFactory(final boolean d2,
                               final TimeScales timeScales, final SatelliteSystem system,
                               final Frame inertial, final Frame bodyFixed,
                               final AbsoluteDate date, final double mu) {
        super(new BeidouLegacyNavigationMessage(d2, timeScales, system,
                                                d2 ?
                                                BeidouLegacyNavigationMessage.D2 :
                                                BeidouLegacyNavigationMessage.D1),
              inertial, bodyFixed, date, mu);
        this.d2 = d2;
    }

    /** {@inheritDoc} */
    @Override
    protected BeidouLegacyNavigationMessage createEmptyMessage(final TimeScales timeScales,
                                                               final SatelliteSystem system,
                                                               final String type) {
        return new BeidouLegacyNavigationMessage(d2, timeScales, system, type);
    }

}
