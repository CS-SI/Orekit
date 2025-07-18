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
package org.orekit.files.rinex.navigation;

import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Base container for data contained in a ionosphere message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class IonosphereBaseMessage extends TypeSvMessage {

    /** Converters for Klobuchar parameters. */
    public static final Unit[] S_PER_SC_N;

    static {
        final Unit sc = Unit.RADIAN.scale("sc", GNSSConstants.GNSS_PI);
        S_PER_SC_N = new Unit[4];
        S_PER_SC_N[0] = Unit.SECOND;
        S_PER_SC_N[1] = S_PER_SC_N[0].divide("s/sc",  sc);
        S_PER_SC_N[2] = S_PER_SC_N[1].divide("s/sc²", sc);
        S_PER_SC_N[3] = S_PER_SC_N[2].divide("s/sc³", sc);
    }

    /** Transmit time. */
    private AbsoluteDate transmitTime;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param type navigation message type
     * @param subType navigation message subtype
     */
    public IonosphereBaseMessage(final SatelliteSystem system, final int prn,
                                 final String type, final String subType) {
        super(system, prn, type, subType);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return transmitTime;
    }

    /** Get the transmit time.
     * @return the transmit time
     */
    public AbsoluteDate getTransmitTime() {
        return transmitTime;
    }

    /** Set the transmit time.
     * @param transmitTime the transmit time to set
     */
    public void setTransmitTime(final AbsoluteDate transmitTime) {
        this.transmitTime = transmitTime;
    }

}
