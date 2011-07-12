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
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Veis 1950 Frame.
 * <p>Its parent frame is the {@link PEFFrame} without EOP correction application.<p>
 * <p>This frame is mainly provided for consistency with legacy softwares.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
class VEISFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -8200570473557338686L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Reference date. */
    private static AbsoluteDate VST_REFERENCE =
        new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeScalesFactory.getTAI());

    /** 1st coef for Veis sidereal time computation in radians (100.075542 deg). */
    private static final double VST0 = 1.746647708617871;

    /** 2nd coef for Veis sidereal time computation in rad/s (0.985612288 deg/s). */
    private static final double VST1 = 0.17202179573714597e-1;

    /** Veis sidereal time derivative in rad/s. */
    private static final double VSTD = 7.292115146705209e-5;

    /** Cached date to avoid useless calculus. */
    private AbsoluteDate cachedDate;

    /** EOP history. */
    private final EOP1980History eopHistory;

    /** Constructor for the singleton.
     * @param date the date.
     * @param name name of the frame
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public VEISFrame(final AbsoluteDate date, final String name)
        throws OrekitException {

        super(FramesFactory.getPEF(false), null, name, true);

        eopHistory = FramesFactory.getEOP1980History();

        // frame synchronization
        updateFrame(date);

    }

    /** Update the frame to the given date.
     * @param date new value of the date
     * @exception OrekitException if data embedded in the library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // offset from FIFTIES epoch (UT1 scale)
            final double dtai = date.durationFrom(VST_REFERENCE);
            final double dutc = TimeScalesFactory.getUTC().offsetFromTAI(date);
            final double dut1 = eopHistory.getUT1MinusUTC(date);

            final double tut1 = dtai + dutc + dut1;
            final double ttd  = tut1 / Constants.JULIAN_DAY;
            final double rdtt = ttd - (int) ttd;

            // compute Veis sidereal time, in radians
            final double vst = (VST0 + VST1 * ttd + TWO_PI * rdtt) % TWO_PI;

            // compute angular rotation of Earth, in rad/s
            final Vector3D rotationRate = new Vector3D(-VSTD, Vector3D.PLUS_K);

            // set up the transform from parent PEF
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, vst), rotationRate));

            cachedDate = date;
        }

    }

}
