/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.gnss;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/** Top level interface for radio waves.
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public interface RadioWave {

    /** Default 1MHz tolerance for {@link #closeTo(RadioWave)}.
     * @since 13.0
     */
    double ONE_MILLI_HERTZ = 1.0e-3;

    /** Get the value of the frequency in Hz.
     * @return value of the frequency in Hz
     * @see #getWavelength()
     */
    double getFrequency();

    /** Get the wavelength in meters.
     * @return wavelength in meters
     * @see #getFrequency()
     */
    default double getWavelength() {
        return Constants.SPEED_OF_LIGHT / getFrequency();
    }

    /** Check if two radio waves are closer than {@link #ONE_MILLI_HERTZ}.
     * @param other other radio wave to check against instance
     * @return true if radio waves are closer than {@link #ONE_MILLI_HERTZ}
     * @see #closeTo(RadioWave, double)
     * @since 13.0
     */
    default boolean closeTo(final RadioWave other) {
        return closeTo(other, ONE_MILLI_HERTZ);
    }

    /** Check if two radio waves are closer than tolerance.
     * @param other other radio wave to check against instance
     * @param tolerance frequency tolerance in Hz
     * @return true if radio waves are closer than tolerance
     * @see #ONE_MILLI_HERTZ
     * @see #closeTo(RadioWave)
     * @since 13.0
     */
    default boolean closeTo(final RadioWave other, final double tolerance) {
        return FastMath.abs(getFrequency() - other.getFrequency()) < tolerance;
    }

}
