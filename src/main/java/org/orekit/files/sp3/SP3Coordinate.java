/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** A single record of position clock and possibly derivatives in an SP3 file.
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Coordinate extends TimeStampedPVCoordinates {

    /** Dummy coordinate with all fields set to 0.0. */
    public static final SP3Coordinate DUMMY = new SP3Coordinate(AbsoluteDate.ARBITRARY_EPOCH,
                                                                Vector3D.ZERO, null, Vector3D.ZERO, null,
                                                                SP3Utils.CLOCK_UNIT.toSI(SP3Utils.DEFAULT_CLOCK_VALUE),
                                                                Double.NaN,
                                                                SP3Utils.CLOCK_RATE_UNIT.toSI(SP3Utils.DEFAULT_CLOCK_RATE_VALUE),
                                                                Double.NaN,
                                                                false, false, false, false);

    /** Serializable UID. */
    private static final long serialVersionUID = 20230903L;

    /** Clock correction in s. */
    private final double clock;

    /** Clock rate in s / s. */
    private final double clockRate;

    /** Position accuracy. */
    private final Vector3D positionAccuracy;

    /** Velocity accuracy. */
    private final Vector3D velocityAccuracy;

    /** Clock accuracy. */
    private final double clockAccuracy;

    /** Clock rate accuracy. */
    private final double clockRateAccuracy;

    /** Clock event flag. */
    private final boolean clockEvent;

    /** Clock prediction flag. */
    private final boolean clockPrediction;

    /** Orbit maneuver event flag. */
    private final boolean orbitManeuverEvent;

    /** Clock orbit prediction flag. */
    private final boolean orbitPrediction;

    /** Create a coordinate with position and velocity.
     * @param date      of validity.
     * @param position  of the satellite.
     * @param positionAccuracy  of the satellite (null if not known).
     * @param velocity  of the satellite.
     * @param velocityAccuracy  of the satellite (null if not known).
     * @param clock     correction in s.
     * @param clockAccuracy     correction in s ({@code Double.NaN} if not known).
     * @param clockRate in s / s.
     * @param clockRateAccuracy in s / s ({@code Double.NaN} if not known).
     * @param clockEvent clock event flag
     * @param clockPrediction clock prediction flag
     * @param orbitManeuverEvent orbit maneuver event flag
     * @param orbitPrediction flag
     */
    public SP3Coordinate(final AbsoluteDate date,
                         final Vector3D position,          final Vector3D positionAccuracy,
                         final Vector3D velocity,          final Vector3D velocityAccuracy,
                         final double clock,               final double clockAccuracy,
                         final double clockRate,           final double clockRateAccuracy,
                         final boolean clockEvent,         final boolean clockPrediction,
                         final boolean orbitManeuverEvent, final boolean orbitPrediction) {

        super(date, position, velocity, Vector3D.ZERO);
        this.clock     = clock;
        this.clockRate = clockRate;

        this.positionAccuracy   = positionAccuracy;
        this.velocityAccuracy   = velocityAccuracy;
        this.clockAccuracy      = clockAccuracy;
        this.clockRateAccuracy  = clockRateAccuracy;
        this.clockEvent         = clockEvent;
        this.clockPrediction    = clockPrediction;
        this.orbitManeuverEvent = orbitManeuverEvent;
        this.orbitPrediction    = orbitPrediction;

    }

    /** Get the clock correction value.
     * @return the clock correction in s.
     */
    public double getClockCorrection() {
        return clock;
    }

    /** Get the clock rate.
     * @return the clock rate of change in s/s.
     */
    public double getClockRateChange() {
        return clockRate;
    }

    /** Get the position accuracy.
     * @return position accuracy in m (null if not known).
     */
    public Vector3D getPositionAccuracy() {
        return positionAccuracy;
    }

    /** Get the velocity accuracy.
     * @return velocity accuracy in m/s (null if not known).
     */
    public Vector3D getVelocityAccuracy() {
        return velocityAccuracy;
    }

    /** Get the clock accuracy.
     * @return clock accuracy in s ({@code Double.NaN} if not known).
     */
    public double getClockAccuracy() {
        return clockAccuracy;
    }

    /** Get the clock rate accuracy.
     * @return clock rate accuracy in s/s ({@code Double.NaN} if not known).
     */
    public double getClockRateAccuracy() {
        return clockRateAccuracy;
    }

    /** Get clock event flag.
     * @return true if clock event flag is set
     */
    public boolean hasClockEvent() {
        return clockEvent;
    }

    /** Get clock prediction flag.
     * @return true if clock prediction flag is set
     */
    public boolean hasClockPrediction() {
        return clockPrediction;
    }

    /** Get orbit maneuver event flag.
     * @return true if orbit maneuver event flag is set
     */
    public boolean hasOrbitManeuverEvent() {
        return orbitManeuverEvent;
    }

    /** Get orbit prediction flag.
     * @return true if orbit prediction flag is set
     */
    public boolean hasOrbitPrediction() {
        return orbitPrediction;
    }

}
