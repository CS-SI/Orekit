/* Copyright 2022-2025 Luc Maisonobe
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a GPS navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GPSCivilianNavigationMessage extends CivilianNavigationMessage<GPSCivilianNavigationMessage> {

    /** Message type.
     * @since 14.0
     */
    public static final String CNAV = "CNAV";

    /** Message type.
     * @since 14.0
     */
    public static final String CNV2 = "CNV2";

    /** Constructor.
     * @param cnv2       indicator for CNV2 messages
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     * @param type       message type
     */
    public GPSCivilianNavigationMessage(final boolean cnv2,
                                        final TimeScales timeScales, final SatelliteSystem system,
                                        final String type) {
        super(cnv2, GNSSConstants.GPS_MU, GNSSConstants.GPS_AV, GNSSConstants.GPS_WEEK_NB,
              timeScales, system, type);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> GPSCivilianNavigationMessage(final FieldGPSCivilianNavigationMessage<T> original) {
        super(original);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, GPSCivilianNavigationMessage>>
        F toField(final Field<T> field) {
        return (F) new FieldGPSCivilianNavigationMessage<>(field, this);
    }

}
