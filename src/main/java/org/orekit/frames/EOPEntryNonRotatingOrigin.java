/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.orekit.errors.OrekitException;

/** This class holds an Earth Orientation Parameters entry for Non-Rotating Origin paradigm.
 * @author Luc Maisonobe
 */
public class EOPEntryNonRotatingOrigin extends EOPEntry {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130919L;

    /** Correction for nutation in Celestial Intermediate Pole (CIP) coordinates. */
    private final double dx;

    /** Correction for nutation in Celestial Intermediate Pole (CIP) coordinates. */
    private final double dy;

   /** Simple constructor.
    * @param mjd entry date (modified julian day, 00h00 UTC scale)
    * @param dt UT1-UTC in seconds
    * @param lod length of day
    * @param x X component of pole motion
    * @param y Y component of pole motion
    * @param dx correction for Celestial Intermediate Pole (CIP) coordinates
    * @param dy correction for Celestial Intermediate Pole (CIP) coordinates
    * @exception OrekitException if UTC time scale cannot be retrieved
    */
    public EOPEntryNonRotatingOrigin(final int mjd, final double dt, final double lod,
                                     final double x, final double y,
                                     final double dx, final double dy)
        throws OrekitException {
        super(mjd, dt, lod, x, y);
        this.dx = dx;
        this.dy = dy;
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
