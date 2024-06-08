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
package org.orekit.gnss.antenna;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.RadioWave;

/**
 * GNSS antenna model.
 *
 * @author Luc Maisonobe
 * @since 9.2
 * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
 *
 */
public class Antenna {

    /** Type of the antenna. */
    private final String type;

    /** Sinex code. */
    private final String sinexCode;

    /** Radio waves patterns. */
    private final Map<RadioWave, FrequencyPattern> patterns;

    /** Simple constructor.
     * @param type antenna type
     * @param sinexCode sinex code
     * @param patterns frequencies patterns
     */
    protected Antenna(final String type, final String sinexCode,
                      final Map<RadioWave, FrequencyPattern> patterns) {
        this.type      = type;
        this.sinexCode = sinexCode;
        this.patterns  = patterns;
    }

    /** Get the type of the antenna.
     * @return type of the antenna
     */
    public String getType() {
        return type;
    }

    /** Get the sinex code of the antenna.
     * @return sinex code of the antenna
     */
    public String getSinexCode() {
        return sinexCode;
    }

    /** Get supported radio waves.
     * @return supported radio waves
     * @since 13.0
     */
    public List<RadioWave> getRadioWaves() {
        return patterns.
               entrySet().
               stream().
               map(Map.Entry::getKey).
               collect(Collectors.toList());
    }

    /**
     * Get the phase center eccentricities.
     *
     * @param radioWave radio wave of the signal to consider
     * @return phase center eccentricities (m)
     */
    public Vector3D getEccentricities(final RadioWave radioWave) {
        return getPattern(radioWave).getEccentricities();
    }

    /**
     * Get the value of the phase center variation in a signal direction.
     *
     * @param radioWave radio wave of the signal to consider
     * @param direction signal direction in antenna reference frame
     * @return value of the phase center variation (m)
     */
    public double getPhaseCenterVariation(final RadioWave radioWave, final Vector3D direction) {
        return getPattern(radioWave).getPhaseCenterVariation(direction);
    }

    /**
     * Get a frequency pattern.
     *
     * @param radioWave radio wave of the signal to consider
     * @return pattern for this frequency
     */
    public FrequencyPattern getPattern(final RadioWave radioWave) {
        final FrequencyPattern pattern = patterns.get(radioWave);
        if (pattern == null) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_FREQUENCY_FOR_ANTENNA, radioWave, type);
        }
        return pattern;
    }

}
