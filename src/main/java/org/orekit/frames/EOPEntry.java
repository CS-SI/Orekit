/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;

/** This class holds an Earth Orientation Parameters entry.
 * @author Luc Maisonobe
 */
public class EOPEntry implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130924L;

    /** Entry date (modified julian day, 00h00 UTC scale). */
    private final int mjd;

    /** Entry date (absolute date). */
    private final AbsoluteDate date;

    /** UT1-UTC. */
    private final double dt;

    /** Length of day. */
    private final double lod;

    /** X component of pole motion. */
    private final double x;

    /** Y component of pole motion. */
    private final double y;

    /** Correction for nutation in longitude. */
    private final double ddPsi;

    /** Correction for nutation in obliquity. */
    private final double ddEps;

    /** Correction for nutation in Celestial Intermediate Pole (CIP) coordinates. */
    private final double dx;

    /** Correction for nutation in Celestial Intermediate Pole (CIP) coordinates. */
    private final double dy;

   /** Simple constructor.
    * @param mjd entry date (modified Julian day, 00h00 UTC scale)
    * @param dt UT1-UTC in seconds
    * @param lod length of day
    * @param x X component of pole motion
    * @param y Y component of pole motion
    * @param ddPsi correction for nutation in longitude δΔΨ
    * @param ddEps correction for nutation in obliquity δΔε
    * @param dx correction for Celestial Intermediate Pole (CIP) coordinates
    * @param dy correction for Celestial Intermediate Pole (CIP) coordinates
    * @exception OrekitException if UTC time scale cannot be retrieved
    */
    public EOPEntry(final int mjd, final double dt, final double lod,
                    final double x, final double y,
                    final double ddPsi, final double ddEps,
                    final double dx, final double dy)
        throws OrekitException {

        this.mjd   = mjd;
        this.date  = AbsoluteDate.createMJDDate(mjd, 0.0, TimeScalesFactory.getUTC());
        this.dt    = dt;
        this.lod   = lod;
        this.x     = x;
        this.y     = y;
        this.ddPsi = ddPsi;
        this.ddEps = ddEps;
        this.dx    = dx;
        this.dy    = dy;

    }

    /** Get the entry date (modified julian day, 00h00 UTC scale).
     * @return entry date
     * @see #getDate()
     */
    public int getMjd() {
        return mjd;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the UT1-UTC value.
     * @return UT1-UTC in seconds
     */
    public double getUT1MinusUTC() {
        return dt;
    }

    /** Get the LoD (Length of Day) value.
     * @return LoD in seconds
     */
    public double getLOD() {
        return lod;
    }

    /** Get the X component of the pole motion.
     * @return X component of pole motion
     */
    public double getX() {
        return x;
    }

    /** Get the Y component of the pole motion.
     * @return Y component of pole motion
     */
    public double getY() {
        return y;
    }

    /** Get the correction for nutation in longitude δΔΨ.
     * @return correction for nutation in longitude  δΔΨ
     */
    public double getDdPsi() {
        return ddPsi;
    }

    /** Get the correction for nutation in obliquity δΔε.
     * @return correction for nutation in obliquity δΔε
     */
    public double getDdEps() {
        return ddEps;
    }

    /** Get the correction for Celestial Intermediate Pole (CIP) coordinates.
     * @return correction for Celestial Intermediate Pole (CIP) coordinates
     */
    public double getDx() {
        return dx;
    }

    /** Get the correction for Celestial Intermediate Pole (CIP) coordinates.
     * @return correction for Celestial Intermediate Pole (CIP) coordinates
     */
    public double getDy() {
        return dy;
    }

}
