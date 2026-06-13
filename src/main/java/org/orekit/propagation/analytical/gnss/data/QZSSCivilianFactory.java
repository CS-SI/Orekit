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
 * Factory for {@link QZSSCivilianNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class QZSSCivilianFactory extends GNSSOrbitalElementsFactory<QZSSCivilianNavigationMessage> {

    /** Indicator for CNV2 messages. */
    private final boolean cnv2;

    /** Simple constructor.
     * @param cnv2       indicator for CNV2 messages
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     * @param inertial   inertial frame
     * @param bodyFixed  body fixed frame, corresponding to the navigation message
     * @param date       date of the orbital parameters
     * @param mu         central attraction coefficient (m³/s²)
     */
    public QZSSCivilianFactory(final boolean cnv2,
                               final TimeScales timeScales, final SatelliteSystem system,
                               final Frame inertial, final Frame bodyFixed,
                               final AbsoluteDate date, final double mu) {
        super(new QZSSCivilianNavigationMessage(cnv2, timeScales, system,
                                                cnv2 ?
                                                QZSSCivilianNavigationMessage.CNV2 :
                                                QZSSCivilianNavigationMessage.CNAV),
              inertial, bodyFixed, date, mu);
        this.cnv2 = cnv2;
    }

    /** {@inheritDoc} */
    @Override
    protected QZSSCivilianNavigationMessage createEmptyMessage(final TimeScales timeScales,
                                                               final SatelliteSystem system,
                                                               final String type) {
        return new QZSSCivilianNavigationMessage(cnv2, timeScales, system, type);
    }

}
