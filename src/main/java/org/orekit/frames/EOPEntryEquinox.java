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

/** This class holds an Earth Orientation Parameters entry for equinox-based paradigm.
 * @author Luc Maisonobe
 */
public class EOPEntryEquinox extends EOPEntry {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130919L;

    /** Correction for nutation in longitude. */
    private final double ddPsi;

    /** Correction for nutation in obliquity. */
    private final double ddEps;

   /** Simple constructor.
    * @param mjd entry date (modified julian day, 00h00 UTC scale)
    * @param dt UT1-UTC in seconds
    * @param lod length of day
    * @param x X component of pole motion
    * @param y Y component of pole motion
    * @param ddPsi correction for nutation in longitude &delta;&Delta;&Psi;
    * @param ddEps correction for nutation in obliquity &delta;&Delta;&epsilon;
    * @exception OrekitException if UTC time scale cannot be retrieved
    */
    public EOPEntryEquinox(final int mjd, final double dt, final double lod,
                           final double x, final double y,
                           final double ddPsi, final double ddEps)
        throws OrekitException {
        super(mjd, dt, lod, x, y);
        this.ddPsi = ddPsi;
        this.ddEps = ddEps;
    }

    /** Get the correction for nutation in longitude &delta;&Delta;&Psi;.
     * @return correction for nutation in longitude  &delta;&Delta;&Psi;
     */
    public double getDdPsi() {
        return ddPsi;
    }

    /** Get the correction for nutation in obliquity &delta;&Delta;&epsilon;.
     * @return correction for nutation in obliquity &delta;&Delta;&epsilon;
     */
    public double getDdEps() {
        return ddEps;
    }

}
