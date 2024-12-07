/* Copyright 2002-2024 Luc Maisonobe
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

import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a GPS navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GPSCivilianNavigationMessage extends CivilianNavigationMessage {

    /** Constructor.
     * @param cnv2 indicator for CNV2 messages
     * @param timeScales known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav weeks
     *                        are always according to GPS)
     */
    public GPSCivilianNavigationMessage(final boolean cnv2,
                                        final TimeScales timeScales, final SatelliteSystem system) {
        super(cnv2, GNSSConstants.GPS_MU, GNSSConstants.GPS_AV, GNSSConstants.GPS_WEEK_NB,
              timeScales, system);
    }

    /**  {@inheritDoc} */
    @Override
    protected GPSCivilianNavigationMessage uninitializedCopy() {
        return new GPSCivilianNavigationMessage(isCnv2(), getTimeScales(), getSystem());
    }

}
