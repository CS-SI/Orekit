/* Copyright 2002-2011 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.files.general;

import java.io.Serializable;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

/** Contains the position/velocity of a satellite at an specific epoch.
 * @author Thomas Neidhart
 */
public class SatelliteTimeCoordinate implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2099947583052252633L;

    /** Epoch for this entry. */
    private AbsoluteDate epoch;

    /** Position/velocity coordinates for this entry. */
    private PVCoordinates coordinate;

    /** Clock correction in micro-seconds. */
    private double clockCorrection;

    /** Clock rate change. */
    private double clockRateChange;

    /** Creates a new {@link SatelliteTimeCoordinate} instance with
     * a given epoch and coordinate.
     * @param time the epoch of the entry
     * @param coord the coordinate of the entry
     */
    public SatelliteTimeCoordinate(final AbsoluteDate time,
                                   final PVCoordinates coord) {
        this(time, coord, 0.0d, 0.0d);
    }

    /** Creates a new {@link SatelliteTimeCoordinate} object with a given epoch
     * and position coordinate. The velocity is set to a zero vector.
     * @param time the epoch of the entry
     * @param pos the position coordinate of the entry
     * @param clock the clock value in (micro-seconds)
     */
    public SatelliteTimeCoordinate(final AbsoluteDate time,
                                   final Vector3D pos, final double clock) {
        this(time, new PVCoordinates(pos, Vector3D.ZERO), clock, 0.0d);
    }

    /** Creates a new {@link SatelliteTimeCoordinate} instance with a given
     * epoch, coordinate and clock value / rate change.
     * @param time the epoch of the entry
     * @param coord the coordinate of the entry
     * @param clockCorr the clock value that corresponds to this coordinate
     * @param rateChange the clock rate change
     */
    public SatelliteTimeCoordinate(final AbsoluteDate time,
                                   final PVCoordinates coord,
                                   final double clockCorr,
                                   final double rateChange) {
        this.epoch = time;
        this.coordinate = coord;
        this.clockCorrection = clockCorr;
        this.clockRateChange = rateChange;
    }

    /** Returns the epoch for this coordinate.
     * @return the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return getEpoch();
    }

    /** Set the epoch for this coordinate.
     * @param epoch the epoch to be set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** Returns the coordinate of this entry.
     * @return the coordinate
     */
    public PVCoordinates getCoordinate() {
        return coordinate;
    }

    /** Set the coordinate for this entry.
     * @param coordinate the coordinate to be set
     */
    public void setCoordinate(final PVCoordinates coordinate) {
        this.coordinate = coordinate;
    }

    /** Returns the clock correction value.
     * @return the clock correction
     */
    public double getClockCorrection() {
        return clockCorrection;
    }

    /** Set the clock correction to the given value.
     * @param corr the clock correction value
     */
    public void setClockCorrection(final double corr) {
        this.clockCorrection = corr;
    }

    /** Returns the clock rate change value.
     * @return the clock rate change
     */
    public double getClockRateChange() {
        return clockRateChange;
    }

    /** Set the clock rate change to the given value.
     * @param rateChange the clock rate change value
     */
    public void setClockRateChange(final double rateChange) {
        this.clockRateChange = rateChange;
    }
}
