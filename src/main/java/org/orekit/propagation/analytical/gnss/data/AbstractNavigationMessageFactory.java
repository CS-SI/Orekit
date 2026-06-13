/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link AbstractNavigationMessage}.
 * @param <O> type of the orbital elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractNavigationMessageFactory<O extends AbstractNavigationMessage<O>>
    extends GNSSOrbitalElementsFactory<O> {

    /** Transmission time.
     * @since 12.0
     */
    private double transmissionTime;

    /** Simple constructor.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param mu              central attraction coefficient (m³/s²)
     */
    public AbstractNavigationMessageFactory(final double angularVelocity,
                                            final TimeScales timeScales, final SatelliteSystem system,
                                            final String type, final Frame inertial, final Frame bodyFixed,
                                            final double mu) {
        super(angularVelocity, timeScales, system, type, inertial, bodyFixed, mu);
    }

    /** Get transmission time.
     * @return transmission time
     */
    public double getTransmissionTime() {
        return transmissionTime;
    }

    /** Set transmission time.
     * @param transmissionTime transmission time
     */
    public void setTransmissionTime(final double transmissionTime) {
        this.transmissionTime = transmissionTime;
    }

}
