/* Copyright 2002-2008 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TAIScale;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;

/** Pseudo Earth Fixed Frame.
 * <p> This frame handles the sidereal time according to IAU-82 model.</p>
 * <p> Its parent frame is the {@link TEMEFrame}.</p>
 * <p>The pole motion is not applied here.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
class PEFFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -1980349559678388058L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Seconds per day. */
    private static final double SECONDS_PER_DAY = 86400.;

    /** Radians per second of time. */
    private static final double RADIANS_PER_SECOND = TWO_PI / SECONDS_PER_DAY;

    /** Julian century per second. */
    private static final double JULIAN_CENTURY_PER_SECOND = 1.0 / (36525.0 * SECONDS_PER_DAY);

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** Reference date for IAU 1982 GMST-UT1 model. */
    private static final AbsoluteDate GMST_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TAIScale.getInstance());

    /** First coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_0 = 24110.54841;
    /** Second coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_1 = 8640184.812866;
    /** Third coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_2 = 0.093104;
    /** Fourth coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_3 = -6.2e-6;

    /** Cached date to avoid useless calculus. */
    private AbsoluteDate cachedDate;

    /** Simple constructor, applying nutation correction.
     * @param date the date.
     * @param name name of the frame
     * @exception OrekitException if the nutation model data embedded
     * in the library cannot be read.
     */
    protected PEFFrame(final AbsoluteDate date, final String name)
        throws OrekitException {

        this(true, date, name);

    }

    /** Simple constructor.
     * @param applyEOPCorr if true, nutation correction is applied
     * @param date the current date
     * @param name the string representation
     * @exception OrekitException if the nutation model data embedded
     * in the library cannot be read.
     */
    protected PEFFrame(final boolean applyEOPCorr,
                       final AbsoluteDate date, final String name)
        throws OrekitException {

        super(FrameFactory.getTEME(applyEOPCorr), null, name);

        // everything is in place, we can now synchronize the frame
        updateFrame(date);

    }

    /** Update the frame to the given date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // compute equation of the equinoxes
            final Frame teme = this.getParent();
            final double eqe = ((TEMEFrame) teme).getEquationOfEquinoxes(date);

            // offset in julian centuries from J2000 epoch (UT1 scale)
            final double dtai = date.durationFrom(GMST_REFERENCE);
            final double dutc = UTCScale.getInstance().offsetFromTAI(date);
            final double dut1 = EOP1980History.getInstance().getUT1MinusUTC(date);
            final double tut1 = dtai + dutc + dut1;
            final double tt   = tut1 * JULIAN_CENTURY_PER_SECOND;

            // Seconds in the day, adjusted by 12 hours because the
            // UT1 is supplied as a Julian date beginning at noon.
            final double sd = (tut1 + SECONDS_PER_DAY / 2.) % SECONDS_PER_DAY;

            // compute Greenwich mean sidereal time, in radians
            final double gmst = (((GMST_3 * tt + GMST_2) * tt + GMST_1) * tt + GMST_0 + sd) *
                                RADIANS_PER_SECOND;

            // compute Greenwich apparent sidereal time, in radians
            final double gast = gmst + eqe;

            // compute true angular rotation of Earth, in rad/s
            final double lod = EOP1980History.getInstance().getLOD(date);
            final double omp = AVE * (1 - lod / SECONDS_PER_DAY);
            final Vector3D rotationRate = new Vector3D(omp, Vector3D.PLUS_K);

            // set up the transform from parent TEME
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, -gast), rotationRate));

            cachedDate = date;

        }
    }

}
