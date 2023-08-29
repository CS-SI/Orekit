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
package org.orekit.files.rinex.observation;

import org.orekit.gnss.ObservationType;

/** Observation Data.
 * @since 9.2
 */
public class ObservationData {

    /** Observed RINEX frequency. */
    private final ObservationType observationType;

    /** Observed value. */
    private final double value;

    /** Loss of Lock Indicator (LLI). */
    private final int lli;

    /** Signal strength. */
    private final int signalStrength;

    /** Simple constructor.
     * @param observationType observation type
     * @param value observed value (may be {@code Double.NaN} if observation not available)
     * @param lli Loss of Lock Indicator
     * @param signalStrength signal strength
     */
    public ObservationData(final ObservationType observationType,
                           final double value, final int lli, final int signalStrength) {
        this.observationType = observationType;
        this.value           = value;
        this.lli             = lli;
        this.signalStrength  = signalStrength;
    }

    /** Get the observation type.
     * @return observation type
     */
    public ObservationType getObservationType() {
        return observationType;
    }

    /** Get the observed value.
     * @return observed value (may be {@code Double.NaN} if observation not available)
     */
    public double getValue() {
        return value;
    }

    /** Get the Loss of Lock Indicator.
     * @return Loss of Lock Indicator
     */
    public int getLossOfLockIndicator() {
        return lli;
    }

    /** Get the signal strength.
     * @return signal strength
     */
    public int getSignalStrength() {
        return signalStrength;
    }

}
