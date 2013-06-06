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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Greenwich True Of Date Frame, also known as True of Date Rotating frame (TDR)
 * or Greenwich Rotating Coordinate frame (GCR).
 * <p> This frame handles the sidereal time according to IAU-82 model.</p>
 * <p> Its parent frame is the {@link TODProvider}.</p>
 * <p> The pole motion is not applied here.</p>
 * @author Pascal Parraud
 * @author Thierry Ceolin
 */
public class GTODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130606L;

    /** Radians per second of time. */
    private static final double RADIANS_PER_SECOND = MathUtils.TWO_PI / Constants.JULIAN_DAY;

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** Reference date for IAU 1982 GMST-UT1 model. */
    private static final AbsoluteDate GMST_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());

    /** First coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_0 = 24110.54841;

    /** Second coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_1 = 8640184.812866;

    /** Third coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_2 = 0.093104;

    /** Fourth coefficient of IAU 1982 GMST-UT1 model. */
    private static final double GMST_3 = -6.2e-6;

    /** EOP history. */
    private final EOP1980History eopHistory;

    /** Provider for the parent ToD frame. */
    private final TODProvider todProvider;

    /** Simple constructor.
     * @param todProvider provider for the parent ToD frame
     * @param applyEOPCorr if true, EOP correction is applied (here, LOD)
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected GTODProvider(final TODProvider todProvider, final boolean applyEOPCorr)
        throws OrekitException {
        this.eopHistory  = applyEOPCorr ? FramesFactory.getEOP1980History() : null;
        this.todProvider = todProvider;
    }

    /** Get the transform from TOD at specified date.
     * <p>The update considers the Earth rotation from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute Greenwich apparent sidereal time, in radians
        final double gast = getGAST(date);

        // compute true angular rotation of Earth, in rad/s
        final double lod = (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
        final double omp = AVE * (1 - lod / Constants.JULIAN_DAY);
        final Vector3D rotationRate = new Vector3D(omp, Vector3D.PLUS_K);

        // set up the transform from parent TOD
        return new Transform(date, new Rotation(Vector3D.PLUS_K, -gast), rotationRate);

    }

    /** Get the Greenwich mean sidereal time, in radians.
     * @param date current date
     * @return Greenwich mean sidereal time, in radians
     * @exception OrekitException if UTS time scale cannot be retrieved
     * @see #getGAST(AbsoluteDate)
     */
    public double getGMST(final AbsoluteDate date) throws OrekitException {

        // offset in julian centuries from J2000 epoch (UT1 scale)
        final double dtai = date.durationFrom(GMST_REFERENCE);
        final double dutc = TimeScalesFactory.getUTC().offsetFromTAI(date);
        final double dut1 = (eopHistory == null) ? 0.0 : eopHistory.getUT1MinusUTC(date);

        final double tut1 = dtai + dutc + dut1;
        final double tt   = tut1 / Constants.JULIAN_CENTURY;

        // Seconds in the day, adjusted by 12 hours because the
        // UT1 is supplied as a Julian date beginning at noon.
        final double sd = (tut1 + Constants.JULIAN_DAY / 2.) % Constants.JULIAN_DAY;

        // compute Greenwich mean sidereal time, in radians
        return (((GMST_3 * tt + GMST_2) * tt + GMST_1) * tt + GMST_0 + sd) * RADIANS_PER_SECOND;

    }

    /** Get the Greenwich apparent sidereal time, in radians.
     * <p>
     * Greenwich apparent sidereal time is {@link
     * #getGMST(AbsoluteDate) Greenwich mean sidereal time} plus {@link
     * TODProvider#getEquationOfEquinoxes(AbsoluteDate) equation of equinoxes}.
     * </p>
     * @param date current date
     * @return Greenwich apparent sidereal time, in radians
     * @exception OrekitException if UTS taime scale cannot be retrieved
     * @see #getGMST(AbsoluteDate)
     */
    public double getGAST(final AbsoluteDate date) throws OrekitException {

        // offset from J2000.0 epoch
        final double eqe = todProvider.getEquationOfEquinoxes(date);

        // compute Greenwich apparent sidereal time, in radians
        return getGMST(date) + eqe;

    }

}
