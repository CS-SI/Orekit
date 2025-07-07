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
package org.orekit.files.rinex.clock;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;

/** Clock data for a single station.
 * <p> Data epoch is not linked to any time system in order to parse files with missing lines.
 * Though, the default version of the getEpoch() method links the data time components with the clock file object time scale.
 * The latter can be set with a default value (UTC). Caution is recommended.
 * @since 14.0
 */
public class ClockDataLine {

    /** Clock data type. */
    private final ClockDataType dataType;

    /** Receiver/Satellite name. */
    private final String name;

    /** Data line epoch. */
    private final AbsoluteDate epoch;

    /** Number of data values to follow. This number might not represent the non zero values in the line. */
    private final int numberOfValues;

    /** Clock bias (seconds). */
    private final double clockBias;

    /** Clock bias sigma (seconds). */
    private final double clockBiasSigma;

    /** Clock rate (dimensionless). */
    private final double clockRate;

    /** Clock rate sigma (dimensionless). */
    private final double clockRateSigma;

    /** Clock acceleration (seconds^-1). */
    private final double clockAcceleration;

    /** Clock acceleration sigma (seconds^-1). */
    private final double clockAccelerationSigma;

    /** Constructor.
     * @param type                   clock data type
     * @param name                   receiver/satellite name
     * @param epoch                  data line epoch
     * @param numberOfValues         number of values to follow
     * @param clockBias              clock bias in seconds
     * @param clockBiasSigma         clock bias sigma in seconds
     * @param clockRate              clock rate
     * @param clockRateSigma         clock rate sigma
     * @param clockAcceleration      clock acceleration in seconds^-1
     * @param clockAccelerationSigma clock acceleration in seconds^-1
     */
    public ClockDataLine(final ClockDataType type, final String name,
                         final AbsoluteDate epoch, final int numberOfValues,
                         final double clockBias, final double clockBiasSigma,
                         final double clockRate, final double clockRateSigma,
                         final double clockAcceleration, final double clockAccelerationSigma) {
        this.dataType               = type;
        this.name                   = name;
        this.epoch                  = epoch;
        this.numberOfValues         = numberOfValues;
        this.clockBias              = clockBias;
        this.clockBiasSigma         = clockBiasSigma;
        this.clockRate              = clockRate;
        this.clockRateSigma         = clockRateSigma;
        this.clockAcceleration      = clockAcceleration;
        this.clockAccelerationSigma = clockAccelerationSigma;
    }

    /** Getter for the clock data type.
     * @return the clock data type
     */
    public ClockDataType getDataType() {
        return dataType;
    }

    /** Getter for the receiver/satellite name.
     * @return the receiver/satellite name
     */
    public String getName() {
        return name;
    }

    /** Getter for the number of values to follow.
     * @return the number of values to follow
     */
    public int getNumberOfValues() {
        return numberOfValues;
    }

    /** Get data line epoch.
     * @return the data line epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Getter for the clock bias.
     * @return the clock bias in seconds
     */
    public double getClockBias() {
        return clockBias;
    }

    /** Getter for the clock bias sigma.
     * @return the clock bias sigma in seconds
     */
    public double getClockBiasSigma() {
        return clockBiasSigma;
    }

    /** Getter for the clock rate.
     * @return the clock rate
     */
    public double getClockRate() {
        return clockRate;
    }

    /** Getter for the clock rate sigma.
     * @return the clock rate sigma
     */
    public double getClockRateSigma() {
        return clockRateSigma;
    }

    /** Getter for the clock acceleration.
     * @return the clock acceleration in seconds^-1
     */
    public double getClockAcceleration() {
        return clockAcceleration;
    }

    /** Getter for the clock acceleration sigma.
     * @return the clock acceleration sigma in seconds^-1
     */
    public double getClockAccelerationSigma() {
        return clockAccelerationSigma;
    }

}
