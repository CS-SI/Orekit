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
 * Factory for {@link LegacyNavigationMessage}.
 * @param <O> type of the orbital elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class LegacyNavigationMessageFactory<O extends LegacyNavigationMessage<O>>
    extends AbstractNavigationMessageFactory<O> {

    /** Issue of Data, Ephemeris. */
    private int iode;

    /** Issue of Data, Clock. */
    private int iodc;

    /** The user SV accuracy (m). */
    private double svAccuracy;

    /** Satellite health status. */
    private int svHealth;

    /** Fit interval. */
    private int fitInterval;

    /** Codes on L2 channel. */
    private int l2Codes;

    /** L2 P data flags. */
    private int l2PFlags;

    /** Simple constructor.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param mu              central attraction coefficient (m³/s²)
     */
    public LegacyNavigationMessageFactory(final double angularVelocity,
                                          final TimeScales timeScales, final SatelliteSystem system,
                                          final String type, final Frame inertial, final Frame bodyFixed,
                                          final double mu) {
        super(angularVelocity, timeScales, system, type, inertial, bodyFixed, mu);
    }

    /** Get the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /** Set the Issue Of Data Ephemeris (IODE).
     * @param iode the Issue Of Data Ephemeris (IODE)
     */
    public void setIODE(final int iode) {
        this.iode = iode;
    }

    /** Get the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /** Set the Issue Of Data Clock (IODC).
     * @param iodc the Issue Of Data Clock (IODC)
     */
    public void setIODC(final int iodc) {
        this.iodc = iodc;
    }

    /** Get the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /** Set the user SV accuray (meters).
     * @param svAccuracy the user SV accuracy
     */
    public void setSvAccuracy(final double svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

    /** Get the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /** Set the satellite health status.
     * @param svHealth the satellite health status
     */
    public void setSvHealth(final int svHealth) {
        this.svHealth = svHealth;
    }

    /** Get the fit interval.
     * @return the fit interval
     */
    public int getFitInterval() {
        return fitInterval;
    }

    /** Set the fit interval.
     * @param fitInterval fit interval
     */
    public void setFitInterval(final int fitInterval) {
        this.fitInterval = fitInterval;
    }

    /** Get the codes on L2 channel.
     * @return codes on L2 channel
     */
    public int getL2Codes() {
        return l2Codes;
    }

    /** Set the codes on L2 channel.
     * @param l2Codes codes on L2 channel
     */
    public void setL2Codes(final int l2Codes) {
        this.l2Codes = l2Codes;
    }

    /** Get the L2 P data flags.
     * @return L2 P data flags
     */
    public int getL2PFlags() {
        return l2PFlags;
    }

    /** Set the L2 P data flags.
     * @param l2PFlags L2 P data flags
     */
    public void setL2PFlags(final int l2PFlags) {
        this.l2PFlags = l2PFlags;
    }

}
