/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.gnss.Frequency;

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

    /** Frequencies patterns. */
    private final Map<Frequency, FrequencyPattern> patterns;

    /** Simple constructor.
     * @param type antenna type
     * @param sinexCode sinex code
     * @param patterns frequencies patterns
     */
    protected Antenna(final String type, final String sinexCode,
                      final Map<Frequency, FrequencyPattern> patterns) {
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

    /** Get the phase center eccentricities.
     * @param frequency frequency of the signal to consider
     * @return phase center eccentricities (m)
     */
    public Vector3D getEccentricities(final Frequency frequency) {
        return patterns.get(frequency).getEccentricities();
    }

    /** Get the value of the phase center variation in a signal direction.
     * @param frequency frequency of the signal to consider
     * @param direction signal direction in antenna reference frame
     * @return value of the phase center variation (m)
     */
    public double getPhaseCenterVariation(final Frequency frequency, final Vector3D direction) {
        return patterns.get(frequency).getPhaseCenterVariation(direction);
    }

}
