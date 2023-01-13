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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.time.AbsoluteDate;

/**
 * Base class for ephemeris-based navigation messages.
 * @author Bryan Cazabonne
 * @since 11.0
 *
 * @see GLONASSNavigationMessage
 * @see SBASNavigationMessage
 */
public abstract class AbstractEphemerisMessage {

    /** Ephemeris reference epoch. */
    private AbsoluteDate date;

    /** Time of clock epoch. */
    private AbsoluteDate epochToc;

    /** PRN number of the satellite. */
    private int prn;

    /** Satellite X position in meters. */
    private double x;

    /** Satellite X velocity in meters per second. */
    private double xDot;

    /** Satellite X acceleration in meters per second². */
    private double xDotDot;

    /** Satellite Y position in meters. */
    private double y;

    /** Satellite Y velocity in meters per second. */
    private double yDot;

    /** Satellite Y acceleration in meters per second². */
    private double yDotDot;

    /** Satellite Z position in meters. */
    private double z;

    /** Satellite Z velocity in meters per second. */
    private double zDot;

    /** Satellite Z acceleration in meters per second². */
    private double zDotDot;

    /** Health status. */
    private double health;

    /** Constructor. */
    public AbstractEphemerisMessage() {
        // Nothing to do ...
    }

    /**
     * Getter for the reference date of the ephemeris.
     * @return the reference date of the ephemeris
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Setter for the reference date of the ephemeris.
     * @param date the date to set
     */
    public void setDate(final AbsoluteDate date) {
        this.date = date;
    }

    /**
     * Getter for the time of clock epoch.
     * @return the time of clock epoch
     */
    public AbsoluteDate getEpochToc() {
        return epochToc;
    }

    /**
     * Setter for the time of clock epoch.
     * @param epochToc the epoch to set
     */
    public void setEpochToc(final AbsoluteDate epochToc) {
        this.epochToc = epochToc;
    }

    /**
     * Getter for the PRN number of the satellite.
     * @return the PRN number of the satellite
     */
    public int getPRN() {
        return prn;
    }

    /**
     * Setter for the PRN number of the satellite.
     * @param number the prn number ot set
     */
    public void setPRN(final int number) {
        this.prn = number;
    }

    /**
     * Getter for the satellite X position.
     * @return the satellite X position in meters
     */
    public double getX() {
        return x;
    }

    /**
     * Setter for the satellite X position.
     * @param x satellite X position (meters) to set
     */
    public void setX(final double x) {
        this.x = x;
    }

    /**
     * Getter for the satellite X velocity.
     * @return the satellite X velocity in m/s
     */
    public double getXDot() {
        return xDot;
    }

    /**
     * Setter for the satellite X velocity.
     * @param vx the satellite X velocity (m/s) to set
     */
    public void setXDot(final double vx) {
        this.xDot = vx;
    }

    /**
     * Getter for the satellite X acceleration.
     * @return the satellite X acceleration in m/s²
     */
    public double getXDotDot() {
        return xDotDot;
    }

    /**
     * Setter for the satellite X acceleration.
     * @param ax the satellite X acceleration (m/s²) to set
     */
    public void setXDotDot(final double ax) {
        this.xDotDot = ax;
    }

    /**
     * Getter for the satellite Y position.
     * @return the satellite Y position in meters
     */
    public double getY() {
        return y;
    }

    /**
     * Setter for the satellite Y position.
     * @param y satellite Y position (meters) to set
     */
    public void setY(final double y) {
        this.y = y;
    }

    /**
     * Getter for the satellite Y velocity.
     * @return the satellite Y velocity in m/s
     */
    public double getYDot() {
        return yDot;
    }

    /**
     * Setter for the satellite Y velocity.
     * @param vy the satellite Y velocity (m/s) to set
     */
    public void setYDot(final double vy) {
        this.yDot = vy;
    }

    /**
     * Getter for the satellite Y acceleration.
     * @return the satellite Y acceleration in m/s²
     */
    public double getYDotDot() {
        return yDotDot;
    }

    /**
     * Setter for the satellite Y acceleration.
     * @param ay the satellite Y acceleration (m/s²) to set
     */
    public void setYDotDot(final double ay) {
        this.yDotDot = ay;
    }

    /**
     * Getter for the satellite Z position.
     * @return the satellite Z position in meters
     */
    public double getZ() {
        return z;
    }

    /**
     * Setter for the satellite Z position.
     * @param z satellite Z position (meters) to set
     */
    public void setZ(final double z) {
        this.z = z;
    }

    /**
     * Getter for the satellite Z velocity.
     * @return the satellite Z velocity in m/s
     */
    public double getZDot() {
        return zDot;
    }

    /**
     * Setter for the satellite Z velocity.
     * @param vz the satellite Z velocity (m/s) to set
     */
    public void setZDot(final double vz) {
        this.zDot = vz;
    }

    /**
     * Getter for the satellite Z acceleration.
     * @return the satellite Z acceleration in m/s²
     */
    public double getZDotDot() {
        return zDotDot;
    }

    /**
     * Setter for the satellite Z acceleration.
     * @param az the satellite Z acceleration (m/s²) to set
     */
    public void setZDotDot(final double az) {
        this.zDotDot = az;
    }

    /**
     * Getter for the health status.
     * @return the health status
     */
    public double getHealth() {
        return health;
    }

    /**
     * Setter for the health status.
     * @param health the health status to set
     */
    public void setHealth(final double health) {
        this.health = health;
    }

}
