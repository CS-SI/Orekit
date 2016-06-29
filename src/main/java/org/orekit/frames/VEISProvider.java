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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Veis 1950 Frame.
 * <p>Its parent frame is the {@link GTODProvider} without EOP correction application.
 * <p>This frame is mainly provided for consistency with legacy softwares.</p>
 * @author Pascal Parraud
 */
class VEISProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130530L;

    /** Reference date. */
    private static AbsoluteDate VST_REFERENCE =
        new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeScalesFactory.getTAI());

    /** 1st coef for Veis sidereal time computation in radians (100.075542 deg). */
    private static final double VST0 = 1.746647708617871;

    /** 2nd coef for Veis sidereal time computation in rad/s (0.985612288 deg/s). */
    private static final double VST1 = 0.17202179573714597e-1;

    /** Veis sidereal time derivative in rad/s. */
    private static final double VSTD = 7.292115146705209e-5;

    /** Constructor for the singleton.
     */
    VEISProvider() {
    }

    /** Get the transform from GTOD at specified date.
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // offset from FIFTIES epoch (UT1 scale)
        final double dtai = date.durationFrom(VST_REFERENCE);
        final double dutc = TimeScalesFactory.getUTC().offsetFromTAI(date);
        final double dut1 = 0.0; // fixed at 0 since Veis parent is GTOD frame WITHOUT EOP corrections

        final double tut1 = dtai + dutc + dut1;
        final double ttd  = tut1 / Constants.JULIAN_DAY;
        final double rdtt = ttd - (int) ttd;

        // compute Veis sidereal time, in radians
        final double vst = (VST0 + VST1 * ttd + MathUtils.TWO_PI * rdtt) % MathUtils.TWO_PI;

        // compute angular rotation of Earth, in rad/s
        final Vector3D rotationRate = new Vector3D(-VSTD, Vector3D.PLUS_K);

        // set up the transform from parent GTOD
        return new Transform(date,
                             new Rotation(Vector3D.PLUS_K, vst, RotationConvention.VECTOR_OPERATOR),
                             rotationRate);

    }

}
