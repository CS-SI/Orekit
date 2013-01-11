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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;

/** Terrestrial Intermediate Reference Frame 2000.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link CIRF2000Provider}</p>
 */
class TIRF2000Provider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 7243684504752696164L;

    /** Reference date of Capitaine's Earth Rotation Angle model. */
    private static final AbsoluteDate ERA_REFERENCE =
        new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());

    /** Constant term of Capitaine's Earth Rotation Angle model. */
    private static final double ERA_0 = MathUtils.TWO_PI * 0.7790572732640;

    /** Rate term of Capitaine's Earth Rotation Angle model.
     * (radians per day, main part) */
    private static final double ERA_1A = MathUtils.TWO_PI;

    /** Rate term of Capitaine's Earth Rotation Angle model.
     * (radians per day, fractional part) */
    private static final double ERA_1B = ERA_1A * 0.00273781191135448;

    /** Tidal correction (null if tidal effects are ignored). */
    private final TidalCorrection tidalCorrection;

    /** EOP history. */
    private final EOP2000History eopHistory;

    /** UT1 time scale. */
    private transient UT1Scale ut1;

    /** Simple constructor.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @exception OrekitException if nutation cannot be computed
     */
    protected TIRF2000Provider(final boolean ignoreTidalEffects)
        throws OrekitException {

        tidalCorrection = ignoreTidalEffects ? null : new TidalCorrection();
        eopHistory      = FramesFactory.getEOP2000History();
        ut1             = TimeScalesFactory.getUT1();

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

    /** Get the transform from CIRF 2000 at specified date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // set up the transform from parent CIRF2000
        final Vector3D rotationRate = new Vector3D((ERA_1A + ERA_1B) / Constants.JULIAN_DAY, Vector3D.PLUS_K);
        final double era = getEarthRotationAngle(date);
        return new Transform(date, new Rotation(Vector3D.PLUS_K, -era), rotationRate);

    }

    /** Get the Earth Rotation Angle at the current date.
     * @param  date the date
     * @return Earth Rotation Angle at the current date in radians
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEarthRotationAngle(final AbsoluteDate date) throws OrekitException {

        // compute Earth Rotation Angle using Nicole Capitaine model (2000)
        final double tidalDtu1   = (tidalCorrection == null) ? 0 : tidalCorrection.getDUT1(date);
        final double tu =
                (date.durationFrom(ERA_REFERENCE) + ut1.offsetFromTAI(date) + tidalDtu1) / Constants.JULIAN_DAY;
        return MathUtils.normalizeAngle(ERA_0 + ERA_1A * tu + ERA_1B * tu, 0);

    }

    /** Serialize an instance.
     * @param out stream to serialize instance to
     * @throws IOException if instance cannot be serialized to the stream
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /** Deserialize an instance.
     * @param in stream to deserialize the instance from
     * @throws IOException if instance cannot be serialized from the stream
     * @throws ClassNotFoundException if class cannot be built
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            ut1 = TimeScalesFactory.getUT1();
            in.defaultReadObject();
        } catch (OrekitException oe) {
            throw OrekitException.createIllegalStateException(oe.getSpecifier(), oe.getParts());
        }
    }

}
