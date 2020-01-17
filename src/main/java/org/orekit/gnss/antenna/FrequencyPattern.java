/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;

/**
 * Pattern for GNSS antenna model on one frequency.
 *
 * @author Luc Maisonobe
 * @since 9.2
 * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
 *
 */
public class FrequencyPattern {

    /** Phase center eccentricities (m). */
    private final Vector3D eccentricities;

    /** Phase center variation function. */
    private final PhaseCenterVariationFunction phaseCenterVariationFunction;

    /** Simple constructor.
     * @param eccentricities phase center eccentricities (m)
     * @param phaseCenterVariationFunction phase center variation function
     */
    protected FrequencyPattern(final Vector3D eccentricities,
                               final PhaseCenterVariationFunction phaseCenterVariationFunction) {
        this.eccentricities               = eccentricities;
        this.phaseCenterVariationFunction = phaseCenterVariationFunction;
    }

    /** Get the phase center eccentricities.
     * @return phase center eccentricities (m)
     */
    public Vector3D getEccentricities() {
        return eccentricities;
    }

    /** Get the value of the phase center variation in a signal direction.
     * @param direction signal direction in antenna reference frame
     * @return value of the phase center variation
     */
    public double getPhaseCenterVariation(final Vector3D direction) {
        return phaseCenterVariationFunction.value(0.5 * FastMath.PI - direction.getDelta(),
                                                  direction.getAlpha());
    }

}
