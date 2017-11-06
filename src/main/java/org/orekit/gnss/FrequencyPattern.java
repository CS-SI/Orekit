/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.gnss;

import org.hipparchus.analysis.BivariateFunction;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;

/**
 * Pattern for GNSS antenna model on one frequency.
 *
 * @author Luc Maisonobe
 * @since 9.1
 * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
 *
 */
public class FrequencyPattern {

    /** Phase center eccentricities (m). */
    private final Vector3D eccentricities;

    /** Azimuth independent phase model. */
    private final UnivariateFunction azimuthIndependentPhase;

    /** Azimuth dependent phase model. */
    private final BivariateFunction azimuthDependentPhase;

    /** Simple constructor.
     * @param eccentricities phase center eccentricities (m)
     * @param azimuthIndependentPhasehase phase model (function argument
     * is zenith angle for receiver antennas, nadir angle for GNSS satellite)
     * @param azimuthDependentPhasehase phase model (first function argument is azimuth, second function argument
     * is polar angle, i.e. zenith for receiver antennas, nadir angle for GNSS satellite),
     * may be null
     */
    protected FrequencyPattern(final Vector3D eccentricities,
                               final double polarAngleStart, final double polarAngleEnd,
                               final UnivariateFunction azimuthIndependentPhase,
                               final BivariateFunction azimuthDependentPhase) {
        this.eccentricities          = eccentricities;
        this.azimuthIndependentPhase = azimuthIndependentPhase;
        this.azimuthDependentPhase   = azimuthDependentPhase;
    }

    /** Get the phase center eccentricities.
     * @return phase center eccentricities (m)
     */
    public Vector3D getEccentricities() {
        return eccentricities;
    }

    /** Get the phase in a signal direction.
     * @param direction signal direction in antenna reference frame
     * @return phase
     */
    public double getPhase(final Vector3D direction) {
        if (azimuthDependentPhase == null) {
            return azimuthIndependentPhase.value(0.5 * FastMath.PI - direction.getDelta());
        } else {
            return azimuthDependentPhase.value(direction.getAlpha() + FastMath.PI,
                                               0.5 * FastMath.PI - direction.getDelta());
        }
    }

}
