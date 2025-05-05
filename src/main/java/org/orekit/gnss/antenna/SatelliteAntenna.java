/* Copyright 2002-2025 CS GROUP
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
package org.orekit.gnss.antenna;

import java.util.Map;

import org.orekit.gnss.RadioWave;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;

/**
 * GNSS satellite antenna model.
 *
 * @author Luc Maisonobe
 * @since 9.2
 * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
 *
 */
public class SatelliteAntenna extends Antenna {

    /** Satellite in system.
     * @since 13.0
     */
    private final SatInSystem satInSystem;

    /** Satellite type.
     * @since 9.3
     */
    private final SatelliteType satelliteType;

    /** Satellite code. */
    private final int satelliteCode;

    /** COSPAR ID. */
    private final String cosparID;

    /** Start of validity. */
    private final AbsoluteDate validFrom;

    /** End of validity. */
    private final AbsoluteDate validUntil;

    /** Simple constructor.
     * @param type antenna type
     * @param sinexCode sinex code
     * @param patterns frequencies patterns
     * @param satInSystem satellite in system
     * @param satelliteType satellite type
     * @param satelliteCode satellite code
     * @param cosparID COSPAR ID
     * @param validFrom start of validity
     * @param validUntil end of validity
     */
    public SatelliteAntenna(final String type, final String sinexCode,
                            final Map<RadioWave, FrequencyPattern> patterns,
                            final SatInSystem satInSystem,
                            final SatelliteType satelliteType, final int satelliteCode,
                            final String cosparID,
                            final AbsoluteDate validFrom, final AbsoluteDate validUntil) {
        super(type, sinexCode, patterns);
        this.satInSystem   = satInSystem;
        this.satelliteType = satelliteType;
        this.satelliteCode = satelliteCode;
        this.cosparID      = cosparID;
        this.validFrom     = validFrom;
        this.validUntil    = validUntil;
    }

    /** Get satellite in system.
     * @return satellite in system
     * @since 13.0
     */
    public SatInSystem getSatInSystem() {
        return satInSystem;
    }

    /** Get satellite type.
     * @return satellite type
     * @since 9.3
     */
    public SatelliteType getSatelliteType() {
        return satelliteType;
    }

    /** Get satellite code.
     * @return satellite code
     */
    public int getSatelliteCode() {
        return satelliteCode;
    }

    /** Get COSPAR ID.
     * @return COSPAR ID
     */
    public String getCosparID() {
        return cosparID;
    }

    /** Get start of validity.
     * @return start of validity
     */
    public AbsoluteDate getValidFrom() {
        return validFrom;
    }

    /** Get end of validity.
     * @return end of validity
     */
    public AbsoluteDate getValidUntil() {
        return validUntil;
    }

}
