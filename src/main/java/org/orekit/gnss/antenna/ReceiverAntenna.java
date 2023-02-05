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
package org.orekit.gnss.antenna;

import java.util.Map;

import org.orekit.gnss.Frequency;

/**
 * GNSS receiver antenna model.
 *
 * @author Luc Maisonobe
 * @since 9.2
 * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
 *
 */
public class ReceiverAntenna extends Antenna {

    /** Serial number. */
    private final String serialNumber;

    /** Simple constructor.
     * @param type antenna type
     * @param sinexCode sinex code
     * @param patterns frequencies patterns
     * @param serialNumber serial number
     */
    public ReceiverAntenna(final String type, final String sinexCode,
                           final Map<Frequency, FrequencyPattern> patterns,
                           final String serialNumber) {
        super(type, sinexCode, patterns);
        this.serialNumber = serialNumber;
    }

    /** Get the serial number.
     * @return serial number
     */
    public String getSerialNumber() {
        return serialNumber;
    }

}
