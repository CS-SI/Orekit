/* Copyright 2002-2012 CS Systèmes d'Information
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
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Mean Equator, Mean Equinox Frame.
 * <p>This frame handles precession effects according to the IAU-76 model (Lieske).</p>
 * <p>Its parent frame is the GCRF frame.<p>
 * <p>It is sometimes called Mean of Date (MoD) frame.<p>
 * @author Pascal Parraud
 */
class MODFrame extends FactoryManagedFrame {

    /** Serializable UID. */
    private static final long serialVersionUID = -8560514103170944033L;

    /** 1st coefficient for ZETA precession angle. */
    private static final double ZETA_1 = 2306.2181   * Constants.ARC_SECONDS_TO_RADIANS;
    /** 2nd coefficient for ZETA precession angle. */
    private static final double ZETA_2 =    0.30188  * Constants.ARC_SECONDS_TO_RADIANS;
    /** 3rd coefficient for ZETA precession angle. */
    private static final double ZETA_3 =    0.017998 * Constants.ARC_SECONDS_TO_RADIANS;

    /** 1st coefficient for THETA precession angle. */
    private static final double THETA_1 = 2004.3109   * Constants.ARC_SECONDS_TO_RADIANS;
    /** 2nd coefficient for THETA precession angle. */
    private static final double THETA_2 =   -0.42665  * Constants.ARC_SECONDS_TO_RADIANS;
    /** 3rd coefficient for THETA precession angle. */
    private static final double THETA_3 =   -0.041833 * Constants.ARC_SECONDS_TO_RADIANS;

    /** 1st coefficient for Z precession angle. */
    private static final double Z_1 = 2306.2181   * Constants.ARC_SECONDS_TO_RADIANS;
    /** 2nd coefficient for Z precession angle. */
    private static final double Z_2 =    1.09468  * Constants.ARC_SECONDS_TO_RADIANS;
    /** 3rd coefficient for Z precession angle. */
    private static final double Z_3 =    0.018203 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Simple constructor, applying EOP corrections (here, EME2000/GCRF bias compensation).
     * @param factoryKey key of the frame within the factory
     * @exception OrekitException if EOP parameters cannot be read
     */
    protected MODFrame(final Predefined factoryKey)
        throws OrekitException {
        this(true, factoryKey);
    }

    /** Simple constructor.
     * @param applyEOPCorr if true, EOP correction is applied (here, EME2000/GCRF bias compensation)
     * @param factoryKey key of the frame within the factory
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected MODFrame(final boolean applyEOPCorr, final Predefined factoryKey)
        throws OrekitException {

        super(applyEOPCorr ? FramesFactory.getGCRF() : FramesFactory.getEME2000(),
              null , true, factoryKey);

        // everything is in place, we can now synchronize the frame
        updateFrame(AbsoluteDate.J2000_EPOCH);

    }

    /** Update the frame to the given date.
     * <p>The update considers the precession effects.</p>
     * @param date new value of the date
     */
    protected void updateFrame(final AbsoluteDate date) {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // offset from J2000 epoch in julian centuries
            final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
            final double ttc = tts / Constants.JULIAN_CENTURY;

            // compute the zeta precession angle
            final double zeta = ((ZETA_3 * ttc + ZETA_2) * ttc + ZETA_1) * ttc;

            // compute the theta precession angle
            final double theta = ((THETA_3 * ttc + THETA_2) * ttc + THETA_1) * ttc;

            // compute the z precession angle
            final double z = ((Z_3 * ttc + Z_2) * ttc + Z_1) * ttc;

            // elementary rotations for precession
            final Rotation r1 = new Rotation(Vector3D.PLUS_K,  z);
            final Rotation r2 = new Rotation(Vector3D.PLUS_J, -theta);
            final Rotation r3 = new Rotation(Vector3D.PLUS_K,  zeta);

            // complete precession
            final Rotation precession = r1.applyTo(r2.applyTo(r3));

            // set up the transform from parent GCRF
            setTransform(new Transform(date, precession));

            cachedDate = date;
        }

    }

}
