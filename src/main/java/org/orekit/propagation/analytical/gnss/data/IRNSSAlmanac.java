/* Copyright 2002-2024 CS GROUP
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
 * Class for IRNSS almanac.
 *
 * @see "Indian Regiona Navigation Satellite System, Signal In Space ICD
 *       for standard positioning service, version 1.1 - Table 28"
 *
 * @author Bryan Cazabonne
 * @since 10.1
 *
 */
public class IRNSSAlmanac extends AbstractAlmanac {

    /**
     * Constructor.
     * @param timeScales known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav weeks
     *                        are always according to GPS)
     */
    public IRNSSAlmanac(final TimeScales timeScales, final SatelliteSystem system) {
        super(GNSSConstants.IRNSS_MU, GNSSConstants.IRNSS_AV, GNSSConstants.IRNSS_WEEK_NB, timeScales, system);
    }

    /**  {@inheritDoc} */
    @Override
    protected IRNSSAlmanac uninitializedCopy() {
        return new IRNSSAlmanac(getTimeScales(), getSystem());
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final double sqrtA) {
        setSma(sqrtA * sqrtA);
    }

}
