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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Terrestrial Intermediate Reference Frame 2000.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link CIRF2000Frame}</p>
 * @version $Revision$ $Date$
 */
class TIRF2000Frame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 2467681437070913647L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Reference date of Capitaine's Earth Rotation Angle model. */
    private static final AbsoluteDate ERA_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());

    /** Constant term of Capitaine's Earth Rotation Angle model. */
    private static final double ERA_0 = TWO_PI * 0.7790572732640;

    /** Rate term of Capitaine's Earth Rotation Angle model.
     * (radians per day, main part) */
    private static final double ERA_1A = TWO_PI;

    /** Rate term of Capitaine's Earth Rotation Angle model.
     * (radians per day, fractional part) */
    private static final double ERA_1B = ERA_1A * 0.00273781191135448;

    /** Cached date to avoid useless calculus. */
    private AbsoluteDate cachedDate;

    /** Earth Rotation Angle, in radians. */
    private double era;

    /** Tidal correction (null if tidal effects are ignored). */
    private final TidalCorrection tidalCorrection;

    /** EOP history. */
    private final EOP2000History eopHistory;

    /** Simple constructor, ignoring tidal effects.
     * @param date the current date
     * @param name the string representation
     * @exception OrekitException if nutation cannot be computed
     */
    protected TIRF2000Frame(final AbsoluteDate date, final String name)
        throws OrekitException {
        this(true, date, name);
    }

    /** Simple constructor.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @param date the current date
     * @param name the string representation
     * @exception OrekitException if nutation cannot be computed
     */
    protected TIRF2000Frame(final boolean ignoreTidalEffects,
                            final AbsoluteDate date, final String name)
        throws OrekitException {

        super(FramesFactory.getCIRF2000(), null, name, false);
        tidalCorrection = ignoreTidalEffects ? null : new TidalCorrection();
        eopHistory = FramesFactory.getEOP2000History();

        // everything is in place, we can now synchronize the frame
        updateFrame(date);

    }

    /** Get the pole correction.
     * @param date date at which the correction is desired
     * @return pole correction including both EOP values and tidal correction
     * if they have been configured
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        final PoleCorrection eop = eopHistory.getPoleCorrection(date);
        if (tidalCorrection == null) {
            return eop;
        } else {
            final PoleCorrection tidal = tidalCorrection.getPoleCorrection(date);
            return new PoleCorrection(eop.getXp() + tidal.getXp(),
                                      eop.getYp() + tidal.getYp());
        }
    }

    /** Update the frame to the given date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // compute Earth Rotation Angle using Nicole Capitaine model (2000)
            final double tidalDtu1   = (tidalCorrection == null) ? 0 : tidalCorrection.getDUT1(date);
            final double dtu1        = eopHistory.getUT1MinusUTC(date);
            final double utcMinusTai = TimeScalesFactory.getUTC().offsetFromTAI(date);
            final double tu =
                (date.durationFrom(ERA_REFERENCE) + utcMinusTai + dtu1 + tidalDtu1) / Constants.JULIAN_DAY;
            era  = ERA_0 + ERA_1A * tu + ERA_1B * tu;
            era -= TWO_PI * Math.floor((era + Math.PI) / TWO_PI);

            // set up the transform from parent CIRF
            final Vector3D rotationRate = new Vector3D((ERA_1A + ERA_1B) / Constants.JULIAN_DAY, Vector3D.PLUS_K);
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, -era), rotationRate));
            cachedDate = date;

        }
    }

    /** Get the Earth Rotation Angle at the current date.
     * @param  date the date
     * @return Earth Rotation Angle at the current date in radians
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEarthRotationAngle(final AbsoluteDate date) throws OrekitException {
        updateFrame(date);
        return era;
    }

}
