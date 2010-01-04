/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.Date;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;

/** This class holds an Earth Orientation Parameters entry.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class EOPEntry implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2662304704586116425L;

    /** Entry date (modified julian day, 00h00 UTC scale). */
    private final int mjd;

    /** Entry date (absolute date). */
    private final AbsoluteDate date;

    /** UT1-UTC. */
    private final double dt;

    /** Length of day. */
    private final double lod;

   /** Simple constructor.
    * @param mjd entry date (modified julian day, 00h00 UTC scale)
    * @param dt UT1-UTC in seconds
    * @param lod length of day
    * @exception OrekitException if UTC time scale cannot be retrieved
    */
    public EOPEntry(final int mjd, final double dt, final double lod)
        throws OrekitException {

        this.mjd = mjd;

        // convert mjd date at 00h00 UTC to absolute date
        final long javaTime = (mjd - 40587) * 86400000l;
        date = new AbsoluteDate(new Date(javaTime), TimeScalesFactory.getUTC());

        this.dt    = dt;
        this.lod   = lod;

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

}
